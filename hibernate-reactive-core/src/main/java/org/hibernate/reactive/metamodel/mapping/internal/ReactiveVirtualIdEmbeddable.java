/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.metamodel.mapping.internal;

import org.hibernate.mapping.Component;
import org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.metamodel.mapping.internal.VirtualIdEmbeddable;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;

public class ReactiveVirtualIdEmbeddable extends VirtualIdEmbeddable {
	public ReactiveVirtualIdEmbeddable(
			Component virtualIdSource,
			NonAggregatedIdentifierMapping idMapping,
			EntityPersister entityPersister,
			String rootTableExpression,
			String[] rootTableKeyColumnNames,
			MappingModelCreationProcess creationProcess) {
		super(
				virtualIdSource,
				idMapping,
				entityPersister,
				rootTableExpression,
				rootTableKeyColumnNames,
				creationProcess
		);
	}

	public ReactiveVirtualIdEmbeddable(
			EmbeddedAttributeMapping valueMapping,
			TableGroupProducer declaringTableGroupProducer,
			SelectableMappings selectableMappings,
			VirtualIdEmbeddable inverseMappingType,
			MappingModelCreationProcess creationProcess) {
		super( valueMapping, declaringTableGroupProducer, selectableMappings, inverseMappingType, creationProcess );
	}
}
