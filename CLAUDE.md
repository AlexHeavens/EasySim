# EasySim

- Run: `./gradlew jar && java -jar build/libs/EasySim.jar` (needs a display).
- Test: `./gradlew build` (compiler warnings are errors, except the categories excluded in `build.gradle`). Lint workflows: `scripts/lint-workflows.sh`.
- Docs: `README.md` for setup. Source lives in `src/`, tests in `test/`. Design decisions are in `docs/adr/`.
- Tests in `scheduler` and `child` packages need the Tascalate agent and run in the Gradle task `agentTest`, not `test`. `check` depends on it.
- Before each commit, check the staged diff for secrets. The pre-commit hook needs `git config core.hooksPath .githooks`.
