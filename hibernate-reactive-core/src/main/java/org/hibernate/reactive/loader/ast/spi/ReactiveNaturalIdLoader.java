/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.loader.ast.spi;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.NaturalIdLoadOptions;
import org.hibernate.loader.ast.spi.NaturalIdLoader;

import java.util.concurrent.CompletionStage;

public interface ReactiveNaturalIdLoader<T> extends NaturalIdLoader<CompletionStage<T>> {

    /**
     * @deprecated use {@link #reactiveLoad(Object, NaturalIdLoadOptions, SharedSessionContractImplementor)}
     */
    @Deprecated
    @Override
    default CompletionStage<T> load(Object naturalIdToLoad, NaturalIdLoadOptions options, SharedSessionContractImplementor session) {
        throw new UnsupportedOperationException("Use the reactive method: reactiveLoad(Object, NaturalIdLoadOptions, SharedSessionContractImplementor)");
    }

    CompletionStage<T> reactiveLoad(Object naturalIdToLoad, NaturalIdLoadOptions options, SharedSessionContractImplementor session);

    /**
     * @deprecated use {@link #reactiveResolveNaturalIdToId(Object, SharedSessionContractImplementor)}
     */
    @Deprecated
    @Override
    default Object resolveNaturalIdToId(Object naturalIdValue, SharedSessionContractImplementor session) {
        throw new UnsupportedOperationException("Use the reactive method: reactiveResolveNaturalIdToId(Object, NaturalIdLoadOptions, SharedSessionContractImplementor)");
    }

    CompletionStage<Object> reactiveResolveNaturalIdToId(Object naturalIdValue, SharedSessionContractImplementor session);

    /**
     * @deprecated use {@link #reactiveResolveIdToNaturalId(Object, SharedSessionContractImplementor)}
     */
    default Object resolveIdToNaturalId(Object id, SharedSessionContractImplementor session) {
        throw new UnsupportedOperationException("Use the reactive method: reactiveResolveIdToNaturalId(Object, NaturalIdLoadOptions, SharedSessionContractImplementor)");
    }


    CompletionStage<Object> reactiveResolveIdToNaturalId(Object id, SharedSessionContractImplementor session);

}
