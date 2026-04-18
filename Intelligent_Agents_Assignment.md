# SC4003/CZ4046/CE4046: Intelligent Agents

## Assignment 2 (Due April 22 11:59:59PM)

---

## Repeated Prisoners Dilemma

As you all know, in the game the Prisoners' Dilemma, the dominant strategy equilibrium is for agents to defect, even though both agents would be best off cooperating. If we move to a repeated version of the Prisoners' Dilemma, then this lack of cooperation can possibly disappear.

In a repeated game, a given game (often thought of in normal form) is played multiple times (possibly infinitely many times) by the same set of players. We compute the (average) reward of a player in a repeated game, to be (*r*_j is the player's payoff in round *j*)

$$\lim_{k \to \infty} \sum_{j=1}^{k} r_j / k \tag{1}$$

A strategy in a repeated game specifies what action the agent should take in each stage of the game, given all the actions taken by all players in the past. For example, one strategy in the Prisoner's Dilemma is Tit-for-Tat (TfT). In this strategy, the agent starts by cooperating, and thereafter chooses in round *j* + 1 the same action that the other agent chose in round *j*. If both agents play TfT then we have an equilibrium (with certain additional conditions). However, this is not the only strategy that agents might consider playing, in fact there are infinitely many strategies which agents may consider. For example, in the Trigger strategy, an agent starts by cooperating but if the other player ever defects then the first defects forever. The Trigger strategy forms a Nash equilibrium both with itself and with TfT.

In this assignment you will develop a strategy for an agent in a *three player* repeated prisoners' dilemma. In this simulation, triples of players will play each other repeatedly in a 'match'. A match consists of about 100 rounds, and your score from that match is the average of the payoffs from each round of that match. For each round, your strategy is given a list of the previous plays (so you can remember what your opponent did) and must compute the next action. We represent cooperation by the integer 0, and defection by the integer 1.

Your 'strategy' will be a code fragment that looks at the previous plays (by you and your opponents) for that match, and computes your next play. For example, here is a code fragment that makes random plays. (Here *n* is the number of rounds elapsed so far.)

```java
class RandomPlayer extends Player {
    // RandomPlayer randomly picks his action each time
    int selectAction(int n, int[] myHistory, int[] oppHistory1, int[] oppHistory2) {
        if (Math.random() < 0.5)
            return 0; // cooperates half the time
        else
            return 1; // defects half the time
    }
}
```

The source code contains more elaborate examples and the actual code to run the tournament. To run the code at a UNIX console, type `javac ThreePrisonersDilemma.java` and then `java ThreePrisonersDilemma`.

---

## Submission and Evaluation

The assignment should be submitted through **ntulearn** website by April 22 11:59:59PM. Each submission must consist of the following two attachments:

- A report in PDF format. The report should briefly describe your design of agent. It is better for you to include evaluation of your agent against the example strategies included in the source code. The name of the report file should be in the form of **lastname_firstname_Player.pdf**.

- Your code fragment, which must be a single class like the above snippet. We will run the players against each other (we will try all the combinations) in a tournament. It is better to comment your code so that we could easily understand your code. The name of the code archive should be **lastname_firstname_Player.java**.

Grades will be determined by **how well a player does (70 percent) while competing with agents designed by all other students (we will try all the combinations), as well as the clarity of the report and the code (30 percent).**

**IMPORTANT:** Make sure that you format your Java file correctly! The file should (only) contain a class **lastname_firstname_Player** which extends the player class and implements `selectAction`. The tournaments will be run automatically based on this signature and failing to follow this format will lead to losing some amount of points.

---

## Plagiarism Policy

Your work for this course must be the result of your own individual effort. While you are allowed to discuss problems with your classmates, you must not blatantly copy others' solutions.

---

## Questions

If you have questions, please email Teng Yao Long (email: YAOLONG001@e.ntu.edu.sg).
