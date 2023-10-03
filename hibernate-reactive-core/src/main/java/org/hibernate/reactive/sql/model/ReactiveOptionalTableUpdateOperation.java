/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.sql.model;

import java.util.concurrent.CompletionStage;

import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.reactive.logging.impl.Log;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.ValuesAnalysis;
import org.hibernate.sql.model.internal.OptionalTableUpdate;
import org.hibernate.sql.model.jdbc.OptionalTableUpdateOperation;

import static java.lang.invoke.MethodHandles.lookup;
import static org.hibernate.reactive.logging.impl.LoggerFactory.make;

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

	@Override
	public CompletionStage<Void> performReactiveMutation(
			JdbcValueBindings jdbcValueBindings,
			ValuesAnalysis valuesAnalysis,
			SharedSessionContractImplementor session) {
		// TODO:
		throw  LOG.notYetImplemented();
	}
}
