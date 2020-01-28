package com.github.solarknight.akka.sample.iot;

import static org.junit.Assert.assertEquals;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import com.github.solarknight.akka.sample.iot.Device.Command;
import com.github.solarknight.akka.sample.iot.Device.ReadTemperature;
import com.github.solarknight.akka.sample.iot.Device.RespondTemperature;
import com.github.solarknight.akka.sample.iot.DeviceGroupQuery.WrappedRespondTemperature;
import com.github.solarknight.akka.sample.iot.DeviceManager.DeviceNotAvailable;
import com.github.solarknight.akka.sample.iot.DeviceManager.DeviceTimedOut;
import com.github.solarknight.akka.sample.iot.DeviceManager.RespondAllTemperatures;
import com.github.solarknight.akka.sample.iot.DeviceManager.Temperature;
import com.github.solarknight.akka.sample.iot.DeviceManager.TemperatureNotAvailable;
import com.github.solarknight.akka.sample.iot.DeviceManager.TemperatureReading;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * @author solarknight created on Dec 17, 2019
 * @version 1.0
 */
public class DeviceGroupQueryTest {

  @ClassRule public static final TestKitJunitResource testKit = new TestKitJunitResource();

  @Test
  public void testReturnTemperatureValueForWorkingDevices() {
    TestProbe<RespondAllTemperatures> requester =
        testKit.createTestProbe(RespondAllTemperatures.class);
    TestProbe<Device.Command> device1 = testKit.createTestProbe(Device.Command.class);
    TestProbe<Device.Command> device2 = testKit.createTestProbe(Device.Command.class);

    Map<String, ActorRef<Command>> deviceIdToActor = new HashMap<>();
    deviceIdToActor.put("device1", device1.getRef());
    deviceIdToActor.put("device2", device2.getRef());

    ActorRef<DeviceGroupQuery.Command> queryActor =
        testKit.spawn(
            DeviceGroupQuery.create(
                deviceIdToActor, 1L, requester.getRef(), Duration.ofSeconds(3)));

    device1.expectMessageClass(Device.ReadTemperature.class);
    device2.expectMessageClass(Device.ReadTemperature.class);

    queryActor.tell(
        new DeviceGroupQuery.WrappedRespondTemperature(
            new Device.RespondTemperature(0L, "device1", Optional.of(1.0))));

    queryActor.tell(
        new DeviceGroupQuery.WrappedRespondTemperature(
            new Device.RespondTemperature(0L, "device2", Optional.of(2.0))));

    RespondAllTemperatures response = requester.receiveMessage();
    assertEquals(1L, response.requestId);

    Map<String, TemperatureReading> expectedTemperatures = new HashMap<>();
    expectedTemperatures.put("device1", new Temperature(1.0));
    expectedTemperatures.put("device2", new Temperature(2.0));

    assertEquals(expectedTemperatures, response.temperatures);
  }

  @Test
  public void testReturnTemperatureNotAvailableForDevicesWithNoReadings() {
    TestProbe<RespondAllTemperatures> requester =
        testKit.createTestProbe(RespondAllTemperatures.class);
    TestProbe<Device.Command> device1 = testKit.createTestProbe(Device.Command.class);
    TestProbe<Device.Command> device2 = testKit.createTestProbe(Device.Command.class);

    Map<String, ActorRef<Device.Command>> deviceIdToActor = new HashMap<>();
    deviceIdToActor.put("device1", device1.getRef());
    deviceIdToActor.put("device2", device2.getRef());

    ActorRef<DeviceGroupQuery.Command> queryActor =
        testKit.spawn(
            DeviceGroupQuery.create(
                deviceIdToActor, 1L, requester.getRef(), Duration.ofSeconds(3)));

    assertEquals(0L, device1.expectMessageClass(Device.ReadTemperature.class).requestId);
    assertEquals(0L, device2.expectMessageClass(Device.ReadTemperature.class).requestId);

    queryActor.tell(
        new DeviceGroupQuery.WrappedRespondTemperature(
            new Device.RespondTemperature(0L, "device1", Optional.empty())));

    queryActor.tell(
        new DeviceGroupQuery.WrappedRespondTemperature(
            new Device.RespondTemperature(0L, "device2", Optional.of(2.0))));

    RespondAllTemperatures response = requester.receiveMessage();
    assertEquals(1L, response.requestId);

    Map<String, TemperatureReading> expectedTemperatures = new HashMap<>();
    expectedTemperatures.put("device1", TemperatureNotAvailable.INSTANCE);
    expectedTemperatures.put("device2", new Temperature(2.0));

    assertEquals(expectedTemperatures, response.temperatures);
  }

  @Test
  public void testReturnDeviceNotAvailableIfDeviceStopsBeforeAnswering() {
    TestProbe<RespondAllTemperatures> requester =
        testKit.createTestProbe(RespondAllTemperatures.class);
    TestProbe<Device.Command> device1 = testKit.createTestProbe(Device.Command.class);
    TestProbe<Device.Command> device2 = testKit.createTestProbe(Device.Command.class);

    Map<String, ActorRef<Device.Command>> deviceIdToActor = new HashMap<>();
    deviceIdToActor.put("device1", device1.getRef());
    deviceIdToActor.put("device2", device2.getRef());

    ActorRef<DeviceGroupQuery.Command> queryActor =
        testKit.spawn(
            DeviceGroupQuery.create(
                deviceIdToActor, 1L, requester.getRef(), Duration.ofSeconds(3)));

    assertEquals(0L, device1.expectMessageClass(ReadTemperature.class).requestId);
    assertEquals(0L, device2.expectMessageClass(ReadTemperature.class).requestId);

    queryActor.tell(
        new WrappedRespondTemperature(new RespondTemperature(0L, "device1", Optional.of(1.0))));

    device2.stop();

    RespondAllTemperatures response = requester.receiveMessage();
    assertEquals(1L, response.requestId);

    Map<String, TemperatureReading> expectedMap = new HashMap<>();
    expectedMap.put("device1", new Temperature(1.0));
    expectedMap.put("device2", DeviceNotAvailable.INSTANCE);

    assertEquals(expectedMap, response.temperatures);
  }

  @Test
  public void testReturnTemperatureReadingEvenIfDeviceStopsAfterAnswering() {
    TestProbe<RespondAllTemperatures> requester =
        testKit.createTestProbe(RespondAllTemperatures.class);
    TestProbe<Device.Command> device1 = testKit.createTestProbe(Device.Command.class);
    TestProbe<Device.Command> device2 = testKit.createTestProbe(Device.Command.class);

    Map<String, ActorRef<Device.Command>> deviceIdToActor = new HashMap<>();
    deviceIdToActor.put("device1", device1.getRef());
    deviceIdToActor.put("device2", device2.getRef());

    ActorRef<DeviceGroupQuery.Command> queryActor =
        testKit.spawn(
            DeviceGroupQuery.create(
                deviceIdToActor, 1L, requester.getRef(), Duration.ofSeconds(3)));

    assertEquals(0L, device1.expectMessageClass(Device.ReadTemperature.class).requestId);
    assertEquals(0L, device2.expectMessageClass(Device.ReadTemperature.class).requestId);

    queryActor.tell(
        new DeviceGroupQuery.WrappedRespondTemperature(
            new Device.RespondTemperature(0L, "device1", Optional.of(1.0))));

    queryActor.tell(
        new DeviceGroupQuery.WrappedRespondTemperature(
            new Device.RespondTemperature(0L, "device2", Optional.of(2.0))));

    device2.stop();

    RespondAllTemperatures response = requester.receiveMessage();
    assertEquals(1L, response.requestId);

    Map<String, TemperatureReading> expectedTemperatures = new HashMap<>();
    expectedTemperatures.put("device1", new Temperature(1.0));
    expectedTemperatures.put("device2", new Temperature(2.0));

    assertEquals(expectedTemperatures, response.temperatures);
  }

  @Test
  public void testReturnDeviceTimedOutIfDeviceDoesNotAnswerInTime() {
    TestProbe<RespondAllTemperatures> requester =
        testKit.createTestProbe(RespondAllTemperatures.class);
    TestProbe<Device.Command> device1 = testKit.createTestProbe(Device.Command.class);
    TestProbe<Device.Command> device2 = testKit.createTestProbe(Device.Command.class);

    Map<String, ActorRef<Device.Command>> deviceIdToActor = new HashMap<>();
    deviceIdToActor.put("device1", device1.getRef());
    deviceIdToActor.put("device2", device2.getRef());

    ActorRef<DeviceGroupQuery.Command> queryActor =
        testKit.spawn(
            DeviceGroupQuery.create(
                deviceIdToActor, 1L, requester.getRef(), Duration.ofMillis(200)));

    assertEquals(0L, device1.expectMessageClass(Device.ReadTemperature.class).requestId);
    assertEquals(0L, device2.expectMessageClass(Device.ReadTemperature.class).requestId);

    queryActor.tell(
        new DeviceGroupQuery.WrappedRespondTemperature(
            new Device.RespondTemperature(0L, "device1", Optional.of(1.0))));

    // no reply from device2

    RespondAllTemperatures response = requester.receiveMessage();
    assertEquals(1L, response.requestId);

    Map<String, TemperatureReading> expectedTemperatures = new HashMap<>();
    expectedTemperatures.put("device1", new Temperature(1.0));
    expectedTemperatures.put("device2", DeviceTimedOut.INSTANCE);

    assertEquals(expectedTemperatures, response.temperatures);
  }
}
