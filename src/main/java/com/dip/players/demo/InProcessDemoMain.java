package com.dip.players.demo;

import com.dip.players.Player;
import com.dip.players.transport.InMemoryTransport;

import java.util.concurrent.CountDownLatch;

/**
 * InProcessDemoMain
 * Responsibility: demo entrypoint running both players in a single JVM using the InMemoryTransport.
 */
public class InProcessDemoMain {
    public static void main(String[] args) throws Exception {
        try (var transport = new InMemoryTransport()) {
            Player initiator = new Player("A", transport);
            Player responder = new Player("B", transport);

            initiator.start();
            responder.start();

            CountDownLatch latch = new CountDownLatch(10); // expect 10 replies
            initiator.startConversation("B", "hello", 10, latch);

            // Wait for all 10 replies; BYE is sent by initiator on completion.
            latch.await();

            // No arbitrary sleep needed; transport.close() will flush mailbox tasks.
            System.out.println("Both players completed in-process demo. Shutting down...");
        }
    }
}
