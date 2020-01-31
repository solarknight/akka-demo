package com.github.solarknight.akka.sample.basic;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.DispatcherSelector;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * @author peiheng.zph created on Jan 31, 2020
 * @version 1.0
 */
public class BlockingActorDemo {

  static class BlockingActor extends AbstractBehavior<Integer> {

    public static Behavior<Integer> create() {
      return Behaviors.setup(BlockingActor::new);
    }

    private BlockingActor(ActorContext<Integer> context) {
      super(context);
    }

    @Override
    public Receive<Integer> createReceive() {
      return newReceiveBuilder()
          .onMessage(
              Integer.class,
              i -> {
                // DO NOT DO THIS HERE: this is an example of incorrect code,
                // better alternatives are described further on.

                // block for 5 seconds, representing blocking I/O, etc
                Thread.sleep(5000);
                System.out.println("Blocking operation finished: " + i);
                return Behaviors.same();
              })
          .build();
    }
  }

  static class PrintActor extends AbstractBehavior<Integer> {
    public static Behavior<Integer> create() {
      return Behaviors.setup(PrintActor::new);
    }

    private PrintActor(ActorContext<Integer> context) {
      super(context);
    }

    @Override
    public Receive<Integer> createReceive() {
      return newReceiveBuilder()
          .onMessage(
              Integer.class,
              i -> {
                System.out.println("PrintActor: " + i);
                return Behaviors.same();
              })
          .build();
    }
  }

  static class SeparateDispatcherFutureActor extends AbstractBehavior<Integer> {
    private final Executor ec;

    static Behavior<Integer> create() {
      return Behaviors.setup(SeparateDispatcherFutureActor::new);
    }

    private SeparateDispatcherFutureActor(ActorContext<Integer> context) {
      super(context);
      ec =
          context
              .getSystem()
              .dispatchers()
              .lookup(DispatcherSelector.fromConfig("my-blocking-dispatcher"));
    }

    @Override
    public Receive<Integer> createReceive() {
      return newReceiveBuilder()
          .onMessage(
              Integer.class,
              i -> {
                triggerFutureBlockingOperation(i, ec);
                return Behaviors.same();
              })
          .build();
    }

    private void triggerFutureBlockingOperation(Integer i, Executor ec) {
      System.out.println("Calling blocking Future on separate dispatcher: " + i);
      CompletableFuture<Integer> f =
          CompletableFuture.supplyAsync(
              () -> {
                try {
                  Thread.sleep(5000);
                  System.out.println("Blocking future finished: " + i);
                  return i;
                } catch (InterruptedException e) {
                  return -1;
                }
              },
              ec);
    }
  }

  public static void main(String[] args) {
    Behavior<Void> root =
        Behaviors.setup(
            context -> {
              for (int i = 0; i < 100; i++) {
                context.spawn(SeparateDispatcherFutureActor.create(), "BlockingActor-" + i).tell(i);
                context.spawn(PrintActor.create(), "PrintActor-" + i).tell(i);
              }
              return Behaviors.ignore();
            });
    ActorSystem.create(root, "root");
  }
}
