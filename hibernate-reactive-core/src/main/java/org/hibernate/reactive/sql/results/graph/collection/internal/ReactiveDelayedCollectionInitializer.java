/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.sql.results.graph.collection.internal;

import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.collection.internal.DelayedCollectionInitializer;

public class ReactiveDelayedCollectionInitializer extends DelayedCollectionInitializer {
	public ReactiveDelayedCollectionInitializer(
			NavigablePath fetchedPath,
			PluralAttributeMapping fetchedMapping,
			InitializerParent<?> parent,
			DomainResult<?> collectionKeyResult,
			AssemblerCreationState creationState) {
		super( fetchedPath, fetchedMapping, parent, collectionKeyResult, creationState );
	}
}
