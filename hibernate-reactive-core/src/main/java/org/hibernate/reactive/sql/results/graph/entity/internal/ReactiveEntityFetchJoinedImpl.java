/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.sql.results.graph.entity.internal;

import org.hibernate.engine.FetchTiming;
import org.hibernate.reactive.sql.results.graph.embeddable.internal.ReactiveNonAggregatedIdentifierMappingFetch;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.embeddable.internal.NonAggregatedIdentifierMappingFetch;
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

	@Override
	public Fetch generateFetchableFetch(
			Fetchable fetchable,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		Fetch fetch = super.generateFetchableFetch(
				fetchable,
				fetchablePath,
				fetchTiming,
				selected,
				resultVariable,
				creationState
		);
		if ( fetch instanceof NonAggregatedIdentifierMappingFetch ) {
			return new ReactiveNonAggregatedIdentifierMappingFetch( (NonAggregatedIdentifierMappingFetch) fetch );
		}
		return fetch;
	}
}
