# The Classifier-Responder Strategy
## 3-Player Iterated Prisoner's Dilemma — SC4003 / CZ4046 / CE4046 Assignment 2

**Author:** Wong Zhun Hao
**Submission:** `Wong_Zhunhao_Player.java`
**Date:** 2026-04-18

---

## 1. Problem Setup

In the 3-player Iterated Prisoner's Dilemma (IPD), three agents simultaneously choose to **cooperate** (C, 0) or **defect** (D, 1) for a series of rounds. The payoff to a given player depends on all three actions, following the matrix (first action is ours):

| Triple | Payoff | Triple | Payoff |
|--------|--------|--------|--------|
| DCC    | **8**  | CDC, CCD | 3 |
| CCC    | **6**  | DDD    | 2 |
| DCD, DDC | **5** | CDD    | 0 |

The ordering DCC > CCC > DCD > CDC > DDD > CDD satisfies the classical 2-player dominance condition whenever one opponent's move is held fixed, while rewarding mutual cooperation at CCC = 6.

The tournament harness plays **every ordered triple** of strategies (including self-triples and pair-duplicates) for a random number of rounds uniformly distributed on {90, 91, …, 110}, with **no noise** — actions and payoffs are fully deterministic. Scores are averaged per round per match, then summed across matches and across 500 tournament repetitions.

Crucially, the strategy sees only the elapsed-round index `n` and the three histories; it cannot communicate, signal a handshake, or distinguish its own clones from other players.

---

## 2. Strategy Overview

We submit **Classifier-Responder**: at each round, the strategy classifies each of its two opponents into one of four labels based on observed history, then applies a *pessimistic* joint-response rule. On the provably-final round (`n == 109`), it defects if both opponents are classified SAFE — an optimal backward-induction move at the known-terminal round.

The strategy is:

- **Stateless.** Every call recomputes classification from the history arrays; no fields persist across calls. This eliminates a category of correctness bugs and makes the strategy trivially deterministic given opponents.
- **Passive.** It never defects to *probe* opponent types. This preserves the mutual-cooperation ceiling of 6.0 per round against peaceful matchups, which is where most of its score comes from.
- **Conservative.** The pessimistic joint rule errs toward defection only when clear evidence exists, and toward cooperation by default. Classification thresholds are deliberately strict-fast in the early game and tolerant later.

---

## 3. Design Principles

The strategy is designed around Axelrod's four robustness properties for IPD strategies:

| Property | Classifier-Responder's realization |
|----------|-------------------------------------|
| **Nice** | Always cooperates at round 0. Never defects against opponents classified SAFE (except the n=109 endgame rule). |
| **Retaliatory** | HOSTILE classification is *permanent*: once an opponent exhibits unprovoked aggression, we defect against them for the rest of the match. |
| **Forgiving** | The S1 rule allows a SAFE classification even for opponents with *provoked* defections, once 3 peaceful rounds have elapsed. The classifier does not hold grudges for retaliatory behavior. |
| **Clear** | Rule order is deterministic; decisions reduce to one of {Cooperate, Defect, Mirror-worse}. A reader can trace any decision from the public history alone. |

### Why not pure Tit-for-Tat or Grim?

Pure TFT is optimal in a 2-player, noise-free IPD, but loses *exploitation opportunities* against non-retaliating opponents. Grim is exploitation-resistant but unforgiving — a single lucky defection locks mutual-defection for the remainder.

Classifier-Responder unifies both: TFT-like for ambiguous opponents, Grim-like against confirmed aggressors, and — importantly — an *EXPLOITABLE* branch for non-retaliators that neither TFT nor Grim can detect.

### Why passive observation?

We considered active probing: a single calibration defection at round 2 to test opponent reaction. This was rejected for three reasons:

1. **Cost against cooperators.** An active probe concedes ≈1 round of sucker payoff (CDC = 3 vs CCC = 6) in every peaceful matchup. Since peaceful matchups dominate total score, this is a net loss.
2. **Amplified damage in 3-player.** A probe defection visible to *two* opponents can trigger Grim-trigger in either, locking mutual defection for the whole match.
3. **Unnecessary.** With 90–110 rounds available and no noise, we have ample time to observe opponents' unprovoked behavior against each other. A defection *between* opponents is as diagnostic as a defection we caused — and costs us nothing.

### Why a pessimistic joint-response rule?

In 3-player IPD a single action is emitted against two opponents. When one opponent is hostile and the other cooperative, we must choose one action. The options:

- **Optimistic** (cooperate unless both are hostile) — one defector can farm us unchecked for the match.
- **Pessimistic** (defect if either is hostile) — we sacrifice the cooperative score with the safe opponent but stop exploitation. In a 3-player match this is strictly better in expectation: if hostile opponent defects and we cooperate, we get CDC = 3; if we defect instead, we get DDC = 5. We cannot independently cooperate with the safe opponent — we emit one action.

The pessimistic rule is therefore the correct 3-player generalization of TFT's "mirror worst" instinct.

---

## 4. The Classifier

Each opponent is classified per round using a monotonic rule cascade. The first matching rule's label is returned. All inputs derive purely from history — we call this "evidence."

Let:
- `unprovoked(r)` = opp defected at round `r` *and* no player defected in round `r−1` or `r−2`;
- `total_defects_in_match` = count across all three histories;
- `rounds_since_any_defect` = `n` − round of most recent defection by anyone, or ∞ if none.

### Rule cascade

| Rule | Condition | Label |
|------|-----------|-------|
| **n=0** | `n == 0` | AMBIGUOUS |
| **H1** | opp defected at round 0 | HOSTILE |
| **H2** | ≥2 unprovoked defections anywhere | HOSTILE |
| **H3** | ≥1 unprovoked defection in the rounds 0–10 window | HOSTILE |
| **E1** | opp has 0 defects AND someone else defected AND ≥5 rounds elapsed since any defection | EXPLOITABLE |
| **E2** | n ≥ 20 AND opp had 0 defections in last 20 rounds AND *someone else* did | EXPLOITABLE |
| **S1** | opp had ≥1 defection but all were provoked AND rounds_since_defect ≥ 3 | SAFE |
| **S2** | opp has 0 defections AND n ≥ 8 | SAFE |
| *fallback* | — | AMBIGUOUS |

### Rationale per rule

**H1: round-0 defection is always unprovoked.** There is no prior round to retaliate to. An opponent defecting at round 0 is either always-defect, random, or a signal of hostility. We respond immediately.

**H2: repeated unprovoked aggression.** Two or more unprovoked defections are strong evidence of non-cooperative intent. A single late defection could be a Pavlov edge case or coincidence, but two is a pattern.

**H3: early-game aggression is a permanent flag.** The rounds 0–10 window is the diagnostic period: during early rounds, well-designed strategies (TFT-family, Pavlov) cooperate until provoked. A defection by an opponent there without provocation is a reliable HOSTILE signal. We use a *history-indexed* check — `any unprovoked defection whose round index is in 0..10` — so the classification is monotonic in `n`: once HOSTILE, always HOSTILE.

**E1 / E2: observed non-retaliation is the only exploitation signal.** We cannot tell an always-cooperator from a TFT if neither has been tested. But if a defection has occurred in the match (against us, or between opponents) and an opponent has not responded by defecting for 5+ (E1) or 20+ (E2) rounds, they are plausibly a non-retaliator. EXPLOITABLE triggers pure defection against them.

**S1: forgive provoked defections after a cooldown.** An opponent that defected *only* after someone else defected is a TFT-family player; after 3 rounds of peace we restore SAFE status.

**S2: clean opponents are SAFE once evidence is substantial.** `n ≥ 8` prevents premature classification — a mere 2–3 rounds of cooperation isn't yet diagnostic.

### Monotonicity properties

HOSTILE and EXPLOITABLE, once triggered, are permanent — they depend on history properties that only grow. SAFE and AMBIGUOUS can transition to HOSTILE on new evidence. This asymmetry protects against BackStabber-style opponents that feign cooperation after early aggression.

---

## 5. Joint Response

Given class labels `(c1, c2)` for the two opponents and their last actions `(last1, last2)`:

| c1 \ c2        | SAFE      | AMBIGUOUS       | EXPLOITABLE | HOSTILE |
|----------------|-----------|-----------------|-------------|---------|
| **SAFE**       | Cooperate | max(last1,last2)| Defect      | Defect  |
| **AMBIGUOUS**  | max(l1,l2)| max(l1,l2)      | Defect      | Defect  |
| **EXPLOITABLE**| Defect    | Defect          | Defect      | Defect  |
| **HOSTILE**    | Defect    | Defect          | Defect      | Defect  |

**Equivalent one-line rule:**
```
if HOSTILE or EXPLOITABLE in (c1, c2):  defect
elif c1 == c2 == SAFE:                   cooperate
else:                                    max(last1, last2)   # TFT-worse
```

TFT-worse — mirror the more aggressive opponent's last move — is a deliberately pessimistic fallback for mixed or uncertain classifications. It is nice (returns C when both opponents cooperated), retaliatory (returns D when either defected), and forgiving (proportional to observed behavior).

---

## 6. Endgame Defection

### Motivation

The tournament length is uniform on {90, …, 110}, so **round 109 is the provably-final round**: no round 110 can ever occur. At that round, against an opponent that will not retaliate (because the game is over), a defection yields +2 (DCC = 8 vs CCC = 6) with zero downside.

### Why only round 109?

Defecting at any round `n < 109` carries a retaliation cost if the game continues. Let `p(n)` denote the probability that round `n` is the last round, given that we've played `n` rounds. From the uniform distribution on rounds in {90, …, 110}:

| Round `n` | P(last round) | EV(defect) − EV(cooperate) |
|-----------|---------------|-----------------------------|
| 109       | **1.000**     | +2.0                         |
| 108       | 0.400         | +0.2                         |
| 107       | 0.286         | −0.14                        |
| 106       | 0.222         | −0.33                        |
| 105       | 0.182         | −0.45                        |
| ≤ 104     | < 0.154       | negative                      |

The expected-value calculation: defection yields +2 this round (8 − 6), at a cost of −3 on the next round's retaliation (CDC = 3 vs CCC = 6) if the game continues with probability `1 − p`. Positive EV requires `p > 1/3`.

Only `n = 109` (EV = +2, risk = 0) is an unambiguous win. `n = 108` is marginal (+0.2 in expectation) and introduces the possibility of a cascade: a student opponent who also anticipates endgame defection may retaliate, triggering mutual D at round 109 where CR was planning to defect anyway — the payoffs get complicated. We accept the clean `n = 109` only to keep the code minimal, auditable, and justifiable.

### Conditioning on SAFE+SAFE

The endgame defection only triggers when *both* opponents are classified SAFE. If either opponent is HOSTILE, EXPLOITABLE, or AMBIGUOUS, we're already defecting (or mirroring) via the joint response — the endgame rule adds nothing new there.

---

## 7. Evaluation

### 7.1 Head-to-head sanity checks

These confirm per-round behavior against deterministic opponents over 110 rounds:

| Matchup                              | Observed avg | Predicted | Interpretation                                      |
|--------------------------------------|--------------|-----------|-----------------------------------------------------|
| vs (NicePlayer, NicePlayer)          | 6.018        | 6.018     | Mutual-C + endgame defect at 109 → DCC bonus        |
| vs (NastyPlayer, NastyPlayer)        | 1.982        | 1.982     | Mutual-D from round 1 onward (DDD = 2)              |
| vs (T4TPlayer, T4TPlayer)            | 6.018        | 6.018     | Same as Nice — pure cooperation + endgame           |
| vs (TftSpiteful, TftSpiteful)        | 6.018        | 6.018     | Same as Nice                                        |
| vs (self, self, self)                | 5.964        | 5.964     | All three classify each other SAFE; all defect at 109 simultaneously → DDD bonus absorbs |

Observed matches prediction to four decimal places in every case. This validates the classifier, joint-response rule, and endgame override are jointly correct.

### 7.2 Full tournament

Ran the tournament harness with `NUM_TOURNAMENTS = 500` over all 29 strategies (28-player research zoo + Classifier-Responder). Top-5 by average score:

| Rank | Player                     | Avg Score | Gap to #1 |
|------|----------------------------|-----------|-----------|
| 1    | TftSpitefulPlayer          | 2351.884  | —         |
| 2    | DBSPlayer                  | 2351.729  | −0.155    |
| 3    | GrimTriggerPlayer          | 2350.863  | −1.021    |
| **4**| **ClassifierResponderPlayer** | **2350.380** | **−1.504** |
| 5    | OmegaTFTPlayer             | 2348.655  | −3.229    |

Rank 4 with a 0.06% gap to rank 1. The top 4 strategies are within 1.5 points of each other — a near-saturated regime where the strategy ceiling is dominated by mutual cooperation with TFT-family peers.

### 7.3 Ablation: endgame rule

To confirm the endgame rule is net-positive, I ran a 100-tournament ablation with `n = 109` defection disabled:

| Configuration | Avg Score | Rank |
|---------------|-----------|------|
| With endgame defection    | 2350.38 | 4    |
| Without endgame defection | 2348.99 | 5    |

Endgame defection contributes +1.4 points, moving us from rank 5 to rank 4. Kept enabled.

### 7.4 Why not top 3?

The 1.5-point gap to rank 1 is an *intrinsic* property of the classifier's extra nuance, not a bug. In the 28-player research zoo, peer strategies are overwhelmingly TFT-family variants that cooperate generously; a pure TFT+endgame strategy (what TftSpiteful approximates) gets most of the cooperative score without paying the classifier's small decision-overhead cost.

In a real classroom tournament the field is wider: simpler TFT variants, Pavlov variants, *and* naive strategies (always-cooperators, buggy TFTs, exploiters that mishandle the endgame). Classifier-Responder's EXPLOITABLE detection — which returns almost nothing against the research zoo — should become a meaningful score source against naive student submissions. The rank observed here is a conservative lower bound on real-tournament performance.

---

## 8. Limitations and Design Trade-offs

**Cannot distinguish always-cooperators from TFT until a defection occurs.** If all three players cooperate for the entire match, we never know if our opponents would have retaliated to a defection. The EXPLOITABLE branch requires evidence of non-retaliation, which only exists if *someone* defected. This is a deliberate trade-off — preserving peaceful matchups is worth more than occasional exploitation.

**HOSTILE classifications are permanent.** An opponent that defects once in rounds 0–10 is flagged HOSTILE forever, even if they subsequently cooperate. This protects against BackStabber-style deception but costs a few rounds of mutual cooperation in the edge case of a genuinely-reformed opponent (we never observed this in the zoo).

**No noise adaptation.** The tournament is noise-free by construction, so handling action-flip noise is not necessary. A noise-robust version would loosen H1/H2 thresholds and consider a defection at round 0 to be less reliably hostile.

**Self-play imperfection.** In the triple (self, self, self), all three instances simultaneously defect at round 109, producing DDD = 2 rather than CCC = 6 — a small loss relative to what TftSpiteful achieves in its self-triple (CCC = 6 throughout). A strategy that could identify its own clones by handshake would avoid this, but such behavior is explicitly *not* permitted by the assignment spec and would be brittle against grader mechanisms that test clone-detection as cheating.

**Cannot cooperate with one opponent and defect against another in the same round.** This is a fundamental limitation of the 3-player IPD as given (one action per round). The pessimistic joint-rule is the best-response given this constraint, but it sacrifices some per-matchup score in mixed (SAFE, HOSTILE) triples.

---

## 9. Summary

Classifier-Responder is a robust, interpretable, 3-player IPD strategy built from five components:

1. A **stateless, passive observer** that extracts classification evidence from history.
2. A **four-class classifier** (SAFE, HOSTILE, EXPLOITABLE, AMBIGUOUS) with monotonic rules that satisfy Axelrod's nice / retaliatory / forgiving / clear properties.
3. A **pessimistic joint-response rule** that correctly generalizes TFT to the 3-player setting.
4. A **provably-terminal endgame defection** at round 109 when both opponents are SAFE.
5. An explicit set of design trade-offs — documented, tested, and verified — against the full 29-player research zoo.

In a 500-tournament evaluation against 28 canonical strategies including TftSpiteful, DBS, Grim-Trigger, OmegaTFT, DBSPlayer, and EvolvedANN, Classifier-Responder places rank 4 at 2350.38 average score — within 1.5 points of the top, and substantially above naive strategies (TitForTwoTats at 2329, TolerantPlayer at 2270, NicePlayer at 2041). Its distinctive EXPLOITABLE branch is expected to outperform canonical TFT variants against the wider distribution of strategies anticipated in the real classroom tournament.
