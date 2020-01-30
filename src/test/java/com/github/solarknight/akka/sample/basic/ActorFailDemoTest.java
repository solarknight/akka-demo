package com.github.solarknight.akka.sample.basic;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.typed.ActorRef;
import com.github.solarknight.akka.sample.basic.ActorFailDemo.Boss;
import com.github.solarknight.akka.sample.basic.ActorFailDemo.Protocol.Command;
import com.github.solarknight.akka.sample.basic.ActorFailDemo.Protocol.Fail;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * @author peiheng.zph created on Jan 30, 2020
 * @version 1.0
 */
public class ActorFailDemoTest {

  @ClassRule public static final TestKitJunitResource testKit = new TestKitJunitResource();

  @Test
  public void testFail() {
    ActorRef<Command> boss = testKit.spawn(Boss.create(), "boss");
    boss.tell(new Fail("IllegalState"));
  }
}