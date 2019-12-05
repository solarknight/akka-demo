package com.github.solarknight.demo;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.typed.ActorRef;
import com.github.solarknight.demo.ActorStopDemo.StartStopActor1;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * @author peiheng.zph created on Dec 04, 2019
 * @version 1.0
 */
public class ActorStopDemoTest {

  @ClassRule
  public static final TestKitJunitResource testKit = new TestKitJunitResource();

  @Test
  public void testStop() {
    ActorRef<String> first = testKit.spawn(StartStopActor1.create(), "first");
    first.tell("stop");
  }

}