package qunar.tc.bistoury.common;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import java.io.File;
import java.util.Locale;

/**
 * @author cai.wen created on 2019/11/11 17:38
 */
public class OsUtils {

    private static final String OS_NAME = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
    private static final String OPERATING_SYSTEM_ARCH = System.getProperty("os.arch").toLowerCase(Locale.ENGLISH);
    private static final String UNKNOWN = "unknown";

    private OsUtils() {
    }

    public static boolean isLinux() {
        return OS_NAME.startsWith("linux");
    }

    public static boolean isWindows() {
        return OS_NAME.startsWith("windows");
    }

    public static boolean isMac() {
        return OS_NAME.startsWith("mac") || OS_NAME.startsWith("darwin");
    }

    public static boolean isSupportPerf() {
        File paranoidFile = new File("/proc/sys/kernel/perf_event_paranoid");
        if (!paranoidFile.exists()) {
            return false;
        }
        try {
            String value = Files.readFirstLine(paranoidFile, Charsets.UTF_8);
            int paranoid = Integer.parseInt(value);
            if (paranoid >= 2) {
                return false;
            }
        } catch (Exception e) {
            throw new RuntimeException("read /proc/sys/kernel/perf_event_paranoid error.", e);
        }
        return true;
    }

    public static boolean isArm() {
        return "arm_32".equals(normalizeArch(OPERATING_SYSTEM_ARCH));
    }

    private static String normalizeArch(String value) {
        value = normalize(value);
        if (value.matches("^(x8664|amd64|ia32e|em64t|x64)$")) {
            return "x86_64";
        }
        if (value.matches("^(x8632|x86|i[3-6]86|ia32|x32)$")) {
            return "x86_32";
        }
        if (value.matches("^(ia64w?|itanium64)$")) {
            return "itanium_64";
        }
        if ("ia64n".equals(value)) {
            return "itanium_32";
        }
        if (value.matches("^(sparc|sparc32)$")) {
            return "sparc_32";
        }
        if (value.matches("^(sparcv9|sparc64)$")) {
            return "sparc_64";
        }
        if (value.matches("^(arm|arm32)$")) {
            return "arm_32";
        }
        if ("aarch64".equals(value)) {
            return "aarch_64";
        }
        if (value.matches("^(mips|mips32)$")) {
            return "mips_32";
        }
        if (value.matches("^(mipsel|mips32el)$")) {
            return "mipsel_32";
        }
        if ("mips64".equals(value)) {
            return "mips_64";
        }
        if ("mips64el".equals(value)) {
            return "mipsel_64";
        }
        if (value.matches("^(ppc|ppc32)$")) {
            return "ppc_32";
        }
        if (value.matches("^(ppcle|ppc32le)$")) {
            return "ppcle_32";
        }
        if ("ppc64".equals(value)) {
            return "ppc_64";
        }
        if ("ppc64le".equals(value)) {
            return "ppcle_64";
        }
        if ("s390".equals(value)) {
            return "s390_32";
        }
        if ("s390x".equals(value)) {
            return "s390_64";
        }

        return UNKNOWN;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "");
    }
}
