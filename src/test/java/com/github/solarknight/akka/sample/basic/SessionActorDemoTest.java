package com.github.solarknight.akka.sample.basic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import com.github.solarknight.akka.sample.basic.SessionActorDemo.Home;
import com.github.solarknight.akka.sample.basic.SessionActorDemo.Home.Command;
import com.github.solarknight.akka.sample.basic.SessionActorDemo.Home.LeaveHome;
import com.github.solarknight.akka.sample.basic.SessionActorDemo.Home.ReadyToLeaveHome;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * @author peiheng.zph created on Jan 29, 2020
 * @version 1.0
 */
public class SessionActorDemoTest {

  @ClassRule public static final TestKitJunitResource testKit = new TestKitJunitResource();

  @Test
  public void testSuccess() {
    ActorRef<Command> home = testKit.spawn(Home.create(), "home");
    TestProbe<ReadyToLeaveHome> testProbe = testKit.createTestProbe(ReadyToLeaveHome.class);

    home.tell(new LeaveHome("Tom", testProbe.getRef()));
    ReadyToLeaveHome message = testProbe.receiveMessage();

    assertNotNull(message);
    assertEquals("Tom", message.who);
  }
}
