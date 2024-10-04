/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import org.junit.jupiter.api.Test;

import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxTestContext;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

import static java.util.concurrent.TimeUnit.MINUTES;

@Timeout(value = 10, timeUnit = MINUTES)
public class JPQLTest extends BaseReactiveTest {

	@Override
	protected java.util.Collection<Class<?>> annotatedEntities() {
		return List.of( Collection.class, Vocabulary.class, CollectionAccess.class );
	}

	@Test
	public void testJPQL(VertxTestContext context) {
		test( context, getMutinySessionFactory().withTransaction( s -> s
				.createSelectionQuery( "SELECT c FROM Collection c " +
											   "  LEFT JOIN FETCH c.vocabularies v " +
											   "WHERE c.id IN (" +
											   "    SELECT id FROM (" +
											   "        SELECT " +
											   "          cc.id AS id, " +
											   "          dense_rank() over (" +
											   "            order by " +
											   "              id asc" +
											   "          ) AS ranking " +
											   "        FROM Collection cc " +
											   "        WHERE " +
											   "          UPPER(name) LIKE ?1 " +
											   "          AND (" +
											   "            createdBy = ?2 " +
											   "            OR EXISTS (" +
											   "              SELECT 1 FROM CollectionAccess ca " +
											   "              WHERE ca.collection.id = cc.id " +
											   "                AND ca.accessType = :accessType " +
											   "                AND ca.expirationAtUtc <= :expirationAtUtc " +
											   "            )" +
											   "          )" +
											   "      ) pr " +
											   "    WHERE " +
											   "      ranking >= : offset " +
											   "      AND ranking <= : until " +
											   "  ) " +
											   "order by " +
											   "  c.id asc", Collection.class )
				.setParameter( 1, "Davide" )
				.setParameter( 2, UUID.randomUUID() )
				.setParameter( "accessType", 1 )
				.setParameter( "expirationAtUtc", LocalDateTime.now() )
				.setParameter( "offset", 1L )
				.setParameter( "until", 100L )
				.getResultList()
		) );
	}

	@Entity(name = "Collection")
	public static class Collection {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		private String name;

		@OneToMany(mappedBy = "belongsTo", cascade = CascadeType.ALL)
		@OrderBy("id desc")
		private List<Vocabulary> vocabularies;

		@OneToMany(mappedBy = "collection")
		private List<CollectionAccess> collectionAccesses;

		@CreationTimestamp
		protected LocalDateTime createdAtUtc;
		protected UUID createdBy;
		@UpdateTimestamp
		protected LocalDateTime lastModifiedAtUtc;
		protected UUID lastModifiedBy;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<Vocabulary> getVocabularies() {
			return vocabularies;
		}

		public void setVocabularies(List<Vocabulary> vocabularies) {
			this.vocabularies = vocabularies;
		}

		public List<CollectionAccess> getCollectionAccesses() {
			return collectionAccesses;
		}

		public void setCollectionAccesses(List<CollectionAccess> collectionAccesses) {
			this.collectionAccesses = collectionAccesses;
		}

		public LocalDateTime getCreatedAtUtc() {
			return createdAtUtc;
		}

		public void setCreatedAtUtc(LocalDateTime createdAtUtc) {
			this.createdAtUtc = createdAtUtc;
		}

		public UUID getCreatedBy() {
			return createdBy;
		}

		public void setCreatedBy(UUID createdBy) {
			this.createdBy = createdBy;
		}

		public LocalDateTime getLastModifiedAtUtc() {
			return lastModifiedAtUtc;
		}

		public void setLastModifiedAtUtc(LocalDateTime lastModifiedAtUtc) {
			this.lastModifiedAtUtc = lastModifiedAtUtc;
		}

		public UUID getLastModifiedBy() {
			return lastModifiedBy;
		}

		public void setLastModifiedBy(UUID lastModifiedBy) {
			this.lastModifiedBy = lastModifiedBy;
		}
	}

	@Entity(name = "Vocabulary")
	public static class Vocabulary {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		@ManyToOne
		private Collection belongsTo;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Collection getBelongsTo() {
			return belongsTo;
		}

		public void setBelongsTo(Collection belongsTo) {
			this.belongsTo = belongsTo;
		}
	}

	@Entity(name = "CollectionAccess")
	public static class CollectionAccess {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		private Integer accessType;
		private LocalDateTime expirationAtUtc;

		@ManyToOne
		private Collection collection;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Integer getAccessType() {
			return accessType;
		}

		public void setAccessType(Integer accessType) {
			this.accessType = accessType;
		}

		public LocalDateTime getExpirationAtUtc() {
			return expirationAtUtc;
		}

		public void setExpirationAtUtc(LocalDateTime expirationAtUtc) {
			this.expirationAtUtc = expirationAtUtc;
		}

		public Collection getCollection() {
			return collection;
		}

		public void setCollection(Collection collection) {
			this.collection = collection;
		}
	}
}
