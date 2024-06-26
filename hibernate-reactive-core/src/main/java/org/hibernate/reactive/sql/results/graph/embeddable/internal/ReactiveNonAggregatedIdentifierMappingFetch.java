/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.sql.results.graph.embeddable.internal;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.embeddable.EmbeddableInitializer;
import org.hibernate.sql.results.graph.embeddable.internal.NonAggregatedIdentifierMappingFetch;

public class ReactiveNonAggregatedIdentifierMappingFetch extends NonAggregatedIdentifierMappingFetch {
	public ReactiveNonAggregatedIdentifierMappingFetch(
			NavigablePath navigablePath,
			NonAggregatedIdentifierMapping embeddedPartDescriptor,
			FetchParent fetchParent,
			FetchTiming fetchTiming,
			boolean hasTableGroup,
			DomainResultCreationState creationState) {
		super( navigablePath, embeddedPartDescriptor, fetchParent, fetchTiming, hasTableGroup, creationState );
	}

	public ReactiveNonAggregatedIdentifierMappingFetch(NonAggregatedIdentifierMappingFetch fetch) {
		super( fetch );
	}

	@Override
	public EmbeddableInitializer<?> createInitializer(
			InitializerParent<?> parent,
			AssemblerCreationState creationState) {
		return new ReactiveNonAggregatedIdentifierMappingInitializer( this, parent, creationState, false );
	}
}
