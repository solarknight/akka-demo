package com.github.solarknight.akka.sample.basic;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.github.solarknight.akka.sample.basic.MasterControlProgram.Command;

/**
 * @author peiheng.zph created on Jan 27, 2020
 * @version 1.0
 */
public class MasterControlProgram extends AbstractBehavior<Command> {

  interface Command {}

  public static final class SpawnJob implements Command {
    public final String name;
    public final ActorRef<JobCreated> replyTo;
    public final ActorRef<JobDone> replyToWhenDone;

    public SpawnJob(String name, ActorRef<JobCreated> replyTo, ActorRef<JobDone> replyToWhenDone) {
      this.name = name;
      this.replyTo = replyTo;
      this.replyToWhenDone = replyToWhenDone;
    }
  }

  public static final class JobCreated {
    public final String name;
    public final ActorRef<Job.Command> job;

    public JobCreated(String name, ActorRef<Job.Command> job) {
      this.name = name;
      this.job = job;
    }
  }

  public static final class JobDone {
    public final String name;

    public JobDone(String name) {
      this.name = name;
    }
  }

  public static final class JobTerminated implements Command {
    final String name;
    final ActorRef<JobDone> replyToWhenDone;

    public JobTerminated(String name, ActorRef<JobDone> replyToWhenDone) {
      this.name = name;
      this.replyToWhenDone = replyToWhenDone;
    }
  }

  public enum GracefulShutdown implements Command {
    INSTANCE
  }

  public static Behavior<Command> create() {
    return Behaviors.setup(MasterControlProgram::new);
  }

  public MasterControlProgram(ActorContext<Command> context) {
    super(context);
  }

  @Override
  public Receive<Command> createReceive() {
    return newReceiveBuilder()
        .onMessage(SpawnJob.class, this::onSpawnJob)
        .onMessage(JobTerminated.class, this::onJobTerminated)
        .onMessage(GracefulShutdown.class, message -> onGracefulShutdown())
        .onSignal(PostStop.class, signal -> onPostStop())
        .build();
  }

  private Behavior<Command> onSpawnJob(SpawnJob message) {
    getContext().getLog().info("Spawning job {}!", message.name);

    ActorRef<Job.Command> job = getContext().spawn(Job.create(message.name), message.name);
    message.replyTo.tell(new JobCreated(message.name, job));

    getContext().watchWith(job, new JobTerminated(message.name, message.replyToWhenDone));
    return this;
  }

  private Behavior<Command> onJobTerminated(JobTerminated terminated) {
    getContext().getLog().info("Job stopped: {}", terminated.name);
    terminated.replyToWhenDone.tell(new JobDone(terminated.name));
    return this;
  }

  private Behavior<Command> onGracefulShutdown() {
    getContext().getLog().info("Initiating graceful shutdown...");
    return Behaviors.stopped(() -> getContext().getLog().info("Cleanup!"));
  }

  private Behavior<Command> onPostStop() {
    getContext().getLog().info("Master Control Program stopped");
    return this;
  }

  static class Job extends AbstractBehavior<Job.Command> {

    interface Command {}

    public enum GracefulShutdown implements Command {
      INSTANCE
    }

    public static Behavior<Command> create(String name) {
      return Behaviors.setup(context -> new Job(context, name));
    }

    private final String name;

    public Job(ActorContext<Command> context, String name) {
      super(context);
      this.name = name;
    }

    @Override
    public Receive<Command> createReceive() {
      return newReceiveBuilder()
          .onMessage(GracefulShutdown.class, message -> Behaviors.stopped())
          .onSignal(PostStop.class, signal -> onPostStop())
          .build();
    }

    private Behavior<Command> onPostStop() {
      getContext().getLog().info("Worker {} stopped", name);
      return Behaviors.stopped();
    }
  }
}
