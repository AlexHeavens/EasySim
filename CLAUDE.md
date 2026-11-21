# EasySim

- Run: `./gradlew jar && java -jar build/libs/EasySim.jar` (needs a display).
- Test: `./gradlew build` (compiler warnings are errors, except the categories excluded in `build.gradle`). Lint workflows: `scripts/lint-workflows.sh`.
- Docs: `README.md` for setup. Source lives in `src/`, tests in `test/`.
- Before each commit, check the staged diff for secrets. The pre-commit hook needs `git config core.hooksPath .githooks`.
