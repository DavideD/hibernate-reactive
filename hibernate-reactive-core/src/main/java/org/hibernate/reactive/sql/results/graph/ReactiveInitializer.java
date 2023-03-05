/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.sql.results.graph;

import java.util.concurrent.CompletionStage;

import org.hibernate.Incubating;
import org.hibernate.reactive.sql.exec.spi.ReactiveRowProcessingState;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

/**
 * @see org.hibernate.sql.results.graph.Initializer
 */
@Incubating
public interface ReactiveInitializer {

	CompletionStage<Void> reactiveResolveKey(RowProcessingState rowProcessingState);

	CompletionStage<Void> reactiveResolveInstance(ReactiveRowProcessingState rowProcessingState);

	CompletionStage<Void> reactiveInitializeInstance(ReactiveRowProcessingState rowProcessingState);

}
