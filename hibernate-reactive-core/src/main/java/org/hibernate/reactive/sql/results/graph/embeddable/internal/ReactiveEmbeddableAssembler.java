/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.sql.results.graph.embeddable.internal;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletionStage;

import org.hibernate.reactive.logging.impl.Log;
import org.hibernate.reactive.logging.impl.LoggerFactory;
import org.hibernate.reactive.sql.exec.spi.ReactiveRowProcessingState;
import org.hibernate.reactive.sql.results.graph.ReactiveDomainResultsAssembler;
import org.hibernate.reactive.sql.results.graph.embeddable.ReactiveEmbeddableInitializer;
import org.hibernate.sql.results.graph.embeddable.EmbeddableInitializer;
import org.hibernate.sql.results.graph.embeddable.internal.EmbeddableAssembler;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;

/**
 * @see org.hibernate.sql.results.graph.embeddable.internal.EmbeddableAssembler
 */
public class ReactiveEmbeddableAssembler extends EmbeddableAssembler implements ReactiveDomainResultsAssembler {
	private static Log LOG = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final ReactiveEmbeddableInitializer initializer;

	public ReactiveEmbeddableAssembler(EmbeddableInitializer initializer) {
		super( initializer );
		this.initializer = (ReactiveEmbeddableInitializer) initializer;
	}

	@Override
	public CompletionStage<Object> reactiveAssemble(
			ReactiveRowProcessingState rowProcessingState,
			JdbcValuesSourceProcessingOptions options) {
		return initializer
				.reactiveResolveKey( rowProcessingState )
				.thenCompose( v -> initializer.reactiveResolveInstance( rowProcessingState ) )
				.thenCompose( v -> initializer.reactiveInitializeInstance( rowProcessingState ) )
				.thenApply( v -> initializer.reactiveGetCompositeInstance() );
	}
}
