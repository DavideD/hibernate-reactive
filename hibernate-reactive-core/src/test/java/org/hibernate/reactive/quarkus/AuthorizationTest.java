/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.quarkus;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.reactive.BaseReactiveTest;

import org.junit.jupiter.api.Test;

import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxTestContext;

import static java.util.concurrent.TimeUnit.MINUTES;


@Timeout(value = 10, timeUnit = MINUTES)
public class AuthorizationTest extends BaseReactiveTest {
	@Override
	protected Collection<Class<?>> annotatedEntities() {
		return List.of( User.class, WebAuthnCredential.class);
	}

	@Override
	protected Configuration constructConfiguration() {
		Configuration configuration = super.constructConfiguration();
		configuration.setProperty( AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, -1 );
		return configuration;
	}

	@Test
	public void test(VertxTestContext context) {
		WebAuthnCredential.RequiredPersistedData data = new WebAuthnCredential.RequiredPersistedData(
				"Azel",
				"credential-id-unique-byte-sequence",
				UUID.randomUUID(),
				"very-safe-public-key".getBytes(),
				-7L,
				333L
		);

		User user = new User();
		user.username = data.username();

		test( context, getMutinySessionFactory()
				.withTransaction( session -> session.persist( user ) )
				.chain( () -> getMutinySessionFactory().withTransaction( session -> session
						.find( User.class, user.id )
						.chain( found -> {
							WebAuthnCredential credential = new WebAuthnCredential( data );
							credential.user = found;
							found.webAuthnCredential = credential;

							return session.persist( credential );
						} )
				) )
		);
	}
}
