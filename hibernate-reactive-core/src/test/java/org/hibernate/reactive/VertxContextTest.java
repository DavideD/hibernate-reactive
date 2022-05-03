/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;


import org.junit.Test;

import io.vertx.ext.unit.TestContext;

import static org.hibernate.reactive.util.impl.CompletionStages.loop;

public class VertxContextTest extends WithVertxContextTest {

	@Override
	protected Collection<Class<?>> annotatedEntities() {
		return Set.of( PopularCharacter.class );
	}

	@Test
	public void test(TestContext context) {
		final PopularCharacter ichi = new PopularCharacter( 1, "Ichi" );
		test( context, getSessionFactory()
				.withTransaction( s -> s.persist( ichi ) )
				.thenCompose( v -> loop( 2, 200, index -> getSessionFactory()
						.withTransaction( s -> s.persist( new PopularCharacter( index, "Character " + index) ) ) ) )
				.thenCompose( v -> getSessionFactory()
						.withSession( s -> s.find( PopularCharacter.class, 1 ) ) )
				.thenAccept( found -> context.assertEquals( ichi, found ) )
		);
	}

	@Table(name = "Popular_Character")
	@Entity(name = "PC")
	public static class PopularCharacter {

		@Id
		public Integer id;

		@Column
		public String name;

		public PopularCharacter() {
		}

		public PopularCharacter(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			PopularCharacter that = (PopularCharacter) o;
			return Objects.equals( name, that.name );
		}

		@Override
		public int hashCode() {
			return Objects.hash( name );
		}
	}
}
