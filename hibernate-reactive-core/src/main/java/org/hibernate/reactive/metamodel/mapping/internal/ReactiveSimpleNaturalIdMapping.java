/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.metamodel.mapping.internal;

import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.metamodel.mapping.internal.SimpleNaturalIdMapping;

public class ReactiveSimpleNaturalIdMapping extends SimpleNaturalIdMapping {
    public ReactiveSimpleNaturalIdMapping(SingularAttributeMapping attribute, EntityMappingType declaringType, MappingModelCreationProcess creationProcess) {
        super(attribute, declaringType, creationProcess);
    }
}
