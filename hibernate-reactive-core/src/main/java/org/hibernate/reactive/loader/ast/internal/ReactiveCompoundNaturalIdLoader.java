/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.loader.ast.internal;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.internal.CompoundNaturalIdLoader;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.internal.CompoundNaturalIdMapping;

public class ReactiveCompoundNaturalIdLoader<T> extends CompoundNaturalIdLoader {
    public ReactiveCompoundNaturalIdLoader(CompoundNaturalIdMapping naturalIdMapping, EntityMappingType entityDescriptor) {
        super(naturalIdMapping, entityDescriptor);
    }

    @Override
    public Object resolveNaturalIdToId(Object id, SharedSessionContractImplementor session) {
        return super.resolveNaturalIdToId(id, session);
    }
}
