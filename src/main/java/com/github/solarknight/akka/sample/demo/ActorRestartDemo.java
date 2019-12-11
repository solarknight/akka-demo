package com.github.solarknight.akka.sample.demo;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.PreRestart;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

/**
 * @author peiheng.zph created on Dec 04, 2019
 * @version 1.0
 */
public class ActorRestartDemo {

  static class SupervisingActor extends AbstractBehavior<String> {

    static Behavior<String> create() {
      return Behaviors.setup(SupervisingActor::new);
    }

    private final ActorRef<String> child;

    SupervisingActor(ActorContext<String> context) {
      super(context);
      child = context.spawn(Behaviors.supervise(SupervisedActor.create()).onFailure(SupervisorStrategy.restart()), "supervised-actor");
    }

    @Override
    public Receive<String> createReceive() {
      return newReceiveBuilder().onMessageEquals("failChild", this::onFailChild).build();
    }

    private Behavior<String> onFailChild() {
      child.tell("fail");
      return this;
    }
  }

  static class SupervisedActor extends AbstractBehavior<String> {

    static Behavior<String> create() {
      return Behaviors.setup(SupervisedActor::new);
    }

    SupervisedActor(ActorContext<String> context) {
      super(context);
      System.out.println("Supervised actor started");
    }

    @Override
    public Receive<String> createReceive() {
      return newReceiveBuilder()
          .onMessageEquals("fail", this::fail)
          .onSignal(PreRestart.class, signal -> preRestart())
          .onSignal(PostStop.class, signal -> postStop())
          .build();
    }

    private Behavior<String> fail() {
      System.out.println("Supervised actor failed now");
      throw new RuntimeException("I failed");
    }

    private Behavior<String> preRestart() {
      System.out.println("Second will be restarted");
      return this;
    }

    private Behavior<String> postStop() {
      System.out.println("Second stopped");
      return this;
    }
  }
}
