/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.sql.results.graph.embeddable.internal;

import org.hibernate.reactive.sql.results.graph.embeddable.ReactiveAbstractEmbeddableInitializer;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.embeddable.EmbeddableResultGraphNode;

/**
 * @see org.hibernate.sql.results.graph.embeddable.internal.EmbeddableFetchInitializer
 */
public class ReactiveEmbeddableFetchInitializer extends ReactiveAbstractEmbeddableInitializer {

	public ReactiveEmbeddableFetchInitializer(
			FetchParentAccess fetchParentAccess,
			EmbeddableResultGraphNode resultDescriptor,
			AssemblerCreationState creationState) {
		super( resultDescriptor, fetchParentAccess, creationState );
	}

	@Override
	public Object getParentKey() {
		return findFirstEntityDescriptorAccess().getParentKey();
	}
}
