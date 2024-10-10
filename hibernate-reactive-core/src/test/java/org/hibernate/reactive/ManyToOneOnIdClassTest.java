/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxTestContext;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.JdbcSettings.DIALECT;
import static org.hibernate.cfg.JdbcSettings.DRIVER;
import static org.hibernate.reactive.containers.DatabaseConfiguration.dbType;
import static org.hibernate.reactive.util.impl.CompletionStages.voidFuture;

@Timeout(value = 10, timeUnit = MINUTES)
public class ManyToOneOnIdClassTest extends BaseReactiveTest {

	@Override
	protected Collection<Class<?>> annotatedEntities() {
		return List.of( Customer.class, Product.class, Delivery.class );
	}


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
		ormFactory.close();
	}

	@Test
	public void test(VertxTestContext context) {
		UUID productId = UUID.randomUUID();
		Product product = new Product( productId, "Milk" );
		UUID customerId = UUID.randomUUID();
		Customer customer = new Customer( customerId, "John Doe" );
		Delivery delivery = new Delivery( product, customer, "testDelivery" );
		test( context, getMutinySessionFactory()
				.withTransaction( session -> session.persistAll( product, customer, delivery ) )
				.chain( () -> getMutinySessionFactory().withTransaction( s -> s
						.createSelectionQuery( "FROM Delivery", Delivery.class ).getResultList()
				) )
				.invoke( list -> assertThat( list ).containsExactlyInAnyOrder( delivery ) )
		);
	}

	@Test
	public void testORM() {
		UUID productId = UUID.randomUUID();
		Product product = new Product( productId, "Milk" );
		UUID customerId = UUID.randomUUID();
		Customer customer = new Customer( customerId, "John Doe" );
		Delivery delivery = new Delivery( product, customer, "testDelivery" );
		try (Session session = ormFactory.openSession()) {
			session.beginTransaction();
			session.persist( product );
			session.persist( customer );
			session.persist( delivery );
			session.getTransaction().commit();
		}
		try (Session session = ormFactory.openSession()) {
			session.beginTransaction();
			List<Delivery> deliveries = session.createSelectionQuery( "From Delivery", Delivery.class )
					.getResultList();
			assertThat( deliveries ).containsExactlyInAnyOrder( delivery );
			session.getTransaction().commit();
		}
	}

	@Override
	protected CompletionStage<Void> cleanDb() {
		return voidFuture();
	}

	@Entity
	private static class Customer {

		@Id
		@Column(name = "id")
		private UUID id;

		@Column(name = "name")
		private String name;

		public Customer(UUID id, String name) {
			this.id = id;
			this.name = name;
		}

		public Customer() {
		}

		public UUID getId() {
			return id;
		}

		public void setId(UUID id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Customer customer = (Customer) o;
			return Objects.equals( id, customer.id ) && Objects.equals( name, customer.name );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id, name );
		}

		@Override
		public String toString() {
			return "Customer:" + id + ":" + name;
		}
	}

	@Entity
	private static class Product {

		@Id
		@Column(name = "id")
		private UUID id;

		@Column(name = "name")
		private String name;

		public Product() {
		}

		public Product(UUID id, String name) {
			this.id = id;
			this.name = name;
		}

		public UUID getId() {
			return id;
		}

		public void setId(UUID id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Product product = (Product) o;
			return Objects.equals( id, product.id ) && Objects.equals( name, product.name );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id, name );
		}

		@Override
		public String toString() {
			return "Product:" + id + ":" + name;
		}
	}

	@Entity(name = "Delivery")
	@IdClass(DeliveryId.class)
	private static class Delivery {

		@Id
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "product_id", referencedColumnName = "id")
		private Product product;

		@Id
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "customer_id", referencedColumnName = "id")
		private Customer customer;

		@Id
		@Column(name = "additional_key")
		private String additionalKey;

		public Delivery(Product product, Customer customer, String additionalKey) {
			this.product = product;
			this.customer = customer;
			this.additionalKey = additionalKey;
		}

		public Delivery() {
		}

		public Product getProduct() {
			return product;
		}

		public void setProduct(Product product) {
			this.product = product;
		}

		public Customer getCustomer() {
			return customer;
		}

		public void setCustomer(Customer customer) {
			this.customer = customer;
		}

		public String getAdditionalKey() {
			return additionalKey;
		}

		public void setAdditionalKey(String additionalKey) {
			this.additionalKey = additionalKey;
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			if ( !( obj instanceof Delivery ) ) {
				return false;
			}
			Delivery other = (Delivery) obj;
			if ( product == null
					|| other.getProduct() == null
					|| customer == null
					|| other.getCustomer() == null
					|| additionalKey == null
					|| other.getAdditionalKey() == null
			) {
				return false;
			}
			return product.equals( other.getProduct() )
					&& customer.equals( other.getCustomer() )
					&& Objects.equals( additionalKey, other.getAdditionalKey() );
		}

		@Override
		public int hashCode() {
			return Objects.hash( product, customer, additionalKey );
		}

		@Override
		public String toString() {
			return "[" + product + "-" + customer + "]:" + additionalKey;
		}

	}

	private static class DeliveryId {

		private UUID product;
		private UUID customer;
		private String additionalKey;

		public UUID getProduct() {
			return product;
		}

		public void setProduct(UUID product) {
			this.product = product;
		}

		public UUID getCustomer() {
			return customer;
		}

		public void setCustomer(UUID customer) {
			this.customer = customer;
		}

		public String getAdditionalKey() {
			return additionalKey;
		}

		public void setAdditionalKey(String additionalKey) {
			this.additionalKey = additionalKey;
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			if ( !( obj instanceof DeliveryId ) ) {
				return false;
			}
			DeliveryId other = (DeliveryId) obj;
			if ( product == null
					|| other.getProduct() == null
					|| customer == null
					|| other.getCustomer() == null
			) {
				return false;
			}
			return product.equals( other.getProduct() )
					&& customer.equals( other.getCustomer() )
					&& Objects.equals( additionalKey, other.getAdditionalKey() );
		}

		@Override
		public int hashCode() {
			return Objects.hash( product, customer, additionalKey );
		}
	}
}
