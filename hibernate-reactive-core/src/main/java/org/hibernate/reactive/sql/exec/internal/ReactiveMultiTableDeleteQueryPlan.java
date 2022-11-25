/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.sql.exec.internal;

import java.util.concurrent.CompletionStage;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.reactive.query.sql.spi.ReactiveNonSelectQueryPlan;

/**
 * @see org.hibernate.query.sqm.internal.MultiTableDeleteQueryPlan
 */
public class ReactiveMultiTableDeleteQueryPlan implements ReactiveNonSelectQueryPlan {

	private final SqmDeleteStatement sqmDelete;
	private final DomainParameterXref domainParameterXref;
	private final SqmMultiTableMutationStrategy deleteStrategy;

	public ReactiveMultiTableDeleteQueryPlan(SqmDeleteStatement sqmDelete, DomainParameterXref domainParameterXref, SqmMultiTableMutationStrategy deleteStrategy) {
		this.sqmDelete = sqmDelete;
		this.domainParameterXref = domainParameterXref;
		this.deleteStrategy = deleteStrategy;
	}

	@Override
	public CompletionStage<Integer> executeReactiveUpdate(DomainQueryExecutionContext executionContext) {
//		BulkOperationCleanupAction.schedule( executionContext.getSession(), sqmDelete );
//		return deleteStrategy.executeDelete( sqmDelete, domainParameterXref, executionContext );
		throw new NotYetImplementedFor6Exception();
	}
}
