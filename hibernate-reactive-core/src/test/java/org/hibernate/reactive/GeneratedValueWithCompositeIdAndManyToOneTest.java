/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.reactive.mutiny.Mutiny;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.smallrye.mutiny.Uni;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxTestContext;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.assertj.core.api.Assertions;

import static java.util.concurrent.TimeUnit.MINUTES;

@Timeout(value = 10, timeUnit = MINUTES)
public class GeneratedValueWithCompositeIdAndManyToOneTest extends BaseReactiveTest {

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
								.createSelectionQuery(
										"from Fruit f left join fetch f.basket order by f.id",
										Fruit.class
								)
								.getResultList()
								.call( list -> {
									Assertions.assertThat( list ).isNotNull();
									return Mutiny.fetch( list.get( 0 ).basket.fruits );
								} )
								.invoke( fruits -> fruits.stream().forEach( System.out::println ) )
						)
				)
		);
	}

	@Entity(name = "Fruit")
	@Table(name = "fruit")
	@IdClass(FruitId.class)
	static class Fruit {

		@Id
		@GeneratedValue
		public Long id;

		@Id
		@JoinColumn(name = "basket_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "basket_fk"))
		@ManyToOne(fetch = FetchType.LAZY)
		public FruitBasket basket;

		@Column(length = 40, unique = true)
		public String name;

		public Fruit() {
		}

		public Fruit(String name) {
			this.name = name;
		}
	}

	@Entity
	@Table(name = "fruit_basket")
	static class FruitBasket {

		@Id
		@GeneratedValue
		public Long id;

		@Column
		public String name;

		// Just in case you get it running, to prevent issues with api json gen (cycle).
		@JsonIgnore
		@OneToMany(mappedBy = "basket", fetch = FetchType.LAZY)
		public Collection<Fruit> fruits = new ArrayList<>();
	}

	static class FruitId implements Serializable {

		private Long id;
		private FruitBasket basket;

		public Long getId() {
			return id;
		}

		public FruitId setId(Long id) {
			this.id = id;
			return this;
		}

		public FruitBasket getBasket() {
			return basket;
		}

		public FruitId setBasket(FruitBasket basket) {
			this.basket = basket;
			return this;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			FruitId fruitId = (FruitId) o;
			return Objects.equals( id, fruitId.id ) && Objects.equals( basket, fruitId.basket );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id, basket );
		}
	}
}
