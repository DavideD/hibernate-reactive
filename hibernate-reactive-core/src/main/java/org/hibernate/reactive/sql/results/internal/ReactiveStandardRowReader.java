/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.sql.results.internal;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.query.named.RowReaderMemento;
import org.hibernate.reactive.logging.impl.Log;
import org.hibernate.reactive.logging.impl.LoggerFactory;
import org.hibernate.reactive.sql.results.spi.ReactiveRowReader;
import org.hibernate.reactive.util.impl.CompletionStages;
import org.hibernate.sql.results.LoadingLogger;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.internal.InitializersList;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingState;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.sql.results.spi.RowTransformer;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @see org.hibernate.sql.results.internal.StandardRowReader
 */
public class ReactiveStandardRowReader<R> implements ReactiveRowReader<R> {

	private final List<DomainResultAssembler<?>> resultAssemblers;
	private final ReactiveInitializersList initializers;
	private final RowTransformer<R> rowTransformer;
	private final Class<R> domainResultJavaType;

	private final int assemblerCount;

	public ReactiveStandardRowReader(
			List<DomainResultAssembler<?>> resultAssemblers,
			ReactiveInitializersList initializers,
			RowTransformer<R> rowTransformer,
			Class<R> domainResultJavaType) {
		this.resultAssemblers = resultAssemblers;
		this.initializers = initializers;
		this.rowTransformer = rowTransformer;
		this.assemblerCount = resultAssemblers.size();
		this.domainResultJavaType = domainResultJavaType;
	}

	@Override
	public CompletionStage<R> reactiveReadRow(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) {
		LoadingLogger.LOGGER.trace( "StandardRowReader#readRow" );

		coordinateInitializers( rowProcessingState );

		final Object[] resultRow = new Object[assemblerCount];

		for ( int i = 0; i < assemblerCount; i++ ) {
			final DomainResultAssembler assembler = resultAssemblers.get( i );
			LoadingLogger.LOGGER.debugf( "Calling top-level assembler (%s / %s) : %s", i, assemblerCount, assembler );
			resultRow[i] = assembler.assemble( rowProcessingState, options );
		}

		afterRow( rowProcessingState );

		return CompletionStages.completedFuture( rowTransformer.transformRow( resultRow ) );

	}

	@Override
	public Class<R> getDomainResultResultJavaType() {
		return domainResultJavaType;
	}

	@Override
	public Class<?> getResultJavaType() {
		if ( resultAssemblers.size() == 1 ) {
			return resultAssemblers.get( 0 ).getAssembledJavaType().getJavaTypeClass();
		}

		return Object[].class;
	}

	@Override
	public List<JavaType<?>> getResultJavaTypes() {
		List<JavaType<?>> javaTypes = new ArrayList<>( resultAssemblers.size() );
		for ( DomainResultAssembler resultAssembler : resultAssemblers ) {
			javaTypes.add( resultAssembler.getAssembledJavaType() );
		}
		return javaTypes;
	}

	@Override
	public ReactiveInitializersList getReactiveInitializersList() {
		return initializers;
	}

	@Override
	public R readRow(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) {
		throw LOG.nonReactiveMethodCall( "reactiveRowReader" );
	}

	private void afterRow(RowProcessingState rowProcessingState) {
		LOG.trace( "ReactiveStandardRowReader#afterRow" );
		initializers.finishUpRow( rowProcessingState );
	}

	@SuppressWarnings("ForLoopReplaceableByForEach")
	private void coordinateInitializers(RowProcessingState rowProcessingState) {
		initializers.resolveKeys( rowProcessingState );
		initializers.resolveInstances( rowProcessingState );
		initializers.initializeInstance( rowProcessingState );
	}

	@Override
	@SuppressWarnings("ForLoopReplaceableByForEach")
	public void finishUp(JdbcValuesSourceProcessingState processingState) {
		initializers.endLoading( processingState.getExecutionContext() );
	}

	@Override
	public RowReaderMemento toMemento(SessionFactoryImplementor factory) {
		return new RowReaderMemento() {
			@Override
			public Class<?>[] getResultClasses() {
				return ArrayHelper.EMPTY_CLASS_ARRAY;
			}

			@Override
			public String[] getResultMappingNames() {
				return ArrayHelper.EMPTY_STRING_ARRAY;
			}
		};
	}
}
