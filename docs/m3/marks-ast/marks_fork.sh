#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Compiler-AST marks (M3MarksPlugin inside the JDK21 gate compile) and CNPCDA anchors for every Java-25 bundle of a
# fork: javac --release 21 with the plugin over the fork's jdk21 worktree (gate args and sources), then the anchors
# on branch m3/marks-tags-20261020 off m3/jdk21-compat-20261020 in a second sparse worktree, pushed, PR opened.
# Usage: marks_fork.sh <repo>   (needs S:/m3-scratch/jdk21/gate/<repo>.args and .sources, worktree jdk21-<repo>)
set -uo pipefail
R="$1"; D=S:/m3-scratch/jdk21; G=S:/m3-scratch/forks/$R.git; J=S:/m3-scratch/worktrees/jdk21-$R; W=S:/m3-scratch/worktrees/marks-$R
P=$D/pass7/forks/$R; mkdir -p "$P"; export LC_ALL=C
CPDIR=$(cygpath -m /tmp/m3p)
{ echo "-processorpath"; echo "\"$CPDIR\""; echo "\"-Xplugin:M3Marks $(cygpath -m $D/crate2) $(cygpath -m $P/marks.tsv)\""; } > "$P/plugin.args"
rm -rf "$P/classes"; mkdir -p "$P/classes"
"C:/Program Files/Java/jdk-21/bin/javac" -J-Xmx6g --release 21 -encoding UTF-8 -nowarn -Xmaxerrs 1000000 -implicit:none \
	-XDshould-stop.ifError=FLOW -XDshould-stop.ifNoError=FLOW -d "$(cygpath -m "$P/classes")" @"$D/gate/$R.args" @"$P/plugin.args" @"$D/gate/$R.sources" > "$P/javac.log" 2>&1
rm -rf "$P/classes"
marks=$(($(wc -l < "$P/marks.tsv") - 1)); echo "$R: $marks marks ($(grep -c ' error: ' "$P/javac.log") javac errors, class-path noise)"
tr '\\' '/' < "$P/marks.tsv" | awk -F'\t' -v OFS='\t' -v j="$R" 'NR > 1 { p = $4; sub("^.*/jdk21-" j "/", "", p); print p, $5, $1, $2, $3 }' | sort -t$'\t' -k1,1 -k2,2n -u > "$P/tags.tsv"
export MSYS_NO_PATHCONV=1
git -C "$G" fetch -q origin "+refs/heads/m3/jdk21-compat-20261020:refs/remotes/origin/m3/jdk21-compat-20261020" 2>/dev/null
if [ ! -d "$W" ]; then git -C "$G" worktree add --no-checkout "$W" -b m3/marks-tags-20261020 refs/remotes/origin/m3/jdk21-compat-20261020 >/dev/null 2>&1
	git -C "$W" config core.autocrlf false; git -C "$W" config core.longpaths true; git -C "$W" sparse-checkout init --no-cone
	{ sed 's|$|/|' "$D/bundles-$R.txt"; echo "docs/m3/marks-ast/"; } | git -C "$W" sparse-checkout set --no-cone --stdin >/dev/null 2>&1
	git -C "$W" checkout -q m3/marks-tags-20261020; fi
cut -f1 "$P/tags.tsv" | sort -u | while IFS= read -r p; do f="$W/$p"; [ -f "$f" ] || continue
	awk -F'\t' -v p="$p" '$1 == p { print $2 "\t" $3 "\t" $4 "\t" $5 }' "$P/tags.tsv" > "$P/.m"
	awk -v BINMODE=3 -v MARKS="$P/.m" -f "$D/pass7/tag_sites.awk" "$f" > "$f.tag" && [ "$(wc -l < "$f.tag")" -eq "$(wc -l < "$f")" ] && cat "$f.tag" > "$f"; rm -f "$f.tag"; done; rm -f "$P/.m"
stat=$(git -C "$W" diff --numstat | awk '$1 != $2 { bad++ } { f++; a += $1 } END { printf "%d files, %d anchored lines, asymmetric %d", f, a, bad + 0 }')
mkdir -p "$W/docs/m3/marks-ast/$R"; cp "$P/marks.tsv" "$P/tags.tsv" "$W/docs/m3/marks-ast/$R/"; cp "$D/pass7/tag_sites.awk" "$D/pass7/marks_fork.sh" "$W/docs/m3/marks-ast/"
kinds=$(awk -F'\t' 'NR > 1 { c[$3]++ } END { for (k in c) printf "%s %d, ", k, c[k] }' "$P/marks.tsv" | sed 's/, $//')
git -C "$W" add -A --sparse
git -C "$W" commit -q -m "M3 marks: inline anchors at every compiler-AST site of the Java-25 bundles of $R

M3MarksPlugin (javac plugin, rule 33: no second AST) marked $marks sites
during the JDK21 gate compile ($kinds). tag_sites.awk appended one CNPCDA
anchor per line and template: $stat; line counts unchanged, fixed point.
Marks: docs/m3/marks-ast/$R." && git -C "$W" push -q origin m3/marks-tags-20261020 2>&1 | grep -E 'error|fatal|rejected'
echo "$R: $stat; $(git -C "$W" log -1 --format=%h)"
gh pr list -R "hsoliwal/$R" --head m3/marks-tags-20261020 --state open --json number --jq '.[0].number' | grep -q . || gh pr create -R "hsoliwal/$R" --base m3/jdk21-compat-20261020 --head m3/marks-tags-20261020 --title "M3 marks: CNPCDA anchors at every compiler-AST site of the Java-25 bundles" --body "$(printf 'Stacked on the Java 21 PR. M3MarksPlugin (javac plugin, rule 33: no second AST) marked %s sites during the gate compile; tag_sites.awk appended one anchor per line and template: //FLLDCIM: MARK — <template> <target> <kind>. %s, line counts unchanged, fixed point. The agent that fixes a file reads what needs fixing in the file itself. Merge after the Java 21 PR; the owner'"'"'s call.' "$marks" "$stat")" 2>&1 | tail -1
