package com.dip.players.transport;

import com.dip.players.Message;
import com.dip.players.PlayerListener;

/**
 * Transport strategy abstraction.
 * Responsibility: deliver messages between players and manage listener registration,
 * decoupling Player logic from delivery mechanics (in-memory, TCP, etc.).
 */
public interface Transport extends AutoCloseable {
    void register(String playerId, PlayerListener listener);
    void unregister(String playerId);
    void send(Message message);
    @Override default void close() {}
}
