package com.github.solarknight.akka.sample;

import akka.actor.typed.ActorSystem;
import com.github.solarknight.akka.sample.iot.IotSupervisor;

/**
 * @author solarknight created on Dec 03, 2019
 * @version 1.0
 */
public class Application {

  public static void main(String[] args) {
    ActorSystem<Void> actorSystem = ActorSystem.create(IotSupervisor.create(), "iot-system");
    actorSystem.terminate();
  }
}
