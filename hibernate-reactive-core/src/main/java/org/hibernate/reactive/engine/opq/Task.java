/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.engine.opq;

import java.util.concurrent.CompletionStage;


public interface Task<T, R> {
//
//	default <V> Function<?, V> andThen(Function<R, V> after) {
//		requireNonNull( after );
//		return (T t) -> apply( t ).thenCompose( obj -> after.apply( obj ) );
//	}

	String getDescription();

	CompletionStage<R> apply(T obj);

	<E extends Throwable> Object failed(E t);
}
