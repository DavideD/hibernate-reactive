/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletionStage;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.reactive.provider.Settings;
import org.hibernate.reactive.testing.DatabaseSelectionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import static org.hibernate.reactive.containers.DatabaseConfiguration.DBType.POSTGRESQL;
import static org.hibernate.reactive.util.impl.CompletionStages.voidFuture;

/**
 * This test class verifies that data can be persisted and queried on the same database
 * using both JPA/hibernate and reactive session factories.
 */
public class ORMReactivePersistenceTest extends BaseReactiveTest {

	@Rule
	public DatabaseSelectionRule rule = DatabaseSelectionRule.runOnlyFor( POSTGRESQL );

	private SessionFactory ormFactory;

	@Override
	protected Collection<Class<?>> annotatedEntities() {
		return List.of( EndpointWebhook.class, Endpoint.class );
	}

	@Before
	public void prepareOrmFactory() {
		Configuration configuration = constructConfiguration();
		configuration.setProperty( Settings.DRIVER, "org.postgresql.Driver" );
		configuration.setProperty( Settings.DIALECT, PostgreSQLDialect.class.getName() );

		StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder()
				.applySettings( configuration.getProperties() );

		StandardServiceRegistry registry = builder.build();
		ormFactory = configuration.buildSessionFactory( registry );
	}

	@Override
	protected CompletionStage<Void> cleanDb() {
		return voidFuture();
	}

	@After
	public void closeOrmFactory() {
		ormFactory.close();
	}

	@Test
	public void testQuery() {
		final Endpoint endpoint = new Endpoint();
		endpoint.setAccountId( "XYZ_123"  );
		final EndpointWebhook webhook = new EndpointWebhook();
		endpoint.setWebhook( webhook );
		webhook.setEndpoint( endpoint );

		String query = "FROM Endpoint WHERE id = :id AND accountId = :accountId";
		try (Session session = ormFactory.openSession()) {
			Transaction transaction = session.beginTransaction();
			session.persist( endpoint );
			transaction.commit();
		}

		try (Session session = ormFactory.openSession()) {
			Transaction transaction = session.beginTransaction();
			Object singleResultOrNull = session.createQuery( query )
					.setParameter( "id", endpoint.getId() )
					.setParameter( "accountId", endpoint.getAccountId() )
					.getSingleResultOrNull();
			transaction.commit();
			System.out.println( singleResultOrNull );
		}
	}

	@Entity(name = "Endpoint")
	@Table(name = "endpoints")
	public static class Endpoint {

		@Id
		@GeneratedValue
		private Long id;

		@OneToOne(mappedBy = "endpoint", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
		private EndpointWebhook webhook;

		private String accountId;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public EndpointWebhook getWebhook() {
			return webhook;
		}

		public void setWebhook(EndpointWebhook webhook) {
			this.webhook = webhook;
		}

		public String getAccountId() {
			return accountId;
		}

		public void setAccountId(String accountId) {
			this.accountId = accountId;
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder( "Endpoint{" );
			sb.append( id );
			sb.append( ", " );
			sb.append( accountId );
			sb.append( '}' );
			return sb.toString();
		}
	}

	@Entity
	@Table(name = "endpoint_webhooks")
	public static class EndpointWebhook {

		@Id
		@GeneratedValue
		private Long id;

		@OneToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "endpoint_id")
		private Endpoint endpoint;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Endpoint getEndpoint() {
			return endpoint;
		}

		public void setEndpoint(Endpoint endpoint) {
			this.endpoint = endpoint;
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder( "EndpointWebhook{" );
			sb.append( id );
			sb.append( '}' );
			return sb.toString();
		}
	}
}
