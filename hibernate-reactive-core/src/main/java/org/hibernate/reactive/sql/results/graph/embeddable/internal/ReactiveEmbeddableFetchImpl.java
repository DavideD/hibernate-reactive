/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.sql.results.graph.embeddable.internal;

import org.hibernate.engine.FetchTiming;
import org.hibernate.reactive.sql.results.graph.entity.internal.ReactiveEntityFetchSelectImpl;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.embeddable.EmbeddableInitializer;
import org.hibernate.sql.results.graph.embeddable.EmbeddableResultGraphNode;
import org.hibernate.sql.results.graph.embeddable.EmbeddableValuedFetchable;
import org.hibernate.sql.results.graph.embeddable.internal.EmbeddableFetchImpl;
import org.hibernate.sql.results.graph.entity.internal.EntityFetchSelectImpl;

public class ReactiveEmbeddableFetchImpl extends EmbeddableFetchImpl {

	public ReactiveEmbeddableFetchImpl(
			NavigablePath navigablePath,
			EmbeddableValuedFetchable embeddedPartDescriptor,
			FetchParent fetchParent,
			FetchTiming fetchTiming,
			boolean hasTableGroup,
			DomainResultCreationState creationState) {
		super( navigablePath, embeddedPartDescriptor, fetchParent, fetchTiming, hasTableGroup, creationState );
	}

	public ReactiveEmbeddableFetchImpl(EmbeddableFetchImpl original) {
		super( original );
	}

	@Override
	protected DomainResultAssembler<?> createEmbeddableAssembler(EmbeddableInitializer initializer) {
		return new ReactiveEmbeddableAssembler( initializer );
	}

	@Override
	protected Initializer buildEmbeddableFetchInitializer(
			FetchParentAccess parentAccess,
			EmbeddableResultGraphNode embeddableFetch,
			AssemblerCreationState creationState) {
		return new ReactiveEmbeddableFetchInitializer( parentAccess, this, creationState );
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
		if ( fetch instanceof EntityFetchSelectImpl ) {
			return new ReactiveEntityFetchSelectImpl( (EntityFetchSelectImpl) fetch );
		}
		return fetch;
	}
}
