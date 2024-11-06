/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.tuple.entity;

import java.util.Map;
import java.util.function.Function;

import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.generator.Generator;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.SelectGenerator;
import org.hibernate.id.enhanced.DatabaseStructure;
import org.hibernate.id.enhanced.SequenceStructure;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.id.enhanced.TableStructure;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.reactive.id.ReactiveIdentifierGenerator;
import org.hibernate.reactive.id.impl.EmulatedSequenceReactiveIdentifierGenerator;
import org.hibernate.reactive.id.impl.ReactiveGeneratorWrapper;
import org.hibernate.reactive.id.impl.ReactiveSequenceIdentifierGenerator;
import org.hibernate.reactive.id.impl.TableReactiveIdentifierGenerator;
import org.hibernate.reactive.logging.impl.Log;
import org.hibernate.reactive.provider.Settings;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tuple.entity.EntityMetamodel;

import static java.lang.invoke.MethodHandles.lookup;
import static org.hibernate.reactive.logging.impl.LoggerFactory.make;

public class ReactiveEntityMetamodel extends EntityMetamodel {

	private static final Log LOG = make( Log.class, lookup() );

	public ReactiveEntityMetamodel(
			PersistentClass persistentClass,
			EntityPersister persister,
			RuntimeModelCreationContext creationContext) {
		this( persistentClass, persister, creationContext, s -> buildIdGenerator( s, persistentClass, creationContext ) );
	}

	public ReactiveEntityMetamodel(
			PersistentClass persistentClass,
			EntityPersister persister,
			RuntimeModelCreationContext creationContext,
			Function<String, Generator> generatorSupplier) {
		super( persistentClass, persister, creationContext, generatorSupplier );
	}

	private static Generator buildIdGenerator(String rootName, PersistentClass persistentClass, RuntimeModelCreationContext creationContext) {
		final Generator existing = creationContext.getGenerators().get( rootName );
		if ( existing != null ) {
			return existing;
		}
		else {
			SimpleValue identifier = (SimpleValue) persistentClass.getIdentifier();
			final Generator idgenerator = identifier
					// returns the cached Generator if it was already created
					.createGenerator(
							creationContext.getDialect(),
							persistentClass.getRootClass(),
							persistentClass.getIdentifierProperty(),
							creationContext.getGeneratorSettings()
					);

			creationContext.getGenerators().put( rootName, idgenerator );
			return idgenerator;
		}
	}

	public static Generator augmentWithReactiveGenerator(Generator generator, RuntimeModelCreationContext creationContext) {
		final ReactiveIdentifierGenerator<?> reactiveGenerator;
		if ( generator instanceof SequenceStyleGenerator ) {
			final DatabaseStructure structure = ( (SequenceStyleGenerator) generator ).getDatabaseStructure();
			if ( structure instanceof TableStructure ) {
				reactiveGenerator = new EmulatedSequenceReactiveIdentifierGenerator();
			}
			else if ( structure instanceof SequenceStructure ) {
				reactiveGenerator = new ReactiveSequenceIdentifierGenerator();
			}
			else {
				throw LOG.unknownStructureType();
			}
		}
		else if ( generator instanceof TableGenerator ) {
			reactiveGenerator = new TableReactiveIdentifierGenerator();
		}
		else if ( generator instanceof SelectGenerator ) {
			throw LOG.selectGeneratorIsNotSupportedInHibernateReactive();
		}
		else {
			//nothing to do
			return generator;
		}

		ServiceRegistry serviceRegistry = creationContext.getServiceRegistry();
		//this is not the way ORM does this: instead it passes a
		//SqlStringGenerationContext to IdentifierGenerator.initialize()
		final ConfigurationService cs = serviceRegistry.getService( ConfigurationService.class );
		if ( !params.containsKey( PersistentIdentifierGenerator.SCHEMA ) ) {
			final String schema = cs.getSetting( Settings.DEFAULT_SCHEMA, StandardConverters.STRING );
			if ( schema != null ) {
				params.put( PersistentIdentifierGenerator.SCHEMA, schema );
			}
		}
		if ( !params.containsKey( PersistentIdentifierGenerator.CATALOG ) ) {
			final String catalog = cs.getSetting( Settings.DEFAULT_CATALOG, StandardConverters.STRING );
			if ( catalog != null ) {
				params.put( PersistentIdentifierGenerator.CATALOG, catalog );
			}
		}

		( (Configurable) reactiveGenerator ).configure( creationContext,  );
		return new ReactiveGeneratorWrapper( reactiveGenerator, (IdentifierGenerator) generator );
	}
}
