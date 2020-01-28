package com.github.solarknight.akka.sample.hello;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.github.solarknight.akka.sample.hello.HelloWorld.Greet;
import com.github.solarknight.akka.sample.hello.HelloWorld.Greeted;

/**
 * @author peiheng.zph created on Dec 24, 2019
 * @version 1.0
 */
public class HelloWorldBot extends AbstractBehavior<Greeted> {

  public static Behavior<Greeted> create(int max) {
    return Behaviors.setup(context -> new HelloWorldBot(context, max));
  }

  private final int max;
  private int greetingCount;

  private HelloWorldBot(ActorContext<Greeted> context, int max) {
    super(context);

    this.max = max;
    greetingCount = 0;
  }

  @Override
  public Receive<Greeted> createReceive() {
    return newReceiveBuilder().onMessage(Greeted.class, this::onGreeted).build();
  }

  private Behavior<Greeted> onGreeted(Greeted msg) {
    greetingCount++;
    getContext().getLog().info("Greeting {} for {}", greetingCount, msg.whom);
    if (greetingCount == max) {
      return Behaviors.stopped();
    } else {
      msg.from.tell(new Greet(msg.whom, getContext().getSelf()));
      return this;
    }
  }
}
