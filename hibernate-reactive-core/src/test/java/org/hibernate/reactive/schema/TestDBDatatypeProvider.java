/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.schema;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.reactive.containers.DatabaseConfiguration.DBType;

public interface TestDBDatatypeProvider {
	enum DATATYPE {
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

	String tableVar = "$table";
	String columnVar = "$column";

	Map<DATATYPE, String> pgExpectedResultsMap = new HashMap<>();
	Map<DATATYPE, String> mySqlExpectedResultsMap = new HashMap<>();
	Map<DATATYPE, String> sqlServerExpectedResultsMap = new HashMap<>();
	Map<DATATYPE, String> db2ExpectedResultsMap = new HashMap<>();

	String pgColumnTypeBaseQuery = "select data_type from information_schema.columns where table_name = 'testentity' and column_name = '" + columnVar + "'";
	String mySqlColumnTypeBaseQuery = "select data_type from information_schema.columns where table_name = 'TestEntity' and column_name = '" + columnVar + "'";
	String sqlServerColumnTypeBaseQuery = pgColumnTypeBaseQuery;
	String db2ColumnTypeBaseQuery = "SELECT TYPENAME FROM SYSCAT.COLUMNS where TABNAME = 'TESTENTITY' and COLNAME = '" + columnVar + "'";

	static void loadMaps() {
		pgExpectedResultsMap.put(DATATYPE.TEXT, "text");
		pgExpectedResultsMap.put(DATATYPE.CHARACTER, "character");
		pgExpectedResultsMap.put(DATATYPE.SERIALIZABLE, "bytea");
		pgExpectedResultsMap.put(DATATYPE.BOOLEAN, "boolean");
		pgExpectedResultsMap.put(DATATYPE.BLOB, "oid");
		pgExpectedResultsMap.put(DATATYPE.BIGDECIMAL, "numeric");
		pgExpectedResultsMap.put(DATATYPE.LONG, "bigint");
		pgExpectedResultsMap.put(DATATYPE.INTEGER, "integer");
		pgExpectedResultsMap.put(DATATYPE.FLOAT, "real");
		pgExpectedResultsMap.put(DATATYPE.DOUBLE, "double precision");

		sqlServerExpectedResultsMap.put(DATATYPE.TEXT, "text");
		sqlServerExpectedResultsMap.put(DATATYPE.CHARACTER, "text");
		sqlServerExpectedResultsMap.put(DATATYPE.SERIALIZABLE, "text");
		sqlServerExpectedResultsMap.put(DATATYPE.BOOLEAN, "text");
		sqlServerExpectedResultsMap.put(DATATYPE.BLOB, "text");
		sqlServerExpectedResultsMap.put(DATATYPE.BIGDECIMAL, "numeric");
		sqlServerExpectedResultsMap.put(DATATYPE.LONG, "bigint");
		sqlServerExpectedResultsMap.put(DATATYPE.INTEGER, "int");
		sqlServerExpectedResultsMap.put(DATATYPE.FLOAT, "float");
		sqlServerExpectedResultsMap.put(DATATYPE.DOUBLE, "float");

		db2ExpectedResultsMap.put(DATATYPE.TEXT, "text");
		db2ExpectedResultsMap.put(DATATYPE.BIGDECIMAL, "DECIMAL");
		db2ExpectedResultsMap.put(DATATYPE.LONG, "BIGINT");
		db2ExpectedResultsMap.put(DATATYPE.INTEGER, "INTEGER");
		db2ExpectedResultsMap.put(DATATYPE.FLOAT, "DOUBLE");
		db2ExpectedResultsMap.put(DATATYPE.DOUBLE, "DOUBLE");

	}

	static String getDatatypeQuery(DBType dbType, String actualTableName, String actualColumnName) {
		if( pgExpectedResultsMap.isEmpty()) {
			loadMaps();
		}

		String tableName = getDBTableName(dbType, actualTableName );
		String columnName = getDBTableName(dbType, actualColumnName );

		String result = null;

		switch( dbType ) {
			case MYSQL: {
				result = mySqlColumnTypeBaseQuery.replace( tableVar, tableName ).replace( columnVar, columnName );
			} break;
			case SQLSERVER: {
				result =  sqlServerColumnTypeBaseQuery.replace( tableVar, tableName ).replace( columnVar, columnName );
			} break;
			case DB2: {
				result =  db2ColumnTypeBaseQuery.replace( tableVar, tableName ).replace( columnVar, columnName );
			} break;
			case POSTGRESQL:
			default: {
				result =  pgColumnTypeBaseQuery.replace( tableVar, tableName ).replace( columnVar, columnName );
			} break;
		}

		System.out.println("   >>>  query = " + result);
		return result;
	}

	static String getDBColumnName( DBType dbType, String actualColumnName ) {
		switch( dbType ) {
			case MYSQL: return actualColumnName;
			case DB2: return actualColumnName.toUpperCase();
			case POSTGRESQL:
			case SQLSERVER:
			default: return actualColumnName.toLowerCase();
		}
	}

	static String getDBTableName( DBType dbType, String actualTableName ) {
		switch( dbType ) {
			case MYSQL: return actualTableName;
			case DB2: return actualTableName.toUpperCase();
			case POSTGRESQL:
			case SQLSERVER:
			default: return actualTableName.toLowerCase();
		}
	}


	static String getExpectedResult(DATATYPE datatype, DBType dbType) {
		if( pgExpectedResultsMap.isEmpty()) {
			loadMaps();
		}

		switch( dbType ) {
			case MYSQL: return (String) mySqlExpectedResultsMap.get(datatype);
			case SQLSERVER: return (String) sqlServerExpectedResultsMap.get(datatype);
			case DB2: return (String) db2ExpectedResultsMap.get(datatype);
			case POSTGRESQL:
			default: return (String) pgExpectedResultsMap.get(datatype);
		}
	}

}
