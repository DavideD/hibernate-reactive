/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.dialect;

import org.hibernate.dialect.SqlAstTranslatorWithUpsert;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.mutation.EntityTableMapping;
import org.hibernate.reactive.sql.model.ReactiveDeleteOrUpsertOperation;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.internal.OptionalTableUpdate;
import org.hibernate.sql.model.jdbc.UpsertOperation;

public class ReactiveSqlAstTranslatorWithUpsert extends SqlAstTranslatorWithUpsert {
	public ReactiveSqlAstTranslatorWithUpsert(
			SessionFactoryImplementor sessionFactory,
			Statement statement) {
		super( sessionFactory, statement );
	}

	@Override
	public MutationOperation createMergeOperation(OptionalTableUpdate optionalTableUpdate) {
		renderUpsertStatement( optionalTableUpdate );

		final UpsertOperation upsertOperation = new UpsertOperation(
				optionalTableUpdate.getMutatingTable().getTableMapping(),
				optionalTableUpdate.getMutationTarget(),
				getSql(),
				getParameterBinders()
		);

		return new ReactiveDeleteOrUpsertOperation(
				optionalTableUpdate.getMutationTarget(),
				(EntityTableMapping) optionalTableUpdate.getMutatingTable().getTableMapping(),
				upsertOperation,
				optionalTableUpdate
		);
	}

	public MutationOperation createReactiveMergeOperation(OptionalTableUpdate optionalTableUpdate) {
		renderUpsertStatement( optionalTableUpdate );

		final UpsertOperation upsertOperation = new UpsertOperation(
				optionalTableUpdate.getMutatingTable().getTableMapping(),
				optionalTableUpdate.getMutationTarget(),
				getSql(),
				getParameterBinders()
		);

		return new ReactiveDeleteOrUpsertOperation(
				optionalTableUpdate.getMutationTarget(),
				(EntityTableMapping) optionalTableUpdate.getMutatingTable().getTableMapping(),
				upsertOperation,
				optionalTableUpdate
		);
	}

	private void renderUpsertStatement(OptionalTableUpdate optionalTableUpdate) {
		// template:
		//
		// merge into [table] as t
		// using values([bindings]) as s ([column-names])
		// on t.[key] = s.[key]
		// when not matched
		// 		then insert ...
		// when matched
		//		then update ...

		renderMergeInto( optionalTableUpdate );
		appendSql( " " );
		renderMergeUsing( optionalTableUpdate );
		appendSql( " " );
		renderMergeOn( optionalTableUpdate );
		appendSql( " " );
		renderMergeInsert( optionalTableUpdate );
		appendSql( " " );
		renderMergeUpdate( optionalTableUpdate );
	}
}
