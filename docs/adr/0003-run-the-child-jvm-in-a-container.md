# 3. Run the child JVM in a container

Date: 2026-11-23. Status: accepted (Alex, 2026-11-23). Builds on [ADR 2](0002-child-jvm-protocol.md).

## Context

The child JVM keeps a buggy student script from hanging or crashing the simulator. It does not limit what the script can do. The child runs as the simulator's user, so a script can read that user's files, open network connections and start processes. Java 24 and later permanently disabled the `SecurityManager`, so the JVM cannot sandbox the script itself. The operating system has to.

## Decision

`ChildJvm` starts the child with `docker run`. The image is `eclipse-temurin:27-jdk`, pinned by digest in `pins/sandbox/Dockerfile`. The build bundles that line into the jar. The image needs the JDK because the child compiles the student sources.

| Flag | What it stops |
|---|---|
| `--network none` | all network access, including the host's loopback |
| `--read-only`, with a 64 MB `/sandbox-tmp` | writes anywhere but the scratch folder |
| `--cap-drop ALL`, `--security-opt no-new-privileges` | privileged operations and privilege gain |
| `--user <simulator uid:gid>` | running as root. Without capabilities, root cannot read a private folder of the simulator's user. |
| `--memory`, `--memory-swap` | heap plus 256 MB, so a memory hog is killed, not swapped |
| `--cpus`, `--pids-limit 512` | a CPU spin that starves the simulator, and a fork bomb |
| read-only volumes | the agent jar, the student sources and the classpath entries, each mounted read-only |

**Transport.** The container has no network, so the frames of ADR 2 travel over the container's standard input and output. This works on every host that runs Docker, where a Unix socket would not cross the virtual machine boundary on macOS and Windows. `ChildMain` points `System.out` at standard error first, so student output cannot corrupt the frames. The child writes `READY` once it can take frames, so the start-up wait covers an image pull.

**Kill ladder.** Killing the `docker` client does not stop the container, because the Docker daemon owns it. The simulator therefore runs `docker rm --force <name>` first, and then kills the client. If the simulator itself dies, the child sees its input close and exits.

## What it does not stop

- **One node can harm another.** All nodes share one JVM inside the container (ADR 1). A node can read the static state of another, or write to the protocol stream through `FileDescriptor.out`. This is a known, accepted limit. One container per node would remove it at a cost that ADR 1 rejected.
- **The student code can read the simulator's classes.** The classpath is mounted read-only so the child can run. It holds no secrets, but it is visible.
- **The simulator user needs access to Docker.** On Linux, membership of the `docker` group is equal to root on the host. The sandbox protects the host from student code. It does not protect it from the simulator's user.

## Consequences

- Docker is a run-time requirement. A host without Docker fails with a message that says so. No fallback runs student code outside a container.
- The first run pulls about 430 MB.
- Dependabot proposes image updates, as it does for the other pins. A bump needs the child tests to pass before it merges.
- Tested on Linux only. macOS and Windows hosts are unverified.

## Alternatives

- **`bwrap` or `unshare`.** It needs no daemon, but it exists on Linux only, and the policy is hand-built.
- **A container per node.** It gives full isolation between nodes, and it gives up the shared child JVM of ADR 1.
- **gVisor or a micro-VM.** A stronger boundary against kernel exploits. It is a possible later step on the same `docker run` interface (`--runtime`).
