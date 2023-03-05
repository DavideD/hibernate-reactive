/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.metamodel.mapping.internal;

import org.hibernate.mapping.Component;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.metamodel.mapping.internal.IdClassEmbeddable;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.metamodel.mapping.internal.VirtualIdEmbeddable;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;

public class ReactiveIdClassEmbeddable extends IdClassEmbeddable {
	public ReactiveIdClassEmbeddable(
			Component idClassSource,
			RootClass bootEntityDescriptor,
			NonAggregatedIdentifierMapping idMapping,
			EntityMappingType identifiedEntityMapping,
			String idTable,
			String[] idColumns,
			VirtualIdEmbeddable virtualIdEmbeddable,
			MappingModelCreationProcess creationProcess) {
		super(
				idClassSource,
				bootEntityDescriptor,
				idMapping,
				identifiedEntityMapping,
				idTable,
				idColumns,
				virtualIdEmbeddable,
				creationProcess
		);
	}

	public ReactiveIdClassEmbeddable(
			EmbeddedAttributeMapping valueMapping,
			TableGroupProducer declaringTableGroupProducer,
			SelectableMappings selectableMappings,
			IdClassEmbeddable inverseMappingType,
			MappingModelCreationProcess creationProcess) {
		super( valueMapping, declaringTableGroupProducer, selectableMappings, inverseMappingType, creationProcess );
	}
}
