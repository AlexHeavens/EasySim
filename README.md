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

Prerequisites: JDK 17 (Gradle downloads the pinned Gradle version itself), and Docker for the workflow linter.

```
scripts/install-gitleaks.sh        # the pre-commit hook needs gitleaks
git config core.hooksPath .githooks
./gradlew jar
java -jar build/libs/EasySim.jar   # opens the window; needs a display
```

## Tests

```
./gradlew build                    # compiles with warnings as errors, runs the tests; CI runs the same
scripts/lint-workflows.sh          # actionlint
```

All tests run offline and need no display.
