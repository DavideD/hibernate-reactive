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
import org.hibernate.loader.BatchFetchStyle;
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

	/**
	 * "hibernate.order_by.default_null_ordering" -> "none"
	 * "jakarta.persistence.validation.mode" -> "AUTO"
	 * "hibernate.cache.use_query_cache" -> {Boolean@7540} true
	 * "jakarta.persistence.database-product-name" -> "PostgreSQL"
	 * "hibernate.hbm2ddl.charset_name" -> "UTF-8"
	 * "hibernate.query.plan_cache_max_size" -> "2048"
	 * "hibernate.id.sequence.increment_size_mismatch_strategy" -> {SequenceMismatchStrategy@7548} "NONE"
	 * "hibernate.cache.use_reference_entries" -> {Boolean@7540} true
	 * "hibernate.cache.use_second_level_cache" -> {Boolean@7540} true
	 * "hibernate.query.in_clause_parameter_padding" -> "true"
	 * "jakarta.persistence.sharedCache.mode" -> {SharedCacheMode@7554} "ENABLE_SELECTIVE"
	 * "hibernate.id.optimizer.pooled.preferred" -> "pooled-lo"
	 * "hibernate.default_batch_fetch_size" -> "16"
	 * "hibernate.batch_fetch_style" -> "PADDED"
	 */
	@Override
	protected Configuration constructConfiguration() {
		Configuration configuration = super.constructConfiguration();
		configuration.setProperty( AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, 16 );
		configuration.setProperty( "hibernate.batch_fetch_style", BatchFetchStyle.PADDED );
		configuration.setProperty( "hibernate.batch_fetch_style", "" );
		configuration.setProperty( "hibernate.order_by.default_null_ordering", "none" );
		configuration.setProperty( "hibernate.cache.use_reference_entries", Boolean.TRUE );
		configuration.setProperty( "hibernate.query.in_clause_parameter_padding", "true" );
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
