# 2. Split a node into a simulator side and a child-JVM side

Date: 2026-11-22. Status: accepted (Alex, 2026-11-22). Builds on [ADR 1](0001-shared-child-jvm-with-continuation-scheduler.md).

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

## Decisions (Alex, 2026-11-22)

- **Message payloads are primitives only:** a length-limited string, a number, or an enum. The codec rejects anything else, and the string limit is a named constant. Student-defined message classes are not supported.
- **Replay of a failed run is approved**, with the failed node pre-failed. It is built only once node behaviour is deterministic, meaning the cause of non-determinism (`Random`, timing) is removed or seeded. Until then a failed run is reported, not replayed.
- **Instrumentation comes next.** Once this ADR merges, prove that the Tascalate agent instruments student classes compiled by `ClassFileManager`, before step 2 builds on it.

## Findings on the first two open questions (2026-11-23)

### Yielding inside `synchronized`: broken silently, so the build rejects it

`SynchronizedYieldTest` runs two nodes on one lock. Node A parks, or ticks until released, inside `synchronized`. Node B then starts. With 1 and with 2 workers, each run ended with both nodes completed and no exception. Two nodes were inside the block at once (`maxInside=2`). The continuation does not keep the monitor when it suspends. There is no deadlock and no pinned worker. A critical section stops being atomic, and nothing reports it.

The decision is to make the limit explicit. `NodeInstrumenter` now throws `UnsupportedNodeCodeException` for a `synchronized` method or block in student code. The message names the method and tells the student to use messages. Nodes share nothing in this model, so the loss is small. A library that locks internally (`StringBuffer`, `System.out`) is unaffected, because library code never suspends. The test stays as a pin: if a Tascalate upgrade keeps the monitor, the test fails and the rejection can be reconsidered.

### Cost of instrumenting every method: 9x to 50x on CPU-bound code

The benchmark runs one workload on a 1-worker scheduler, once as compiled and once through `NodeInstrumenter` with the agent. Runs 1 to 3 of 4 are averaged, in milliseconds, so JIT warm-up is left out. One machine, JDK 27.0.1, so treat the figures as orders of magnitude.

| Workload | As compiled | Every method instrumented | Slowdown | After leaf skipping | Slowdown |
|---|---|---|---|---|---|
| `fib(38)`, recursion | 251 ms | 2181 ms | 8.7x | 2436 ms | 9.7x |
| 82M loop iterations over an array | 30 ms | 1486 ms | 49.5x | 1085 ms | 36.2x |
| 400M calls of a leaf helper | 440 ms | 12016 ms | 27.3x | 5427 ms | 12.3x |

Running the instrumented class on a plain thread, where the safe point returns at once, costs 2.5x (`fib`) and 4x (leaf calls) with the checks alone. The agent adds little to that. The rest of the slowdown appears only inside a continuation: each call of the safe point goes through `ContinuationScheduler.tick()` and its thread-local lookup. This split is measured. The reason for it is not.

The cheap fix is applied: a method with no call, no loop and no exception handler is left as compiled (`NodeInstrumenterTest.leavesMethodsThatCannotSuspendAlone`). It cannot suspend or run long. It halves the leaf-call case and changes nothing for recursion or loops, which dominate. Run-to-run spread reaches 40% (the loop case: 1014 to 1774 ms), so 9.7x against 8.7x for `fib` is noise, not a regression.

The remaining cost is not cheap to remove. Options, with the call left to Alex:
- Accept it. A student node that spends its time in loops runs 10x to 50x slower than plain Java, in return for fairness and kill.
- Check a counter in the student class and call `tick()` only every N loop iterations. This needs a new field in student classes and a decision on how often a kill must take effect.
- Instrument loops only, and not method entry. Recursion without a loop then gets no safe point, so a runaway recursion is no longer killable. This conflicts with ADR 1.

## Determinism for replay: design only

Replay of a failed run needs the same inputs to give the same event order. This section lists the sources found in the code, and a design. Nothing here is built.

### Sources found in the code

| # | Source | Where | Remove or seed | Cost |
|---|---|---|---|---|
| 1 | Unseeded `new Random()` picks the initiator and the id scramble | `Network` constructor | Seed from one run seed that the run record stores | Trivial |
| 2 | `links` and `neighbourIds` are `HashMap<INode, ...>`. `Node` has no `hashCode`, so the iteration order follows the identity hash. `sendAll` and the neighbour iterator follow it. | `Node` fields, `sendAll` | Use `LinkedHashMap`, so the order is the link creation order | Trivial. Link creation order comes from `NetworkConfig.links`, a `HashSet` of `LinkConfig`. `LinkConfig` defines `hashCode`, so that order is stable for equal input. |
| 3 | Events at the same timestep. `SimulationEvent.compareTo` compares the timestep only, so ties leave the order of the `PriorityBlockingQueue` unspecified. The insertion order comes from nodes that raise events on their own threads. | `EventController`, `SimulationEvent` | Add a tie-break key: timestep, then node id, then a per-node sequence number | Small code change. It defines an order that today is arbitrary, so recorded runs from before the change do not match. |
| 4 | Arrival order of messages at one node within a timestep. `MessageImpl.compareTo` compares `sentAt` only, so equal values tie. A blocked receiver also wakes when the first message is queued (`Node.queueMessage`), and then reads the top of the pile. Which other messages are already there depends on thread timing. | `MessageImpl`, `Node.queueMessage`, `Node.receive` | Tie-break by sender id and sequence number. Wake a blocked receiver only after the simulator has delivered every message of the timestep. | Medium. The second part changes when a receiver wakes, so it needs the simulator to hold the wake-up until the barrier. Not verified how much code that touches. |
| 5 | Thread scheduling. In-process, each node has a thread. In the child, the scheduler has one worker per core, and a node's slice ends after a fixed count of safe points. That count is not time, so the slice itself is deterministic. The order in which requests from different nodes reach the simulator is not. | `Node` threads, `ContinuationScheduler`, `ChildJvm` reader | Process the requests of one timestep sorted by node id, after the barrier | Throughput cost: the simulator waits for every node to reach the barrier. The lock-step barrier already exists for `pause`. |
| 6 | Time limits. The profiler kills a node on thread CPU time and on blocked time (`System.nanoTime`). The kill grace and the kill timer also use wall time. A node that is killed at a different point gives a different run. | `SimulationProfiler`, `ChildJvm` timers | Replace the CPU limit with a budget of safe points, which the scheduler already counts. Keep wall time as a last-resort kill, and write its occurrence to the run record as an input to replay. | Medium. Safe points do not meter time spent in library or native calls. |
| 7 | Student use of `Random`, `Math.random`, `UUID`, `ThreadLocalRandom`, `System.currentTimeMillis` and `System.nanoTime`. Nothing in `CodeValidator` or `NodeInstrumenter` stops them. | student code | Reject the calls in `NodeInstrumenter` (as for `synchronized`), and offer a seeded `random()` on the node API and a simulated clock | Students lose the plain calls. `Math.random` shares one generator between nodes, so even a fixed seed gives interleaving-dependent values. |
| 8 | Student `HashMap` or `HashSet` over objects without `hashCode`. The order follows the identity hash. | student code | Not removable by us. `String` and boxed keys are stable. | Document it. Unverified: whether a JVM flag can make identity hashes repeat. |

No example script and no test script uses sources 7 (grep of `examples/node_scripts` and `test/node_scripts`). This is a statement about the shipped scripts, not about what students write.

### Proposed design

1. **One run seed.** A run takes a `long` seed, stored in the run record. It feeds source 1, and a per-node generator with seed `(run seed, node id)` for source 7.
2. **Defined order, not recorded order.** Fix sources 2 to 5 by giving every ordering a key (timestep, node id, sequence number), and make the simulator process one timestep as a batch. A replay then needs no event log: the same network, script, seed and kill list give the same run.
3. **Nodes see only simulated time and a seeded generator.** Reject the calls in source 7 at instrumentation, with a message that names the replacement.
4. **Wall-clock kills are inputs.** A time-based kill (source 6) is written to the run record with the timestep and the node. A replay applies it as a pre-failed node, as ADR 2 already approved.
5. **Check the claim with a test, before building replay.** Run one network twice with the same seed and compare the event logs. Failure of that test finds the sources this list missed.

### Decisions that belong to Alex

- Whether students may call `Random`, `Math.random` and the clocks at all. Banning them is the simplest design and breaks existing student code.
- Whether the timestep batch of sources 4 and 5 is acceptable. It trades throughput for a defined order, and it changes what students observe for ties.
- Whether a CPU limit counted in safe points replaces the CPU-time limit.
- Whether recorded runs from before the tie-break change need to stay readable.
- Which of the three options for the instrumentation cost (above) to take.

## Open questions

- **Instrumentation of student classes: verified 2026-11-22.** The Tascalate agent instruments classes that `ClassFileManager` defines in memory, but only methods marked `@continuable`. `NodeInstrumenter` marks the student methods that can suspend, and `StudentClassInstrumentationTest` covers it.
- **Child JVM flags.** Memory cap and agent path are not chosen.
