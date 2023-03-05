/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.metamodel.mapping.internal;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.mapping.CompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @see org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper
 */
public class ReactiveMappingModelCreationHelper {

	public static CompositeIdentifierMapping buildNonEncapsulatedCompositeIdentifierMapping(
			EntityPersister entityPersister,
			String rootTableName,
			String[] rootTableKeyColumnNames,
			PersistentClass bootEntityDescriptor,
			MappingModelCreationProcess creationProcess) {
		return new ReactiveNonAggregatedIdentifierMappingImpl(
				entityPersister,
				bootEntityDescriptor.getRootClass(),
				rootTableName,
				rootTableKeyColumnNames,
				creationProcess
		);
	}
}
