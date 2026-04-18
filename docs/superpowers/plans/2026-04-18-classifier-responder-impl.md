# Classifier-Responder Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a new 3-player IPD strategy (`ClassifierResponderPlayer`) that classifies each opponent into {SAFE, HOSTILE, EXPLOITABLE, AMBIGUOUS}, applies a pessimistic joint response rule, and defects on the provably-final round (n=109) when both opponents are SAFE.

**Architecture:** New inner class inside `ThreePrisonersDilemma.java`. Stateless — all logic is a pure function of the history arrays passed by the harness. Helpers are private instance methods on the player. Tests added to the existing `mainTest()` harness (triggered by `java ThreePrisonersDilemma --test`).

**Tech Stack:** Java (existing file; no build system, compiled with `javac`). Tests are `if`-assertions inside `mainTest()` that increment a `failures` counter and print `PASS`/`FAIL` — follow the style already used for `T4TPlayer`, `GradualPunisherPlayer`, etc.

**Reference:** Spec at `docs/superpowers/specs/2026-04-18-classifier-responder-design.md`.

**Insertion points in `ThreePrisonersDilemma.java`:**
- New player class: immediately after `DBSPlayer` (currently ends at line 1001), before the `scoresOfMatch` comment at line 1003
- `makePlayer()` switch: add `case 28` (currently ends at line 1102 with `case 27`)
- `numPlayers` field: currently line 1043, change from 28 → 29
- `mainTest()`: append new test block near end of function (currently line 1182+)

---

## Task 1: Scaffold the player class and register it

**Files:**
- Modify: `ThreePrisonersDilemma.java` (insert class after line 1001; update `numPlayers` on line 1043; add `case 28` in `makePlayer`; add round-0 test in `mainTest`)

**Goal:** Get a minimal new `ClassifierResponderPlayer` that cooperates on round 0 and defects otherwise, registered in the tournament harness, with one passing test. No logic yet — just scaffolding we can grow into.

- [ ] **Step 1: Write the failing test in `mainTest()`**

Insert near the end of `mainTest()`, just before the `if (failures == 0)` summary block (grep for that line to find it). Add:

```java
        // ---- ClassifierResponderPlayer ----
        ClassifierResponderPlayer cr = inst.new ClassifierResponderPlayer();
        if (cr.selectAction(0, new int[0], new int[0], new int[0]) != 0) {
            failures++;
            System.out.println("FAIL ClassifierResponder round0 should cooperate");
        } else {
            System.out.println("PASS ClassifierResponder round0");
        }
```

- [ ] **Step 2: Run test to verify it fails with a compile error**

```bash
cd /Users/zhunhao/Documents/Projects/prisoners-dilemma
javac ThreePrisonersDilemma.java
```

Expected output: `error: cannot find symbol ... class ClassifierResponderPlayer`.

- [ ] **Step 3: Add the stub player class**

Insert immediately after the closing brace of `DBSPlayer` (line 1001) and before the `/* In our tournament ... */` comment:

```java
    // =========================================================
    // SUBMISSION: Classifier-Responder
    // =========================================================
    /*
     * Submission strategy. Classifies each opponent into one of four classes based on
     * observed history (no active probing), then emits one action per round using a
     * pessimistic joint-response rule. On the provably-final round (n=109), defects
     * if both opponents are classified SAFE.
     *
     * See docs/superpowers/specs/2026-04-18-classifier-responder-design.md.
     *
     * Classes: AMBIGUOUS=0, SAFE=1, EXPLOITABLE=2, HOSTILE=3.
     */
    class ClassifierResponderPlayer extends Player {
        static final int CLASS_AMBIGUOUS = 0;
        static final int CLASS_SAFE = 1;
        static final int CLASS_EXPLOITABLE = 2;
        static final int CLASS_HOSTILE = 3;

        int selectAction(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2) {
            if (n == 0) return 0;
            return 1; // stub — replaced in later tasks
        }
    }
```

- [ ] **Step 4: Register the player in `makePlayer()` and bump `numPlayers`**

Change line 1043 from:

```java
    int numPlayers = 28; // 6 baseline + 10 prior (EvolvedANN x2) + 12 research canon (2 duplicates pruned)
```

to:

```java
    int numPlayers = 29; // 28 zoo + 1 submission (ClassifierResponder)
```

Add a new case to the `makePlayer()` switch (currently ends with `case 27: return new DBSPlayer();`). Insert just before the closing `}` of the switch:

```java
            case 28:
                return new ClassifierResponderPlayer();
```

- [ ] **Step 5: Compile and run the test**

```bash
javac ThreePrisonersDilemma.java && java ThreePrisonersDilemma --test 2>&1 | grep ClassifierResponder
```

Expected:
```
PASS ClassifierResponder round0
```

- [ ] **Step 6: Commit**

```bash
git add ThreePrisonersDilemma.java docs/superpowers/
git commit -m "feat(submission): scaffold ClassifierResponderPlayer"
```

---

## Task 2: Implement `isProvoked` helper

**Files:**
- Modify: `ThreePrisonersDilemma.java` (add method inside `ClassifierResponderPlayer`; add tests in `mainTest()`)

**Goal:** Helper that answers "was a defection at round `r` provoked?" — i.e., did any of the three players defect in round `r-1` or `r-2`? This underpins all HOSTILE/SAFE rules.

**Definition (from spec):** A defection at round `r` is **provoked** if any player defected in round `r-1` or `r-2`. At `r == 0` there is no prior round, so defections there are never provoked.

- [ ] **Step 1: Write the failing tests**

Append in `mainTest()` immediately after the round-0 test from Task 1:

```java
        // isProvoked: round 0 is never provoked
        if (cr.isProvoked(0, new int[0], new int[0], new int[0]) != false) {
            failures++; System.out.println("FAIL isProvoked r0 should be false");
        }
        // isProvoked: round 1 provoked if any history[0] == 1
        if (cr.isProvoked(1, new int[]{0}, new int[]{1}, new int[]{0}) != true) {
            failures++; System.out.println("FAIL isProvoked r1 h2 defect");
        }
        if (cr.isProvoked(1, new int[]{0}, new int[]{0}, new int[]{0}) != false) {
            failures++; System.out.println("FAIL isProvoked r1 all cooperated");
        }
        // isProvoked: round 3 provoked if defection at round 1 or 2 (r-2 or r-1)
        if (cr.isProvoked(3, new int[]{0,0,0}, new int[]{0,1,0}, new int[]{0,0,0}) != true) {
            failures++; System.out.println("FAIL isProvoked r3 h2[1] defect");
        }
        // isProvoked: round 3 NOT provoked if the only defection was at round 0 (out of window)
        if (cr.isProvoked(3, new int[]{0,0,0}, new int[]{1,0,0}, new int[]{0,0,0}) != false) {
            failures++; System.out.println("FAIL isProvoked r3 defect at r0 out of window");
        }
        System.out.println("PASS isProvoked tests");
```

- [ ] **Step 2: Run test to verify it fails with compile error**

```bash
javac ThreePrisonersDilemma.java
```

Expected: `error: cannot find symbol ... method isProvoked(int,int[],int[],int[])`.

- [ ] **Step 3: Implement the helper inside `ClassifierResponderPlayer`**

Add above `selectAction`:

```java
        /** True if any of the three players defected in round r-1 or r-2.
         *  At r == 0, always false (no prior rounds). */
        boolean isProvoked(int r, int[] h1, int[] h2, int[] h3) {
            if (r <= 0) return false;
            int lo = Math.max(0, r - 2);
            for (int i = lo; i < r; i++) {
                if (h1[i] == 1 || h2[i] == 1 || h3[i] == 1) return true;
            }
            return false;
        }
```

- [ ] **Step 4: Run tests**

```bash
javac ThreePrisonersDilemma.java && java ThreePrisonersDilemma --test 2>&1 | grep -E "isProvoked|ClassifierResponder"
```

Expected:
```
PASS ClassifierResponder round0
PASS isProvoked tests
```

- [ ] **Step 5: Commit**

```bash
git add ThreePrisonersDilemma.java
git commit -m "feat(submission): add isProvoked helper"
```

---

## Task 3: Implement unprovoked-defect counting helpers

**Files:**
- Modify: `ThreePrisonersDilemma.java` (add two methods on `ClassifierResponderPlayer`; add tests in `mainTest()`)

**Goal:** Two helpers:
- `countUnprovokedDefects(opp, me, other)` — count defections in `opp` that were not provoked.
- `countUnprovokedDefectsInWindow(opp, me, other, lo, hi)` — same but restricted to rounds in `[lo, hi]`.

These implement H2 and H3 evidence rules directly.

- [ ] **Step 1: Write the failing tests**

Append in `mainTest()` after the `isProvoked` tests:

```java
        // countUnprovokedDefects: no defections
        if (cr.countUnprovokedDefects(new int[]{0,0,0,0}, new int[]{0,0,0,0}, new int[]{0,0,0,0}) != 0) {
            failures++; System.out.println("FAIL countUnprovoked clean history");
        }
        // countUnprovokedDefects: single round-0 defection (always unprovoked)
        if (cr.countUnprovokedDefects(new int[]{1,0,0,0}, new int[]{0,0,0,0}, new int[]{0,0,0,0}) != 1) {
            failures++; System.out.println("FAIL countUnprovoked r0 defect");
        }
        // countUnprovokedDefects: defection at r=3 right after my defection at r=2 -> provoked, count 0
        if (cr.countUnprovokedDefects(new int[]{0,0,0,1}, new int[]{0,0,1,0}, new int[]{0,0,0,0}) != 0) {
            failures++; System.out.println("FAIL countUnprovoked provoked defect");
        }
        // countUnprovokedDefects: defection at r=3 with clean preceding rounds -> unprovoked, count 1
        if (cr.countUnprovokedDefects(new int[]{0,0,0,1}, new int[]{0,0,0,0}, new int[]{0,0,0,0}) != 1) {
            failures++; System.out.println("FAIL countUnprovoked unprovoked late defect");
        }
        // countUnprovokedDefectsInWindow: restrict to rounds 0-10
        int[] longOpp = new int[15]; longOpp[5] = 1; longOpp[12] = 1; // two defections
        int[] zero15 = new int[15];
        if (cr.countUnprovokedDefectsInWindow(longOpp, zero15, zero15, 0, 10) != 1) {
            failures++; System.out.println("FAIL countUnprovokedInWindow should count only r=5");
        }
        if (cr.countUnprovokedDefectsInWindow(longOpp, zero15, zero15, 0, 20) != 2) {
            failures++; System.out.println("FAIL countUnprovokedInWindow wide window");
        }
        System.out.println("PASS countUnprovoked tests");
```

- [ ] **Step 2: Run test to verify it fails**

```bash
javac ThreePrisonersDilemma.java
```

Expected: compile errors about missing `countUnprovokedDefects` and `countUnprovokedDefectsInWindow`.

- [ ] **Step 3: Implement both helpers inside `ClassifierResponderPlayer`**

Add below `isProvoked`:

```java
        /** Count defections in opp that were not provoked by any player's defection in r-1 or r-2. */
        int countUnprovokedDefects(int[] opp, int[] me, int[] other) {
            int count = 0;
            for (int r = 0; r < opp.length; r++) {
                if (opp[r] == 1 && !isProvoked(r, opp, me, other)) count++;
            }
            return count;
        }

        /** Same as countUnprovokedDefects, but only for rounds whose index is in [lo, hi] inclusive. */
        int countUnprovokedDefectsInWindow(int[] opp, int[] me, int[] other, int lo, int hi) {
            int count = 0;
            int end = Math.min(opp.length - 1, hi);
            for (int r = Math.max(0, lo); r <= end; r++) {
                if (opp[r] == 1 && !isProvoked(r, opp, me, other)) count++;
            }
            return count;
        }
```

- [ ] **Step 4: Run tests**

```bash
javac ThreePrisonersDilemma.java && java ThreePrisonersDilemma --test 2>&1 | grep -E "countUnprovoked|isProvoked|ClassifierResponder"
```

Expected:
```
PASS ClassifierResponder round0
PASS isProvoked tests
PASS countUnprovoked tests
```

- [ ] **Step 5: Commit**

```bash
git add ThreePrisonersDilemma.java
git commit -m "feat(submission): add unprovoked-defect counting helpers"
```

---

## Task 4: Implement aggregate helpers (`countDefects`, `totalDefectsInMatch`, `roundsSinceAnyDefect`)

**Files:**
- Modify: `ThreePrisonersDilemma.java` (add three helpers; add tests)

**Goal:** Helpers needed by EXPLOITABLE rules E1 and E2.

- [ ] **Step 1: Write the failing tests**

Append in `mainTest()`:

```java
        // countDefects
        if (cr.countDefects(new int[]{0,1,0,1,1}) != 3) {
            failures++; System.out.println("FAIL countDefects basic");
        }
        if (cr.countDefects(new int[0]) != 0) {
            failures++; System.out.println("FAIL countDefects empty");
        }
        // totalDefectsInMatch: sum across all three histories
        if (cr.totalDefectsInMatch(new int[]{0,1,0}, new int[]{1,0,0}, new int[]{0,0,1}) != 3) {
            failures++; System.out.println("FAIL totalDefectsInMatch");
        }
        // roundsSinceAnyDefect: someone defected at round 2, we're at n=5 -> 5-2 = 3
        if (cr.roundsSinceAnyDefect(5, new int[]{0,0,1,0,0}, new int[]{0,0,0,0,0}, new int[]{0,0,0,0,0}) != 3) {
            failures++; System.out.println("FAIL roundsSinceAnyDefect r2 defect");
        }
        // roundsSinceAnyDefect: no defects ever -> Integer.MAX_VALUE
        if (cr.roundsSinceAnyDefect(5, new int[]{0,0,0,0,0}, new int[]{0,0,0,0,0}, new int[]{0,0,0,0,0}) != Integer.MAX_VALUE) {
            failures++; System.out.println("FAIL roundsSinceAnyDefect no defects");
        }
        // roundsSinceAnyDefect: most recent defect at r=n-1 -> 1
        if (cr.roundsSinceAnyDefect(5, new int[]{0,0,0,0,1}, new int[]{0,0,0,0,0}, new int[]{0,0,0,0,0}) != 1) {
            failures++; System.out.println("FAIL roundsSinceAnyDefect recent");
        }
        System.out.println("PASS aggregate helpers tests");
```

- [ ] **Step 2: Run test to verify it fails**

```bash
javac ThreePrisonersDilemma.java
```

Expected: compile errors about missing helpers.

- [ ] **Step 3: Implement the three helpers inside `ClassifierResponderPlayer`**

Add below `countUnprovokedDefectsInWindow`:

```java
        /** Count defections in a single history. */
        int countDefects(int[] h) {
            int c = 0;
            for (int v : h) if (v == 1) c++;
            return c;
        }

        /** Total defections across all three player histories. */
        int totalDefectsInMatch(int[] h1, int[] h2, int[] h3) {
            return countDefects(h1) + countDefects(h2) + countDefects(h3);
        }

        /** Rounds elapsed since the most recent defection by anyone.
         *  If last defect was at round r, returns n - r.
         *  If no defects, returns Integer.MAX_VALUE. */
        int roundsSinceAnyDefect(int n, int[] h1, int[] h2, int[] h3) {
            for (int r = n - 1; r >= 0; r--) {
                if (h1[r] == 1 || h2[r] == 1 || h3[r] == 1) return n - r;
            }
            return Integer.MAX_VALUE;
        }
```

- [ ] **Step 4: Run tests**

```bash
javac ThreePrisonersDilemma.java && java ThreePrisonersDilemma --test 2>&1 | grep -E "aggregate|countUnprovoked|isProvoked|ClassifierResponder"
```

Expected all `PASS`.

- [ ] **Step 5: Commit**

```bash
git add ThreePrisonersDilemma.java
git commit -m "feat(submission): add aggregate defect-counting helpers"
```

---

## Task 5: Implement `classify` with all rules (H1, H2, H3, E1, E2, S1, S2)

**Files:**
- Modify: `ThreePrisonersDilemma.java` (add `classify` method; add rule-specific tests)

**Goal:** The classifier. Pure function from history to `{AMBIGUOUS, SAFE, EXPLOITABLE, HOSTILE}`. Implements all rules from spec §5 in the documented order.

**Rule recap:**
1. `n == 0` → AMBIGUOUS
2. H1: opp defected at round 0 → HOSTILE
3. H2: `unprovoked_defects(opp) >= 2` → HOSTILE
4. H3: `unprovoked_defects_in_rounds_0_to_10(opp) >= 1` → HOSTILE
5. E1: `total_defects_in_match >= 1 AND rounds_since_any_defect >= 5 AND countDefects(opp) == 0` → EXPLOITABLE
6. E2: `n >= 20 AND opp has no defection in last 20 rounds AND someone else defected in last 20 rounds` → EXPLOITABLE
7. S1: all of opp's defections were provoked AND `rounds_since_defect(opp) >= 3` → SAFE
8. S2: `countDefects(opp) == 0 AND n >= 8` → SAFE
9. Fallback → AMBIGUOUS

- [ ] **Step 1: Write failing tests covering each rule**

Append in `mainTest()`:

```java
        // classify at n=0 -> AMBIGUOUS
        if (cr.classify(0, new int[0], new int[0], new int[0]) != ClassifierResponderPlayer.CLASS_AMBIGUOUS) {
            failures++; System.out.println("FAIL classify n=0");
        }
        // H1: opp defected at round 0 -> HOSTILE (evaluated at n=1)
        if (cr.classify(1, new int[]{1}, new int[]{0}, new int[]{0}) != ClassifierResponderPlayer.CLASS_HOSTILE) {
            failures++; System.out.println("FAIL classify H1 r0 defect");
        }
        // H3: unprovoked defection at round 5 -> HOSTILE at n=6
        {
            int[] opp = {0,0,0,0,0,1};
            int[] zero = {0,0,0,0,0,0};
            if (cr.classify(6, opp, zero, zero) != ClassifierResponderPlayer.CLASS_HOSTILE) {
                failures++; System.out.println("FAIL classify H3 r5 unprovoked");
            }
        }
        // H3 monotonic: still HOSTILE at n=15 (window covers the round-5 defection)
        {
            int[] opp = new int[15]; opp[5] = 1;
            int[] zero = new int[15];
            if (cr.classify(15, opp, zero, zero) != ClassifierResponderPlayer.CLASS_HOSTILE) {
                failures++; System.out.println("FAIL classify H3 still HOSTILE at n=15");
            }
        }
        // H2: two unprovoked defections (both outside 0-10 window) -> HOSTILE
        {
            int[] opp = new int[20]; opp[12] = 1; opp[16] = 1;
            int[] zero = new int[20];
            if (cr.classify(20, opp, zero, zero) != ClassifierResponderPlayer.CLASS_HOSTILE) {
                failures++; System.out.println("FAIL classify H2 two late unprovoked");
            }
        }
        // S2: 0 defects, n=8 -> SAFE
        {
            int[] opp = new int[8];
            int[] zero = new int[8];
            if (cr.classify(8, opp, zero, zero) != ClassifierResponderPlayer.CLASS_SAFE) {
                failures++; System.out.println("FAIL classify S2 clean n=8");
            }
        }
        // S1: opp defected only in response (provoked), last defect >=3 rounds ago -> SAFE
        {
            int[] opp   = {0,0,1,0,0,0,0,0,0,0};
            int[] me    = {0,1,0,0,0,0,0,0,0,0};
            int[] other = {0,0,0,0,0,0,0,0,0,0};
            if (cr.classify(10, opp, me, other) != ClassifierResponderPlayer.CLASS_SAFE) {
                failures++; System.out.println("FAIL classify S1 provoked-only old defect");
            }
        }
        // E1: opp has 0 defections, someone else defected, 5+ rounds ago -> EXPLOITABLE
        {
            int[] opp   = new int[10];
            int[] me    = new int[10];
            int[] other = new int[10]; other[2] = 1; // defect at r=2, we're at n=10 -> 8 rounds ago
            // Note: opp has 0 defects, total_defects_in_match = 1, rounds_since_any_defect = 8 >= 5
            if (cr.classify(10, opp, me, other) != ClassifierResponderPlayer.CLASS_EXPLOITABLE) {
                failures++; System.out.println("FAIL classify E1 non-retaliator");
            }
        }
        System.out.println("PASS classify tests");
```

- [ ] **Step 2: Run test to verify it fails**

```bash
javac ThreePrisonersDilemma.java
```

Expected: `cannot find symbol ... method classify(...)`.

- [ ] **Step 3: Implement `classify` inside `ClassifierResponderPlayer`**

Add below the aggregate helpers:

```java
        /** Classify one opponent based on observed history. See spec §5 for rules. */
        int classify(int n, int[] opp, int[] me, int[] other) {
            if (n == 0) return CLASS_AMBIGUOUS;

            // H1: defection at round 0 is always unprovoked
            if (opp.length > 0 && opp[0] == 1) return CLASS_HOSTILE;

            // H2: two or more unprovoked defections anywhere
            if (countUnprovokedDefects(opp, me, other) >= 2) return CLASS_HOSTILE;

            // H3: any unprovoked defection in the 0..10 window (monotonic)
            if (countUnprovokedDefectsInWindow(opp, me, other, 0, 10) >= 1) return CLASS_HOSTILE;

            // E1: opp has 0 defects, defection happened, and it's been quiet for 5+ rounds
            int totalDefects = totalDefectsInMatch(opp, me, other);
            int rsad = roundsSinceAnyDefect(n, opp, me, other);
            if (totalDefects >= 1 && rsad >= 5 && countDefects(opp) == 0) {
                return CLASS_EXPLOITABLE;
            }

            // E2: opp hasn't defected in last 20 rounds, but someone else has
            if (n >= 20) {
                boolean recentOppDefect = false;
                boolean recentOtherDefect = false;
                for (int r = n - 20; r < n; r++) {
                    if (opp[r] == 1) recentOppDefect = true;
                    if (me[r] == 1 || other[r] == 1) recentOtherDefect = true;
                }
                if (!recentOppDefect && recentOtherDefect) return CLASS_EXPLOITABLE;
            }

            // S1: all opp's defections were provoked, and last one was >= 3 rounds ago
            boolean allProvoked = true;
            int lastOppDefect = -1;
            for (int r = 0; r < opp.length; r++) {
                if (opp[r] == 1) {
                    lastOppDefect = r;
                    if (!isProvoked(r, opp, me, other)) { allProvoked = false; break; }
                }
            }
            if (allProvoked && (lastOppDefect == -1 || n - lastOppDefect >= 3)) {
                if (countDefects(opp) > 0) return CLASS_SAFE; // S1
            }

            // S2: 0 defections and n >= 8
            if (countDefects(opp) == 0 && n >= 8) return CLASS_SAFE;

            return CLASS_AMBIGUOUS;
        }
```

- [ ] **Step 4: Run tests**

```bash
javac ThreePrisonersDilemma.java && java ThreePrisonersDilemma --test 2>&1 | grep -E "classify|aggregate|countUnprovoked|isProvoked|ClassifierResponder"
```

Expected all `PASS`. If any fail, re-read the spec §5 rule, compare to the test case, and fix the classifier. Do NOT relax the test.

- [ ] **Step 5: Commit**

```bash
git add ThreePrisonersDilemma.java
git commit -m "feat(submission): implement classifier with H1/H2/H3/E1/E2/S1/S2 rules"
```

---

## Task 6: Implement `jointResponse` with pessimistic rule

**Files:**
- Modify: `ThreePrisonersDilemma.java` (add `jointResponse` method; add table-cell tests)

**Goal:** Pure function mapping `(class1, class2, opp1Last, opp2Last)` → action, per spec §6.

**Rule:**
- If either opponent is HOSTILE or EXPLOITABLE → defect
- If both SAFE → cooperate
- Otherwise → `max(opp1Last, opp2Last)` (TFT-worse)

- [ ] **Step 1: Write failing tests for every relevant cell**

Append in `mainTest()`:

```java
        // Joint SAFE+SAFE with last=C,C -> cooperate
        if (cr.jointResponse(ClassifierResponderPlayer.CLASS_SAFE, ClassifierResponderPlayer.CLASS_SAFE, 0, 0) != 0) {
            failures++; System.out.println("FAIL joint SAFE+SAFE");
        }
        // Joint SAFE+HOSTILE -> defect
        if (cr.jointResponse(ClassifierResponderPlayer.CLASS_SAFE, ClassifierResponderPlayer.CLASS_HOSTILE, 0, 1) != 1) {
            failures++; System.out.println("FAIL joint SAFE+HOSTILE");
        }
        // Joint EXPLOITABLE + anything -> defect
        if (cr.jointResponse(ClassifierResponderPlayer.CLASS_EXPLOITABLE, ClassifierResponderPlayer.CLASS_SAFE, 0, 0) != 1) {
            failures++; System.out.println("FAIL joint EXPLOITABLE+SAFE");
        }
        // Joint AMBIGUOUS + AMBIGUOUS, opp1 last D -> defect (TFT-worse)
        if (cr.jointResponse(ClassifierResponderPlayer.CLASS_AMBIGUOUS, ClassifierResponderPlayer.CLASS_AMBIGUOUS, 1, 0) != 1) {
            failures++; System.out.println("FAIL joint AMBIG+AMBIG TFT-worse D");
        }
        // Joint AMBIGUOUS + AMBIGUOUS, both last C -> cooperate
        if (cr.jointResponse(ClassifierResponderPlayer.CLASS_AMBIGUOUS, ClassifierResponderPlayer.CLASS_AMBIGUOUS, 0, 0) != 0) {
            failures++; System.out.println("FAIL joint AMBIG+AMBIG TFT-worse C");
        }
        // Joint SAFE + AMBIGUOUS, ambig last D -> defect (TFT-worse)
        if (cr.jointResponse(ClassifierResponderPlayer.CLASS_SAFE, ClassifierResponderPlayer.CLASS_AMBIGUOUS, 0, 1) != 1) {
            failures++; System.out.println("FAIL joint SAFE+AMBIG D");
        }
        // Joint HOSTILE + HOSTILE -> defect
        if (cr.jointResponse(ClassifierResponderPlayer.CLASS_HOSTILE, ClassifierResponderPlayer.CLASS_HOSTILE, 1, 1) != 1) {
            failures++; System.out.println("FAIL joint HOSTILE+HOSTILE");
        }
        System.out.println("PASS jointResponse tests");
```

- [ ] **Step 2: Run test to verify it fails**

```bash
javac ThreePrisonersDilemma.java
```

Expected: `cannot find symbol ... method jointResponse(...)`.

- [ ] **Step 3: Implement `jointResponse` inside `ClassifierResponderPlayer`**

Add below `classify`:

```java
        /** Pessimistic joint response per spec §6. */
        int jointResponse(int c1, int c2, int opp1Last, int opp2Last) {
            // Defect if either opponent is HOSTILE or EXPLOITABLE
            if (c1 == CLASS_HOSTILE || c1 == CLASS_EXPLOITABLE
             || c2 == CLASS_HOSTILE || c2 == CLASS_EXPLOITABLE) {
                return 1;
            }
            // Cooperate if both are SAFE
            if (c1 == CLASS_SAFE && c2 == CLASS_SAFE) return 0;
            // Otherwise mirror the more aggressive opponent's last move (TFT-worse)
            return Math.max(opp1Last, opp2Last);
        }
```

- [ ] **Step 4: Run tests**

```bash
javac ThreePrisonersDilemma.java && java ThreePrisonersDilemma --test 2>&1 | grep -E "jointResponse|classify|aggregate|countUnprovoked|isProvoked|ClassifierResponder"
```

Expected all `PASS`.

- [ ] **Step 5: Commit**

```bash
git add ThreePrisonersDilemma.java
git commit -m "feat(submission): implement pessimistic jointResponse"
```

---

## Task 7: Wire `selectAction` with endgame override

**Files:**
- Modify: `ThreePrisonersDilemma.java` (replace stub `selectAction`; add end-to-end tests)

**Goal:** Replace the stub `selectAction` with the full pipeline: classify both opponents, check endgame override, then apply joint response.

- [ ] **Step 1: Write failing tests for end-to-end behaviour**

Append in `mainTest()`:

```java
        // End-to-end: round 0 returns C
        if (cr.selectAction(0, new int[0], new int[0], new int[0]) != 0) {
            failures++; System.out.println("FAIL selectAction r0");
        }
        // End-to-end: vs two peaceful cooperators at n=50 -> cooperate
        {
            int[] peaceful = new int[50]; // all zeros
            if (cr.selectAction(50, peaceful, peaceful, peaceful) != 0) {
                failures++; System.out.println("FAIL selectAction peaceful midgame");
            }
        }
        // End-to-end: endgame defection at n=109 against joint SAFE
        {
            int[] me       = new int[109];
            int[] o1Coop   = new int[109];
            int[] o2Coop   = new int[109];
            if (cr.selectAction(109, me, o1Coop, o2Coop) != 1) {
                failures++; System.out.println("FAIL selectAction endgame defect at n=109");
            }
        }
        // End-to-end: n=108 with joint SAFE -> still cooperate (no premature defect)
        {
            int[] me     = new int[108];
            int[] o1     = new int[108];
            int[] o2     = new int[108];
            if (cr.selectAction(108, me, o1, o2) != 0) {
                failures++; System.out.println("FAIL selectAction should NOT defect at n=108");
            }
        }
        // End-to-end: opp1 defected at round 0 -> HOSTILE -> we defect from round 1
        {
            int[] me = {0};
            int[] o1 = {1}; // HOSTILE via H1
            int[] o2 = {0};
            if (cr.selectAction(1, me, o1, o2) != 1) {
                failures++; System.out.println("FAIL selectAction defect vs round-0 defector");
            }
        }
        System.out.println("PASS selectAction end-to-end tests");
```

- [ ] **Step 2: Run test to verify the peaceful-midgame test fails**

```bash
javac ThreePrisonersDilemma.java && java ThreePrisonersDilemma --test 2>&1 | grep "selectAction"
```

Expected: at least `FAIL selectAction peaceful midgame` (stub currently returns 1 for any n > 0) and `FAIL selectAction should NOT defect at n=108`.

- [ ] **Step 3: Replace the stub `selectAction` with the full implementation**

Replace the existing `selectAction` body inside `ClassifierResponderPlayer`:

```java
        int selectAction(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2) {
            if (n == 0) return 0;

            int c1 = classify(n, oppHistory1, myHistory, oppHistory2);
            int c2 = classify(n, oppHistory2, myHistory, oppHistory1);

            // Endgame override: provably-final round (n == 109) with both opponents SAFE
            if (n == 109 && c1 == CLASS_SAFE && c2 == CLASS_SAFE) return 1;

            int opp1Last = oppHistory1[n - 1];
            int opp2Last = oppHistory2[n - 1];
            return jointResponse(c1, c2, opp1Last, opp2Last);
        }
```

- [ ] **Step 4: Run all tests**

```bash
javac ThreePrisonersDilemma.java && java ThreePrisonersDilemma --test 2>&1 | tail -40
```

Expected: all `PASS` lines for `ClassifierResponder`, `isProvoked`, `countUnprovoked`, `aggregate`, `classify`, `jointResponse`, and `selectAction end-to-end`. No `FAIL` lines for anything new.

- [ ] **Step 5: Commit**

```bash
git add ThreePrisonersDilemma.java
git commit -m "feat(submission): wire selectAction with endgame override"
```

---

## Task 8: Head-to-head sanity checks

**Files:**
- Modify: `ThreePrisonersDilemma.java` (add per-matchup sanity tests)

**Goal:** Confirm actual per-round payoffs against specific opponent triples match spec §9.2 predictions. This catches logic bugs the unit tests might miss (e.g., classifier oscillating between states).

- [ ] **Step 1: Write the sanity checks**

Append in `mainTest()`:

```java
        // ---- Head-to-head sanity via scoresOfMatch ----
        // Helper: expect the match avg (per round) to be near the predicted value.
        // We run exactly 110 rounds (the tournament max) for determinism — no random length here.
        {
            float[] r;
            // vs (Nice, Nice): expect ~6.02 (pure mutual-C plus +2 endgame bonus)
            r = inst.scoresOfMatch(cr, inst.new NicePlayer(), inst.new NicePlayer(), 110);
            if (r[0] < 5.99f || r[0] > 6.05f) {
                failures++;
                System.out.println("FAIL vs (Nice,Nice) avg=" + r[0]);
            } else {
                System.out.println("PASS vs (Nice,Nice) avg=" + r[0]);
            }

            // vs (Nasty, Nasty): expect ~2.0 (mutual-D from round 1)
            r = inst.scoresOfMatch(inst.new ClassifierResponderPlayer(),
                                    inst.new NastyPlayer(), inst.new NastyPlayer(), 110);
            if (r[0] < 1.95f || r[0] > 2.10f) {
                failures++;
                System.out.println("FAIL vs (Nasty,Nasty) avg=" + r[0]);
            } else {
                System.out.println("PASS vs (Nasty,Nasty) avg=" + r[0]);
            }

            // vs (T4T, T4T): expect >= 6.01 (mutual-C then endgame defect)
            r = inst.scoresOfMatch(inst.new ClassifierResponderPlayer(),
                                    inst.new T4TPlayer(), inst.new T4TPlayer(), 110);
            if (r[0] < 6.00f) {
                failures++;
                System.out.println("FAIL vs (T4T,T4T) avg=" + r[0]);
            } else {
                System.out.println("PASS vs (T4T,T4T) avg=" + r[0]);
            }

            // vs (TftSpiteful, TftSpiteful): expect >= 6.01
            r = inst.scoresOfMatch(inst.new ClassifierResponderPlayer(),
                                    inst.new TftSpitefulPlayer(), inst.new TftSpitefulPlayer(), 110);
            if (r[0] < 6.00f) {
                failures++;
                System.out.println("FAIL vs (TftSpiteful,TftSpiteful) avg=" + r[0]);
            } else {
                System.out.println("PASS vs (TftSpiteful,TftSpiteful) avg=" + r[0]);
            }

            // vs self: three ClassifierResponderPlayer -> all mutual-C then all defect at 109
            r = inst.scoresOfMatch(inst.new ClassifierResponderPlayer(),
                                    inst.new ClassifierResponderPlayer(),
                                    inst.new ClassifierResponderPlayer(), 110);
            if (r[0] < 5.95f || r[0] > 6.05f) {
                failures++;
                System.out.println("FAIL vs (self,self) avg=" + r[0]);
            } else {
                System.out.println("PASS vs (self,self) avg=" + r[0]);
            }
        }
```

- [ ] **Step 2: Run the sanity checks**

```bash
javac ThreePrisonersDilemma.java && java ThreePrisonersDilemma --test 2>&1 | grep "vs ("
```

Expected: all five `PASS` lines. Investigate any `FAIL` by:
1. Printing classifications at rounds 0, 5, 8, 50, 108, 109 for that matchup.
2. Comparing observed per-round payoffs to the payoff matrix (`payoff[me][opp1][opp2]`).

- [ ] **Step 3: Commit**

```bash
git add ThreePrisonersDilemma.java
git commit -m "test(submission): add head-to-head sanity checks"
```

---

## Task 9: Full tournament validation

**Files:** none modified — just a validation run.

**Goal:** Confirm the submission places in top-3 of the local 28-player zoo over 500 tournaments. If it does, we're done with performance work per the spec's "do-not-tune discipline."

- [ ] **Step 1: Temporarily bump `NUM_TOURNAMENTS` for a thorough run**

Edit `ThreePrisonersDilemma.java` line 1110 from:
```java
    static final int NUM_TOURNAMENTS = 100;
```
to:
```java
    static final int NUM_TOURNAMENTS = 500;
```

- [ ] **Step 2: Compile and run the full tournament**

```bash
javac ThreePrisonersDilemma.java && java ThreePrisonersDilemma > /tmp/pd_cr_tournament.txt 2>&1
tail -35 /tmp/pd_cr_tournament.txt
```

Expected output: a ranking table with 29 players. `ClassifierResponderPlayer` should appear in the top 3 rows with Avg Score ≥ 2193.

- [ ] **Step 3: Record the result**

If placement is top-3: proceed to Step 4.

If placement is rank 4+ or score < 2193: create a debug build that prints classifications for a sample matchup, diagnose the issue, fix, re-run. Do **not** tune thresholds to chase marginal gains — fix actual bugs only.

- [ ] **Step 4: Restore `NUM_TOURNAMENTS` to 100 (default)**

Revert line 1110 back to:
```java
    static final int NUM_TOURNAMENTS = 100;
```

The higher value was just for validation; the default stays 100 to keep `java ThreePrisonersDilemma` fast for graders.

- [ ] **Step 5: Commit (with tournament result in the message)**

```bash
# Example with actual observed values — fill in from /tmp/pd_cr_tournament.txt
git add ThreePrisonersDilemma.java
git commit -m "chore(submission): validate ClassifierResponder ranks top-3 at ~2194"
```

---

## Task 10: Rename class for submission and produce deliverables

**Files:**
- Create: `Wong_Zhunhao_Player.java` (standalone submission file — adjust name to match your actual lastname/firstname)
- The `ThreePrisonersDilemma.java` changes stay in place for your own testing; the **submission** is the standalone file only.

**Goal:** Per assignment spec (`Intelligent_Agents_Assignment.md`): the submission must be a **single class** named `lastname_firstname_Player` (adjust delimiter to match the grader's format; the PDF example uses spaces in the filename but Java doesn't allow spaces in class names — use underscores in both).

The submitted class extends `Player` and implements `selectAction`. It cannot be an inner class in the submission (it has to be a top-level class that the grader drops into their harness).

**Note on naming:** The assignment PDF shows the form `lastname firstname Player`. Confirm with the course instructor whether underscores or another delimiter is required before submission. The filename below uses underscores as the safest assumption.

- [ ] **Step 1: Create the standalone submission file**

Create `Wong_Zhunhao_Player.java` (replace with your actual lastname/firstname). The file must compile when dropped into the grader's harness alongside `ThreePrisonersDilemma.java` — which means it needs to be a top-level class (not nested), extend the grader's `Player` abstract class (inner class of `ThreePrisonersDilemma`), and implement `selectAction`.

```java
/*
 * SC4003/CZ4046/CE4046 Assignment 2 — 3-Player Iterated Prisoner's Dilemma
 *
 * Strategy: Classifier-Responder.
 *   Passively observes each opponent, classifies into one of four classes
 *   {AMBIGUOUS, SAFE, EXPLOITABLE, HOSTILE}, emits one action per round
 *   via a pessimistic joint-response rule. Defects on the provably-final
 *   round (n == 109) when both opponents are SAFE.
 *
 * See accompanying report for design rationale.
 */
class Wong_Zhunhao_Player extends ThreePrisonersDilemma.Player {
    static final int CLASS_AMBIGUOUS = 0;
    static final int CLASS_SAFE = 1;
    static final int CLASS_EXPLOITABLE = 2;
    static final int CLASS_HOSTILE = 3;

    boolean isProvoked(int r, int[] h1, int[] h2, int[] h3) {
        if (r <= 0) return false;
        int lo = Math.max(0, r - 2);
        for (int i = lo; i < r; i++) {
            if (h1[i] == 1 || h2[i] == 1 || h3[i] == 1) return true;
        }
        return false;
    }

    int countUnprovokedDefects(int[] opp, int[] me, int[] other) {
        int count = 0;
        for (int r = 0; r < opp.length; r++) {
            if (opp[r] == 1 && !isProvoked(r, opp, me, other)) count++;
        }
        return count;
    }

    int countUnprovokedDefectsInWindow(int[] opp, int[] me, int[] other, int lo, int hi) {
        int count = 0;
        int end = Math.min(opp.length - 1, hi);
        for (int r = Math.max(0, lo); r <= end; r++) {
            if (opp[r] == 1 && !isProvoked(r, opp, me, other)) count++;
        }
        return count;
    }

    int countDefects(int[] h) {
        int c = 0;
        for (int v : h) if (v == 1) c++;
        return c;
    }

    int totalDefectsInMatch(int[] h1, int[] h2, int[] h3) {
        return countDefects(h1) + countDefects(h2) + countDefects(h3);
    }

    int roundsSinceAnyDefect(int n, int[] h1, int[] h2, int[] h3) {
        for (int r = n - 1; r >= 0; r--) {
            if (h1[r] == 1 || h2[r] == 1 || h3[r] == 1) return n - r;
        }
        return Integer.MAX_VALUE;
    }

    int classify(int n, int[] opp, int[] me, int[] other) {
        if (n == 0) return CLASS_AMBIGUOUS;
        if (opp.length > 0 && opp[0] == 1) return CLASS_HOSTILE;              // H1
        if (countUnprovokedDefects(opp, me, other) >= 2) return CLASS_HOSTILE; // H2
        if (countUnprovokedDefectsInWindow(opp, me, other, 0, 10) >= 1)
            return CLASS_HOSTILE;                                              // H3

        int totalDefects = totalDefectsInMatch(opp, me, other);
        int rsad = roundsSinceAnyDefect(n, opp, me, other);
        if (totalDefects >= 1 && rsad >= 5 && countDefects(opp) == 0)
            return CLASS_EXPLOITABLE;                                          // E1

        if (n >= 20) {
            boolean recentOppDefect = false;
            boolean recentOtherDefect = false;
            for (int r = n - 20; r < n; r++) {
                if (opp[r] == 1) recentOppDefect = true;
                if (me[r] == 1 || other[r] == 1) recentOtherDefect = true;
            }
            if (!recentOppDefect && recentOtherDefect) return CLASS_EXPLOITABLE; // E2
        }

        boolean allProvoked = true;
        int lastOppDefect = -1;
        for (int r = 0; r < opp.length; r++) {
            if (opp[r] == 1) {
                lastOppDefect = r;
                if (!isProvoked(r, opp, me, other)) { allProvoked = false; break; }
            }
        }
        if (allProvoked && (lastOppDefect == -1 || n - lastOppDefect >= 3)) {
            if (countDefects(opp) > 0) return CLASS_SAFE;                      // S1
        }

        if (countDefects(opp) == 0 && n >= 8) return CLASS_SAFE;              // S2
        return CLASS_AMBIGUOUS;
    }

    int jointResponse(int c1, int c2, int opp1Last, int opp2Last) {
        if (c1 == CLASS_HOSTILE || c1 == CLASS_EXPLOITABLE
         || c2 == CLASS_HOSTILE || c2 == CLASS_EXPLOITABLE) return 1;
        if (c1 == CLASS_SAFE && c2 == CLASS_SAFE) return 0;
        return Math.max(opp1Last, opp2Last);
    }

    public int selectAction(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2) {
        if (n == 0) return 0;
        int c1 = classify(n, oppHistory1, myHistory, oppHistory2);
        int c2 = classify(n, oppHistory2, myHistory, oppHistory1);
        if (n == 109 && c1 == CLASS_SAFE && c2 == CLASS_SAFE) return 1;
        int opp1Last = oppHistory1[n - 1];
        int opp2Last = oppHistory2[n - 1];
        return jointResponse(c1, c2, opp1Last, opp2Last);
    }
}
```

**⚠ Compatibility note:** The grader's harness expects `Player` to be an inner class of `ThreePrisonersDilemma`. The `extends ThreePrisonersDilemma.Player` form above may or may not work depending on how they run it — verify with a test compile in the grader's environment if possible. If not, the standalone file can instead use the exact same body as the inner class in `ThreePrisonersDilemma.java`; the grader will drop it in as an inner class.

**Simpler alternative (recommended):** Since the assignment says "We will run the players against each other... the tournaments will be run automatically based on this signature," the grader almost certainly pastes your class directly into their own `ThreePrisonersDilemma.java`. Submit the class body WITHOUT the `extends ThreePrisonersDilemma.Player` qualifier — just `extends Player` — matching the inner-class style of the other players.

- [ ] **Step 2: Compile the standalone file against `ThreePrisonersDilemma.java`**

```bash
javac ThreePrisonersDilemma.java Wong_Zhunhao_Player.java
```

If this compiles without errors, the submission is self-contained and drop-in.

If compilation fails with `Player is not accessible`, the simpler alternative above is needed — make the submission an inner class by copying the body into the grader's file, or use the fully-qualified inner-class reference in the extends clause.

- [ ] **Step 3: Smoke-test the standalone class via a short tournament**

Not strictly required, but a quick confidence check:

```bash
java ThreePrisonersDilemma 2>&1 | grep Wong_Zhunhao
```

Should appear in the ranking table alongside `ClassifierResponderPlayer`, with similar scores (they are the same strategy).

- [ ] **Step 4: Write the report**

The report is 30% of the grade and must explain the design. Use spec §10 ("Report Outline") as your section-by-section template. Mandatory contents:

1. Problem setup and payoff matrix
2. Design principle (classify-then-respond, Axelrod's four properties)
3. Why classifier over pure TFT/Grim — cite your zoo results showing TftSpiteful at 2191 and your classifier above it
4. Why passive over active probing
5. Why pessimistic joint rule
6. Classification rules with per-rule rationale (H1/H2/H3, E1/E2, S1/S2)
7. Endgame defection math (include the P(last|n) table from spec §7)
8. Evaluation — head-to-head table vs every zoo player, plus 500-tournament aggregate
9. Limitations — non-monotonic SAFE, no noise adaptation, unable to distinguish ALLC from TFT

Save as `Wong_Zhunhao_Player.pdf` (adjust name). Reference the spec file for exact wording where helpful.

- [ ] **Step 5: Final commit**

```bash
git add Wong_Zhunhao_Player.java docs/
git commit -m "feat(submission): add standalone submission file and report"
```

---

## Self-Review Checklist

Before claiming this plan complete, verify:

**Spec coverage:**
- [x] §5 classifier rules (H1, H2, H3, E1, E2, S1, S2) — Task 5
- [x] §6 joint response table — Task 6
- [x] §7 endgame override — Task 7
- [x] §8 edge cases — covered across Task 7 tests (n=0, peaceful midgame, round-0 defector)
- [x] §9.1 unit tests — Tasks 2–7
- [x] §9.2 head-to-head — Task 8
- [x] §9.3 full tournament — Task 9
- [x] §10 report — Task 10 Step 4

**Placeholder scan:** No "TBD", no "fill in", no "similar to earlier task" — every code step contains the actual code.

**Type/signature consistency:**
- `classify(n, opp, me, other)` — used consistently in Tasks 5, 7, 10.
- `jointResponse(c1, c2, opp1Last, opp2Last)` — consistent in Tasks 6, 7, 10.
- `isProvoked(r, h1, h2, h3)` — consistent across all callers.
- Class constants `CLASS_*` — defined in Task 1, used from Task 5 onwards.

**Remaining risks / things to watch during execution:**
- E2's `n >= 20` check accesses `opp[r]`, `me[r]`, `other[r]` for `r = n-20..n-1`. All three arrays have length `n` at call time, so the indexing is in-bounds. Verify this during Task 5.
- The standalone submission file (Task 10) makes an assumption about how the grader compiles student submissions. The "simpler alternative" inner-class body is the safer fallback.
