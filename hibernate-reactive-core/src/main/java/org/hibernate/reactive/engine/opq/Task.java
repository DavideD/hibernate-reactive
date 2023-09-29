/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.engine.opq;

public interface Task {
	String getDescription();

	Object apply(Object obj);

	<E extends Throwable> Object failed(E t);
}
