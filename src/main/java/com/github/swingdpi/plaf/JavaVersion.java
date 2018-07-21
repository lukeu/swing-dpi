package com.github.swingdpi.plaf;

public class JavaVersion {

    // Some number far bigger than those we know about, if we don't recognise the version format
    private static final int DEFAULT_VERSION = 90;
    private static final int FOUND_VERSION = parseVersion();

    public static void main(String[] args) {
        System.out.println(System.getProperty("java.version") + " => " + getMajorVersion());
    }


    public static int getMajorVersion() {
        return FOUND_VERSION;
    }

    private static int parseVersion() {
        try {
            String[] parts = System.getProperty("java.version").split("[\\.\\-]");
            for (int i = 0; i < parts.length; ++i) {
                if (!"1".equals(parts[i])) {
                    return Integer.parseInt(parts[i]);
                }
            }
        } catch (NullPointerException ignored) {
            // Ignore
        } catch (NumberFormatException ignored) {
            // Ignore
        }
        return DEFAULT_VERSION;
    }

    public static boolean isDpiAware() {
        return FOUND_VERSION >= 9;
    }
}
