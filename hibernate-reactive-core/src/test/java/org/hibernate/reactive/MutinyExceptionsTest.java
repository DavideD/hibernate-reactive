/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.reactive.util.impl.CompletionStages;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.vertx.junit5.VertxTestContext;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.reactive.containers.DatabaseConfiguration.DBType.DB2;
import static org.hibernate.reactive.containers.DatabaseConfiguration.DBType.SQLSERVER;
import static org.hibernate.reactive.containers.DatabaseConfiguration.dbType;
import static org.hibernate.reactive.testing.ReactiveAssertions.assertThrown;
import static org.hibernate.reactive.util.impl.CompletionStages.completedFuture;
import static org.hibernate.reactive.util.impl.CompletionStages.loop;


public class MutinyExceptionsTest extends BaseReactiveTest {

	@Override
	protected Collection<Class<?>> annotatedEntities() {
		return List.of( Person.class );
	}

	@Test
	public void testDuplicateKeyException(VertxTestContext context) {
		test( context, assertThrown( ConstraintViolationException.class, openMutinySession()
				.call( session -> session.persist( new Person( "testFLush1", "unique" ) ) )
				.call( Mutiny.Session::flush )
				.call( session -> session.persist( new Person( "testFlush2", "unique" ) ) )
				.call( Mutiny.Session::flush ) )
				.invoke( MutinyExceptionsTest::assertSqlStateCode )
				.invoke( MutinyExceptionsTest::assertConstraintName )
		);
	}

	private static void assertSqlStateCode(ConstraintViolationException exception) {
		if ( dbType() == SQLSERVER ) {
			// The SQL state code is always null in Sql Server (see https://github.com/eclipse-vertx/vertx-sql-client/issues/1385)
			// We test the vendor code for now
			SQLException sqlException = (SQLException) exception.getCause();
			assertThat( sqlException.getErrorCode() ).isEqualTo( 2601 );
		}
		else {
			assertThat( exception.getSQLState() )
					.as( "Constraint violation SQL state code should start with 23" )
					.matches( "23\\d{3}" );
		}
	}

	private static void assertConstraintName(ConstraintViolationException exception) {
		// DB2 does not return the constraint name
		if ( dbType() != DB2 ) {
			assertThat( exception.getConstraintName() )
					.as( "Failed constraint name should not be null" )
					.isNotNull();
		}
	}

	@Test
	public void testExceptionPropagationWithLoop(VertxTestContext context) {
		test( context, getMutinySessionFactory()
				.withTransaction( session -> Uni.createFrom().completionStage( loop(
						0, 50000, i -> true, i -> {
							System.out.println( i );
							return completedFuture( i );
						}
				) ) )
				.invoke( () -> System.out.println("End") )
		);
	}

	@Test
	public void testExceptionPropagation(VertxTestContext context) {
		test( context, getMutinySessionFactory()
				.withTransaction( session -> {
					int loop = 5000;
					Uni<?> uni = Uni.createFrom().voidItem();
					for ( int i = 0; i < loop; i++ ) {
						final int index = i;
						uni = uni.invoke( () -> System.out.println( index ) );
					}
					System.out.println("Finished chaining uni");
					return uni;
				} )
				.invoke( () -> System.out.println("End") )
		);
	}

	@Entity(name = "Person")
	@Table(name = "PersonForExceptionWithMutiny")
	public static class Person {

		@Id
		@Column(name = "[name]")
		public String name;

		@Column(unique = true)
		public String uniqueName;

		public Person() {
		}

		public Person(String name, String uniqueName) {
			this.name = name;
			this.uniqueName = uniqueName;
		}

		@Override
		public String toString() {
			return name + ", " + uniqueName;
		}
	}
}
