package com.dip.players;

import java.util.Objects;

/**
 * Immutable value object carrying the message between players.
 * Responsibility: represent the sender, receiver and textual payload.
 */
public final class Message {
    private final String from;
    private final String to;
    private final String payload;

    public Message(String from, String to, String payload) {
        this.from = Objects.requireNonNull(from);
        this.to = Objects.requireNonNull(to);
        this.payload = Objects.requireNonNull(payload);
    }

    public String from() { return from; }
    public String to() { return to; }
    public String payload() { return payload; }

    @Override
    public String toString() {
        return "Message{from='%s', to='%s', payload='%s'}".formatted(from, to, payload);
    }
}
