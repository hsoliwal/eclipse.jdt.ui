// SPDX-License-Identifier: EPL-2.0
/** Differential proof for literal comma replacement in the JDT UI receiving patch. */
public final class LiteralCommaRegexParity {
    private LiteralCommaRegexParity() {}

    public static void main(String[] args) {
        final java.util.Random random = new java.util.Random(0x4D334A44545549L);
        final String[] atoms = {
            "", ",", " ", ",,", "alpha", "a,b", "x\\n", "\t",
            "हिंदी", "日本語", "😀", "a.b", "$", "\\", "(a,b)"
        };
        int checks = 0;
        for (int i = 0; i < 100_000; i++) {
            final StringBuilder value = new StringBuilder();
            for (int n = random.nextInt(12); n >= 0; n--) {
                value.append(atoms[random.nextInt(atoms.length)]);
            }
            final String input = value.toString();
            final String regex = input.replaceAll(",", ", ");
            final String literal = input.replace(",", ", ");
            if (!regex.equals(literal)) {
                throw new AssertionError("literal comma parity failed at case " + i);
            }
            checks++;
        }
        if (checks != 100_000) {
            throw new AssertionError("incomplete verification");
        }
        System.out.println("literal_comma_parity=PASS cases=" + checks);
    }
}
