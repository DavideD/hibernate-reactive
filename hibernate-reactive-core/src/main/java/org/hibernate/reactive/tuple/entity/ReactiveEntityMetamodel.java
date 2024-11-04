/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.tuple.entity;

import java.util.function.Function;

import org.hibernate.generator.Generator;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.entity.EntityMetamodel;

public class ReactiveEntityMetamodel extends EntityMetamodel {
	public ReactiveEntityMetamodel(
			PersistentClass persistentClass,
			EntityPersister persister,
			RuntimeModelCreationContext creationContext) {
		super( persistentClass, persister, creationContext );
	}

	public ReactiveEntityMetamodel(
			PersistentClass persistentClass,
			EntityPersister persister,
			RuntimeModelCreationContext creationContext,
			Function<String, Generator> generatorSupplier) {
		super( persistentClass, persister, creationContext, generatorSupplier );
	}
}
