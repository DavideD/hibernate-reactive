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
import static org.hibernate.reactive.util.impl.CompletionStages.completedFuture;
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
		add( new WhenCompleteTask( consumer ) );
		return this;
	}

	public OperationQueue chainStage(Supplier<CompletionStage<Object>> stageSupplier) {
		return add( new StageTask<>( obj -> stageSupplier.get() ) );
	}

	public OperationQueue chainStage(Function<Object, CompletionStage<Object>> stageFunction) {
		add( new StageTask<>( stageFunction ) );
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
			taskInExecution = queues.remove( 0 );
			try {
				result = result.thenCompose( obj -> taskInExecution == null
						? completedFuture( obj )
						: taskInExecution.apply( obj )
				);
			}
			finally {
				queues.addAll( 0, executionQueue );
				executionQueue.clear();
				taskInExecution = null;
			}
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
