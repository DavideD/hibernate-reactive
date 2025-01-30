/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxTestContext;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.Root;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;

@Timeout(value = 10, timeUnit = MINUTES)
public class MutationQueryDeleteCriteriaTest extends BaseReactiveTest {

	Flour spelt = new Flour( 1, "Spelt", "An ancient grain, is a hexaploid species of wheat.", "Wheat flour" );
	Flour rye = new Flour( 2, "Rye", "Used to bake the traditional sourdough breads of Germany.", "Wheat flour" );
	Flour almond = new Flour( 3, "Almond", "made from ground almonds.", "Gluten free" );

	@Override
	protected Collection<Class<?>> annotatedEntities() {
		return List.of( Flour.class );
	}

	@BeforeEach
	public void populateDb(VertxTestContext context) {
		test( context, getSessionFactory().withTransaction( s -> s.persist( spelt, rye, almond ) ) );
	}

	@Test
	public void testDeleteCriteriaQuery(VertxTestContext context) {
		CriteriaBuilder criteriaBuilder = getSessionFactory().getCriteriaBuilder();
		test( context, getSessionFactory()
				.withTransaction( s -> s
						.createMutationQuery( criteriaDelete( criteriaBuilder ) )
						.executeUpdate() )
				.thenAccept( resultCount -> assertThat( resultCount ).isEqualTo( 1 ) )
				.thenCompose( v -> getSessionFactory()
						.withTransaction( s -> s.find( Flour.class, rye.getId() ) ) )
				.thenAccept( result -> assertThat( result ).isNull() )
		);
	}

	@Test
	public void testMutinyDeleteCriteriaQuery(VertxTestContext context) {
		CriteriaBuilder cb = getMutinySessionFactory().getCriteriaBuilder();
		test( context, getMutinySessionFactory()
				.withTransaction( s -> s
						.createMutationQuery( criteriaDelete( cb ) )
						.executeUpdate() )
				.invoke( resultCount -> assertThat( resultCount ).isEqualTo( 1 ) )
				.chain( v -> getMutinySessionFactory()
						.withTransaction( s -> s.find( Flour.class, almond.getId() ) ) )
				.invoke( result -> assertThat( result ).isNull() )
		);
	}

	private CriteriaDelete<Flour> criteriaDelete(CriteriaBuilder criteriaBuilder) {
		CriteriaDelete<Flour> criteriaDelete = criteriaBuilder.createCriteriaDelete( Flour.class );
		Root<Flour> from = criteriaDelete.from( Flour.class );
		criteriaDelete.where( criteriaBuilder.equal( from.get( "id" ), rye.getId() ) );
		return criteriaDelete;
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
