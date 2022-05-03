/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive;

import org.junit.Before;

import io.vertx.ext.unit.TestContext;

public abstract class WithoutVertxContextTest extends BaseReactiveTest {

	@Before
	public void before(TestContext context) {
		factoryManager.start( () -> createHibernateSessionFactory( constructConfiguration() ) );
	}



}
