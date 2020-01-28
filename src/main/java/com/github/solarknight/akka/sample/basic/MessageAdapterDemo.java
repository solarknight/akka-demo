package com.github.solarknight.akka.sample.basic;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.github.solarknight.akka.sample.basic.MessageAdapterDemo.Backend.Response;
import com.github.solarknight.akka.sample.basic.MessageAdapterDemo.Frontend.Command;
import com.github.solarknight.akka.sample.basic.MessageAdapterDemo.Frontend.Translate;
import com.github.solarknight.akka.sample.basic.MessageAdapterDemo.Frontend.WrappedBackendResponse;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Adapted-response pattern<br>
 *
 * @see <a
 *     href="https://doc.akka.io/docs/akka/current/typed/interaction-patterns.html#adapted-response">interaction-patterns</a>
 * @author peiheng.zph created on Jan 28, 2020
 * @version 1.0
 */
public class MessageAdapterDemo {

  public static class Backend {
    public interface Request {}

    public static class StartTranslationJob implements Request {
      public final int taskId;
      public final URI site;
      public final ActorRef<Response> replyTo;

      public StartTranslationJob(int taskId, URI site, ActorRef<Response> replyTo) {
        this.taskId = taskId;
        this.site = site;
        this.replyTo = replyTo;
      }
    }

    public interface Response {}

    public static class JobStarted implements Response {
      public final int taskId;

      public JobStarted(int taskId) {
        this.taskId = taskId;
      }
    }

    public static class JobProgress implements Response {
      public final int taskId;
      public final double progress;

      public JobProgress(int taskId, double progress) {
        this.taskId = taskId;
        this.progress = progress;
      }
    }

    public static class JobCompleted implements Response {
      public final int taskId;
      public final URI result;

      public JobCompleted(int taskId, URI result) {
        this.taskId = taskId;
        this.result = result;
      }
    }
  }

  public static class Frontend {
    public interface Command {}

    public static class Translate implements Command {
      public final URI site;
      public final ActorRef<URI> replyTo;

      public Translate(URI site, ActorRef<URI> replyTo) {
        this.site = site;
        this.replyTo = replyTo;
      }
    }

    static class WrappedBackendResponse implements Command {
      final Backend.Response response;

      public WrappedBackendResponse(Response response) {
        this.response = response;
      }
    }
  }

  public static class Translator extends AbstractBehavior<Command> {
    private final ActorRef<Backend.Request> backend;
    private final ActorRef<Backend.Response> backendResponseAdapter;

    private int taskIdCounter = 0;
    private Map<Integer, ActorRef<URI>> inProgress = new HashMap<>();

    public static Behavior<Command> create(ActorRef<Backend.Request> backend) {
      return Behaviors.setup(context -> new Translator(context, backend));
    }

    public Translator(ActorContext<Command> context, ActorRef<Backend.Request> backend) {
      super(context);
      this.backend = backend;
      this.backendResponseAdapter =
          context.messageAdapter(Backend.Response.class, WrappedBackendResponse::new);
    }

    @Override
    public Receive<Command> createReceive() {
      return newReceiveBuilder()
          .onMessage(Translate.class, this::onTranslate)
          .onMessage(WrappedBackendResponse.class, this::onWrappedBackendResponse)
          .build();
    }

    private Behavior<Command> onTranslate(Translate cmd) {
      taskIdCounter += 1;
      inProgress.put(taskIdCounter, cmd.replyTo);
      backend.tell(
          new Backend.StartTranslationJob(taskIdCounter, cmd.site, backendResponseAdapter));
      return this;
    }

    private Behavior<Command> onWrappedBackendResponse(WrappedBackendResponse wrapped) {
      Backend.Response response = wrapped.response;

      if (response instanceof Backend.JobStarted) {
        Backend.JobStarted rsp = (Backend.JobStarted) response;
        getContext().getLog().info("Started {}", rsp.taskId);

      } else if (response instanceof Backend.JobProgress) {
        Backend.JobProgress rsp = (Backend.JobProgress) response;
        getContext().getLog().info("Progress {}", rsp.taskId);

      } else if (response instanceof Backend.JobCompleted) {
        Backend.JobCompleted rsp = (Backend.JobCompleted) response;
        getContext().getLog().info("Completed {}", rsp.taskId);
        inProgress.get(rsp.taskId).tell(rsp.result);
        inProgress.remove(rsp.taskId);

      } else {
        return Behaviors.unhandled();
      }

      return this;
    }
  }
}
