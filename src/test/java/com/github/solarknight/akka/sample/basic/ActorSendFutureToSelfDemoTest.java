package com.github.solarknight.akka.sample.basic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import akka.Done;
import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import com.github.solarknight.akka.sample.basic.ActorSendFutureToSelfDemo.Customer;
import com.github.solarknight.akka.sample.basic.ActorSendFutureToSelfDemo.CustomerDataAccess;
import com.github.solarknight.akka.sample.basic.ActorSendFutureToSelfDemo.CustomerRepository;
import com.github.solarknight.akka.sample.basic.ActorSendFutureToSelfDemo.CustomerRepository.Command;
import com.github.solarknight.akka.sample.basic.ActorSendFutureToSelfDemo.CustomerRepository.OperationResult;
import com.github.solarknight.akka.sample.basic.ActorSendFutureToSelfDemo.CustomerRepository.Update;
import com.github.solarknight.akka.sample.basic.ActorSendFutureToSelfDemo.CustomerRepository.UpdateSuccess;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * @author peiheng.zph created on Jan 28, 2020
 * @version 1.0
 */
public class ActorSendFutureToSelfDemoTest {

  @ClassRule public static final TestKitJunitResource testKit = new TestKitJunitResource();

  @Test
  public void testUpdate() {
    CustomerDataAccess customerDataAccess = customer -> CompletableFuture.supplyAsync(Done::done);
    ActorRef<Command> actorRef =
        testKit.spawn(CustomerRepository.create(customerDataAccess), "repository");
    TestProbe<OperationResult> replyTo = testKit.createTestProbe(OperationResult.class);

    Customer customer = new Customer("1", 0, "test", "");
    actorRef.tell(new Update(customer, replyTo.getRef()));

    OperationResult result = replyTo.receiveMessage(Duration.ofSeconds(1));
    assertTrue(result instanceof UpdateSuccess);
    assertEquals("1", ((UpdateSuccess) result).id);
  }
}
