/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.engine.opq;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.reactive.logging.impl.Log;

import static java.lang.invoke.MethodHandles.lookup;
import static org.hibernate.reactive.logging.impl.LoggerFactory.make;
import static org.hibernate.reactive.util.impl.CompletionStages.failedFuture;
import static org.hibernate.reactive.util.impl.CompletionStages.voidFuture;

/**
 * NOT THREAD SAFE!
 */
public class OperationQueue {

	private static final Log LOG = make( Log.class, lookup() );

	private final List<Task> queues;
	private final List<Task> executionQueue;

	private Task taskInExecution;

	public OperationQueue() {
		this.queues = new ArrayList<>();
		this.executionQueue = new ArrayList<>();
	}

	public OperationQueue whenComplete(BiConsumer<Object, Throwable> consumer) {
		whenComplete( null, consumer );
		return this;
	}

	public OperationQueue whenComplete(String description, BiConsumer<Object, Throwable> consumer) {
		add( new WhenCompleteTask( description, consumer ) );
		return this;
	}

	public OperationQueue chainStage(Supplier<CompletionStage<Object>> stageSupplier) {
		return chainStage( null, stageSupplier );
	}

	public OperationQueue chainStage(String description, Supplier<CompletionStage<Object>> stageSupplier) {
		return chainStage( description, obj -> stageSupplier.get() );
	}

	public OperationQueue chainStage(Function<Object, CompletionStage<Object>> stageFunction) {
		chainStage( null, stageFunction );
		return this;
	}

	public OperationQueue chainStage(String description, Function<Object, CompletionStage<Object>> stageFunction) {
		add( new StageTask<>( description, stageFunction ) );
		return this;
	}

	public OperationQueue chain(Callable<RegularTask> fun) {
		return chain( convert( fun ) );
	}

	public OperationQueue chain(Function<Object, RegularTask> fun) {
		final RegularTask task = new RegularTask( obj -> add( fun.apply( obj ) ) );
		if ( taskInExecution != null ) {
			LOG.debugf( "Adding [%s] to the execution-queue", task  );
			executionQueue.add( task );
		}
		else {
			LOG.debugf( "Adding [%s] to last place", task  );
			queues.add( task );
		}
		return this;
	}

	public OperationQueue add(Function<Object, Object> fun) {
		return add( new RegularTask( fun ) );
	}

	public OperationQueue accept(Consumer<Object> fun) {
		return add( new RegularTask( convert( fun ) ) );
	}

	public OperationQueue add(String description, Function<Object, Object> fun) {
		return add( new RegularTask( description, fun ) );
	}

	public OperationQueue add(Callable<Object> fun) {
		return add( convert( fun ) );
	}

	public OperationQueue add(String description, Callable<Object> fun) {
		return add( description, convert( fun ) );
	}

	private OperationQueue add(Task task) {
		if ( taskInExecution != null ) {
			LOG.debugf( "Adding to the execution-queue: [%s]", task  );
			executionQueue.add( task );
		}
		else {
			LOG.debugf( "Adding to the queue: [%s]", task  );
			queues.add( task );
		}
		return this;
	}

	private <T> Function<T, Void> convert(Consumer<T> consumer) {
		return obj -> {
			consumer.accept( obj );
			return null;
		};
	}

	private <T> Function<Object, T> convert(Callable<T> callable) {
		return obj -> {
			try {
				return callable.call();
			}
			catch (Exception e) {
				throw new RuntimeException( e );
			}
		};
	}

	public OperationQueue ignoreResult() {
		return add( "ignoring result", () -> null );
	}

	public <R> R get() {
		return null;
	}

	private CompletionStage subscribe() {
		LOG.debugf( "Subscribing the queue" );
		CompletionStage<?> result = voidFuture();
		while ( !queues.isEmpty() ) {
			final Task task = queues.remove( 0 );
			result = result.thenCompose( obj -> {
				taskInExecution = task;
				return taskInExecution.apply( obj )
						.whenComplete( (o, o2) -> {
							queues.addAll( 0, executionQueue );
							executionQueue.clear();
							taskInExecution = null;
						} );
			} );
		}
		return result;
	}

	public <T> CompletionStage<T> asCompletionStage() {
		try {
			return subscribe();
		}
		catch (Throwable t) {
			return failedFuture( t );
		}
	}
}
