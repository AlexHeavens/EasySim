# 1. Run all student nodes in one child JVM on a continuation scheduler

Date: 2026-11-22. Status: accepted.

## Context

Student code runs as simulation nodes. We must assume it is buggy. A run can hold thousands of nodes. We need to kill a node that never returns. We also want to let a chosen node hang or fail, so students can debug partial results.

The JVM cannot guarantee a kill in-process. `Thread.stop()` was removed in JDK 20. Our bytecode safe-point checks and watchdog (`KillableThread`) cannot stop a node in a native call or in a blocking call that ignores interrupts. Only killing a process is guaranteed.

## Decision

1. Run all student nodes in one dedicated child JVM, separate from the simulator. The simulator talks to it over a socket. If a node cannot be killed, kill the child process (`Process.destroyForcibly()`) and report the run as failed, naming the node.
2. Run each node as a continuation (a suspendable computation) on a small scheduler, instead of one thread per node. The scheduler has one worker per core and a FIFO run queue. The existing `KillableThread.check()` safe points yield the continuation after a time slice.
3. Use the Tascalate JavaFlow library (instrumented-continuations variant) for the continuations. Keep the scheduler behind an interface, so we can swap the continuation provider.

The existing kill, watchdog and blocked-thread timeout stay as the first line of defence.

## Evidence

Spike on 4 cores, JDK 27, one run each. Waiting nodes reply to one message. Spinner nodes loop until stopped.

| Approach | Scale tested | Result |
|---|---|---|
| Platform threads, 256 KB stack | 1,000 nodes | about 30 MB, preemption works; heavy beyond about 5,000 |
| Default virtual-thread scheduler | 5,000 nodes + spinners | starved: replies arrived only after spinners stopped |
| Own scheduler on `jdk.internal.vm.Continuation` | 200,000 nodes + 400 spinners | replies in 1,697 ms, +94 MB |
| Tascalate JavaFlow (instrumented) | 200,000 nodes + 400 spinners | replies in 150 ms, +133 MB |

## Options rejected

- **Platform thread per node:** memory and scheduling cost too high for thousands of nodes.
- **Default virtual-thread scheduler:** spinners starve other nodes. The cause is unconfirmed.
- **Own scheduler on `jdk.internal.vm.Continuation`:** needs `--add-exports=java.base/jdk.internal.vm=ALL-UNNAMED` and a pinned JDK, and we would maintain it. Kept as the fallback behind the interface.
- **SimGrid:** replaces the whole simulator, not just the node runtime.
- **Jobe, CodeJail, Docker:** sandbox one process per submission. They do not give thousands of nodes in one process.
- **Quasar, Kilim:** appear unmaintained.

We chose an existing library because the scheduler is not where EasySim differentiates.

## Licences (checked 2026-11-22)

- EasySim is MIT.
- Tascalate JavaFlow is Apache-2.0: GitHub reports `Apache-2.0` and `LICENSE` on `master` is the Apache text. The Maven POMs carry no licence element.
- The agent jar bundles relocated copies of ASM (`net/tascalate/asmx`) and SLF4J. ASM is BSD-3-Clause and SLF4J is MIT, from our knowledge of those projects. The jar holds no licence files for them. Verify against their upstream licences before release.
- Apache-2.0, BSD and MIT are compatible with an MIT project. We must carry their notices in `NOTICE`.

## Consequences and unverified risks

- A node that blocks (sleep, lock, I/O) holds a worker. The blocked-thread timeout must kill it, or the scheduler must replace the worker.
- A yield inside `synchronized` loses the monitor, so `NodeInstrumenter` rejects `synchronized` (ADR 0002).
- Instrumentation of student classes is verified, and its run-time cost is measured in ADR 0002: about 9x to 50x on CPU-bound code.
- Unverified: behaviour with many workers blocked at once.
- Single-run numbers; repeat before relying on the 150 ms figure.
- The last resort kills the whole child JVM, so one stuck node fails the whole run. Replaying the run with that node pre-failed is deferred, because it needs deterministic nodes.

## Tests to add with the build

- A scheduler fairness test: spinners must not delay replies from waiting nodes.
- A process-kill test: an uninterruptible node ends the run and names the node.
