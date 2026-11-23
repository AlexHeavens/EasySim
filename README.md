# EasySim

EasySim simulates and visualises abstract network algorithms that you define. You describe a network as JSON, write the behaviour of each node as a Java class, run the simulation, and watch the messages and node states in a desktop (Swing) window.

It is a student project from 2012. It is not a library, a packet-level network simulator or a hardened sandbox: node scripts run in a Docker container with no network, a read-only file system and capped resources, but all nodes share one JVM inside it, so a hostile node can harm the others.

## Contents

- `src/`: the application (simulation engine, user-code loader, visualisation, Swing UI).
- `examples/`: example networks, node scripts and saved simulation results.
- `test/`: JUnit tests, plus the networks and node scripts they use.
- `build.gradle`, `gradlew`, `gradle/`: the build and its pinned Gradle wrapper.
- `scripts/`, `pins/`, `.githooks/`: secrets scanning and workflow linting.

## Setup

Prerequisites: JDK 27 (Gradle downloads the pinned Gradle version itself), and Docker. Docker runs the student nodes and the workflow linter. The first simulation pulls a 430 MB image.

```
scripts/install-gitleaks.sh        # the pre-commit hook needs gitleaks
git config core.hooksPath .githooks
./gradlew jar
java -jar build/libs/EasySim.jar   # opens the window; needs a display
```

## How student nodes run

All student nodes run in one child JVM inside a Docker container, as set out in [ADR 0001](docs/adr/0001-shared-child-jvm-with-continuation-scheduler.md), [ADR 0002](docs/adr/0002-child-jvm-protocol.md) and [ADR 0003](docs/adr/0003-run-the-child-jvm-in-a-container.md). The container has no network, a read-only file system, no capabilities, and limits on memory, CPU and processes. A node that cannot be stopped fails the run and names the node. `-Deasysim.child.heap=<megabytes>` sets the child's heap cap (default 512). A script is compiled in the child from the source folder, or loaded from the classpath if it is a compiled class. The build bundles the Tascalate agent jar that the child needs. `-Djavaflow.agent=<path>` replaces it.

The time limit of a node counts wall-clock time that the node spends between requests. The node CPU times in the results are those of the simulator's bridge threads, not of the student code.

## Tests

```
./gradlew build                    # compiles with warnings as errors, runs the tests; CI runs the same
scripts/lint-workflows.sh          # actionlint
```

All tests need no display. The child-JVM tests need Docker and the pinned image, which `docker pull` fetches once. `scripts/pull-sandbox-image.sh` pulls it.
