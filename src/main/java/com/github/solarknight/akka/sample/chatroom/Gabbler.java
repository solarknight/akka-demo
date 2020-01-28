package com.github.solarknight.akka.sample.chatroom;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import com.github.solarknight.akka.sample.chatroom.Protocol.MessagePosted;
import com.github.solarknight.akka.sample.chatroom.Protocol.PostMessage;
import com.github.solarknight.akka.sample.chatroom.Protocol.SessionDenied;
import com.github.solarknight.akka.sample.chatroom.Protocol.SessionEvent;
import com.github.solarknight.akka.sample.chatroom.Protocol.SessionGranted;

/**
 * @author peiheng.zph created on Dec 27, 2019
 * @version 1.0
 */
public class Gabbler {

  public static Behavior<SessionEvent> create() {
    return Behaviors.setup(ctx -> new Gabbler(ctx).behavior());
  }

  private final ActorContext<SessionEvent> context;

  private Gabbler(ActorContext<SessionEvent> context) {
    this.context = context;
  }

  private Behavior<SessionEvent> behavior() {
    return Behaviors.receive(SessionEvent.class)
        .onMessage(SessionDenied.class, this::onSessionDenied)
        .onMessage(SessionGranted.class, this::onSessionGranted)
        .onMessage(MessagePosted.class, this::onMessagePosted)
        .build();
  }

  private Behavior<SessionEvent> onSessionDenied(SessionDenied message) {
    context.getLog().info("cannot start chat room session: {}", message.reason);
    return Behaviors.stopped();
  }

  private Behavior<SessionEvent> onSessionGranted(SessionGranted message) {
    message.handle.tell(new PostMessage("Hello World!"));
    return Behaviors.same();
  }

  private Behavior<SessionEvent> onMessagePosted(MessagePosted message) {
    context
        .getLog()
        .info("message has been posted by '{}': {}", message.screenName, message.message);
    return Behaviors.stopped();
  }
}
