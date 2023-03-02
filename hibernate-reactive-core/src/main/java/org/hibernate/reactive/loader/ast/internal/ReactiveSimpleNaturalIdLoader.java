/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.loader.ast.internal;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.internal.AbstractNaturalIdLoader;
import org.hibernate.loader.ast.spi.NaturalIdLoadOptions;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.reactive.util.impl.CompletionStages;

import java.util.concurrent.CompletionStage;

public class ReactiveSimpleNaturalIdLoader implements ReactiveAbstractNaturalIdLoader {
    public ReactiveSimpleNaturalIdLoader(){}

    @Override
    public AbstractNaturalIdLoader delegate() {
        return ReactiveAbstractNaturalIdLoader.super.delegate();
    }

    @Override
    public CompletionStage reactiveLoad(Object naturalIdToLoad, NaturalIdLoadOptions options, SharedSessionContractImplementor session) {
        return null;
    }

    @Override
    public CompletionStage<Object> reactiveResolveNaturalIdToId(Object naturalIdValue, SharedSessionContractImplementor session) {
        return CompletionStages.nullFuture(); //completedFuture(new UnsupportedOperationException("not yet implemented"));
    }

    @Override
    public CompletionStage<Object> reactiveResolveIdToNaturalId(Object id, SharedSessionContractImplementor session) {
        return null;
    }

    @Override
    public EntityMappingType getLoadable() {
        return null;
    }
}
