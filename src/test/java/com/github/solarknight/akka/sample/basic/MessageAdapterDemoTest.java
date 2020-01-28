package com.github.solarknight.akka.sample.basic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import com.github.solarknight.akka.sample.basic.MessageAdapterDemo.Backend;
import com.github.solarknight.akka.sample.basic.MessageAdapterDemo.Backend.JobCompleted;
import com.github.solarknight.akka.sample.basic.MessageAdapterDemo.Backend.JobProgress;
import com.github.solarknight.akka.sample.basic.MessageAdapterDemo.Backend.JobStarted;
import com.github.solarknight.akka.sample.basic.MessageAdapterDemo.Backend.StartTranslationJob;
import com.github.solarknight.akka.sample.basic.MessageAdapterDemo.Frontend;
import com.github.solarknight.akka.sample.basic.MessageAdapterDemo.Frontend.Translate;
import com.github.solarknight.akka.sample.basic.MessageAdapterDemo.Translator;
import java.net.URI;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * @author peiheng.zph created on Jan 28, 2020
 * @version 1.0
 */
public class MessageAdapterDemoTest {

  @ClassRule public static final TestKitJunitResource testKit = new TestKitJunitResource();

  @Test
  public void testMessageAdapter() {
    TestProbe<Backend.Request> backendProbe = testKit.createTestProbe(Backend.Request.class);
    TestProbe<URI> uriProbe = testKit.createTestProbe(URI.class);

    ActorRef<Frontend.Command> translator =
        testKit.spawn(Translator.create(backendProbe.getRef()), "translator");

    URI site = URI.create("http://test.xyz");
    translator.tell(new Translate(site, uriProbe.getRef()));

    Backend.Request request = backendProbe.receiveMessage();
    assertTrue(request instanceof Backend.StartTranslationJob);
    assertEquals(1, ((StartTranslationJob) request).taskId);
    assertEquals(site, ((StartTranslationJob) request).site);
    assertNotNull(((StartTranslationJob) request).replyTo);

    ActorRef<Backend.Response> responseActorRef = ((StartTranslationJob) request).replyTo;
    responseActorRef.tell(new JobStarted(1));
    responseActorRef.tell(new JobProgress(1, 20.0D));
    responseActorRef.tell(new JobProgress(1, 100.0D));

    URI site2 = URI.create("http://test2.xyz");
    responseActorRef.tell(new JobCompleted(1, site2));
    assertEquals(site2, uriProbe.receiveMessage());
  }
}
