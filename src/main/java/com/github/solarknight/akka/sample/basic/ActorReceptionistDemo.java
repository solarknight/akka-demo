package com.github.solarknight.akka.sample.basic;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;

/**
 * @author peiheng.zph created on Jan 30, 2020
 * @version 1.0
 */
public class ActorReceptionistDemo {

  public static class PingService {
    public static final ServiceKey<Ping> pingServiceKey =
        ServiceKey.create(Ping.class, "pingService");

    public static class Pong {}

    public static class Ping {
      private final ActorRef<Pong> replyTo;

      public Ping(ActorRef<Pong> replyTo) {
        this.replyTo = replyTo;
      }
    }

    public static Behavior<Ping> create() {
      return Behaviors.setup(
          context -> {
            context
                .getSystem()
                .receptionist()
                .tell(Receptionist.register(pingServiceKey, context.getSelf()));

            return new PingService(context).behavior();
          });
    }

    private final ActorContext<Ping> context;

    private PingService(ActorContext<Ping> context) {
      this.context = context;
    }

    private Behavior<Ping> behavior() {
      return Behaviors.receiveMessage(this::onPing);
    }

    private Behavior<Ping> onPing(Ping msg) {
      context.getLog().info("Pinged by {}", msg.replyTo);
      msg.replyTo.tell(new Pong());
      return Behaviors.same();
    }
  }

  public static class Pinger {

    private final ActorContext<PingService.Pong> context;
    private final ActorRef<PingService.Ping> pingService;

    private Pinger(ActorContext<PingService.Pong> context, ActorRef<PingService.Ping> pingService) {
      this.context = context;
      this.pingService = pingService;
    }

    public static Behavior<PingService.Pong> create(ActorRef<PingService.Ping> pingService) {
      return Behaviors.setup(
          ctx -> {
            pingService.tell(new PingService.Ping(ctx.getSelf()));
            return new Pinger(ctx, pingService).behavior();
          });
    }

    private Behavior<PingService.Pong> behavior() {
      return Behaviors.receive(PingService.Pong.class)
          .onMessage(PingService.Pong.class, this::onPong)
          .build();
    }

    private Behavior<PingService.Pong> onPong(PingService.Pong msg) {
      context.getLog().info("{} was ponged!!", context.getSelf());
      return Behaviors.stopped();
    }
  }

  public static class Guardian {

    public static Behavior<Void> create() {
      return Behaviors.setup(
              (ActorContext<Receptionist.Listing> context) -> {
                context
                    .getSystem()
                    .receptionist()
                    .tell(
                        Receptionist.subscribe(
                            PingService.pingServiceKey, context.getSelf().narrow()));
                context.spawnAnonymous(PingService.create());

                return new Guardian(context).behavior();
              })
          .unsafeCast(); // Void
    }

    private final ActorContext<Receptionist.Listing> context;

    private Guardian(ActorContext<Receptionist.Listing> context) {
      this.context = context;
    }

    private Behavior<Receptionist.Listing> behavior() {
      return Behaviors.receive(Receptionist.Listing.class)
          .onMessage(Receptionist.Listing.class, this::onListing)
          .build();
    }

    private Behavior<Receptionist.Listing> onListing(Receptionist.Listing msg) {
      msg.getServiceInstances(PingService.pingServiceKey)
          .forEach(pingService -> context.spawnAnonymous(Pinger.create(pingService)));
      return Behaviors.same();
    }
  }
}
