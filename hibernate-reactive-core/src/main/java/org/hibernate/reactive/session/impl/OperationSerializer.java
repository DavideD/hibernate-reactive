/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.session.impl;

import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/**
 * Internal contract for session implementations that serialize
 * sequential async operations to prevent interleaving.
 */
public interface OperationSerializer {
	<T> CompletionStage<T> serialized(Supplier<CompletionStage<T>> operation);
}
