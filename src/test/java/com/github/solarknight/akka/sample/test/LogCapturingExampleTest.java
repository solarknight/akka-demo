package com.github.solarknight.akka.sample.test;

import akka.actor.testkit.typed.javadsl.LogCapturing;
import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import com.github.solarknight.akka.sample.test.AsyncTestingExample.Echo;
import com.github.solarknight.akka.sample.test.AsyncTestingExample.Echo.Ping;
import com.github.solarknight.akka.sample.test.AsyncTestingExample.Echo.Pong;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author peiheng.zph created on Feb 01, 2020
 * @version 1.0
 */
public class LogCapturingExampleTest {

  @ClassRule public static final TestKitJunitResource testKit = new TestKitJunitResource();

  @Rule public final LogCapturing logCapturing = new LogCapturing();

  @Test
  public void testSomething() {
    ActorRef<Ping> pinger = testKit.spawn(Echo.create(), "ping");
    TestProbe<Pong> probe = testKit.createTestProbe();

    pinger.tell(new Echo.Ping("hello", probe.ref()));
    probe.expectMessage(new Echo.Pong("hello"));
  }
}
