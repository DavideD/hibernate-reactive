/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.cfg.Configuration;
import org.hibernate.reactive.mutiny.Mutiny;

import org.junit.Test;

import io.vertx.ext.unit.TestContext;

public class ManyToOneIssue975 extends BaseReactiveTest {

	@Override
	protected Configuration constructConfiguration() {
		Configuration configuration = super.constructConfiguration();
		configuration.addAnnotatedClass( TelemetryData.class );
		configuration.addAnnotatedClass( Device.class );
		configuration.addAnnotatedClass( Feature.class );
		return configuration;
	}

	@Test
	public void test(TestContext context) {
		Device device = new Device();
		device.setName( "Portal gun" );

		Feature feature = new Feature();
		feature.setName( "Multiverse traveling" );
		feature.setDevice( device );

		TelemetryData td = new TelemetryData();
		td.setFeature( feature );
		td.setValue( 12L );

		test( context, getMutinySessionFactory()
				.withTransaction( (session, tx) -> session.persistAll( device, feature, td ) )
				.chain( () -> getMutinySessionFactory()
						.withTransaction( (session, transaction) -> session
								.createQuery( "from TelemetryData t ", TelemetryData.class )
								.setMaxResults( 1 )
								.getSingleResultOrNull()
								.call( tdResult -> {
									context.assertEquals( td, tdResult );
									return Mutiny
											.fetch( td.getFeature() )
											.invoke( fetched -> tdResult.setFeature( fetched ) )
											;
								} ) )
				)
				.invoke( queryResult -> {
					context.assertEquals( feature, queryResult.getFeature() );
					context.assertEquals( device, queryResult.getFeature().getDevice() );
				} )
		);
	}

	@Entity(name = "TelemetryData")
	@Table(name = "telemetry_data")
	public static class TelemetryData {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		@Column(name = "value", nullable = false)
		private Long value;

		@JoinColumn(name = "feature_id", nullable = false)
		@ManyToOne(fetch = FetchType.LAZY, targetEntity = Feature.class)
		private Feature feature;

		public TelemetryData() {
		}

		public TelemetryData(Long value, Feature feature) {
			this.value = value;
			this.feature = feature;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Long getValue() {
			return value;
		}

		public void setValue(Long value) {
			this.value = value;
		}

		public Feature getFeature() {
			return feature;
		}

		public void setFeature(Feature feature) {
			this.feature = feature;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			TelemetryData that = (TelemetryData) o;
			return Objects.equals( value, that.value );
		}

		@Override
		public int hashCode() {
			return Objects.hash( value );
		}
	}

	@Entity(name = "Feature")
	@Table(name = "features")
	public static class Feature {

		@Id
		@Column(name = "id")
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		@Column(name = "name", nullable = false)
		private String name;

		@JoinColumn(name = "device_id", nullable = false)
		@ManyToOne(fetch = FetchType.LAZY)
		private Device device;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Device getDevice() {
			return device;
		}

		public void setDevice(Device device) {
			this.device = device;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Feature feature = (Feature) o;
			return Objects.equals( name, feature.name );
		}

		@Override
		public int hashCode() {
			return Objects.hash( name );
		}
	}

	@Entity(name = "Device")
	@Table(name = "devices")
	public static class Device {

		@Id
		@Column(name = "id")
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		@Column(name = "name", nullable = false)
		private String name;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Device device = (Device) o;
			return Objects.equals( name, device.name );
		}

		@Override
		public int hashCode() {
			return Objects.hash( name );
		}
	}
}
