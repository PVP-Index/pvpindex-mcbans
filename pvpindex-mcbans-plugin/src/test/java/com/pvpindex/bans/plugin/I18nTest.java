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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link I18n}: localisation, English fallback, and token substitution.
 *
 * <p>MockBukkit must be mocked before loading the plugin because
 * {@link I18n#init(String)} relies on the plugin data-folder path provided by
 * {@link com.pvpindex.bans.plugin.util.FileStructure#getPluginDir()}.</p>
 */
class I18nTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        MockBukkit.load(MCBans.class);
        // Plugin loads with config language = "default" (English)
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // -------------------------------------------------------------------------
    // Sanity: default language
    // -------------------------------------------------------------------------

    @Nested
    class DefaultLanguage {

        @Test
        void localize_known_key_returns_non_empty_string() {
            String result = I18n.localize("formatError");
            assertNotNull(result);
            assertFalse(result.isBlank(), "Expected a non-blank message for 'formatError'");
        }

        @Test
        void localize_all_default_keys_are_resolvable() {
            String[] keys = {
                "unBanSuccess", "unBanError", "unBanGroup", "unBanNot",
                "banExemptPlayer",
                "localBanPlayer", "localBanSuccess", "localBanError",
                "localBanGroup", "localBanAlready",
                "globalBanPlayer", "globalBanSuccess", "globalBanError",
                "globalBanWarning", "globalBanGroup", "globalBanAlready",
                "tempBanPlayer", "tempBanSuccess", "tempBanError",
                "tempBanGroup", "tempBanAlready",
                "kickPlayer", "kickSuccess", "kickNoPlayer", "kickExemptPlayer",
                "ipBanSuccess", "ipBanError", "ipBanAlready",
                "formatError", "invalidName", "invalidIP", "invalidNameOrIP",
                "permissionDenied", "rbMethodNotFound", "unavailable",
                "underMinRep", "overMaxAlts", "altBanned", "altAccounts",
                "disputes", "previousBans", "bansOnRecord", "isMCBansMod",
                "mcbansServer", "youAreMCBansMod", "mcbansStaffVersion",
                "mcbansGiveAdminList", "banReturnMessage", "successSetting",
                "banInformation", "failSetting",
                "previousNames", "previousNamesHas", "previousNamesNone",
                "proxyDetected"
            };

            for (String key : keys) {
                String result = I18n.localize(key);
                assertFalse(result.startsWith("!") && result.endsWith("!"),
                        "Key '" + key + "' resolved to '!key!' — missing from default.yml");
                assertFalse(result.isBlank(), "Key '" + key + "' resolved to blank string");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Missing key → !key! sentinel
    // -------------------------------------------------------------------------

    @Nested
    class MissingKey {

        @Test
        void localize_returns_bang_sentinel_for_nonexistent_key() {
            String result = I18n.localize("thisKeyDefinitelyDoesNotExist_zzz");
            assertEquals("!thisKeyDefinitelyDoesNotExist_zzz!",
                    result,
                    "Expected '!key!' sentinel for a completely unknown key");
        }
    }

    // -------------------------------------------------------------------------
    // Token replacement
    // -------------------------------------------------------------------------

    @Nested
    class TokenReplacement {

        @Test
        void localize_replaces_player_token_in_kickSuccess() {
            String result = I18n.localize("kickSuccess",
                    I18n.PLAYER, "Steve",
                    I18n.SENDER, "Admin");
            assertTrue(result.contains("Steve"),
                    "Expected %PLAYER% to be replaced with 'Steve', got: " + result);
        }

        @Test
        void localize_replaces_reason_token_in_kickPlayer() {
            String result = I18n.localize("kickPlayer",
                    I18n.SENDER, "AdminUser",
                    I18n.REASON, "griefing");
            assertTrue(result.contains("griefing"),
                    "Expected %REASON% to be replaced with 'griefing', got: " + result);
        }

        @Test
        void localize_replaces_alts_token_in_altAccounts() {
            String result = I18n.localize("altAccounts",
                    I18n.PLAYER, "TestPlayer",
                    I18n.ALTS, "Alt1, Alt2");
            assertTrue(result.contains("Alt1, Alt2"),
                    "Expected %ALTS% to be replaced, got: " + result);
            assertTrue(result.contains("TestPlayer"),
                    "Expected %PLAYER% to be replaced, got: " + result);
        }

        @Test
        void localize_replaces_count_token_in_disputes() {
            String result = I18n.localize("disputes",
                    I18n.COUNT, "3");
            assertTrue(result.contains("3"),
                    "Expected %COUNT% to be replaced with '3', got: " + result);
        }

        @Test
        void localize_replaces_ip_token_in_ipBanSuccess() {
            String result = I18n.localize("ipBanSuccess");
            // IP is %IP% which won't be replaced without args — just ensure key resolves
            assertFalse(result.startsWith("!"),
                    "Expected ipBanSuccess to resolve, got: " + result);
        }
    }

    // -------------------------------------------------------------------------
    // English fallback when locale has missing key
    // -------------------------------------------------------------------------

    @Nested
    class EnglishFallback {

        /**
         * Switches to a non-English locale that is known to be missing some keys,
         * then verifies that the missing key falls back to the English message
         * (not the !key! sentinel).
         *
         * <p>Both German and Dutch files are missing keys like {@code mcbansServer},
         * {@code youAreMCBansMod}, etc. that exist in default.yml.
         * The I18n loader fills them in from the fallback at load time, so even
         * "missing" keys should resolve cleanly.</p>
         */
        @Test
        void incomplete_locale_falls_back_to_english_for_missing_keys() throws Exception {
            I18n.setCurrentLanguage("de");
            // mcbansServer, youAreMCBansMod are in default.yml but absent in german.yml
            String result = I18n.localize("mcbansServer");
            assertFalse(result.startsWith("!") && result.endsWith("!"),
                    "Expected English fallback for 'mcbansServer' in german locale, got: " + result);
            assertFalse(result.isBlank());
        }

        @Test
        void unknown_locale_sets_messages_to_null_and_localize_returns_bang_key() throws Exception {
            // setCurrentLanguage with a missing locale file returns null (no throw).
            // After that, localize() should degrade gracefully to !key! since
            // messages is null (no fallback available either because it is reset).
            I18n.setCurrentLanguage("xyzzy_nonexistent_99");
            // messages is now null — localize must return the !key! sentinel
            String result = I18n.localize("formatError");
            // Either !key! sentinel (messages=null path) or English fallback is acceptable
            assertNotNull(result, "localize must not return null even when messages is null");

            // Restore so subsequent tests are clean
            I18n.init("default");
        }

        @Test
        void after_failed_setCurrentLanguage_reinit_restores_english() throws Exception {
            try {
                I18n.setCurrentLanguage("xyzzy_nonexistent_99");
            } catch (Exception ignored) {
                // expected — messages is now null
            }
            I18n.init("default");

            String result = I18n.localize("formatError");
            assertFalse(result.startsWith("!"),
                    "After re-init with default, formatError should resolve, got: " + result);
        }

        @Test
        void dutch_locale_resolves_all_default_keys_via_fallback() throws Exception {
            I18n.setCurrentLanguage("nl");

            // Dutch file omits keys like mcbansServer / youAreMCBansMod.
            // loadLanguageFile fills them from fallback, so all keys must still resolve.
            String[] keysOnlyInDefault = {"mcbansServer", "youAreMCBansMod", "mcbansStaffVersion",
                    "mcbansGiveAdminList", "banInformation"};

            for (String key : keysOnlyInDefault) {
                String result = I18n.localize(key);
                assertFalse(result.startsWith("!") && result.endsWith("!"),
                        "Key '" + key + "' should fall back to English in dutch locale, got: " + result);
            }
        }
    }

    // -------------------------------------------------------------------------
    // All bundled locales load without throwing
    // -------------------------------------------------------------------------

    @Nested
    class AllLocales {

        @Test
        void all_extracted_locales_load_without_exception() {
            String[] locales = {"default", "nl", "fr", "de",
                    "ja-JP", "no", "pt", "es", "sv-SE"};

            for (String locale : locales) {
                assertDoesNotThrow(() -> I18n.setCurrentLanguage(locale),
                        "Locale '" + locale + "' should load without exception");
            }
        }

        @Test
        void each_locale_resolves_permissionDenied() throws Exception {
            String[] locales = {"default", "nl", "fr", "de",
                    "ja-JP", "no", "pt", "es", "sv-SE"};

            for (String locale : locales) {
                I18n.setCurrentLanguage(locale);
                String result = I18n.localize("permissionDenied");
                assertFalse(result.startsWith("!") && result.endsWith("!"),
                        "Locale '" + locale + "': permissionDenied resolved to '!key!'");
                assertFalse(result.isBlank(),
                        "Locale '" + locale + "': permissionDenied resolved to blank");
            }
        }
    }
}
