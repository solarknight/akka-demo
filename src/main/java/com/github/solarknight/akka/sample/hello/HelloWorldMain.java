package com.github.solarknight.akka.sample.hello;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.github.solarknight.akka.sample.hello.HelloWorld.Greet;
import com.github.solarknight.akka.sample.hello.HelloWorld.Greeted;
import com.github.solarknight.akka.sample.hello.HelloWorldMain.Start;

/**
 * @author peiheng.zph created on Dec 24, 2019
 * @version 1.0
 */
public class HelloWorldMain extends AbstractBehavior<Start> {

  public static class Start {
    public final String name;

    public Start(String name) {
      this.name = name;
    }
  }

  public static Behavior<Start> create() {
    return Behaviors.setup(HelloWorldMain::new);
  }

  private final ActorRef<Greet> greeter;

  private HelloWorldMain(ActorContext<Start> context) {
    super(context);
    greeter = context.spawn(HelloWorld.create(), "greeter");
  }

  @Override
  public Receive<Start> createReceive() {
    return newReceiveBuilder().onMessage(Start.class, this::onStart).build();
  }

  public Behavior<Start> onStart(Start command) {
    ActorRef<Greeted> bot = getContext().spawn(HelloWorldBot.create(3), command.name);
    greeter.tell(new HelloWorld.Greet(command.name, bot));
    return Behaviors.stopped();
  }

  public static void main(String[] args) {
    final ActorSystem<Start> system = ActorSystem.create(HelloWorldMain.create(), "hello");

    system.tell(new Start("World"));
    system.tell(new Start("Akka"));
  }
}
