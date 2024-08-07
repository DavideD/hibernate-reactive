/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.engine.impl;

import java.util.Iterator;
import java.util.concurrent.CompletionStage;

import org.hibernate.HibernateException;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.reactive.stage.Stage;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;

/**
 * A {@link Stage.Session reactive session} operation that may
 * be cascaded from a parent entity to its children. A non-blocking counterpart
 * to {@link org.hibernate.engine.spi.CascadingAction}.
 *
 * @see CascadingActions
 * @author Gavin King
 */
public interface CascadingAction<C> {

	/**
	 * Cascade the action to the child object.
	 *
	 * @param session The session within which the cascade is occurring.
	 * @param child The child to which cascading should be performed.
	 * @param entityName The child's entity name
	 * @param context Anything ;)  Typically some form of cascade-local cache
	 * which is specific to each CascadingAction type
	 * @param isCascadeDeleteEnabled Are cascading deletes enabled.
	 */
	CompletionStage<Void> cascade(
			EventSource session,
			Object child,
			String entityName,
			C context,
			boolean isCascadeDeleteEnabled) throws HibernateException;

	/**
	 * Given a collection, get an iterator of the children upon which the
	 * current cascading action should be visited.
	 *
	 * @param session The session within which the cascade is occurring.
	 * @param collectionType The mapping type of the collection.
	 * @param collection The collection instance.
	 * @return The children iterator.
	 */
	Iterator<?> getCascadableChildrenIterator(
			EventSource session,
			CollectionType collectionType,
			Object collection);

	/**
	 * Does this action potentially extrapolate to orphan deletes?
	 *
	 * @return True if this action can lead to deletions of orphans.
	 */
	boolean deleteOrphans();

	/**
	 * Does the specified cascading action require verification of no cascade validity?
	 *
	 * @return True if this action requires no-cascade verification; false otherwise.
	 *
	 * @deprecated No longer used
	 */
	@Deprecated(since = "2.4", forRemoval = true)
	default boolean requiresNoCascadeChecking() {
		return false;
	}

	/**
	 * Called (in the case of {@link #requiresNoCascadeChecking} returning true) to validate
	 * that no cascade on the given property is considered a valid semantic.
	 *
	 * @param session The session within which the cascade is occurring.
	 * @param parent The property value owner
	 * @param persister The entity persister for the owner
	 * @param propertyType The property type
	 * @param propertyIndex The index of the property within the owner.
	 */
	@Deprecated(since = "2.4", forRemoval = true)
	CompletionStage<Void> noCascade(EventSource session, Object parent, EntityPersister persister, Type propertyType, int propertyIndex);

	/**
	 * Should this action be performed (or noCascade consulted) in the case of lazy properties.
	 */
	boolean performOnLazyProperty();

	org.hibernate.engine.spi.CascadingAction<C> delegate();
}
