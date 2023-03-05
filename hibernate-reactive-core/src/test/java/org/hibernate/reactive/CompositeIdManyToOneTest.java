/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.junit.Ignore;
import org.junit.Test;

import io.vertx.ext.unit.TestContext;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.Assertions.assertThat;

@Ignore // This also fails in 1.1, see issue https://github.com/hibernate/hibernate-reactive/issues/1384
public class CompositeIdManyToOneTest extends BaseReactiveTest {

	@Override
	protected Collection<Class<?>> annotatedEntities() {
		return List.of( GroceryList.class, ShoppingItem.class );
	}

	@Test
	public void reactivePersist(TestContext context) {
		GroceryList gl = new GroceryList();
		gl.id = 4L;
		ShoppingItem si = new ShoppingItem();
		si.groceryList = gl;
		si.itemName = "Milk";
		si.itemCount = 2;
		gl.shoppingItems.add( si );

		test( context, openSession()
				.thenCompose( s -> s.persist( gl )
						.thenCompose( v -> s.flush() ) )
				.thenCompose( v -> openSession() )
				.thenCompose( s -> s.createQuery( "from ShoppingItem si where si.groceryList.id = :gl" )
						.setParameter( "gl", gl.id )
						.getResultList() )
				.thenAccept( list -> assertThat( list ).contains( si ) )
		);
	}

	@Entity(name = "GroceryList")
	public static class GroceryList implements Serializable {

		@Id
		private Long id;

		@OneToMany(mappedBy = "groceryList", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
		private List<ShoppingItem> shoppingItems = new ArrayList<>();

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public List<ShoppingItem> getShoppingItems() {
			return shoppingItems;
		}

		public void setShoppingItems(List<ShoppingItem> shoppingItems) {
			this.shoppingItems = shoppingItems;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			GroceryList that = (GroceryList) o;
			return Objects.equals( id, that.id );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id );
		}
	}

	@Entity(name = "ShoppingItem")
	@IdClass(ShoppingItemId.class)
	public static class ShoppingItem implements Serializable {

		@Id
		private String itemName;

		@Id
		@ManyToOne
		@JoinColumn(name = "grocerylistid")
		private GroceryList groceryList;


		private int itemCount;

		public String getItemName() {
			return itemName;
		}

		public void setItemName(String itemName) {
			this.itemName = itemName;
		}

		public GroceryList getGroceryList() {
			return groceryList;
		}

		public void setGroceryList(GroceryList groceryList) {
			this.groceryList = groceryList;
		}

		public int getItemCount() {
			return itemCount;
		}

		public void setItemCount(int itemCount) {
			this.itemCount = itemCount;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			ShoppingItem that = (ShoppingItem) o;
			return itemCount == that.itemCount && Objects.equals(
					itemName,
					that.itemName
			) && Objects.equals( groceryList, that.groceryList );
		}

		@Override
		public int hashCode() {
			return Objects.hash( itemName, groceryList, itemCount );
		}
	}

	public static class ShoppingItemId implements Serializable {
		private String itemName;
		private GroceryList groceryList;
	}
}
