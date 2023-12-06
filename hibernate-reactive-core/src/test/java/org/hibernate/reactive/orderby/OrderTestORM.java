/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.orderby;

import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.internal.MappingMetamodelImpl;
import org.hibernate.reactive.BaseReactiveTest;
import org.hibernate.reactive.stage.Stage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.metamodel.SingularAttribute;

import static java.util.stream.Collectors.toList;
import static org.hibernate.cfg.JdbcSettings.DIALECT;
import static org.hibernate.cfg.JdbcSettings.DRIVER;
import static org.hibernate.query.Order.desc;
import static org.hibernate.reactive.containers.DatabaseConfiguration.dbType;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class OrderTestORM  extends BaseReactiveTest {
	private SessionFactory ormFactory;

	@BeforeEach
	public void prepareOrmFactory() {
		Configuration configuration = constructConfiguration();
		configuration.setProperty( DRIVER, dbType().getJdbcDriver() );
		configuration.setProperty( DIALECT, dbType().getDialectClass().getName() );

		StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder()
				.applySettings( configuration.getProperties() );

		StandardServiceRegistry registry = builder.build();
		ormFactory = configuration.buildSessionFactory( registry );
	}

	@AfterEach
	public void closeOrmFactory() {
		if ( ormFactory != null ) {
			ormFactory.close();
		}
	}


	@Override
	protected Set<Class<?>> annotatedEntities() {
		return Set.of( Book.class );
	}

	@Test
	public void testAscDescOrder() {
		Book book1 = new Book("9781932394153", "Hibernate in Action");
		Book book2 = new Book("9781617290459", "Java Persistence with Hibernate");

		try (Session session = ormFactory.openSession()) {
			session.beginTransaction();
			session.persist(book1);
			session.persist(book2);
			session.flush();
			session.getTransaction().commit();
		}

		try (Session session = ormFactory.openSession() ) {
			session.beginTransaction();
			Stage.SessionFactory sessionFactory = getSessionFactory();

			MappingMetamodelImpl metamodel = (MappingMetamodelImpl)sessionFactory.getMetamodel();
			EntityDomainType<Book> bookType = metamodel.getJpaMetamodel().findEntityType( Book.class);
			SingularAttribute<? super Book, ?> title = bookType.findSingularAttribute( "title");
			SingularAttribute<? super Book, ?> isbn = bookType.findSingularAttribute("isbn");
//			List<String> titlesAsc = session.createSelectionQuery( "from Book", Book.class)
//					.setOrder(asc(title))
//					.getResultList()
//					.stream().map(book -> book.title)
//					.collect(toList());
//			assertEquals("Hibernate in Action", titlesAsc.get(0));
//			assertEquals("Java Persistence with Hibernate", titlesAsc.get(1));
			session.getTransaction().commit();

			session.beginTransaction();
			List<String> titleDesc= session.createSelectionQuery( "from Book", Book.class)
					.setOrder(desc(title))
					.getResultList()
					.stream().map(book -> book.title)
					.collect(toList());
			assertEquals("Hibernate in Action", titleDesc.get(1));
			assertEquals("Java Persistence with Hibernate", titleDesc.get(0));
			session.getTransaction().commit();

		}
	}

	@Entity(name="Book")
	static class Book {
		@Id
		String isbn;
		String title;

		Book(String isbn, String title) {
			this.isbn = isbn;
			this.title = title;
		}

		Book() {
		}
	}

}
