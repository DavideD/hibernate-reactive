/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.engine.opq;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public class StageTask<T, R> implements Task<T, R> {

	private final Function<T, CompletionStage<R>> stageFunction;
	private final String description;

	public StageTask(Function<T, CompletionStage<R>> stageFunction) {
		this( null, stageFunction );
	}

	public StageTask(String description, Function<T, CompletionStage<R>> stageFunction) {
		requireNonNull( stageFunction );
		this.description = description;
		this.stageFunction = stageFunction;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public CompletionStage<R> apply(T obj) {
		return stageFunction.apply( obj );
	}

	@Override
	public <E extends Throwable> Object failed(E t) {
		return null;
	}
}
