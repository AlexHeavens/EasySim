# EasySim

EasySim simulates and visualises abstract network algorithms that you define. You describe a network as JSON, write the behaviour of each node as a Java class, run the simulation, and watch the messages and node states in a desktop (Swing) window.

It is a student project from 2012. It is not a library, a packet-level network simulator or a tool that runs untrusted code safely: node scripts are compiled and run in your JVM.

## Contents

- `src/`: the application (simulation engine, user-code loader, visualisation, Swing UI).
- `examples/`: example networks, node scripts and saved simulation results.
- `test/`: JUnit tests, plus the networks and node scripts they use.
- `build.gradle`, `gradlew`, `gradle/`: the build and its pinned Gradle wrapper.
- `scripts/`, `pins/`, `.githooks/`: secrets scanning and workflow linting.

## Setup

Prerequisites: JDK 27 (Gradle downloads the pinned Gradle version itself), and Docker for the workflow linter.

```
scripts/install-gitleaks.sh        # the pre-commit hook needs gitleaks
git config core.hooksPath .githooks
./gradlew jar
java -jar build/libs/EasySim.jar   # opens the window; needs a display
```

## Running nodes in a child JVM (experimental)

Add `-Deasysim.child=true -Djavaflow.agent=<path of the Tascalate agent jar>` to run all student nodes in one child JVM, as set out in [ADR 0001](docs/adr/0001-shared-child-jvm-with-continuation-scheduler.md) and [ADR 0002](docs/adr/0002-child-jvm-protocol.md). A node that cannot be stopped fails the run and names the node. `-Deasysim.child.heap=<size>` sets the child's heap cap (default `512m`). The mode is off by default while it is built out. A script is compiled in the child from the source folder, or loaded from the classpath if it is a compiled class.

Differences from the default mode: the time limit of a node counts wall-clock time that the node spends between requests, and the node CPU times in the results are those of the simulator's bridge threads, not of the student code.

## Tests

```
./gradlew build                    # compiles with warnings as errors, runs the tests; CI runs the same
scripts/lint-workflows.sh          # actionlint
```

All tests run offline and need no display.
