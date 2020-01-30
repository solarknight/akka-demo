package com.github.solarknight.akka.sample.basic;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.typed.ActorRef;
import com.github.solarknight.akka.sample.basic.ActorReceptionistDemo.Guardian;
import com.github.solarknight.akka.sample.basic.ActorReceptionistDemo.PingManager;
import com.github.solarknight.akka.sample.basic.ActorReceptionistDemo.PingManager.Command;
import com.github.solarknight.akka.sample.basic.ActorReceptionistDemo.PingManager.PingAll;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * @author peiheng.zph created on Jan 30, 2020
 * @version 1.0
 */
public class ActorReceptionistDemoTest {

  @ClassRule public static final TestKitJunitResource testKit = new TestKitJunitResource();

  @Test
  public void testReceptionist() {
    testKit.spawn(Guardian.create(), "guardian");
  }

  @Test
  public void testReceptionist2() {
    ActorRef<Command> pingManager = testKit.spawn(PingManager.create(), "pingManager");
    pingManager.tell(PingAll.INSTANCE);
  }
}
