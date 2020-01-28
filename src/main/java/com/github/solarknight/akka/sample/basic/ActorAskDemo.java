package com.github.solarknight.akka.sample.basic;

import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.github.solarknight.akka.sample.basic.ActorAskDemo.Hal.OpenThePodBayDoorsPlease;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * @see <a
 *     href="https://doc.akka.io/docs/akka/current/typed/interaction-patterns.html#request-response-with-ask-between-two-actors">Request-Response
 *     with ask between two actors</a>
 * @author peiheng.zph created on Jan 28, 2020
 * @version 1.0
 */
public class ActorAskDemo {

  static class Hal extends AbstractBehavior<Hal.Command> {

    interface Command {}

    public static final class OpenThePodBayDoorsPlease implements Command {
      public final ActorRef<HalResponse> respondTo;

      public OpenThePodBayDoorsPlease(ActorRef<HalResponse> respondTo) {
        this.respondTo = respondTo;
      }
    }

    public static final class HalResponse {
      public final String message;

      public HalResponse(String message) {
        this.message = message;
      }
    }

    public static Behavior<Command> create() {
      return Behaviors.setup(Hal::new);
    }

    public Hal(ActorContext<Command> context) {
      super(context);
    }

    @Override
    public Receive<Command> createReceive() {
      return newReceiveBuilder()
          .onMessage(OpenThePodBayDoorsPlease.class, this::onOpenThePodBayDoorsPlease)
          .build();
    }

    private Behavior<Command> onOpenThePodBayDoorsPlease(OpenThePodBayDoorsPlease message) {
      sleepUninterruptibly(1, TimeUnit.SECONDS);
      message.respondTo.tell(new HalResponse("I'm sorry. I'm afraid I can't do that."));
      return Behaviors.stopped();
    }
  }

  static class Dave extends AbstractBehavior<Dave.Command> {

    interface Command {}

    // this is a part of the protocol that is internal to the actor itself
    private static final class AdaptedResponse implements Command {
      public final String message;

      public AdaptedResponse(String message) {
        this.message = message;
      }
    }

    public static Behavior<Command> create(ActorRef<Hal.Command> hal) {
      return Behaviors.setup(context -> new Dave(context, hal));
    }

    public Dave(ActorContext<Command> context, ActorRef<Hal.Command> hal) {
      super(context);

      // asking someone requires a timeout, if the timeout hits without response
      // the ask is failed with a TimeoutException
      final Duration timeout = Duration.ofSeconds(3);

      context.ask(
          Hal.HalResponse.class,
          hal,
          timeout,
          OpenThePodBayDoorsPlease::new,
          (response, t) -> {
            if (response != null) {
              return new AdaptedResponse(response.message);
            } else {
              return new AdaptedResponse("Request failed");
            }
          });
    }

    @Override
    public Receive<Command> createReceive() {
      return newReceiveBuilder()
          // the adapted message ends up being processed like any other
          // message sent to the actor
          .onMessage(AdaptedResponse.class, this::onAdaptedResponse)
          .build();
    }

    private Behavior<Command> onAdaptedResponse(AdaptedResponse response) {
      getContext().getLog().info("Got response from HAL: {}", response.message);
      return Behaviors.stopped();
    }
  }

  public static void main(String[] args) {
    ActorSystem<Hal.Command> hal = ActorSystem.create(Hal.create(), "hal");
    ActorSystem.create(Dave.create(hal), "dave");
    System.out.println("Actor create finished");
  }
}
