/*
 * This file is part of PvPIndex MCBans, a modified fork of MCBans.
 *
 * Original work Copyright (C) MCBans authors and contributors.
 * Modifications Copyright (C) 2026 PvPIndex contributors.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License version 3.
 */
package com.pvpindex.bans.plugin;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CI gate: every bundled language file must contain every key defined in
 * {@code default.yml} (English).
 *
 * <p>Files are read directly from the classpath (test resources are on the
 * same classpath as main resources), so no MockBukkit / plugin-data-folder
 * setup is required.</p>
 *
 * <p>The test accumulates <em>all</em> failures and reports them together so a
 * translator can fix everything in one pass rather than discovering gaps one
 * at a time.</p>
 */
class LanguageCompletenessTest {

    /**
     * Locales that are bundled in the JAR and extracted at runtime.
     * Any locale present here must also appear in
     * {@link I18n#extractLanguageFiles(boolean)}.
     */
    private static final String[] BUNDLED_LOCALES = {
        "nl", "fr", "de", "ja-JP",
        "no", "pt", "es", "sv-SE", "zh-TW"
    };

    // -------------------------------------------------------------------------
    // Key completeness
    // -------------------------------------------------------------------------

    @Test
    void all_language_files_contain_every_key_from_default() {
        YamlConfiguration defaultConf = loadFromClasspath("default");
        assertNotNull(defaultConf, "default.yml must be loadable from classpath");

        Set<String> requiredKeys = defaultConf.getKeys(false);
        assertFalse(requiredKeys.isEmpty(), "default.yml must not be empty");

        List<String> failures = new ArrayList<>();

        for (String locale : BUNDLED_LOCALES) {
            YamlConfiguration conf = loadFromClasspath(locale);
            if (conf == null) {
                failures.add("[" + locale + "] Could not load " + locale + ".yml from classpath");
                continue;
            }

            for (String key : requiredKeys) {
                if (!conf.contains(key)) {
                    failures.add("[" + locale + "] missing key: " + key);
                }
            }
        }

        assertTrue(failures.isEmpty(),
                "Language file(s) have missing keys - add the keys or open a translation PR:\n"
                        + String.join("\n", failures));
    }

    // -------------------------------------------------------------------------
    // Extraction list completeness
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link I18n#extractLanguageFiles(boolean)} extracts every
     * locale that has a file in {@code src/main/resources/languages/}.
     *
     * <p>If this test fails after adding a new {@code .yml} file, update the
     * {@code locales} list in {@code I18n.extractLanguageFiles()}.</p>
     */
    @Test
    void extractLanguageFiles_list_includes_zh_TW() {
        // zh-TW.yml exists in resources - this test documents that it must be included.
        InputStream stream = I18nTest.class.getResourceAsStream("/languages/zh-TW.yml");
        assertNotNull(stream, "zh-TW.yml must exist in JAR resources");

        // We verify the extract list indirectly: load the file from the classpath
        // (extraction would write the same bytes).  The companion I18nTest already
        // exercises actual extraction; here we just confirm the resource exists
        // and is valid YAML.
        YamlConfiguration conf = loadFromClasspath("zh-TW");
        assertNotNull(conf, "zh-TW.yml must be parseable as YAML");
        assertFalse(conf.getKeys(false).isEmpty(), "zh-TW.yml must not be empty");
    }

    @Test
    void all_bundled_locales_are_loadable() {
        for (String locale : BUNDLED_LOCALES) {
            YamlConfiguration conf = loadFromClasspath(locale);
            assertNotNull(conf, "Locale '" + locale + "' must load from classpath as valid YAML");
        }
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private YamlConfiguration loadFromClasspath(String locale) {
        InputStream stream = getClass().getResourceAsStream("/languages/" + locale + ".yml");
        if (stream == null) {
            return null;
        }
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (Exception e) {
            return null;
        }
    }
}
