package com.github.solarknight.akka.sample.basic;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.MailboxSelector;
import akka.actor.typed.javadsl.Behaviors;
import com.typesafe.config.ConfigFactory;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * @author peiheng.zph created on Jan 31, 2020
 * @version 1.0
 */
public class ActorMailboxDemoTest {

  @ClassRule
  public static final TestKitJunitResource testKit = new TestKitJunitResource(ConfigFactory.load());

  @Test
  public void test() {
    Behavior<Object> root = Behaviors.setup(context -> Behaviors.ignore());
    ActorRef<Object> actorRef =
        testKit.spawn(root, "root", MailboxSelector.fromConfig("my-app.my-special-mailbox"));

    actorRef.tell(new Object());
  }
}
