import java.util.Arrays;

public class ThreePrisonersDilemma {

    /*
     * This Java program models the two-player Prisoner's Dilemma game.
     * We use the integer "0" to represent cooperation, and "1" to represent
     * defection.
     * 
     * Recall that in the 2-players dilemma, U(DC) > U(CC) > U(DD) > U(CD), where
     * we give the payoff for the first player in the list. We want the three-player
     * game
     * to resemble the 2-player game whenever one player's response is fixed, and we
     * also want symmetry, so U(CCD) = U(CDC) etc. This gives the unique ordering
     * 
     * U(DCC) > U(CCC) > U(DDC) > U(CDC) > U(DDD) > U(CDD)
     * 
     * The payoffs for player 1 are given by the following matrix:
     */

    static int[][][] payoff = {
            { { 6, 3 }, // payoffs when first and second players cooperate
                    { 3, 0 } }, // payoffs when first player coops, second defects
            { { 8, 5 }, // payoffs when first player defects, second coops
                    { 5, 2 } } };// payoffs when first and second players defect

    /*
     * So payoff[i][j][k] represents the payoff to player 1 when the first
     * player's action is i, the second player's action is j, and the
     * third player's action is k.
     * 
     * In this simulation, triples of players will play each other repeatedly in a
     * 'match'. A match consists of about 100 rounds, and your score from that match
     * is the average of the payoffs from each round of that match. For each round,
     * your
     * strategy is given a list of the previous plays (so you can remember what your
     * opponent did) and must compute the next action.
     */

    abstract class Player {
        // This procedure takes in the number of rounds elapsed so far (n), and
        // the previous plays in the match, and returns the appropriate action.
        int selectAction(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2) {
            throw new RuntimeException("You need to override the selectAction method.");
        }

        // Used to extract the name of this player class.
        final String name() {
            String result = getClass().getName();
            return result.substring(result.indexOf('$') + 1);
        }

        // True iff either opponent defected in round r.
        final boolean anyDefected(int r, int[] opp1, int[] opp2) {
            return opp1[r] == 1 || opp2[r] == 1;
        }

        // Fraction of rounds 0..n-1 where hist[] == 0 (cooperation). Returns 1.0 when n==0.
        final double coopRate(int[] hist, int n) {
            int coops = 0;
            for (int i = 0; i < n; i++) if (hist[i] == 0) coops++;
            return n == 0 ? 1.0 : (double) coops / n;
        }
    }

    /* Here are four simple strategies: */

    class NicePlayer extends Player {
        // NicePlayer always cooperates
        int selectAction(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2) {
            return 0;
        }
    }

    class NastyPlayer extends Player {
        // NastyPlayer always defects
        int selectAction(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2) {
            return 1;
        }
    }

    class RandomPlayer extends Player {
        // RandomPlayer randomly picks his action each time
        int selectAction(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2) {
            if (Math.random() < 0.5)
                return 0; // cooperates half the time
            else
                return 1; // defects half the time
        }
    }

    class TolerantPlayer extends Player {
        // TolerantPlayer looks at his opponents' histories, and only defects
        // if at least half of the other players' actions have been defects
        int selectAction(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2) {
            int opponentCoop = 0;
            int opponentDefect = 0;
            for (int i = 0; i < n; i++) {
                if (oppHistory1[i] == 0)
                    opponentCoop = opponentCoop + 1;
                else
                    opponentDefect = opponentDefect + 1;
            }
            for (int i = 0; i < n; i++) {
                if (oppHistory2[i] == 0)
                    opponentCoop = opponentCoop + 1;
                else
                    opponentDefect = opponentDefect + 1;
            }
            if (opponentDefect > opponentCoop)
                return 1;
            else
                return 0;
        }
    }

    class FreakyPlayer extends Player {
        // FreakyPlayer determines, at the start of the match,
        // either to always be nice or always be nasty.
        // Note that this class has a non-trivial constructor.
        int action;

        FreakyPlayer() {
            if (Math.random() < 0.5)
                action = 0; // cooperates half the time
            else
                action = 1; // defects half the time
        }

        int selectAction(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2) {
            return action;
        }
    }

    class T4TPlayer extends Player {
        // Canonical 3-player TFT: copy opponents' last moves under set-union
        // retaliation. Cooperate iff BOTH opponents cooperated last round.
        int selectAction(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2) {
            if (n == 0)
                return 0;
            if (anyDefected(n - 1, oppHistory1, oppHistory2))
                return 1;
            return 0;
        }
    }

    // =========================================================
    // STRATEGY 1: Generous Tit-for-Tat (GTFT) — Nowak & Sigmund 1993
    // =========================================================
    /*
     * Canonical GTFT adapted for 3 players.
     *
     * - Round 0: cooperate.
     * - If EITHER opponent defected last round, retaliate with probability 1-q,
     *   forgive with probability q = 0.1.
     * - Designed to escape mutual-defection spirals under noise without being
     *   exploitable by persistent defectors.
     */
    class GenerousTfTPlayer extends Player {
        final double q = 0.1;

        int selectAction(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2) {
            if (n == 0)
                return 0;

            if (anyDefected(n - 1, oppHistory1, oppHistory2)) {
                if (Math.random() < q)
                    return 0; // forgive
                return 1;     // retaliate
            }
            return 0;
        }
    }

    // =========================================================
    // STRATEGY 2: Soft Majority (soft_majo) — Mathieu & Delahaye JASSS 2017
    // =========================================================
    /*
     * Canonical per-opponent soft majority, adapted for 3 players by requiring
     * the soft_majo condition to hold for BOTH opponents independently.
     *
     * - Round 0: cooperate.
     * - For each opponent, check if their cooperation count >= defection count
     *   over full history. Cooperate only if both pass; otherwise defect.
     * - Ties go to cooperate (consistent with soft_majo definition).
     */
    class MajorityRulePlayer extends Player {
        int selectAction(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2) {
            if (n == 0)
                return 0;

            int op1Coop = 0, op1Def = 0;
            int op2Coop = 0, op2Def = 0;
            for (int i = 0; i < n; i++) {
                if (oppHistory1[i] == 0) op1Coop++; else op1Def++;
                if (oppHistory2[i] == 0) op2Coop++; else op2Def++;
            }

            boolean op1Ok = op1Coop >= op1Def;
            boolean op2Ok = op2Coop >= op2Def;
            return (op1Ok && op2Ok) ? 0 : 1;
        }
    }

    // =========================================================
    // STRATEGY 3: Gradual Punisher — Beaufils-Delahaye-Mathieu 1996
    // =========================================================
    /*
     * Canonical Gradual adapted for 3 players.
     *
     * - Round 0: cooperate.
     * - Count `defectionEvents` = number of prior rounds where at least one
     *   opponent defected.
     * - On the k-th event, defect for k rounds total (this round is round 1),
     *   then emit 2 cooperation ("peace") rounds.
     * - During punishment/calm, ignore the latest round's actions.
     *
     * State: punishRemaining, calmRemaining, defectionEvents.
     */
    class GradualPunisherPlayer extends Player {
        int punishRemaining = 0;
        int calmRemaining = 0;
        int defectionEvents = 0;

        int selectAction(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2) {
            if (n == 0)
                return 0;

            if (calmRemaining > 0) {
                calmRemaining--;
                return 0;
            }

            if (punishRemaining > 0) {
                punishRemaining--;
                if (punishRemaining == 0)
                    calmRemaining = 2;
                return 1;
            }

            if (anyDefected(n - 1, oppHistory1, oppHistory2)) {
                defectionEvents++;
                // This round is the first of `defectionEvents` punish rounds.
                punishRemaining = defectionEvents - 1;
                if (punishRemaining == 0)
                    calmRemaining = 2;
                return 1;
            }

            return 0;
        }
    }

    // =========================================================
    // STRATEGY 4: Equalizer ZD Player (from Taha & Ghoneim 2021)
    // =========================================================
    /*
     * Based on the Zero-Determinant (ZD) equalizer strategy derived in:
     * Taha & Ghoneim (2021), "Zero-determinant strategies in infinitely repeated
     * three-player prisoner's dilemma game", Chaos Solitons & Fractals.
     *
     * Key idea:
     * - A ZD equalizer player can unilaterally PIN the combined expected payoff
     * of both opponents to a fixed value, regardless of what they do.
     * - The strategy is a memory-one stochastic strategy: cooperation probability
     * depends only on what happened in the LAST round.
     *
     * Payoff values from our game: T=8, R=6, L=5, K=3, P=2, S=0
     * From the paper, the equalizer strategy is parameterised by pC2 and pD0,
     * where pC2 = prob. to cooperate after (C,C,C) and pD0 = prob. after (D,D,D).
     *
     * We use pC2 = 0.9, pD0 = 0.2 (within valid bounds derived in the paper).
     * This pins E2+E3 to approximately 2*R*pD0 + 2*P*(1-pC2) / (pD0 + 1-pC2).
     *
     * The 6 cooperation probabilities based on last-round state:
     * pCC = prob cooperate after both opponents cooperated
     * pCD = prob cooperate after one opponent cooperated, one defected
     * pDD = prob cooperate after both opponents defected
     * dCC = prob cooperate after I defected, both cooperated
     * dCD = prob cooperate after I defected, one cooperated one defected
     * dDD = prob cooperate after I defected, both defected (= pD0)
     */
    class EqualizerZDPlayer extends Player {

        // Payoff values: T=8, R=6, L=5, K=3, P=2, S=0
        final double T = 8, R = 6, L = 5, K = 3, P = 2, S = 0;

        // Free parameters (chosen within valid bounds from the paper)
        final double pC2 = 0.9; // cooperate prob after CCC
        final double pD0 = 0.2; // cooperate prob after DDD

        // Derived probabilities from equalizer ZD formulas (Eq. 20 in paper)
        double pC1, pC0, dC2, dC1;

        EqualizerZDPlayer() {
            // Derived probabilities (Taha & Ghoneim 2021, Eq. 20).
            // With pC2=0.9, pD0=0.2 and our payoffs, every derived value is in [0,1].
            // Assert rather than clamp so parameter misuse fails loudly.
            pC1 = ((pD0 + 1) * (2 * R - K - T) + pC2 * (K + T - 2 * P)) / (2 * (R - P));
            pC0 = 1 + ((R - L) * pD0 + (L - P) * (pC2 - 1)) / (R - P);
            dC2 = ((R - K) * pD0 + (pC2 - 1) * (K - P)) / (R - P);
            dC1 = ((2 * R - S - L) * pD0 + (pC2 - 1) * (S + L - 2 * P)) / (2 * (R - P));

            double[] probs = { pC2, pC1, pC0, dC2, dC1, pD0 };
            String[] names = { "pC2", "pC1", "pC0", "dC2", "dC1", "pD0" };
            for (int i = 0; i < probs.length; i++) {
                if (probs[i] < 0.0 || probs[i] > 1.0)
                    throw new IllegalStateException(
                            "EqualizerZD parameter " + names[i] + " out of [0,1]: " + probs[i]);
            }
        }

        int selectAction(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2) {
            // Round 0: cooperate
            if (n == 0)
                return 0;

            int myLast = myHistory[n - 1];
            int opp1Last = oppHistory1[n - 1];
            int opp2Last = oppHistory2[n - 1];

            // Count cooperators among opponents last round
            int oppCoops = (1 - opp1Last) + (1 - opp2Last); // 0, 1, or 2

            double prob;
            if (myLast == 0) {
                // I cooperated last round
                if (oppCoops == 2)
                    prob = pC2; // both cooperated: CCC
                else if (oppCoops == 1)
                    prob = pC1; // one coop, one defect: CCD/CDC
                else
                    prob = pC0; // both defected: CDD
            } else {
                // I defected last round
                if (oppCoops == 2)
                    prob = dC2; // both cooperated: DCC
                else if (oppCoops == 1)
                    prob = dC1; // one coop, one defect: DCD/DDC
                else
                    prob = pD0; // both defected: DDD
            }

            return (Math.random() < prob) ? 0 : 1;
        }
    }

    // =========================================================
    // STRATEGY 5: Extortion ZD Player (from Taha & Ghoneim 2021)
    // =========================================================
    /*
     * Based on the Zero-Determinant (ZD) extortion strategy derived in:
     * Taha & Ghoneim (2021), same paper as above.
     *
     * Key idea:
     * - An extortion ZD player enforces a relationship where their own payoff
     * is always proportionally HIGHER than opponents' payoffs above mutual
     * defection.
     * - Specifically: E1 - P >= chi * (E2 + E3 - 2P) for extortion factor chi >
     * 0.5.
     * - Opponents can only improve their payoff by helping the extorter earn more.
     *
     * Parameters (from paper Eq. 24-26, with our payoff values
     * T=8,R=6,L=5,K=3,P=2,S=0):
     * chi = 1.0 (extortion factor, must be in (0.5, min((T-P)/2(K-P),
     * (L-P)/(L+S-2P))))
     * phi = 0.1 (scaling factor, must be in (0, upper bound from Eq.25))
     *
     * The cooperation probabilities are fully determined by chi and phi.
     * pD0 is always 0 in the extortion strategy (never cooperate after DDD).
     */
    class ExtortionZDPlayer extends Player {

        // Payoff values: T=8, R=6, L=5, K=3, P=2, S=0
        final double T = 8, R = 6, L = 5, K = 3, P = 2, S = 0;

        // Extortion parameters (within valid bounds from paper Eq. 25-26)
        final double chi = 1.0; // extortion factor
        final double phi = 0.1; // scaling factor

        // Cooperation probabilities derived from Eq. 24 of the paper
        double pC2, pC1, pC0, dC2, dC1;
        final double pD0 = 0.0; // always 0 for extortion strategy

        ExtortionZDPlayer() {
            // Validate chi bounds first (Taha & Ghoneim 2021, Eq. 25-26).
            double chiMax = Math.min((T - P) / (2 * (K - P)), (L - P) / (L + S - 2 * P));
            if (!(chi > 0.5 && chi < chiMax))
                throw new IllegalStateException(
                        "Extortion chi out of (0.5, " + chiMax + "): " + chi);

            // Derived probabilities (Eq. 24).
            pC2 = 1 - phi * (R - P) * (2 * chi - 1);
            pC1 = 1 - phi * (chi * (K + T - 2 * P) - (K - P));
            dC2 = phi * ((T - P) - 2 * chi * (K - P));
            pC0 = 1 - phi * ((P - S) + 2 * chi * (L - P));
            dC1 = phi * ((L - P) - chi * (S + L - 2 * P));

            double[] probs = { pC2, pC1, pC0, dC2, dC1, pD0 };
            String[] names = { "pC2", "pC1", "pC0", "dC2", "dC1", "pD0" };
            for (int i = 0; i < probs.length; i++) {
                if (probs[i] < 0.0 || probs[i] > 1.0)
                    throw new IllegalStateException(
                            "ExtortionZD parameter " + names[i] + " out of [0,1]: " + probs[i]);
            }
        }

        int selectAction(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2) {
            // Round 0: cooperate
            if (n == 0)
                return 0;

            int myLast = myHistory[n - 1];
            int opp1Last = oppHistory1[n - 1];
            int opp2Last = oppHistory2[n - 1];

            // Count cooperators among opponents last round
            int oppCoops = (1 - opp1Last) + (1 - opp2Last); // 0, 1, or 2

            double prob;
            if (myLast == 0) {
                // I cooperated last round
                if (oppCoops == 2)
                    prob = pC2; // CCC
                else if (oppCoops == 1)
                    prob = pC1; // CCD or CDC
                else
                    prob = pC0; // CDD
            } else {
                // I defected last round
                if (oppCoops == 2)
                    prob = dC2; // DCC
                else if (oppCoops == 1)
                    prob = dC1; // DCD or DDC
                else
                    prob = pD0; // DDD (always 0 = defect)
            }

            return (Math.random() < prob) ? 0 : 1;
        }
    }

    // =========================================================
    // STRATEGY 6: Asylum Player (Inverted Gradual Punisher)
    // =========================================================
    /*
     * A mirror-image of the Gradual Punisher strategy.
     *
     * Key ideas:
     * - Starts by DEFECTING (opposite of Gradual Punisher).
     * - Tracks how many times each opponent has COOPERATED (not defected).
     * - When opponents cooperate, enters a "punishment" phase where it defects
     * for a number of rounds equal to total cooperations seen.
     * - After punishment, "resets" with 2 rounds of COOPERATION to lure
     * opponents back into cooperating (so it can punish again).
     * - When opponents defect, it cooperates — rewarding defection.
     *
     * State variables:
     * - punishCount: how many punishment (defection) rounds remain.
     * - calmCount: how many calm (cooperation) rounds remain after punishment.
     *
     * Expected performance:
     * - vs NicePlayer: punishes heavily since NicePlayer always cooperates.
     * - vs NastyPlayer: cooperates always since NastyPlayer always defects.
     * - vs RandomPlayer: mixed behaviour — punishes cooperative streaks.
     * - An "insane" strategy that inverts the logic of rational play.
     */
    class AsylumPlayer extends Player {
        int punishCount = 0; // remaining rounds of punishment (defection)
        int calmCount = 0; // remaining rounds of calm (cooperation after punishment)

        int selectAction(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2) {
            // Round 0: always defect (opposite of Gradual Punisher)
            if (n == 0)
                return 1;

            // If in calm phase after punishment, cooperate (to lure opponents)
            if (calmCount > 0) {
                calmCount--;
                return 0;
            }

            // If in punishment phase, keep defecting
            if (punishCount > 0) {
                punishCount--;
                // After punishment ends, signal peace with 2 calm rounds
                if (punishCount == 0)
                    calmCount = 2;
                return 1;
            }

            // Count cooperations (not defections) from last round
            int newCooperations = 0;
            if (oppHistory1[n - 1] == 0)
                newCooperations++; // opponent cooperated
            if (oppHistory2[n - 1] == 0)
                newCooperations++; // opponent cooperated

            // If any cooperation occurred, trigger punishment (defect)
            if (newCooperations > 0) {
                // Count total historical cooperations across both opponents
                int totalCooperations = 0;
                for (int i = 0; i < n; i++) {
                    if (oppHistory1[i] == 0)
                        totalCooperations++;
                    if (oppHistory2[i] == 0)
                        totalCooperations++;
                }
                // Punish for as many rounds as total cooperations seen so far
                punishCount = totalCooperations - 1; // -1 because we defect this round
                if (punishCount == 0)
                    calmCount = 2;
                return 1;
            }

            // Both opponents defected last round -> cooperate (reward defection)
            return 0;
        }
    }

    // =========================================================
    // STRATEGY 7: Hallucination Player
    // =========================================================
    /*
     * HallucinationPlayer is disconnected from the present reality.
     * 
     * Key ideas:
     * - Instead of looking at the last round to decide its next move, it randomly
     * picks ANY previous round in the match and "hallucinates" that it was the last
     * round.
     * - It then plays a standard Tit-for-Tat against that hallucinated reality:
     * If either opponent defected in that randomly chosen past round, it defects.
     * Otherwise, it cooperates.
     *
     * Expected performance:
     * - vs NicePlayer: always cooperates (since ALL past rounds are cooperation).
     * - vs NastyPlayer: always defects after round 0.
     * - vs T4T and other reactive strategies: introduces massive noise and chaos.
     * It acts like a player with sporadic amnesia/hallucinations, breaking
     * fragile cooperative patterns.
     */
    class HallucinationPlayer extends Player {
        int selectAction(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2) {
            // Round 0: always cooperate initially
            if (n == 0)
                return 0;

            // Hallucinate that a random past round is the current context
            int hallucinatedRound = (int) (Math.random() * n);

            int perceivedOpp1 = oppHistory1[hallucinatedRound];
            int perceivedOpp2 = oppHistory2[hallucinatedRound];

            // React to the hallucinated reality: defect if either defected
            if (perceivedOpp1 == 1 || perceivedOpp2 == 1) {
                return 1;
            }
            return 0;
        }
    }

    // =========================================================
    // STRATEGY 8: Drunken Player
    // =========================================================
    /*
     * DrunkenPlayer starts off as a Gradual Punisher, but over time it gets "drunk"
     * and acts more and more randomly. The probability of acting randomly increases
     * linearly with the round number.
     * 
     * When it "blacks out" (acts randomly), it also bypasses its normal state
     * updates,
     * meaning it might completely ignore defections that happen while it's drunk,
     * or forget to count down its punishment/calm phases.
     */
    class DrunkenPlayer extends GradualPunisherPlayer {
        int selectAction(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2) {
            // Probability of random action increases from 0% at n=0 to ~80% at n>=80
            double drunkChance = Math.min(0.8, n / 100.0);
            if (Math.random() < drunkChance) {
                return (Math.random() < 0.5) ? 0 : 1;
            }
            // When sober enough, act like a normal Gradual Punisher
            return super.selectAction(n, myHistory, oppHistory1, oppHistory2);
        }
    }

    // =========================================================
    // STRATEGY 9: Evolved ANN — Axelrod-Python inspired (Harper et al. 2017)
    // =========================================================
    /*
     * Canonical Evolved ANN in the family of Axelrod-Python's `EvolvedANN`.
     *
     * Architecture: a single-hidden-layer feed-forward network.
     *   - Inputs (7): normalized round, self/opp1/opp2 cooperation rates, and
     *     the three players' last actions.
     *   - Hidden layer: 5 sigmoid units.
     *   - Output: 1 sigmoid unit. prob >= 0.5 => defect; otherwise cooperate.
     *
     * Weights were fit offline via an evolutionary loop (axelrod-dojo style)
     * against a mixed opponent pool. The flat weight vector is laid out as
     * W1 (7x5) then b1 (5) then W2 (5) then b2 (1), for 46 total parameters.
     */
    class EvolvedANNPlayer extends Player {
        // Flat weight vector: 7*5 (W1) + 5 (b1) + 5 (W2) + 1 (b2) = 46 floats.
        static final double[] CHAMPION_WEIGHTS = {
                -0.0283, -2.1069, -1.3460, -0.4087, -0.0387, -1.0510, 0.3381, -1.6319, 0.2295, 0.9876,
                0.6254, -0.6738, -1.8125, -0.3809, -0.4621, -0.8069, 1.5765, 0.6437, -0.1090, 0.0904,
                -0.7449, -1.5441, -1.4230, -0.3713, -2.1603, -0.7364, -1.4281, 0.6809, 0.0731, -1.4301,
                0.3928, -2.0727, -0.5932, -1.0264, -1.3548, 2.4783, -0.6026, -1.5292, -0.8584, 0.4174,
                1.2140, -1.6916, -0.1464, -2.4422, -1.7594, 1.5684
        };

        static final int IN_DIM = 7;
        static final int HIDDEN = 5;

        final double[][] W1 = new double[IN_DIM][HIDDEN];
        final double[] b1 = new double[HIDDEN];
        final double[] W2 = new double[HIDDEN];
        double b2;

        EvolvedANNPlayer() {
            this(CHAMPION_WEIGHTS);
        }

        EvolvedANNPlayer(double[] flatWeights) {
            int expected = IN_DIM * HIDDEN + HIDDEN + HIDDEN + 1;
            if (flatWeights == null || flatWeights.length < expected)
                throw new IllegalArgumentException(
                        "EvolvedANN expected " + expected + " weights, got "
                                + (flatWeights == null ? 0 : flatWeights.length));
            int idx = 0;
            for (int i = 0; i < IN_DIM; i++)
                for (int j = 0; j < HIDDEN; j++)
                    W1[i][j] = flatWeights[idx++];
            for (int j = 0; j < HIDDEN; j++)
                b1[j] = flatWeights[idx++];
            for (int j = 0; j < HIDDEN; j++)
                W2[j] = flatWeights[idx++];
            b2 = flatWeights[idx++];
        }

        static double sigmoid(double x) {
            return 1.0 / (1.0 + Math.exp(-x));
        }

        double[] features(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2) {
            double f1 = n / 100.0;
            double f2 = coopRate(myHistory, n);
            double f3 = coopRate(oppHistory1, n);
            double f4 = coopRate(oppHistory2, n);
            double f5 = myHistory[n - 1];
            double f6 = oppHistory1[n - 1];
            double f7 = oppHistory2[n - 1];
            return new double[] { f1, f2, f3, f4, f5, f6, f7 };
        }

        double forward(double[] inputs) {
            double[] hidden = new double[HIDDEN];
            for (int j = 0; j < HIDDEN; j++) {
                double sum = b1[j];
                for (int i = 0; i < IN_DIM; i++)
                    sum += inputs[i] * W1[i][j];
                hidden[j] = sigmoid(sum);
            }
            double outSum = b2;
            for (int j = 0; j < HIDDEN; j++)
                outSum += hidden[j] * W2[j];
            return sigmoid(outSum);
        }

        int selectAction(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2) {
            if (n == 0)
                return 0; // nice open, consistent with Axelrod EvolvedANN convention
            double prob = forward(features(n, myHistory, oppHistory1, oppHistory2));
            return (prob >= 0.5) ? 1 : 0;
        }
    }

    // =========================================================
    // STRATEGY 10: Evolved ANN (Noise) — axelrod-dojo Noise 05 analogue
    // =========================================================
    /*
     * Noise-robust counterpart to EvolvedANNPlayer in the spirit of Axelrod's
     * `EvolvedANN5Noise05` — same topology, weights shifted to prefer cooperation
     * under uncertainty. Achieved by adding a cooperation-biased offset to the
     * output bias b2, which lowers the sigmoid output for ambiguous inputs and
     * mirrors what training under 5% action-flip noise produces.
     */
    class EvolvedANNNoisePlayer extends EvolvedANNPlayer {
        static final double COOP_BIAS = 1.0; // subtract from b2; larger => more forgiving

        EvolvedANNNoisePlayer() {
            super(CHAMPION_WEIGHTS);
            this.b2 -= COOP_BIAS;
        }
    }

    // =========================================================
    // STRATEGY: Tit-for-Two-Tats (TFTT, Axelrod 1980)
    // =========================================================
    class TitForTwoTatsPlayer extends Player {
        // Defect only after two consecutive rounds with >=1 opponent defection.
        int selectAction(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2) {
            if (n < 2)
                return 0;
            boolean prev = anyDefected(n - 1, oppHistory1, oppHistory2);
            boolean prev2 = anyDefected(n - 2, oppHistory1, oppHistory2);
            return (prev && prev2) ? 1 : 0;
        }
    }

    // =========================================================
    // STRATEGY: Pavlov / Win-Stay-Lose-Shift (Nowak & Sigmund 1993)
    // =========================================================
    class PavlovPlayer extends Player {
        // Win threshold = R = 6 (payoff for all-cooperate).
        int selectAction(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2) {
            if (n == 0)
                return 0;
            int my = myHistory[n - 1];
            int o1 = oppHistory1[n - 1];
            int o2 = oppHistory2[n - 1];
            int lastPayoff = payoff[my][o1][o2];
            boolean won = lastPayoff >= PAYOFF_R;
            return won ? my : (1 - my);
        }
    }

    // =========================================================
    // STRATEGY: Grim Trigger / Spiteful (Axelrod 1984)
    // =========================================================
    class GrimTriggerPlayer extends Player {
        // Cooperate until ANY opponent defects, then defect forever.
        boolean triggered = false;

        int selectAction(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2) {
            if (triggered)
                return 1;
            if (n == 0)
                return 0;
            if (anyDefected(n - 1, oppHistory1, oppHistory2)) {
                triggered = true;
                return 1;
            }
            return 0;
        }
    }

    // =========================================================
    // STRATEGY: tft_spiteful — Mathieu & Delahaye JASSS 2017 (#1 overall)
    // =========================================================
    class TftSpitefulPlayer extends Player {
        // TFT normally; after two consecutive defection events, switch to AllD.
        boolean triggered = false;

        int selectAction(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2) {
            if (triggered)
                return 1;
            if (n == 0)
                return 0;
            if (n >= 2) {
                boolean prev = anyDefected(n - 1, oppHistory1, oppHistory2);
                boolean prev2 = anyDefected(n - 2, oppHistory1, oppHistory2);
                if (prev && prev2) {
                    triggered = true;
                    return 1;
                }
            }
            return anyDefected(n - 1, oppHistory1, oppHistory2) ? 1 : 0;
        }
    }

    // =========================================================
    // STRATEGY: spiteful_cc — JASSS 2017
    // =========================================================
    class SpitefulCCPlayer extends Player {
        // Play CC for the first two rounds, then Grim Trigger.
        boolean triggered = false;

        int selectAction(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2) {
            if (n < 2)
                return 0;
            if (triggered)
                return 1;
            // Full-history scan on first post-buffer call catches any defection
            // that occurred during rounds 0-1 (which we cooperated through
            // unconditionally without checking). After `triggered` is set,
            // subsequent calls short-circuit above, so the loop is effectively
            // amortised O(1) per match.
            for (int i = 0; i < n; i++) {
                if (anyDefected(i, oppHistory1, oppHistory2)) {
                    triggered = true;
                    return 1;
                }
            }
            return 0;
        }
    }

    // =========================================================
    // STRATEGY: Hard TFT — defect if defection in either of last 2 rounds
    // =========================================================
    class HardTFTPlayer extends Player {
        int selectAction(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2) {
            if (n == 0)
                return 0;
            boolean prev = anyDefected(n - 1, oppHistory1, oppHistory2);
            boolean prev2 = (n >= 2) && anyDefected(n - 2, oppHistory1, oppHistory2);
            return (prev || prev2) ? 1 : 0;
        }
    }

    // =========================================================
    // STRATEGY: Omega TFT — Slany & Kienreich 2000
    // =========================================================
    class OmegaTFTPlayer extends Player {
        static final int DEADLOCK_THRESHOLD = 3;
        static final int RANDOMNESS_THRESHOLD = 8;
        int deadlock1 = 0;
        int deadlock2 = 0;
        int randomness = 0;
        boolean gaveUp = false;

        int selectAction(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2) {
            if (gaveUp)
                return 1;
            if (n == 0)
                return 0;

            int opp1Last = oppHistory1[n - 1];
            int opp2Last = oppHistory2[n - 1];
            boolean oppDefected = (opp1Last == 1) || (opp2Last == 1);

            if (n >= 2) {
                // Deadlock check first: if opponents are oscillating, try to
                // recover cooperation before deciding they are random.
                int opp1Prev = oppHistory1[n - 2];
                int opp2Prev = oppHistory2[n - 2];
                if (opp1Last != opp1Prev) deadlock1++; else deadlock1 = 0;
                if (opp2Last != opp2Prev) deadlock2++; else deadlock2 = 0;
                if (deadlock1 >= DEADLOCK_THRESHOLD || deadlock2 >= DEADLOCK_THRESHOLD) {
                    deadlock1 = 0;
                    deadlock2 = 0;
                    return 0;
                }

                // Randomness give-up second.
                int myPrev = myHistory[n - 2];
                if (opp1Last != myPrev) randomness++;
                if (opp2Last != myPrev) randomness++;
                if (randomness >= RANDOMNESS_THRESHOLD * 2) {
                    gaveUp = true;
                    return 1;
                }
            }

            return oppDefected ? 1 : 0;
        }
    }

    // =========================================================
    // STRATEGY: Contrite TFT — Boerlijst, Nowak, Sigmund 1997
    // =========================================================
    class ContriteTFTPlayer extends Player {
        boolean myStanding = true;
        boolean oppStanding = true;

        int selectAction(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2) {
            if (n == 0)
                return 0;

            int myLast = myHistory[n - 1];
            boolean oppDefected = anyDefected(n - 1, oppHistory1, oppHistory2);

            // Update opponent standing first (was their action justified?).
            boolean prevOppStanding = oppStanding;
            oppStanding = !oppDefected;

            // Then decide my own standing: bad if my last defection was against a good-standing opponent.
            if (myLast == 1 && prevOppStanding)
                myStanding = false;
            else
                myStanding = true;

            if (!myStanding)
                return 0;

            return oppDefected ? 1 : 0;
        }
    }

    // =========================================================
    // STRATEGY: Adaptive Pavlov — Li 2007 meta-classifier
    // =========================================================
    class AdaptivePavlovPlayer extends Player {
        static final int CLASSIFY_ROUNDS = 6;

        int classifyOpp(int n, int[] myHist, int[] oppHist) {
            int coopCount = 0, tftMatches = 0;
            for (int i = 0; i < n; i++) {
                if (oppHist[i] == 0)
                    coopCount++;
                if (i > 0 && oppHist[i] == myHist[i - 1])
                    tftMatches++;
            }
            if (coopCount == n)
                return 0;
            if (coopCount == 0)
                return 1;
            int comparable = Math.max(1, n - 1);
            if (tftMatches >= (int) Math.ceil(comparable * 0.7))
                return 2;
            return 3;
        }

        int selectAction(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2) {
            if (n == 0)
                return 0;
            if (n < CLASSIFY_ROUNDS)
                return anyDefected(n - 1, oppHistory1, oppHistory2) ? 1 : 0;

            int c1 = classifyOpp(n, myHistory, oppHistory1);
            int c2 = classifyOpp(n, myHistory, oppHistory2);

            if (c1 == 1 || c2 == 1)
                return 1;
            if ((c1 == 0 || c1 == 2) && (c2 == 0 || c2 == 2))
                return 0;
            return anyDefected(n - 1, oppHistory1, oppHistory2) ? 1 : 0;
        }
    }

    // =========================================================
    // STRATEGY: mem2 — meta-classifier over {AllC, TFT, AllD}
    // =========================================================
    class Mem2Player extends Player {
        static final int WINDOW = 10;

        int selectAction(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2) {
            if (n == 0)
                return 0;
            int from = Math.max(0, n - WINDOW);
            int coops = 0, total = 0;
            for (int i = from; i < n; i++) {
                if (oppHistory1[i] == 0) coops++;
                if (oppHistory2[i] == 0) coops++;
                total += 2;
            }
            double rate = (double) coops / total;
            if (rate > 0.75)
                return 0;
            if (rate < 0.25)
                return 1;
            return anyDefected(n - 1, oppHistory1, oppHistory2) ? 1 : 0;
        }
    }

    // =========================================================
    // STRATEGY: BackStabber — end-game defector (Harper et al. 2024)
    // =========================================================
    class BackStabberPlayer extends Player {
        static final int DEFECT_FROM = 88;

        int selectAction(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2) {
            if (n >= DEFECT_FROM)
                return 1;
            return 0;
        }
    }

    // =========================================================
    // STRATEGY: DBS (simplified) — Au & Nau 2006 Derived Belief Strategy
    // =========================================================
    /*
     * Simplified 3-player DBS:
     * - Start assuming both opponents are TFT.
     * - Count rounds where opponent's action did not mirror my prior move.
     * - If >= 3 deviations in the last 10 rounds, flag opponent untrusted.
     * - Defect if EITHER opponent is untrusted; else TFT.
     */
    class DBSPlayer extends Player {
        static final int WINDOW = 10;
        static final int PERSIST = 3;

        int deviations(int n, int[] myHist, int[] oppHist) {
            int from = Math.max(1, n - WINDOW);
            int count = 0;
            for (int i = from; i < n; i++) {
                int tftPrediction = myHist[i - 1];
                if (oppHist[i] != tftPrediction)
                    count++;
            }
            return count;
        }

        int selectAction(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2) {
            if (n == 0)
                return 0;

            boolean untrusted1 = (n >= 2) && (deviations(n, myHistory, oppHistory1) >= PERSIST);
            boolean untrusted2 = (n >= 2) && (deviations(n, myHistory, oppHistory2) >= PERSIST);

            if (untrusted1 || untrusted2)
                return 1;
            return anyDefected(n - 1, oppHistory1, oppHistory2) ? 1 : 0;
        }
    }

    // =========================================================
    // EXHIBITION: NostalgicPlayer — "Midnight in Paris"
    // =========================================================
    /*
     * Golden Age thinking. Tracks each opponent's longest cooperation
     * streak as their "golden era." Stays romantic about an opponent once
     * they've had a real era (>=3 consecutive C), even through later defections,
     * unless recent behavior (3+ defects in last 5) cracks the spell.
     *
     * Decision: both still romantic -> cooperate. Both disillusioned -> defect.
     * Exactly one romantic -> "Gil/Adriana moment," two idealized pasts don't
     * reconcile -> defect.
     *
     * Stays in the dream through the final round (no endgame defection).
     * Fully deterministic.
     */
    class NostalgicPlayer extends Player {
        int longestCoopStreak(int[] h) {
            int best = 0, cur = 0;
            for (int v : h) {
                if (v == 0) { cur++; if (cur > best) best = cur; }
                else cur = 0;
            }
            return best;
        }

        int recentDefects(int[] h, int window) {
            int start = Math.max(0, h.length - window);
            int c = 0;
            for (int i = start; i < h.length; i++) if (h[i] == 1) c++;
            return c;
        }

        boolean hasGoldenEra(int[] h)  { return longestCoopStreak(h) >= 3; }
        boolean spellCracked(int[] h)  { return recentDefects(h, 5) >= 3; }
        boolean stillRomantic(int[] h) { return hasGoldenEra(h) && !spellCracked(h); }

        int selectAction(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2) {
            if (n == 0) return 0;

            boolean g1 = hasGoldenEra(oppHistory1);
            boolean g2 = hasGoldenEra(oppHistory2);
            if (!g1 && !g2) return 0;

            boolean r1 = stillRomantic(oppHistory1);
            boolean r2 = stillRomantic(oppHistory2);
            if (r1 && r2) return 0;
            return 1;
        }
    }

    // =========================================================
    // EXHIBITION: LaLaLandPlayer — "City of Stars"
    // =========================================================
    /*
     * Six-chapter seasonal arc over the 110-round match, mirroring the
     * film's structure: Winter -> Spring -> Summer -> Fall -> Winter -> Epilogue.
     *
     *   Winter 1 (0-19):   TFT. Wary opening.
     *   Spring   (20-44):  Always cooperate. Falling in love.
     *   Summer   (45-69):  Cooperate unless BOTH opponents defected last round.
     *                      One you forgive, two you notice.
     *   Fall     (70-94):  Grim within the season. One in-season defection
     *                      ends cooperation for the rest of Fall.
     *   Winter 2 (95-108): TFT. Resigned, seasoned by history.
     *   Epilogue (109):    Cooperate unconditionally. The knowing smile.
     */
    class LaLaLandPlayer extends Player {
        int phase(int n) {
            if (n == 109) return 5;
            if (n < 20) return 0;
            if (n < 45) return 1;
            if (n < 70) return 2;
            if (n < 95) return 3;
            return 4;
        }

        int selectAction(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2) {
            switch (phase(n)) {
                case 0: // Winter 1 — TFT
                    if (n == 0) return 0;
                    return anyDefected(n - 1, oppHistory1, oppHistory2) ? 1 : 0;
                case 1: // Spring — always cooperate
                    return 0;
                case 2: // Summer — cooperate unless BOTH defected last round
                    return (oppHistory1[n - 1] == 1 && oppHistory2[n - 1] == 1) ? 1 : 0;
                case 3: // Fall — grim within season
                    for (int i = 70; i < n; i++) {
                        if (anyDefected(i, oppHistory1, oppHistory2)) return 1;
                    }
                    return 0;
                case 4: // Winter 2 — TFT
                    return anyDefected(n - 1, oppHistory1, oppHistory2) ? 1 : 0;
                case 5: // Epilogue — knowing smile
                    return 0;
            }
            return 0;
        }
    }

    // =========================================================
    // EXHIBITION: FistBumpPlayer — "Project Hail Mary" (Partnership)
    // =========================================================
    /*
     * Asymmetric 3-player partnership. Round 0 cooperate. Track mutual-C
     * streak with each opponent. First opponent to reach a streak of 5
     * becomes "Rocky" — locked in as partner for the match. Before the
     * partner locks in, play TFT (defect iff either opponent defected last
     * round). After lock-in, mirror the partner's last move and ignore the
     * non-partner — even if they're exploiting us. If the partner defects
     * in 3+ of the last 5 rounds, grief switch to permanent defect. Round
     * 109: cooperate iff the partner is still active (stay on Erid).
     */
    class FistBumpPlayer extends Player {
        // Returns 0 or 1 for the partner opponent, or -1 if no partner yet.
        int findPartner(int n, int[] myHistory, int[] opp1, int[] opp2) {
            int streak1 = 0, streak2 = 0;
            for (int i = 0; i < n; i++) {
                if (myHistory[i] == 0 && opp1[i] == 0) {
                    streak1++;
                    if (streak1 >= 5) return 0;
                } else streak1 = 0;
                if (myHistory[i] == 0 && opp2[i] == 0) {
                    streak2++;
                    if (streak2 >= 5) return 1;
                } else streak2 = 0;
            }
            return -1;
        }

        boolean griefTriggered(int n, int[] partnerHist) {
            int start = Math.max(0, n - 5);
            int defects = 0;
            for (int i = start; i < n; i++) {
                if (partnerHist[i] == 1) defects++;
            }
            return defects >= 3;
        }

        int selectAction(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2) {
            if (n == 0) return 0;

            int partner = findPartner(n, myHistory, oppHistory1, oppHistory2);

            // Epilogue: stay with an active partner, else go home.
            if (n == 109) {
                if (partner == -1) return 1;
                int[] pHist = (partner == 0) ? oppHistory1 : oppHistory2;
                return griefTriggered(n, pHist) ? 1 : 0;
            }

            // Before partner is locked in: TFT
            if (partner == -1) {
                return anyDefected(n - 1, oppHistory1, oppHistory2) ? 1 : 0;
            }

            // After partner locked in: mirror partner, ignore non-partner,
            // grief-switch on sustained partner defection.
            int[] partnerHist = (partner == 0) ? oppHistory1 : oppHistory2;
            if (griefTriggered(n, partnerHist)) return 1;
            return partnerHist[n - 1] == 0 ? 0 : 1;
        }
    }

    // =========================================================
    // EXHIBITION: XenolinguistPlayer — "Project Hail Mary" (Probe)
    // =========================================================
    /*
     * Probe-then-commit. Rounds 0-5 play a fixed C,D,C,D,C,D probe
     * regardless of opponents (Grace banging on the wall). At round 6,
     * classify each opponent one-shot from their probe-phase behavior:
     *   <=1 coop -> DEFECTOR (always defect against them)
     *   >=5 coop -> COOPERATOR (always cooperate)
     *   else     -> REACTIVE (play TFT against them)
     * Per-opponent actions are joined pessimistically: defect if either
     * per-opponent rule says defect. No re-classification — the experiment
     * is done.
     */
    class XenolinguistPlayer extends Player {
        static final int CLASS_DEFECTOR = 0;
        static final int CLASS_COOPERATOR = 1;
        static final int CLASS_REACTIVE = 2;

        int probeMove(int n) { return (n % 2 == 0) ? 0 : 1; }

        int classify(int[] oppHist) {
            int coop = 0;
            for (int i = 0; i < 6; i++) if (oppHist[i] == 0) coop++;
            if (coop <= 1) return CLASS_DEFECTOR;
            if (coop >= 5) return CLASS_COOPERATOR;
            return CLASS_REACTIVE;
        }

        int perOppAction(int klass, int oppLast) {
            switch (klass) {
                case CLASS_DEFECTOR:   return 1;
                case CLASS_COOPERATOR: return 0;
                case CLASS_REACTIVE:   return oppLast; // TFT
            }
            return 0;
        }

        int selectAction(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2) {
            if (n < 6) return probeMove(n);

            int c1 = classify(oppHistory1);
            int c2 = classify(oppHistory2);

            int a1 = perOppAction(c1, oppHistory1[n - 1]);
            int a2 = perOppAction(c2, oppHistory2[n - 1]);

            return (a1 == 1 || a2 == 1) ? 1 : 0;
        }
    }

    // =========================================================
    // EXHIBITION: FiveHundredDaysPlayer — "500 Days of Summer"
    // =========================================================
    /*
     * Expectations vs Reality. Each opponent is held to a 90% cooperation
     * baseline — the fantasy. The dream is "broken" the moment either
     * opponent's cumulative cooperation rate ever dips below 0.9 at any
     * point in history. Once broken, it stays broken forever, even if the
     * rate climbs back. Dream intact -> cooperate. Dream broken -> defect.
     *
     * Any defection in the first ~10 rounds is fatal to the dream. Tom's
     * impossible standard: anything less than perfect is a betrayal.
     */
    class FiveHundredDaysPlayer extends Player {
        // Was either opponent's cumulative cooperation rate ever below 90%
        // at any prior round? Integer math: rate < 0.9 iff 10*coops < 9*t.
        boolean dreamBroken(int n, int[] h1, int[] h2) {
            int c1 = 0, c2 = 0;
            for (int t = 1; t <= n; t++) {
                if (h1[t - 1] == 0) c1++;
                if (h2[t - 1] == 0) c2++;
                if (10 * c1 < 9 * t || 10 * c2 < 9 * t) return true;
            }
            return false;
        }

        int selectAction(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2) {
            if (n == 0) return 0;
            return dreamBroken(n, oppHistory1, oppHistory2) ? 1 : 0;
        }
    }

    // =========================================================
    // EXHIBITION: MillersPlanetPlayer — "Interstellar" (Delayed Signal)
    // =========================================================
    /*
     * Time dilation / delayed communication. The player only reacts to
     * opponent actions from 7 rounds ago (Miller's-planet ratio: 1 hour
     * = 7 years). Rounds 0-6: cooperate (no signal has arrived yet).
     * Round 7+: TFT based on opp actions at round n-7 — defect iff
     * either opponent defected back then. The final 7 rounds of the
     * match can never be reacted to; opponents who defect at the end
     * go unpunished. Cooper watching years-old videos from home.
     */
    class MillersPlanetPlayer extends Player {
        static final int LAG = 7;

        int selectAction(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2) {
            if (n < LAG) return 0; // no signal yet
            int t = n - LAG;
            return anyDefected(t, oppHistory1, oppHistory2) ? 1 : 0;
        }
    }

    // =========================================================
    // EXHIBITION: PlanABPlayer — "Interstellar" (Plan A / Plan B)
    // =========================================================
    /*
     * Publicly commits to Plan A (TFT — save everyone). At round 50,
     * checks cumulative score over the first 50 rounds. If average
     * payoff < 4.0 (total < 200), Plan A has failed and the player
     * reveals the real plan: Plan B, permanent defect. Otherwise stays
     * on Plan A for the rest of the match. Captures Professor Brand's
     * deception — the mission was always a survival strategy.
     */
    class PlanABPlayer extends Player {
        int scoreFirstFifty(int[] me, int[] o1, int[] o2) {
            int total = 0;
            for (int i = 0; i < 50; i++) {
                total += payoff[me[i]][o1[i]][o2[i]];
            }
            return total;
        }

        boolean planBRevealed(int n, int[] me, int[] o1, int[] o2) {
            if (n < 50) return false;
            return scoreFirstFifty(me, o1, o2) < 200;
        }

        int selectAction(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2) {
            if (n == 0) return 0;
            if (planBRevealed(n, myHistory, oppHistory1, oppHistory2)) return 1;
            // Plan A: TFT (set-union retaliation)
            return anyDefected(n - 1, oppHistory1, oppHistory2) ? 1 : 0;
        }
    }

    // =========================================================
    // EXHIBITION: BiancasBanPlayer — "10 Things I Hate About You"
    // =========================================================
    /*
     * Asymmetric contingent rule. One opponent is "Kat" (the harder sister,
     * sets the gate); the other is "Bianca" (gated). Kat is the first
     * opponent to defect. If neither has defected by round 5, opp1 is
     * Kat by default. Player cooperates iff Kat has cooperated in at least
     * one of the last 3 rounds, otherwise defects. Bianca's behavior is
     * irrelevant — she's gated by Kat. Captures the film's setup:
     * "Bianca can date only when Kat does."
     */
    class BiancasBanPlayer extends Player {
        // Return 0 or 1 for Kat, or -1 if not yet determined.
        int findKat(int n, int[] opp1, int[] opp2) {
            for (int i = 0; i < n; i++) {
                if (opp1[i] == 1) return 0; // opp1 defected first (ties go to opp1)
                if (opp2[i] == 1) return 1;
            }
            if (n >= 5) return 0; // arbitrary default once the sister dynamic is forced
            return -1;
        }

        int selectAction(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2) {
            if (n == 0) return 0;

            int kat = findKat(n, oppHistory1, oppHistory2);
            if (kat == -1) return 0; // sister dynamic hasn't emerged yet

            int[] katHist = (kat == 0) ? oppHistory1 : oppHistory2;

            // Cooperate iff Kat cooperated in at least one of the last 3 rounds.
            int start = Math.max(0, n - 3);
            for (int i = start; i < n; i++) {
                if (katHist[i] == 0) return 0;
            }
            return 1;
        }
    }

    /*
     * In our tournament, each pair of strategies will play one match against each
     * other.
     * This procedure simulates a single match and returns the scores.
     */
    float[] scoresOfMatch(Player A, Player B, Player C, int rounds) {
        int[] HistoryA = new int[rounds], HistoryB = new int[rounds], HistoryC = new int[rounds];
        float ScoreA = 0, ScoreB = 0, ScoreC = 0;

        for (int i = 0; i < rounds; i++) {
            // Slices are shared across the three selectAction calls this round; implementations must not mutate them.
            int[] sliceA = Arrays.copyOf(HistoryA, i);
            int[] sliceB = Arrays.copyOf(HistoryB, i);
            int[] sliceC = Arrays.copyOf(HistoryC, i);
            int PlayA = A.selectAction(i, sliceA, sliceB, sliceC);
            int PlayB = B.selectAction(i, sliceB, sliceC, sliceA);
            int PlayC = C.selectAction(i, sliceC, sliceA, sliceB);
            ScoreA += payoff[PlayA][PlayB][PlayC];
            ScoreB += payoff[PlayB][PlayC][PlayA];
            ScoreC += payoff[PlayC][PlayA][PlayB];
            HistoryA[i] = PlayA;
            HistoryB[i] = PlayB;
            HistoryC[i] = PlayC;
        }
        float[] result = { ScoreA / rounds, ScoreB / rounds, ScoreC / rounds };
        return result;
    }

    /*
     * The procedure makePlayer is used to reset each of the Players
     * (strategies) in between matches. When you add your own strategy,
     * you will need to add a new entry to makePlayer, and change numPlayers.
     */

    int numPlayers = 36; // 28 zoo + 8 exhibition (Nostalgic, LaLaLand, FistBump, Xenolinguist, FiveHundredDays, MillersPlanet, PlanAB, BiancasBan)

    Player makePlayer(int which) {
        switch (which) {
            case 0:
                return new NicePlayer();
            case 1:
                return new NastyPlayer();
            case 2:
                return new RandomPlayer();
            case 3:
                return new TolerantPlayer();
            case 4:
                return new FreakyPlayer();
            case 5:
                return new T4TPlayer();
            case 6:
                return new GenerousTfTPlayer();
            case 7:
                return new MajorityRulePlayer();
            case 8:
                return new GradualPunisherPlayer();
            case 9:
                return new EqualizerZDPlayer();
            case 10:
                return new ExtortionZDPlayer();
            case 11:
                return new AsylumPlayer();
            case 12:
                return new HallucinationPlayer();
            case 13:
                return new DrunkenPlayer();
            case 14:
                return new EvolvedANNPlayer();
            case 15:
                return new EvolvedANNNoisePlayer();
            case 16:
                return new TitForTwoTatsPlayer();
            case 17:
                return new PavlovPlayer();
            case 18:
                return new GrimTriggerPlayer();
            case 19:
                return new TftSpitefulPlayer();
            case 20:
                return new SpitefulCCPlayer();
            case 21:
                return new HardTFTPlayer();
            case 22:
                return new OmegaTFTPlayer();
            case 23:
                return new ContriteTFTPlayer();
            case 24:
                return new AdaptivePavlovPlayer();
            case 25:
                return new Mem2Player();
            case 26:
                return new BackStabberPlayer();
            case 27:
                return new DBSPlayer();
            case 28:
                return new NostalgicPlayer();
            case 29:
                return new LaLaLandPlayer();
            case 30:
                return new FistBumpPlayer();
            case 31:
                return new XenolinguistPlayer();
            case 32:
                return new FiveHundredDaysPlayer();
            case 33:
                return new MillersPlanetPlayer();
            case 34:
                return new PlanABPlayer();
            case 35:
                return new BiancasBanPlayer();
        }
        throw new RuntimeException("Bad argument passed to makePlayer");
    }

    /* Finally, the remaining code actually runs the tournament. */

    // Set number of tournaments to average over
    static final int NUM_TOURNAMENTS = 100;

    // Match length is drawn uniformly from [MATCH_ROUNDS_MIN, MATCH_ROUNDS_MIN + MATCH_ROUNDS_RANGE]
    static final int MATCH_ROUNDS_MIN   = 90;
    static final int MATCH_ROUNDS_RANGE = 20;

    // payoff[0][0][0]: all-cooperate score (used as Pavlov win threshold)
    static final int PAYOFF_R = 6;

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--test")) {
            mainTest();
            return;
        }
        ThreePrisonersDilemma instance = new ThreePrisonersDilemma();

        System.out.println("Running " + NUM_TOURNAMENTS + " tournaments, please wait...\n");

        // Accumulate scores across all tournament runs
        float[] grandTotal = new float[instance.numPlayers];
        int[] rankPoints = new int[instance.numPlayers]; // points for finishing 1st, 2nd etc.

        for (int t = 0; t < NUM_TOURNAMENTS; t++) {
            float[] scores = instance.runTournament();
            for (int i = 0; i < instance.numPlayers; i++) {
                grandTotal[i] += scores[i];
            }
            // Track rank in this tournament (1st place gets numPlayers points, etc.)
            int[] order = instance.getSortedOrder(scores);
            for (int i = 0; i < instance.numPlayers; i++) {
                rankPoints[order[i]] += (instance.numPlayers - i); // higher rank = more points
            }
        }

        // Compute averages
        float[] avgScore = new float[instance.numPlayers];
        for (int i = 0; i < instance.numPlayers; i++) {
            avgScore[i] = grandTotal[i] / NUM_TOURNAMENTS;
        }

        // Sort by average score
        int[] sortedOrder = instance.getSortedOrder(avgScore);

        // Print final results
        System.out.println("=== AVERAGED RESULTS OVER " + NUM_TOURNAMENTS + " TOURNAMENTS ===\n");
        System.out.printf("%-5s %-25s %-15s %-10s%n", "Rank", "Player", "Avg Score", "Win Points");
        System.out.println("-".repeat(60));
        for (int i = 0; i < instance.numPlayers; i++) {
            int p = sortedOrder[i];
            System.out.printf("%-5d %-25s %-15.3f %-10d%n",
                    (i + 1),
                    instance.makePlayer(p).name(),
                    avgScore[p],
                    rankPoints[p]);
        }
        System.out.println("\n'Avg Score' = average total points per tournament.");
        System.out.println(
                "'Win Points' = ranking points accumulated (1st place = " + instance.numPlayers + " pts each run).");
    }

    // Runs a single tournament and returns the raw scores array
    float[] runTournament() {
        float[] totalScore = new float[numPlayers];

        for (int i = 0; i < numPlayers; i++)
            for (int j = i; j < numPlayers; j++)
                for (int k = j; k < numPlayers; k++) {
                    Player A = makePlayer(i);
                    Player B = makePlayer(j);
                    Player C = makePlayer(k);
                    int rounds = MATCH_ROUNDS_MIN + (int) Math.rint(MATCH_ROUNDS_RANGE * Math.random());
                    float[] matchResults = scoresOfMatch(A, B, C, rounds);
                    totalScore[i] += matchResults[0];
                    totalScore[j] += matchResults[1];
                    totalScore[k] += matchResults[2];
                }
        return totalScore;
    }

    static void mainTest() {
        ThreePrisonersDilemma inst = new ThreePrisonersDilemma();
        int failures = 0;

        // ---- T4TPlayer ----
        T4TPlayer tft = inst.new T4TPlayer();
        if (tft.selectAction(0, new int[0], new int[0], new int[0]) != 0) {
            failures++;
            System.out.println("FAIL T4T round0");
        }
        if (tft.selectAction(1, new int[]{0}, new int[]{0}, new int[]{0}) != 0) {
            failures++;
            System.out.println("FAIL T4T both coop");
        }
        if (tft.selectAction(1, new int[]{0}, new int[]{1}, new int[]{0}) != 1) {
            failures++;
            System.out.println("FAIL T4T opp1 defect");
        }
        if (tft.selectAction(1, new int[]{0}, new int[]{0}, new int[]{1}) != 1) {
            failures++;
            System.out.println("FAIL T4T opp2 defect");
        }
        if (tft.selectAction(1, new int[]{0}, new int[]{1}, new int[]{1}) != 1) {
            failures++;
            System.out.println("FAIL T4T both defect");
        }
        System.out.println("T4T: 5 deterministic checks done");

        // ---- GenerousTfTPlayer: retaliation rate ~1-q after a defection ----
        GenerousTfTPlayer gtft = inst.new GenerousTfTPlayer();
        int defects = 0, trials = 4000;
        for (int i = 0; i < trials; i++) {
            if (gtft.selectAction(1, new int[]{0}, new int[]{1}, new int[]{0}) == 1)
                defects++;
        }
        double rate = defects / (double) trials;
        if (rate < 0.85 || rate > 0.95) {
            failures++;
            System.out.println("FAIL GTFT retaliation rate=" + rate);
        } else {
            System.out.println("PASS GTFT retaliation rate=" + rate);
        }
        if (gtft.selectAction(1, new int[]{0}, new int[]{0}, new int[]{0}) != 0) {
            failures++;
            System.out.println("FAIL GTFT should coop after both-coop");
        }

        // ---- MajorityRulePlayer: per-opponent ----
        MajorityRulePlayer maj = inst.new MajorityRulePlayer();
        int m = maj.selectAction(3, new int[]{0,0,0}, new int[]{0,0,0}, new int[]{1,1,1});
        if (m != 1) {
            failures++;
            System.out.println("FAIL soft_majo: opp2 all-defect should trigger defect");
        }
        int m2 = maj.selectAction(3, new int[]{0,0,0}, new int[]{0,0,1}, new int[]{1,0,0});
        if (m2 != 0) {
            failures++;
            System.out.println("FAIL soft_majo: should cooperate when both opps 2C1D");
        }
        System.out.println("soft_majo deterministic checks done");

        // ---- GradualPunisherPlayer: 1st-event = punish 1 then calm 2 ----
        GradualPunisherPlayer g = inst.new GradualPunisherPlayer();
        int a0 = g.selectAction(0, new int[0], new int[0], new int[0]);
        int a1 = g.selectAction(1, new int[]{0},       new int[]{1},       new int[]{0});
        int a2 = g.selectAction(2, new int[]{0,1},     new int[]{1,0},     new int[]{0,0});
        int a3 = g.selectAction(3, new int[]{0,1,0},   new int[]{1,0,0},   new int[]{0,0,0});
        int a4 = g.selectAction(4, new int[]{0,1,0,0}, new int[]{1,0,0,0}, new int[]{0,0,0,0});
        int[] want = {0,1,0,0,0};
        int[] got  = {a0,a1,a2,a3,a4};
        for (int i = 0; i < 5; i++) {
            if (want[i] != got[i]) {
                failures++;
                System.out.println("FAIL Gradual 1st-event r" + i + " want=" + want[i] + " got=" + got[i]);
            }
        }

        // 2nd event = punish 2 then calm 2
        GradualPunisherPlayer g2 = inst.new GradualPunisherPlayer();
        g2.selectAction(0, new int[0], new int[0], new int[0]);
        g2.selectAction(1, new int[]{0},       new int[]{1},       new int[]{0});
        g2.selectAction(2, new int[]{0,1},     new int[]{1,0},     new int[]{0,0});
        g2.selectAction(3, new int[]{0,1,0},   new int[]{1,0,0},   new int[]{0,0,0});
        int b4 = g2.selectAction(4, new int[]{0,1,0,0},     new int[]{1,0,0,1},     new int[]{0,0,0,0});
        int b5 = g2.selectAction(5, new int[]{0,1,0,0,1},   new int[]{1,0,0,1,0},   new int[]{0,0,0,0,0});
        int b6 = g2.selectAction(6, new int[]{0,1,0,0,1,1}, new int[]{1,0,0,1,0,0}, new int[]{0,0,0,0,0,0});
        if (!(b4 == 1 && b5 == 1 && b6 == 0)) {
            failures++;
            System.out.println("FAIL Gradual 2nd-event got " + b4 + "," + b5 + "," + b6);
        } else {
            System.out.println("PASS Gradual 2nd-event");
        }

        // ---- ZD players: just verify they construct cleanly (no clamp exception) ----
        try {
            inst.new EqualizerZDPlayer();
            System.out.println("PASS EqualizerZD construct");
        } catch (Throwable t) {
            failures++;
            System.out.println("FAIL EqualizerZD construct: " + t.getMessage());
        }
        try {
            inst.new ExtortionZDPlayer();
            System.out.println("PASS ExtortionZD construct");
        } catch (Throwable t) {
            failures++;
            System.out.println("FAIL ExtortionZD construct: " + t.getMessage());
        }

        // ---- EvolvedANN players ----
        try {
            EvolvedANNPlayer ann = inst.new EvolvedANNPlayer();
            int r0 = ann.selectAction(0, new int[0], new int[0], new int[0]);
            if (r0 != 0) {
                failures++;
                System.out.println("FAIL EvolvedANN round0 not cooperate: " + r0);
            } else {
                System.out.println("PASS EvolvedANN construct + nice open");
            }
        } catch (Throwable t) {
            failures++;
            System.out.println("FAIL EvolvedANN construct: " + t.getMessage());
        }
        try {
            EvolvedANNNoisePlayer annN = inst.new EvolvedANNNoisePlayer();
            EvolvedANNPlayer annBase = inst.new EvolvedANNPlayer();
            // Noise variant should have b2 lower than base by COOP_BIAS.
            double diff = annBase.b2 - annN.b2;
            if (Math.abs(diff - EvolvedANNNoisePlayer.COOP_BIAS) > 1e-9) {
                failures++;
                System.out.println("FAIL EvolvedANNNoise bias shift diff=" + diff);
            } else {
                System.out.println("PASS EvolvedANNNoise cooperation bias applied");
            }
        } catch (Throwable t) {
            failures++;
            System.out.println("FAIL EvolvedANNNoise construct: " + t.getMessage());
        }

        try {
            TitForTwoTatsPlayer tftt = inst.new TitForTwoTatsPlayer();
            if (tftt.selectAction(0, new int[0], new int[0], new int[0]) != 0) { failures++; System.out.println("FAIL TFTT r0"); }
            if (tftt.selectAction(1, new int[]{0}, new int[]{1}, new int[]{0}) != 0) { failures++; System.out.println("FAIL TFTT r1 single"); }
            if (tftt.selectAction(2, new int[]{0,0}, new int[]{1,1}, new int[]{0,0}) != 1) { failures++; System.out.println("FAIL TFTT r2 double"); }
            if (tftt.selectAction(2, new int[]{0,0}, new int[]{1,0}, new int[]{0,0}) != 0) { failures++; System.out.println("FAIL TFTT r2 non-consec"); }
            System.out.println("PASS TitForTwoTats");
        } catch (Throwable t) { failures++; System.out.println("FAIL TFTT construct: " + t.getMessage()); }

        try {
            PavlovPlayer pv = inst.new PavlovPlayer();
            if (pv.selectAction(0, new int[0], new int[0], new int[0]) != 0) { failures++; System.out.println("FAIL Pavlov r0"); }
            if (pv.selectAction(1, new int[]{0}, new int[]{0}, new int[]{0}) != 0) { failures++; System.out.println("FAIL Pavlov CCC win-stay"); }
            if (pv.selectAction(1, new int[]{0}, new int[]{1}, new int[]{1}) != 1) { failures++; System.out.println("FAIL Pavlov CDD lose-shift"); }
            if (pv.selectAction(1, new int[]{1}, new int[]{0}, new int[]{0}) != 1) { failures++; System.out.println("FAIL Pavlov DCC win-stay"); }
            System.out.println("PASS Pavlov");
        } catch (Throwable t) { failures++; System.out.println("FAIL Pavlov construct: " + t.getMessage()); }

        try {
            GrimTriggerPlayer gt = inst.new GrimTriggerPlayer();
            int r0 = gt.selectAction(0, new int[0], new int[0], new int[0]);
            int r1 = gt.selectAction(1, new int[]{0}, new int[]{1}, new int[]{0});
            int r2 = gt.selectAction(2, new int[]{0,1}, new int[]{1,0}, new int[]{0,0});
            int r3 = gt.selectAction(3, new int[]{0,1,1}, new int[]{1,0,0}, new int[]{0,0,0});
            if (!(r0 == 0 && r1 == 1 && r2 == 1 && r3 == 1)) {
                failures++;
                System.out.println("FAIL Grim got " + r0 + r1 + r2 + r3 + " want 0111");
            } else {
                System.out.println("PASS GrimTrigger");
            }
        } catch (Throwable t) { failures++; System.out.println("FAIL Grim construct: " + t.getMessage()); }

        try {
            TftSpitefulPlayer ts = inst.new TftSpitefulPlayer();
            if (ts.selectAction(0, new int[0], new int[0], new int[0]) != 0) { failures++; System.out.println("FAIL tft_spiteful r0"); }
            int r1 = ts.selectAction(1, new int[]{0}, new int[]{1}, new int[]{0});
            int r2 = ts.selectAction(2, new int[]{0,1}, new int[]{1,1}, new int[]{0,0});
            int r3 = ts.selectAction(3, new int[]{0,1,1}, new int[]{1,1,0}, new int[]{0,0,0});
            if (!(r1 == 1 && r2 == 1 && r3 == 1)) {
                failures++;
                System.out.println("FAIL tft_spiteful sequence " + r1 + r2 + r3);
            } else {
                System.out.println("PASS tft_spiteful");
            }
        } catch (Throwable t) { failures++; System.out.println("FAIL tft_spiteful construct: " + t.getMessage()); }

        try {
            SpitefulCCPlayer sc = inst.new SpitefulCCPlayer();
            if (sc.selectAction(0, new int[0], new int[0], new int[0]) != 0) { failures++; System.out.println("FAIL sc r0"); }
            if (sc.selectAction(1, new int[]{0}, new int[]{1}, new int[]{1}) != 0) { failures++; System.out.println("FAIL sc r1 buffer"); }
            if (sc.selectAction(2, new int[]{0,0}, new int[]{1,0}, new int[]{1,0}) != 1) { failures++; System.out.println("FAIL sc r2 trigger"); }
            if (sc.selectAction(3, new int[]{0,0,1}, new int[]{1,0,0}, new int[]{1,0,0}) != 1) { failures++; System.out.println("FAIL sc r3 locked"); }
            System.out.println("PASS spiteful_cc");
        } catch (Throwable t) { failures++; System.out.println("FAIL sc construct: " + t.getMessage()); }

        try {
            HardTFTPlayer hard = inst.new HardTFTPlayer();
            if (hard.selectAction(2, new int[]{0,0}, new int[]{1,0}, new int[]{0,0}) != 1) { failures++; System.out.println("FAIL HardTFT recent"); }
            if (hard.selectAction(2, new int[]{0,0}, new int[]{0,0}, new int[]{0,0}) != 0) { failures++; System.out.println("FAIL HardTFT clean"); }
            System.out.println("PASS HardTFT");
        } catch (Throwable t) { failures++; System.out.println("FAIL HardTFT construct: " + t.getMessage()); }

        try {
            OmegaTFTPlayer om = inst.new OmegaTFTPlayer();
            if (om.selectAction(0, new int[0], new int[0], new int[0]) != 0) { failures++; System.out.println("FAIL OmegaTFT r0"); }
            if (om.selectAction(5, new int[]{0,0,0,0,0}, new int[]{0,0,0,0,0}, new int[]{0,0,0,0,0}) != 0) { failures++; System.out.println("FAIL OmegaTFT clean"); }
            System.out.println("PASS OmegaTFT construct + nice");
        } catch (Throwable t) { failures++; System.out.println("FAIL OmegaTFT construct: " + t.getMessage()); }

        try {
            ContriteTFTPlayer ct = inst.new ContriteTFTPlayer();
            if (ct.selectAction(0, new int[0], new int[0], new int[0]) != 0) { failures++; System.out.println("FAIL ContriteTFT r0"); }
            if (ct.selectAction(1, new int[]{0}, new int[]{0}, new int[]{0}) != 0) { failures++; System.out.println("FAIL ContriteTFT r1 clean"); }
            System.out.println("PASS ContriteTFT");
        } catch (Throwable t) { failures++; System.out.println("FAIL ContriteTFT construct: " + t.getMessage()); }

        try {
            AdaptivePavlovPlayer ap = inst.new AdaptivePavlovPlayer();
            if (ap.selectAction(0, new int[0], new int[0], new int[0]) != 0) { failures++; System.out.println("FAIL APavlov r0"); }
            int[] allC = {0,0,0,0,0,0};
            if (ap.selectAction(6, allC, allC, allC) != 0) { failures++; System.out.println("FAIL APavlov all-coop"); }
            int[] allD = {1,1,1,1,1,1};
            if (ap.selectAction(6, allC, allD, allC) != 1) { failures++; System.out.println("FAIL APavlov all-defect"); }
            System.out.println("PASS AdaptivePavlov");
        } catch (Throwable t) { failures++; System.out.println("FAIL APavlov construct: " + t.getMessage()); }

        try {
            Mem2Player mem2 = inst.new Mem2Player();
            int[] myH = {0,0,0,0,0,0,0,0,0,0};
            int[] oC = {0,0,0,0,0,0,0,0,0,0};
            if (mem2.selectAction(10, myH, oC, oC) != 0) { failures++; System.out.println("FAIL Mem2 AllC"); }
            int[] oD = {1,1,1,1,1,1,1,1,1,1};
            if (mem2.selectAction(10, myH, oD, oD) != 1) { failures++; System.out.println("FAIL Mem2 AllD"); }
            System.out.println("PASS Mem2");
        } catch (Throwable t) { failures++; System.out.println("FAIL Mem2 construct: " + t.getMessage()); }

        try {
            BackStabberPlayer bs = inst.new BackStabberPlayer();
            if (bs.selectAction(0, new int[0], new int[0], new int[0]) != 0) { failures++; System.out.println("FAIL BS early"); }
            if (bs.selectAction(80, new int[80], new int[80], new int[80]) != 0) { failures++; System.out.println("FAIL BS mid"); }
            if (bs.selectAction(88, new int[88], new int[88], new int[88]) != 1) { failures++; System.out.println("FAIL BS late"); }
            System.out.println("PASS BackStabber");
        } catch (Throwable t) { failures++; System.out.println("FAIL BS construct: " + t.getMessage()); }

        try {
            DBSPlayer dbs = inst.new DBSPlayer();
            if (dbs.selectAction(0, new int[0], new int[0], new int[0]) != 0) { failures++; System.out.println("FAIL DBS r0"); }
            int[] allC = {0,0,0,0,0,0};
            if (dbs.selectAction(6, allC, allC, allC) != 0) { failures++; System.out.println("FAIL DBS clean"); }
            int[] myHist = {0,0,0,0,0,0};
            int[] random1 = {0,1,0,1,0,1};
            if (dbs.selectAction(6, myHist, random1, allC) != 1) { failures++; System.out.println("FAIL DBS flag untrusted"); }
            System.out.println("PASS DBS");
        } catch (Throwable t) { failures++; System.out.println("FAIL DBS construct: " + t.getMessage()); }

        // ---- NostalgicPlayer ----
        try {
            NostalgicPlayer nos = inst.new NostalgicPlayer();
            if (nos.selectAction(0, new int[0], new int[0], new int[0]) != 0) { failures++; System.out.println("FAIL Nostalgic r0"); }
            // Neither has golden era: cooperate by default
            if (nos.selectAction(3, new int[]{0,0,0}, new int[]{1,1,1}, new int[]{1,1,1}) != 0) { failures++; System.out.println("FAIL Nostalgic no-era default coop"); }
            // Both have golden era and still romantic: cooperate
            int[] golden = {0,0,0,1,1};
            if (nos.selectAction(5, new int[]{0,0,0,0,0}, golden, golden) != 0) { failures++; System.out.println("FAIL Nostalgic both-romantic coop"); }
            // One romantic, one cracked (3+ defects in last 5): defect
            int[] cracked = {0,0,0,1,1,1};
            if (nos.selectAction(6, new int[]{0,0,0,0,0,0}, new int[]{0,0,0,0,1,1}, cracked) != 1) { failures++; System.out.println("FAIL Nostalgic one-cracked defect"); }
            System.out.println("PASS NostalgicPlayer");
        } catch (Throwable t) { failures++; System.out.println("FAIL Nostalgic: " + t.getMessage()); }

        // ---- LaLaLandPlayer ----
        try {
            LaLaLandPlayer ll = inst.new LaLaLandPlayer();
            if (ll.selectAction(0, new int[0], new int[0], new int[0]) != 0) { failures++; System.out.println("FAIL LaLaLand r0 winter TFT"); }
            // Spring (n=22): always cooperate
            int[] h22 = new int[22];
            if (ll.selectAction(22, h22, h22, h22) != 0) { failures++; System.out.println("FAIL LaLaLand spring coop"); }
            // Summer (n=50): cooperate unless BOTH defected last round
            int[] h50 = new int[50];
            int[] h50d = new int[50]; h50d[49] = 1;
            if (ll.selectAction(50, h50, h50d, h50) != 0) { failures++; System.out.println("FAIL LaLaLand summer one-defect should coop"); }
            if (ll.selectAction(50, h50, h50d, h50d) != 1) { failures++; System.out.println("FAIL LaLaLand summer both-defect should defect"); }
            // Epilogue (n=109): always cooperate regardless of history
            int[] h109 = new int[109]; for (int i = 0; i < 109; i++) h109[i] = 1;
            if (ll.selectAction(109, h109, h109, h109) != 0) { failures++; System.out.println("FAIL LaLaLand epilogue coop"); }
            System.out.println("PASS LaLaLandPlayer");
        } catch (Throwable t) { failures++; System.out.println("FAIL LaLaLand: " + t.getMessage()); }

        // ---- FistBumpPlayer ----
        try {
            FistBumpPlayer fb = inst.new FistBumpPlayer();
            if (fb.selectAction(0, new int[0], new int[0], new int[0]) != 0) { failures++; System.out.println("FAIL FistBump r0"); }
            // Before partner: TFT (opp1 defected -> defect)
            if (fb.selectAction(1, new int[]{0}, new int[]{1}, new int[]{0}) != 1) { failures++; System.out.println("FAIL FistBump pre-partner TFT"); }
            // After 5 mutual-coop streak with opp1: mirror partner's last coop
            int[] my5 = {0,0,0,0,0};
            int[] coopOpp = {0,0,0,0,0};
            int[] badOpp  = {1,1,1,1,1};
            if (fb.selectAction(5, my5, coopOpp, badOpp) != 0) { failures++; System.out.println("FAIL FistBump partner-coop mirror"); }
            System.out.println("PASS FistBumpPlayer");
        } catch (Throwable t) { failures++; System.out.println("FAIL FistBump: " + t.getMessage()); }

        // ---- XenolinguistPlayer ----
        try {
            XenolinguistPlayer xl = inst.new XenolinguistPlayer();
            // Probe sequence: C,D,C,D,C,D
            if (xl.selectAction(0, new int[0], new int[0], new int[0]) != 0) { failures++; System.out.println("FAIL Xenolinguist probe r0 (C)"); }
            if (xl.selectAction(1, new int[]{0}, new int[]{0}, new int[]{0}) != 1) { failures++; System.out.println("FAIL Xenolinguist probe r1 (D)"); }
            if (xl.selectAction(2, new int[]{0,1}, new int[]{0,0}, new int[]{0,0}) != 0) { failures++; System.out.println("FAIL Xenolinguist probe r2 (C)"); }
            // Round 6: all-coop opponent -> classify COOPERATOR -> cooperate
            int[] myProbe = {0,1,0,1,0,1};
            int[] allCoopProbe = {0,0,0,0,0,0};
            if (xl.selectAction(6, myProbe, allCoopProbe, allCoopProbe) != 0) { failures++; System.out.println("FAIL Xenolinguist post-probe coop"); }
            // Round 6: all-defect opponent -> classify DEFECTOR -> defect
            int[] allDefProbe = {1,1,1,1,1,1};
            if (xl.selectAction(6, myProbe, allDefProbe, allDefProbe) != 1) { failures++; System.out.println("FAIL Xenolinguist post-probe defect"); }
            System.out.println("PASS XenolinguistPlayer");
        } catch (Throwable t) { failures++; System.out.println("FAIL Xenolinguist: " + t.getMessage()); }

        // ---- FiveHundredDaysPlayer ----
        try {
            FiveHundredDaysPlayer fhd = inst.new FiveHundredDaysPlayer();
            if (fhd.selectAction(0, new int[0], new int[0], new int[0]) != 0) { failures++; System.out.println("FAIL FiveHundredDays r0"); }
            int[] allCoop = {0,0,0,0,0};
            if (fhd.selectAction(5, allCoop, allCoop, allCoop) != 0) { failures++; System.out.println("FAIL FiveHundredDays perfect-coop should coop"); }
            int[] withDefect = {0,1,0,0,0}; // defect at round 1 breaks the 90% threshold
            if (fhd.selectAction(5, allCoop, withDefect, allCoop) != 1) { failures++; System.out.println("FAIL FiveHundredDays broken-dream should defect"); }
            System.out.println("PASS FiveHundredDaysPlayer");
        } catch (Throwable t) { failures++; System.out.println("FAIL FiveHundredDays: " + t.getMessage()); }

        // ---- MillersPlanetPlayer ----
        try {
            MillersPlanetPlayer mp = inst.new MillersPlanetPlayer();
            if (mp.selectAction(0, new int[0], new int[0], new int[0]) != 0) { failures++; System.out.println("FAIL MillersPlanet r0"); }
            if (mp.selectAction(6, new int[6], new int[6], new int[6]) != 0) { failures++; System.out.println("FAIL MillersPlanet no-signal"); }
            // Round 7: react to round 0
            int[] h7coop = new int[7];
            if (mp.selectAction(7, h7coop, h7coop, h7coop) != 0) { failures++; System.out.println("FAIL MillersPlanet r7 coop-signal"); }
            int[] h7def = new int[7]; h7def[0] = 1;
            if (mp.selectAction(7, h7def, h7def, h7coop) != 1) { failures++; System.out.println("FAIL MillersPlanet r7 defect-signal"); }
            System.out.println("PASS MillersPlanetPlayer");
        } catch (Throwable t) { failures++; System.out.println("FAIL MillersPlanet: " + t.getMessage()); }

        // ---- PlanABPlayer ----
        try {
            PlanABPlayer pab = inst.new PlanABPlayer();
            if (pab.selectAction(0, new int[0], new int[0], new int[0]) != 0) { failures++; System.out.println("FAIL PlanAB r0"); }
            // n=50 with all-CCC history: avg=6 per round, total=300 >= 200 -> stay Plan A (TFT)
            int[] h50a = new int[50]; // all cooperate
            if (pab.selectAction(50, h50a, h50a, h50a) != 0) { failures++; System.out.println("FAIL PlanAB plan-A TFT"); }
            // n=50 with all CDD: payoff[0][1][1]=0, total=0 < 200 -> Plan B (defect)
            int[] h50d = new int[50]; for (int i = 0; i < 50; i++) h50d[i] = 1;
            if (pab.selectAction(50, h50a, h50d, h50d) != 1) { failures++; System.out.println("FAIL PlanAB plan-B defect"); }
            System.out.println("PASS PlanABPlayer");
        } catch (Throwable t) { failures++; System.out.println("FAIL PlanAB: " + t.getMessage()); }

        // ---- BiancasBanPlayer ----
        try {
            BiancasBanPlayer bb = inst.new BiancasBanPlayer();
            if (bb.selectAction(0, new int[0], new int[0], new int[0]) != 0) { failures++; System.out.println("FAIL BiancasBan r0"); }
            // n=3, all-coop, no kat yet (n < 5): cooperate
            int[] h3 = new int[3];
            if (bb.selectAction(3, h3, h3, h3) != 0) { failures++; System.out.println("FAIL BiancasBan early no-kat coop"); }
            // n=5, all-coop: opp1=Kat by default; Kat cooperated in last 3 -> cooperate
            int[] h5coop = new int[5];
            if (bb.selectAction(5, h5coop, h5coop, h5coop) != 0) { failures++; System.out.println("FAIL BiancasBan kat-coop gate open"); }
            // n=5, opp1 (Kat) defected rounds 2-4: no coop in last 3 -> defect
            int[] katDef = {0,0,1,1,1};
            if (bb.selectAction(5, h5coop, katDef, h5coop) != 1) { failures++; System.out.println("FAIL BiancasBan kat-defect gate closed"); }
            System.out.println("PASS BiancasBanPlayer");
        } catch (Throwable t) { failures++; System.out.println("FAIL BiancasBan: " + t.getMessage()); }

        System.out.println(failures == 0 ? "ALL TESTS PASS" : ("FAILURES: " + failures));
        if (failures > 0)
            System.exit(1);
    }

    // Helper: returns player indices sorted from highest to lowest score
    int[] getSortedOrder(float[] scores) {
        int[] sortedOrder = new int[numPlayers];
        for (int i = 0; i < numPlayers; i++) {
            int j = i - 1;
            for (; j >= 0; j--) {
                if (scores[i] > scores[sortedOrder[j]])
                    sortedOrder[j + 1] = sortedOrder[j];
                else
                    break;
            }
            sortedOrder[j + 1] = i;
        }
        return sortedOrder;
    }

} // end of class ThreePrisonersDilemma
