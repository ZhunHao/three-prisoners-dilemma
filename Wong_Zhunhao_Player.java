/*
 * SC4003/CZ4046/CE4046 Assignment 2 — 3-Player Iterated Prisoner's Dilemma
 * Author: Wong Zhun Hao
 *
 * Strategy: Classifier-Responder.
 *   Passively observes each opponent, classifies into one of four classes
 *   {AMBIGUOUS, SAFE, EXPLOITABLE, HOSTILE} based on the observed history,
 *   then emits one action per round using a pessimistic joint-response rule.
 *   Defects on the provably-final round (n == 109) when both opponents are
 *   classified SAFE — an optimal backward-induction move at the known-terminal
 *   round. See accompanying report for full design rationale.
 *
 * This class is intended to be pasted as an inner class inside
 * ThreePrisonersDilemma. It extends the harness's Player inner class.
 */

/*
 * SUBMISSION FORMAT NOTE
 * This file contains the class body in inner-class form (using `extends Player`).
 * The grader is expected to paste this class inside their own ThreePrisonersDilemma.java.
 *
 * To TEST the strategy in the local harness: see the equivalent inner class
 * `ClassifierResponderPlayer` already registered in ThreePrisonersDilemma.java
 * (case 28 in makePlayer()). The two classes have identical behavior.
 *
 * Attempting to compile this file STANDALONE (`javac Wong_Zhunhao_Player.java`)
 * will fail with "no enclosing instance of type ThreePrisonersDilemma" — this is
 * expected. The file is intended for drop-in inclusion, not standalone compilation.
 */
class Wong_Zhunhao_Player extends Player {
    static final int CLASS_AMBIGUOUS = 0;
    static final int CLASS_SAFE = 1;
    static final int CLASS_EXPLOITABLE = 2;
    static final int CLASS_HOSTILE = 3;

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
     *  If last defect was at round r, returns n - r. If no defects, returns Integer.MAX_VALUE. */
    int roundsSinceAnyDefect(int n, int[] h1, int[] h2, int[] h3) {
        for (int r = n - 1; r >= 0; r--) {
            if (h1[r] == 1 || h2[r] == 1 || h3[r] == 1) return n - r;
        }
        return Integer.MAX_VALUE;
    }

    /** Classify one opponent based on observed history. */
    int classify(int n, int[] opp, int[] me, int[] other) {
        if (n == 0) return CLASS_AMBIGUOUS;

        // H1: defection at round 0 is always unprovoked
        if (opp.length > 0 && opp[0] == 1) return CLASS_HOSTILE;
        // H2: two or more unprovoked defections anywhere
        if (countUnprovokedDefects(opp, me, other) >= 2) return CLASS_HOSTILE;
        // H3: any unprovoked defection in the 0..10 window (history-monotonic)
        if (countUnprovokedDefectsInWindow(opp, me, other, 0, 10) >= 1) return CLASS_HOSTILE;

        // E1: opp has 0 defects, someone else defected, quiet for 5+ rounds
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

        // S1: all opp's defections were provoked, last one was >= 3 rounds ago
        boolean allProvoked = true;
        int lastOppDefect = -1;
        for (int r = 0; r < opp.length; r++) {
            if (opp[r] == 1) {
                lastOppDefect = r;
                if (!isProvoked(r, opp, me, other)) { allProvoked = false; break; }
            }
        }
        if (allProvoked && (lastOppDefect == -1 || n - lastOppDefect >= 3)) {
            if (countDefects(opp) > 0) return CLASS_SAFE;
        }

        // S2: 0 defections and n >= 8
        if (countDefects(opp) == 0 && n >= 8) return CLASS_SAFE;

        return CLASS_AMBIGUOUS;
    }

    /** Pessimistic joint response. */
    int jointResponse(int c1, int c2, int opp1Last, int opp2Last) {
        if (c1 == CLASS_HOSTILE || c1 == CLASS_EXPLOITABLE
         || c2 == CLASS_HOSTILE || c2 == CLASS_EXPLOITABLE) return 1;
        if (c1 == CLASS_SAFE && c2 == CLASS_SAFE) return 0;
        return Math.max(opp1Last, opp2Last);
    }

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
}
