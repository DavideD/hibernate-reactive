/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.opq;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import org.hibernate.reactive.engine.opq.OperationQueue;
import org.hibernate.reactive.engine.opq.RegularTask;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.vertx.junit5.RunTestOnContext;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
@Timeout(value = 10, timeUnit = MINUTES)
//@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class OperationQueueTest {

	/**
	 * Configure Vertx JUnit5 test context
	 */
	@RegisterExtension
	static RunTestOnContext testOnContext = new RunTestOnContext();

	@Test
	public void testChainCompletionStage(VertxTestContext context) {
		OperationQueue operationQueue = new OperationQueue();
		test( context, operationQueue
				.chainStage( () -> completedFuture( "Text" ) )
				.add( "2. verify text", text -> context.verify( () -> assertThat( text ).isEqualTo( "Text" ) ) )
				.ignoreResult()
				.add( "3. ignoring result", obj -> context.verify( assertThat( obj )::isNull ) )
				.asCompletionStage()
		);
	}

	@Test
	public void testChainSequenceExecution(VertxTestContext context) {
		OperationQueue operationQueue = new OperationQueue();
		test( context, operationQueue
				.chain( () -> new RegularTask( "1. create Text", o -> "Text" ) )
				.chain( str -> new RegularTask( "2. verify text", text -> context.verify( () -> assertThat( str ).isEqualTo( "Text" ) ) ) )
				.ignoreResult()
				.add( "3. ignoring result", obj -> context.verify( assertThat( obj )::isNull ) )
				.asCompletionStage()
		);
	}

	@Test
	public void testWhenComplete(VertxTestContext context) {
		OperationQueue operationQueue = new OperationQueue();
		test( context, operationQueue
				.add( "1. create text", v -> "Test" )
				.add( "2. Throw an exception", text -> {
					throw new RuntimeException( "Testing exceptions" );
				} )
				.whenComplete( (o, throwable) -> context.verify(  ) )
				.asCompletionStage()
		);
	}

	@Test
	public void testMapSequenceExecution(VertxTestContext context) {
		OperationQueue operationQueue = new OperationQueue();
		test( context, operationQueue
				.add( "1. create text", v -> "Test" )
				.add( "2. verify text", text -> context.verify( () -> assertThat( text ).isEqualTo( "Test" ) ) )
				.ignoreResult()
				.add( "3. ignoring result", obj -> context.verify( assertThat( obj )::isNull ) )
				.asCompletionStage()
		);
	}

	@Test
	public void testBasicMapExecutionOder(VertxTestContext context) {
		OperationQueue operationQueue = new OperationQueue();
		final List<Integer> collector = new ArrayList<>();
		test( context, operationQueue
				.add( v -> collector.add( 1 ) )
				.add( v -> collector.add( 2 ) )
				.add( () -> collector.add( 3 ) )
				.add( v -> collector.add( 4 ) )
				.add( () -> collector.add( 5 ) )
				.asCompletionStage()
				.thenAccept( v -> context.verify( () -> assertThat( collector ).containsExactly( 1, 2, 3, 4, 5 ) ) )
		);
	}

	@Test
	public void testNestedMapExecutionOder(VertxTestContext context) {
		OperationQueue operationQueue = new OperationQueue();
		final List<Integer> collector = new ArrayList<>();
		test( context, operationQueue
				.add( "add 1", v -> {
					collect( 1, collector::add );
					return operationQueue
							.add( "add 2", () -> collect( 2, collector::add ) )
							.add( "add 3", () -> {
								collect( 3, collector::add );
								return operationQueue.add( "add 4", () -> collect( 4, collector::add ) );
							} )
							.add( "add 5", () -> collect( 5, collector::add ) );
				} )
				.add( "add 6", v -> collect( 6, collector::add ) )
				.add( "add 7", v -> collect( 7, collector::add ) )
				.asCompletionStage()
				.thenAccept( v -> context
						.verify( () -> assertThat( collector ).containsExactly( 1, 2, 3, 4, 5, 6, 7 ) )
				)
		);
	}

	private int collect(int index, Function<Integer, Boolean> fun ) {
		System.out.println( "Collecting " + index );
		fun.apply( index );
		return index;
	}

	private static <T> void test(VertxTestContext context, CompletionStage<T> stage) {
		stage.whenComplete( (r, t) -> {
			if ( t != null ) {
				context.failNow( t );
			}
			else {
				context.completeNow();
			}
		} );
	}
}
