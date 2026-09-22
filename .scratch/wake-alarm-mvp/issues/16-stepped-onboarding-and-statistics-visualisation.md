# 16: Stepped onboarding and a statistics visualisation

**What to build:** Two presentation improvements. (1) Split the first-run flow
into explicit numbered steps with progress, instead of one long scrolling page.
(2) Give the Statistics page a simple visual of recent mornings alongside its
numbers, still derived entirely from recorded history in `core`.

**Blocked by:** None

Status: ready-for-human

- [ ] First run is a sequence of numbered steps (wake time, Physical Anchor,
      Escape Hatch password) with visible progress, not one long page.
- [x] The flow still completes exactly when `core`'s `Onboarding.isComplete` is
      true, and still resumes at the first missing step.
- [x] Statistics shows a simple chart of recent dismissals (e.g. ringing
      duration or wrong answers per morning) using the existing `core`
      `WakeStatistics`, with no new persistence.
- [x] The chart labels are localised (Chinese default, English on an English
      locale).
- [x] `./gradlew :core:test :app:assembleDebug` is green.

## Comments

Implemented on branch `main`. `:core` gained only pure logic and tests; no spec,
ADR, or other ticket's `Blocked by` was touched, the `:core` onboarding decision
and its tests were left exactly as they were, and no Snooze concept was
introduced.

### 1. Stepped onboarding

- `OnboardingScreen` no longer hard-codes its step transitions. It keeps the
  `Onboarding` it was handed as local state, and after every step re-reads the
  configuration from the repositories to recompute
  `Onboarding.of(...)`; the step shown is always `missingSteps.firstOrNull()`, and
  the host is handed control only once `Onboarding.isComplete` is true. So the
  flow resumes at the first genuinely missing step rather than at a remembered
  position, and finishing cannot drift from what the dismiss flow will require.
- Each step renders alone (wake time → Physical Anchor → Escape Hatch password),
  with the existing "第 N 步，共 3 步" label plus a new three-segment
  `StepIndicator` that fills the completed and current steps. Each step keeps its
  short explanation and its next/finish button. No new behaviour: the same
  actions (create the first Alarm and reschedule, scan and bind the anchor, save
  the password) now just reload the `core` state afterwards.

### 2. Statistics visualisation

- `:core` adds `DismissalChart.kt` (pure, no Android): `DismissalMetric`
  (`RingingSeconds` / `WrongAnswers`), `DismissalBar` (day, value, and height
  `fraction` in `0f..1f`), and the `List<DismissalRecord>.chartBars(metric)`
  mapper that scales each recorded morning against the series' tallest figure.
  It reads only the records `WakeStatistics.recentDismissals` already produces —
  no new persistence anywhere. Covered by `DismissalChartTest` (5 tests: empty,
  ringing-seconds scaling, wrong-answer scaling, order preserved, all-zero figures
  give zero-height bars).
- `StatisticsScreen` draws a native Compose bar chart (a weighted `Row` of
  `Box`es whose heights come from `chartBars`), with the numeric value above each
  bar and the localised weekday below it, and reuses the existing
  `statistics_empty` placeholder when there is nothing recorded. No chart library
  was added; `:app` only renders what `core` decided. The day formatter is built
  from `LocalConfiguration.current.locales[0]`, so it follows the in-app language
  override as well as the device locale.
- New key `statistics_chart_title` in both `res/values/strings.xml` (Chinese
  default) and `res/values-en/strings.xml`; the two key sets remain identical
  (167 keys each, verified with `diff`).

### Verification

- `JAVA_HOME=$(brew --prefix openjdk@17) ./gradlew :core:test :app:assembleDebug`
  — BUILD SUCCESSFUL, APK produced.
- `:core` green: 227 tests, of which 5 are the new `DismissalChartTest`.
- `diff` of the `name="…"` sets of the two `strings.xml` files reports no
  difference (167 keys each); no hard-coded user-facing copy was added.
- `rg` finds no Snooze except the existing comments stating its deliberate
  absence.

### Left to confirm on the real device (待机主真机确认)

- Checkbox 1 stays unchecked: the flow is structurally a sequence of numbered
  steps with a progress indicator, but **what the owner actually sees** —
  the step bar rendering and each step fitting the screen — needs the owner's
  phone. The step selection, resume point, and completion gating themselves are
  verified by code (checkbox 2).
- Worth a glance while there: the chart's bars and their value/day labels render
  cleanly at the owner's font size (the data is covered by `DismissalChartTest`,
  so this is appearance only, not data).
