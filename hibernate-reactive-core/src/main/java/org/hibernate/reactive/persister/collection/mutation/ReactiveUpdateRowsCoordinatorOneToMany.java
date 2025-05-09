/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.persister.collection.mutation;

import java.util.Iterator;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.spi.BatchKeyAccess;
import org.hibernate.engine.jdbc.mutation.spi.MutationExecutorService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.mutation.CollectionMutationTarget;
import org.hibernate.persister.collection.mutation.RowMutationOperations;
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinatorOneToMany;
import org.hibernate.reactive.engine.jdbc.env.internal.ReactiveMutationExecutor;
import org.hibernate.reactive.logging.impl.Log;
import org.hibernate.reactive.util.impl.CompletionStages;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.jdbc.JdbcMutationOperation;

import static java.lang.invoke.MethodHandles.lookup;
import static org.hibernate.reactive.logging.impl.LoggerFactory.make;
import static org.hibernate.reactive.util.impl.CompletionStages.loop;
import static org.hibernate.reactive.util.impl.CompletionStages.voidFuture;
import static org.hibernate.reactive.util.impl.CompletionStages.zeroFuture;
import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;
import static org.hibernate.sql.model.MutationType.DELETE;
import static org.hibernate.sql.model.MutationType.INSERT;
import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

public class ReactiveUpdateRowsCoordinatorOneToMany extends UpdateRowsCoordinatorOneToMany implements ReactiveUpdateRowsCoordinator{

	private static final Log LOG = make( Log.class, lookup() );
	private final RowMutationOperations rowMutationOperations;

	private MutationOperationGroup deleteOperationGroup;
	private MutationOperationGroup insertOperationGroup;

	public ReactiveUpdateRowsCoordinatorOneToMany(CollectionMutationTarget mutationTarget, RowMutationOperations rowMutationOperations, SessionFactoryImplementor sessionFactory) {
		super( mutationTarget, rowMutationOperations, sessionFactory );
		this.rowMutationOperations = rowMutationOperations;
	}

	@Override
	public void updateRows(Object key, PersistentCollection<?> collection, SharedSessionContractImplementor session) {
		throw LOG.nonReactiveMethodCall( "reactiveUpdateRows" );
	}

	@Override
	protected int doUpdate(Object key, PersistentCollection<?> collection, SharedSessionContractImplementor session) {
		throw LOG.nonReactiveMethodCall( "doReactiveUpdate" );
	}

	@Override
	public CompletionStage<Void> reactiveUpdateRows(Object key, PersistentCollection<?> collection, SharedSessionContractImplementor session) {
		MODEL_MUTATION_LOGGER.tracef( "Updating collection rows - %s#%s", getMutationTarget().getRolePath(), key );

		// update all the modified entries
		return doReactiveUpdate( key, collection, session )
				.thenAccept( count -> MODEL_MUTATION_LOGGER
						.debugf( "Updated `%s` collection rows - %s#%s", count, getMutationTarget().getRolePath(), key ) );
	}

	private CompletionStage<Integer> doReactiveUpdate(Object key, PersistentCollection<?> collection, SharedSessionContractImplementor session) {
		final Function<Void, CompletionStage<Integer>> insertRowsFun = v -> {
			if ( rowMutationOperations.hasInsertRow() ) {
				return insertRows( key, collection, session );
			}

			return zeroFuture();
		};
		if ( rowMutationOperations.hasDeleteRow() ) {
			return deleteRows( key, collection, session )
					.thenCompose( insertRowsFun );
		}
		return insertRowsFun.apply( null );
	}

	private CompletionStage<Integer> insertRows(Object key, PersistentCollection<?> collection, SharedSessionContractImplementor session) {
		final MutationOperationGroup operationGroup = resolveInsertGroup();
		final PluralAttributeMapping attributeMapping = getMutationTarget().getTargetPart();
		final CollectionPersister collectionDescriptor = attributeMapping.getCollectionDescriptor();
		final ReactiveMutationExecutor mutationExecutor = reactiveMutationExecutor( session, operationGroup, this::getInsertBatchKey );

		final int[] entryPosition = { -1 };
		return voidFuture()
				.thenApply( unused -> {
					final JdbcValueBindings jdbcValueBindings = mutationExecutor.getJdbcValueBindings();
					final Iterator<?> entries = collection.entries( collectionDescriptor );

					return loop( entries, (entry, integer) -> {
						entryPosition[0]++;
						if ( !collection.needsUpdating( entry, entryPosition[0], attributeMapping ) ) {
							return voidFuture();
						}

						rowMutationOperations.getInsertRowValues()
								.applyValues( collection, key, entry, entryPosition[0], session, jdbcValueBindings );

						return mutationExecutor.executeReactive( entry, null, null, null, session );
					} );
				} )
				.whenComplete( (o, throwable) -> mutationExecutor.release() )
				.thenApply( unused -> entryPosition[0] );
	}

	private BasicBatchKey getInsertBatchKey() {
		return new BasicBatchKey( getMutationTarget().getRolePath() + "#UPDATE-INSERT" );
	}

	private BasicBatchKey getDeleteBatchKey() {
		return new BasicBatchKey( getMutationTarget().getRolePath() + "#UPDATE-DELETE" );
	}

	private CompletionStage<Void> deleteRows(
			Object key,
			PersistentCollection<?> collection,
			SharedSessionContractImplementor session) {
		final MutationOperationGroup operationGroup = resolveDeleteGroup();
		final PluralAttributeMapping attributeMapping = getMutationTarget().getTargetPart();
		final CollectionPersister collectionDescriptor = attributeMapping.getCollectionDescriptor();
		final ReactiveMutationExecutor mutationExecutor = reactiveMutationExecutor( session, operationGroup, this::getDeleteBatchKey );

		final int[] entryPosition = { -1 };
		return voidFuture()
				.thenApply( unused -> {
					final JdbcValueBindings jdbcValueBindings = mutationExecutor.getJdbcValueBindings();
					final Iterator<?> entries = collection.entries( collectionDescriptor );

					return loop( entries, (entry, integer) -> {
						entryPosition[0]++;
						if ( !collection.needsUpdating( entry, entryPosition[0], attributeMapping ) ) {
							return voidFuture();
						}

						rowMutationOperations.getDeleteRowRestrictions()
								.applyRestrictions( collection, key, entry, entryPosition[0], session, jdbcValueBindings );

						return mutationExecutor.executeReactive( entry, null, null, null, session );
					} );
				} )
				.whenComplete( (o, throwable) -> mutationExecutor.release() )
				.thenCompose( CompletionStages::voidFuture );
	}

	private ReactiveMutationExecutor reactiveMutationExecutor(SharedSessionContractImplementor session, MutationOperationGroup operationGroup, BatchKeyAccess batchKeySupplier) {
		final MutationExecutorService mutationExecutorService = session
				.getFactory()
				.getServiceRegistry()
				.getService( MutationExecutorService.class );
		return (ReactiveMutationExecutor) mutationExecutorService
				.createExecutor( batchKeySupplier, operationGroup, session );
	}

	//FIXME: Duplicated form ORM
	private MutationOperationGroup resolveDeleteGroup() {
		if ( deleteOperationGroup == null ) {
			final JdbcMutationOperation operation = rowMutationOperations.getDeleteRowOperation();
			assert operation != null;

			deleteOperationGroup = singleOperation( DELETE, getMutationTarget(), operation );
		}

		return deleteOperationGroup;
	}


	//FIXME: Duplicated from ORM
	private MutationOperationGroup resolveInsertGroup() {
		if ( insertOperationGroup == null ) {
			final JdbcMutationOperation operation = rowMutationOperations.getInsertRowOperation();
			assert operation != null;

			insertOperationGroup = singleOperation( INSERT, getMutationTarget(), operation );
		}

		return insertOperationGroup;
	}
}
