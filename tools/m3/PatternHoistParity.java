// SPDX-License-Identifier: EPL-2.0
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Differential proof for three constant-regex hoists in JDT UI. */
public final class PatternHoistParity {
    private static final Pattern NUMERIC_SUFFIX = Pattern.compile("[0-9]+$");
    private static final Pattern MODULE_NAME = Pattern.compile("\\s*module\\s*(\\w*).*");
    private static final Pattern TEMPLATE_VARIABLE = Pattern.compile("\\$\\{[^\\}]*\\}");
    private PatternHoistParity() {}
    public static void main(String[] args) {
        final Random random = new Random(0x4D3352544748L);
        final String[] parts = {"", "0", "123", "foo", "bar23", "mod", "module", " ", "\t", "$", "$" + "{x}", "$" + "{line:selection}", "}", "हिन्दी", "😀", "\\", "\n", "foo.bar"};
        for (int i = 0; i < 100_000; i++) {
            final StringBuilder source = new StringBuilder();
            for (int j = random.nextInt(8); j >= 0; j--) source.append(parts[random.nextInt(parts.length)]);
            final String text = source.toString();
            final Matcher oldSuffix = Pattern.compile("[0-9]+$").matcher(text);
            final Matcher newSuffix = NUMERIC_SUFFIX.matcher(text);
            if (oldSuffix.find() != newSuffix.find()) throw new AssertionError("numeric find " + i);
            if (oldSuffix.hitEnd() != newSuffix.hitEnd()) throw new AssertionError("numeric end " + i);
            final Matcher oldModule = Pattern.compile("\\s*module\\s*(\\w*).*").matcher(text);
            final Matcher newModule = MODULE_NAME.matcher(text);
            final boolean a = oldModule.matches(), b = newModule.matches();
            if (a != b || (a && !oldModule.group(1).equals(newModule.group(1)))) throw new AssertionError("module " + i);
            final String regex = "\\$\\{[^\\}]*\\}";
            if (!Pattern.compile(regex).matcher(text).replaceAll("").equals(TEMPLATE_VARIABLE.matcher(text).replaceAll(""))) throw new AssertionError("template all " + i);
            if (!Pattern.compile(regex).matcher(text).replaceFirst("").equals(TEMPLATE_VARIABLE.matcher(text).replaceFirst(""))) throw new AssertionError("template first " + i);
        }
        System.out.println("pattern_hoist_parity=PASS cases=100000 patterns=3");
    }
}
