package com.github.solarknight.demo;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

/**
 * @author peiheng.zph created on Dec 04, 2019
 * @version 1.0
 */
public class ActorStopDemo {

  static class StartStopActor1 extends AbstractBehavior<String> {

    static Behavior<String> create() {
      return Behaviors.setup(StartStopActor1::new);
    }

    private ActorRef<String> childActor;

    StartStopActor1(ActorContext<String> context) {
      super(context);
      childActor = context.spawn(StartStopActor2.create(), "child");
      System.out.println("First started");
    }

    @Override
    public Receive<String> createReceive() {
      return newReceiveBuilder()
          .onMessageEquals("stop", Behaviors::stopped)
          .onSignal(PostStop.class, signal -> onPostStop())
          .build();
    }

    private Behavior<String> onPostStop() {
      System.out.println("First stopped");
      return this;
    }
  }

  static class StartStopActor2 extends AbstractBehavior<String> {

    static Behavior<String> create() {
      return Behaviors.setup(StartStopActor2::new);
    }

    StartStopActor2(ActorContext<String> context) {
      super(context);
      System.out.println("Second started");
    }

    @Override
    public Receive<String> createReceive() {
      return newReceiveBuilder()
          .onSignal(PostStop.class, signal -> onPostStop())
          .build();
    }

    private Behavior<String> onPostStop() {
      System.out.println("Second stopped");
      return this;
    }
  }
}
