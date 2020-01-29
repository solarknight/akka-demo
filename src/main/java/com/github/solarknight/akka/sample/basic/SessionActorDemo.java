package com.github.solarknight.akka.sample.basic;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.TimerScheduler;
import com.github.solarknight.akka.sample.basic.SessionActorDemo.Drawer.GetWallet;
import com.github.solarknight.akka.sample.basic.SessionActorDemo.Home.ReadyToLeaveHome;
import com.github.solarknight.akka.sample.basic.SessionActorDemo.KeyCabinet.GetKeys;
import java.time.Duration;
import java.util.Optional;

/**
 * @see <a
 *     href="https://doc.akka.io/docs/akka/current/typed/interaction-patterns.html#per-session-child-actor">Per
 *     session child Actor</a>
 * @author peiheng.zph created on Jan 29, 2020
 * @version 1.0
 */
public class SessionActorDemo {

  public static class Keys {}

  public static class Wallet {}

  public static class KeyCabinet {
    public static class GetKeys {
      public final String whoseKeys;
      public final ActorRef<Keys> replyTo;

      public GetKeys(String whoseKeys, ActorRef<Keys> respondTo) {
        this.whoseKeys = whoseKeys;
        this.replyTo = respondTo;
      }
    }

    public static Behavior<GetKeys> create() {
      return Behaviors.receiveMessage(KeyCabinet::onGetKeys);
    }

    private static Behavior<GetKeys> onGetKeys(GetKeys message) {
      message.replyTo.tell(new Keys());
      return Behaviors.same();
    }
  }

  public static class Drawer {
    public static class GetWallet {
      public final String whoseWallet;
      public final ActorRef<Wallet> replyTo;

      public GetWallet(String whoseWallet, ActorRef<Wallet> replyTo) {
        this.whoseWallet = whoseWallet;
        this.replyTo = replyTo;
      }
    }

    public static Behavior<GetWallet> create() {
      return Behaviors.receiveMessage(Drawer::onGetWallet);
    }

    private static Behavior<GetWallet> onGetWallet(GetWallet message) {
      message.replyTo.tell(new Wallet());
      return Behaviors.same();
    }
  }

  public static class Home {
    public interface Command {}

    public static class LeaveHome implements Command {
      public final String who;
      public final ActorRef<ReadyToLeaveHome> respondTo;

      public LeaveHome(String who, ActorRef<ReadyToLeaveHome> respondTo) {
        this.who = who;
        this.respondTo = respondTo;
      }
    }

    public static class ReadyToLeaveHome {
      public final String who;
      public final Keys keys;
      public final Wallet wallet;

      public ReadyToLeaveHome(String who, Keys keys, Wallet wallet) {
        this.who = who;
        this.keys = keys;
        this.wallet = wallet;
      }
    }

    private final ActorContext<Command> context;

    private final ActorRef<KeyCabinet.GetKeys> keyCabinet;
    private final ActorRef<Drawer.GetWallet> drawer;

    public static Behavior<Command> create() {
      return Behaviors.setup(context -> new Home(context).behavior());
    }

    private Home(ActorContext<Command> context) {
      this.context = context;
      this.keyCabinet = context.spawn(KeyCabinet.create(), "key-cabinet");
      this.drawer = context.spawn(Drawer.create(), "drawer");
    }

    private Behavior<Command> behavior() {
      return Behaviors.receive(Command.class).onMessage(LeaveHome.class, this::onLeaveHome).build();
    }

    private Behavior<Command> onLeaveHome(LeaveHome message) {
      context.spawn(
          PrepareToLeaveHome.create(message.who, message.respondTo, keyCabinet, drawer),
          "leaving" + message.who);
      return Behaviors.same();
    }
  }

  static class PrepareToLeaveHome extends AbstractBehavior<Object> {

    private static final Duration TTL = Duration.ofSeconds(2);

    static Behavior<Object> create(
        String who,
        ActorRef<Home.ReadyToLeaveHome> replyTo,
        ActorRef<KeyCabinet.GetKeys> keyCabinet,
        ActorRef<Drawer.GetWallet> drawer) {
      return Behaviors.setup(
          context ->
              Behaviors.withTimers(
                  timers ->
                      new PrepareToLeaveHome(context, who, replyTo, keyCabinet, drawer, timers)));
    }

    enum Timeout {
      INSTANCE
    }

    private final String who;
    private final ActorRef<Home.ReadyToLeaveHome> replyTo;
    private final ActorRef<KeyCabinet.GetKeys> keyCabinet;
    private final ActorRef<Drawer.GetWallet> drawer;
    private Optional<Wallet> wallet = Optional.empty();
    private Optional<Keys> keys = Optional.empty();

    public PrepareToLeaveHome(
        ActorContext<Object> context,
        String who,
        ActorRef<ReadyToLeaveHome> replyTo,
        ActorRef<GetKeys> keyCabinet,
        ActorRef<GetWallet> drawer,
        TimerScheduler<Object> timers) {
      super(context);
      this.who = who;
      this.replyTo = replyTo;
      this.keyCabinet = keyCabinet;
      this.drawer = drawer;

      keyCabinet.tell(new GetKeys(who, getContext().getSelf().narrow()));
      drawer.tell(new GetWallet(who, getContext().getSelf().narrow()));
      timers.startSingleTimer("Schedule-timer", Timeout.INSTANCE, TTL);
    }

    @Override
    public Receive<Object> createReceive() {
      return newReceiveBuilder()
          .onMessage(Keys.class, this::onKeys)
          .onMessage(Wallet.class, this::onWallet)
          .onMessage(Timeout.class, this::onTimeout)
          .build();
    }

    private Behavior<Object> onKeys(Keys keys) {
      this.keys = Optional.of(keys);
      return completeOrContinue();
    }

    private Behavior<Object> onWallet(Wallet wallet) {
      this.wallet = Optional.of(wallet);
      return completeOrContinue();
    }

    private Behavior<Object> onTimeout(Timeout message) {
      return Behaviors.stopped();
    }

    private Behavior<Object> completeOrContinue() {
      if (keys.isPresent() && wallet.isPresent()) {
        replyTo.tell(new ReadyToLeaveHome(who, keys.get(), wallet.get()));
        return Behaviors.stopped();

      } else {
        return this;
      }
    }
  }
}
