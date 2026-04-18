# Three-Player Iterated Prisoner's Dilemma

A tournament simulator for the three-player Prisoner's Dilemma, written in Java. 36 strategies battle it out across thousands of rounds — from canonical game theory classics to movie-themed chaos agents.

Built for fun with a friend.

---

## The Game

Three players simultaneously choose to **Cooperate (0)** or **Defect (1)** each round. Payoffs depend on all three players' choices:

| Your action | Both opponents | Payoff |
|-------------|---------------|--------|
| Defect | Both cooperate | **8** (Temptation) |
| Cooperate | Both cooperate | **6** (Reward) |
| Defect | One defects | **5** |
| Cooperate | One defects | **3** |
| Defect | Both defect | **2** (Punishment) |
| Cooperate | Both defects | **0** (Sucker) |

Each match runs for a random number of rounds (90–110). Every unordered triple of players competes once per tournament — 8,436 matches total with 36 players.

---

## Strategies

### Baselines
| Player | Strategy |
|--------|----------|
| NicePlayer | Always cooperate |
| NastyPlayer | Always defect |
| RandomPlayer | 50/50 coin flip |
| TolerantPlayer | Defect only if total opponent defections > cooperations |
| FreakyPlayer | Randomly commits to all-C or all-D at round 0 |
| T4TPlayer | Tit-for-Tat |

### Research-Backed
| Player | Source |
|--------|--------|
| GenerousTfT | Nowak & Sigmund 1993 — TFT with 10% forgiveness |
| GradualPunisher | Beaufils et al. 1996 — Escalating punishment + peace signal |
| GrimTrigger | Axelrod 1984 — Cooperate until first defection, then defect forever |
| TftSpiteful | JASSS 2017 #1 — TFT with grim trigger after 2 defection events |
| OmegaTFT | Slany & Kienreich 2000 — TFT + deadlock breaker + randomness detector |
| AdaptivePavlov | Li 2007 — Classify opponents in first 6 rounds, respond per class |
| BackStabber | Harper et al. 2024 — Cooperate until round 88, then defect |
| EvolvedANN | Harper et al. 2017 — ANN trained offline via evolutionary algorithm |
| ExtortionZD | Taha & Ghoneim 2021 — Zero-determinant extortion strategy |
| EqualizerZD | Taha & Ghoneim 2021 — Zero-determinant equalizer |
| + 9 more | See [PLAYERS.md](PLAYERS.md) |

### Trolls (our own creations)
| Player | What it does |
|--------|-------------|
| AsylumPlayer | Inverted Gradual Punisher — rewards defection, punishes cooperation |
| HallucinationPlayer | TFT but against a randomly recalled past round |
| DrunkenPlayer | GradualPunisher that gets progressively more random as rounds go on |

### Exhibition (movie-themed)
| Player | Based on |
|--------|---------|
| NostalgicPlayer | *Midnight in Paris* — tracks each opponent's "golden era" |
| LaLaLandPlayer | *La La Land* — six seasonal chapters with different strategies |
| FistBumpPlayer | *Project Hail Mary* — bonds with the first cooperative opponent |
| XenolinguistPlayer | *Project Hail Mary* — probe and classify, then commit |
| FiveHundredDaysPlayer | *500 Days of Summer* — holds opponents to 90% cooperation baseline |
| MillersPlanetPlayer | *Interstellar* — reacts with a 7-round time delay |
| PlanABPlayer | *Interstellar* — switches to permanent defect if round-50 average < 4.0 |
| BiancasBanPlayer | *10 Things I Hate About You* — ignores one opponent entirely |

---

## Running

**Requirements:** Java (any modern version)

```bash
# Compile
javac ThreePrisonersDilemma.java

# Run tournament (100 rounds by default)
java ThreePrisonersDilemma

# Run sanity checks
java ThreePrisonersDilemma --test

# Benchmark (1000 tournaments)
make bench
```

---

## Scoring

- Each player's score per tournament = sum of average payoffs across all matches played
- Ranking points: 1st place = 36 pts, 2nd = 35, ..., 36th = 1 pt
- Final leaderboard shows average score and cumulative ranking points over all runs

---

## Results (100 tournaments)

```
Rank  Player                    Avg Score   Win Points
   1  EvolvedANNNoisePlayer      3575.67        3397
   2  OmegaTFTPlayer             3575.20        3411
   3  TftSpitefulPlayer          3573.95        3386
   4  DBSPlayer                  3561.61        3057
   5  GrimTriggerPlayer          3558.42        2951
  ...
  32  NastyPlayer                2245.34         500
  33  ExtortionZDPlayer          2186.34         400
  34  EqualizerZDPlayer          1904.16         299
  35  RandomPlayer               1832.13         201
  36  AsylumPlayer               1584.56         100
```

---

## Files

| File | Description |
|------|-------------|
| `ThreePrisonersDilemma.java` | All 36 strategies + tournament engine |
| `PLAYERS.md` | Detailed writeup of every player with design rationale |
| `STRATEGY_RESEARCH.md` | Literature survey of top IPD strategies |
| `Makefile` | Build and benchmark targets |
