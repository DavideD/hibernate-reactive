/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.it.onetoone;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * In quarkus: io.quarkus.it.security.webauthn.User
 */
@Entity
@Table(name = "user_table")
public class User {

	@Id
	@GeneratedValue
	public Long id;

	@Column(unique = true)
	public String username;

	// non-owning side, so we can add more credentials later
	@OneToOne(mappedBy = "user")
	public WebAuthnCredential webAuthnCredential;
}
