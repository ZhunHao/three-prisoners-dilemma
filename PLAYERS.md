# Players Reference

Documentation for the 28 strategies competing in the Three-Player Iterated Prisoner's Dilemma tournament defined in [ThreePrisonersDilemma.java](ThreePrisonersDilemma.java).

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

## Original Variants

Three strategies original to this project, with no canonical literature equivalent. Included as non-standard foils that stress-test the rest of the field.

### AsylumPlayer ([:456](ThreePrisonersDilemma.java:456))
Inverted Gradual Punisher. Starts by defecting, **punishes cooperation**, rewards defection. An intentionally irrational foil that rewards opponents for defection and attacks cooperators.

### HallucinationPlayer ([:530](ThreePrisonersDilemma.java:530))
Picks a random past round and plays TfT against that hallucinated "last round". Deterministic against pure nice/nasty opponents; injects noise against reactive strategies.

### DrunkenPlayer ([:563](ThreePrisonersDilemma.java:563))
Extends `GradualPunisherPlayer`. Probability of a random action grows linearly with `n`, capped at 80% once `n ≥ 80`. When drunk, it skips the parent's state updates — so punishment/calm counters drift.

## Adding a New Player

1. Define `class MyPlayer extends Player` inside `ThreePrisonersDilemma` and override `selectAction`.
2. Increment `numPlayers` at [:1043](ThreePrisonersDilemma.java:1043).
3. Add a new `case` in [`makePlayer`](ThreePrisonersDilemma.java:1045).
4. Re-run: `javac ThreePrisonersDilemma.java && java ThreePrisonersDilemma`.

Each match calls `makePlayer` fresh, so instance fields reset between matches — safe to keep per-match state (e.g. `punishRemaining`).

## Verification

Deterministic sanity checks live inside a `mainTest` static method gated by the `--test` flag. Run:

```
javac ThreePrisonersDilemma.java
java ThreePrisonersDilemma --test
```

Prints `ALL TESTS PASS` when every covered strategy behaves per its canonical definition (20 checks across the 28-strategy field).

## Tournament Mechanics

- Every unordered triple `(i, j, k)` with `i ≤ j ≤ k` plays one match ([`runTournament`](ThreePrisonersDilemma.java:1164)).
- Match length: `90 + round(20 * rand)` ≈ 90–110 rounds.
- `main` runs `NUM_TOURNAMENTS = 100` tournaments and reports average score plus rank-points (1st place earns `numPlayers` points per run).
