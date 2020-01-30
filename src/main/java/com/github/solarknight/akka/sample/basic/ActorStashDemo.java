package com.github.solarknight.akka.sample.basic;

import akka.Done;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.StashBuffer;
import java.util.concurrent.CompletionStage;
import lombok.RequiredArgsConstructor;

/**
 * @author peiheng.zph created on Jan 30, 2020
 * @version 1.0
 */
public class ActorStashDemo {

  interface DB {
    CompletionStage<Done> save(String id, String value);

    CompletionStage<String> load(String id);
  }

  public static class DataAccess {

    public interface Command {}

    @RequiredArgsConstructor
    public static class Save implements Command {
      public final String payload;
      public final ActorRef<Done> replyTo;
    }

    @RequiredArgsConstructor
    public static class Get implements Command {
      public final ActorRef<String> replyTo;
    }

    @RequiredArgsConstructor
    private static class InitialState implements Command {
      public final String value;
    }

    private enum SaveSuccess implements Command {
      INSTANCE
    }

    @RequiredArgsConstructor
    private static class DBError implements Command {
      public final RuntimeException cause;
    }

    public static Behavior<Command> create(String id, DB db) {
      return Behaviors.withStash(
          100,
          stash ->
              Behaviors.setup(
                  ctx -> {
                    ctx.pipeToSelf(
                        db.load(id),
                        (value, cause) -> {
                          if (cause == null) return new InitialState(value);
                          else return new DBError(asRuntimeException(cause));
                        });
                    return new DataAccess(ctx, stash, id, db).start();
                  }));
    }

    private final ActorContext<Command> context;
    private final StashBuffer<Command> buffer;
    private final String id;
    private final DB db;

    public DataAccess(
        ActorContext<Command> context, StashBuffer<Command> buffer, String id, DB db) {
      this.context = context;
      this.buffer = buffer;
      this.id = id;
      this.db = db;
    }

    private Behavior<Command> start() {
      return Behaviors.receive(Command.class)
          .onMessage(InitialState.class, this::onInitialState)
          .onMessage(DBError.class, this::onDBError)
          .onMessage(Command.class, this::stashOtherCommand)
          .build();
    }

    private Behavior<Command> onInitialState(InitialState message) {
      // now we are ready to handle stashed messages if any
      context.getLog().info("Switch to active state");
      return buffer.unstashAll(active(message.value));
    }

    private Behavior<Command> onDBError(DBError message) {
      throw message.cause;
    }

    private Behavior<Command> stashOtherCommand(Command message) {
      // stash all other messages for later processing
      buffer.stash(message);
      return Behaviors.same();
    }

    private Behavior<Command> active(String state) {
      return Behaviors.receive(Command.class)
          .onMessage(Get.class, message -> onGet(state, message))
          .onMessage(Save.class, this::onSave)
          .build();
    }

    private Behavior<Command> onGet(String state, Get message) {
      message.replyTo.tell(state);
      return Behaviors.same();
    }

    private Behavior<Command> onSave(Save message) {
      context.pipeToSelf(
          db.save(id, message.payload),
          (value, cause) -> {
            if (cause == null) return SaveSuccess.INSTANCE;
            else return new DBError(asRuntimeException(cause));
          });

      context.getLog().info("Switch to saving state");
      return saving(message.payload, message.replyTo);
    }

    private Behavior<Command> saving(String state, ActorRef<Done> replyTo) {
      return Behaviors.receive(Command.class)
          .onMessage(SaveSuccess.class, message -> onSaveSuccess(state, replyTo))
          .onMessage(DBError.class, this::onDBError)
          .onMessage(Command.class, this::stashOtherCommand)
          .build();
    }

    private Behavior<Command> onSaveSuccess(String state, ActorRef<Done> replyTo) {
      replyTo.tell(Done.getInstance());

      context.getLog().info("Switch to active state");
      return buffer.unstashAll(active(state));
    }

    private static RuntimeException asRuntimeException(Throwable t) {
      // can't throw Throwable in lambdas
      if (t instanceof RuntimeException) {
        return (RuntimeException) t;
      } else {
        return new RuntimeException(t);
      }
    }
  }
}
