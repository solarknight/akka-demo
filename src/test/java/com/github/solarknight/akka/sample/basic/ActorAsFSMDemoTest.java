package com.github.solarknight.akka.sample.basic;

import static org.junit.Assert.assertEquals;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import com.github.solarknight.akka.sample.basic.ActorAsFSMDemo.Batch;
import com.github.solarknight.akka.sample.basic.ActorAsFSMDemo.Buncher.Event;
import com.github.solarknight.akka.sample.basic.ActorAsFSMDemo.Buncher.Flush;
import com.github.solarknight.akka.sample.basic.ActorAsFSMDemo.Buncher.Queue;
import com.github.solarknight.akka.sample.basic.ActorAsFSMDemo.Buncher.SetTarget;
import java.time.Duration;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * @author peiheng.zph created on Jan 30, 2020
 * @version 1.0
 */
public class ActorAsFSMDemoTest {

  @ClassRule public static final TestKitJunitResource testKit = new TestKitJunitResource();

  @Test
  public void testTimeout() {
    ActorRef<Event> fsm = testKit.spawn(ActorAsFSMDemo.create(), "fsm");
    TestProbe<Batch> testProbe = testKit.createTestProbe(Batch.class);

    fsm.tell(new SetTarget(testProbe.getRef()));
    fsm.tell(new Queue(new Object()));

    Batch batch = testProbe.receiveMessage(Duration.ofSeconds(2));
    assertEquals(1, batch.list.size());
  }

  @Test
  public void testFlush() {
    ActorRef<Event> fsm = testKit.spawn(ActorAsFSMDemo.create(), "fsm2");
    TestProbe<Batch> testProbe = testKit.createTestProbe(Batch.class);

    fsm.tell(new SetTarget(testProbe.getRef()));
    fsm.tell(new Queue(new Object()));
    fsm.tell(new Queue(new Object()));
    fsm.tell(Flush.INSTANCE);

    Batch batch = testProbe.receiveMessage();
    assertEquals(2, batch.list.size());
  }
}
