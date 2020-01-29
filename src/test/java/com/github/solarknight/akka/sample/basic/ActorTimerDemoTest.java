package com.github.solarknight.akka.sample.basic;

import static org.junit.Assert.assertEquals;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import com.github.solarknight.akka.sample.basic.ActorTimerDemo.Buncher;
import com.github.solarknight.akka.sample.basic.ActorTimerDemo.Buncher.Command;
import com.github.solarknight.akka.sample.basic.ActorTimerDemo.Buncher.ExcitingMessage;
import java.time.Duration;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * @author peiheng.zph created on Jan 29, 2020
 * @version 1.0
 */
public class ActorTimerDemoTest {

  @ClassRule public static final TestKitJunitResource testKit = new TestKitJunitResource();

  @Test
  public void testSuccess() {
    Duration delay = Duration.ofSeconds(2);
    TestProbe<Buncher.Batch> testProbe = testKit.createTestProbe(Buncher.Batch.class);
    ActorRef<Command> buncher = testKit.spawn(Buncher.create(testProbe.getRef(), delay, 2));

    ExcitingMessage message1 = new ExcitingMessage("message1");
    ExcitingMessage message2 = new ExcitingMessage("message2");
    buncher.tell(message1);
    buncher.tell(message2);

    Buncher.Batch batch = testProbe.receiveMessage();
    assertEquals(2, batch.getMessages().size());
    assertEquals(message1, batch.getMessages().get(0));
    assertEquals(message2, batch.getMessages().get(1));
  }

  @Test
  public void testTimeout() {
    Duration delay = Duration.ofSeconds(2);
    TestProbe<Buncher.Batch> testProbe = testKit.createTestProbe(Buncher.Batch.class);
    ActorRef<Command> buncher = testKit.spawn(Buncher.create(testProbe.getRef(), delay, 2));

    ExcitingMessage message1 = new ExcitingMessage("message1");
    buncher.tell(message1);

    Buncher.Batch batch = testProbe.receiveMessage(Duration.ofSeconds(3));
    assertEquals(1, batch.getMessages().size());
    assertEquals(message1, batch.getMessages().get(0));
  }
}
