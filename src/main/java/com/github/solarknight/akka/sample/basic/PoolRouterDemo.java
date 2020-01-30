package com.github.solarknight.akka.sample.basic;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.PoolRouter;
import akka.actor.typed.javadsl.Routers;
import lombok.RequiredArgsConstructor;

/**
 * @author peiheng.zph created on Jan 30, 2020
 * @version 1.0
 */
public class PoolRouterDemo {

  public static class Worker {
    interface Command {}

    @RequiredArgsConstructor
    static class DoLog implements Command {
      public final String text;
    }

    static Behavior<Command> create() {
      return Behaviors.setup(
          context -> {
            context.getLog().info("Starting worker");

            return Behaviors.receive(Command.class)
                .onMessage(DoLog.class, doLog -> onDoLog(context, doLog))
                .build();
          });
    }

    private static Behavior<Command> onDoLog(ActorContext<Command> context, DoLog doLog) {
      context.getLog().info("Got message {}", doLog.text);
      return Behaviors.same();
    }
  }

  public static class Guardian {

    interface Command {}

    enum Stop implements Guardian.Command {
      INSTANCE
    }

    static Behavior<Guardian.Command> create() {
      return Behaviors.setup(
          context -> {
            int poolSize = 4;
            PoolRouter<Worker.Command> pool =
                Routers.pool(
                    poolSize,
                    // make sure the workers are restarted if they fail
                    Behaviors.supervise(Worker.create()).onFailure(SupervisorStrategy.restart()));
            ActorRef<Worker.Command> router = context.spawn(pool, "worker-pool");

            for (int i = 0; i < 10; i++) {
              router.tell(new Worker.DoLog("msg " + i));
            }

            return Behaviors.receive(Command.class)
                .onMessage(Stop.class, msg -> Behaviors.stopped())
                .build();
          });
    }
  }
}
