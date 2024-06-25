/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.sql.results.graph.embeddable.internal;

import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.hibernate.reactive.logging.impl.Log;
import org.hibernate.reactive.sql.results.graph.ReactiveInitializer;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerData;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.embeddable.EmbeddableResultGraphNode;
import org.hibernate.sql.results.graph.embeddable.internal.NonAggregatedIdentifierMappingInitializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import static java.lang.invoke.MethodHandles.lookup;
import static org.hibernate.reactive.logging.impl.LoggerFactory.make;
import static org.hibernate.reactive.util.impl.CompletionStages.loop;
import static org.hibernate.reactive.util.impl.CompletionStages.voidFuture;

public class ReactiveNonAggregatedIdentifierMappingInitializer extends NonAggregatedIdentifierMappingInitializer
		implements ReactiveInitializer<NonAggregatedIdentifierMappingInitializer.NonAggregatedIdentifierMappingInitializerData> {

	private static final Log LOG = make( Log.class, lookup() );

	public ReactiveNonAggregatedIdentifierMappingInitializer(
			EmbeddableResultGraphNode resultDescriptor,
			InitializerParent<?> parent,
			AssemblerCreationState creationState,
			boolean isResultInitializer) {
		super( resultDescriptor, parent, creationState, isResultInitializer );
	}
//
//	@Override
//	public void resolveInstance(NonAggregatedIdentifierMappingInitializerData data) {
//		throw LOG.nonReactiveMethodCall( "reactiveResolveInstance" );
//	}

	@Override
	public CompletionStage<Void> reactiveResolveInstance(NonAggregatedIdentifierMappingInitializerData data) {
		super.resolveInstance( data );
		return voidFuture();
	}

	@Override
	public void initializeInstance(NonAggregatedIdentifierMappingInitializerData data) {
		throw LOG.nonReactiveMethodCall( "reactiveInitializeInstance" );
	}

	@Override
	public CompletionStage<Void> reactiveInitializeInstance(NonAggregatedIdentifierMappingInitializerData data) {
		throw LOG.notYetImplemented();
	}

	@Override
	protected void forEachSubInitializer(
			BiConsumer<Initializer<?>, RowProcessingState> consumer,
			InitializerData data) {
//		throw LOG.nonReactiveMethodCall( "forEachReactiveSubInitializer" );
		super.forEachSubInitializer( consumer, data );
	}

	@Override
	public CompletionStage<Void> forEachReactiveSubInitializer(
			BiFunction<ReactiveInitializer<?>, RowProcessingState, CompletionStage<Void>> consumer,
			InitializerData data) {
		final RowProcessingState rowProcessingState = data.getRowProcessingState();
		return loop( getInitializers(), initializer -> consumer
				.apply( (ReactiveInitializer<?>) initializer, rowProcessingState )
		);
	}
}
