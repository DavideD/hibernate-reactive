/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.sql.results.graph.embeddable;

import org.hibernate.reactive.sql.results.graph.ReactiveInitializer;
import org.hibernate.sql.results.graph.embeddable.EmbeddableInitializer;

/**
 * @see org.hibernate.sql.results.graph.embeddable.EmbeddableInitializer
 */
public interface ReactiveEmbeddableInitializer extends EmbeddableInitializer, ReactiveInitializer {

	Object reactiveGetCompositeInstance();
}
