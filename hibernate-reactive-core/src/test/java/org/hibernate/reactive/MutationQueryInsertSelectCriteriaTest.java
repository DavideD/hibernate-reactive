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
import org.hibernate.query.criteria.JpaCriteriaInsertSelect;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxTestContext;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;

@Timeout(value = 10, timeUnit = MINUTES)
public class MutationQueryInsertSelectCriteriaTest extends BaseReactiveTest {

	private static final Integer SPELT_ID = 1;
	private static final String SPELT_NAME = "Spelt";
	private static final String SPELT_TYPE = "Wheat flour";
	Flour spelt = new Flour( SPELT_ID, SPELT_NAME, "An ancient grain, is a hexaploid species of wheat.", SPELT_TYPE );

	@Override
	protected Collection<Class<?>> annotatedEntities() {
		return List.of( Flour.class );
	}

	@BeforeEach
	public void populateDb(VertxTestContext context) {
		test( context, getSessionFactory().withTransaction( s -> s.persist( spelt ) ) );
	}

	@Test
	public void testStageInsertSelectCriteriaQuery(VertxTestContext context) {
		final int idOfTheNewFlour = 2;
		test(
				context,
				openSession()
						.thenCompose( s -> {
							/*
							 	The query executes and insert of Flour with id equals to 2 a name and type
							 	selected from the existing spelt flour saved in the db
							 */
							HibernateCriteriaBuilder cb = s.getFactory().getCriteriaBuilder();
							JpaCriteriaInsertSelect<Flour> insertSelect = cb.createCriteriaInsertSelect( Flour.class );
							// columns to insert
							insertSelect.setInsertionTargetPaths(
									insertSelect.getTarget().get( "id" ),
									insertSelect.getTarget().get( "name" ),
									insertSelect.getTarget().get( "type" )
							);
							// select query
							JpaCriteriaQuery<Tuple> select = cb.createQuery( Tuple.class );
							JpaRoot<Flour> root = select.from( Flour.class );
							select.multiselect( cb.literal( idOfTheNewFlour ), root.get( "name" ), root.get( "type" )  );
							select.where( cb.equal( root.get( "id" ), SPELT_ID ) );

							insertSelect.select( select );

							return s.createMutationQuery( insertSelect ).executeUpdate();
						} )
						.thenAccept( resultCount -> assertThat( resultCount ).isEqualTo( 1 ) )
						.thenCompose( v -> openSession() )
						.thenCompose( s -> s.find( Flour.class, idOfTheNewFlour ) )
						.thenAccept( result -> {
							assertThat( result ).isNotNull();
							assertThat( result.name ).isEqualTo( SPELT_NAME );
							assertThat( result.description ).isNull();
							assertThat( result.type ).isEqualTo( SPELT_TYPE );
						} )
		);
	}

	@Test
	public void testMutinyInsertSelectCriteriaQuery(VertxTestContext context) {
		final int idOfTheNewFlour = 3;

		test(
				context,
				openMutinySession()
						.chain( s -> {
							/*
							 	The query executes and insert of Flour with id equals to 2 a name and type
							 	selected from the existing spelt flour saved in the db
							 */
							HibernateCriteriaBuilder cb = s.getFactory().getCriteriaBuilder();
							JpaCriteriaInsertSelect<Flour> insertSelect = cb.createCriteriaInsertSelect( Flour.class );
							// columns to insert
							insertSelect.setInsertionTargetPaths(
									insertSelect.getTarget().get( "id" ),
									insertSelect.getTarget().get( "name" ),
									insertSelect.getTarget().get( "type" )
							);
							// select query
							JpaCriteriaQuery<Tuple> select = cb.createQuery( Tuple.class );
							JpaRoot<Flour> root = select.from( Flour.class );
							select.multiselect( cb.literal( idOfTheNewFlour ), root.get( "name" ), root.get( "type" )  );
							select.where( cb.equal( root.get( "id" ), SPELT_ID ) );

							insertSelect.select( select );

							return s.createMutationQuery( insertSelect ).executeUpdate();
						} )
						.invoke( resultCount -> assertThat( resultCount ).isEqualTo( 1 ) )
						.chain( v -> openMutinySession() )
						.chain( s -> s.find( Flour.class, idOfTheNewFlour ) )
						.invoke( result -> {
							assertThat( result ).isNotNull();
							assertThat( result.name ).isEqualTo( SPELT_NAME );
							assertThat( result.description ).isNull();
							assertThat( result.type ).isEqualTo( SPELT_TYPE );
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
