package com.github.solarknight.akka.sample.basic;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.EntityRef;
import akka.cluster.sharding.typed.javadsl.EntityTypeKey;
import lombok.Data;

/**
 * @see <a
 *     href=https://doc.akka.io/docs/akka/current/typed/interaction-patterns.html#responding-to-a-sharded-actor>Responding
 *     to a sharded actor</a>
 * @author peiheng.zph created on Jan 29, 2020
 * @version 1.0
 */
public class ShardedActorDemo {

  // a sharded actor that needs counter updates
  public static class CounterConsumer {
    public static EntityTypeKey<Command> typeKey =
        EntityTypeKey.create(Command.class, "example-sharded-response");

    public interface Command {}

    @Data
    public static class NewCount implements Command {
      public final long value;
    }
  }

  public static class Counter extends AbstractBehavior<Counter.Command> {
    public static EntityTypeKey<Command> typeKey =
        EntityTypeKey.create(Command.class, "example-sharded-counter");

    public interface Command {}

    public enum Increment implements Command {
      INSTANCE
    }

    @Data
    public static class GetValue implements Command {
      public final String replyToEntityId;
    }

    public static Behavior<Command> create() {
      return Behaviors.setup(context -> new Counter(context));
    }

    private final ClusterSharding sharding;
    private int value = 0;

    private Counter(ActorContext<Counter.Command> context) {
      super(context);
      this.sharding = ClusterSharding.get(context.getSystem());
    }

    @Override
    public Receive<Command> createReceive() {
      return newReceiveBuilder()
          .onMessage(Increment.class, msg -> onIncrement())
          .onMessage(GetValue.class, this::onGetValue)
          .build();
    }

    private Behavior<Command> onIncrement() {
      value++;
      return this;
    }

    private Behavior<Command> onGetValue(GetValue msg) {
      EntityRef<CounterConsumer.Command> entityRef =
          sharding.entityRefFor(CounterConsumer.typeKey, msg.replyToEntityId);
      entityRef.tell(new CounterConsumer.NewCount(value));
      return this;
    }
  }
}
