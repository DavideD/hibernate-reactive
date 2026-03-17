/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.persister.state.internal;

import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.state.spi.StateManagement;
import org.hibernate.reactive.persister.entity.impl.ReactiveMergeCoordinatorStandardScopeFactory;
import org.hibernate.reactive.persister.entity.impl.ReactiveUpdateCoordinatorStandardScopeFactory;
import org.hibernate.reactive.persister.entity.mutation.ReactiveInsertCoordinatorStandard;
import org.hibernate.reactive.persister.entity.mutation.ReactiveUpdateCoordinator;
import org.hibernate.reactive.persister.entity.mutation.ReactiveUpdateCoordinatorNoOp;

/**
 * @see org.hibernate.persister.state.internal.AbstractStateManagement
 */
public abstract class ReactiveAbstractStateManagement implements StateManagement {

	@Override
	public ReactiveInsertCoordinatorStandard createInsertCoordinator(EntityPersister persister) {
		return new ReactiveInsertCoordinatorStandard( persister, persister.getFactory() );
	}

	@Override
	public ReactiveUpdateCoordinator createUpdateCoordinator(EntityPersister persister) {
		final var attributeMappings = persister.getAttributeMappings();
		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			if ( attributeMappings.get( i ) instanceof SingularAttributeMapping ) {
				return new ReactiveUpdateCoordinatorStandardScopeFactory( persister, persister.getFactory() );
			}
		}
		return new ReactiveUpdateCoordinatorNoOp( persister );
	}

	@Override
	public ReactiveUpdateCoordinator createMergeCoordinator(EntityPersister persister) {
		return new ReactiveMergeCoordinatorStandardScopeFactory( persister, persister.getFactory() );
	}
}
