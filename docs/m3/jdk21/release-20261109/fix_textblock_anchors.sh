#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
#
# CNPCDA repair over a fork: every Java file with a `//FLLDCIM:` anchor after an opening text-block delimiter gets
# the anchor on its own line above (textblock_anchor.awk). Usage: fix_textblock_anchors.sh <worktree>
# Prints: anchor TAB path TAB moved-anchors. Fixed point.
set -euo pipefail
W="$1"; H=$(cd "$(dirname "$0")" && pwd); export LC_ALL=C
{ git -C "$W" grep -l -P '"""[ \t]*//FLLDCIM:' -- '*.java' || true; } | while read -r f; do
	p="$W/$f"; n=$(awk -v BINMODE=3 -v report=1 -f "$H/textblock_anchor.awk" "$p" | wc -l)
	[ "$n" -gt 0 ] || continue
	awk -v BINMODE=3 -f "$H/textblock_anchor.awk" "$p" > "$p.tba.tmp" && mv "$p.tba.tmp" "$p" && printf 'anchor\t%s\t%s\n' "$f" "$n"
done
