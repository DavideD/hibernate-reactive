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

import io.smallrye.mutiny.Uni;

import static java.lang.invoke.MethodHandles.lookup;
import static org.hibernate.reactive.logging.impl.LoggerFactory.make;
import static org.hibernate.reactive.util.impl.CompletionStages.completedFuture;
import static org.hibernate.reactive.util.impl.CompletionStages.failedFuture;

/**
 * NOT THREAD SAFE!
 */
public class OperationQueue {

	private static final Log LOG = make( Log.class, lookup() );

	private final List<Task> queues;
	private final List<Task> subQueue;

	private Task taskInExecution;

	public OperationQueue() {
		this.queues = new ArrayList<>();
		this.subQueue = new ArrayList<>();
	}

	public OperationQueue whenComplete(BiConsumer<Object, Throwable> consumer) {
		Task task = new WhenCompleteTask( consumer );
		add( task );
		return this;
	}

	public OperationQueue chainStage(Supplier<? extends CompletionStage<?>> stageSupplier) {
		add( () -> stageSupplier.get().thenAccept( obj -> add( () -> obj ) ) );
		return this;
	}


	public OperationQueue chainStage(Function<Object, ? extends CompletionStage<?>> stageSupplier) {
		add( obj1 -> stageSupplier.apply( obj1 ).thenAccept( obj2 -> add( () -> obj2 ) ) );
		return this;
	}

	public OperationQueue chain(Callable<RegularTask> fun) {
		return chain( convert( fun ) );
	}

	public OperationQueue chain(Function<Object, RegularTask> fun) {
		final RegularTask task = new RegularTask( obj -> add( fun.apply( obj ) ) );
		if ( taskInExecution != null ) {
			LOG.debugf( "Adding [%s] to the sub-queue", task  );
			subQueue.add( task );
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
			LOG.debugf( "Adding to the sub-queue: [%s]", task  );
			subQueue.add( task );
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

	private <T> T subscribe() {
		LOG.debugf( "Subscribing the queue" );
		Object result = null;
		Throwable failure = null;
		while ( !queues.isEmpty() ) {
			taskInExecution = queues.remove( 0 );
			try {
				if ( failure == null ) {
					result = taskInExecution.apply( result );
				}
				else {
					result = taskInExecution.failed( failure );
				}
			}
			catch (Throwable t) {
				if ( failure == null ) {
					failure = t;
				}
				else {
					failure.addSuppressed( t );
				}
			}
			finally {
				queues.addAll( 0, subQueue );
				subQueue.clear();
				taskInExecution = null;
			}
		}
		if ( failure != null ) {
			if ( failure instanceof RuntimeException ) {
				throw (RuntimeException) failure;
			}
			else {
				throw new RuntimeException( failure );
			}
		}
		return (T) result;
	}

	public <T> CompletionStage<T> asCompletionStage() {
		try {
			return completedFuture( subscribe() );
		}
		catch (Throwable t) {
			return failedFuture( t );
		}
	}

	public <T> Uni<T> asUni() {
		try {
			return Uni.createFrom().item( subscribe() );
		}
		catch (Throwable t) {
			return Uni.createFrom().failure( t );
		}
	}
}
