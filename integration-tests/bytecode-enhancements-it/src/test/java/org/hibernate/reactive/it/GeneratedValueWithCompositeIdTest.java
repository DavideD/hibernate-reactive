/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.it;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.reactive.it.generatedvalue.Fruit;
import org.hibernate.reactive.it.generatedvalue.FruitBasket;
import org.hibernate.reactive.mutiny.Mutiny;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxTestContext;
import org.assertj.core.api.Assertions;

import static java.util.concurrent.TimeUnit.MINUTES;

@Timeout(value = 10, timeUnit = MINUTES)
public class GeneratedValueWithCompositeIdTest extends BaseReactiveIT {

	@Override
	protected Collection<Class<?>> annotatedEntities() {
		return List.of( Fruit.class, FruitBasket.class );
	}

	public Uni<Void> populateDb() {
		FruitBasket fruitBasket = new FruitBasket();
		fruitBasket.name = "MyBasket";
		Fruit[] fruits = {
				new Fruit( "Cherry" ),
				new Fruit( "Apple" ),
				new Fruit( "Banana" )
		};
		AtomicLong id = new AtomicLong( 1 );
		Arrays.stream( fruits ).forEach( fruit -> {
			fruit.id = id.getAndIncrement();
			fruit.basket = fruitBasket;
			fruitBasket.fruits.add( fruit );
		} );
		return getMutinySessionFactory()
				.withTransaction( session -> session
						.persist( fruitBasket )
						.call( () -> session.persistAll( fruits ) )
				);
	}

	@Test
	public void testFetchAll(VertxTestContext context) {
		test( context, populateDb()
				.call( () -> getMutinySessionFactory()
						.withTransaction( session -> session
								.createSelectionQuery( "from Fruit f left join fetch f.basket order by f.id", Fruit.class )
								.getResultList()
								.call( list -> {
									Assertions.assertThat( list ).isNotNull();
									return Mutiny.fetch( list.get( 0 ).basket.fruits );
								} )
								.invoke( fruits -> fruits.stream().forEach( System.out::println ) )
						)
				) );
	}
}
