package toilatester.jmeter.influxdb;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

import toilatester.jmeter.config.influxdb.measurement.ConnectMeasurement;
import toilatester.jmeter.config.influxdb.measurement.ErrorMeasurement;
import toilatester.jmeter.config.influxdb.measurement.RequestMeasurement;
import toilatester.jmeter.config.influxdb.measurement.TestStartEndMeasurement;
import toilatester.jmeter.config.influxdb.measurement.VirtualUsersMeasurement;

public class InfluxDBConstantMeasurementTest {

	@Test
	public void testConnectMeasurementConstructorIsPrivate() throws NoSuchMethodException, SecurityException {
		Constructor<ConnectMeasurement> constructor = ConnectMeasurement.class.getDeclaredConstructor();
		assertTrue(Modifier.isPrivate(constructor.getModifiers()));
		constructor.setAccessible(true);
		assertThrows(InvocationTargetException.class, () -> {
			constructor.newInstance();
		});
	}

	@Test
	public void testErrorMeasurementConstructorIsPrivate() throws NoSuchMethodException, SecurityException {
		Constructor<ErrorMeasurement> constructor = ErrorMeasurement.class.getDeclaredConstructor();
		assertTrue(Modifier.isPrivate(constructor.getModifiers()));
		constructor.setAccessible(true);
		assertThrows(InvocationTargetException.class, () -> {
			constructor.newInstance();
		});
	}

	@Test
	public void testRequestMeasurementConstructorIsPrivate() throws NoSuchMethodException, SecurityException {
		Constructor<RequestMeasurement> constructor = RequestMeasurement.class.getDeclaredConstructor();
		assertTrue(Modifier.isPrivate(constructor.getModifiers()));
		constructor.setAccessible(true);
		assertThrows(InvocationTargetException.class, () -> {
			constructor.newInstance();
		});
	}

	@Test
	public void testTestStartEndMeasurementConstructorIsPrivate() throws NoSuchMethodException, SecurityException {
		Constructor<TestStartEndMeasurement> constructor = TestStartEndMeasurement.class.getDeclaredConstructor();
		assertTrue(Modifier.isPrivate(constructor.getModifiers()));
		constructor.setAccessible(true);
		assertThrows(InvocationTargetException.class, () -> {
			constructor.newInstance();
		});
	}

	@Test
	public void testVirtualUsersMeasurementMeasurementConstructorIsPrivate()
			throws NoSuchMethodException, SecurityException {
		Constructor<VirtualUsersMeasurement> constructor = VirtualUsersMeasurement.class.getDeclaredConstructor();
		assertTrue(Modifier.isPrivate(constructor.getModifiers()));
		constructor.setAccessible(true);
		assertThrows(InvocationTargetException.class, () -> {
			constructor.newInstance();
		});
	}

}
