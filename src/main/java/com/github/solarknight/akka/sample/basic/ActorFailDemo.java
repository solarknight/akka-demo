package com.github.solarknight.akka.sample.basic;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.github.solarknight.akka.sample.basic.ActorFailDemo.Protocol.Command;
import com.github.solarknight.akka.sample.basic.ActorFailDemo.Protocol.Fail;
import com.github.solarknight.akka.sample.basic.ActorFailDemo.Protocol.Hello;

/**
 * @author peiheng.zph created on Jan 30, 2020
 * @version 1.0
 */
public class ActorFailDemo {

  public interface Protocol {
    interface Command {}

    class Fail implements Command {
      public final String text;

      public Fail(String text) {
        this.text = text;
      }
    }

    class Hello implements Command {
      public final String text;
      public final ActorRef<String> replyTo;

      public Hello(String text, ActorRef<String> replyTo) {
        this.text = text;
        this.replyTo = replyTo;
      }
    }
  }

  public static class Worker extends AbstractBehavior<Protocol.Command> {

    public static Behavior<Command> create() {
      return Behaviors.setup(Worker::new);
    }

    private Worker(ActorContext<Command> context) {
      super(context);
    }

    @Override
    public Receive<Command> createReceive() {
      return newReceiveBuilder()
          .onMessage(Protocol.Fail.class, this::onFail)
          .onMessage(Protocol.Hello.class, this::onHello)
          .build();
    }

    private Behavior<Command> onFail(Fail message) {
      throw new RuntimeException(message.text);
    }

    private Behavior<Command> onHello(Hello message) {
      message.replyTo.tell(message.text);
      return this;
    }
  }

  public static class MiddleManagement extends AbstractBehavior<Protocol.Command> {

    public static Behavior<Protocol.Command> create() {
      return Behaviors.setup(MiddleManagement::new);
    }

    private final ActorRef<Protocol.Command> child;

    private MiddleManagement(ActorContext<Protocol.Command> context) {
      super(context);

      context.getLog().info("Middle management starting up");

      // default supervision of child, meaning that it will stop on failure
      child = context.spawn(Worker.create(), "child");
      context.watch(child);
    }

    @Override
    public Receive<Command> createReceive() {
      // here we don't handle Terminated at all which means that
      // when the child fails or stops gracefully this actor will
      // fail with a DeathPactException
      return newReceiveBuilder().onMessage(Protocol.Command.class, this::onCommand).build();
    }

    private Behavior<Command> onCommand(Protocol.Command message) {
      child.tell(message);
      return this;
    }
  }

  public static class Boss extends AbstractBehavior<Protocol.Command> {
    public static Behavior<Protocol.Command> create() {
      return Behaviors.setup(Boss::new);
    }

    private final ActorRef<Protocol.Command> middle;

    private Boss(ActorContext<Protocol.Command> context) {
      super(context);
      context.getLog().info("Boss starting up");

      // default supervision of child, meaning that it will stop on failure
      middle = context.spawn(MiddleManagement.create(), "middle");
      context.watch(middle);
    }

    @Override
    public Receive<Command> createReceive() {
      // here we don't handle Terminated at all which means that
      // when middle management fails with a DeathPactException
      // this actor will also fail
      return newReceiveBuilder().onMessage(Protocol.Command.class, this::onCommand).build();
    }

    private Behavior<Protocol.Command> onCommand(Protocol.Command message) {
      // just pass messages on to the child
      middle.tell(message);
      return this;
    }
  }
}
