/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.testing;



import org.hibernate.reactive.containers.DatabaseConfiguration;
import org.hibernate.reactive.containers.DatabaseConfiguration.DBType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.assertj.core.api.Assertions;

public class DatabaseSelectionRuleTests {

	public static class SkipDBTest {
		@RegisterExtension
		public DatabaseSelectionRule rule = DatabaseSelectionRule.skipTestsFor( DBType.POSTGRESQL );

		@Test
		public void shouldSkipPostgres() {
			Assertions.assertThat( DatabaseConfiguration.dbType() ).isNotEqualTo( DBType.POSTGRESQL );
		}
	}

	public static class SkipMultipleDBsTest {
		@RegisterExtension
		public DatabaseSelectionRule rule = DatabaseSelectionRule.skipTestsFor( DBType.POSTGRESQL, DBType.MYSQL, DBType.MARIA );

		@Test
		public void shouldSkipMultipleDBs() {
			switch ( DatabaseConfiguration.dbType() ) {
				case MARIA:
				case MYSQL:
				case POSTGRESQL: {
					Assertions.assertThat( rule.evaluateExecutionCondition( null ).isDisabled() ).isTrue();
				}
				break;
				default: {
					Assertions.assertThat( rule.evaluateExecutionCondition( null ).isDisabled() ).isFalse();
				}
				break;

			}
		}
	}

	public static class RunOnlyOnDBTest {
		@RegisterExtension
		public DatabaseSelectionRule rule = DatabaseSelectionRule.runOnlyFor( DBType.POSTGRESQL );

		@Test
		public void shouldOnlyRunForPostgres() {
			switch ( DatabaseConfiguration.dbType() ) {
				case POSTGRESQL: {
					Assertions.assertThat( rule.evaluateExecutionCondition( null ).isDisabled() ).isFalse();
				}
				break;
				default: {
					Assertions.assertThat( rule.evaluateExecutionCondition( null ).isDisabled() ).isTrue();
				}
				break;

			}
		}
	}

	public static class shouldSkipRunningPostgreSQLTest {
		@RegisterExtension
		public DatabaseSelectionRule rule = DatabaseSelectionRule.skipTestsFor( DBType.POSTGRESQL );

		@Test
		public void shouldSkipOnlyPostgres() {
			switch ( DatabaseConfiguration.dbType() ) {
				case POSTGRESQL: {
					Assertions.assertThat( rule.evaluateExecutionCondition( null ).isDisabled() ).isTrue();
				}
				break;
				default: {
					Assertions.assertThat( rule.evaluateExecutionCondition( null ).isDisabled() ).isFalse();
				}
				break;

			}
		}
	}

	public static class RunOnlyOnDMultipleDBsTest {
		@RegisterExtension
		public DatabaseSelectionRule rule = DatabaseSelectionRule.runOnlyFor( DBType.POSTGRESQL, DBType.MYSQL, DBType.MARIA );

		@Test
		public void shouldRunOnlyForMultipleDBs() {
			switch ( DatabaseConfiguration.dbType() ) {
				case MARIA:
				case MYSQL:
				case POSTGRESQL: {
					Assertions.assertThat( rule.evaluateExecutionCondition( null ).isDisabled() ).isFalse();
				}
				break;
				default: {
					Assertions.assertThat( rule.evaluateExecutionCondition( null ).isDisabled() ).isTrue();
				}
				break;

			}
		}
	}
}
