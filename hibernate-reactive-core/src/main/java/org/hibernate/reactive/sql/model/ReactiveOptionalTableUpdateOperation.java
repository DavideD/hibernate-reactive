/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.sql.model;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.concurrent.CompletionStage;

import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.jdbc.mutation.internal.MutationQueryOptions;
import org.hibernate.engine.jdbc.mutation.internal.PreparedStatementGroupSingleTable;
import org.hibernate.engine.jdbc.mutation.spi.BindingGroup;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.persister.entity.mutation.UpdateValuesAnalysis;
import org.hibernate.reactive.logging.impl.Log;
import org.hibernate.reactive.util.impl.CompletionStages;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ValuesAnalysis;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.TableInsert;
import org.hibernate.sql.model.ast.TableUpdate;
import org.hibernate.sql.model.internal.OptionalTableUpdate;
import org.hibernate.sql.model.internal.TableInsertCustomSql;
import org.hibernate.sql.model.internal.TableInsertStandard;
import org.hibernate.sql.model.internal.TableUpdateCustomSql;
import org.hibernate.sql.model.internal.TableUpdateStandard;
import org.hibernate.sql.model.jdbc.JdbcDeleteMutation;
import org.hibernate.sql.model.jdbc.JdbcInsertMutation;
import org.hibernate.sql.model.jdbc.JdbcMutationOperation;
import org.hibernate.sql.model.jdbc.OptionalTableUpdateOperation;

import static java.lang.invoke.MethodHandles.lookup;
import static org.hibernate.reactive.logging.impl.LoggerFactory.make;
import static org.hibernate.reactive.util.impl.CompletionStages.voidFuture;

public class ReactiveOptionalTableUpdateOperation extends OptionalTableUpdateOperation
		implements ReactiveSelfExecutingUpdateOperation {
	private static final Log LOG = make( Log.class, lookup() );

	public ReactiveOptionalTableUpdateOperation(
			MutationTarget<?> mutationTarget,
			OptionalTableUpdate upsert,
			SessionFactoryImplementor factory) {
		super( mutationTarget, upsert, factory );
	}

	@Override
	public void performMutation(
			JdbcValueBindings jdbcValueBindings,
			ValuesAnalysis valuesAnalysis,
			SharedSessionContractImplementor session) {
		throw LOG.nonReactiveMethodCall( "performReactiveMutation" );
	}

	public CompletionStage<Void> performReactiveMutation(
			JdbcValueBindings jdbcValueBindings,
			ValuesAnalysis incomingValuesAnalysis,
			SharedSessionContractImplementor session) {
		final TableMapping tableMapping = getTableDetails();
		final UpdateValuesAnalysis valuesAnalysis = (UpdateValuesAnalysis) incomingValuesAnalysis;

		return voidFuture().thenCompose( v -> !valuesAnalysis.getTablesNeedingUpdate().contains( getTableDetails() )
				? voidFuture()
				: doReactiveMutation( tableMapping, jdbcValueBindings, valuesAnalysis, session ) );
	}

	/**
	 *
	 * @see OptionalTableUpdateOperation
	 * @param tableMapping
	 * @param jdbcValueBindings
	 * @param valuesAnalysis
	 * @param session
	 * @return
	 */
	private CompletionStage<Void> doReactiveMutation(
			TableMapping tableMapping,
			JdbcValueBindings jdbcValueBindings,
			UpdateValuesAnalysis valuesAnalysis,
			SharedSessionContractImplementor session) {

		return voidFuture().thenCompose( v -> {
			// Check if should delete
			if ( shouldDelete( valuesAnalysis, tableMapping ) ) {
				// all the new values for this table were null - possibly delete the row
				return performReactiveDelete( jdbcValueBindings, session );
			} else {
				CompletionStage<Boolean> didUpdate = CompletionStages.falseFuture(); //performReactiveUpdate( tableMapping, jdbcValueBindings, valuesAnalysis, session );
//				if( !didUpdate ) {
					return performReactiveInsert( jdbcValueBindings, session );
//				}
			}
		} );
	}

	private boolean shouldDelete(UpdateValuesAnalysis valuesAnalysis, TableMapping tableMapping) {
		return !valuesAnalysis.getTablesWithNonNullValues().contains( tableMapping ) && valuesAnalysis.getTablesWithPreviousNonNullValues().contains( tableMapping );
	}

	protected CompletionStage<Void> performReactiveDelete(
			JdbcValueBindings jdbcValueBindings,
			SharedSessionContractImplementor session) {
		final JdbcDeleteMutation jdbcDelete = createJdbcDelete( session );

		final PreparedStatement deleteStatement = createStatementDetails( jdbcDelete, session );
		session.getJdbcServices().getSqlStatementLogger().logStatement( jdbcDelete.getSqlString() );

		bindKeyValues( jdbcValueBindings, deleteStatement, jdbcDelete, session );
		String deleteSQL = jdbcDelete.getSqlString();
		CompletionStage<Integer> result = CompletionStages.completedFuture( session.getSessionFactory().fromSession( s -> s.createMutationQuery( deleteSQL ).executeUpdate() ) );
		return voidFuture();
	}

	/**
	 *  @See org.hibernate.sql.model.jdbc.OptionalTableUpdateOperation.performUpdate()
	 * @param tableMapping
	 * @param jdbcValueBindings
	 * @param valuesAnalysis
	 * @param session
	 * @return
	 */
	protected CompletionStage<Boolean> performReactiveUpdate(
			TableMapping tableMapping,
			JdbcValueBindings jdbcValueBindings,
			UpdateValuesAnalysis valuesAnalysis,
			SharedSessionContractImplementor session) {
		if ( valuesAnalysis.getTablesWithPreviousNonNullValues().contains( tableMapping ) ) {
			return CompletionStages.completedFuture( Boolean.FALSE );
		}

		return doReactiveUpdate( tableMapping, jdbcValueBindings, valuesAnalysis, session);
	}

	private CompletionStage<Boolean> doReactiveUpdate(TableMapping tableMapping,
													  JdbcValueBindings jdbcValueBindings,
													  UpdateValuesAnalysis valuesAnalysis,
													  SharedSessionContractImplementor session) {
		return voidFuture().thenCompose( v -> {
			MutationTarget mutationTarget = getMutationTarget();
			final TableUpdate<JdbcMutationOperation> tableUpdate;
			if ( tableMapping.getUpdateDetails() != null && tableMapping.getUpdateDetails().getCustomSql() != null ) {
				tableUpdate = new TableUpdateCustomSql(
						new MutatingTableReference( tableMapping ),
						mutationTarget,
						"upsert update for " + mutationTarget.getRolePath(),
						getValueBindings(),
						getKeyBindings(),
						getOptimisticLockBindings(),
						getParameters()
				);
			}
			else {
				tableUpdate = new TableUpdateStandard(
						new MutatingTableReference( tableMapping ),
						mutationTarget,
						"upsert update for " + mutationTarget.getRolePath(),
						getValueBindings(),
						getKeyBindings(),
						getOptimisticLockBindings(),
						getParameters()
				);
			}

			final SqlAstTranslator<JdbcMutationOperation> translator = session
					.getJdbcServices()
					.getJdbcEnvironment()
					.getSqlAstTranslatorFactory()
					.buildModelMutationTranslator( tableUpdate, session.getFactory() );

			final JdbcMutationOperation jdbcUpdate = translator.translate( null, MutationQueryOptions.INSTANCE );

			final PreparedStatementGroupSingleTable statementGroup = new PreparedStatementGroupSingleTable(
					jdbcUpdate,
					session
			);

			final PreparedStatementDetails statementDetails = statementGroup.resolvePreparedStatementDetails(
					tableMapping.getTableName() );

			final PreparedStatement updateStatement = statementDetails.resolveStatement();

			session.getJdbcServices().getSqlStatementLogger().logStatement( statementDetails.getSqlString() );

			jdbcValueBindings.beforeStatement( statementDetails );

			final int rowCount = session.getJdbcCoordinator().getResultSetReturn()
					.executeUpdate( updateStatement, statementDetails.getSqlString() );

			if ( rowCount == 0 ) {
				return CompletionStages.completedFuture( Boolean.FALSE );
			}

//			getExpectation().verifyOutcome( rowCount, updateStatement, -1, statementDetails.getSqlString() );

			return CompletionStages.completedFuture( Boolean.TRUE );
		} );
	}

	private CompletionStage<Void> performReactiveInsert(
			JdbcValueBindings jdbcValueBindings,
			SharedSessionContractImplementor session) {
		return doPerformReactiveInsert( jdbcValueBindings, session ).thenCompose( s -> CompletionStages.voidFuture() );
	}

	/**
	 * @See org.hibernate.sql.model.jdbc.OptionalTableUpdateOperation.performInsert()
	 *
	 * @param jdbcValueBindings
	 * @param session
	 * @return
	 */
	private CompletionStage<Void> doPerformReactiveInsert(
			JdbcValueBindings jdbcValueBindings,
			SharedSessionContractImplementor session) {
		// TODO:  Currently Fails
		return voidFuture().thenCompose( v -> {

			final JdbcInsertMutation jdbcInsert = reactiveCreateJdbcInsert( session );

			final PreparedStatement insertStatement = session.getJdbcCoordinator()
					.getStatementPreparer()
					.prepareStatement(
							jdbcInsert.getSqlString() );


			session.getJdbcServices().getSqlStatementLogger().logStatement( jdbcInsert.getSqlString() );

			final BindingGroup bindingGroup = jdbcValueBindings.getBindingGroup( getTableDetails().getTableName() );
			if ( bindingGroup != null ) {
				bindingGroup.forEachBinding( (binding) -> {
					try {
						binding.getValueBinder().bind(
								insertStatement,
								binding.getValue(),
								binding.getPosition(),
								session
						);
					}
					catch (SQLException e) {
						throw session.getJdbcServices().getSqlExceptionHelper().convert(
								e,
								"Unable to bind parameter for upsert insert",
								jdbcInsert.getSqlString()
						);
					}
				} );
			}

			session.getJdbcCoordinator().getResultSetReturn()
					.executeUpdate( insertStatement, jdbcInsert.getSqlString() );

			return voidFuture();
		} );
	}

	/**
	 * @See org.hibernate.sql.model.jdbc.OptionalTableUpdateOperation.createJdbcInsert()
	 * @param session
	 * @return
	 */
	private JdbcInsertMutation reactiveCreateJdbcInsert(SharedSessionContractImplementor session) {
		final TableInsert tableInsert;
		if ( getTableDetails().getInsertDetails() != null
				&& getTableDetails().getInsertDetails().getCustomSql() != null ) {
			tableInsert = new TableInsertCustomSql(
					new MutatingTableReference( getTableDetails() ),
					getMutationTarget(),
					CollectionHelper.combine( getValueBindings(), getKeyBindings() ),
					getParameters()
			);
		}
		else {
			tableInsert = new TableInsertStandard(
					new MutatingTableReference( getTableDetails() ),
					getMutationTarget(),
					CollectionHelper.combine( getValueBindings(), getKeyBindings() ),
					Collections.emptyList(),
					getParameters()
			);
		}

		final SessionFactoryImplementor factory = session.getSessionFactory();
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = factory
				.getJdbcServices()
				.getJdbcEnvironment()
				.getSqlAstTranslatorFactory();

		final SqlAstTranslator<JdbcInsertMutation> translator = sqlAstTranslatorFactory.buildModelMutationTranslator(
				tableInsert,
				factory
		);

		return translator.translate( null, MutationQueryOptions.INSTANCE );
	}
}
