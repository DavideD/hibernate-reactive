/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.schema;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.reactive.containers.DatabaseConfiguration.DBType;

//TODO: Remove this interface and  move things in TestableDatabase
public interface TestDBDatatypeProvider {
	enum DataType {
		BIGDECIMAL,
		LONG,
		INTEGER,
		DOUBLE,
		FLOAT,
		BOOLEAN,
		CHARACTER,
		TEXT,
		SERIALIZABLE,
		BLOB
	}

	String TABLE_PARAM = "$table";
	String COLUMN_PARAM = "$column";

	Map<DataType, String> pgExpectedResultsMap = new EnumMap<DataType, String>( DataType.class) {{
		pgExpectedResultsMap.put( DataType.TEXT, "text");
		pgExpectedResultsMap.put( DataType.CHARACTER, "character");
		pgExpectedResultsMap.put( DataType.SERIALIZABLE, "bytea");
		pgExpectedResultsMap.put( DataType.BOOLEAN, "boolean");
		pgExpectedResultsMap.put( DataType.BLOB, "oid");
		pgExpectedResultsMap.put( DataType.BIGDECIMAL, "numeric");
		pgExpectedResultsMap.put( DataType.LONG, "bigint");
		pgExpectedResultsMap.put( DataType.INTEGER, "integer");
		pgExpectedResultsMap.put( DataType.FLOAT, "real");
		pgExpectedResultsMap.put( DataType.DOUBLE, "double precision");
	}};

	Map<DataType, String> mySqlExpectedResultsMap = new HashMap<>();
	Map<DataType, String> sqlServerExpectedResultsMap = new HashMap<>();
	Map<DataType, String> db2ExpectedResultsMap = new HashMap<>();

	String pgColumnTypeBaseQuery = "select data_type from information_schema.columns where table_name = '" + TABLE_PARAM + "' and column_name = '" + COLUMN_PARAM + "'";
	// FIXME: Use TABLE_PARAM for the name of the table
	String mySqlColumnTypeBaseQuery = "select data_type from information_schema.columns where table_name = 'TestEntity' and column_name = '" + COLUMN_PARAM + "'";
	String sqlServerColumnTypeBaseQuery = pgColumnTypeBaseQuery;
	String db2ColumnTypeBaseQuery = "SELECT TYPENAME FROM SYSCAT.COLUMNS where TABNAME = 'TESTENTITY' and COLNAME = '" + COLUMN_PARAM + "'";

	//TODO: Remove loadmaps and initialize maps during construction
	static void loadMaps() {
		sqlServerExpectedResultsMap.put( DataType.TEXT, "text");
		sqlServerExpectedResultsMap.put( DataType.CHARACTER, "text");
		sqlServerExpectedResultsMap.put( DataType.SERIALIZABLE, "text");
		sqlServerExpectedResultsMap.put( DataType.BOOLEAN, "text");
		sqlServerExpectedResultsMap.put( DataType.BLOB, "text");
		sqlServerExpectedResultsMap.put( DataType.BIGDECIMAL, "numeric");
		sqlServerExpectedResultsMap.put( DataType.LONG, "bigint");
		sqlServerExpectedResultsMap.put( DataType.INTEGER, "int");
		sqlServerExpectedResultsMap.put( DataType.FLOAT, "float");
		sqlServerExpectedResultsMap.put( DataType.DOUBLE, "float");

		db2ExpectedResultsMap.put( DataType.TEXT, "text");
		db2ExpectedResultsMap.put( DataType.BIGDECIMAL, "DECIMAL");
		db2ExpectedResultsMap.put( DataType.LONG, "BIGINT");
		db2ExpectedResultsMap.put( DataType.INTEGER, "INTEGER");
		db2ExpectedResultsMap.put( DataType.FLOAT, "DOUBLE");
		db2ExpectedResultsMap.put( DataType.DOUBLE, "DOUBLE");

	}

	static String getDatatypeQuery(DBType dbType, String actualTableName, String actualColumnName) {
		String tableName = parseValue(dbType, actualTableName );
		String columnName = parseValue(dbType, actualColumnName );

		switch( dbType ) {
			case MYSQL:
				return mySqlColumnTypeBaseQuery.replace( TABLE_PARAM, tableName ).replace( COLUMN_PARAM, columnName );
			case SQLSERVER:
				return sqlServerColumnTypeBaseQuery.replace( TABLE_PARAM, tableName ).replace( COLUMN_PARAM, columnName );
			case DB2:
				return db2ColumnTypeBaseQuery.replace( TABLE_PARAM, tableName ).replace( COLUMN_PARAM, columnName );
			case POSTGRESQL:
				return pgColumnTypeBaseQuery.replace( TABLE_PARAM, tableName ).replace( COLUMN_PARAM, columnName );
			default:
				throw new IllegalArgumentException("Db not recognize");
		}
	}

	static String parseValue( DBType dbType, String actualColumnName ) {
		switch( dbType ) {
			case MYSQL: return actualColumnName;
			case DB2: return actualColumnName.toUpperCase();
			case POSTGRESQL:
			case SQLSERVER:
			default: return actualColumnName.toLowerCase();
		}
	}

	static String getExpectedResult(DataType datatype, DBType dbType) {
		switch( dbType ) {
			case MYSQL: return mySqlExpectedResultsMap.get(datatype);
			case SQLSERVER: return sqlServerExpectedResultsMap.get(datatype);
			case DB2: return db2ExpectedResultsMap.get(datatype);
			case POSTGRESQL:
			default: return pgExpectedResultsMap.get(datatype);
		}
	}

}
