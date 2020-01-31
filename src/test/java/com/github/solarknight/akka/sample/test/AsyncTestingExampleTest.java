package com.github.solarknight.akka.sample.test;

import akka.actor.testkit.typed.javadsl.ActorTestKit;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import com.github.solarknight.akka.sample.test.AsyncTestingExample.Echo;
import com.github.solarknight.akka.sample.test.AsyncTestingExample.Echo.Ping;
import com.github.solarknight.akka.sample.test.AsyncTestingExample.Echo.Pong;
import java.time.Duration;
import org.junit.AfterClass;
import org.junit.Test;

/**
 * @author peiheng.zph created on Jan 31, 2020
 * @version 1.0
 */
public class AsyncTestingExampleTest {

  static final ActorTestKit testKit = ActorTestKit.create();

  @Test
  public void test1() {
    ActorRef<Ping> pinger = testKit.spawn(Echo.create());
    TestProbe<Pong> probe = testKit.createTestProbe();

    pinger.tell(new Echo.Ping("hello", probe.ref()));
    probe.expectMessage(new Echo.Pong("hello"));
  }

  @Test
  public void test2() {
    ActorRef<Echo.Ping> pinger1 = testKit.spawn(Echo.create(), "pinger");
    TestProbe<Pong> probe = testKit.createTestProbe();

    pinger1.tell(new Echo.Ping("hello", probe.ref()));
    probe.expectMessage(new Echo.Pong("hello"));

    testKit.stop(pinger1);

    // Immediately creating an actor with the same name
    ActorRef<Echo.Ping> pinger2 = testKit.spawn(Echo.create(), "pinger");

    pinger2.tell(new Echo.Ping("hello2", probe.ref()));
    probe.expectMessage(new Echo.Pong("hello2"));

    testKit.stop(pinger2, Duration.ofSeconds(10));
  }

  /**
   * Use TestKitJunitResource to have the async test kit automatically shutdown when the test is
   * complete.
   */
  @AfterClass
  public static void cleanup() {
    testKit.shutdownTestKit();
  }
}
