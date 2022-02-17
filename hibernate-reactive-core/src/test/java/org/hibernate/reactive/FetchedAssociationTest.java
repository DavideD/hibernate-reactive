/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.reactive.provider.Settings;
import org.hibernate.reactive.testing.DatabaseSelectionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.vertx.ext.unit.TestContext;

import static org.hibernate.reactive.containers.DatabaseConfiguration.DBType.POSTGRESQL;

public class FetchedAssociationTest extends BaseReactiveTest {

	@Rule
	public DatabaseSelectionRule rule = DatabaseSelectionRule.runOnlyFor( POSTGRESQL );

	private SessionFactory ormFactory;

	@Override
	protected Configuration constructConfiguration() {
		Configuration configuration = super.constructConfiguration();
		configuration.addAnnotatedClass( Parent.class );
		configuration.addAnnotatedClass( Child.class );
		configuration.setProperty( Settings.SHOW_SQL, "true");
		return configuration;
	}

	@Before
	public void prepareOrmFactory() {
		Configuration configuration = constructConfiguration();
		configuration.setProperty( Settings.DRIVER, "org.postgresql.Driver" );
		configuration.setProperty( Settings.DIALECT, "org.hibernate.dialect.PostgreSQL95Dialect");

		StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder()
				.applySettings( configuration.getProperties() );

		StandardServiceRegistry registry = builder.build();
		ormFactory = configuration.buildSessionFactory( registry );
	}

	@After
	public void cleanDb(TestContext context) {
		ormFactory.close();

		test( context, deleteEntities( "Child", "Parent" ) );
	}

	@Test
	public void testWithMutiny(TestContext context) {
		test( context, getMutinySessionFactory()
				.withTransaction( s -> {
					final Parent parent = new Parent();
					parent.setName( "Parent" );
					return s.persist( parent );
				} )
				.call( () -> getMutinySessionFactory()
						.withTransaction( s -> s
								.createQuery( "From Parent", Parent.class )
								.getSingleResult()
								.call( parent -> Mutiny.fetch( parent.getChildren() )
										.call( children -> {
											Child child = new Child();
											child.setParent( parent );
											children.add( child );
											return s.persist( child );
										} )
								)
						)
				)
		);
	}

	@Test
	public void testWithORM() {
		try (Session ormSession = ormFactory.openSession()) {
			final Parent parent = new Parent();
			parent.setName( "Parent" );
			ormSession.beginTransaction();
			ormSession.persist( parent );
			ormSession.getTransaction().commit();
		}

		try (Session ormSession = ormFactory.openSession()) {
			ormSession.beginTransaction();
			final Parent parent = ormSession.createQuery( "From Parent", Parent.class ).getSingleResult();
			Child child = new Child();
			parent.getChildren().add( child );
			child.setParent( parent );
			ormSession.persist( child );
			ormSession.getTransaction().commit();
		}
	}

	@Entity(name = "Parent")
	@Table(name = "PARENT")
	public static class Parent {
 		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@OneToMany(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY, mappedBy = "parent")
		private List<Child> children = new ArrayList<>();

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<Child> getChildren() {
			return children;
		}

		public void setChildren(List<Child> children) {
			this.children = children;
		}
	}

	@Entity(name = "Child")
	@Table(name = "CHILD")
	public static class Child {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@ManyToOne
		@JoinColumn(name = "lazy_parent_id")
		private Parent parent;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
