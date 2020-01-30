package com.github.solarknight.akka.sample.basic;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.GroupRouter;
import akka.actor.typed.javadsl.Routers;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import lombok.RequiredArgsConstructor;

/**
 * @author peiheng.zph created on Jan 30, 2020
 * @version 1.0
 */
public class GroupRouterDemo {

  public static class Worker {
    interface Command {}

    @RequiredArgsConstructor
    static class DoLog implements Worker.Command {
      public final String text;
    }

    static Behavior<Worker.Command> create() {
      return Behaviors.setup(
          context -> {
            context.getLog().info("Starting worker");

            return Behaviors.receive(Worker.Command.class)
                .onMessage(Worker.DoLog.class, doLog -> onDoLog(context, doLog))
                .build();
          });
    }

    private static Behavior<Worker.Command> onDoLog(
        ActorContext<Worker.Command> context, Worker.DoLog doLog) {
      context.getLog().info("Got message {}", doLog.text);
      return Behaviors.same();
    }
  }

  public static class Guardian {

    interface Command {}

    enum Stop implements Guardian.Command {
      INSTANCE
    }

    static ServiceKey<Worker.Command> serviceKey =
        ServiceKey.create(Worker.Command.class, "log-worker");

    static Behavior<Guardian.Command> create() {
      return Behaviors.setup(
          context -> {
            // this would likely happen elsewhere - if we create it locally we
            // can just as well use a pool
            ActorRef<Worker.Command> worker = context.spawn(Worker.create(), "worker");
            context.getSystem().receptionist().tell(Receptionist.register(serviceKey, worker));

            GroupRouter<Worker.Command> group = Routers.group(serviceKey);
            ActorRef<Worker.Command> router = context.spawn(group, "worker-group");

            // the group router will stash messages until it sees the first listing of registered
            // services from the receptionist, so it is safe to send messages right away
            for (int i = 0; i < 10; i++) {
              router.tell(new Worker.DoLog("msg " + i));
            }
            return Behaviors.receive(Guardian.Command.class)
                .onMessage(Guardian.Stop.class, msg -> Behaviors.stopped())
                .build();
          });
    }
  }
}
