/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.engine.opq;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import org.hibernate.reactive.logging.impl.Log;

import static java.lang.String.valueOf;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Objects.requireNonNull;
import static org.hibernate.reactive.logging.impl.LoggerFactory.make;
import static org.hibernate.reactive.util.impl.CompletionStages.completedFuture;

public class RegularTask implements Task {
	private static final Log LOG = make( Log.class, lookup() );
	private final Function<Object, ?> fun;
	private final String description;

	public RegularTask(Function<Object, ?> fun) {
		this( valueOf( fun ), fun );
	}

	public RegularTask(String description, Function<Object, ?> fun) {
		requireNonNull( fun );
		this.fun = fun;
		this.description = description;
	}

	public CompletionStage<Object> apply(Object obj) {
		LOG.debugf( "Applying %s", obj );
		return completedFuture( fun.apply( obj ) );
	}

	@Override
	public Object failed(Throwable t) {
		// This task doesn't handle failure;
		return null;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String toString() {
		return "RegularTask: " + description;
	}
}
