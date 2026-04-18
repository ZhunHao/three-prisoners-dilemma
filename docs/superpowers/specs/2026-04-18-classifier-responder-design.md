# Classifier-Responder — Submission Design

**Date:** 2026-04-18
**Context:** SC4003/CZ4046/CE4046 Assignment 2 — 3-player Repeated Prisoner's Dilemma
**Submission deadline:** 2026-04-22 23:59:59
**Working file:** `ThreePrisonersDilemma.java` (submitted as a single class)

---

## 1. Goal

Design a single strategy class, to be submitted as the student's agent in the classroom tournament, optimised to win against a field expected to contain a mix of literature-established strategies plus some buggy or naive student submissions.

Grade: 70% performance, 30% report/code clarity.

## 2. Tournament Facts (from `ThreePrisonersDilemma.java`)

- **Rounds per match:** uniform on {90, 91, ..., 110} (computed as `90 + Math.rint(20 * Math.random())`)
- **Noise:** none — payoffs and actions are deterministic
- **Pairing:** every triple of players including duplicates (`for i; for j≥i; for k≥j`); averaged over many tournaments
- **Scoring:** average payoff per round is summed across all triples
- **Payoff matrix** (player 1's view):
  - CCC = 6, DCC = 8, CCD/CDC = 3, DDC/DCD = 5, CDD = 0, DDD = 2
  - Ordering: DCC > CCC > DCD > CDC > DDD > CDD

## 3. Strategic Thesis

In the submitted field, most players will be literature-canon variants (TFT-family, Grim-family, Pavlov-family) plus some buggy/naive strategies. With no noise, mutual cooperation at 6/round is the ceiling; the top of a pure-canon field clusters tightly around that ceiling.

The edge above that ceiling comes from two sources:

1. **Exploiting non-retaliators** (ALLC-types, slow-forgiving strategies) when evidence shows they won't punish defection.
2. **Defecting on the provably-final round** (round index 109), which is pure +2 with no retaliation window.

Both require classifying opponents cheaply. A classifier that costs nothing in all-cooperator matchups (no active probing) captures these gains without sacrificing the ceiling.

## 4. Architecture

```
selectAction(n, myHistory, oppHistory1, oppHistory2)
│
├── [0] Endgame override
│        if (n == 109 && classify(opp1) == SAFE && classify(opp2) == SAFE)
│            return DEFECT
│
├── [1] Classifier (pure function of histories, recomputed each round)
│        classify(oppHistory, allHistories) → {SAFE, HOSTILE, EXPLOITABLE, AMBIGUOUS}
│
└── [2] Joint response (pessimistic rule)
         pessimistic(class1, class2) → {COOPERATE, DEFECT, TFT-worse}
```

**Stateless Player instance:** All logic is derived from the history arrays passed in by the harness. No fields on the Player object persist across `selectAction` calls. Makes the strategy deterministic given opponents and trivial to audit.

## 5. Classifier

For each opponent, classify using the first matching rule:

### Definitions

- `unprovoked_defection(opp, r)`: `opp[r] == D` AND no player (me, other opp, opp itself) defected in round `r−1` or `r−2`.
- `unprovoked_defects(opp)` = count of unprovoked defections in opp's history.
- `unprovoked_defects_in_rounds_0_to_10(opp)` = count of unprovoked defections whose round index is in [0, 10]. A property of history (monotonic in n).
- `total_defects_in_match` = total defections by anyone up to round n-1.
- `rounds_since_defect(opp)` = n − (last defection round of opp), or infinity if none.

### Rules (evaluated in order)

```
if n == 0:                             return AMBIGUOUS      # no history yet

# HOSTILE — strict-fast for early aggression (fires even at low n)
if opp defected at round 0:            return HOSTILE        # H1 (round 0 is always unprovoked)
if unprovoked_defects(opp) >= 2:       return HOSTILE        # H2
if unprovoked_defects_in_rounds_0_to_10(opp) >= 1:
                                       return HOSTILE        # H3

# EXPLOITABLE — requires observed non-retaliation (needs defection history)
if total_defects_in_match >= 1
   and rounds_since_any_defect >= 5
   and opp has 0 defections total:     return EXPLOITABLE    # E1
if opp cooperated 20+ consecutive rounds following any defection in the match:
                                       return EXPLOITABLE    # E2

# SAFE — clean or provoked-only (needs time to accumulate evidence)
if opp's only defections were provoked and rounds_since_defect(opp) >= 3:
                                       return SAFE           # S1
if opp has 0 defections and n >= 8:    return SAFE           # S2

return AMBIGUOUS
```

**Ordering invariant:** HOSTILE rules fire first and at any `n ≥ 1`, so a round-0 defector is classified HOSTILE at round 1 immediately. The SAFE rules require `n ≥ 8` or elapsed-since-defection evidence, which naturally gates them to later rounds.

### Monotonicity (emergent from rule design)

The classifier is a pure function of history. As n grows, history only grows, so some transitions are provably one-way:

- **HOSTILE is permanent.** The HOSTILE-firing conditions (H1, H2, H3) are all properties of accumulated history that can never be "undone" — once fired, they keep firing at every future round. This protects against BackStabber-style opponents who defect early then feign cooperation.
- **EXPLOITABLE is permanent.** E1 and E2 are also history-monotonic.
- **SAFE is not permanent.** An opponent classified SAFE at round 10 can become HOSTILE at round 15 if they commit a second unprovoked defection (triggers H2). AMBIGUOUS can similarly become HOSTILE. These transitions are intentional: the classifier responds to new evidence.
- **AMBIGUOUS** is the fallback when no rule fires; it can move to any class as evidence accrues.

## 6. Joint Response (Pessimistic Rule)

| c1 \ c2        | SAFE      | AMBIGUOUS | EXPLOITABLE | HOSTILE |
|----------------|-----------|-----------|-------------|---------|
| **SAFE**       | Cooperate | TFT-worse | Defect      | Defect  |
| **AMBIGUOUS**  | TFT-worse | TFT-worse | Defect      | Defect  |
| **EXPLOITABLE**| Defect    | Defect    | Defect      | Defect  |
| **HOSTILE**    | Defect    | Defect    | Defect      | Defect  |

**TFT-worse:** `return max(opp1.last, opp2.last)` — mirror the more aggressive of the two opponents' last action. Falls back to Cooperate when n == 0 (no last action).

**Rule summary:** Defect if either opponent is HOSTILE or EXPLOITABLE. Cooperate only when both are SAFE. Otherwise TFT-worse.

## 7. Endgame Rule

```
if (n == 109 && classify(opp1) == SAFE && classify(opp2) == SAFE)
    return DEFECT;
```

Evaluated before the joint-response step. Triggers on round index 109, which is provably the last round the game can have (max rounds = 110, indices 0..109). Against jointly-SAFE opponents, defecting here yields +2 with zero retaliation window.

Endgame defection is NOT extended to round 108 (P(last)=40%) because the expected-value gain (+0.2) is marginal and the complexity cost of reasoning about cascade effects outweighs it.

## 8. Edge Cases

1. **n == 0:** Cooperate. Hardcoded via AMBIGUOUS → TFT-worse of empty history → Cooperate.
2. **n ∈ {1, 2}:** AMBIGUOUS → TFT-worse. Effectively TFT behavior in early rounds.
3. **Self-match (Me, Me, Me):** Three identical instances classify each other SAFE by round 8 (S2), mutual-C until round 109, then all three defect simultaneously. Score per round ≈ 5.96.
4. **Pair with duplicate (Me, Me, X):** Both Me-instances classify X independently. If X is SAFE, mutual-C and endgame defection as in the self-triple case. If X is HOSTILE (e.g., defects at round 0), then at round 1 both Me-instances see X=HOSTILE via H1, the other Me=AMBIGUOUS (no evidence yet), and the pessimistic rule forces both to defect. From round 1 onwards each Me-instance's defections are provoked (X defected in a recent round), so they stay classified AMBIGUOUS for each other and keep defecting via the pessimistic rule. Result: DDD=2/round for everyone from round 1 onwards. Unavoidable — in 3-player with one HOSTILE opponent, a single action can't simultaneously cooperate with one opp and defect against another.
5. **Opp1 defects at round 0:** H1 fires → HOSTILE for opp1, AMBIG/SAFE for opp2, pessimistic rule → Defect from round 1 onwards.
6. **Noisy-looking defection at round 30 from opp1:** Unprovoked, single occurrence, n > 10 → does NOT trigger H3 (window expired) or H2 (count < 2). Opp1 stays AMBIGUOUS; we TFT-worse. If they defect a second time unprovoked, H2 fires and they become HOSTILE.

## 9. Testing Plan

### 9.1 Unit tests (extend `mainTest()` in `ThreePrisonersDilemma.java`)

- Round 0 returns C.
- Round-0 defection by opp ⇒ HOSTILE (H1).
- Two unprovoked defections ⇒ HOSTILE (H2).
- n=109 with joint SAFE ⇒ D.
- n=108 with joint SAFE ⇒ C.
- Joint SAFE+HOSTILE ⇒ D.
- Joint SAFE+EXPLOITABLE ⇒ D.
- Joint AMBIG+AMBIG where both last-played C ⇒ C.
- Joint AMBIG+AMBIG where opp1 last-played D ⇒ D.

### 9.2 Head-to-head sanity checks (via `scoresOfMatch`)

- vs (TftSpiteful, TftSpiteful): expect avg ≥ 6.01/round (mutual-C + 2 bonus at 109)
- vs (Nasty, Nasty): expect avg ≈ 2.0 (mutual-D)
- vs (Nice, Nice): expect avg ≈ 6.0 (pure mutual-C; EXPLOITABLE never triggers because no defection happens)
- vs (Nice, Nasty): Nasty ⇒ HOSTILE, Nice ⇒ eventually EXPLOITABLE once Nasty defects and Nice doesn't retaliate. We defect. Per-round ≈ 5 (DDC pattern).
- vs (Grim, Grim): mutual-C forever, then our round-109 defection locks their grim trigger — but there's no round 110. Safe. Avg ≈ 6.01.

### 9.3 Full tournament run

- `java ThreePrisonersDilemma` with `NUM_TOURNAMENTS = 500`
- Target: top-3 by avg score; absolute score ≥ 2193
- If underperforming: log classifications at rounds {5, 10, 30, 60, 90, 109} for 5 random triples and diagnose

### 9.4 Do-not-tune discipline

Once the unit tests pass and the full tournament run shows top-3 placement, **stop tuning**. Further tuning against the zoo overfits to this specific field and loses robustness against the real submission field. Freeze and ship.

## 10. Report Outline (30% of grade)

1. **Problem setup** — 3-player IPD, payoff matrix, tournament structure
2. **Design principle** — classify-then-respond; Axelrod's four properties (nice, retaliatory, forgiving, clear)
3. **Why classifier over pure TFT/Grim** — ability to detect EXPLOITABLE opponents and perform endgame defection
4. **Why passive observation over active probing** — no cost against peaceful matches (the dominant scoring regime)
5. **Why pessimistic joint rule** — asymmetric downside of being exploited in 3-player
6. **Classification rules with rationale** — H1/H2/H3, E1/E2, S1/S2
7. **Endgame defection math** — P(last-round) derivation, EV computation, justification for 109-only
8. **Evaluation** — head-to-head scores vs every strategy in the local zoo; 500-tournament aggregate
9. **Limitations** — cannot distinguish ALLC from TFT without a defection occurring; HOSTILE is permanent (trades recovery for robustness); no noise adaptation (appropriate — tournament is noiseless)

## 11. Non-Goals

- No offline training, no ML weights, no GA search. The strategy is hand-designed and interpretable.
- No handshake or self-identification protocol. The strategy does not attempt to detect its own clones.
- No probing defection. Passive-only by design.
- No adaptation to noise (tournament is noiseless).
- No per-opponent action (single action per round by harness contract).

## 12. Open Questions / Risks

- **Risk:** Misclassification of a Pavlov-like opponent that defects once due to its `lose-shift` logic after an unlucky pattern. Mitigation: the H3 window closes at round 10; beyond that, a single defection is tolerated (AMBIGUOUS).
- **Risk:** A student submission that is intentionally weird (e.g., cooperates for 10 rounds then defects always). Will be classified SAFE, then AMBIGUOUS/HOSTILE after first defection. Worst case: we eat 1–3 suboptimal rounds before switching. Acceptable.
- **Risk:** A student submission that aggressively exploits peaceful round-109 defection (defects at round 108 knowing we'll defect at 109). Would cost us ~3 points per affected match. Acceptable tradeoff; fixing requires cascading endgame logic that bloats the strategy.
