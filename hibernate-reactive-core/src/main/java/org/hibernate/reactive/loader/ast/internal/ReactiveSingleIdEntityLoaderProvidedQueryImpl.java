/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.loader.ast.internal;

import java.util.concurrent.CompletionStage;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.Query;
import org.hibernate.query.QueryFlushMode;
import org.hibernate.query.named.NamedQueryMemento;
import org.hibernate.reactive.loader.ast.spi.ReactiveSingleIdEntityLoader;
import org.hibernate.reactive.query.ReactiveSelectionQuery;

import jakarta.persistence.Parameter;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.reactive.util.impl.CompletionStages.completedFuture;

/**
 * Implementation of SingleIdEntityLoader for cases where the application has
 * provided the select load query
 */
public class ReactiveSingleIdEntityLoaderProvidedQueryImpl<T> implements ReactiveSingleIdEntityLoader<T> {

	private static final CompletionStage<Object[]> EMPTY_ARRAY_STAGE = completedFuture( ArrayHelper.EMPTY_OBJECT_ARRAY );

	private final EntityMappingType entityDescriptor;
	private final NamedQueryMemento<T> namedQueryMemento;

	public ReactiveSingleIdEntityLoaderProvidedQueryImpl(EntityMappingType entityDescriptor, NamedQueryMemento<T> namedQueryMemento) {
		this.entityDescriptor = entityDescriptor;
		this.namedQueryMemento = namedQueryMemento;
	}

	@Override
	public EntityMappingType getLoadable() {
		return entityDescriptor;
	}

	@Override @SuppressWarnings("unchecked")
	public CompletionStage<T> load(Object pkValue, LockOptions lockOptions, Boolean readOnly, SharedSessionContractImplementor session) {
		final JavaType<T> mappedJavaType = (JavaType<T>) entityDescriptor.getMappedJavaType();
		final Query<T> query = namedQueryMemento.toQuery( session, mappedJavaType.getJavaTypeClass() );
		query.setParameter( (Parameter<Object>) query.getParameters().iterator().next(), pkValue );
		query.setQueryFlushMode( QueryFlushMode.NO_FLUSH );
		return ( (ReactiveSelectionQuery<T>) query ).reactiveUnique();
	}

	@Override
	public CompletionStage<T> load(
			Object pkValue,
			Object entityInstance,
			LockOptions lockOptions,
			Boolean readOnly,
			SharedSessionContractImplementor session) {
		if ( entityInstance != null ) {
			throw new UnsupportedOperationException("null entity instance");
		}
		return load( pkValue, lockOptions, readOnly, session );
	}

	@Override
	public CompletionStage<Object[]> reactiveLoadDatabaseSnapshot(Object id, SharedSessionContractImplementor session) {
		return EMPTY_ARRAY_STAGE;
	}
}
