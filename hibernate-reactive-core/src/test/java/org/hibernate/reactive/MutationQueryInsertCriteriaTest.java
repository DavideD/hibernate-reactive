/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaInsertValues;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxTestContext;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;

@Timeout(value = 10, timeUnit = MINUTES)
public class MutationQueryInsertCriteriaTest extends BaseReactiveTest {

	Flour spelt = new Flour( 1, "Spelt", "An ancient grain, is a hexaploid species of wheat.", "Wheat flour" );

	@Override
	protected Collection<Class<?>> annotatedEntities() {
		return List.of( Flour.class );
	}

	@BeforeEach
	public void populateDb(VertxTestContext context) {
		test( context, getSessionFactory().withTransaction( s -> s.persist( spelt ) ) );
	}

	@Test
	public void testStageInsertCriteriaQuery(VertxTestContext context) {
		final int id = 2;
		final String name = "Rye";
		final String description = "Used to bake the traditional sourdough breads of Germany.";
		final String type = "Wheat flour";
		test(
				context,
				openSession()
						.thenCompose( s -> {
							HibernateCriteriaBuilder cb = s.getFactory().getCriteriaBuilder();
							JpaCriteriaInsertValues<Flour> insert = cb.createCriteriaInsertValues( Flour.class );
							insert.setInsertionTargetPaths(
									insert.getTarget().get( "id" ),
									insert.getTarget().get( "name" ),
									insert.getTarget().get( "description" ),
									insert.getTarget().get( "type" )
							);
							insert.values(
									cb.values(
											cb.value( id ),
											cb.value( name ),
											cb.value( description ),
											cb.value( type )
									) );
							return s.createMutationQuery( insert ).executeUpdate();
						} )
						.thenAccept( resultCount -> assertThat( resultCount ).isEqualTo( 1 ) )
						.thenCompose( v -> openSession() )
						.thenCompose( s -> s.find( Flour.class, id ) )
						.thenAccept( result -> {
							assertThat( result ).isNotNull();
							assertThat( result.name ).isEqualTo( name );
							assertThat( result.description ).isEqualTo( description );
							assertThat( result.type ).isEqualTo( type );
						} )
		);
	}

	@Test
	public void testMutinyInsertCriteriaQuery(VertxTestContext context) {
		final int id = 3;
		final String name = "Almond";
		final String description = "made from ground almonds.";
		final String type = "Gluten free";
		test(
				context,
				openMutinySession()
						.chain( s -> {
							HibernateCriteriaBuilder cb = s.getFactory().getCriteriaBuilder();
							JpaCriteriaInsertValues<Flour> insert = cb.createCriteriaInsertValues( Flour.class );
							insert.setInsertionTargetPaths(
									insert.getTarget().get( "id" ),
									insert.getTarget().get( "name" ),
									insert.getTarget().get( "description" ),
									insert.getTarget().get( "type" )
							);
							insert.values(
									cb.values(
											cb.value( id ),
											cb.value( name ),
											cb.value( description ),
											cb.value( type )
									) );
							return s.createMutationQuery( insert ).executeUpdate();
						} )
						.invoke( resultCount -> assertThat( resultCount ).isEqualTo( 1 ) )
						.chain( v -> openMutinySession() )
						.chain( s -> s.find( Flour.class, id ) )
						.invoke( result -> {
							assertThat( result ).isNotNull();
							assertThat( result.name ).isEqualTo( name );
							assertThat( result.description ).isEqualTo( description );
							assertThat( result.type ).isEqualTo( type );
						} )
		);
	}

	@Entity(name = "Flour")
	@Table(name = "Flour")
	public static class Flour {
		@Id
		private Integer id;
		private String name;
		private String description;
		private String type;

		public Flour() {
		}

		public Flour(Integer id, String name, String description, String type) {
			this.id = id;
			this.name = name;
			this.description = description;
			this.type = type;
		}

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

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		@Override
		public String toString() {
			return name;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Flour flour = (Flour) o;
			return Objects.equals( name, flour.name ) &&
					Objects.equals( description, flour.description ) &&
					Objects.equals( type, flour.type );
		}

		@Override
		public int hashCode() {
			return Objects.hash( name, description, type );
		}
	}
}
