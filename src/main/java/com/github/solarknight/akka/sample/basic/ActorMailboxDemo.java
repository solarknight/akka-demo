package com.github.solarknight.akka.sample.basic;

import akka.dispatch.Envelope;
import akka.dispatch.MailboxType;
import akka.dispatch.MessageQueue;
import akka.dispatch.ProducesMessageQueue;
import com.github.solarknight.akka.sample.basic.ActorMailboxDemo.MyMessageQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import scala.Option;

/**
 * @author peiheng.zph created on Jan 31, 2020
 * @version 1.0
 */
public class ActorMailboxDemo implements MailboxType, ProducesMessageQueue<MyMessageQueue> {

  // Marker interface used for mailbox requirements mapping
  public interface MyUnboundedMessageQueueSemantics {}

  // This constructor signature must exist, it will be called by Akka
  public ActorMailboxDemo(
      akka.actor.ActorSystem.Settings settings, com.typesafe.config.Config config) {
    // put your initialization code here
  }

  @Override
  public MessageQueue create(
      Option<akka.actor.ActorRef> owner, Option<akka.actor.ActorSystem> system) {
    return new MyMessageQueue();
  }

  // This is the MessageQueue implementation
  public static class MyMessageQueue implements MessageQueue, MyUnboundedMessageQueueSemantics {
    private final Queue<Envelope> queue = new ConcurrentLinkedQueue<Envelope>();

    @Override
    public void enqueue(akka.actor.ActorRef receiver, Envelope handle) {
      queue.offer(handle);
      System.out.println("Element enqueued");
    }

    public Envelope dequeue() {
      System.out.println("Element dequeue");
      return queue.poll();
    }

    public int numberOfMessages() {
      return queue.size();
    }

    public boolean hasMessages() {
      return !queue.isEmpty();
    }

    @Override
    public void cleanUp(akka.actor.ActorRef owner, MessageQueue deadLetters) {
      for (Envelope handle : queue) {
        deadLetters.enqueue(owner, handle);
      }
    }
  }
}
