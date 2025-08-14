/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive;

import java.util.Collection;
import java.util.Set;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.PersistentTableInsertStrategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.ValueSource;

import io.vertx.junit5.VertxTestContext;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Adapt the test class AbstractMutationStrategyGeneratedIdWithOptimizerTest in ORM.
 */
@ParameterizedClass
@ValueSource(classes = { PersistentTableInsertStrategy.class, LocalTemporaryTableInsertStrategy.class, GlobalTemporaryTableInsertStrategy.class, MutationStrategyGeneratedIdWithOptimizerTest.NullClass.class })
public class MutationStrategyGeneratedIdWithOptimizerTest extends BaseReactiveTest {

	// I cannot specify null as value, so I'm using the name of this class as a marker
	public static final class NullClass {}

	@Parameter
	Class<?> insertStrategy;

	@Override
	protected Collection<Class<?>> annotatedEntities() {
		return Set.of( Person.class, Doctor.class, Engineer.class );
	}

	@Override
	protected Configuration constructConfiguration() {
		Configuration configuration = super.constructConfiguration();
		if ( insertStrategy != NullClass.class ) {
			configuration.setProperty( AvailableSettings.QUERY_MULTI_TABLE_INSERT_STRATEGY, insertStrategy );
		}
		return configuration;
	}

	@BeforeEach
	public void populateDb(VertxTestContext context) {
		Doctor doctor = new Doctor();
		doctor.setName( "Doctor John" );
		doctor.setEmployed( true );
		test( context, getMutinySessionFactory().withTransaction( s -> s.persist( doctor ) ) );
	}

	@Test
	public void testInsertStatic(VertxTestContext context) {
		test( context, getMutinySessionFactory()
				.withTransaction( session -> session
						.createMutationQuery( "insert into Engineer(id, name, employed, fellow) values (0, :name, :employed, false)" )
						.setParameter( "name", "John Doe" )
						.setParameter( "employed", true )
						.executeUpdate()
						.invoke( updateCount -> assertThat( updateCount ).isEqualTo( 1 ) )
						.chain( () -> session.find( Engineer.class, 0 ) )
						.invoke( engineer -> {
							assertThat( engineer ).isNotNull();
							assertThat( engineer.getName() ).isEqualTo( "John Doe" );
							assertThat( engineer.isEmployed() ).isTrue();
							assertThat( engineer.isFellow() ).isFalse();
						} )
				)
		);
	}

	@Test
	public void testInsertGenerated(VertxTestContext context) {
		test( context, getMutinySessionFactory()
				.withTransaction( session -> session
						.createMutationQuery( "insert into Engineer(name, employed, fellow) values (:name, :employed, false)" )
						.setParameter( "name", "John Doe" )
						.setParameter( "employed", true )
						.executeUpdate()
						.invoke( updateCount -> assertThat( updateCount ).isEqualTo( 1 ) )
						.chain( () -> session
								.createQuery( "from Engineer e where e.name = 'John Doe'", Engineer.class )
								.getSingleResult()
						)
						.invoke( engineer -> {
							assertThat( engineer.getName() ).isEqualTo( "John Doe" );
							assertThat( engineer.isEmployed() ).isTrue();
							assertThat( engineer.isFellow() ).isFalse();
						} )
				)
		);
	}

	@Test
	public void testInsertSelectStatic(VertxTestContext context) {
		test( context, getMutinySessionFactory()
				.withTransaction( session -> session
						.createMutationQuery( "insert into Engineer(id, name, employed, fellow) select d.id + 1, 'John Doe', true, false from Doctor d" )
						.executeUpdate()
						.invoke( updateCount -> assertThat( updateCount ).isEqualTo( 1 ) )
						.chain( () -> session
								.createQuery( "from Engineer e where e.name = 'John Doe'", Engineer.class )
								.getSingleResult()
						)
						.invoke( engineer -> {
							assertThat( engineer.getName() ).isEqualTo( "John Doe" );
							assertThat( engineer.isEmployed() ).isTrue();
							assertThat( engineer.isFellow() ).isFalse();
						} )
				)
		);
	}

	@Test
	public void testInsertSelectGenerated(VertxTestContext context) {
		test( context, getMutinySessionFactory()
				.withTransaction( session -> session
						.createMutationQuery( "insert into Engineer(id, name, employed, fellow) select d.id + 1, 'John Doe', true, false from Doctor d" )
						.executeUpdate()
						.invoke( updateCount -> assertThat( updateCount ).isEqualTo( 1 ) )
						.chain( () -> session
								.createQuery( "from Engineer e where e.name = 'John Doe'", Engineer.class )
								.getSingleResult()
						)
						.invoke( engineer -> {
							assertThat( engineer.getName() ).isEqualTo( "John Doe" );
							assertThat( engineer.isEmployed() ).isTrue();
							assertThat( engineer.isFellow() ).isFalse();
						} )
				)
		);
	}

	@Entity(name = "Person")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class Person {

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		private Integer id;

		private String name;

		private boolean employed;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public boolean isEmployed() {
			return employed;
		}

		public void setEmployed(boolean employed) {
			this.employed = employed;
		}
	}

	@Entity(name = "Doctor")
	public static class Doctor extends Person {
	}

	@Entity(name = "Engineer")
	public static class Engineer extends Person {

		private boolean fellow;

		public boolean isFellow() {
			return fellow;
		}

		public void setFellow(boolean fellow) {
			this.fellow = fellow;
		}
	}

}
