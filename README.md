<img width="3612" height="3840" alt="image" src="https://github.com/user-attachments/assets/dbf76475-4d1b-4f81-9dd0-e77e9dd6a19d" /># Players App — In‑Process & Multi‑Process Messaging

This project implements two communicating **Player** instances:

- **In‑process mode (same JVM):** message routing via an in‑memory broker.
- **Multi‑process mode (different PIDs):** message exchange over TCP (loopback).

## Requirements mapping

- 2 players created; one is the **initiator**.
- On receiving a message, a player replies with the original payload **concatenated** with **its own send counter**.
- Program stops after the initiator completes **10 round‑trips** (10 sends + 10 replies) and performs a graceful shutdown.
- Pure Java, no frameworks. Maven project with source only. Shell scripts included.

## How to run

### In‑process (same Java process)

```bash
./run_inprocess.sh
```

Expected logs show 10 exchanges and shutdown.

### Multi‑process (different PIDs, TCP)

```bash
./run_tcp_two_processes.sh
```

This script starts the responder on port 5000 and then runs the initiator against it.

---

## Design overview

- `Player`: encapsulates identity, send counter, and behavior for receiving & replying.
- `Message`: immutable value describing sender, receiver, and payload.
- `Transport` (strategy): abstraction for message delivery; promotes clean separation.
  - `InMemoryTransport`: routes messages between players in the same JVM using per‑player mailboxes and an executor to avoid recursion and ensure ordering.
- `InProcessDemoMain`: boots two players in one JVM, wires them through the in‑memory transport, runs the 10‑round protocol, then shuts down.
- TCP mode (separate PIDs): minimal line‑based protocol over sockets.
  - `TcpInitiatorMain`: connects, performs 10 round‑trips, sends `BYE`, awaits `BYE-ACK`, and exits.
  - `TcpResponderMain`: accepts a single client, replies to each line with the payload + counter, on `BYE` returns `BYE-ACK` and exits.

Each class has Javadoc documenting its responsibilities.

## Notes

- Message counters are **per player** and increment **on send**. When a player replies, it increments its counter and appends it to the reply (e.g., `hello #1`, `hello #2`, ...).
- Shutdown is explicit: in‑process via a latch & broker shutdown; TCP via a `BYE`/`BYE-ACK` handshake.
---
## Simulation Steps
### In-process (same JVM, `InMemoryTransport`)

**Bootstrapping**

* `A` (initiator) and `B` (responder) call `start()`, which registers their listeners.
* `A` creates `CountDownLatch(10)` and calls `startConversation("B","hello",10,latch)`.

  <img width="3020" height="3840" alt="image" src="https://github.com/user-attachments/assets/e8e4740a-cecf-40bb-be74-6b6f879194e8" />

**Delivery model (updated)**

* `InMemoryTransport` uses a **single-threaded mailbox per player** (strict FIFO), with a **bounded queue** and **CallerRuns** backpressure. No global cached pool.

**Round-trip loop (10 times)**

1. `A` increments its own counter and sends `"hello #<A_n>"`.
2. Transport enqueues to **B’s mailbox** → B’s listener runs on its single thread.
3. `B` increments its own counter and replies with `"<received> #<B_m>"`.
4. Transport enqueues to **A’s mailbox** → A receives and `latch.countDown()`.
5. If latch isn’t zero, `A` sends the next message using the **latest payload** (the text grows).

**Termination**

* When latch hits **0**, `A` sends `"BYE"`.
* `B` sees `"BYE"`, logs, and `stop()`s (**no BYE-ACK in in-process mode**).
* `A` then `stop()`s. Closing the transport **drains mailboxes**; no sleeps or thread leaks.

---

### Multi-process (different PIDs, TCP line protocol)

**Connection**

* `TcpResponderMain` listens (single client).
* `TcpInitiatorMain` connects on loopback using blocking I/O with **newline-framed** messages (`readLine` / `write("\n")`).

  
<img width="3612" height="3840" alt="image" src="https://github.com/user-attachments/assets/c313f20c-a4a7-4d55-a97a-72d29995ad91" />

**Round-trip loop (10 times)**

1. Initiator increments its own counter and writes `"hello #<A_n>\n"`.
2. Responder reads a line, increments its own counter, and writes back `"<received> #<B_m>\n"`.
3. Initiator reads the reply and continues (payload **grows** each hop, mirroring in-process behavior).

**Graceful shutdown handshake**

* Initiator writes `"BYE\n"`.
* Responder replies `"BYE-ACK\n"` and exits; initiator reads the ACK and exits.


