run:
	javac ThreePrisonersDilemma.java
	java ThreePrisonersDilemma

bench:
	@tmpdir=$$(mktemp -d); \
	sed 's/NUM_TOURNAMENTS = 100/NUM_TOURNAMENTS = 1000/' ThreePrisonersDilemma.java > "$$tmpdir/ThreePrisonersDilemma.java"; \
	cd "$$tmpdir" && javac ThreePrisonersDilemma.java && java ThreePrisonersDilemma; \
	rm -rf "$$tmpdir"

clean:
	rm -f *.class
