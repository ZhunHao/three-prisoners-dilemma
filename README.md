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

## Results (1000 tournaments)

```
Rank  Player                    Avg Score   Win Points
   1  EvolvedANNNoisePlayer      3576.358       34199
   2  OmegaTFTPlayer             3574.994       33892
   3  TftSpitefulPlayer          3571.616       33244
   4  DBSPlayer                  3560.053       30042
   5  GrimTriggerPlayer          3559.528       29899
   6  SpitefulCCPlayer           3556.533       28915
   7  TitForTwoTatsPlayer        3555.105       28551
   8  FiveHundredDaysPlayer      3554.923       28472
   9  ContriteTFTPlayer          3551.673       27363
  10  PlanABPlayer               3550.873       27117
  11  T4TPlayer                  3550.444       26979
  12  Mem2Player                 3549.923       26796
  13  HardTFTPlayer              3549.294       26502
  14  MajorityRulePlayer         3548.663       26412
  15  MillersPlanetPlayer        3545.368       25366
  16  FistBumpPlayer             3528.873       21566
  17  AdaptivePavlovPlayer       3515.326       19569
  18  TolerantPlayer             3509.717       18961
  19  HallucinationPlayer        3508.383       18825
  20  GenerousTfTPlayer          3489.650       17314
  21  BiancasBanPlayer           3451.834       15752
  22  GradualPunisherPlayer      3438.152       15041
  23  LaLaLandPlayer             3421.704       14216
  24  NostalgicPlayer            3367.814       13000
  25  PavlovPlayer               3315.448       12007
  26  NicePlayer                 3216.108       10951
  27  BackStabberPlayer          3178.125       10049
  28  EvolvedANNPlayer           3036.442        9000
  29  FreakyPlayer               2713.032        8000
  30  DrunkenPlayer              2416.940        6602
  31  XenolinguistPlayer         2410.966        6398
  32  NastyPlayer                2246.135        4999
  33  ExtortionZDPlayer          2187.587        4001
  34  EqualizerZDPlayer          1902.558        2992
  35  RandomPlayer               1831.054        2008
  36  AsylumPlayer               1586.424        1000
```

---

## Files

| File | Description |
|------|-------------|
| `ThreePrisonersDilemma.java` | All 36 strategies + tournament engine |
| `PLAYERS.md` | Detailed writeup of every player with design rationale |
| `STRATEGY_RESEARCH.md` | Literature survey of top IPD strategies |
| `Makefile` | Build and benchmark targets |
