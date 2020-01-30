package com.github.solarknight.akka.sample.basic;

import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static org.junit.Assert.assertEquals;

import akka.Done;
import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import com.github.solarknight.akka.sample.basic.ActorStashDemo.DB;
import com.github.solarknight.akka.sample.basic.ActorStashDemo.DataAccess;
import com.github.solarknight.akka.sample.basic.ActorStashDemo.DataAccess.Command;
import com.github.solarknight.akka.sample.basic.ActorStashDemo.DataAccess.Get;
import com.github.solarknight.akka.sample.basic.ActorStashDemo.DataAccess.Save;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * @author peiheng.zph created on Jan 30, 2020
 * @version 1.0
 */
public class ActorStashDemoTest {

  @ClassRule public static final TestKitJunitResource testKit = new TestKitJunitResource();

  @Test
  public void test() {
    DB db =
        new DB() {
          private String value = "OK";

          @Override
          public CompletionStage<Done> save(String id, String value) {
            sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
            this.value = value;
            return CompletableFuture.supplyAsync(Done::done);
          }

          @Override
          public CompletionStage<String> load(String id) {
            sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
            return CompletableFuture.supplyAsync(() -> value);
          }
        };

    ActorRef<Command> dataAccess = testKit.spawn(DataAccess.create("DAL", db), "DAL");
    TestProbe<String> testProbe = testKit.createTestProbe(String.class);
    TestProbe<Done> testProbe2 = testKit.createTestProbe(Done.class);

    dataAccess.tell(new Get(testProbe.getRef()));

    String value = testProbe.receiveMessage(Duration.ofSeconds(1));
    assertEquals("OK", value);

    dataAccess.tell(new Save("Hello", testProbe2.getRef()));
    dataAccess.tell(new Get(testProbe.getRef()));

    value = testProbe.receiveMessage(Duration.ofSeconds(1));
    assertEquals("Hello", value);
  }
}
