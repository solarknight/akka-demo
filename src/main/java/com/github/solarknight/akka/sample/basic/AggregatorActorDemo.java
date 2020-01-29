package com.github.solarknight.akka.sample.basic;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.github.solarknight.akka.sample.basic.AggregatorActorDemo.Hotel1.RequestQuote;
import com.github.solarknight.akka.sample.basic.AggregatorActorDemo.Hotel2.RequestPrice;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * @author peiheng.zph created on Jan 29, 2020
 * @version 1.0
 */
public class AggregatorActorDemo {

  public static class Hotel1 {
    @RequiredArgsConstructor
    public static class RequestQuote {
      public final ActorRef<Quote> replyTo;
    }

    @RequiredArgsConstructor
    public static class Quote {
      public final String hotel;
      public final BigDecimal price;
    }
  }

  public static class Hotel2 {
    @RequiredArgsConstructor
    public static class RequestPrice {
      public final ActorRef<Price> replyTo;
    }

    @RequiredArgsConstructor
    public static class Price {
      public final String hotel;
      public final BigDecimal price;
    }
  }

  public static class HotelCustomer extends AbstractBehavior<HotelCustomer.Command> {
    public interface Command {}

    @Data
    public static class Quote {
      public final String hotel;
      public final BigDecimal price;
    }

    @RequiredArgsConstructor
    public static class AggregatedQuotes implements Command {
      public final List<Quote> quotes;
    }

    public static Behavior<Command> create(
        ActorRef<Hotel1.RequestQuote> hotel1, ActorRef<Hotel2.RequestPrice> hotel2) {
      return Behaviors.setup(context -> new HotelCustomer(context, hotel1, hotel2));
    }

    private HotelCustomer(
        ActorContext<Command> context,
        ActorRef<Hotel1.RequestQuote> hotel1,
        ActorRef<Hotel2.RequestPrice> hotel2) {
      super(context);

      // Object since no common type between Hotel1 and Hotel2
      Consumer<ActorRef<Object>> sendRequests =
          replyTo -> {
            hotel1.tell(new RequestQuote(replyTo.narrow()));
            hotel2.tell(new RequestPrice(replyTo.narrow()));
          };

      int expectedReplies = 2;

      context.spawnAnonymous(
          Aggregator.create(
              Object.class,
              sendRequests,
              expectedReplies,
              context.getSelf(),
              this::aggregateReplies,
              Duration.ofSeconds(5)));
    }

    private AggregatedQuotes aggregateReplies(List<Object> replies) {
      List<Quote> quotes =
          replies.stream()
              .map(
                  r -> {
                    // The hotels have different protocols with different replies,
                    // convert them to `HotelCustomer.Quote` that this actor understands.
                    if (r instanceof Hotel1.Quote) {
                      Hotel1.Quote q = (Hotel1.Quote) r;
                      return new Quote(q.hotel, q.price);
                    } else if (r instanceof Hotel2.Price) {
                      Hotel2.Price p = (Hotel2.Price) r;
                      return new Quote(p.hotel, p.price);
                    } else {
                      throw new IllegalArgumentException("Unknown reply " + r);
                    }
                  })
              .sorted(Comparator.comparing(a -> a.price))
              .collect(Collectors.toList());

      return new AggregatedQuotes(quotes);
    }

    @Override
    public Receive<Command> createReceive() {
      return newReceiveBuilder()
          .onMessage(AggregatedQuotes.class, this::onAggregatedQuotes)
          .build();
    }

    private Behavior<Command> onAggregatedQuotes(AggregatedQuotes message) {
      if (message.quotes.isEmpty()) {
        getContext().getLog().info("Best Quote N/A");

      } else {
        getContext().getLog().info("Best {}", message.quotes.get(0));
      }

      return this;
    }
  }
}
