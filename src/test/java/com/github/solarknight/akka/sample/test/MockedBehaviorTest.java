package com.github.solarknight.akka.sample.test;

import static org.junit.Assert.assertEquals;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.Scheduler;
import akka.actor.typed.javadsl.AskPattern;
import akka.actor.typed.javadsl.Behaviors;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * @author peiheng.zph created on Jan 31, 2020
 * @version 1.0
 */
public class MockedBehaviorTest {

  @ClassRule public static final TestKitJunitResource testKit = new TestKitJunitResource();

  @RequiredArgsConstructor
  static class Message {
    public final int i;
    public final ActorRef<Integer> replyTo;
  }

  public static class Producer {

    private Scheduler scheduler;
    private ActorRef<Message> publisher;

    Producer(Scheduler scheduler, ActorRef<Message> publisher) {
      this.scheduler = scheduler;
      this.publisher = publisher;
    }

    public void produce(int messages) {
      IntStream.range(0, messages).forEach(this::publish);
    }

    private CompletionStage<Integer> publish(int i) {
      return AskPattern.ask(
          publisher,
          (ActorRef<Integer> ref) -> new Message(i, ref),
          Duration.ofSeconds(3),
          scheduler);
    }
  }

  @Test
  public void test() {
    // simulate the happy path
    Behavior<Message> mockedBehavior =
        Behaviors.receiveMessage(
            message -> {
              message.replyTo.tell(message.i);
              return Behaviors.same();
            });
    TestProbe<Message> probe = testKit.createTestProbe();
    ActorRef<Message> mockedPublisher =
        testKit.spawn(Behaviors.monitor(Message.class, probe.ref(), mockedBehavior));

    // test our component
    Producer producer = new Producer(testKit.scheduler(), mockedPublisher);
    int messages = 3;
    producer.produce(messages);

    // verify expected behavior
    IntStream.range(0, messages)
        .forEach(
            i -> {
              Message msg = probe.expectMessageClass(Message.class);
              assertEquals(i, msg.i);
            });
  }
}
