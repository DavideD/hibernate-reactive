/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.reactive.BaseReactiveTest;
import org.hibernate.reactive.stage.Stage;
import org.hibernate.reactive.testing.SqlStatementTracker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxTestContext;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The test aims to check that methods accepting the batch size as parameter e.g. {@link Stage.StatelessSession#insert(int, Object...)}
 * work when {@link AvailableSettings#STATEMENT_BATCH_SIZE} hasn't been set.
 */
@Timeout(value = 10, timeUnit = MINUTES)
public class ReactiveStatelessDefaultBatchSizeTest extends BaseReactiveTest {
	private static SqlStatementTracker sqlTracker;

	private static final Object[] PIGS = {
			new GuineaPig( 11, "One" ),
			new GuineaPig( 22, "Two" ),
			new GuineaPig( 33, "Three" ),
			new GuineaPig( 44, "Four" ),
			new GuineaPig( 55, "Five" ),
			new GuineaPig( 66, "Six" ),
	};

	@Override
	protected Set<Class<?>> annotatedEntities() {
		return Set.of( GuineaPig.class );
	}

	@Override
	protected Configuration constructConfiguration() {
		Configuration configuration = super.constructConfiguration();

		// Construct a tracker that collects query statements via the SqlStatementLogger framework.
		// Pass in configuration properties to hand off any actual logging properties
		sqlTracker = new SqlStatementTracker(
				ReactiveStatelessDefaultBatchSizeTest::filter,
				configuration.getProperties()
		);
		return configuration;
	}

	@BeforeEach
	public void clearTracker() {
		sqlTracker.clear();
	}

	@Override
	protected void addServices(StandardServiceRegistryBuilder builder) {
		sqlTracker.registerService( builder );
	}

	private static boolean filter(String s) {
		String[] accepted = { "insert ", "update ", "delete " };
		for ( String valid : accepted ) {
			if ( s.toLowerCase().startsWith( valid ) ) {
				return true;
			}
		}
		return false;
	}

	@Test
	public void testMutinyBatchingInsert(VertxTestContext context) {
		test( context, getMutinySessionFactory()
				.withStatelessTransaction( s -> s.insertAll( 10, PIGS ) )
				.invoke( () -> {
					// We expect only one insert query
					assertThat( sqlTracker.getLoggedQueries() ).hasSize( 1 );
					// Parameters are different for different dbs, so we cannot do an exact match
					assertThat( sqlTracker.getLoggedQueries().get( 0 ) ).matches( "insert into pig \\(name,id\\) values (.*)" );
				} )
		);
	}

	@Test
	public void testMutinyBatchingInsertMultiple(VertxTestContext context) {
		test( context, getMutinySessionFactory()
				.withStatelessTransaction( s -> s.insertMultiple( List.of( PIGS ) ) )
				.invoke( () -> {
					// We expect only one insert query
					assertThat( sqlTracker.getLoggedQueries() ).hasSize( 1 );
					// Parameters are different for different dbs, so we cannot do an exact match
					assertThat( sqlTracker.getLoggedQueries().get( 0 ) ).matches( "insert into pig \\(name,id\\) values (.*)" );
				} )
				.invoke( () -> getMutinySessionFactory().withStatelessTransaction( s -> s
						.createQuery( "from GuineaPig p", GuineaPig.class )
						.getResultList()
						.invoke( pigs -> assertThat( pigs ).hasSize( PIGS.length ) )
				) )
		);
	}

	@Test
	public void testStageBatchingInsert(VertxTestContext context) {
		test( context, getSessionFactory()
				.withStatelessTransaction( s -> s.insert( 10, PIGS ) )
				.thenAccept( v -> {
					// We expect only one insert query
					assertThat( sqlTracker.getLoggedQueries() ).hasSize( 1 );
					// Parameters are different for different dbs, so we cannot do an exact match
					assertThat( sqlTracker.getLoggedQueries().get( 0 ) ).matches( "insert into pig \\(name,id\\) values (.*)" );
				} )
				.thenAccept( v -> getSessionFactory().withStatelessTransaction( s -> s
						.createQuery( "from GuineaPig p", GuineaPig.class )
						.getResultList()
						.thenAccept( pigs -> assertThat( pigs ).hasSize( PIGS.length ) )
				) )
		);
	}

	@Test
	public void testStageBatchingInsertMultiple(VertxTestContext context) {
		test( context, getSessionFactory()
				.withStatelessTransaction( s -> s.insertMultiple( List.of( PIGS ) ) )
				.thenAccept( v -> {
					// We expect only one insert query
					assertThat( sqlTracker.getLoggedQueries() ).hasSize( 1 );
					// Parameters are different for different dbs, so we cannot do an exact match
					assertThat( sqlTracker.getLoggedQueries().get( 0 ) ).matches( "insert into pig \\(name,id\\) values (.*)" );
				} )
				.thenAccept( v -> getSessionFactory().withStatelessTransaction( s -> s
						.createQuery( "from GuineaPig p", GuineaPig.class )
						.getResultList()
						.thenAccept( pigs -> assertThat( pigs ).hasSize( PIGS.length ) )
				) )
		);
	}

	@Test
	public void testMutinyBatchingDelete(VertxTestContext context) {
		test( context, getMutinySessionFactory()
				.withStatelessTransaction( s -> s.insertAll( 10, PIGS ) )
				.invoke( sqlTracker::clear )
				.chain( () -> getMutinySessionFactory().withStatelessTransaction( s -> s
						.createQuery( "from GuineaPig p", GuineaPig.class )
						.getResultList()
				) )
				.chain( pigs -> getMutinySessionFactory().withStatelessTransaction( s -> s
						.deleteAll( 10, pigs.subList( 0, 2 ).toArray() ) )
				)
				.invoke( () -> {
					// We expect only one delete query
					assertThat( sqlTracker.getLoggedQueries() ).hasSize( 1 );
					// Parameters are different for different dbs, so we cannot do an exact match
					assertThat( sqlTracker.getLoggedQueries().get( 0 ) ).matches( "delete from pig where id=.*" );
				} )
				.chain( () -> getMutinySessionFactory().withStatelessTransaction( s -> s
						.createQuery( "from GuineaPig p", GuineaPig.class )
						.getResultList()
				) )
				.invoke( guineaPigs -> assertThat( guineaPigs.size() ).isEqualTo( 4 ) )
		);
	}

	@Test
	public void testMutinyBatchingDeleteMultiple(VertxTestContext context) {
		test( context, getMutinySessionFactory()
				.withStatelessTransaction( s -> s.insertAll( 10, PIGS ) )
				.invoke( sqlTracker::clear )
				.chain( () -> getMutinySessionFactory().withStatelessTransaction( s -> s
						.createQuery( "from GuineaPig p", GuineaPig.class )
						.getResultList()
				) )
				.chain( pigs -> getMutinySessionFactory()
						.withStatelessTransaction( s -> s.deleteMultiple( pigs.subList( 0, 2 ) ) )
				)
				.invoke( () -> {
					// We expect only one delete query
					assertThat( sqlTracker.getLoggedQueries() ).hasSize( 1 );
					// Parameters are different for different dbs, so we cannot do an exact match
					assertThat( sqlTracker.getLoggedQueries().get( 0 ) ).matches( "delete from pig where id=.*" );
				} )
				.chain( () -> getMutinySessionFactory().withStatelessTransaction( s -> s
						.createQuery( "from GuineaPig p", GuineaPig.class )
						.getResultList()
				) )
				.invoke( guineaPigs -> assertThat( guineaPigs.size() ).isEqualTo( 4 ) )
		);
	}

	@Test
	public void testStageBatchingDelete(VertxTestContext context) {
		test( context, getSessionFactory().withStatelessTransaction( s -> s.insert( 10, PIGS ) )
				.thenAccept( v -> sqlTracker.clear() )
				.thenCompose( v -> getSessionFactory().withStatelessTransaction( s -> s
						.createQuery( "from GuineaPig p", GuineaPig.class ).getResultList()
						.thenCompose( pigs -> s.delete( 10, pigs.subList( 0, 2 ).toArray() ) )
				) )
				.thenAccept( v -> {
					// We expect only one delete query
					assertThat( sqlTracker.getLoggedQueries() ).hasSize( 1 );
					// Parameters are different for different dbs, so we cannot do an exact match
					assertThat( sqlTracker.getLoggedQueries().get( 0 ) ).matches( "delete from pig where id=.*" );
				} )
				.thenCompose( v -> getSessionFactory()
						.withStatelessTransaction( s -> s
								.createQuery( "from GuineaPig p", GuineaPig.class )
								.getResultList()
						)
						.thenAccept( guineaPigs -> assertThat( guineaPigs.size() ).isEqualTo( 4 ) )
				)
		);
	}


	@Test
	public void testStageBatchingDeleteMultiple(VertxTestContext context) {
		test( context, getSessionFactory()
				.withStatelessTransaction( s -> s.insert( 10, PIGS ) )
				.thenAccept( v -> sqlTracker.clear() )
				.thenCompose( v -> getSessionFactory().withStatelessTransaction( s -> s
						.createQuery( "from GuineaPig p", GuineaPig.class ).getResultList()
						.thenCompose( pigs -> s.deleteMultiple( pigs.subList( 0, 2 ) ) )
				) )
				.thenAccept( v -> {
					// We expect only one delete query
					assertThat( sqlTracker.getLoggedQueries() ).hasSize( 1 );
					// Parameters are different for different dbs, so we cannot do an exact match
					assertThat( sqlTracker.getLoggedQueries().get( 0 ) ).matches( "delete from pig where id=.*" );
				} )
				.thenCompose( v -> getSessionFactory().withStatelessTransaction( s -> s
						.createQuery( "from GuineaPig p", GuineaPig.class ).getResultList()
				) )
				.thenAccept( guineaPigs -> assertThat( guineaPigs.size() ).isEqualTo( 4 ) )
		);
	}

	@Test
	public void testMutinyBatchingUpdate(VertxTestContext context) {
		test( context, getMutinySessionFactory()
				.withStatelessTransaction( s -> s.insertAll( 10, PIGS ) )
				.invoke( sqlTracker::clear )
				.chain( v -> getMutinySessionFactory().withStatelessTransaction( s -> s
						.createQuery( "from GuineaPig p order by p.id", GuineaPig.class )
						.getResultList()
						.chain( pigs -> {
							pigs.get( 0 ).setName( "One updated" );
							pigs.get( 1 ).setName( "Two updated" );
							return s.updateAll( 10, pigs.toArray() );
						} )
				) )
				.invoke( () -> {
					// We expect only one update query
					assertThat( sqlTracker.getLoggedQueries() ).hasSize( 1 );
					// Parameters are different for different dbs, so we cannot do an exact match
					assertThat( sqlTracker.getLoggedQueries().get( 0 ) ).matches( "update pig set name=.* where id=.*" );
				} )
				.chain( () -> getMutinySessionFactory().withStatelessTransaction( s -> s
						.createQuery( "from GuineaPig p order by id", GuineaPig.class )
						.getResultList()
						.invoke( ReactiveStatelessDefaultBatchSizeTest::checkPigsAreCorrectlyUpdated )
				) )
		);
	}

	@Test
	public void testMutinyBatchingUpdateMultiple(VertxTestContext context) {
		test( context, getMutinySessionFactory()
				.withStatelessTransaction( s -> s.insertAll( 10, PIGS ) )
				.invoke( sqlTracker::clear )
				.chain( v -> getMutinySessionFactory().withStatelessTransaction( s -> s
						.createQuery( "from GuineaPig p order by p.id", GuineaPig.class )
						.getResultList()
						.chain( pigs -> {
							pigs.get( 0 ).setName( "One updated" );
							pigs.get( 1 ).setName( "Two updated" );
							return s.updateMultiple( pigs );
						} )
				) )
				.invoke( () -> {
					// We expect only one update query
					assertThat( sqlTracker.getLoggedQueries() ).hasSize( 1 );
					// Parameters are different for different dbs, so we cannot do an exact match
					assertThat( sqlTracker.getLoggedQueries().get( 0 ) ).matches( "update pig set name=.* where id=.*" );
				} )
				.chain( () -> getMutinySessionFactory().withStatelessTransaction( s -> s
						.createQuery( "from GuineaPig p order by id", GuineaPig.class )
						.getResultList()
				) )
				.invoke( ReactiveStatelessDefaultBatchSizeTest::checkPigsAreCorrectlyUpdated )
		);
	}

	@Test
	public void testStageBatchingUpdate(VertxTestContext context) {
		test(context, getSessionFactory()
				.withStatelessTransaction( s -> s.insert( 10, PIGS ) )
				.thenAccept( v -> sqlTracker.clear() )
				.thenCompose( v -> getSessionFactory().withStatelessTransaction(s -> s
						.createQuery( "from GuineaPig p order by p.id", GuineaPig.class )
						.getResultList()
						.thenApply( pigs -> {
							pigs.get( 0 ).setName( "One updated" );
							pigs.get( 1 ).setName( "Two updated" );
							return s.update( 10, pigs.toArray() );
						} )
				) )
				.thenAccept( vo -> {
					// We expect only one update query
					assertThat( sqlTracker.getLoggedQueries() ).hasSize( 1 );
					// Parameters are different for different dbs, so we cannot do an exact match
					assertThat( sqlTracker.getLoggedQueries().get( 0 ) ).matches( "update pig set name=.* where id=.*" );
				} )
				.thenCompose( vo -> getSessionFactory().withStatelessTransaction( s -> s
						.createQuery( "from GuineaPig p order by id", GuineaPig.class )
						.getResultList()
				) )
				.thenAccept( ReactiveStatelessDefaultBatchSizeTest::checkPigsAreCorrectlyUpdated )
		);
	}

	@Test
	public void testStageBatchingUpdateMultiple(VertxTestContext context) {
		test(context, getSessionFactory()
				.withStatelessTransaction( s -> s.insert( 10, PIGS ) )
				.thenAccept( v -> sqlTracker.clear() )
				.thenCompose( v -> getSessionFactory().withStatelessTransaction( s -> s
						.createQuery( "from GuineaPig p order by p.id", GuineaPig.class )
						.getResultList()
						.thenApply( pigs -> {
							pigs.get( 0 ).setName( "One updated" );
							pigs.get( 1 ).setName( "Two updated" );
							return s.updateMultiple( pigs.subList( 0, 2 ) );
						} )
				) )
				.thenAccept( v  -> {
					// We expect only one update query
					assertThat( sqlTracker.getLoggedQueries() ).hasSize( 1 );
					// Parameters are different for different dbs, so we cannot do an exact match
					assertThat( sqlTracker.getLoggedQueries().get( 0 ) ).matches( "update pig set name=.* where id=.*" );
				} )
				.thenCompose( v -> getSessionFactory().withStatelessTransaction( s -> s
						.createQuery( "from GuineaPig p order by id", GuineaPig.class )
						.getResultList()
				) )
				.thenAccept( ReactiveStatelessDefaultBatchSizeTest::checkPigsAreCorrectlyUpdated )
		);
	}

	private static void checkPigsAreCorrectlyUpdated(List<GuineaPig> guineaPigs) {
		// Only the first 2 elements should have been updated
		assertThat( guineaPigs.subList( 0, 2 ) )
				.extracting( "name" )
				.containsExactly( "One updated", "Two updated" );

		// Every other entity should be the same
		GuineaPig[] array = guineaPigs.subList( 2, guineaPigs.size() ).toArray( new GuineaPig[0] );
		assertThat( guineaPigs.subList( 2, guineaPigs.size() ) ).containsExactly( array );
	}

	@Entity(name = "GuineaPig")
	@Table(name = "pig")
	public static class GuineaPig {
		@Id
		private Integer id;
		private String name;

		public GuineaPig() {
		}

		public GuineaPig(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return id + ": " + name;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			GuineaPig guineaPig = (GuineaPig) o;
			return Objects.equals( name, guineaPig.name );
		}

		@Override
		public int hashCode() {
			return Objects.hash( name );
		}
	}
}
