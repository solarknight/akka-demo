package com.github.solarknight.akka.sample.basic;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.typed.ActorRef;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * @author peiheng.zph created on Jan 29, 2020
 * @version 1.0
 */
public class ActorCleanResourceDemoTest {

  @ClassRule public static final TestKitJunitResource testKit = new TestKitJunitResource();

  @Test
  public void testRestart() {
    ActorRef<String> supervising =
        testKit.spawn(ActorCleanResourceDemo.behavior(), "supervising-actor");

    supervising.tell("passed message");
    // Check resource is closed during PreRestart
    supervising.tell("failedMessage");
  }
}
