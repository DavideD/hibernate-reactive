/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.id.impl;

import io.vertx.core.Context;
import io.vertx.core.net.impl.pool.CombinerExecutor;
import io.vertx.core.net.impl.pool.Executor;
import io.vertx.core.net.impl.pool.Task;
import org.hibernate.reactive.id.ReactiveIdentifierGenerator;
import org.hibernate.reactive.session.ReactiveConnectionSupplier;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.hibernate.reactive.util.impl.CompletionStages.completedFuture;

/**
 * A {@link ReactiveIdentifierGenerator} which uses the database to allocate
 * blocks of ids. A block is identified by its "hi" value (the first id in
 * the block). While a new block is being allocated, concurrent streams wait
 * without blocking.
 *
 * @author Gavin King
 */
public abstract class BlockingIdentifierGenerator implements ReactiveIdentifierGenerator<Long> {

	/**
	 * The block size (the number of "lo" values for each "hi" value)
	 */

	protected abstract int getBlockSize();

	private final GeneratorState state = new GeneratorState();
	private final CombinerExecutor executor = new CombinerExecutor( state );

	/**
	 * Allocate a new block, by obtaining the next "hi" value from the database
	 */
	protected abstract CompletionStage<Long> nextHiValue(ReactiveConnectionSupplier session);

	private static class GeneratorState {
		private int loValue;
		private long hiValue;
	}

	//Not synchronized: needs to be accessed exclusively via the CombinerExecutor
	protected long next() {
		return state.loValue > 0 && state.loValue < getBlockSize()
				? state.hiValue + state.loValue++
				: -1; //flag value indicating that we need to hit db
	}

	//Not synchronized: needs to be accessed exclusively via the CombinerExecutor
	protected long next(long hi) {
		state.hiValue = hi;
		state.loValue = 1;
		return hi;
	}

	@Override
	public CompletionStage<Long> generate(ReactiveConnectionSupplier session, Object entity) {
		Objects.requireNonNull(session);
		CompletableFuture<Long> result = new CompletableFuture<>();
		executor.submit(new GenerateIdAction(session, result));
		return result;
	}

	private final class GenerateIdAction implements Executor.Action<GeneratorState> {

		private final ReactiveConnectionSupplier connectionSupplier;
		private final CompletableFuture<Long> result;

		public GenerateIdAction(ReactiveConnectionSupplier session, CompletableFuture<Long> result) {
			this.connectionSupplier = session;
			this.result = result;
		}

		@Override
		public Task execute(GeneratorState state) {
			if ( getBlockSize() <= 1 ) {
				//special case where we're not using blocking at all
				nextHiValue(connectionSupplier)
						.whenComplete(this::acceptAsReturnValue);
				return null;
			}
			long local = next();
			if ( local >= 0 ) {
				// We don't need to update or initialize the hi
				// value in the table, so just increment the lo
				// value and return the next id in the block
				completedFuture( local )
						.whenComplete(this::acceptAsReturnValue);
				return null;
			} else {
				nextHiValue( connectionSupplier )
						.whenComplete( (id, throwable) -> {
							if ( throwable != null ) {
								result.completeExceptionally( throwable );
							}
							else {
								executor.submit( new Executor.Action() {
									@Override
									public Task execute(Object state) {
										result.complete( next( id ) );
										return null;
									}
								});
							}
						} );
				return null;
			}
		}

		private void acceptAsReturnValue(Long aLong, Throwable throwable) {
			if (throwable != null) {
				result.complete(aLong);
			} else {
				result.completeExceptionally(throwable);
			}
		}
	}

	private final class DeferredTask {

		private final ReactiveConnectionSupplier session;
		private final Object entity;
		private final Context context;
		private final CompletableFuture<Long> result;

		public DeferredTask(ReactiveConnectionSupplier session, Object entity, Context context, CompletableFuture<Long> result) {
			this.session = session;
			this.entity = entity;
			this.context = context;
			this.result = result;
		}

		public void resume() {
//			prettyOut("Resuming");
			try {
				context.runOnContext((v) -> generate(session, entity)
						.whenComplete((r, t) -> {
							if (t != null) {
								prettyOut("Exception");
								result.completeExceptionally(t);
							} else {
								result.complete(r);
							}
						}));
			}
			catch (RuntimeException e) {
				result.completeExceptionally(e);
			}
		}
	}

	private static void prettyOut(final String message) {
		final String threadName = Thread.currentThread().getName();
		final long l = System.currentTimeMillis();
		final long seconds = ( l / 1000 ) - initialSecond;
		//We prefix log messages by seconds since bootstrap; I'm preferring this over millisecond precision
		//as it's not very relevant to see exactly how long each stage took (it's actually distracting)
		//but it's more useful to group things coarsely when some lock or timeout introduces a significant
		//divide between some operations (when a starvation or timeout happens it takes some seconds).
		System.out.println( seconds + " - " + threadName + ": " + message );
	}
	private static final long initialSecond = ( System.currentTimeMillis() / 1000 );
}
