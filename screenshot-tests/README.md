# Screenshot baseline review

The tracked PNG files in `src/test/screenshots` are a reviewed visual contract, not disposable test output. Linux
`ubuntu-24.04` is the source of truth; locally recorded macOS or Windows images are for preliminary inspection only.

## Updating goldens

1. Push the intended UI and deterministic preview changes to a branch.
2. Run `Update screenshot goldens` from GitHub Actions and select that branch.
3. Download the `screenshot-goldens-*` artifact and inspect every added or changed PNG. A contact sheet may be attached
   to the pull request, but it does not replace inspection at the original resolution.
4. Copy only accepted reference PNG files into `src/test/screenshots`. Do not commit `build/`, actual images, diff
   images, reports, or test results.
5. Open a pull request with the dedicated `screenshot-baseline.md` template. List all affected scenarios, explain
   every expected visual change, and link the Linux workflow run plus its artifact or an attached contact sheet.
6. Require a green `PR screenshot tests / Verify screenshot goldens` result before merge.

Keep strict pixel comparison by default. Add a tolerance only for a measured, reproducible renderer difference and
scope it to the affected scenario. A mass PNG update must never be accepted without reviewing every image.
