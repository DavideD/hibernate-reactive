/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.sql.results.graph.entity.internal;

import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.graph.entity.internal.EntityAssembler;
import org.hibernate.sql.results.graph.entity.internal.EntityFetchJoinedImpl;

public class ReactiveEntityFetchJoinedImpl extends EntityFetchJoinedImpl {
	public ReactiveEntityFetchJoinedImpl(EntityFetchJoinedImpl entityFetch) {
		super( entityFetch );
	}

	@Override
	public EntityInitializer<?> createInitializer(InitializerParent<?> parent, AssemblerCreationState creationState) {
		return new ReactiveEntityInitializerImpl(
				this,
				getSourceAlias(),
				getEntityResult().getIdentifierFetch(),
				getEntityResult().getDiscriminatorFetch(),
				getKeyResult(),
				getEntityResult().getRowIdResult(),
				getNotFoundAction(),
				isAffectedByFilter(),
				parent,
				false,
				creationState
		);
	}

	@Override
	protected EntityAssembler buildEntityAssembler(EntityInitializer<?> entityInitializer) {
		return new ReactiveEntityAssembler( getFetchedMapping().getJavaType(), entityInitializer );
	}
}
