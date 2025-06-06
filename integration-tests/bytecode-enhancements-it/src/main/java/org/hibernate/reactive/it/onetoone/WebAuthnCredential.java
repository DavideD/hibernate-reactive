/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.it.onetoone;

import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

@Entity
public class WebAuthnCredential {

    /**
     * The non user identifiable id for the authenticator
     */
    @Id
    public String credID;

    /**
     * The public key associated with this authenticator
     */
    public byte[] publicKey;

    public long publicKeyAlgorithm;

    /**
     * The signature counter of the authenticator to prevent replay attacks
     */
    public long counter;

    public UUID aaguid;

    // owning side
    @OneToOne
    public User user;

    public WebAuthnCredential() {
    }

    public WebAuthnCredential(RequiredPersistedData requiredPersistedData, User user) {
        aaguid = requiredPersistedData.aaguid();
        counter = requiredPersistedData.counter();
        credID = requiredPersistedData.credentialId();
        publicKey = requiredPersistedData.publicKey();
        publicKeyAlgorithm = requiredPersistedData.publicKeyAlgorithm();

        this.user = user;
        user.webAuthnCredential = this;
    }

	/**
	 * Record holding all the required persistent fields for logging back someone over WebAuthn.
	 */
	public static final class RequiredPersistedData {
		private final String username;
		private final String credentialId;
		private final UUID aaguid;
		private final byte[] publicKey;
		private final long publicKeyAlgorithm;
		private final long counter;

		/**
		 *
		 */
		public RequiredPersistedData(
				/**
				 * The user name. A single user name may be associated with multiple WebAuthn credentials.
				 */
				String username,
				/**
				 * The credential ID. This must be unique. See https://w3c.github.io/webauthn/#credential-id
				 */
				String credentialId,
				/**
				 * See https://w3c.github.io/webauthn/#aaguid
				 */
				UUID aaguid,
				/**
				 * A X.509 encoding of the public key. See https://w3c.github.io/webauthn/#credential-public-key
				 */
				byte[] publicKey,
				/**
				 * The COSE algorithm used for signing with the public key. See
				 * https://w3c.github.io/webauthn/#typedefdef-cosealgorithmidentifier
				 */
				long publicKeyAlgorithm,
				/**
				 * The increasing signature counter for usage of this credential record. See
				 * https://w3c.github.io/webauthn/#signature-counter
				 */
				long counter) {
			this.username = username;
			this.credentialId = credentialId;
			this.aaguid = aaguid;
			this.publicKey = publicKey;
			this.publicKeyAlgorithm = publicKeyAlgorithm;
			this.counter = counter;
		}

		/**
		 * Returns a PEM-encoded representation of the public key. This is a utility method you can use as an alternate for
		 * storing the
		 * binary public key if you do not want to store a <code>byte[]</code> and prefer strings.
		 *
		 * @return a PEM-encoded representation of the public key
		 */
		public String getPublicKeyPEM() {
			return "-----BEGIN PUBLIC KEY-----\n"
					+ Base64.getEncoder().encodeToString( publicKey )
					+ "\n-----END PUBLIC KEY-----\n";
		}

		public String username() {
			return username;
		}

		public String credentialId() {
			return credentialId;
		}

		public UUID aaguid() {
			return aaguid;
		}

		public byte[] publicKey() {
			return publicKey;
		}

		public long publicKeyAlgorithm() {
			return publicKeyAlgorithm;
		}

		public long counter() {
			return counter;
		}

		@Override
		public boolean equals(Object obj) {
			if ( obj == this ) {
				return true;
			}
			if ( obj == null || obj.getClass() != this.getClass() ) {
				return false;
			}
			var that = (RequiredPersistedData) obj;
			return Objects.equals( this.username, that.username ) &&
					Objects.equals( this.credentialId, that.credentialId ) &&
					Objects.equals( this.aaguid, that.aaguid ) &&
					Objects.equals( this.publicKey, that.publicKey ) &&
					this.publicKeyAlgorithm == that.publicKeyAlgorithm &&
					this.counter == that.counter;
		}

		@Override
		public int hashCode() {
			return Objects.hash( username, credentialId, aaguid, publicKey, publicKeyAlgorithm, counter );
		}

		@Override
		public String toString() {
			return "RequiredPersistedData[" +
					"username=" + username + ", " +
					"credentialId=" + credentialId + ", " +
					"aaguid=" + aaguid + ", " +
					"publicKey=" + publicKey + ", " +
					"publicKeyAlgorithm=" + publicKeyAlgorithm + ", " +
					"counter=" + counter + ']';
		}

	}
}
