/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.it;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.hibernate.reactive.it.onetoone.User;
import org.hibernate.reactive.it.onetoone.WebAuthnCredential;

import org.junit.jupiter.api.Test;

import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxTestContext;

import static java.util.concurrent.TimeUnit.MINUTES;


@Timeout(value = 10, timeUnit = MINUTES)
public class AuthorizationBETest extends BaseReactiveIT {
	@Override
	protected Collection<Class<?>> annotatedEntities() {
		return List.of( User.class, WebAuthnCredential.class);
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

		WebAuthnCredential credential = new WebAuthnCredential(data, user);
		user.webAuthnCredential = credential;

		test( context, getMutinySessionFactory()
				.withTransaction( session -> session.persist( user ) )
				.chain( () -> getMutinySessionFactory().withTransaction( session -> session
						.persistAll( credential ) )
				)
		);
	}
}
