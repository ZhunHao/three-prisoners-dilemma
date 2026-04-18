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
            if (oppHistory1[n - 1] == 1 || oppHistory2[n - 1] == 1)
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

            boolean anyDefected = (oppHistory1[n - 1] == 1) || (oppHistory2[n - 1] == 1);
            if (anyDefected) {
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

            boolean prevEvent = (oppHistory1[n - 1] == 1) || (oppHistory2[n - 1] == 1);
            if (prevEvent) {
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
            int myCoop = 0, op1Coop = 0, op2Coop = 0;
            for (int i = 0; i < n; i++) {
                if (myHistory[i] == 0) myCoop++;
                if (oppHistory1[i] == 0) op1Coop++;
                if (oppHistory2[i] == 0) op2Coop++;
            }
            double f2 = (double) myCoop / n;
            double f3 = (double) op1Coop / n;
            double f4 = (double) op2Coop / n;
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
            boolean prev = (oppHistory1[n - 1] == 1) || (oppHistory2[n - 1] == 1);
            boolean prev2 = (oppHistory1[n - 2] == 1) || (oppHistory2[n - 2] == 1);
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
            boolean won = lastPayoff >= 6;
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
            if (oppHistory1[n - 1] == 1 || oppHistory2[n - 1] == 1) {
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
                boolean prev = (oppHistory1[n - 1] == 1) || (oppHistory2[n - 1] == 1);
                boolean prev2 = (oppHistory1[n - 2] == 1) || (oppHistory2[n - 2] == 1);
                if (prev && prev2) {
                    triggered = true;
                    return 1;
                }
            }
            return (oppHistory1[n - 1] == 1 || oppHistory2[n - 1] == 1) ? 1 : 0;
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
                if (oppHistory1[i] == 1 || oppHistory2[i] == 1) {
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
            boolean prev = (oppHistory1[n - 1] == 1) || (oppHistory2[n - 1] == 1);
            boolean prev2 = (n >= 2) && ((oppHistory1[n - 2] == 1) || (oppHistory2[n - 2] == 1));
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
            boolean oppDefected = (oppHistory1[n - 1] == 1) || (oppHistory2[n - 1] == 1);

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
                return (oppHistory1[n - 1] == 1 || oppHistory2[n - 1] == 1) ? 1 : 0;

            int c1 = classifyOpp(n, myHistory, oppHistory1);
            int c2 = classifyOpp(n, myHistory, oppHistory2);

            if (c1 == 1 || c2 == 1)
                return 1;
            if ((c1 == 0 || c1 == 2) && (c2 == 0 || c2 == 2))
                return 0;
            return (oppHistory1[n - 1] == 1 || oppHistory2[n - 1] == 1) ? 1 : 0;
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
            return (oppHistory1[n - 1] == 1 || oppHistory2[n - 1] == 1) ? 1 : 0;
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
            return (oppHistory1[n - 1] == 1 || oppHistory2[n - 1] == 1) ? 1 : 0;
        }
    }

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
         *  If last defect was at round r, returns n - r.
         *  If no defects, returns Integer.MAX_VALUE. */
        int roundsSinceAnyDefect(int n, int[] h1, int[] h2, int[] h3) {
            for (int r = n - 1; r >= 0; r--) {
                if (h1[r] == 1 || h2[r] == 1 || h3[r] == 1) return n - r;
            }
            return Integer.MAX_VALUE;
        }

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

    /*
     * In our tournament, each pair of strategies will play one match against each
     * other.
     * This procedure simulates a single match and returns the scores.
     */
    float[] scoresOfMatch(Player A, Player B, Player C, int rounds) {
        int[] HistoryA = new int[0], HistoryB = new int[0], HistoryC = new int[0];
        float ScoreA = 0, ScoreB = 0, ScoreC = 0;

        for (int i = 0; i < rounds; i++) {
            int PlayA = A.selectAction(i, HistoryA, HistoryB, HistoryC);
            int PlayB = B.selectAction(i, HistoryB, HistoryC, HistoryA);
            int PlayC = C.selectAction(i, HistoryC, HistoryA, HistoryB);
            ScoreA = ScoreA + payoff[PlayA][PlayB][PlayC];
            ScoreB = ScoreB + payoff[PlayB][PlayC][PlayA];
            ScoreC = ScoreC + payoff[PlayC][PlayA][PlayB];
            HistoryA = extendIntArray(HistoryA, PlayA);
            HistoryB = extendIntArray(HistoryB, PlayB);
            HistoryC = extendIntArray(HistoryC, PlayC);
        }
        float[] result = { ScoreA / rounds, ScoreB / rounds, ScoreC / rounds };
        return result;
    }

    // This is a helper function needed by scoresOfMatch.
    int[] extendIntArray(int[] arr, int next) {
        int[] result = new int[arr.length + 1];
        for (int i = 0; i < arr.length; i++) {
            result[i] = arr[i];
        }
        result[result.length - 1] = next;
        return result;
    }

    /*
     * The procedure makePlayer is used to reset each of the Players
     * (strategies) in between matches. When you add your own strategy,
     * you will need to add a new entry to makePlayer, and change numPlayers.
     */

    int numPlayers = 29; // 28 zoo + 1 submission (ClassifierResponder)

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
                return new ClassifierResponderPlayer();
        }
        throw new RuntimeException("Bad argument passed to makePlayer");
    }

    /* Finally, the remaining code actually runs the tournament. */

    // Set number of tournaments to average over
    static final int NUM_TOURNAMENTS = 100;

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
                    int rounds = 90 + (int) Math.rint(20 * Math.random());
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

        // ---- ClassifierResponderPlayer ----
        try {
            ClassifierResponderPlayer cr = inst.new ClassifierResponderPlayer();
            if (cr.selectAction(0, new int[0], new int[0], new int[0]) != 0) {
                failures++;
                System.out.println("FAIL ClassifierResponder round0 should cooperate");
            } else {
                System.out.println("PASS ClassifierResponder round0");
            }
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
            {
                int[] longOpp = new int[15]; longOpp[5] = 1; longOpp[12] = 1; // two defections
                int[] zero15 = new int[15];
                if (cr.countUnprovokedDefectsInWindow(longOpp, zero15, zero15, 0, 10) != 1) {
                    failures++; System.out.println("FAIL countUnprovokedInWindow should count only r=5");
                }
                if (cr.countUnprovokedDefectsInWindow(longOpp, zero15, zero15, 0, 20) != 2) {
                    failures++; System.out.println("FAIL countUnprovokedInWindow wide window");
                }
            }
            System.out.println("PASS countUnprovoked tests");
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
                if (cr.classify(10, opp, me, other) != ClassifierResponderPlayer.CLASS_EXPLOITABLE) {
                    failures++; System.out.println("FAIL classify E1 non-retaliator");
                }
            }
            System.out.println("PASS classify tests");
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
            // ---- Head-to-head sanity via scoresOfMatch ----
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

                // vs (T4T, T4T): expect >= 6.00 (mutual-C then endgame defect)
                r = inst.scoresOfMatch(inst.new ClassifierResponderPlayer(),
                                        inst.new T4TPlayer(), inst.new T4TPlayer(), 110);
                if (r[0] < 6.00f) {
                    failures++;
                    System.out.println("FAIL vs (T4T,T4T) avg=" + r[0]);
                } else {
                    System.out.println("PASS vs (T4T,T4T) avg=" + r[0]);
                }

                // vs (TftSpiteful, TftSpiteful): expect >= 6.00
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
        } catch (Throwable t) {
            failures++;
            System.out.println("FAIL ClassifierResponder construct: " + t.getMessage());
        }

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
