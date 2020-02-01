package com.github.solarknight.akka.sample.test;

import akka.actor.testkit.typed.javadsl.ManualTime;
import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import java.time.Duration;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * @author peiheng.zph created on Feb 01, 2020
 * @version 1.0
 */
public class ManualTimerExampleTest {

  @ClassRule
  public static final TestKitJunitResource testKit = new TestKitJunitResource(ManualTime.config());

  private final ManualTime manualTime = ManualTime.get(testKit.system());

  static final class Tick {
    private Tick() {}

    static final Tick INSTANCE = new Tick();
  }

  static final class Tock {}

  @Test
  public void testScheduleNonRepeatedTicks() {
    TestProbe<Tock> probe = testKit.createTestProbe();
    Behavior<Tick> behavior =
        Behaviors.withTimers(
            timer -> {
              timer.startSingleTimer(Tick.INSTANCE, Duration.ofMillis(10));
              return Behaviors.receiveMessage(
                  tick -> {
                    probe.ref().tell(new Tock());
                    return Behaviors.same();
                  });
            });

    testKit.spawn(behavior);

    manualTime.expectNoMessageFor(Duration.ofMillis(9), probe);

    manualTime.timePasses(Duration.ofMillis(2));
    probe.expectMessageClass(Tock.class);

    manualTime.expectNoMessageFor(Duration.ofSeconds(10), probe);
  }
}
