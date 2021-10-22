package org.hibernate.reactive;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.cfg.Configuration;
import org.hibernate.reactive.mutiny.impl.MutinySessionImpl;
import org.hibernate.reactive.mutiny.impl.MutinyStatelessSessionImpl;
import org.hibernate.reactive.pool.BatchingConnection;
import org.hibernate.reactive.pool.impl.SqlClientConnection;
import org.hibernate.reactive.stage.impl.StageSessionImpl;
import org.hibernate.reactive.stage.impl.StageStatelessSessionImpl;

import org.junit.After;
import org.junit.Test;

import io.vertx.ext.unit.TestContext;

import static org.assertj.core.api.Assertions.assertThat;

public class BatchSizeTest extends BaseReactiveTest {

	@Override
	protected Configuration constructConfiguration() {
		Configuration configuration = super.constructConfiguration();
		configuration.addAnnotatedClass( Cell.class );
		return configuration;
	}

	@After
	public void cleanDB(TestContext context) {
		test( context, deleteEntities( Cell.class ) );
	}

	@Test
	public void testBatchingConnection(TestContext context) {
		test( context, openSession()
				.thenAccept( session -> {
					assertThat( ( (StageSessionImpl) session ).getReactiveConnection() ).isInstanceOf( SqlClientConnection.class );
					session.setBatchSize( 200 );
					assertThat( ( (StageSessionImpl) session ).getReactiveConnection() ).isInstanceOf( BatchingConnection.class );
				} )
		);
	}

	@Test
	public void testBatchingConnectionWithStateless(TestContext context) {
		test( context, openStatelessSession()
				.thenAccept( session -> {
					assertThat( ( (StageStatelessSessionImpl) session ).getReactiveConnection() ).isInstanceOf( SqlClientConnection.class );
					( (StageStatelessSessionImpl) session ).setBatchSize( 200 );
					assertThat( ( (StageSessionImpl) session ).getReactiveConnection() ).isInstanceOf( BatchingConnection.class );
				} )
		);
	}

	@Test
	public void testBatchingConnectionMutiny(TestContext context) {
		test( context, openMutinySession()
				.invoke( session -> assertThat( ( (MutinySessionImpl) session ).getReactiveConnection() )
						.isInstanceOf( SqlClientConnection.class ) )
		);
	}

	@Test
	public void testBatchingConnectionWithMutinyStateless(TestContext context) {
		test( context, openMutinyStatelessSession()
				.invoke( session -> assertThat( ( (MutinyStatelessSessionImpl) session ).getReactiveConnection() )
						.isInstanceOf( SqlClientConnection.class ) )
		);
	}

	@Entity
	static class Cell {
		@Id
		Long id;
	}
}
