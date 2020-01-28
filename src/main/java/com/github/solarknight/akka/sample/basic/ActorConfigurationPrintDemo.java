package com.github.solarknight.akka.sample.basic;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;

/**
 * @author peiheng.zph created on Dec 24, 2019
 * @version 1.0
 */
public class ActorConfigurationPrintDemo {

  public static void main(String[] args) {
    ActorSystem<Void> actorSystem = ActorSystem.create(Behaviors.empty(), "Empty");
    actorSystem.logConfiguration();
    actorSystem.terminate();
  }
}
