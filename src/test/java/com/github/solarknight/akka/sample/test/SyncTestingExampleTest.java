package com.github.solarknight.akka.sample.test;

import static org.junit.Assert.assertEquals;

import akka.actor.testkit.typed.CapturedLogEvent;
import akka.actor.testkit.typed.Effect;
import akka.actor.testkit.typed.javadsl.BehaviorTestKit;
import akka.actor.testkit.typed.javadsl.TestInbox;
import com.github.solarknight.akka.sample.test.SyncTestingExample.Hello;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.slf4j.event.Level;

/**
 * @author peiheng.zph created on Feb 01, 2020
 * @version 1.0
 */
public class SyncTestingExampleTest {

  @Test
  public void testSpawningChildren() {
    BehaviorTestKit<Hello.Command> test = BehaviorTestKit.create(Hello.create());
    test.run(new Hello.CreateAChild("child"));
    assertEquals("child", test.expectEffectClass(Effect.Spawned.class).childName());
  }

  @Test
  public void testSpawningChildrenAnonymously() {
    BehaviorTestKit<Hello.Command> test = BehaviorTestKit.create(Hello.create());
    test.run(Hello.CreateAnAnonymousChild.INSTANCE);
    test.expectEffectClass(Effect.SpawnedAnonymous.class);
  }

  @Test
  public void testSendingMessages() {
    BehaviorTestKit<Hello.Command> test = BehaviorTestKit.create(Hello.create());
    TestInbox<String> inbox = TestInbox.create();

    test.run(new Hello.SayHello(inbox.getRef()));
    inbox.expectMessage("hello");
  }

  @Test
  public void testSendingMessages2() {
    BehaviorTestKit<Hello.Command> testKit = BehaviorTestKit.create(Hello.create());
    testKit.run(new Hello.SayHelloToChild("child"));

    TestInbox<String> childInbox = testKit.childInbox("child");
    childInbox.expectMessage("hello");
  }

  @Test
  public void testSendingMessages3() {
    BehaviorTestKit<Hello.Command> testKit = BehaviorTestKit.create(Hello.create());
    testKit.run(Hello.SayHelloToAnonymousChild.INSTANCE);

    // Anonymous actors are created as: $a $b etc
    TestInbox<String> childInbox = testKit.childInbox("$a");
    childInbox.expectMessage("hello stranger");
  }

  @Test
  public void testLogging() {
    BehaviorTestKit<Hello.Command> test = BehaviorTestKit.create(Hello.create());
    TestInbox<String> inbox = TestInbox.create("Inboxer");
    test.run(new Hello.LogAndSayHello(inbox.getRef()));

    List<CapturedLogEvent> allLogEntries = test.getAllLogEntries();
    assertEquals(1, allLogEntries.size());
    CapturedLogEvent expectedLogEvent =
        new CapturedLogEvent(
            Level.INFO,
            "Saying hello to Inboxer",
            Optional.empty(),
            Optional.empty(),
            new HashMap<>());
    assertEquals(expectedLogEvent, allLogEntries.get(0));
  }
}
