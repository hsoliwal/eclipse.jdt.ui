# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
#
# CNPCDA repair: a `//FLLDCIM:` anchor written after an opening text-block delimiter (`""" //FLLDCIM: ...`) makes the
# file illegal in every Java release ("illegal text block open delimiter sequence, missing line terminator").
# The anchor moves, unchanged, to its own line above the statement (same indentation); nothing else changes.
# Same lexer states as recipe/unnamed_to_named.awk; CR bytes kept (run with -v BINMODE=3).
# Usage: awk -v BINMODE=3 [-v report=1] -f textblock_anchor.awk File.java   (report=1: print LINE TAB anchor only)
BEGIN { inBlock = 0; inText = 0 }
{
	line = $0; n = length(line); i = 1; hit = 0
	cr = (substr(line, n, 1) == "\r") ? "\r" : ""
	while (i <= n) {
		c = substr(line, i, 1); c2 = substr(line, i, 2); c3 = substr(line, i, 3)
		if (inBlock) { if (c2 == "*/") { i += 2; inBlock = 0 } else i++; continue }
		if (inText) { if (c == "\\") { i += 2; continue }; if (c3 == "\"\"\"") { i += 3; inText = 0 } else i++; continue }
		if (c2 == "//") break
		if (c2 == "/*") { i += 2; inBlock = 1; continue }
		if (c3 == "\"\"\"") {
			rest = substr(line, i + 3)
			if (match(rest, /^[ \t]*\/\/FLLDCIM:/)) { hit = i + 3; break }
			i += 3; inText = 1; continue
		}
		if (c == "\"" || c == "'") { q = c; i++
			while (i <= n) { d = substr(line, i, 1); if (d == "\\") { i += 2; continue }; i++; if (d == q) break }
			continue }
		i++
	}
	if (!hit) { if (!report) printf "%s%s", $0, RT; next }
	code = substr(line, 1, hit - 1); sub(/[ \t]+$/, "", code); anchor = substr(line, hit); sub(/^[ \t]+/, "", anchor); sub(/\r$/, "", anchor)
	if (report) { print FNR "\t" anchor; inText = 1; next }
	match(line, /^[ \t]*/); ind = substr(line, 1, RLENGTH)
	printf "%s%s\n%s%s%s", ind, anchor cr, code, cr, RT
	inText = 1  # the delimiter opened a text block
}
