<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 original Java source audit — hsoliwal/eclipse.jdt.ui

Source-sealed Synexia recipe `com.synexia.eclipse.EstateAstEclipseJdtUi20261021`, commit `b77c892d7519c6aa037c2b5cebd8658867ea712b`,
received into the original POM without replacing JDT's original ECJ/DOM,
Java model, refactoring, source generation or Maven/Tycho contracts.
The profile `m3-ast-source-audit-20261021` is opt-in and its only
operation is a local source-pin validation. Default product behavior
is unchanged. This is NOT full M3AST, SWT/JFace or JNI parity.

Run `python3 .m3/m3ast-20261021/verify.py`. Maven profile execution
requires the original toolchain and plugins. A candidate is not promoted
without JavaCompiler/JDT, Maven/Tycho and downstream runtime gates.
