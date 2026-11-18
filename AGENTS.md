# Eclipse JDT UI agent instructions

## Clean output rule (owner directive, 2026-11-14) - read first

This rule overrides any older text in this file.

- This fork is clean output: one coherent Eclipse JDT UI repository. M3 work lives
  inside the project's own classes and packages, written the way the project
  writes its code. It is not stamped across folders.
- Do not add `m3/`, `.m3/`, `docs/m3/` or `tools/m3*` folders.
- Do not add any of the following. They belong in `hsoliwal/com.synexia`, the
  convergence workspace:
  - qualification bundles;
  - evidence, receipts and benchmark tables;
  - work orders;
  - donor copies;
  - recipe sources or recipe resources.
- Do not add parallel `M3`-prefixed types or `*.m3` packages where an existing
  class owns the behaviour. Change that class in place, keep it internal unless
  the owner admits new API, and follow the project's naming.
- A pull request here contains only the product change, plus its tests in the
  project's own test layout. Put the proof material in the paired `com.synexia`
  pull request and link to it.
- This fork's merged pull-request history shows the intended direction.



## Priority 0 — full-history superset-merge custody and last-checked rule (2026-11-18)

Every accepted fork evolves as a **backward-compatible optimized API/behavior superset** of applicable historical upstream and fork capabilities. Source/API/ABI, tests, platform/native lifecycle, formats, earlier fixes and measured optimizations must not be silently lost. Evaluate multiple candidate algorithms, old branches and PRs, and choose the best *proven* implementation using correctness, allocation, latency, throughput, concurrency and license compatibility; grow APIs additively. Before any merge, inspect Git ancestry back to the earliest reachable root across all relevant heads/tags, merged/closed/reverted PRs, both merge parents, renames/deletions and previously rejected bad commits; never rebase `develop`. Record per-ref last-checked date, exact SHA, scan scope/oldest ancestor, PR/branch coverage, previous verified SHA and findings in the canonical Synexia checkpoint ledger. A HEAD snapshot is only PARTIAL/PENDING. Use the established Synexia Maven/OpenRewrite recipes, exact-source history-loss and target compiler/behavior/native gates in accordance with each repository role. Prefer Apache-2.0 for **new separable first-party** code, retain applicable upstream/donor licenses and NOTICE, and never treat LLM-assisted atomization/patternization as automatic relicensing. Canonical instructions and explicitly PENDING 236-repo inventory: [com.synexia PR #18764](https://github.com/hsoliwal/com.synexia/pull/18764).



## Superset merge and history-audit rule (2026-11-18)

Read [.github/MERGE_RULE.md](.github/MERGE_RULE.md) before changing an existing implementation or integrating any branch or PR.
Preserve the supported additive API/behavior superset and select implementations by correctness, predictability, measured performance, then elegance.
Audit all reachable history back to every root, all branches/tags and all PR states for lost code or regressions; preserve append-only history.
Record actual UTC coverage and remaining work in [.github/HISTORY_AUDIT.tsv](.github/HISTORY_AUDIT.tsv); policy installation is not a completed source-history audit.
Prefer verified Apache-2.0 sources and independent implementations while preserving all existing third-party rights; LLM rewriting does not remove license obligations.
Detailed private recipe/proof records remain in canonical Synexia; the compact local policy and checkpoint make the rule discoverable here.
<!-- MERGE-SUPERSET-HISTORY-20261118-2 -->

## MOP SDK cross-repository agent read gate (2026-11-18)
<!-- MOP-AGENT-2026-11-18-1 -->
Before any source analysis, refactor, PR, merge, licensing decision, or history checkpoint, also read [AGENT.md](AGENT.md). It governs bidirectional atomization/patternization, documented contract-sealed atoms, additive best-of-best API supersets, complete available-history loss checks, dated last-verified proof, Apache-2.0 eligibility and upstream rights. Existing stronger project-specific and locked-canon constraints remain in force. Installation of this pointer does not establish that a source or historical audit has run.
