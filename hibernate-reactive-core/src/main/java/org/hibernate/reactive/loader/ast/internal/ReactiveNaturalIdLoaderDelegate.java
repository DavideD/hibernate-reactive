/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.loader.ast.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.internal.AbstractNaturalIdLoader;
import org.hibernate.loader.ast.internal.LoaderSelectBuilder;
import org.hibernate.loader.ast.internal.LoaderSqlAstCreationState;
import org.hibernate.loader.ast.internal.NoCallbackExecutionContext;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.reactive.loader.ast.spi.ReactiveNaturalIdLoader;
import org.hibernate.reactive.sql.exec.internal.StandardReactiveSelectExecutor;
import org.hibernate.reactive.sql.results.spi.ReactiveListResultsConsumer;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

public class ReactiveNaturalIdLoaderDelegate<T> extends AbstractNaturalIdLoader<CompletionStage<T>> implements ReactiveNaturalIdLoader<T> {
    public ReactiveNaturalIdLoaderDelegate(
            NaturalIdMapping naturalIdMapping,
            EntityMappingType entityDescriptor) {
        super( naturalIdMapping, entityDescriptor );
    }

    @Override
    protected void applyNaturalIdRestriction(
            Object bindValue,
            TableGroup rootTableGroup,
            Consumer consumer,
            BiConsumer jdbcParameterConsumer,
            LoaderSqlAstCreationState sqlAstCreationState,
            SharedSessionContractImplementor session) {

    }

    @Override
    public CompletionStage<Object> reactiveResolveNaturalIdToId(
            Object naturalIdValue,
            SharedSessionContractImplementor session) {
        return null;
    }

    @Override
    public CompletionStage<Object> reactiveResolveIdToNaturalId(Object id, SharedSessionContractImplementor session) {
        final SessionFactoryImplementor sessionFactory = session.getFactory();

        final List<JdbcParameter> jdbcParameters = new ArrayList<>();
        final SelectStatement sqlSelect = LoaderSelectBuilder.createSelect(
                entityDescriptor(),
                Collections.singletonList( naturalIdMapping() ),
                entityDescriptor().getIdentifierMapping(),
                null,
                1,
                session.getLoadQueryInfluencers(),
                LockOptions.NONE,
                jdbcParameters::add,
                sessionFactory
        );

        final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
        final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
        final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcEnvironment.getSqlAstTranslatorFactory();

        final JdbcParameterBindings jdbcParamBindings = new JdbcParameterBindingsImpl( jdbcParameters.size() );
        int offset = jdbcParamBindings.registerParametersForEachJdbcValue(
                id,
                entityDescriptor().getIdentifierMapping(),
                jdbcParameters,
                session
        );
        assert offset == jdbcParameters.size();

        final JdbcOperationQuerySelect jdbcSelect = sqlAstTranslatorFactory.buildSelectTranslator(
                        sessionFactory,
                        sqlSelect
                )
                .translate( jdbcParamBindings, QueryOptions.NONE );
        return StandardReactiveSelectExecutor.INSTANCE
                .list(
                        jdbcSelect,
                        jdbcParamBindings,
                        new NoCallbackExecutionContext( session ),
                        row -> {
                            // because we select the natural-id we want to "reduce" the result
                            assert row.length == 1;
                            return row[0];
                        },
                        ReactiveListResultsConsumer.UniqueSemantic.FILTER
                )
                .thenApply( results -> {
                    if ( results.isEmpty() ) {
                        return null;
                    }

                    if ( results.size() > 1 ) {
                        throw new HibernateException(
                                String.format(
                                        "Resolving id to natural-id returned more that one row : %s #%s",
                                        entityDescriptor().getEntityName(),
                                        id
                                )
                        );
                    }
                    return results.get( 0 );
                } );
    }
}
