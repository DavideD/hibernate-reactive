/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.metamodel.mapping.internal;

import org.hibernate.engine.FetchTiming;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping;
import org.hibernate.metamodel.mapping.internal.IdClassEmbeddable;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.metamodel.mapping.internal.NonAggregatedIdentifierMappingImpl;
import org.hibernate.metamodel.mapping.internal.VirtualIdEmbeddable;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.reactive.sql.results.graph.embeddable.internal.ReactiveEmbeddableFetchImpl;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;

public class ReactiveNonAggregatedIdentifierMappingImpl extends NonAggregatedIdentifierMappingImpl {

	private static ReactiveEmbeddableFactory EMBEDDABLE_FACTORY = new ReactiveEmbeddableFactory();

	private static class ReactiveEmbeddableFactory implements EmbeddableFactory {

		@Override
		public VirtualIdEmbeddable buildVirtualIdEmbeddable(
				EntityPersister entityPersister,
				NonAggregatedIdentifierMapping idMapping,
				String rootTableName,
				String[] rootTableKeyColumnNames,
				MappingModelCreationProcess creationProcess,
				Component virtualIdSource) {
			return new ReactiveVirtualIdEmbeddable(
					virtualIdSource,
					idMapping,
					entityPersister,
					rootTableName,
					rootTableKeyColumnNames,
					creationProcess);
		}

		@Override
		public IdClassEmbeddable buildIdClassEmbeddable(
				VirtualIdEmbeddable virtualIdEmbeddable,
				EntityPersister entityPersister,
				NonAggregatedIdentifierMapping idMapping,
				RootClass bootEntityDescriptor,
				String rootTableName,
				String[] rootTableKeyColumnNames,
				MappingModelCreationProcess creationProcess,
				Component idClassSource) {
			return new ReactiveIdClassEmbeddable(
					idClassSource,
					bootEntityDescriptor,
					idMapping,
					entityPersister,
					rootTableName,
					rootTableKeyColumnNames,
					virtualIdEmbeddable,
					creationProcess
			);
		}
	}

	public ReactiveNonAggregatedIdentifierMappingImpl(
			EntityPersister entityPersister,
			RootClass bootEntityDescriptor,
			String rootTableName,
			String[] rootTableKeyColumnNames,
			MappingModelCreationProcess creationProcess) {
		super(
				entityPersister,
				bootEntityDescriptor,
				rootTableName,
				rootTableKeyColumnNames,
				creationProcess,
				EMBEDDABLE_FACTORY
		);
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		return new ReactiveEmbeddableFetchImpl(
				fetchablePath,
				this,
				fetchParent,
				fetchTiming,
				selected,
				creationState
		);
	}
}
