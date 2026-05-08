/*
 * This file is part of PvPIndex MCBans, a modified fork of MCBans.
 *
 * Original work Copyright (C) MCBans authors and contributors.
 * Modifications Copyright (C) 2026 PvPIndex contributors.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License version 3.
 */
package com.pvpindex.bans;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for open issues in the original MCBans repository.
 *
 * <ul>
 *   <li><b>#116 Debug-Messages</b> - the plugin must not use {@code System.out.println}
 *       or {@code System.err.print} in production code; all logging must go through
 *       the plugin's {@link java.util.logging.Logger}.</li>
 *   <li><b>#122 Missing plugin.yml</b> - {@code plugin.yml} must be present in the
 *       compiled JAR (i.e. on the classpath) so Paper / Purpur can load the plugin.</li>
 * </ul>
 */
class IssueRegressionTest {

    // =========================================================================
    // Issue #116 - No System.out.println in production source
    // =========================================================================

    /**
     * Scans every {@code .java} file under {@code src/main/} and asserts that none of them
     * contain an uncommented {@code System.out.println} or {@code System.err.print} call.
     *
     * <p>This is a regression test for issue #116, where the original MCBans had
     * several {@code System.out.println} statements that spammed the server log and
     * caused Paper to nag plugin authors about using the plugin logger.</p>
     */
    @Test
    void no_system_out_println_in_production_sources() throws IOException, URISyntaxException {
        Path srcMain = findSourceRoot("src/main");
        if (srcMain == null) {
            // If we can't locate the source root (e.g. running from a JAR in CI), skip the scan
            return;
        }

        // Matches uncommented System.out.println or System.err.print (not inside a // comment)
        // Uses line-by-line comparison to avoid cross-line false positives from multi-line regex.

        List<String> violations = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(srcMain)) {
            paths.filter(p -> p.toString().endsWith(".java"))
                 .forEach(javaFile -> {
                     try {
                         for (String line : Files.readAllLines(javaFile, StandardCharsets.UTF_8)) {
                             String stripped = line.stripLeading();
                             // Skip single-line comments and block comment continuation lines
                             if (stripped.startsWith("//") || stripped.startsWith("*")) {
                                 continue;
                             }
                             if (stripped.contains("System.out.println(")
                                     || stripped.contains("System.out.print(")
                                     || stripped.contains("System.err.println(")
                                     || stripped.contains("System.err.print(")) {
                                 violations.add(srcMain.relativize(javaFile)
                                         + ": " + stripped.trim());
                             }
                         }
                     } catch (IOException e) {
                         // Ignore unreadable files
                     }
                 });
        }

        assertTrue(violations.isEmpty(),
                "Issue #116: System.out/err.print found in production source files.\n"
                        + "Use plugin.getLogger() instead. Files: " + violations);
    }

    // =========================================================================
    // Issue #122 - plugin.yml must be present in the classpath
    // =========================================================================

    /**
     * Verifies that {@code plugin.yml} is on the classpath.
     *
     * <p>This is a regression test for issue #122, where CI-built jars were missing
     * {@code plugin.yml}, causing Paper to refuse to load the plugin with
     * "does not contain a paper-plugin.yml or plugin.yml".</p>
     */
    @Test
    void plugin_yml_is_present_on_classpath() {
        URL resource = getClass().getClassLoader().getResource("plugin.yml");
        assertNotNull(resource,
                "Issue #122: plugin.yml must be present in the jar/classpath. "
                        + "Check that src/main/resources/plugin.yml is included in the Maven build.");
    }

    /**
     * Verifies that {@code plugin.yml} contains the required {@code main} and {@code name} keys.
     */
    @Test
    void plugin_yml_contains_required_fields() throws IOException {
        URL resource = getClass().getClassLoader().getResource("plugin.yml");
        assertNotNull(resource, "plugin.yml must be present on classpath");

        String content = Files.readString(Path.of(resource.getFile()));
        assertTrue(content.contains("name:"),
                "plugin.yml must declare a 'name' field");
        assertTrue(content.contains("main:"),
                "plugin.yml must declare a 'main' field (fully-qualified class name)");
        assertTrue(content.contains("com.pvpindex.bans"),
                "plugin.yml 'main' field must point to the com.pvpindex.bans package");
        assertFalse(content.contains("com.mcbans"),
                "plugin.yml must not reference the old com.mcbans package");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Attempts to locate a source directory relative to the project root.
     *
     * <p>Walks up from the test's classpath location to find a directory named {@code src}.</p>
     */
    private Path findSourceRoot(String relPath) throws URISyntaxException {
        URL classesUrl = getClass().getProtectionDomain().getCodeSource().getLocation();
        if (classesUrl == null) {
            return null;
        }
        // target/test-classes → target → (project root)
        Path dir = Path.of(classesUrl.toURI()).getParent().getParent();
        Path candidate = dir.resolve(relPath);
        return Files.isDirectory(candidate) ? candidate : null;
    }
}
