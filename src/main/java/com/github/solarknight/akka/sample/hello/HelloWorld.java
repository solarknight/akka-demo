package com.github.solarknight.akka.sample.hello;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.github.solarknight.akka.sample.hello.HelloWorld.Greet;

/**
 * @author peiheng.zph created on Dec 24, 2019
 * @version 1.0
 */
public class HelloWorld extends AbstractBehavior<Greet> {

  public static final class Greet {
    public final String whom;
    public final ActorRef<Greeted> replyTo;

    public Greet(String whom, ActorRef<Greeted> replyTo) {
      this.whom = whom;
      this.replyTo = replyTo;
    }
  }

  public static final class Greeted {
    public final String whom;
    public final ActorRef<Greet> from;

    public Greeted(String whom, ActorRef<Greet> from) {
      this.whom = whom;
      this.from = from;
    }
  }

  public static Behavior<Greet> create() {
    return Behaviors.setup(HelloWorld::new);
  }

  private HelloWorld(ActorContext<Greet> context) {
    super(context);
  }

  @Override
  public Receive<Greet> createReceive() {
    return newReceiveBuilder().onMessage(Greet.class, this::onGreet).build();
  }

  public Behavior<Greet> onGreet(Greet greet) {
    getContext().getLog().info("Hello {}!", greet.whom);
    greet.replyTo.tell(new Greeted(greet.whom, getContext().getSelf()));
    return this;
  }
}
