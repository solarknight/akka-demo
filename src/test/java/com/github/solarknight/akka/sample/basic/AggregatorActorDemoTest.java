package com.github.solarknight.akka.sample.basic;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import com.github.solarknight.akka.sample.basic.AggregatorActorDemo.Hotel1;
import com.github.solarknight.akka.sample.basic.AggregatorActorDemo.Hotel1.Quote;
import com.github.solarknight.akka.sample.basic.AggregatorActorDemo.Hotel1.RequestQuote;
import com.github.solarknight.akka.sample.basic.AggregatorActorDemo.Hotel2;
import com.github.solarknight.akka.sample.basic.AggregatorActorDemo.Hotel2.Price;
import com.github.solarknight.akka.sample.basic.AggregatorActorDemo.Hotel2.RequestPrice;
import com.github.solarknight.akka.sample.basic.AggregatorActorDemo.HotelCustomer;
import com.github.solarknight.akka.sample.basic.AggregatorActorDemo.HotelCustomer.Command;
import java.math.BigDecimal;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * @author peiheng.zph created on Jan 29, 2020
 * @version 1.0
 */
public class AggregatorActorDemoTest {

  @ClassRule public static final TestKitJunitResource testKit = new TestKitJunitResource();

  @Test
  public void testAggregate() {
    TestProbe<RequestQuote> hotel1 = testKit.createTestProbe(Hotel1.RequestQuote.class);
    TestProbe<RequestPrice> hotel2 = testKit.createTestProbe(Hotel2.RequestPrice.class);
    ActorRef<Command> customer =
        testKit.spawn(HotelCustomer.create(hotel1.getRef(), hotel2.getRef()));

    RequestQuote requestQuote = hotel1.receiveMessage();
    RequestPrice requestPrice = hotel2.receiveMessage();

    requestQuote.replyTo.tell(new Quote("Hotel1", BigDecimal.ONE));
    requestPrice.replyTo.tell(new Price("Hotel2", BigDecimal.TEN));
  }
}
