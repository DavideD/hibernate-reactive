/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.sql.results.graph.entity;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletionStage;

import org.hibernate.LockMode;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.reactive.logging.impl.Log;
import org.hibernate.reactive.logging.impl.LoggerFactory;
import org.hibernate.reactive.sql.exec.spi.ReactiveRowProcessingState;
import org.hibernate.reactive.sql.results.graph.ReactiveDomainResultsAssembler;
import org.hibernate.reactive.sql.results.graph.ReactiveInitializer;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.entity.AbstractEntityInitializer;
import org.hibernate.sql.results.graph.entity.EntityLoadingLogging;
import org.hibernate.sql.results.graph.entity.EntityResultGraphNode;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.stat.spi.StatisticsImplementor;

import static org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer.UNFETCHED_PROPERTY;
import static org.hibernate.engine.internal.ManagedTypeHelper.asPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.ManagedTypeHelper.isPersistentAttributeInterceptable;
import static org.hibernate.internal.log.LoggingHelper.toLoggableString;
import static org.hibernate.reactive.util.impl.CompletionStages.loop;
import static org.hibernate.reactive.util.impl.CompletionStages.voidFuture;

public abstract class ReactiveAbstractEntityInitializer extends AbstractEntityInitializer implements ReactiveInitializer {

	private static final Log LOG = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected ReactiveAbstractEntityInitializer(
			EntityResultGraphNode resultDescriptor,
			NavigablePath navigablePath,
			LockMode lockMode,
			Fetch identifierFetch,
			Fetch discriminatorFetch,
			DomainResult<Object> rowIdResult,
			AssemblerCreationState creationState) {
		this(
				resultDescriptor,
				navigablePath,
				lockMode,
				identifierFetch,
				discriminatorFetch,
				rowIdResult,
				null,
				creationState
		);
	}

	protected ReactiveAbstractEntityInitializer(
			EntityResultGraphNode resultDescriptor,
			NavigablePath navigablePath,
			LockMode lockMode,
			Fetch identifierFetch,
			Fetch discriminatorFetch,
			DomainResult<Object> rowIdResult,
			FetchParentAccess parentAccess,
			AssemblerCreationState creationState) {
		super(
				resultDescriptor,
				navigablePath,
				lockMode,
				identifierFetch,
				discriminatorFetch,
				rowIdResult,
				parentAccess,
				creationState
		);
	}

	@Override
	public void resolveInstance(RowProcessingState rowProcessingState) {
		super.resolveInstance( rowProcessingState );
	}

	@Override
	public void initializeInstance(RowProcessingState rowProcessingState) {
		throw LOG.nonReactiveMethodCall( "reactiveInitializeInstance" );
	}

	@Override
	public CompletionStage<Void> reactiveResolveInstance(ReactiveRowProcessingState rowProcessingState) {
		super.resolveInstance( rowProcessingState );
		return voidFuture();
	}

	@Override
	public CompletionStage<Void> reactiveInitializeInstance(ReactiveRowProcessingState rowProcessingState) {
		if ( state == State.KEY_RESOLVED || state == State.RESOLVED ) {
			return initializeEntity( getEntityInstanceForNotify(), rowProcessingState )
					.thenAccept( v -> state = State.INITIALIZED );
		}
		return voidFuture();
	}

	protected CompletionStage<Void> initializeEntity(Object toInitialize, RowProcessingState rowProcessingState) {
		if ( !skipInitialization( toInitialize, rowProcessingState ) ) {
			assert consistentInstance( toInitialize, rowProcessingState );
			return initializeEntityInstance( toInitialize, rowProcessingState );
		}
		return voidFuture();
	}


	protected CompletionStage<Object[]> reactiveExtractConcreteTypeStateValues(RowProcessingState rowProcessingState) {
		final Object[] values = new Object[getConcreteDescriptor().getNumberOfAttributeMappings()];
		final DomainResultAssembler<?>[] concreteAssemblers = getAssemblers()[getConcreteDescriptor().getSubclassId()];
		return loop( 0, values.length, i -> {
			final DomainResultAssembler<?> assembler = concreteAssemblers[i];
			if ( assembler instanceof  ReactiveDomainResultsAssembler) {
				return ( (ReactiveDomainResultsAssembler) assembler )
						.reactiveAssemble( (ReactiveRowProcessingState) rowProcessingState )
						.thenAccept( obj -> values[i] = obj );
			}
			else {
				values[i] = assembler == null ? UNFETCHED_PROPERTY : assembler.assemble( rowProcessingState );
				return voidFuture();
			}
		} ).thenApply( unused -> values );
	}

	private CompletionStage<Void> initializeEntityInstance(Object toInitialize, RowProcessingState rowProcessingState) {
		final Object entityIdentifier = getEntityKey().getIdentifier();
		final SharedSessionContractImplementor session = rowProcessingState.getSession();
		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();

		if ( EntityLoadingLogging.ENTITY_LOADING_LOGGER.isTraceEnabled() ) {
			EntityLoadingLogging.ENTITY_LOADING_LOGGER.tracef(
					"(%s) Beginning Initializer#initializeInstance process for entity %s",
					getSimpleConcreteImplName(),
					toLoggableString( getNavigablePath(), entityIdentifier )
			);
		}

		getEntityDescriptor().setIdentifier( toInitialize, entityIdentifier, session );
		return reactiveExtractConcreteTypeStateValues( rowProcessingState )
				.thenCompose( entityState -> loop( 0, entityState.length, i -> {
								  if ( entityState[i] instanceof CompletionStage ) {
									  return ( (CompletionStage<Object>) entityState[i] )
											  .thenAccept( state -> entityState[i] = state );
								  }
								  return voidFuture();
							  } ).thenAccept( v -> setResolvedEntityState( entityState ) )
				)
				.thenAccept( v -> {
					if ( isPersistentAttributeInterceptable(toInitialize) ) {
						PersistentAttributeInterceptor persistentAttributeInterceptor =
								asPersistentAttributeInterceptable( toInitialize ).$$_hibernate_getInterceptor();
						if ( persistentAttributeInterceptor == null
								|| persistentAttributeInterceptor instanceof EnhancementAsProxyLazinessInterceptor ) {
							// if we do this after the entity has been initialized the
							// BytecodeLazyAttributeInterceptor#isAttributeLoaded(String fieldName) would return false;
							getConcreteDescriptor().getBytecodeEnhancementMetadata()
									.injectInterceptor( toInitialize, entityIdentifier, session );
						}
					}
					getConcreteDescriptor().setValues( toInitialize, getResolvedEntityState() );
					persistenceContext.addEntity( getEntityKey(), toInitialize );

					// Also register possible unique key entries
					registerPossibleUniqueKeyEntries( toInitialize, session );

					final Object version = getVersionAssembler() != null ? getVersionAssembler().assemble( rowProcessingState ) : null;
					final Object rowId = getRowIdAssembler() != null ? getRowIdAssembler().assemble( rowProcessingState ) : null;

					// from the perspective of Hibernate, an entity is read locked as soon as it is read
					// so regardless of the requested lock mode, we upgrade to at least the read level
					final LockMode lockModeToAcquire = getLockMode() == LockMode.NONE ? LockMode.READ : getLockMode();

					final EntityEntry entityEntry = persistenceContext.addEntry(
							toInitialize,
							Status.LOADING,
							getResolvedEntityState(),
							rowId,
							getEntityKey().getIdentifier(),
							version,
							lockModeToAcquire,
							true,
							getConcreteDescriptor(),
							false
					);

					updateCaches( toInitialize, rowProcessingState, session, persistenceContext, entityIdentifier, version );
					registerNaturalIdResolution( persistenceContext, entityIdentifier );
					takeSnapshot( rowProcessingState, session, persistenceContext, entityEntry );
					getConcreteDescriptor().afterInitialize( toInitialize, session );
					if ( EntityLoadingLogging.ENTITY_LOADING_LOGGER.isDebugEnabled() ) {
						EntityLoadingLogging.ENTITY_LOADING_LOGGER.debugf(
								"(%s) Done materializing entityInstance : %s",
								getSimpleConcreteImplName(),
								toLoggableString( getNavigablePath(), entityIdentifier )
						);
					}

					final StatisticsImplementor statistics = session.getFactory().getStatistics();
					if ( statistics.isStatisticsEnabled() ) {
						if ( !rowProcessingState.isQueryCacheHit() ) {
							statistics.loadEntity( getConcreteDescriptor().getEntityName() );
						}
					}
				} );
	}
}
