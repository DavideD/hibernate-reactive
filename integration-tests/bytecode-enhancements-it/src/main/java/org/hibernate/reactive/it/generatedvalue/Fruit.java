/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.it.generatedvalue;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity(name = "Fruit")
@Table(name = "fruit")
@IdClass(Fruit.FruitId.class)
public class Fruit {

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

	@Override
	public String toString() {
		return id + ":" + name;
	}

	public static class FruitId implements Serializable {

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
