/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.persister.entity.impl;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.AttributeMappingsList;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.SoftDeleteMapping;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.DeleteCoordinator;
import org.hibernate.reactive.persister.entity.mutation.ReactiveDeleteCoordinatorStandard;
import org.hibernate.reactive.persister.entity.mutation.ReactiveDeleteCoordinatorSoft;
import org.hibernate.reactive.persister.entity.mutation.ReactiveInsertCoordinatorStandard;
import org.hibernate.reactive.persister.entity.mutation.ReactiveUpdateCoordinator;
import org.hibernate.reactive.persister.entity.mutation.ReactiveUpdateCoordinatorNoOp;
import org.hibernate.reactive.persister.state.internal.RactiveStandardStateManagemt;

public final class ReactiveCoordinatorFactory {

	public static ReactiveInsertCoordinatorStandard buildInsertCoordinator(EntityPersister entityPersister) {
		return RactiveStandardStateManagemt.INSTANCE.createInsertCoordinator( entityPersister );
	}

	public static ReactiveUpdateCoordinator buildUpdateCoordinator(EntityPersister entityPersister) {
		return RactiveStandardStateManagemt.INSTANCE.createUpdateCoordinator( entityPersister );
	}

	public static DeleteCoordinator buildDeleteCoordinator(
			SoftDeleteMapping softDeleteMapping,
			AbstractEntityPersister entityPersister,
			SessionFactoryImplementor factory) {
		return softDeleteMapping != null
				? new ReactiveDeleteCoordinatorSoft( entityPersister, factory )
				: new ReactiveDeleteCoordinatorStandard( entityPersister, factory );
	}

	public static ReactiveUpdateCoordinator buildMergeCoordinator(
			AbstractEntityPersister entityPersister,
			SessionFactoryImplementor factory) {
		// we only have updates to issue for entities with one or more singular attributes
		final AttributeMappingsList attributeMappings = entityPersister.getAttributeMappings();
		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			AttributeMapping attributeMapping = attributeMappings.get( i );
			if ( attributeMapping instanceof SingularAttributeMapping ) {
				return new ReactiveMergeCoordinatorStandardScopeFactory( entityPersister, factory );
			}
		}

		// otherwise, nothing to update
		return new ReactiveUpdateCoordinatorNoOp( entityPersister );
	}
}
