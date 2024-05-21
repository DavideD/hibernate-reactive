/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.vertx.junit5.VertxTestContext;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

public class IdentityGeneratorWithBatchingTest extends BaseReactiveTest {
	@Override
	protected Collection<Class<?>> annotatedEntities() {
		return List.of( Hero.class );
	}

	@Test
	public void testInsert(VertxTestContext context) {
		final Hero atheena = new Hero( "Atheena Bluestar" );
		test( context, getMutinySessionFactory().withTransaction( s -> s.persist( atheena ) ) );
	}

	@Entity
	static class Hero {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;
		private String name;

		public Hero() {
		}

		public Hero(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return id + ":" + name;
		}
	}
}
