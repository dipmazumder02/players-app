package com.dip.players;

/**
 * Responsibility: callback for delivering inbound messages to a player.
 */
@FunctionalInterface
public interface PlayerListener {
    void onMessage(Message message);
}
