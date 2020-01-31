package com.github.solarknight.akka.sample.test;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

/**
 * @author peiheng.zph created on Jan 31, 2020
 * @version 1.0
 */
public class AsyncTestingExample {

  public static class Echo {

    @RequiredArgsConstructor
    public static class Ping {
      public final String message;
      public final ActorRef<Pong> replyTo;
    }

    @EqualsAndHashCode
    @RequiredArgsConstructor
    public static class Pong {
      public final String message;
    }

    public static Behavior<Ping> create() {
      return Behaviors.receive(Ping.class)
          .onMessage(
              Ping.class,
              ping -> {
                ping.replyTo.tell(new Pong(ping.message));
                return Behaviors.same();
              })
          .build();
    }
  }
}
