package com.dip.players.transport;

import com.dip.players.Message;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for InMemoryTransport mailboxes.
 */
public class InMemoryTransportTest {

    @Test
    void testMessageDeliveredToListener() throws Exception {
        try (InMemoryTransport transport = new InMemoryTransport()) {
            CountDownLatch latch = new CountDownLatch(1);

            transport.register("A", msg -> {
                assertEquals("hello", msg.payload());
                latch.countDown();
            });

            transport.send(new Message("B", "A", "hello"));

            assertTrue(latch.await(1, TimeUnit.SECONDS), "Message should be delivered");
        }
    }

    @Test
    void testUnregisterStopsDelivery() throws Exception {
        try (InMemoryTransport transport = new InMemoryTransport()) {
            CountDownLatch latch = new CountDownLatch(1);

            transport.register("A", msg -> latch.countDown());
            transport.unregister("A");

            assertThrows(IllegalStateException.class,
                    () -> transport.send(new Message("B", "A", "test")),
                    "Send to unregistered player should fail");
        }
    }
}
