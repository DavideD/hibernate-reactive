/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.reactive.containers.DatabaseConfiguration.DBType.POSTGRESQL;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.reactive.provider.Settings;
import org.hibernate.reactive.testing.DatabaseSelectionRule;
import org.hibernate.reactive.testing.SqlStatementTracker;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import io.vertx.ext.unit.TestContext;

/**
 *
 */
public abstract class InsertOrderingReferenceSeveralDifferentSubclassBase extends BaseReactiveTest {

	@Rule
	public DatabaseSelectionRule rule = DatabaseSelectionRule.runOnlyFor( POSTGRESQL );

	// Actual # of inserts = 7, but these should be sorted & collapsed to 4 inserts with the duplicate
	// inserts being batched
	public static class OrderedTest extends InsertOrderingReferenceSeveralDifferentSubclassBase {

		@Override
		public boolean isOrderedInsert() {
			return true;
		}

		@Override
		public String[] getExpectedSqlList() {
			return new String[] {
					"insert into UnrelatedEntity (unrelatedValue, id) values ($1, $2)",
					"insert into BaseEntity (name, TYPE, id) values ($1, 'ZERO', $2)",
					"insert into BaseEntity (name, PARENT_ID, TYPE, id) values ($1, $2, 'TWO', $3)",
					"insert into BaseEntity (name, PARENT_ID, TYPE, id) values ($1, $2, 'ONE', $3)"
			};
		}
	}

	public static class UnorderedTest extends InsertOrderingReferenceSeveralDifferentSubclassBase {

		@Override
		public boolean isOrderedInsert() {
			return false;
		}

		@Override
		public String[] getExpectedSqlList() {
			return new String[] {
					"insert into UnrelatedEntity (unrelatedValue, id) values ($1, $2)",
					"insert into BaseEntity (name, TYPE, id) values ($1, 'ZERO', $2)",
					"insert into BaseEntity (name, PARENT_ID, TYPE, id) values ($1, $2, 'TWO', $3)",
					"insert into BaseEntity (name, PARENT_ID, TYPE, id) values ($1, $2, 'ONE', $3)",
					"insert into UnrelatedEntity (unrelatedValue, id) values ($1, $2)",
					"insert into BaseEntity (name, PARENT_ID, TYPE, id) values ($1, $2, 'ONE', $3)",
					"insert into BaseEntity (name, PARENT_ID, TYPE, id) values ($1, $2, 'TWO', $3)"
			};
		}
	}

	/**
	 * @return the expected list of SQL queries
	 */
	public abstract String[] getExpectedSqlList();

	/**
	 * @return value of property {@link Settings#ORDER_INSERTS}
	 */
	public abstract boolean isOrderedInsert();

	private SqlStatementTracker sqlTracker;

	@Override
	protected Configuration constructConfiguration() {
		Configuration configuration = super.constructConfiguration();
		configuration.setProperty( Settings.ORDER_INSERTS, String.valueOf( isOrderedInsert() ) );
		configuration.setProperty( Settings.STATEMENT_BATCH_SIZE, "10" );
		configuration.addAnnotatedClass( BaseEntity.class );
		configuration.addAnnotatedClass( SubclassZero.class );
		configuration.addAnnotatedClass( SubclassOne.class );
		configuration.addAnnotatedClass( SubclassTwo.class );
		configuration.addAnnotatedClass( UnrelatedEntity.class );

		sqlTracker = new SqlStatementTracker(
				InsertOrderingReferenceSeveralDifferentSubclassBase::onlyInserts,
				configuration.getProperties() );
		return configuration;
	}

	private static boolean onlyInserts(String s) {
		return s.toLowerCase().startsWith( "insert" );
	}

	@Override
	protected void addServices(StandardServiceRegistryBuilder builder) {
		sqlTracker.registerService( builder );
	}

	@After
	public void cleanDB(TestContext context) {
		deleteEntities( "SubclassZero", "SubclassOne", "SubclassTwo", "UnrelatedEntity" );
		sqlTracker.clear();
	}

	@Test
	public void testSubclassReferenceChain(TestContext context) {
		UnrelatedEntity unrelatedEntity1 = new UnrelatedEntity();
		SubclassZero subclassZero = new SubclassZero( "SubclassZero" );
		SubclassOne subclassOne = new SubclassOne( "SubclassOne" );
		subclassOne.setParent( subclassZero );
		SubclassTwo subclassTwo = new SubclassTwo( "SubclassTwo" );
		subclassTwo.setParent( subclassOne );

		// add extra instances for the sake of volume
		UnrelatedEntity unrelatedEntity2 = new UnrelatedEntity();
		SubclassOne subclassOne2 = new SubclassOne( "SubclassOne2" );
		SubclassTwo subclassD2 = new SubclassTwo( "SubclassD2" );

		test( context, getMutinySessionFactory().withTransaction( session -> session
						.persistAll(
								unrelatedEntity1, subclassZero, subclassTwo, subclassOne, unrelatedEntity2, subclassOne2,
								subclassD2
						) )
				.chain( () -> getMutinySessionFactory()
						.withSession( s -> s.find( SubclassOne.class, subclassOne.id ) ) )
				.invoke( result -> {
					context.assertEquals( subclassOne.name, result.name );
					assertThat( sqlTracker.getLoggedQueries() ).containsExactly( getExpectedSqlList() );
				} )

		);
	}

	@Entity(name = "BaseEntity")
	@DiscriminatorColumn(name = "TYPE")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public static abstract class BaseEntity {

		@Id
		@GeneratedValue
		public Long id;

		public String name;

		public BaseEntity() {
		}

		public BaseEntity(String name) {
			this.name = name;
		}
	}

	@Entity(name = "SubclassZero")
	@DiscriminatorValue("ZERO")
	public static class SubclassZero extends BaseEntity {

		public SubclassZero() {
		}

		public SubclassZero(String name) {
			super( name );
		}
	}

	@Entity(name = "SubclassOne")
	@DiscriminatorValue("ONE")
	public static class SubclassOne extends BaseEntity {

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "PARENT_ID")
		private SubclassZero parent;

		@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST, orphanRemoval = true, mappedBy = "parent", targetEntity = SubclassTwo.class)
		private List<SubclassTwo> subclassTwoList = new ArrayList<>();

		public SubclassOne() {
		}

		public SubclassOne(String name) {
			super( name );
		}

		public SubclassOne(String name, SubclassZero parent) {
			super( name );
			this.parent = parent;
		}

		public void setParent(SubclassZero parent) {
			this.parent = parent;
		}
	}

	@Entity(name = "SubclassTwo")
	@DiscriminatorValue("TWO")
	public static class SubclassTwo extends BaseEntity {

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "PARENT_ID")
		private SubclassOne parent;

		public SubclassTwo() {
		}

		public SubclassTwo(String name) {
			super( name );
		}

		public void setParent(SubclassOne parent) {
			this.parent = parent;
		}

		public SubclassTwo(String name, SubclassOne parent) {
			super( name );
			this.parent = parent;
		}
	}

	@Entity(name = "UnrelatedEntity")
	public static class UnrelatedEntity {

		@Id
		@GeneratedValue
		private Long id;

		private String unrelatedValue;

		public UnrelatedEntity() {
		}

		public UnrelatedEntity(Long id) {
			this.id = id;
		}

		public void setUnrelatedValue(String unrelatedValue) {
			this.unrelatedValue = unrelatedValue;
		}
	}
}
