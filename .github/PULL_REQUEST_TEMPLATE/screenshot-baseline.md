## Screenshot baseline update

### Scenarios

List every added, changed, or removed screenshot scenario and its viewport/theme/locale when relevant.

- TODO

### Expected visual changes

Explain why each reference image is expected to change. If production UI and goldens change together, call out both
parts explicitly.

### Review evidence

- Linux golden workflow run: TODO
- `screenshot-goldens-*` artifact or attached contact sheet: TODO

### Checklist

- [ ] I generated the accepted PNG files with `Update screenshot goldens` on `ubuntu-24.04`.
- [ ] I reviewed every added or changed reference image.
- [ ] The PR contains only reference PNG files under `screenshot-tests/src/test/screenshots`; actual, diff, reports,
      and other build output are not committed.
- [ ] `PR screenshot tests / Verify screenshot goldens` passes with strict comparison.
- [ ] Any production UI change in this PR is described above and is visible in the review evidence.
