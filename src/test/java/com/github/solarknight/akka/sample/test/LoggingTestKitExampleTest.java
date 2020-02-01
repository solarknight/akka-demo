package com.github.solarknight.akka.sample.test;

import akka.actor.testkit.typed.javadsl.LoggingTestKit;
import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * @author peiheng.zph created on Feb 01, 2020
 * @version 1.0
 */
public class LoggingTestKitExampleTest {

  @ClassRule public static final TestKitJunitResource testKit = new TestKitJunitResource();

  @Test
  public void test1() {
    Behavior<String> behavior =
        Behaviors.setup(
            (ctx) -> {
              ctx.getLog().info("Initialized");
              return Behaviors.ignore();
            });

    LoggingTestKit.info("Initialized").expect(testKit.system(), () -> testKit.spawn(behavior));
  }

  @Test
  public void test2() {
    Behavior<String> behavior =
        Behaviors.setup(
            (ctx) -> {
              ctx.getLog()
                  .error("Input was rejected", new IllegalArgumentException("Input was rejected"));
              return Behaviors.ignore();
            });

    LoggingTestKit.error(IllegalArgumentException.class)
        .withMessageRegex(".*was rejected")
        .expect(testKit.system(), () -> testKit.spawn(behavior));
  }
}
