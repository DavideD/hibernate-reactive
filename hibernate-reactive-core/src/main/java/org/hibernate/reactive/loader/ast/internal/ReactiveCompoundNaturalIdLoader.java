/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.loader.ast.internal;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.internal.CompoundNaturalIdLoader;
import org.hibernate.loader.ast.internal.LoaderSqlAstCreationState;
import org.hibernate.loader.ast.spi.NaturalIdLoadOptions;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.internal.CompoundNaturalIdMapping;
import org.hibernate.reactive.loader.ast.spi.ReactiveNaturalIdLoader;
import org.hibernate.reactive.logging.impl.Log;
import org.hibernate.reactive.logging.impl.LoggerFactory;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.*;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ReactiveCompoundNaturalIdLoader<T> extends CompoundNaturalIdLoader implements ReactiveNaturalIdLoader {

    private static final Log LOG = LoggerFactory.make( Log.class, MethodHandles.lookup() );

    private final ReactiveNaturalIdLoaderDelegate<T> delegate;

    public ReactiveCompoundNaturalIdLoader(CompoundNaturalIdMapping naturalIdMapping, EntityMappingType entityDescriptor) {
        super(naturalIdMapping, entityDescriptor);
        delegate = new ReactiveNaturalIdLoaderDelegate<>( naturalIdMapping, entityDescriptor ) {
            @Override
            protected void applyNaturalIdRestriction(
                    Object bindValue,
                    TableGroup rootTableGroup,
                    Consumer consumer,
                    BiConsumer jdbcParameterConsumer,
                    LoaderSqlAstCreationState sqlAstCreationState,
                    SharedSessionContractImplementor session) {
                ReactiveCompoundNaturalIdLoader.this.applyNaturalIdRestriction(
                        bindValue,
                        rootTableGroup,
                        consumer,
                        jdbcParameterConsumer,
                        sqlAstCreationState,
                        session
                );
            }
        };
    }

	@Override
	public Object resolveNaturalIdToId(Object naturalIdValue, SharedSessionContractImplementor session) {
		throw LOG.nonReactiveMethodCall( "reactiveResolveNaturalIdToId" );
    }

    @Override
    public CompletionStage<Object> reactiveResolveNaturalIdToId(Object naturalIdValue, SharedSessionContractImplementor session) {
        return delegate.reactiveResolveNaturalIdToId( naturalIdValue, session );
    }

    @Override
    public CompletionStage<Object> reactiveResolveIdToNaturalId(Object id, SharedSessionContractImplementor session) {
		return delegate.reactiveResolveIdToNaturalId( id, session );
    }

    @Override
    public CompletionStage<T> load(Object naturalIdValue, NaturalIdLoadOptions options, SharedSessionContractImplementor session) {
		return delegate.load( naturalIdValue, options, session );
    }
}
