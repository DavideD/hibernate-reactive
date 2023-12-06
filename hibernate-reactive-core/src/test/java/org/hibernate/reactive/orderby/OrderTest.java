/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.orderby;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletionStage;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.internal.MappingMetamodelImpl;
import org.hibernate.query.Order;
import org.hibernate.reactive.BaseReactiveTest;
import org.hibernate.reactive.util.impl.CompletionStages;

import org.junit.jupiter.api.Test;

import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxTestContext;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.metamodel.SingularAttribute;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// TODO: Started this test class based on org.hibernate.orm.test.query.order.OrderTest
// Changes required:
//  - implement "setOrder(...)" methods for reactive session class as well as reactive SelectionQuery classes
//  - ORM test relies on finding the "SingularAttribute" type for an element in order to call
//  	"import static org.hibernate.query.Order.asc;"
//		"import static org.hibernate.query.Order.desc;"
//
@Timeout(value = 10, timeUnit = MINUTES)
public class OrderTest extends BaseReactiveTest {
	final Book book1 = new Book("9781932394153", "Hibernate in Action");
	final Book book2 = new Book("9781617290459", "Java Persistence with Hibernate");


	@Override
	protected Collection<Class<?>> annotatedEntities() {
		return List.of(
				Book.class
		);
	}

	@Override
	protected CompletionStage<Void> cleanDb() {
		return CompletionStages.voidFuture();
	}

	private CompletionStage<Void> populateDB() {
		return getSessionFactory()
				.withTransaction( session -> session.persist( book1, book2 ) );
	}

	@Test
	public void testOrder(VertxTestContext context) {
		test(
				context,
				populateDB().thenCompose( v -> getSessionFactory()
						.withTransaction( (session, tx) -> session
								.createSelectionQuery( "select title from Book", String.class )
								.getResultList()
								.thenAccept( list -> assertTrue( list.size() == 2 ) )
								.thenCompose( vv -> {
									MappingMetamodelImpl metamodel = (MappingMetamodelImpl) getSessionFactory().getMetamodel();
									EntityDomainType<Book> bookType = metamodel.getJpaMetamodel()
											.findEntityType( Book.class );
									SingularAttribute<? super Book, ?> title = bookType.findSingularAttribute( "title" );
									SingularAttribute<? super Book, ?> isbn = bookType.findSingularAttribute( "isbn" );
									return session
											.createSelectionQuery( "from Book", Book.class )
											.setOrder( Order.asc( title ) )
											.getResultList();
								} ).thenAccept( books -> {
									assertEquals( "Hibernate in Action", books.get( 0 ).title );
									assertEquals( "Java Persistence with Hibernate", books.get( 1 ).title );
								} )
						) )
		);

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
