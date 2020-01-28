package com.github.solarknight.akka.sample.chatroom;

import akka.actor.typed.ActorRef;

/**
 * @author peiheng.zph created on Dec 25, 2019
 * @version 1.0
 */
public final class Protocol {

  interface RoomCommand {}

  public static final class GetSession implements RoomCommand {
    public final String screenName;
    public final ActorRef<SessionEvent> replyTo;

    public GetSession(String screenName, ActorRef<SessionEvent> replyTo) {
      this.screenName = screenName;
      this.replyTo = replyTo;
    }
  }

  interface SessionEvent {}

  public static final class SessionGranted implements SessionEvent {
    public final ActorRef<PostMessage> handle;

    public SessionGranted(ActorRef<PostMessage> handle) {
      this.handle = handle;
    }
  }

  public static final class SessionDenied implements SessionEvent {
    public final String reason;

    public SessionDenied(String reason) {
      this.reason = reason;
    }
  }

  public static final class MessagePosted implements SessionEvent {
    public final String screenName;
    public final String message;

    public MessagePosted(String screenName, String message) {
      this.screenName = screenName;
      this.message = message;
    }
  }

  interface SessionCommand {}

  public static final class PostMessage implements SessionCommand {
    public final String message;

    public PostMessage(String message) {
      this.message = message;
    }
  }
}
