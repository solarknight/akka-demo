package com.github.solarknight.akka.sample.iot;

import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

/**
 * @author solarknight created on Dec 11, 2019
 * @version 1.0
 */
public class IotSupervisor extends AbstractBehavior<Void> {

  public static Behavior<Void> create() {
    return Behaviors.setup(IotSupervisor::new);
  }

  private IotSupervisor(ActorContext<Void> context) {
    super(context);
    context.getLog().info("IoT Application started");
  }

  @Override
  public Receive<Void> createReceive() {
    return newReceiveBuilder().onSignal(PostStop.class, signal -> onPostStop()).build();
  }

  private Behavior<Void> onPostStop() {
    getContext().getLog().info("IoT Application stopped");
    return this;
  }
}
