# Contributing to Sketchware KG

Thank you for considering a contribution to Sketchware KG. This project grows through careful, community-driven improvements, and every solid contribution helps keep Sketchware alive for more people.

## Ways to contribute

- Fix bugs or regressions.
- Improve performance, stability, or UX.
- Refine documentation and onboarding.
- Review pull requests and reproduce reported issues.
- Suggest focused improvements with a clear problem statement.

## Before you start

1. Read the [README.md](README.md) for project context and build instructions.
2. Search existing issues and pull requests before starting duplicate work.
3. Prefer small, focused pull requests over large mixed changes.

## Local setup

1. Clone the repository.
2. Configure your Android SDK in `local.properties`.
3. Use JDK 17.
4. Build with `./gradlew assembleRelease` or `.\gradlew.bat assembleRelease` on Windows.

If your contribution depends on Firebase or Sketchub integrations, configure the optional environment variables described in [README.md](README.md).

## Code guidelines

- Keep the existing package structure intact.
- If your feature does not require touching legacy packages outside `pro.sketchware`, prefer implementing it there.
- Prefer Java for new code unless Kotlin is clearly the better fit for the change.
- Avoid unrelated refactors in the same pull request.
- Add comments only when they help explain non-obvious logic.

## Testing expectations

- Test the changed flow locally whenever possible.
- Include reproduction steps for bug fixes.
- Mention any areas you could not test.
- If you change build logic, note the exact command you ran.

## Commit messages

Use clear commit messages with a conventional prefix:

- `feat:` for new functionality
- `fix:` for bug fixes
- `docs:` for documentation changes
- `refactor:` for internal cleanup without behavior changes
- `test:` for test-related updates
- `chore:` for maintenance tasks

Examples:

- `feat: add preview support for custom components`
- `fix: prevent crash when importing malformed project files`
- `docs: clarify local build requirements`

## Pull request checklist

Before opening a pull request, make sure you:

1. Kept the scope focused.
2. Explained what changed and why.
3. Linked any relevant issue.
4. Added screenshots when the UI changed.
5. Confirmed the affected flow works locally.

## Reporting bugs and requesting features

- Use the GitHub issue forms for reproducible bugs and focused feature requests.
- Use Discord or Telegram for general discussion and quick questions.
- Use [SECURITY.md](SECURITY.md) for private vulnerability reports.

## Signing keys and secrets

Do not commit personal signing keys, tokens, or private credentials. If you need to test local signing, use your own local-only files and keep them out of version control.

Thank you for helping improve Sketchware KG.
