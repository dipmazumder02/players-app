package com.dip.players.transport;

import com.dip.players.Message;
import com.dip.players.PlayerListener;

import java.util.Objects;
import java.util.concurrent.*;

/**
 * InMemoryTransport
 * Responsibility: route messages inside the same JVM using a per-player mailbox (single-thread executor).
 * Guarantees: FIFO delivery per player; non-blocking send with bounded queue + backpressure (CallerRunsPolicy).
 */
public class InMemoryTransport implements Transport {

    private final ConcurrentMap<String, PlayerListener> listeners = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Mailbox> mailboxes = new ConcurrentHashMap<>();

    /** Single-threaded, bounded mailbox for one player (not AutoCloseable by design). */
    private static final class Mailbox {
        private final ThreadPoolExecutor executor;

        Mailbox(String id) {
            BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(1024);
            ThreadFactory tf = r -> {
                Thread t = new Thread(r, "inmem-mailbox-" + id);
                t.setDaemon(true);
                return t;
            };
            this.executor = new ThreadPoolExecutor(
                    1, 1,
                    60L, TimeUnit.SECONDS,
                    queue,
                    tf,
                    new ThreadPoolExecutor.CallerRunsPolicy()
            );
        }

        void submit(Runnable task) { executor.submit(task); }

        void shutdown() {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                executor.shutdownNow();
            }
        }
    }

    @Override
    public void register(String playerId, PlayerListener listener) {
        Objects.requireNonNull(playerId);
        Objects.requireNonNull(listener);
        listeners.put(playerId, listener);
        mailboxes.computeIfAbsent(playerId, Mailbox::new);
    }

    @Override
    public void unregister(String playerId) {
        listeners.remove(playerId);
        Mailbox mb = mailboxes.remove(playerId);
        if (mb != null) mb.shutdown();
    }

    @Override
    public void send(Message message) {
        PlayerListener target = listeners.get(message.to());
        if (target == null) throw new IllegalStateException("No listener for player: " + message.to());
        Mailbox mailbox = mailboxes.get(message.to());
        if (mailbox == null) throw new IllegalStateException("No mailbox for player: " + message.to());
        mailbox.submit(() -> target.onMessage(message));
    }

    @Override
    public void close() {
        for (Mailbox mb : mailboxes.values()) mb.shutdown();
        mailboxes.clear();
        listeners.clear();
    }
}
