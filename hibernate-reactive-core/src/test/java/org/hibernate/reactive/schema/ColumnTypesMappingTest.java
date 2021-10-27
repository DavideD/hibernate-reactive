/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.schema;

import org.hibernate.cfg.Configuration;
import org.hibernate.reactive.BaseReactiveTest;
import org.hibernate.reactive.containers.TestableDatabase;
import org.hibernate.reactive.provider.Settings;
import org.hibernate.reactive.testing.DatabaseSelectionRule;

import org.junit.Rule;
import org.junit.Test;

import io.vertx.ext.unit.TestContext;

import static org.hibernate.reactive.containers.DatabaseConfiguration.DBType.DB2;
import static org.hibernate.reactive.containers.DatabaseConfiguration.getDatatypeQuery;
import static org.hibernate.reactive.containers.DatabaseConfiguration.getExpectedDatatype;

/**
 * Check that each property is mapped as the expected type in the database.
 */
public class ColumnTypesMappingTest extends BaseReactiveTest {

	@Rule
	public DatabaseSelectionRule dbRule = DatabaseSelectionRule.skipTestsFor( DB2 );

	@Override
	protected Configuration constructConfiguration() {
		Configuration configuration = super.constructConfiguration();
		configuration.setProperty( Settings.HBM2DDL_AUTO, "update" );
		configuration.addAnnotatedClass( BasicTypesTestEntity.class );
		return configuration;
	}

	private void testDatatype(TestContext context, String columnName, TestableDatabase.DataType datatype) {
		BasicTypesTestEntity testEntity = new BasicTypesTestEntity("Testing: " + columnName);
		test( context, getSessionFactory()
				.withTransaction( (session, t) -> session.persist( testEntity ) )
				.thenCompose( v1 -> openSession()
								.thenCompose( s -> s
										.find( BasicTypesTestEntity.class, testEntity.id )
										.thenAccept( result -> context.assertEquals( testEntity.name, result.name ) )
										.thenCompose( v -> s
												.createNativeQuery( getDatatypeQuery( BasicTypesTestEntity.TABLE_NAME, columnName ), String.class )
												.getSingleResult()
												.thenAccept( result -> context.assertEquals( getExpectedDatatype( datatype ), result ) )
										)
								)
				)
		);
	}

	@Test
	public void testBigDecimal(TestContext context) {
		testDatatype( context, "bigDecimal", TestableDatabase.DataType.BIGDECIMAL);
	}

	@Test
	public void testStringType(TestContext context) {
		testDatatype( context, "aString", TestableDatabase.DataType.STRING );
	}

	@Test
	public void testIntegerFieldType(TestContext context) {
		testDatatype( context, "fieldInteger", TestableDatabase.DataType.INTEGER_FIELD );
	}

	@Test
	public void testIntegerPrimativeType(TestContext context) {
		testDatatype( context, "primativeInt", TestableDatabase.DataType.INT_PRIMATIVE );
	}

	@Test
	public void testBigIntegerType(TestContext context) {
		testDatatype( context, "bigInteger", TestableDatabase.DataType.BIGINTEGER );
	}

	@Test
	public void testLongFieldType(TestContext context) {
		testDatatype( context, "fieldLong", TestableDatabase.DataType.LONG_FIELD );
	}

	@Test
	public void testLongPrimativeType(TestContext context) {
		testDatatype( context, "primativeLong", TestableDatabase.DataType.LONG_PRIMATIVE );
	}

	@Test
	public void testFloatFieldType(TestContext context) {
		testDatatype( context, "fieldFloat", TestableDatabase.DataType.FLOAT_FIELD );
	}

	@Test
	public void testFloatPrimativeType(TestContext context) {
		testDatatype( context, "primativeFloat", TestableDatabase.DataType.FLOAT_PRIMATIVE );
	}


	@Test
	public void testDoubleFieldType(TestContext context) {
		testDatatype( context, "fieldDouble", TestableDatabase.DataType.DOUBLE_FIELD );
	}

	@Test
	public void testDoublePrimativeType(TestContext context) {
		testDatatype( context, "primativeDouble", TestableDatabase.DataType.DOUBLE_PRIMATIVE );
	}

	@Test
	public void testBooleanPrimativeType(TestContext context) {
		testDatatype( context, "primativeBoolean", TestableDatabase.DataType.BOOLEAN_PRIMATIVE );
	}

	@Test
	public void testBooleanFieldType(TestContext context) {
		testDatatype( context, "fieldBoolean", TestableDatabase.DataType.BOOLEAN_FIELD );
	}

	@Test
	public void testBooleanTrueFalseType(TestContext context) {
		testDatatype( context, "booleanTrueFalse", TestableDatabase.DataType.CHARACTER );
	}

	@Test
	public void testBooleanYesNoType(TestContext context) {
		testDatatype( context, "booleanYesNo", TestableDatabase.DataType.BOOLEAN_YES_NO );
	}

	@Test
	public void testBooleanNumericType(TestContext context) {
		testDatatype( context, "booleanNumeric", TestableDatabase.DataType.BOOLEAN_NUMERIC );
	}

	@Test
	public void testBytePrimativeType(TestContext context) {
		testDatatype( context, "primativeByte", TestableDatabase.DataType.BYTE_PRIMATIVE );
	}

	@Test
	public void testBytesPrimativeType(TestContext context) {
		testDatatype( context, "primativeBytes", TestableDatabase.DataType.BYTES_PRIMATIVE );
	}

	@Test
	public void testByteFieldType(TestContext context) {
		testDatatype( context, "fieldByte", TestableDatabase.DataType.BYTE_FIELD );
	}

	@Test
	public void testUrlType(TestContext context) {
		testDatatype( context, "url", TestableDatabase.DataType.URL );
	}

	@Test
	public void testDateType(TestContext context) {
		testDatatype( context, "date", TestableDatabase.DataType.DATE_TEMPORAL_TYPE );
	}

	@Test
	public void testDateAsTimestampType(TestContext context) {
		testDatatype( context, "dateAsTimestamp", TestableDatabase.DataType.DATE_AS_TIMESTAMP_TEMPORAL_TYPE );
	}

	@Test
	public void testTimeZoneType(TestContext context) {
		testDatatype( context, "timeZone", TestableDatabase.DataType.TIMEZONE );
	}

	@Test
	public void testCalendarAsDateType(TestContext context) {
		testDatatype( context, "calendarAsDate", TestableDatabase.DataType.DATE_TEMPORAL_TYPE );
	}

	@Test
	public void testCalendarAsTimestampType(TestContext context) {
		testDatatype( context, "calendarAsTimestamp", TestableDatabase.DataType.CALENDAR_AS_TIMESTAMP_TEMPORAL_TYPE );
	}

	@Test
	public void testLocalDateType(TestContext context) {
		testDatatype( context, "localdayte", TestableDatabase.DataType.LOCALDATE );
	}

	@Test
	public void testLocalDateTimeType(TestContext context) {
		testDatatype( context, "alocalDT", TestableDatabase.DataType.LOCALDATETIME );
	}

	@Test
	public void testSerializableType(TestContext context) {
		testDatatype( context, "serializable", TestableDatabase.DataType.SERIALIZABLE );
	}
}
