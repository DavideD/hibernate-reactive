/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.HibernateException;
import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.reactive.stage.Stage;
import org.hibernate.reactive.testing.DatabaseSelectionRule;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import io.smallrye.mutiny.Uni;
import io.vertx.ext.unit.TestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.reactive.containers.DatabaseConfiguration.DBType.POSTGRESQL;
import static org.hibernate.reactive.testing.DatabaseSelectionRule.runOnlyFor;
import static org.hibernate.reactive.testing.ReactiveAssertions.assertThrown;

/**
 * Checks that we throw the right exception when a session is shared between threads.
 * @see org.hibernate.reactive.common.InternalStateAssertions
 */
public class InternalStateAssertionsTest extends BaseReactiveTest {

	/**
	 * @see org.hibernate.reactive.logging.impl.Log#detectedUsedOfTheSessionOnTheWrongThread
	 */
	private static final String ERROR_CODE = "HR000069";

	private static final String ERROR_CODE_MUTINY = "HR000074";

	private Object currentSession;

	// These tests will fail before touching the database, so there is no reason
	// to run them on all databases
	@Rule
	public DatabaseSelectionRule rule = runOnlyFor( POSTGRESQL );

	@Override
	protected Collection<Class<?>> annotatedEntities() {
		return List.of( Competition.class );
	}

	@After
	public void closeEverything(TestContext context) {
		test( context, closeSession( currentSession ) );
	}

	@Test
	public void testPersistWithStage(TestContext testContext) {
		CompletionStage<Stage.Session> sessionStage = getSessionFactory().openSession();
		currentSession = sessionStage;

		ThreadPerCommandExecutor executor = new ThreadPerCommandExecutor();

		test( testContext, assertThrown( IllegalStateException.class, sessionStage
				.thenComposeAsync( session -> session.persist( new Competition( "Cheese Rolling" ) ), executor ) )
				.thenApply( InternalStateAssertionsTest::assertException )
				.thenAccept( t -> assertStackTrace( t, "lambda$testPersistWithStage", 72 ) )
		);
	}

	@Test
	public void testFindWithStage(TestContext testContext) {
		CompletionStage<Stage.Session> sessionStage = getSessionFactory().openSession();
		currentSession = sessionStage;

		ThreadPerCommandExecutor executor = new ThreadPerCommandExecutor();

		test( testContext, assertThrown( IllegalStateException.class, sessionStage
				.thenComposeAsync( session -> session.find( Competition.class, "Chess boxing" ), executor ) )
				.thenApply( InternalStateAssertionsTest::assertException )
				.thenAccept( t -> assertStackTrace( t, "lambda$testFindWithStage", 86 ) )
		);
	}

	@Test
	public void testPersistWithMutiny(TestContext testContext) {
		Uni<Mutiny.Session> sessionUni = getMutinySessionFactory().openSession();
		currentSession = sessionUni;

		ThreadPerCommandExecutor executor = new ThreadPerCommandExecutor();

		test( testContext, assertThrown( HibernateException.class, sessionUni
				.call( session -> session
						.persist( new Competition( "Cheese Rolling" ) )
						.runSubscriptionOn( executor ) ) )
				.invoke( InternalStateAssertionsTest::assertExceptionWithMutiny )
				.invoke( t -> assertStackTrace( t, "lambda$testPersistWithMutiny", 101 ) )
		);
	}

	/**
	 * We expect the stack trace to contain the line that causes the exception.
	 * But, it's a lambda and the exact line might change if you refactor the code.
	 * I don't expect to refactor this test class too often in the future,
	 * so it shouldn't cause too much trouble
	 */
	private static void assertStackTrace(Throwable t, String methodName, int line ) {
		Optional<StackTraceElement> se = Arrays
				.stream( t.getStackTrace() )
				.filter( stackTraceElement -> stackTraceElement
						.getClassName().equals( InternalStateAssertionsTest.class.getName() )
						&& stackTraceElement.getMethodName().startsWith( methodName )
						&& stackTraceElement.getLineNumber() == line )
				.findFirst();
		assertThat( se ).isPresent();
	}

	@Test
	public void testFindWithMutiny(TestContext testContext) {
		Uni<Mutiny.Session> sessionUni = getMutinySessionFactory().openSession();
		currentSession = sessionUni;

		ThreadPerCommandExecutor executor = new ThreadPerCommandExecutor();

		test( testContext, assertThrown( HibernateException.class, sessionUni
				.chain( session -> session
						.find( Competition.class, "Chess boxing" )
						.runSubscriptionOn( executor ) ) )
				.invoke( InternalStateAssertionsTest::assertExceptionWithMutiny )
				.invoke( t -> assertStackTrace( t, "lambda$testFindWithMutiny", 134 ) )
		);
	}

	private static Throwable assertException(Throwable t) {
		t.printStackTrace();
		assertThat( t.getMessage() ).startsWith( ERROR_CODE );
		return t;
	}

	private static void assertExceptionWithMutiny(Throwable t) {
		assertThat( t.getMessage() ).startsWith( ERROR_CODE_MUTINY );
		assertThat( t.getCause().getMessage() ).startsWith( ERROR_CODE );
	}

	/**
	 * Run a task in a thread that's different from the "Test worker"
	 */
	private static class ThreadPerCommandExecutor implements Executor {
		@Override
		public void execute(Runnable command) {
			new Thread( command, InternalStateAssertionsTest.class.getName() + "-thread" ).start();
		}
	}

	@Entity
	static class Competition {
		@Id
		String name;

		public Competition() {
		}

		public Competition(String name) {
			this.name = name;
		}
	}
}
