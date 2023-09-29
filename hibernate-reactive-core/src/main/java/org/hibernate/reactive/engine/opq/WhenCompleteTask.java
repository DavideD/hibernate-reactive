/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.engine.opq;

import java.util.function.BiConsumer;

import static java.lang.String.valueOf;
import static java.util.Objects.requireNonNull;

public class WhenCompleteTask implements Task {

	private final BiConsumer<Object, Throwable> consumer;
	private final String description;

	public WhenCompleteTask(BiConsumer<Object, Throwable> consumer) {
		this( valueOf( consumer ), consumer );
	}

	public <E extends Throwable> WhenCompleteTask(String description, BiConsumer<Object, Throwable> consumer) {
		requireNonNull( consumer );
		this.description = description;
		this.consumer = consumer;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public Object apply(Object obj) {
		consumer.accept( obj, null );
		return obj;
	}

	@Override
	public Object failed(Throwable t) {
		consumer.accept( null, t );
		return null;
	}
}
