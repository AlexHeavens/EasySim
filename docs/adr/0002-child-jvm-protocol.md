# 2. Split a node into a simulator side and a child-JVM side

Date: 2026-11-22. Status: proposed. Builds on [ADR 1](0001-shared-child-jvm-with-continuation-scheduler.md).

## Context

Today `Node` holds both the simulation state (events, links, latency, the barrier) and the thread that runs student code. Student code calls `send`, `sendAll`, `receive`, `pause`, `setColour`, `neighbourCount`, `machineId` and `isInitiator` on it, and blocks on in-process locks and conditions. A child JVM cannot share those locks, so the blocking calls must become messages.

## Decision

**Simulator side (existing process).** `Node` keeps state, events, links, latency, the barrier and the profiler. It stops owning a thread. A new `ChildJvm` class starts the child process, owns the socket, and routes messages to nodes.

**Child side (new `NodeHost` main class).** `NodeHost` loads the student classes, runs one continuation per node on `ContinuationScheduler`, and gives each `NodeScript` a stub that implements the same API by sending requests.

**One connection for all nodes.** A single socket carries frames tagged with a node id. Each request from a node carries a request id. The node parks until the reply with that id arrives, and a reader thread unparks it.

| Request (child to simulator) | Reply (simulator to child) |
|---|---|
| `SEND(node, neighbour, message)` and `SEND_ALL` | accepted |
| `RECEIVE(node, neighbour?)` | the message, sent when it arrives |
| `PAUSE(node, steps)` | resumed, sent when the lock-step barrier releases the node |
| `SET_COLOUR(node, colour)` | accepted |
| `NEIGHBOURS(node)` | neighbour count and ids; the stub caches them |

| Command (simulator to child) | Effect |
|---|---|
| `START(node, class, script data)` | create and start the node's continuation |
| `KILL(node)` | `Task.kill()`; the child reports `ENDED(node)` when it stops |

| Event (child to simulator) | Meaning |
|---|---|
| `ENDED(node, failure?)` | the body returned, was killed, or threw |

The barrier stays on the simulator side. Lock-step needs no barrier in the child, because the simulator only replies to `RECEIVE` and `PAUSE` when the step allows it.

**Kill ladder.** A kill first runs `KILL(node)`. If `ENDED` does not arrive within the grace period (`Node.killGrace()`), the simulator kills the child process, marks the run failed, and names the node. A blocked node is already handled by the profiler's blocked-state timeout, which now sends `KILL`.

## Build order

Each step is its own PR with tests. Nothing changes behaviour until the last step.

1. Wire format and codec for the frames above, with round-trip tests.
2. `NodeHost` with the stub API, tested against a fake simulator in the same JVM.
3. `ChildJvm` on the simulator side, tested with a real child process, including process kill.
4. Move `Node` onto `ChildJvm` behind a flag, and run the existing simulation tests in both modes.
5. Make the child JVM the default and delete the in-process thread path.

## Open questions

- **Message payloads.** Unverified: which `IMessage` content types students can send. `StringMessage` and `NumericMessage` are plain values. A student-defined message class would need serialisation, and the child must load that class on both sides. Proposal: allow only plain values at first.
- **Determinism.** Replaying a run with one node pre-failed (deferred in ADR 1) needs identical node behaviour. Students can use `Random` and timing. Not decided.
- **Instrumentation of student classes.** Unverified: how the Tascalate agent instruments classes that `ClassFileManager` compiles in memory. Step 2 must prove this before step 4.
- **Child JVM flags.** Memory cap and agent path are not chosen.
