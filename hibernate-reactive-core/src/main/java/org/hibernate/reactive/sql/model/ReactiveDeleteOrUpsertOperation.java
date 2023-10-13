/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.sql.model;

import java.sql.SQLException;
import java.util.concurrent.CompletionStage;

import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.jdbc.mutation.internal.PreparedStatementGroupSingleTable;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.persister.entity.mutation.EntityTableMapping;
import org.hibernate.persister.entity.mutation.UpdateValuesAnalysis;
import org.hibernate.reactive.adaptor.impl.PrepareStatementDetailsAdaptor;
import org.hibernate.reactive.adaptor.impl.PreparedStatementAdaptor;
import org.hibernate.reactive.logging.impl.Log;
import org.hibernate.reactive.pool.ReactiveConnection;
import org.hibernate.reactive.session.ReactiveConnectionSupplier;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ValuesAnalysis;
import org.hibernate.sql.model.internal.OptionalTableUpdate;
import org.hibernate.sql.model.jdbc.DeleteOrUpsertOperation;
import org.hibernate.sql.model.jdbc.OptionalTableUpdateOperation;
import org.hibernate.sql.model.jdbc.UpsertOperation;

import org.jboss.logging.Logger;

import static java.lang.invoke.MethodHandles.lookup;
import static org.hibernate.reactive.logging.impl.LoggerFactory.make;
import static org.hibernate.reactive.util.impl.CompletionStages.voidFuture;

public class ReactiveDeleteOrUpsertOperation extends DeleteOrUpsertOperation implements ReactiveSelfExecutingUpdateOperation {
	private static final Log LOG = make( Log.class, lookup() );
	private final OptionalTableUpdate upsert;
	private UpsertOperation upsertOperation;
	private UpsertMutationInfo upsertMutationInfo;
	public ReactiveDeleteOrUpsertOperation(
			EntityMutationTarget mutationTarget,
			EntityTableMapping tableMapping,
			UpsertOperation upsertOperation,
			OptionalTableUpdate optionalTableUpdate) {
		super( mutationTarget, tableMapping, upsertOperation, optionalTableUpdate );
		this.upsert = optionalTableUpdate;
		this.upsertOperation = upsertOperation;
		this.upsertMutationInfo = new UpsertMutationInfo( this.upsert );
	}

	@Override
	public void performMutation(
			JdbcValueBindings jdbcValueBindings,
			ValuesAnalysis valuesAnalysis,
			SharedSessionContractImplementor session) {
		throw LOG.nonReactiveMethodCall( "performReactiveMutation" );
	}

	@Override
	public CompletionStage<Void> performReactiveMutation(
			JdbcValueBindings jdbcValueBindings,
			ValuesAnalysis incomingValuesAnalysis,
			SharedSessionContractImplementor session) {
		final UpdateValuesAnalysis valuesAnalysis = (UpdateValuesAnalysis) incomingValuesAnalysis;
		if ( !valuesAnalysis.getTablesNeedingUpdate().contains( getTableDetails() ) ) {
			return voidFuture();
		}

		return doReactiveMutation( getTableDetails(), jdbcValueBindings, valuesAnalysis, session );
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

		return voidFuture()
				.thenCompose( v -> {
					if ( !valuesAnalysis.getTablesWithNonNullValues().contains( tableMapping ) ) {
						return performReactiveDelete( jdbcValueBindings, session );
					}
					else {
						return performReactiveUpsert( jdbcValueBindings,session );
					}
				} )
				.whenComplete( (o, throwable) -> jdbcValueBindings.afterStatement( tableMapping ) );
	}

	private CompletionStage<Void> performReactiveUpsert(JdbcValueBindings jdbcValueBindings, SharedSessionContractImplementor session) {
		final PreparedStatementGroupSingleTable statementGroup = new PreparedStatementGroupSingleTable( upsertOperation, session );
		final PreparedStatementDetails statementDetails = statementGroup.resolvePreparedStatementDetails( getTableDetails().getTableName() );
		upsertMutationInfo.setStatementDetails( statementDetails );

		// If we get here the statement is needed - make sure it is resolved
		Object[] params = PreparedStatementAdaptor.bind( statement -> {
			PreparedStatementDetails details = new PrepareStatementDetailsAdaptor(
					statementDetails,
					statement,
					session.getJdbcServices()
			);
			jdbcValueBindings.beforeStatement( details );
		} );

		ReactiveConnection reactiveConnection = ( (ReactiveConnectionSupplier) session ).getReactiveConnection();
		String sqlString = statementDetails.getSqlString();
		return reactiveConnection
				.update( sqlString, params ).thenCompose(this::checkUpsertResults);
	}

	private CompletionStage<Void> checkUpsertResults( Integer rowCount ) {
		if ( rowCount > 0 ) {
			try {
				upsert.getExpectation()
						.verifyOutcome(
								rowCount,
								upsertMutationInfo.getStatementDetails().getStatement(),
								-1,
								upsertMutationInfo.getStatementDetails().getSqlString()
						);
				return voidFuture();
			}
			catch (SQLException e) {
				LOG.log( Logger.Level.ERROR, e );
			}
		}
		return voidFuture();
	}

	private CompletionStage<Void> performReactiveDelete(JdbcValueBindings jdbcValueBindings, SharedSessionContractImplementor session) {
		throw LOG.notYetImplemented();
	}

	class UpsertMutationInfo {
		OptionalTableUpdate upsert;

		PreparedStatementDetails statementDetails;

		public UpsertMutationInfo(OptionalTableUpdate upsert) {
			this.upsert = upsert;
		}

		public OptionalTableUpdate getUpsert() {
			return upsert;
		}

		public void setStatementDetails(PreparedStatementDetails statementDetails) {
			this.statementDetails = statementDetails;
		}

		public PreparedStatementDetails getStatementDetails() {
			return statementDetails;
		}
	}
}
