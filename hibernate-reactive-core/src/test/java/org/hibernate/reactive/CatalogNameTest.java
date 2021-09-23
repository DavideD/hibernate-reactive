/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.cfg.Configuration;
import org.hibernate.reactive.provider.Settings;

import org.junit.Test;

import io.vertx.ext.unit.TestContext;

/**
 * Check that it's possible to set a different catalog and schema.
 */
public class CatalogNameTest extends BaseReactiveTest {

	protected Configuration constructConfiguration() {
		Configuration configuration = super.constructConfiguration();
		configuration.setProperty( Settings.DEFAULT_CATALOG, "master" );
		configuration.setProperty( Settings.DEFAULT_SCHEMA, "dbo" );
		configuration.addAnnotatedClass( Manga.class );
		return configuration;
	}

	@Test
	public void persistAndSelect(TestContext context) {
		final Manga berserk = new Manga( "Berserk" );
		test( context, getSessionFactory()
				.withTransaction( (session, transaction) -> session.persist( berserk ) )
				.thenCompose( v -> openSession().thenCompose( session -> session.find( Manga.class, berserk.getId() ) )
						.thenAccept( manga -> context.assertEquals( berserk.getTitle(), manga.getTitle() ) ) )
		);
	}

	@Entity(name = "Manga")
	@Table(name = "Manga")
	public static class Manga {
		@Id
		@GeneratedValue
		private Integer id;
		private String title;

		public Manga() {
		}

		public Manga(String title) {
			this.title = title;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}
	}
}
