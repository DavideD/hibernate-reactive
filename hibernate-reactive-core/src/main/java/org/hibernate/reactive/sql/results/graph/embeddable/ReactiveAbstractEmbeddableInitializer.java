/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.sql.results.graph.embeddable;


import java.util.concurrent.CompletionStage;

import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.reactive.sql.exec.spi.ReactiveRowProcessingState;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.embeddable.AbstractEmbeddableInitializer;
import org.hibernate.sql.results.graph.embeddable.EmbeddableResultGraphNode;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import static org.hibernate.reactive.util.impl.CompletionStages.completedFuture;
import static org.hibernate.reactive.util.impl.CompletionStages.voidFuture;

/**
 * @see org.hibernate.sql.results.graph.embeddable.AbstractEmbeddableInitializer
 */
public abstract class ReactiveAbstractEmbeddableInitializer extends AbstractEmbeddableInitializer
		implements ReactiveEmbeddableInitializer {
	public ReactiveAbstractEmbeddableInitializer(
			EmbeddableResultGraphNode resultDescriptor,
			FetchParentAccess fetchParentAccess,
			AssemblerCreationState creationState) {
		super( resultDescriptor, fetchParentAccess, creationState );
	}

	/**
	 * @see AbstractEmbeddableInitializer#resolveKey(RowProcessingState)
	 */
	@Override
	public CompletionStage<Void> reactiveResolveKey(RowProcessingState rowProcessingState) {
		// Nothing to do
		return voidFuture();
	}

	/**
	 * @see AbstractEmbeddableInitializer#resolveInstance(RowProcessingState)
	 */
	@Override
	public CompletionStage<Void> reactiveResolveInstance(ReactiveRowProcessingState rowProcessingState) {
		// Nothing to do
		return voidFuture();
	}

	/**
	 * @see AbstractEmbeddableInitializer#initializeInstance(RowProcessingState)
	 */
	@Override
	public CompletionStage<Void> reactiveInitializeInstance(ReactiveRowProcessingState rowProcessingState) {
		super.initializeInstance( rowProcessingState );
		return voidFuture();
	}

	@Override
	public Object reactiveGetCompositeInstance() {
		return completedFuture( getCompositeInstance() );
	}

	@Override
	protected void initializeAssemblers(
			EmbeddableResultGraphNode resultDescriptor,
			AssemblerCreationState creationState,
			EmbeddableMappingType embeddableTypeDescriptor) {
		super.initializeAssemblers( resultDescriptor, creationState, embeddableTypeDescriptor );
	}
}
