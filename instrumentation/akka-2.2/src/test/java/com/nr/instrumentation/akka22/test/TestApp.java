/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.akka22.test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.routing.RoundRobinPool;
import com.newrelic.api.agent.Trace;
import com.nr.instrumentation.akka22.test.actors.broadcasting.ActorA;
import com.nr.instrumentation.akka22.test.actors.broadcasting.ActorB;
import com.nr.instrumentation.akka22.test.actors.broadcasting.ActorC;
import com.nr.instrumentation.akka22.test.actors.broadcasting.branches.ActorNoTxnBranch;
import com.nr.instrumentation.akka22.test.actors.broadcasting.branches.ParentActor;
import com.nr.instrumentation.akka22.test.actors.forwarding.InitActor;
import com.nr.instrumentation.akka22.test.actors.routing.Routee;
import com.nr.instrumentation.akka22.test.actors.routing.RoutingActor;

public class TestApp {

    @Trace(dispatcher = true)
    public static void sendMessageInTransaction(ActorSystem system) throws InterruptedException {
        ActorRef testActor = system.actorOf(Props.create(InitActor.class));

        Thread.sleep(2000); // Let system initialize
        testActor.tell("hi", null);
        Thread.sleep(2000); // Let message processing finish
    }


    @Trace(dispatcher = true)
    public static void broadcastInTransaction(ActorSystem system) throws InterruptedException {
        ActorRef actorA = system.actorOf(Props.create(ActorA.class));
        ActorRef actorB = system.actorOf(Props.create(ActorB.class));
        ActorRef actorC = system.actorOf(Props.create(ActorC.class));

        Thread.sleep(1000); // Let system initialize
        system.actorSelection("/user/*").tell("message", null);
        Thread.sleep(2000); // Let all actors process messages
    }

    public static void broadcastStress(ActorSystem system) throws InterruptedException {
        ActorRef actorStartTxn = system.actorOf(Props.create(ParentActor.class));
        ActorRef actorNoTxnBranch = system.actorOf(Props.create(ActorNoTxnBranch.class));

        Thread.sleep(1000); // Let system initialize
        system.actorSelection("/user/*").tell("IMPORTANT_MESSSAGE", null);
        Thread.sleep(2000); // Let all actors process messages
    }

    @Trace(dispatcher = true)
    public static void sendRoutedMessageInTransaction(ActorSystem system) throws InterruptedException {
        ActorRef router = system.actorOf(new RoundRobinPool(5).props(Props.create(RoutingActor.class)));
        Thread.sleep(2000); // Let system initialize
        router.tell("routing", null);
        Thread.sleep(2000); // Let message processing finish
    }

    @Trace(dispatcher = true)
    public static void sendRoutedMessageToRouteeInTransaction(ActorSystem system) throws InterruptedException {
        final ActorRef routee = system.actorOf(Props.create(Routee.class));

        ActorRef router = system.actorOf(new RoundRobinPool(5).props(Props.create(RoutingActor.class, routee)));

        Thread.sleep(2000); // Let system initialize
        router.tell("routing", null);
        Thread.sleep(2000); // Let message processing finish
    }
}
