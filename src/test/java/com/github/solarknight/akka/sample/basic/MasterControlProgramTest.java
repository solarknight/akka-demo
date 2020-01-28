package com.github.solarknight.akka.sample.basic;

import static org.junit.Assert.assertEquals;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import com.github.solarknight.akka.sample.basic.MasterControlProgram.Command;
import com.github.solarknight.akka.sample.basic.MasterControlProgram.JobCreated;
import com.github.solarknight.akka.sample.basic.MasterControlProgram.JobDone;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * @author peiheng.zph created on Jan 27, 2020
 * @version 1.0
 */
public class MasterControlProgramTest {

  @ClassRule public static final TestKitJunitResource testKit = new TestKitJunitResource();

  @Test
  public void testStop() {
    ActorRef<Command> system = testKit.spawn(MasterControlProgram.create(), "system");
    TestProbe<JobCreated> probe = testKit.createTestProbe(JobCreated.class);
    TestProbe<JobDone> probe2 = testKit.createTestProbe(JobDone.class);

    system.tell(new MasterControlProgram.SpawnJob("a", probe.getRef(), probe2.getRef()));

    JobCreated jobCreated = probe.receiveMessage();
    assertEquals(jobCreated.name, "a");

    jobCreated.job.tell(MasterControlProgram.Job.GracefulShutdown.INSTANCE);
    JobDone jobDone = probe2.receiveMessage();
    assertEquals(jobDone.name, "a");

    system.tell(MasterControlProgram.GracefulShutdown.INSTANCE);
  }
}
