package com.github.solarknight.akka.sample.basic;

import static java.util.Collections.emptyList;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import com.github.solarknight.akka.sample.basic.ActorAsFSMDemo.Buncher.Event;
import com.github.solarknight.akka.sample.basic.ActorAsFSMDemo.Buncher.Flush;
import com.github.solarknight.akka.sample.basic.ActorAsFSMDemo.Buncher.Queue;
import com.github.solarknight.akka.sample.basic.ActorAsFSMDemo.Buncher.SetTarget;
import com.github.solarknight.akka.sample.basic.ActorAsFSMDemo.Buncher.Timeout;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * @author peiheng.zph created on Jan 30, 2020
 * @version 1.0
 */
public class ActorAsFSMDemo {

  /** FSM states represented as behaviors */
  public abstract static class Buncher {

    public interface Event {}

    @RequiredArgsConstructor
    public static class SetTarget implements Event {
      public final ActorRef<Batch> ref;
    }

    public enum Timeout implements Event {
      INSTANCE
    }

    public enum Flush implements Event {
      INSTANCE
    }

    @RequiredArgsConstructor
    public static class Queue implements Event {
      public final Object obj;
    }
  }

  interface Data {}

  @RequiredArgsConstructor
  public static final class Todo implements Data {
    public final ActorRef<Batch> target;
    public final List<Object> queue;

    public Todo addElement(Object element) {
      List<Object> copy = new ArrayList<>(queue);
      copy.add(element);
      return new Todo(target, copy);
    }

    public Todo copy() {
      List<Object> copy = new ArrayList<>(queue);
      return new Todo(target, copy);
    }
  }

  @RequiredArgsConstructor
  public static final class Batch {
    public final List<Object> list;
  }

  // initial state
  public static Behavior<Event> create() {
    return uninitialized();
  }

  private static Behavior<Event> uninitialized() {
    return Behaviors.receive(Event.class)
        .onMessage(SetTarget.class, msg -> idle(new Todo(msg.ref, emptyList())))
        .build();
  }

  private static Behavior<Event> idle(Todo data) {
    return Behaviors.receive(Event.class)
        .onMessage(Queue.class, message -> active(data.addElement(message)))
        .build();
  }

  private static Behavior<Event> active(Todo data) {
    return Behaviors.withTimers(
        timers -> {
          // State timeouts done with withTimers
          timers.startSingleTimer("Timeout", Timeout.INSTANCE, Duration.ofSeconds(1));
          return Behaviors.receive(Event.class)
              .onMessage(Queue.class, message -> active(data.addElement(message)))
              .onMessage(Flush.class, message -> activeOnFlushOrTimeout(data))
              .onMessage(Timeout.class, message -> activeOnFlushOrTimeout(data))
              .build();
        });
  }

  private static Behavior<Event> activeOnFlushOrTimeout(Todo data) {
    data.target.tell(new Batch(data.queue));
    return idle(data.copy());
  }
}
