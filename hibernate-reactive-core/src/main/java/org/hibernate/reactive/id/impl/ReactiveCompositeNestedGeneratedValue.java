package org.hibernate.reactive.id.impl;

import java.util.concurrent.CompletionStage;

import org.hibernate.reactive.id.ReactiveIdentifierGenerator;
import org.hibernate.reactive.session.ReactiveConnectionSupplier;

/**
 * @see org.hibernate.id.CompositeNestedGeneratedValueGenerator
 */
public class ReactiveCompositeNestedGeneratedValue implements ReactiveIdentifierGenerator {
	public ReactiveCompositeNestedGeneratedValue(...) {
		// TODO:
	}

	@Override
	public CompletionStage<Object> generate(ReactiveConnectionSupplier session, Object entity) {
		// TODO:
	}
}
