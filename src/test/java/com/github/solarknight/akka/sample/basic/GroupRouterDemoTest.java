package com.github.solarknight.akka.sample.basic;

import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.typed.ActorRef;
import com.github.solarknight.akka.sample.basic.GroupRouterDemo.Guardian;
import com.github.solarknight.akka.sample.basic.GroupRouterDemo.Guardian.Command;
import com.github.solarknight.akka.sample.basic.GroupRouterDemo.Guardian.Stop;
import java.util.concurrent.TimeUnit;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * @author peiheng.zph created on Jan 30, 2020
 * @version 1.0
 */
public class GroupRouterDemoTest {

  @ClassRule public static final TestKitJunitResource testKit = new TestKitJunitResource();

  @Test
  public void test() {
    ActorRef<Command> actorRef = testKit.spawn(Guardian.create(), "guardian");

    // Wait for messages to be processed
    sleepUninterruptibly(1, TimeUnit.SECONDS);
    actorRef.tell(Stop.INSTANCE);
  }
}