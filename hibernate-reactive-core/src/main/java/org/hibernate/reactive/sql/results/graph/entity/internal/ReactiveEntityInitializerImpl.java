/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.sql.results.graph.entity.internal;

import org.hibernate.annotations.NotFoundAction;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.entity.EntityResultGraphNode;
import org.hibernate.sql.results.graph.entity.internal.EntityInitializerImpl;

public class ReactiveEntityInitializerImpl extends EntityInitializerImpl {

	public ReactiveEntityInitializerImpl(
			EntityResultGraphNode resultDescriptor,
			String sourceAlias,
			Fetch identifierFetch,
			Fetch discriminatorFetch,
			DomainResult<?> keyResult,
			DomainResult<Object> rowIdResult,
			NotFoundAction notFoundAction,
			boolean affectedByFilter,
			InitializerParent<?> parent,
			boolean isResultInitializer,
			AssemblerCreationState creationState) {
		super(
				resultDescriptor,
				sourceAlias,
				identifierFetch,
				discriminatorFetch,
				keyResult,
				rowIdResult,
				notFoundAction,
				affectedByFilter,
				parent,
				isResultInitializer,
				creationState
		);
	}
}
