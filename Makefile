.PHONY: run test bench clean help

BENCH_N ?= 1000

help:
	@echo "Targets:"
	@echo "  make run             Compile + run one tournament"
	@echo "  make test            Compile + run sanity and integration tests"
	@echo "  make bench           Benchmark over BENCH_N tournaments (default 1000)"
	@echo "  make bench BENCH_N=N Override benchmark tournament count"
	@echo "  make clean           Remove compiled .class files"

run: ThreePrisonersDilemma.class
	java ThreePrisonersDilemma

test: ThreePrisonersDilemma.class
	java ThreePrisonersDilemma --test

bench:
	@set -eu; \
	tmpdir=$$(mktemp -d); \
	trap 'rm -rf "$$tmpdir"' EXIT INT TERM; \
	sed 's/\(static final int NUM_TOURNAMENTS = \)[0-9]*/\1$(BENCH_N)/' \
	    ThreePrisonersDilemma.java > "$$tmpdir/ThreePrisonersDilemma.java"; \
	javac "$$tmpdir/ThreePrisonersDilemma.java" -d "$$tmpdir"; \
	echo "==> Running bench with BENCH_N=$(BENCH_N)"; \
	time java -cp "$$tmpdir" ThreePrisonersDilemma

ThreePrisonersDilemma.class: ThreePrisonersDilemma.java
	javac ThreePrisonersDilemma.java

clean:
	rm -f *.class
