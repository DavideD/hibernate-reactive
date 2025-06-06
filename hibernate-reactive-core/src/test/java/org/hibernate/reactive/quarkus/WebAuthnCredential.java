/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.quarkus;

import java.util.Base64;
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

    public WebAuthnCredential(RequiredPersistedData requiredPersistedData) {
        aaguid = requiredPersistedData.aaguid();
        counter = requiredPersistedData.counter();
        credID = requiredPersistedData.credentialId();
        publicKey = requiredPersistedData.publicKey();
        publicKeyAlgorithm = requiredPersistedData.publicKeyAlgorithm();
    }

    /**
     * Record holding all the required persistent fields for logging back someone over WebAuthn.
     */
    public record RequiredPersistedData(
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
        /**
         * Returns a PEM-encoded representation of the public key. This is a utility method you can use as an alternate for
         * storing the
         * binary public key if you do not want to store a <code>byte[]</code> and prefer strings.
         *
         * @return a PEM-encoded representation of the public key
         */
        public String getPublicKeyPEM() {
            return "-----BEGIN PUBLIC KEY-----\n"
                    + Base64.getEncoder().encodeToString( publicKey)
                    + "\n-----END PUBLIC KEY-----\n";
        }
    }
}
