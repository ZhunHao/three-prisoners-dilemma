# Players Reference

Documentation for the 36 strategies competing in the Three-Player Iterated Prisoner's Dilemma tournament defined in [ThreePrisonersDilemma.java](ThreePrisonersDilemma.java): 28 zoo strategies (baseline + research canon + original foils) plus 8 movie-themed exhibition players.

## Game Basics

- Actions: `0` = cooperate, `1` = defect.
- Each round, three players play simultaneously. Payoff for player 1 is `payoff[my][opp1][opp2]`.
- Payoff values (player 1's perspective):

| State | Notation | Payoff |
|-------|----------|--------|
| All cooperate | R | 6 |
| I defect, both coop | T | 8 |
| I coop, both defect | S | 0 |
| All defect | P | 2 |
| I coop, one defects | K | 3 |
| I defect, one defects | L | 5 |

- A match is 90–110 rounds. Final score = mean per-round payoff.
- Each player must override [`selectAction(n, myHistory, oppHistory1, oppHistory2)`](ThreePrisonersDilemma.java:41).

## Player Interface

```java
int selectAction(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2)
```

- `n` — rounds already played (0 on first move).
- `myHistory[i]`, `oppHistoryX[i]` — action taken in round `i` (0 or 1).
- Return `0` to cooperate or `1` to defect.

## Baseline Strategies

These six are provided by the assignment and serve as the reference opponents.

### NicePlayer ([:54](ThreePrisonersDilemma.java:54))
Always cooperates. Maximum exploitable.

### NastyPlayer ([:61](ThreePrisonersDilemma.java:61))
Always defects. Immune to exploitation, captures `T` whenever others cooperate.

### RandomPlayer ([:68](ThreePrisonersDilemma.java:68))
Uniform 50/50 coin-flip every round. No memory.

### TolerantPlayer ([:78](ThreePrisonersDilemma.java:78))
Defects only when total defections across both opponents outnumber cooperations over the full history.

### FreakyPlayer ([:103](ThreePrisonersDilemma.java:103))
Decides once in the constructor to be a permanent NicePlayer or NastyPlayer (50/50).

### T4TPlayer ([:121](ThreePrisonersDilemma.java:121))
Canonical 3-player Tit-for-Tat: cooperates round 0; afterwards defects iff either opponent defected last round.

## Research Canon

Nineteen strategies ported from published IPD literature, listed in the order they appear in [ThreePrisonersDilemma.java](ThreePrisonersDilemma.java). Each is a faithful 3-player adaptation of the cited canonical work.

### GenerousTfTPlayer ([:145](ThreePrisonersDilemma.java:145))
Canonical Nowak–Sigmund GTFT. Retaliates if **either** opponent defected last round, forgiving with probability `q = 0.1`. Forgiveness escapes mutual-defection spirals under noise.

### MajorityRulePlayer ([:174](ThreePrisonersDilemma.java:174))
Canonical per-opponent soft_majo (JASSS 2017). Cooperates iff both opponents individually have cooperation count ≥ defection count over full history; ties break to cooperate.

### GradualPunisherPlayer ([:207](ThreePrisonersDilemma.java:207))
Canonical Beaufils–Delahaye–Mathieu Gradual. Tracks `defectionEvents` = rounds where at least one opponent defected. On the `k`-th event, defects `k` rounds total then emits 2 cooperation rounds. State: `punishRemaining`, `calmRemaining`, `defectionEvents`.

### EqualizerZDPlayer ([:271](ThreePrisonersDilemma.java:271))
Memory-one ZD equalizer from Taha & Ghoneim (2021), Eq. 20. Free params `pC2 = 0.9`, `pD0 = 0.2`; remaining probabilities derived without clamping and asserted to lie in `[0,1]`. Pins the combined opponent payoff regardless of their actions.

### ExtortionZDPlayer ([:360](ThreePrisonersDilemma.java:360))
ZD extortion strategy (Taha & Ghoneim 2021, Eq. 24–26). Parameters `chi = 1.0`, `phi = 0.1` validated against paper bounds on construction; `pD0 = 0`. Enforces `E1 - P ≥ chi · (E2 + E3 - 2P)`.

### EvolvedANNPlayer ([:591](ThreePrisonersDilemma.java:591))
Canonical Evolved ANN in the family of Axelrod-Python's `EvolvedANN` (Harper et al. 2017). Feed-forward net: 7 inputs (normalized round, self/opp1/opp2 cooperation rates, three last actions) → 5 hidden sigmoid → 1 sigmoid output. Weights come from offline evolutionary training (`CHAMPION_WEIGHTS`, 46 floats). Cooperates when `prob < 0.5`.

### EvolvedANNNoisePlayer ([:683](ThreePrisonersDilemma.java:683))
Noise-robust counterpart in the spirit of Axelrod's `EvolvedANN5Noise05`. Same topology and weights as `EvolvedANNPlayer`, with the output bias `b2` shifted by `COOP_BIAS = 1.0` toward cooperation — producing the forgiveness-under-uncertainty behaviour that action-flip-noise training yields.

### TitForTwoTatsPlayer ([:695](ThreePrisonersDilemma.java:695))
Defects only after two consecutive rounds with at least one opponent defection. Axelrod 1980.

### PavlovPlayer ([:709](ThreePrisonersDilemma.java:709))
Win-Stay-Lose-Shift (Nowak & Sigmund 1993). Repeats last action iff last payoff ≥ R = 6; otherwise flips.

### GrimTriggerPlayer ([:726](ThreePrisonersDilemma.java:726))
Cooperates until the first defection from any opponent, then defects forever. Axelrod 1984.

### TftSpitefulPlayer ([:746](ThreePrisonersDilemma.java:746))
JASSS 2017 #1 overall. Plays TFT until two consecutive defection events, then switches to permanent defection.

### SpitefulCCPlayer ([:770](ThreePrisonersDilemma.java:770))
Cooperates rounds 0–1 unconditionally, then Grim Trigger. JASSS 2017.

### HardTFTPlayer ([:797](ThreePrisonersDilemma.java:797))
TFT variant retaliating if defection appeared in either of the last two rounds.

### OmegaTFTPlayer ([:810](ThreePrisonersDilemma.java:810))
TFT plus a per-opponent deadlock breaker (forces cooperation when opponents oscillate; takes precedence over the randomness give-up) and a pooled randomness detector (switches to permanent defection when opponents look random). Slany & Kienreich 2000.

### ContriteTFTPlayer ([:858](ThreePrisonersDilemma.java:858))
Tracks good/bad standing; updates opponent standing before own. When in bad standing, cooperates to recover. Boerlijst, Nowak, Sigmund 1997.

### AdaptivePavlovPlayer ([:889](ThreePrisonersDilemma.java:889))
Plays TFT for 6 rounds to classify each opponent as {Cooperator, Defector, TFT-like, Random}, then responds per class. Li 2007.

### Mem2Player ([:930](ThreePrisonersDilemma.java:930))
Meta-classifier switching among {AllC, TFT, AllD} based on 10-round opponent cooperation rate.

### BackStabberPlayer ([:955](ThreePrisonersDilemma.java:955))
Cooperates until round 88, defects from round 88 onward — exploits the [90, 110] match-length bound. Harper et al. 2024.

### DBSPlayer ([:975](ThreePrisonersDilemma.java:975))
Simplified Derived Belief Strategy (Au & Nau 2006). Per-opponent deviation counter over 10-round window; treats an opponent as untrusted after 3+ deviations and defects.

## Troll Strategies

Three strategies original to this project, with no canonical literature equivalent. Added purely for the lols — deliberately irrational chaos agents with no intention of scoring well. They disrupt cooperative equilibria, and are here for entertainment value first, game theory second.

### AsylumPlayer ([:456](ThreePrisonersDilemma.java:456))
Inverted Gradual Punisher. Starts by defecting, **punishes cooperation**, rewards defection. An intentionally irrational foil that rewards opponents for defection and attacks cooperators.

### HallucinationPlayer ([:530](ThreePrisonersDilemma.java:530))
Picks a random past round and plays TfT against that hallucinated "last round". Deterministic against pure nice/nasty opponents; injects noise against reactive strategies.

### DrunkenPlayer ([:563](ThreePrisonersDilemma.java:563))
Extends `GradualPunisherPlayer`. Probability of a random action grows linearly with `n`, capped at 80% once `n ≥ 80`. When drunk, it skips the parent's state updates — so punishment/calm counters drift.

## Exhibition Strategies (Movie Night)

Eight thematic players built from movies and books watched with friends. None are literature-backed or tournament-optimized — they capture scenes, arcs, or character psychologies rather than game-theoretic logic. Included as oddball foils and for fun.

### NostalgicPlayer ([:1019](ThreePrisonersDilemma.java:1019))
*Midnight in Paris.* Tracks each opponent's "golden era" (longest consecutive cooperation streak, min 3). Once an opponent has a real era we stay romantic about them, unless recent defections (3+ in last 5) crack the spell. Both still romantic → cooperate. Neither → defect. Exactly one → the Gil/Adriana moment, two idealized pasts don't reconcile → defect.

### LaLaLandPlayer ([:1070](ThreePrisonersDilemma.java:1070))
*La La Land.* Six-chapter seasonal arc: Winter 1 (TFT, rounds 0–19), Spring (all-cooperate, 20–44), Summer (cooperate unless both defected last round, 45–69), Fall (grim within season, 70–94), Winter 2 (TFT, 95–108), Epilogue (unconditional cooperate, 109).

### FistBumpPlayer ([:1116](ThreePrisonersDilemma.java:1116))
*Project Hail Mary.* First opponent to hit 5 consecutive mutual-C becomes "Rocky" — locked in as partner. Before lock-in: TFT. After: mirror Rocky's last move, ignore the non-partner. Grief-switches to permanent defect if Rocky defects in 3+ of last 5. Round 109: cooperate iff Rocky is still active.

### XenolinguistPlayer ([:1181](ThreePrisonersDilemma.java:1181))
*Project Hail Mary.* Probe-then-commit. Rounds 0–5 play `C,D,C,D,C,D`. At round 6, classify each opponent from their probe response: ≤1 coop → Defector, ≥5 coop → Cooperator, else Reactive. Per-opponent strategy (Defector→D, Cooperator→C, Reactive→TFT) joined pessimistically.

### FiveHundredDaysPlayer ([:1231](ThreePrisonersDilemma.java:1231))
*500 Days of Summer.* Expectations vs Reality. Each opponent held to a 90% cooperation baseline. The moment either opponent's cumulative cooperation rate ever dips below 0.9, the dream breaks permanently — defect forever, no recovery.

### MillersPlanetPlayer ([:1262](ThreePrisonersDilemma.java:1262))
*Interstellar.* Delayed signal. Cooperates for first 7 rounds (Miller's-planet ratio: 1 hour = 7 years — no message has arrived). From round 7 on, TFT based on opponent actions 7 rounds ago. The last 7 rounds of the match are unreactable.

### PlanABPlayer ([:1283](ThreePrisonersDilemma.java:1283))
*Interstellar.* Plan A = TFT (save everyone). At round 50, check cumulative score over the first 50 rounds. If total < 200 (avg < 4.0), reveal Plan B: permanent defect. Otherwise stay on Plan A.

### BiancasBanPlayer ([:1317](ThreePrisonersDilemma.java:1317))
*10 Things I Hate About You.* One opponent is "Kat" (the first to defect, or opp1 by default after round 5), the other is "Bianca" (gated). Cooperate iff Kat cooperated in at least one of the last 3 rounds, otherwise defect. Bianca's behavior is irrelevant — she's gated by Kat.

## Adding a New Player

1. Define `class MyPlayer extends Player` inside `ThreePrisonersDilemma` and override `selectAction`.
2. Increment `numPlayers` at [:1385](ThreePrisonersDilemma.java:1385).
3. Add a new `case` in [`makePlayer`](ThreePrisonersDilemma.java:1387).
4. Re-run: `javac ThreePrisonersDilemma.java && java ThreePrisonersDilemma`.

Each match calls `makePlayer` fresh, so instance fields reset between matches — safe to keep per-match state (e.g. `punishRemaining`).

## Verification

Deterministic sanity checks live inside a `mainTest` static method gated by the `--test` flag. Run:

```
javac ThreePrisonersDilemma.java
java ThreePrisonersDilemma --test
```

Prints `ALL TESTS PASS` when every covered strategy behaves per its canonical definition. Tests cover T4T, GenerousTFT, MajorityRule, GradualPunisher, ZD players, and the EvolvedANN variants; exhibition players are not tested (thematic, not canonical).

## Tournament Mechanics

- Every unordered triple `(i, j, k)` with `i ≤ j ≤ k` plays one match ([`runTournament`](ThreePrisonersDilemma.java:1522)).
- Match length: `90 + round(20 * rand)` ≈ 90–110 rounds.
- `main` runs `NUM_TOURNAMENTS = 100` tournaments and reports average score plus rank-points (1st place earns `numPlayers` points per run).
