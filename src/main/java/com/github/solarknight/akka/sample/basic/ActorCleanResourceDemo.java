package com.github.solarknight.akka.sample.basic;

import static com.google.common.base.Preconditions.checkArgument;

import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.PreRestart;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.javadsl.Behaviors;

/**
 * @author peiheng.zph created on Jan 29, 2020
 * @version 1.0
 */
public class ActorCleanResourceDemo {

  public static Behavior<String> behavior() {
    return Behaviors.supervise(
            Behaviors.<String>setup(
                ctx -> {
                  final Resource resource = Resource.INSTANCE;

                  return Behaviors.receive(String.class)
                      .onMessage(
                          String.class,
                          msg -> {
                            // message handling that might throw an exception
                            String[] parts = msg.split(" ");
                            resource.process(parts);
                            return Behaviors.same();
                          })

                      // Note: PostStop is not emitted for a restart, so typically you need to
                      // handle both PreRestart and PostStop to cleanup resources.
                      .onSignal(
                          PreRestart.class,
                          signal -> {
                            System.out.println("Actor restarted");
                            resource.close();
                            return Behaviors.same();
                          })
                      .onSignal(
                          PostStop.class,
                          signal -> {
                            System.out.println("Actor stopped");
                            resource.close();
                            return Behaviors.same();
                          })
                      .build();
                }))
        .onFailure(Exception.class, SupervisorStrategy.restart());
  }

  private static class Resource {
    static Resource INSTANCE = new Resource();

    void process(String[] parts) {
      checkArgument(parts.length != 1);
      System.out.println("Process " + String.join(",", parts));
    }

    void close() {
      System.out.println("Close resource...");
    }
  }
}
