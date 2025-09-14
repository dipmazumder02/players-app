package com.dip.players;

import com.dip.players.transport.InMemoryTransport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Player + InMemoryTransport integration.
 */
public class PlayerTest {

    private InMemoryTransport transport;

    @AfterEach
    void tearDown() {
        if (transport != null) {
            transport.close();
        }
    }

    @Test
    void testInProcessConversationCompletes() throws Exception {
        transport = new InMemoryTransport();
        Player initiator = new Player("A", transport);
        Player responder = new Player("B", transport);

        initiator.start();
        responder.start();

        CountDownLatch latch = new CountDownLatch(10);
        initiator.startConversation("B", "hello", 10, latch);

        // Wait for the conversation to complete
        boolean finished = latch.await(3, TimeUnit.SECONDS);

        assertTrue(finished, "Initiator should receive 10 replies");
    }

    @Test
    void testResponderStopsOnBye() throws Exception {
        transport = new InMemoryTransport();
        Player initiator = new Player("A", transport);
        Player responder = new Player("B", transport);

        initiator.start();
        responder.start();

        // Send BYE directly
        transport.send(new Message("A", "B", "BYE"));

        // Give responder a moment to process
        Thread.sleep(200);

        // After BYE, responder should be unregistered
        assertThrows(IllegalStateException.class,
                () -> transport.send(new Message("A", "B", "test")),
                "Responder should be stopped and unregistered after BYE");
    }
}
