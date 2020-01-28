package com.github.solarknight.akka.sample.basic;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.AskPattern;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * @see <a
 *     href="https://doc.akka.io/docs/akka/current/typed/interaction-patterns.html#request-response-with-ask-from-outside-an-actor">Request-Response
 *     with ask from outside an Actor</a>
 * @author peiheng.zph created on Jan 28, 2020
 * @version 1.0
 */
public class ActorAskFromOutsideDemo {

  public static class CookieFabric extends AbstractBehavior<CookieFabric.Command> {

    interface Command {}

    public static class GiveMeCookies implements Command {
      public final int count;
      public final ActorRef<Reply> replyTo;

      public GiveMeCookies(int count, ActorRef<Reply> replyTo) {
        this.count = count;
        this.replyTo = replyTo;
      }
    }

    interface Reply {}

    public static class Cookies implements Reply {
      public final int count;

      public Cookies(int count) {
        this.count = count;
      }
    }

    public static class InvalidRequest implements Reply {
      public final String reason;

      public InvalidRequest(String reason) {
        this.reason = reason;
      }
    }

    public static Behavior<Command> create() {
      return Behaviors.setup(CookieFabric::new);
    }

    private CookieFabric(ActorContext<Command> context) {
      super(context);
    }

    @Override
    public Receive<Command> createReceive() {
      return newReceiveBuilder().onMessage(GiveMeCookies.class, this::onGiveMeCookies).build();
    }

    private Behavior<Command> onGiveMeCookies(GiveMeCookies request) {
      if (request.count >= 5) request.replyTo.tell(new InvalidRequest("Too many cookies."));
      else request.replyTo.tell(new Cookies(request.count));

      return this;
    }
  }

  public static void main(String[] args) {
    ActorSystem<Void> system = ActorSystem.create(Behaviors.empty(), "system");
    ActorSystem<CookieFabric.Command> actorRef =
        ActorSystem.create(CookieFabric.create(), "cookieFabric");
    askAndPrint(system, actorRef);

    system.terminate();
    actorRef.terminate();
  }

  public static void askAndPrint(
      ActorSystem<Void> system, ActorRef<CookieFabric.Command> cookieFabric) {
    CompletionStage<CookieFabric.Reply> result =
        AskPattern.ask(
            cookieFabric,
            replyTo -> new CookieFabric.GiveMeCookies(3, replyTo),
            Duration.ofSeconds(3),
            system.scheduler());

    CompletionStage<CookieFabric.Cookies> cookies =
        result.thenCompose(
            (CookieFabric.Reply reply) -> {
              if (reply instanceof CookieFabric.Cookies) {
                return CompletableFuture.completedFuture((CookieFabric.Cookies) reply);
              } else if (reply instanceof CookieFabric.InvalidRequest) {
                CompletableFuture<CookieFabric.Cookies> failed = new CompletableFuture<>();
                failed.completeExceptionally(
                    new IllegalArgumentException(((CookieFabric.InvalidRequest) reply).reason));
                return failed;
              } else {
                throw new IllegalStateException("Unexpected reply: " + reply.getClass());
              }
            });

    cookies.whenComplete(
        (cookiesReply, failure) -> {
          if (cookies != null) System.out.println("Yay, " + cookiesReply.count + " cookies!");
          else System.out.println("Boo! didn't get cookies in time. " + failure);
        });
  }
}
