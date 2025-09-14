package com.dip.players;

import com.dip.players.transport.Transport;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Player
 * Responsibility: encapsulates a communicating actor with an identity, a send counter,
 * and receive/reply behavior. It is transport-agnostic and can run both in-process and across processes.
 */
public class Player {

    private final String id;
    private final Transport transport;
    private final AtomicInteger sentCounter = new AtomicInteger(0);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile String peerId;

    private CountDownLatch completion; // non-null only for initiator

    public Player(String id, Transport transport) {
        this.id = Objects.requireNonNull(id);
        this.transport = Objects.requireNonNull(transport);
    }

    public String id() { return id; }

    /** Starts the player by registering its inbound listener on the transport. */
    public void start() {
        if (running.compareAndSet(false, true)) {
            transport.register(id, this::onMessage);
        }
    }

    /** Stops the player by unregistering and marking as not running. */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            transport.unregister(id);
        }
    }

    /**
     * Initiate a N-round trip conversation with the given peer.
     * @param rounds Number of replies expected back from the peer (e.g., 10)
     */
    public void startConversation(String peerId, String initialPayload, int rounds, CountDownLatch completion) {
        this.peerId = Objects.requireNonNull(peerId);
        this.completion = Objects.requireNonNull(completion);
        log("Initiating conversation with %s for %d rounds".formatted(peerId, rounds));
        sendToPeer(initialPayload);
    }

    private void onMessage(Message msg) {
        if (!running.get()) return;
        log("Received: \"%s\" from %s".formatted(msg.payload(), msg.from()));

        // If we are the responder (no completion latch => not initiator), reply to every message
        if (completion == null) {
            if ("BYE".equals(msg.payload())) {
                log("Received BYE. Shutting down responder.");
                stop();
                return;
            }
            int c = sentCounter.incrementAndGet();
            String reply = msg.payload() + " #" + c;
            transport.send(new Message(id, msg.from(), reply));
            log("Replied: \"%s\" to %s".formatted(reply, msg.from()));
            return;
        }

        // We are the initiator: count replies and continue until rounds reached
        long remaining = completion.getCount();
        if (remaining > 0) {
            completion.countDown();
            if (completion.getCount() == 0) {
                log("Completed all replies. Sending BYE and stopping.");
                transport.send(new Message(id, msg.from(), "BYE"));
                stop();
            } else {
                // Send next message using the latest payload content for visibility
                sendToPeer(msg.payload());
            }
        }
    }

    private void sendToPeer(String payload) {
        int c = sentCounter.incrementAndGet();
        String messagePayload = payload + " #" + c;
        transport.send(new Message(id, peerId, messagePayload));
        log("Sent: \"%s\" to %s".formatted(messagePayload, peerId));
    }

    private void log(String s) {
        System.out.println("[%s] %s".formatted(id, s));
    }
}
