/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.loader.ast.internal;

import org.hibernate.loader.ast.internal.AbstractNaturalIdLoader;
import org.hibernate.reactive.loader.ast.spi.ReactiveNaturalIdLoader;


public interface ReactiveAbstractNaturalIdLoader extends ReactiveNaturalIdLoader {

    default AbstractNaturalIdLoader delegate() {
        return (AbstractNaturalIdLoader) this;
    }


//    @Override
//    CompletionStage<Object> reactiveResolveNaturalIdToId(Object naturalIdValue, SharedSessionContractImplementor session);
//
//    @Override
//    CompletionStage<Object> reactiveResolveIdToNaturalId(Object id, SharedSessionContractImplementor session);
}
