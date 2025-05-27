/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.sql.results.graph.embeddable.internal;


import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.VirtualModelPart;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.reactive.sql.exec.spi.ReactiveRowProcessingState;
import org.hibernate.reactive.sql.results.graph.ReactiveDomainResultsAssembler;
import org.hibernate.reactive.sql.results.graph.ReactiveInitializer;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerData;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.embeddable.EmbeddableResultGraphNode;
import org.hibernate.sql.results.graph.embeddable.internal.EmbeddableInitializerImpl;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import static org.hibernate.reactive.util.impl.CompletionStages.completedFuture;
import static org.hibernate.reactive.util.impl.CompletionStages.loop;
import static org.hibernate.reactive.util.impl.CompletionStages.voidFuture;
import static org.hibernate.reactive.util.impl.CompletionStages.whileLoop;
import static org.hibernate.sql.results.graph.entity.internal.BatchEntityInsideEmbeddableSelectFetchInitializer.BATCH_PROPERTY;

public class ReactiveEmbeddableInitializerImpl extends EmbeddableInitializerImpl
		implements ReactiveInitializer<EmbeddableInitializerImpl.EmbeddableInitializerData> {

	private static class ReactiveEmbeddableInitializerData extends EmbeddableInitializerData {

		public ReactiveEmbeddableInitializerData(
				EmbeddableInitializerImpl initializer,
				RowProcessingState rowProcessingState) {
			super( initializer, rowProcessingState );
		}

		public Object[] getRowState(){
			return rowState;
		}

		public EmbeddableMappingType.ConcreteEmbeddableType getEmbeddableType() {
			return concreteEmbeddableType;
		}

		@Override
		public void setState(State state) {
			super.setState( state );
			if ( State.UNINITIALIZED == state ) {
				// reset instance to null as otherwise EmbeddableInitializerImpl#prepareCompositeInstance
				//  will never create a new instance after the "first row with a non-null instance" gets processed
				setInstance( null );
			}
		}

		public EmbeddableMappingType.ConcreteEmbeddableType getConcreteEmbeddableType() {
			return super.concreteEmbeddableType;
		}
	}

	public ReactiveEmbeddableInitializerImpl(
			EmbeddableResultGraphNode resultDescriptor,
			BasicFetch<?> discriminatorFetch,
			InitializerParent<?> parent,
			AssemblerCreationState creationState,
			boolean isResultInitializer) {
		super( resultDescriptor, discriminatorFetch, parent, creationState, isResultInitializer );
	}

	@Override
	protected InitializerData createInitializerData(RowProcessingState rowProcessingState) {
		return new ReactiveEmbeddableInitializerData( this, rowProcessingState );
	}

	@Override
	public CompletionStage<Void> reactiveResolveInstance(EmbeddableInitializerData data) {
		if ( data.getState() != State.KEY_RESOLVED ) {
			return voidFuture();
		}

		data.setState( State.RESOLVED );
		return extractRowState( (ReactiveEmbeddableInitializerData) data )
				.thenAccept( unused -> prepareCompositeInstance( (ReactiveEmbeddableInitializerData) data ) );
	}

	private CompletionStage<Void> extractRowState(ReactiveEmbeddableInitializerData data) {
		final DomainResultAssembler<?>[] subAssemblers = assemblers[data.getSubclassId()];
		final RowProcessingState rowProcessingState = data.getRowProcessingState();
		final Object[] rowState = data.getRowState();
		final boolean[] stateAllNull = {true};
		final int[] index = {0};
		final WhileCondition whileCondition = new WhileCondition(subAssemblers, index);
		return whileLoop( whileCondition, () -> {
					final int i = index[0];
					final DomainResultAssembler<?> assembler = subAssemblers[i];
					final CompletionStage<Void> completionStage;
					if ( assembler instanceof ReactiveDomainResultsAssembler<?> reactiveAssembler ) {
						completionStage = reactiveAssembler.reactiveAssemble( (ReactiveRowProcessingState) rowProcessingState )
								.thenCompose( contributorValue -> setContributorValue(
										contributorValue,
										i,
										rowState,
										stateAllNull,
										whileCondition
								) );
					}
					else {
						completionStage = setContributorValue(
								assembler == null ? null : assembler.assemble( rowProcessingState ),
								i,
								rowState,
								stateAllNull,
								whileCondition
						);
					}
					index[0] = i + 1;
					return completionStage;
		})
				.whenComplete(
						(unused, throwable) -> {
							if ( stateAllNull[0] ) {
								data.setState( State.MISSING );
							}
						}
				);
	}

	private static class WhileCondition implements Supplier<Boolean> {
		boolean forceExit;
		final int maxIndex;
		final int[] currentIndex;

		public WhileCondition(DomainResultAssembler<?>[] subAssemblers, int[] index) {
			maxIndex = subAssemblers.length;
			currentIndex = index;
		}

		@Override
		public Boolean get() {
			return currentIndex[0] < maxIndex && !forceExit;
		}
	}

	private CompletionStage<Void> setContributorValue(
			Object contributorValue,
			int index,
			Object[] rowState,
			boolean[] stateAllNull,
			WhileCondition whileCondition) {
		if ( contributorValue == BATCH_PROPERTY ) {
			rowState[index] = null;
		}
		else {
			rowState[index] = contributorValue;
		}
		if ( contributorValue != null ) {
			stateAllNull[0] = false;
		}
		else if ( isPartOfKey() ) {
			// If this is a foreign key and there is a null part, the whole thing has to be turned into null
			stateAllNull[0] = true;
			whileCondition.forceExit = true;
		}
		return voidFuture();
	}

	private CompletionStage<Void> prepareCompositeInstance(ReactiveEmbeddableInitializerData data) {
		// Virtual model parts use the owning entity as container which the fetch parent access provides.
		// For an identifier or foreign key this is called during the resolveKey phase of the fetch parent,
		// so we can't use the fetch parent access in that case.
		final ReactiveInitializer<ReactiveEmbeddableInitializerData> parent = (ReactiveInitializer<ReactiveEmbeddableInitializerData>) getParent();
		if ( parent != null && getInitializedPart() instanceof VirtualModelPart && !isPartOfKey() && data.getState() != State.MISSING ) {
			final ReactiveEmbeddableInitializerData subData = parent.getData( data.getRowProcessingState() );
			return parent.reactiveResolveInstance( subData )
					.thenAccept(
							unused -> {
								data.setInstance( parent.getResolvedInstance( subData ) );
								if ( data.getState() != State.INITIALIZED && data.getInstance() == null ) {
									createCompositeInstance( data )
											.thenAccept( o -> data.setInstance( o ) );
								}
							}
					).thenAccept( unused -> {
						if ( data.getInstance() == null ) {
							createCompositeInstance( data )
									.thenAccept( data::setInstance );
						}
					} );
		}

		if ( data.getInstance() == null ) {
			return createCompositeInstance( data )
					.thenAccept( data::setInstance );
		}
		return voidFuture();
	}

	private CompletionStage<Object> createCompositeInstance(ReactiveEmbeddableInitializerData data) {
		if ( data.getState() == State.MISSING ) {
			return completedFuture( null );
		}

		final EmbeddableInstantiator instantiator = data.getConcreteEmbeddableType() == null
				? getInitializedPart().getEmbeddableTypeDescriptor().getRepresentationStrategy().getInstantiator()
				: data.getConcreteEmbeddableType().getInstantiator();
		final Object instance = instantiator.instantiate( data );
		data.setState( State.RESOLVED );
		return completedFuture( instance );
	}

	@Override
	public CompletionStage<Void> reactiveInitializeInstance(EmbeddableInitializerData data) {
		super.initializeInstance( data );
		return voidFuture();
	}

	@Override
	public CompletionStage<Void> forEachReactiveSubInitializer(
			BiFunction<ReactiveInitializer<?>, RowProcessingState, CompletionStage<Void>> consumer,
			InitializerData data) {
		final ReactiveEmbeddableInitializerData embeddableInitializerData = (ReactiveEmbeddableInitializerData) data;
		final RowProcessingState rowProcessingState = embeddableInitializerData.getRowProcessingState();
		if ( embeddableInitializerData.getConcreteEmbeddableType() == null ) {
			return loop( subInitializers, subInitializer -> loop( subInitializer, initializer -> consumer
					.apply( (ReactiveInitializer<?>) initializer, rowProcessingState )
			) );
		}
		else {
			Initializer<InitializerData>[] initializers = subInitializers[embeddableInitializerData.getSubclassId()];
			return loop( 0, initializers.length, i -> {
				ReactiveInitializer<?> reactiveInitializer = (ReactiveInitializer<?>) initializers[i];
				return consumer.apply( reactiveInitializer, rowProcessingState );
			} );
		}
	}

	@Override
	public void resolveInstance(EmbeddableInitializerData data) {
		// We need to clean up the instance, otherwise the .find with multiple id is not going to work correctly.
		// It will only return the first element of the list. See EmbeddedIdTest#testFindMultipleIds.
		// ORM doesn't have this issue because they don't have a find with multiple ids.
		data.setInstance( null );
		super.resolveInstance( data );
	}

	@Override
	public Object getResolvedInstance(EmbeddableInitializerData data) {
		return super.getResolvedInstance( data );
	}
}
