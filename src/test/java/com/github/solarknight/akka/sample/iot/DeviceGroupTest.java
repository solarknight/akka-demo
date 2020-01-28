package com.github.solarknight.akka.sample.iot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import com.github.solarknight.akka.sample.iot.DeviceGroup.Command;
import com.github.solarknight.akka.sample.iot.DeviceManager.DeviceRegistered;
import com.github.solarknight.akka.sample.iot.DeviceManager.ReplyDeviceList;
import com.github.solarknight.akka.sample.iot.DeviceManager.RequestAllTemperatures;
import com.github.solarknight.akka.sample.iot.DeviceManager.RequestDeviceList;
import com.github.solarknight.akka.sample.iot.DeviceManager.RequestTrackDevice;
import com.github.solarknight.akka.sample.iot.DeviceManager.RespondAllTemperatures;
import com.github.solarknight.akka.sample.iot.DeviceManager.Temperature;
import com.github.solarknight.akka.sample.iot.DeviceManager.TemperatureNotAvailable;
import com.github.solarknight.akka.sample.iot.DeviceManager.TemperatureReading;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * @author solarknight created on Dec 15, 2019
 * @version 1.0
 */
public class DeviceGroupTest {

  @ClassRule public static final TestKitJunitResource testKit = new TestKitJunitResource();

  @Test
  public void testReplyToRegistrationRequests() {
    TestProbe<DeviceRegistered> probe = testKit.createTestProbe(DeviceRegistered.class);
    ActorRef<Command> groupActor = testKit.spawn(DeviceGroup.create("group"));

    groupActor.tell(new RequestTrackDevice("group", "device1", probe.getRef()));
    DeviceRegistered registered1 = probe.receiveMessage();

    groupActor.tell(new RequestTrackDevice("group", "device2", probe.getRef()));
    DeviceRegistered registered2 = probe.receiveMessage();
    assertNotEquals(registered1.device, registered2.device);

    // Check that the device actors are working
    TestProbe<Device.TemperatureRecorded> recordProbe =
        testKit.createTestProbe(Device.TemperatureRecorded.class);
    registered1.device.tell(new Device.RecordTemperature(0L, 1.0, recordProbe.getRef()));
    assertEquals(0L, recordProbe.receiveMessage().requestId);

    registered2.device.tell(new Device.RecordTemperature(1L, 2.0, recordProbe.getRef()));
    assertEquals(1L, recordProbe.receiveMessage().requestId);
  }

  @Test
  public void testIgnoreWrongRegistrationRequests() {
    TestProbe<DeviceRegistered> probe = testKit.createTestProbe(DeviceRegistered.class);
    ActorRef<DeviceGroup.Command> groupActor = testKit.spawn(DeviceGroup.create("group"));
    groupActor.tell(new RequestTrackDevice("wrongGroup", "device1", probe.getRef()));
    probe.expectNoMessage();
  }

  @Test
  public void testListActiveDevices() {
    TestProbe<DeviceRegistered> registeredProbe = testKit.createTestProbe(DeviceRegistered.class);
    ActorRef<DeviceGroup.Command> groupActor = testKit.spawn(DeviceGroup.create("group"));

    groupActor.tell(new RequestTrackDevice("group", "device1", registeredProbe.getRef()));
    registeredProbe.receiveMessage();

    groupActor.tell(new RequestTrackDevice("group", "device2", registeredProbe.getRef()));
    registeredProbe.receiveMessage();

    TestProbe<ReplyDeviceList> deviceListProbe = testKit.createTestProbe(ReplyDeviceList.class);

    groupActor.tell(new RequestDeviceList(0L, "group", deviceListProbe.getRef()));
    ReplyDeviceList reply = deviceListProbe.receiveMessage();
    assertEquals(0L, reply.requestId);
    assertEquals(Stream.of("device1", "device2").collect(Collectors.toSet()), reply.ids);
  }

  @Test
  public void testListActiveDevicesAfterOneShutsDown() {
    TestProbe<DeviceRegistered> registeredProbe = testKit.createTestProbe(DeviceRegistered.class);
    ActorRef<DeviceGroup.Command> groupActor = testKit.spawn(DeviceGroup.create("group"));

    groupActor.tell(new RequestTrackDevice("group", "device1", registeredProbe.getRef()));
    DeviceRegistered registered1 = registeredProbe.receiveMessage();

    groupActor.tell(new RequestTrackDevice("group", "device2", registeredProbe.getRef()));
    DeviceRegistered registered2 = registeredProbe.receiveMessage();

    ActorRef<Device.Command> toShutDown = registered1.device;

    TestProbe<ReplyDeviceList> deviceListProbe = testKit.createTestProbe(ReplyDeviceList.class);

    groupActor.tell(new RequestDeviceList(0L, "group", deviceListProbe.getRef()));
    ReplyDeviceList reply = deviceListProbe.receiveMessage();
    assertEquals(0L, reply.requestId);
    assertEquals(Stream.of("device1", "device2").collect(Collectors.toSet()), reply.ids);

    toShutDown.tell(Device.Passivate.INSTANCE);
    registeredProbe.expectTerminated(toShutDown, registeredProbe.getRemainingOrDefault());

    // using awaitAssert to retry because it might take longer for the groupActor
    // to see the Terminated, that order is undefined
    registeredProbe.awaitAssert(
        () -> {
          groupActor.tell(new RequestDeviceList(1L, "group", deviceListProbe.getRef()));
          ReplyDeviceList r = deviceListProbe.receiveMessage();
          assertEquals(1L, r.requestId);
          assertEquals(Stream.of("device2").collect(Collectors.toSet()), r.ids);
          return null;
        });
  }

  @Test
  public void testCollectTemperaturesFromAllActiveDevices() {
    TestProbe<DeviceRegistered> registeredProbe = testKit.createTestProbe(DeviceRegistered.class);
    ActorRef<DeviceGroup.Command> groupActor = testKit.spawn(DeviceGroup.create("group"));

    groupActor.tell(new RequestTrackDevice("group", "device1", registeredProbe.getRef()));
    ActorRef<Device.Command> deviceActor1 = registeredProbe.receiveMessage().device;

    groupActor.tell(new RequestTrackDevice("group", "device2", registeredProbe.getRef()));
    ActorRef<Device.Command> deviceActor2 = registeredProbe.receiveMessage().device;

    groupActor.tell(new RequestTrackDevice("group", "device3", registeredProbe.getRef()));
    ActorRef<Device.Command> deviceActor3 = registeredProbe.receiveMessage().device;

    // Check that the device actors are working
    TestProbe<Device.TemperatureRecorded> recordProbe =
        testKit.createTestProbe(Device.TemperatureRecorded.class);
    deviceActor1.tell(new Device.RecordTemperature(0L, 1.0, recordProbe.getRef()));
    assertEquals(0L, recordProbe.receiveMessage().requestId);
    deviceActor2.tell(new Device.RecordTemperature(1L, 2.0, recordProbe.getRef()));
    assertEquals(1L, recordProbe.receiveMessage().requestId);
    // No temperature for device 3

    TestProbe<RespondAllTemperatures> allTempProbe =
        testKit.createTestProbe(RespondAllTemperatures.class);
    groupActor.tell(new RequestAllTemperatures(0L, "group", allTempProbe.getRef()));
    RespondAllTemperatures response = allTempProbe.receiveMessage();
    assertEquals(0L, response.requestId);

    Map<String, TemperatureReading> expectedTemperatures = new HashMap<>();
    expectedTemperatures.put("device1", new Temperature(1.0));
    expectedTemperatures.put("device2", new Temperature(2.0));
    expectedTemperatures.put("device3", TemperatureNotAvailable.INSTANCE);

    assertEquals(expectedTemperatures, response.temperatures);
  }
}
