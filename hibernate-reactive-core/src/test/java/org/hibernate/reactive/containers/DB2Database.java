/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.containers;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import org.hibernate.type.NumericBooleanConverter;
import org.hibernate.type.TrueFalseConverter;
import org.hibernate.type.YesNoConverter;

import org.testcontainers.containers.Db2Container;

import static org.hibernate.reactive.containers.DockerImage.fromDockerfile;

class DB2Database implements TestableDatabase {

	public static DB2Database INSTANCE = new DB2Database();

	public static Map<Class<?>, String> expectedDBTypeForClass = new HashMap<>();

	static {{
		expectedDBTypeForClass.put( boolean.class, "BOOLEAN" );
		expectedDBTypeForClass.put( Boolean.class, "BOOLEAN" );

		expectedDBTypeForClass.put( NumericBooleanConverter.class, "INTEGER" );
		expectedDBTypeForClass.put( YesNoConverter.class, "CHARACTER" );
		expectedDBTypeForClass.put( TrueFalseConverter.class, "CHARACTER" );
		expectedDBTypeForClass.put( byte[].class, "VARBINARY" );
		// expectedDBTypeForClass.put( TextType.class, "VARCHAR" );

		expectedDBTypeForClass.put( int.class, "INTEGER" );
		expectedDBTypeForClass.put( Integer.class, "INTEGER" );
		expectedDBTypeForClass.put( long.class, "BIGINT" );
		expectedDBTypeForClass.put( Long.class, "BIGINT" );
		expectedDBTypeForClass.put( float.class, "REAL" );
		expectedDBTypeForClass.put( Float.class, "REAL" );
		expectedDBTypeForClass.put( double.class, "DOUBLE" );
		expectedDBTypeForClass.put( Double.class, "DOUBLE" );
		expectedDBTypeForClass.put( byte.class, "SMALLINT" );
		expectedDBTypeForClass.put( Byte.class, "SMALLINT" );
		expectedDBTypeForClass.put( URL.class, "VARCHAR" );
		expectedDBTypeForClass.put( TimeZone.class, "VARCHAR" );
		expectedDBTypeForClass.put( Date.class, "DATE" );
		expectedDBTypeForClass.put( Timestamp.class, "TIMESTAMP" );
		expectedDBTypeForClass.put( Time.class, "TIME" );
		expectedDBTypeForClass.put( LocalDate.class, "DATE" );
		expectedDBTypeForClass.put( LocalTime.class, "TIME" );
		expectedDBTypeForClass.put( LocalDateTime.class, "TIMESTAMP" );
		expectedDBTypeForClass.put( BigInteger.class, "DECIMAL" );
		expectedDBTypeForClass.put( BigDecimal.class, "DECIMAL" );
		expectedDBTypeForClass.put( Serializable.class, "VARBINARY" );
		expectedDBTypeForClass.put( UUID.class, "VARCHAR" );
		expectedDBTypeForClass.put( Instant.class, "TIMESTAMP" );
		expectedDBTypeForClass.put( Duration.class, "BIGINT" );
		expectedDBTypeForClass.put( Character.class, "CHARACTER" );
		expectedDBTypeForClass.put( char.class, "CHARACTER" );
		expectedDBTypeForClass.put( String.class, "VARCHAR" );
		expectedDBTypeForClass.put( String[].class, "XML" );
		expectedDBTypeForClass.put( Long[].class, "XML" );
		expectedDBTypeForClass.put( BigDecimal[].class, "XML" );
		expectedDBTypeForClass.put( BigInteger[].class, "XML" );
		expectedDBTypeForClass.put( Boolean[].class, "XML" );
	}}

	/**
	 * Holds configuration for the DB2 database container. If the build is run with <code>-Pdocker</code> then
	 * Testcontainers+Docker will be used.
	 * <p>
	 * TIP: To reuse the same containers across multiple runs, set `testcontainers.reuse.enable=true` in a file located
	 * at `$HOME/.testcontainers.properties` (create the file if it does not exist).
	 */
	static final Db2Container db2 = new Db2Container( fromDockerfile( "db2" ) )
			.withUsername( DatabaseConfiguration.USERNAME )
			.withPassword( DatabaseConfiguration.PASSWORD )
			.withDatabaseName( DatabaseConfiguration.DB_NAME )
			.withLogConsumer( of -> System.out.println( of.getUtf8String() ) )
			.acceptLicense()
			.withReuse( true );

	private String getRegularJdbcUrl() {
		return "jdbc:db2://localhost:50000/" + db2.getDatabaseName();
	}

	@Override
	public String getJdbcUrl() {
		return buildJdbcUrlWithCredentials( address() );
	}

	@Override
	public String getUri() {
		return buildUriWithCredentials( address() );
	}

	@Override
	public String getNativeDatatypeQuery(String tableName, String columnName) {
		return "SELECT TYPENAME FROM SYSCAT.COLUMNS where TABNAME = '" + tableName.toUpperCase() + "' and COLNAME = '" + columnName.toUpperCase() + "'";
	}

	@Override
	public String getExpectedNativeDatatype(Class<?> dataType) {
		return expectedDBTypeForClass.get( dataType );
	}

	private String address() {
		if ( DatabaseConfiguration.USE_DOCKER ) {
			// Calling start() will start the container (if not already started)
			// It is required to call start() before obtaining the JDBC URL because it will contain a randomized port
			db2.start();
			return db2.getJdbcUrl();
		}

		return getRegularJdbcUrl();
	}

	@Override
	public String jdbcStartQuery() {
		return ":";
	}

	@Override
	public String jdbcParamDelimiter() {
		return ";";
	}

	private static String buildJdbcUrlWithCredentials(String jdbcUrl) {
		return jdbcUrl + ":user=" + db2.getUsername() + ";password=" + db2.getPassword() + ";";
	}

	private static String buildUriWithCredentials(String jdbcUrl) {
		return "db2://" + db2.getUsername() + ":" + db2.getPassword() + "@" + jdbcUrl.substring(11);
	}

	private DB2Database() {
	}

}
