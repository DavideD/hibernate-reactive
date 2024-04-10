/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.it.generatedvalue;

import java.util.ArrayList;
import java.util.Collection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "fruit_basket")
public class FruitBasket {

	@Id
	@GeneratedValue
	public Long id;

	@Column
	public String name;

	// Just in case you get it running, to prevent issues with api json gen (cycle).
	@OneToMany(mappedBy = "basket", fetch = FetchType.LAZY)
	public Collection<Fruit> fruits = new ArrayList<>();

	@Override
	public String toString() {
		return id + ":" + name;
	}
}
