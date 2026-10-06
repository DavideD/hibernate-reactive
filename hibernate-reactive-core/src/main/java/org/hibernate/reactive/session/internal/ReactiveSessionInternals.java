/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.session.internal;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.reactive.session.ReactiveQueryProducer;
import org.hibernate.reactive.session.ReactiveSession;

/**
 * Internal helpers for invoking session-scoped operations without going through
 * the public API.
 * <p>
 * {@link #enqueue} queues an operation behind any previously-started async
 * work on the same session.  {@link #internalReactiveFetch} fetches an
 * association bypassing that queue, for use inside already-queued operations.
 */
public final class ReactiveSessionInternals {

	private ReactiveSessionInternals() {
	}

	public static <T> CompletionStage<T> enqueue(
			SharedSessionContractImplementor session,
			Supplier<CompletionStage<T>> operation) {
		if ( session instanceof ReactiveSessionImpl impl ) {
			return impl.enqueue( operation );
		}
		if ( session instanceof ReactiveStatelessSessionImpl impl ) {
			return impl.enqueue( operation );
		}
		return operation.get();
	}

	/**
	 * Flush without going through the serialization queue — for use inside
	 * already-enqueued operations (e.g. native query execution).
	 */
	public static CompletionStage<Void> internalReactiveFlush(
			SharedSessionContractImplementor session) {
		if ( session instanceof ReactiveSessionImpl impl ) {
			return impl.doFlush();
		}
		return CompletableFuture.completedStage( null );
	}

	public static <T> CompletionStage<T> internalReactiveGet(
			SharedSessionContractImplementor session,
			Class<T> entityClass,
			Object id) {
		if ( session instanceof ReactiveSessionImpl impl ) {
			return impl.internalReactiveGet( entityClass, id );
		}
		return ( (ReactiveSession) session ).reactiveGet( entityClass, id );
	}

	public static CompletionStage<Void> internalReactiveLock(
			SharedSessionContractImplementor session,
			String entityName,
			Object entity,
			LockOptions lockOptions) {
		if ( session instanceof ReactiveSessionImpl impl ) {
			return impl.internalReactiveLock( entityName, entity, lockOptions );
		}
		return ( (ReactiveSession) session ).reactiveLock( entityName, entity, lockOptions );
	}

	public static <T> CompletionStage<T> internalReactiveFetch(
			SharedSessionContractImplementor session,
			T association,
			boolean unproxy) {
		if ( session instanceof ReactiveSessionImpl impl ) {
			return impl.internalReactiveFetch( association, unproxy );
		}
		if ( session instanceof ReactiveStatelessSessionImpl impl ) {
			return impl.internalReactiveFetch( association, unproxy );
		}
		return ( (ReactiveQueryProducer) session ).reactiveFetch( association, unproxy );
	}
}
