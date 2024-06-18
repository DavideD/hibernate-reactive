/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.sql.results.graph.entity.internal;

import java.util.concurrent.CompletionStage;

import org.hibernate.annotations.NotFoundAction;
import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.EntityUniqueKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.reactive.sql.results.graph.ReactiveInitializer;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.entity.EntityResultGraphNode;
import org.hibernate.sql.results.graph.entity.internal.EntityInitializerImpl;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.Type;

import static org.hibernate.metamodel.mapping.ForeignKeyDescriptor.Nature.TARGET;
import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;
import static org.hibernate.reactive.util.impl.CompletionStages.voidFuture;

public class ReactiveEntityInitializerImpl extends EntityInitializerImpl
		implements ReactiveInitializer<EntityInitializerImpl.EntityInitializerData> {

	public static class ReactiveEntityInitializerData extends EntityInitializerData {

		public ReactiveEntityInitializerData(RowProcessingState rowProcessingState) {
			super( rowProcessingState );
		}

		public void setEntityInstanceForNotify(Object instance) {
			super.entityInstanceForNotify = instance;
		}

		public Object getEntityInstanceForNotify() {
			return super.entityInstanceForNotify;
		}

		public EntityPersister getConcreteDescriptor() {
			return super.concreteDescriptor;
		}

		public void setConcreteDescriptor(EntityPersister entityPersister) {
			super.concreteDescriptor = entityPersister;
		}

		public EntityHolder getEntityHolder() {
			return super.entityHolder;
		}

		public void setEntityHolder(EntityHolder entityHolder) {
			super.entityHolder = entityHolder;
		}

		public EntityKey getEntityKey() {
			return super.entityKey;
		}

		public void setEntityKey(EntityKey entityKey) {
			super.entityKey = entityKey;
		}

		public String getUniqueKeyAttributePath() {
			return super.uniqueKeyAttributePath;
		}

		public void setUniqueKeyAttributePath(String uniqueKeyAttributePath) {
			super.uniqueKeyAttributePath = uniqueKeyAttributePath;
		}

		public Type[] getUniqueKeyPropertyTypes() {
			return super.uniqueKeyPropertyTypes;
		}

		public boolean getShallowCached() {
			return super.shallowCached;
		}

		public void setShallowCached(boolean shallowCached) {
			super.shallowCached = shallowCached;
		}
	}

	public ReactiveEntityInitializerImpl(
			EntityResultGraphNode resultDescriptor,
			String sourceAlias,
			Fetch identifierFetch,
			Fetch discriminatorFetch,
			DomainResult<?> keyResult,
			DomainResult<Object> rowIdResult,
			NotFoundAction notFoundAction,
			boolean affectedByFilter,
			InitializerParent<?> parent,
			boolean isResultInitializer,
			AssemblerCreationState creationState) {
		super(
				resultDescriptor,
				sourceAlias,
				identifierFetch,
				discriminatorFetch,
				keyResult,
				rowIdResult,
				notFoundAction,
				affectedByFilter,
				parent,
				isResultInitializer,
				creationState
		);
	}

	@Override
	protected EntityInitializerData createInitializerData(RowProcessingState rowProcessingState) {
		return new ReactiveEntityInitializerData( rowProcessingState );
	}

	@Override
	public CompletionStage<Void> reactiveResolveInstance(Object instance, EntityInitializerData original) {
		ReactiveEntityInitializerData data = (ReactiveEntityInitializerData) original;
		if ( instance == null ) {
			setMissing( data );
			return voidFuture();
		}
		data.setInstance( instance );
		final LazyInitializer lazyInitializer = extractLazyInitializer( data.getInstance() );
		final RowProcessingState rowProcessingState = data.getRowProcessingState();
		final SharedSessionContractImplementor session = rowProcessingState.getSession();
		if ( lazyInitializer == null ) {
			// Entity is most probably initialized
			data.setEntityInstanceForNotify( data.getInstance() );
			data.setConcreteDescriptor( session.getEntityPersister( null, data.getInstance() ) );
			resolveEntityKey( data, data.getConcreteDescriptor().getIdentifier( data.getInstance(), session ) );
			data.setEntityHolder( session.getPersistenceContextInternal().getEntityHolder( data.getEntityKey() ) );
			if ( data.getEntityHolder() == null ) {
				// Entity was most probably removed in the same session without setting the reference to null
				resolveKey( data );
				assert data.getState() == State.MISSING;
				assert getInitializedPart() instanceof ToOneAttributeMapping
						&& ( (ToOneAttributeMapping) getInitializedPart() ).getSideNature() == TARGET;
				return voidFuture();
			}
			// If the entity initializer is null, we know the entity is fully initialized,
			// otherwise it will be initialized by some other initializer
			data.setState( data.getEntityHolder().getEntityInitializer() == null ? State.INITIALIZED : State.RESOLVED );
		}
		else if ( lazyInitializer.isUninitialized() ) {
			data.setState( State.RESOLVED );
			// Read the discriminator from the result set if necessary
			EntityPersister persister = getDiscriminatorAssembler() == null
					? getEntityDescriptor()
					: determineConcreteEntityDescriptor( rowProcessingState, getDiscriminatorAssembler(), getEntityDescriptor() );
			data.setConcreteDescriptor( persister );
			assert data.getConcreteDescriptor() != null;
			resolveEntityKey( data, lazyInitializer.getIdentifier() );
			data.setEntityHolder( session.getPersistenceContextInternal().claimEntityHolderIfPossible(
					data.getEntityKey(),
					null,
					rowProcessingState.getJdbcValuesSourceProcessingState(),
					this
			) );
			// Resolve and potentially create the entity instance
			data.setEntityInstanceForNotify( resolveEntityInstance( data ) );
			lazyInitializer.setImplementation( data.getEntityInstanceForNotify() );
			registerLoadingEntity( data, data.getEntityInstanceForNotify() );
		}
		else {
			data.setState( State.INITIALIZED );
			data.setEntityInstanceForNotify( lazyInitializer.getImplementation() );
			data.setConcreteDescriptor( session.getEntityPersister( null, data.getEntityInstanceForNotify() ) );
			resolveEntityKey( data, lazyInitializer.getIdentifier() );
			data.setEntityHolder( session.getPersistenceContextInternal().getEntityHolder( data.getEntityKey() ) );
		}
		if ( getIdentifierAssembler() != null ) {
			final Initializer<?> initializer = getIdentifierAssembler().getInitializer();
			if ( initializer != null ) {
				initializer.resolveInstance( data.getEntityKey().getIdentifier(), rowProcessingState );
			}
		}
		upgradeLockMode( data );
		if ( data.getState() == State.INITIALIZED ) {
			registerReloadedEntity( data );
			resolveInstanceSubInitializers( data );
			if ( rowProcessingState.needsResolveState() ) {
				// We need to read result set values to correctly populate the query cache
				resolveState( data );
			}
		}
		else {
			resolveKeySubInitializers( data );
		}
		return voidFuture();
	}

	@Override
	public CompletionStage<Void> reactiveResolveInstance(EntityInitializerData original) {
		ReactiveEntityInitializerData data = (ReactiveEntityInitializerData) original;
		if ( data.getState() != State.KEY_RESOLVED ) {
			return voidFuture();
		}
		final RowProcessingState rowProcessingState = data.getRowProcessingState();
		data.setState( State.RESOLVED );
		if ( data.getEntityKey() == null ) {
			assert getIdentifierAssembler() != null;
			final Object id = getIdentifierAssembler().assemble( rowProcessingState );
			if ( id == null ) {
				setMissing( data );
				return voidFuture();
			}
			resolveEntityKey( data, id );
		}
		final PersistenceContext persistenceContext = rowProcessingState.getSession()
				.getPersistenceContextInternal();
		data.setEntityHolder( persistenceContext.claimEntityHolderIfPossible(
				data.getEntityKey(),
				null,
				rowProcessingState.getJdbcValuesSourceProcessingState(),
				this
		) );

		if ( useEmbeddedIdentifierInstanceAsEntity( data ) ) {
			data.setEntityInstanceForNotify( rowProcessingState.getEntityId() );
			data.setInstance( data.getEntityInstanceForNotify() );
		}
		else {
			resolveEntityInstance1( data );
			if ( data.getUniqueKeyAttributePath() != null ) {
				final SharedSessionContractImplementor session = rowProcessingState.getSession();
				final EntityPersister concreteDescriptor = getConcreteDescriptor( data );
				final EntityUniqueKey euk = new EntityUniqueKey(
						concreteDescriptor.getEntityName(),
						data.getUniqueKeyAttributePath(),
						rowProcessingState.getEntityUniqueKey(),
						data.getUniqueKeyPropertyTypes()[concreteDescriptor.getSubclassId()],
						session.getFactory()
				);
				session.getPersistenceContextInternal().addEntity( euk, getEntityInstance( data ) );
			}
		}

		if ( data.getInstance() != null ) {
			upgradeLockMode( data );
			if ( data.getState() == State.INITIALIZED ) {
				registerReloadedEntity( data );
				if ( rowProcessingState.needsResolveState() ) {
					// We need to read result set values to correctly populate the query cache
					resolveState( data );
				}
			}
			if ( data.getShallowCached() ) {
				initializeSubInstancesFromParent( data );
			}
		}
		return voidFuture();
	}
}
