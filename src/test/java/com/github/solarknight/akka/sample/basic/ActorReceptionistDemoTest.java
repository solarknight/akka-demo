package com.github.solarknight.akka.sample.basic;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import com.github.solarknight.akka.sample.basic.ActorReceptionistDemo.Guardian;
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
}
