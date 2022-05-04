/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;


import org.junit.Test;


public class CompletionStageTest {

	@Test
	public void test() {
		CompletableFuture
				.completedFuture( printThread("Start") )
				.thenComposeAsync( v -> subStage(), new TestExecutor( "Main exec - " ) );
	}

	private CompletionStage<String> subStage() {
		return CompletableFuture.completedFuture( null )
				.thenApply( v -> printThread( "Sub 1" ) )
				.thenApply( v -> printThread( "Sub 2" ) );
	}

	private static String printThread(String step) {
		Thread thread = Thread.currentThread();
		System.out.println( step + ": " + thread.getId() + ":" + thread.getName() );
		return "Done!";
	}

	private static class TestExecutor implements Executor {

		private final String id;

		public TestExecutor(String id) {
			this.id = id;
		}

		@Override
		public void execute(Runnable command) {
			System.out.println( "Test executor" + id );
			command.run();
		}
	}
}
