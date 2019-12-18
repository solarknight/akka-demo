package com.github.solarknight.akka.sample.iot;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.TimerScheduler;
import com.github.solarknight.akka.sample.iot.Device.RespondTemperature;
import com.github.solarknight.akka.sample.iot.DeviceGroupQuery.Command;
import com.github.solarknight.akka.sample.iot.DeviceManager.DeviceNotAvailable;
import com.github.solarknight.akka.sample.iot.DeviceManager.DeviceTimedOut;
import com.github.solarknight.akka.sample.iot.DeviceManager.TemperatureNotAvailable;
import com.github.solarknight.akka.sample.iot.DeviceManager.TemperatureReading;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author solarknight created on Dec 15, 2019
 * @version 1.0
 */
public class DeviceGroupQuery extends AbstractBehavior<Command> {

  public interface Command {}

  public enum CollectionTimeout implements Command {
    INSTANCE
  }

  static class WrappedRespondTemperature implements Command {
    final Device.RespondTemperature response;

    public WrappedRespondTemperature(RespondTemperature response) {
      this.response = response;
    }
  }

  private static class DeviceTerminated implements Command {
    final String deviceId;

    public DeviceTerminated(String deviceId) {
      this.deviceId = deviceId;
    }
  }

  public static Behavior<Command> create(
      Map<String, ActorRef<Device.Command>> deviceIdToActor,
      long requestId,
      ActorRef<DeviceManager.RespondAllTemperatures> requester,
      Duration timeout) {
    return Behaviors.setup(
        context ->
            Behaviors.withTimers(
                timers ->
                    new DeviceGroupQuery(
                        deviceIdToActor, requestId, requester, timeout, context, timers)));
  }

  private final long requestId;
  private final ActorRef<DeviceManager.RespondAllTemperatures> requester;
  private Map<String, DeviceManager.TemperatureReading> repliesSoFar = new HashMap<>();
  private final Set<String> stillWaiting;

  public DeviceGroupQuery(
      Map<String, ActorRef<Device.Command>> deviceIdToActor,
      long requestId,
      ActorRef<DeviceManager.RespondAllTemperatures> requester,
      Duration timeout,
      ActorContext<Command> context,
      TimerScheduler<Command> timers) {
    super(context);
    this.requestId = requestId;
    this.requester = requester;

    timers.startSingleTimer("Schedule-timer", CollectionTimeout.INSTANCE, timeout);

    ActorRef<Device.RespondTemperature> respondTemperatureAdapter =
        context.messageAdapter(Device.RespondTemperature.class, WrappedRespondTemperature::new);

    for (Map.Entry<String, ActorRef<Device.Command>> entry : deviceIdToActor.entrySet()) {
      context.watchWith(entry.getValue(), new DeviceTerminated(entry.getKey()));
      entry.getValue().tell(new Device.ReadTemperature(0L, respondTemperatureAdapter));
    }

    stillWaiting = new HashSet<>(deviceIdToActor.keySet());
  }

  @Override
  public Receive<Command> createReceive() {
    return newReceiveBuilder()
        .onMessage(WrappedRespondTemperature.class, this::onRespondTemperature)
        .onMessage(DeviceTerminated.class, this::onDeviceTerminated)
        .onMessage(CollectionTimeout.class, this::onCollectionTimeout)
        .build();
  }

  private Behavior<Command> onRespondTemperature(WrappedRespondTemperature r) {
    DeviceManager.TemperatureReading reading =
        r.response
            .value
            .map(v -> (TemperatureReading) new DeviceManager.Temperature(v))
            .orElse(TemperatureNotAvailable.INSTANCE);

    String deviceId = r.response.deviceId;
    repliesSoFar.put(deviceId, reading);
    stillWaiting.remove(deviceId);

    return respondWhenAllCollected();
  }

  private Behavior<Command> onDeviceTerminated(DeviceTerminated terminated) {
    if (stillWaiting.contains(terminated.deviceId)) {
      repliesSoFar.put(terminated.deviceId, DeviceNotAvailable.INSTANCE);
      stillWaiting.remove(terminated.deviceId);
    }
    return respondWhenAllCollected();
  }

  private Behavior<Command> onCollectionTimeout(CollectionTimeout timeout) {
    for (String deviceId : stillWaiting) {
      repliesSoFar.put(deviceId, DeviceTimedOut.INSTANCE);
    }
    stillWaiting.clear();
    return respondWhenAllCollected();
  }

  private Behavior<Command> respondWhenAllCollected() {
    if (stillWaiting.isEmpty()) {
      requester.tell(new DeviceManager.RespondAllTemperatures(requestId, repliesSoFar));
      return Behaviors.stopped();
    }

    return this;
  }
}
