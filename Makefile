.PHONY: run test bench clean

BENCH_N ?= 1000

run: ThreePrisonersDilemma.class
	java ThreePrisonersDilemma

test: ThreePrisonersDilemma.class
	java ThreePrisonersDilemma --test

bench:
	@tmpdir=$$(mktemp -d); \
	sed 's/\(static final int NUM_TOURNAMENTS = \)[0-9]*/\1$(BENCH_N)/' \
	    ThreePrisonersDilemma.java > "$$tmpdir/ThreePrisonersDilemma.java"; \
	javac "$$tmpdir/ThreePrisonersDilemma.java" -d "$$tmpdir" \
	    && java -cp "$$tmpdir" ThreePrisonersDilemma; \
	rm -rf "$$tmpdir"

ThreePrisonersDilemma.class: ThreePrisonersDilemma.java
	javac ThreePrisonersDilemma.java

clean:
	rm -f *.class
