# Release readiness for M3JDK21 (Java 21): eclipse.jdt.ui

Measured on 2026-11-09 against fork master `85eadeb77ddc527f0511b12639d912eddaa78624` (upstream master `41599a70e07f`).
Tools and raw evidence: [`docs/m3/jdk21/release-20261109`](../jdk21/release-20261109/). Every gate ran locally on CPU; fork CI runs are not counted
(startup failures and queue starvation on this account, 2026-11).

| # | Gate | Verdict | Evidence |
| --- | --- | --- | --- |
| G1 | No Java 22+ requirement on master: BREE above JavaSE-21 (0), build/IDE pins above 21 (0), unnamed `_` (0), `import module` (0) | PASS | CENSUS.tsv (code view; hits inside strings, text blocks and comments: CENSUS_MASKED.tsv) |
| G2 | Java-21 compile gate: JDK 25 javac `--release 21` minus `--release 25` over 12 bundles (3745 files, 24 roots); 1060 baseline rows cancel | FAIL on master (residue 2, GATE_RESIDUE_MASTER.tsv); PASS with this PR (residue 0) | GATE_RESIDUE.tsv, GATE_SUMMARY.tsv |
| G2b | Parse gate: every tracked Java source (3876 files, test bundles included, test-input folders excluded), JDK 25 javac `--release 21` stopped after parsing | FAIL on master (10 rows, PARSE_GATE_MASTER.tsv); PASS with this PR | PARSE_GATE.tsv |
| G3 | Recipe fixed point (`apply_jdk21.sh` + `apply_jdk21_pins.sh` change nothing on master) | PASS | RECIPE_CHANGES.tsv |
| G4 | Metadata consistent: Tycho bundle/feature versions match their pom, no EE above 21 in a Java-21 bundle | PASS | META.tsv |
| G5 | Full Tycho build of this repository on JDK 21 (`mvn -B clean verify -DskipTests`, fork parent + SDK target from the fork aggregator, other components from the upstream 4.42 I-build repository) | FAIL | BUILD FAILURE in 246 s, modules 9/10 ok, 1 failed, 13 [ERROR] rows (BUILD_G5.tsv, BUILD_G5_ERRORS.tsv, BUILD_G5.log.gz) |
| G6 | Eclipse SDK product build and launch on JDK 21 | PENDING | aggregator verdict, eclipse.platform.releng.aggregator docs/m3/release/RELEASE_READINESS.md |

## What this PR changes

Fork master `85eadeb77ddc` carries the 2026-10-20 delta (12 bundles at JavaSE-21, no unnamed `_` in code; the 7 regex
hits are inside text blocks, CENSUS_MASKED.tsv). It does not compile in any Java release in two test bundles: an M3
marks pass wrote `//FLLDCIM:` anchors after opening text-block delimiters (`""" //FLLDCIM: ...`), which javac rejects
("illegal text block open delimiter sequence, missing line terminator", 10 rows in PARSE_GATE_MASTER.tsv). Parser recovery
after that error differs between `--release 21` and `--release 25`, which is the 2-row master residue (GATE_RESIDUE_MASTER.tsv).

Fill (`fix_textblock_anchors.sh` + `textblock_anchor.awk`, CNPCDA-safe): each anchor moves unchanged to its own line above
the statement; nothing else changes (5 anchors in 4 files: ClassFileEditorTests, MarkdownCommentTests x2, CleanUpTest1d8,
ReplaceInvocationsTests). Gate after the fill: residue 0; parse gate 0 rows over 3876 files.

## G5 (Tycho build on JDK 21.0.9)

NLS pre-check first (`nls_precheck.sh`, ECJ 3.47 over the 8 bundles that compile NLS problems as errors; jdt.ui sets
`suppressOptionalErrors=disabled`, so @SuppressWarnings does not hide them): 12 missing + 7 surplus tags fixed in 11 lines
before attempt 1 (`fix_nls_tags.sh`).

| Attempt | Result |
| --- | --- |
| 1 | BUILD FAILURE: org.eclipse.jdt.ui stopped on 1 ECJ NLS error, TypeInfoViewer 759 `return "" + dashes + fMessage + dashes;` (M3LinearStringBuilderRecipe fold) inside a class-level `@SuppressWarnings("all")`; the first pre-check ran NLS as a suppressible warning and missed it. The pre-check now follows each bundle's `suppressOptionalErrors` and reports it; fixed |

## Re-run

```bash
git clone --depth 1 https://github.com/hsoliwal/eclipse.jdt.ui.git && git -C eclipse.jdt.ui remote add upstream <upstream>
git -C eclipse.jdt.ui fetch --depth 1 --filter=blob:none upstream master:refs/remotes/upstream/master
bash docs/m3/jdk21/release-20261109/gate_master.sh eclipse.jdt.ui    # paths at the top of the script; JAVAC = JDK 25 javac
bash docs/m3/jdk21/release-20261109/meta_check.sh eclipse.jdt.ui > META.tsv
```

The gate compiles every bundle that upstream master declares JavaSE-22 or newer, plus the 2026-10-20 list, against the
newest jar of every bundle in a p2 pool. Errors in both runs are classpath noise (APIs newer than the pool); what remains
after `--release 21` is the Java 22+ residue and must be empty.
