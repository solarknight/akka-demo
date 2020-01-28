package com.github.solarknight.akka.sample.style;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;

/**
 * @author peiheng.zph created on Dec 27, 2019
 * @version 1.0
 */
public class Counter1 {

  public interface Command {}

  public enum Increment implements Command {
    INSTANCE
  }

  public static class GetValue implements Command {
    public final ActorRef<Value> replyTo;

    public GetValue(ActorRef<Value> replyTo) {
      this.replyTo = replyTo;
    }
  }

  public static class Value {
    public final int value;

    public Value(int value) {
      this.value = value;
    }
  }

  public static Behavior<Command> create() {
    return Behaviors.setup(context -> new Counter1(context).counter(0));
  }

  private final ActorContext<Command> context;

  private Counter1(ActorContext<Command> context) {
    this.context = context;
  }

  private Behavior<Command> counter(final int n) {
    return Behaviors.receive(Command.class)
        .onMessage(Increment.class, notUsed -> onIncrement(n))
        .onMessage(GetValue.class, command -> onGetValue(n, command))
        .build();
  }

  private Behavior<Command> onIncrement(int n) {
    int newValue = n + 1;
    context.getLog().debug("Incremented counter to [{}]", newValue);
    return counter(newValue);
  }

  private Behavior<Command> onGetValue(int n, GetValue command) {
    command.replyTo.tell(new Value(n));
    return Behaviors.same();
  }
}
