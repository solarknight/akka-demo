package com.github.solarknight.akka.sample.iot;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.github.solarknight.akka.sample.iot.DeviceGroup.Command;
import com.github.solarknight.akka.sample.iot.DeviceManager.DeviceRegistered;
import com.github.solarknight.akka.sample.iot.DeviceManager.ReplyDeviceList;
import com.github.solarknight.akka.sample.iot.DeviceManager.RequestDeviceList;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author solarknight created on Dec 15, 2019
 * @version 1.0
 */
public class DeviceGroup extends AbstractBehavior<Command> {

  public interface Command {}

  private class DeviceTerminated implements Command {
    public final ActorRef<Device.Command> device;
    public final String groupId;
    public final String deviceId;

    public DeviceTerminated(ActorRef<Device.Command> device, String groupId, String deviceId) {
      this.device = device;
      this.groupId = groupId;
      this.deviceId = deviceId;
    }
  }

  public static Behavior<Command> create(String groupId) {
    return Behaviors.setup(context -> new DeviceGroup(context, groupId));
  }

  private final String groupId;
  private final Map<String, ActorRef<Device.Command>> deviceIdToActor = new HashMap<>();

  public DeviceGroup(ActorContext<Command> context, String groupId) {
    super(context);
    this.groupId = groupId;

    context.getLog().info("DeviceGroup {} started", groupId);
  }

  @Override
  public Receive<Command> createReceive() {
    return newReceiveBuilder()
        .onMessage(DeviceManager.RequestTrackDevice.class, this::onTrackDevice)
        .onMessage(
            DeviceManager.RequestDeviceList.class,
            r -> r.groupId.equals(groupId),
            this::onDeviceList)
        .onMessage(DeviceTerminated.class, this::onTerminated)
        .onMessage(
            DeviceManager.RequestAllTemperatures.class,
            r -> r.groupId.equals(groupId),
            this::onAllTemperatures)
        .onSignal(PostStop.class, signal -> onPostStop())
        .build();
  }

  private DeviceGroup onTrackDevice(DeviceManager.RequestTrackDevice trackMsg) {
    if (this.groupId.equals(trackMsg.groupId)) {
      ActorRef<Device.Command> deviceActor = deviceIdToActor.get(trackMsg.deviceId);
      if (deviceActor != null) {
        trackMsg.replyTo.tell(new DeviceRegistered(deviceActor));
      } else {
        getContext().getLog().info("Creating device actor for {}", trackMsg.deviceId);
        deviceActor =
            getContext()
                .spawn(Device.create(groupId, trackMsg.deviceId), "device-" + trackMsg.deviceId);

        getContext()
            .watchWith(deviceActor, new DeviceTerminated(deviceActor, groupId, trackMsg.deviceId));
        deviceIdToActor.put(trackMsg.deviceId, deviceActor);
        trackMsg.replyTo.tell(new DeviceManager.DeviceRegistered(deviceActor));
      }
    } else {
      getContext()
          .getLog()
          .warn(
              "Ignoring TrackDevice request for {}. This actor is responsible for {}.",
              trackMsg.groupId,
              this.groupId);
    }
    return this;
  }

  private Behavior<Command> onDeviceList(RequestDeviceList r) {
    r.replyTo.tell(new ReplyDeviceList(r.requestId, deviceIdToActor.keySet()));
    return this;
  }

  private Behavior<Command> onTerminated(DeviceTerminated t) {
    getContext().getLog().info("Device actor for {} has been terminated", t.deviceId);
    deviceIdToActor.remove(t.deviceId);
    return this;
  }

  private DeviceGroup onAllTemperatures(DeviceManager.RequestAllTemperatures r) {
    Map<String, ActorRef<Device.Command>> copy = new HashMap<>(this.deviceIdToActor);

    getContext()
        .spawnAnonymous(
            DeviceGroupQuery.create(copy, r.requestId, r.replyTo, Duration.ofSeconds(3)));
    return this;
  }

  private DeviceGroup onPostStop() {
    getContext().getLog().info("DeviceGroup {} stopped", groupId);
    return this;
  }
}
