/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;


import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.junit.jupiter.api.Test;

import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxTestContext;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import static java.util.concurrent.TimeUnit.MINUTES;

@Timeout(value = 10, timeUnit = MINUTES)

public class StatelessSessionWithBatchingTest extends BaseReactiveTest {

	@Override
	protected Collection<Class<?>> annotatedEntities() {
		return List.of( GuineaPig.class );
	}

	@Override
	protected Configuration constructConfiguration() {
		Configuration configuration = super.constructConfiguration();
		configuration.setProperty( AvailableSettings.STATEMENT_BATCH_SIZE, "400" );
		return configuration;
	}

	@Test
	public void testMutinyStatelessSession(VertxTestContext context) {
		List<GuineaPig> pigList = new ArrayList<>();
		for ( int i = 1; i < 300; i++ ) {
			pigList.add( new GuineaPig( i, "Pig " + i ) );
		}
		test( context, getMutinySessionFactory().withStatelessTransaction( ss -> ss
				.insertAll( 400, pigList.toArray() )
				.chain( v -> ss.createQuery( "from GuineaPig", GuineaPig.class )
						.setMaxResults( 1 )
						.getResultList() ) )
		);
	}

	@Entity(name = "GuineaPig")
	@Table(name = "Piggy")
	public static class GuineaPig {
		@Id
		private Integer id;
		private String name;
		@Version
		private int version;

		public GuineaPig() {
		}

		public GuineaPig(Integer id, String name) {
			this.id = id;
			this.name = name;
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

		@Override
		public String toString() {
			return id + ": " + name;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			GuineaPig guineaPig = (GuineaPig) o;
			return Objects.equals( name, guineaPig.name );
		}

		@Override
		public int hashCode() {
			return Objects.hash( name );
		}
	}
}
