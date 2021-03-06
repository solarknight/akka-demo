package com.github.solarknight.akka.sample.basic;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.typed.ActorRef;
import com.github.solarknight.akka.sample.basic.ActorRestartDemo.SupervisingActor;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * @author solarknight created on Dec 04, 2019
 * @version 1.0
 */
public class ActorRestartDemoTest {

  @ClassRule public static final TestKitJunitResource testKit = new TestKitJunitResource();

  @Test
  public void testRestart() {
    ActorRef<String> supervising = testKit.spawn(SupervisingActor.create(), "supervising-actor");
    supervising.tell("failChild");
  }
}
