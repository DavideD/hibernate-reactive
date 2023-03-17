package org.hibernate.reactive.query.sqm.mutation.internal.temptable;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.dialect.temptable.TemporaryTableExporter;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.jdbc.AbstractWork;
import org.hibernate.reactive.adaptor.impl.PreparedStatementAdaptor;
import org.hibernate.reactive.logging.impl.Log;
import org.hibernate.reactive.logging.impl.LoggerFactory;
import org.hibernate.reactive.pool.ReactiveConnection;
import org.hibernate.reactive.session.ReactiveConnectionSupplier;
import org.hibernate.reactive.util.impl.CompletionStages;

/**
 * @see org.hibernate.dialect.temptable.TemporaryTableHelper
 */
public class ReactiveTemporaryTableHelper {
	private static final Log LOG = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Creation

	public static class TemporaryTableCreationWork extends AbstractWork {
		private final TemporaryTable temporaryTable;
		private final TemporaryTableExporter exporter;
		private final SessionFactoryImplementor sessionFactory;

		public TemporaryTableCreationWork(
				TemporaryTable temporaryTable,
				SessionFactoryImplementor sessionFactory) {
			this(
					temporaryTable,
					sessionFactory.getJdbcServices().getDialect().getTemporaryTableExporter(),
					sessionFactory
			);
		}

		public TemporaryTableCreationWork(
				TemporaryTable temporaryTable,
				TemporaryTableExporter exporter,
				SessionFactoryImplementor sessionFactory) {
			this.temporaryTable = temporaryTable;
			this.exporter = exporter;
			this.sessionFactory = sessionFactory;
		}

		@Override
		public void execute(Connection connection) {
			final JdbcServices jdbcServices = sessionFactory.getJdbcServices();

			try {
				final String creationCommand = exporter.getSqlCreateCommand( temporaryTable );
				logStatement( creationCommand, jdbcServices );

				try (Statement statement = connection.createStatement()) {
					statement.executeUpdate( creationCommand );
					jdbcServices.getSqlExceptionHelper().handleAndClearWarnings( statement, WARNING_HANDLER );
				}
				catch (SQLException e) {
					LOG.debugf(
							"unable to create temporary table [%s]; `%s` failed : %s",
							temporaryTable.getQualifiedTableName(),
							creationCommand,
							e.getMessage()
					);
				}
			}
			catch( Exception e ) {
				LOG.debugf( "Error creating temporary table(s) : %s", e.getMessage() );
			}
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Drop

	public static class TemporaryTableDropWork extends AbstractWork {
		private final TemporaryTable temporaryTable;
		private final TemporaryTableExporter exporter;
		private final SessionFactoryImplementor sessionFactory;

		public TemporaryTableDropWork(
				TemporaryTable temporaryTable,
				SessionFactoryImplementor sessionFactory) {
			this(
					temporaryTable,
					sessionFactory.getJdbcServices().getDialect().getTemporaryTableExporter(),
					sessionFactory
			);
		}

		public TemporaryTableDropWork(
				TemporaryTable temporaryTable,
				TemporaryTableExporter exporter,
				SessionFactoryImplementor sessionFactory) {
			this.temporaryTable = temporaryTable;
			this.exporter = exporter;
			this.sessionFactory = sessionFactory;
		}

		@Override
		public void execute(Connection connection) {
			final JdbcServices jdbcServices = sessionFactory.getJdbcServices();

			try {
				final String dropCommand = exporter.getSqlDropCommand( temporaryTable );
				logStatement( dropCommand, jdbcServices );

				try (Statement statement = connection.createStatement()) {
					statement.executeUpdate( dropCommand );
					jdbcServices.getSqlExceptionHelper().handleAndClearWarnings( statement, WARNING_HANDLER );
				}
				catch (SQLException e) {
					LOG.debugf(
							"unable to drop temporary table [%s]; `%s` failed : %s",
							temporaryTable.getQualifiedTableName(),
							dropCommand,
							e.getMessage()
					);
				}
			}
			catch( Exception e ) {
				LOG.debugf( "Error dropping temporary table(s) : %s", e.getMessage() );
			}
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Clean

	public static CompletionStage<Void> cleanTemporaryTableRows(
			TemporaryTable temporaryTable,
			TemporaryTableExporter exporter,
			Function<SharedSessionContractImplementor,String> sessionUidAccess,
			SharedSessionContractImplementor session) {
		final String sql = exporter.getSqlTruncateCommand( temporaryTable, sessionUidAccess, session );
		Object[] params = PreparedStatementAdaptor.bind( ps -> {
			if ( temporaryTable.getSessionUidColumn() != null ) {
				final String sessionUid = sessionUidAccess.apply( session );
				ps.setString( 1, sessionUid );
			}
		} );

		return reactiveConnection( session )
				.update( sql, params )
				.thenCompose( CompletionStages::voidFuture );
	}

	private static ReactiveConnection reactiveConnection(SharedSessionContractImplementor session) {
		return ( (ReactiveConnectionSupplier) session ).getReactiveConnection();
	}

	private static SqlExceptionHelper.WarningHandler WARNING_HANDLER = new SqlExceptionHelper.WarningHandlerLoggingSupport() {
		public boolean doProcess() {
			return LOG.isDebugEnabled();
		}

		public void prepare(SQLWarning warning) {
			LOG.warningsCreatingTempTable( warning );
		}

		@Override
		protected void logWarning(String description, String message) {
			LOG.debug( description );
			LOG.debug( message );
		}
	};


	private static void logStatement(String sql, JdbcServices jdbcServices) {
		final SqlStatementLogger statementLogger = jdbcServices.getSqlStatementLogger();
		statementLogger.logStatement( sql, FormatStyle.BASIC.getFormatter() );
	}
}
