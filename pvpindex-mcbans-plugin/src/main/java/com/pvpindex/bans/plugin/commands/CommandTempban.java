/*
 * This file is part of PvPIndex MCBans, a modified fork of MCBans.
 *
 * Original work Copyright (C) MCBans authors and contributors.
 * Modifications Copyright (C) 2026 PvPIndex contributors.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License version 3.
 */
package com.pvpindex.bans.plugin.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import com.pvpindex.bans.plugin.MCBans;
import org.bukkit.command.CommandSender;

import com.pvpindex.bans.plugin.BanType;
import com.pvpindex.bans.plugin.request.Ban;
import com.pvpindex.bans.plugin.util.Util;

public class CommandTempban extends BaseCommand {
  public CommandTempban() {
    bePlayer = false;
    name = "tempban";
    argLength = 2;
    usage = "temporary ban a player";
    banning = true;
  }

  @Override
  protected List<String> tabComplete(MCBans plugin, CommandSender sender, String cmd, String[] preArgs) {
    switch (preArgs.length) {
      case 1:
        return plugin.getServer().getOnlinePlayers().stream().map(p -> p.getName()).filter(p -> p.startsWith(preArgs[0])).collect(Collectors.toList());
      case 2:
        List<String> suggestions = new ArrayList<>();
        // Preset shortcuts (only those with a default-duration, shown at this slot)
        String partialPreset = preArgs[1].startsWith("#") ? preArgs[1].substring(1) : null;
        if (partialPreset != null || preArgs[1].isEmpty()) {
          String lprefix = partialPreset != null ? partialPreset.toLowerCase(Locale.ROOT) : "";
          plugin.getConfigs().getReasonPresetKeys().stream()
                  .filter(k -> plugin.getConfigs().getPresetDefaultDuration(k) != null)
                  .filter(k -> k.toLowerCase(Locale.ROOT).startsWith(lprefix))
                  .sorted()
                  .map(k -> "#" + k)
                  .forEach(suggestions::add);
        }
        if (preArgs[1].matches("([0-9]+)")) {
          suggestions.add(preArgs[1] + "seconds");
          suggestions.add(preArgs[1] + "minutes");
          suggestions.add(preArgs[1] + "hours");
          suggestions.add(preArgs[1] + "days");
          suggestions.add(preArgs[1] + "weeks");
        } else if (preArgs[1].isEmpty()) {
          suggestions.add("30minutes");
          suggestions.add("5hours");
          suggestions.add("1day");
          suggestions.add("1week");
        }
        return suggestions;
      default:
        if (preArgs.length >= 3) {
          return CommandBan.presetsCompletion(plugin, preArgs[preArgs.length - 1]);
        }
    }
    return new ArrayList<>();
  }

  private static final String DURATION_REGEX =
          "(?sim)([0-9]+)(minute(s|)|m|second(s|)|s|hour(s|)|h|day(s|)|d|week(s|)|w)";
  private static final Pattern DURATION_PATTERN =
          Pattern.compile("([0-9]+)(minute(s|)|m|second(s|)|s|hour(s|)|h|day(s|)|d|week(s|)|w)",
                  Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE);

  /** Parses a combined duration string like "7d" into [number, unit] or returns [raw, ""]. */
  private String[] parseDuration(String raw) {
    if (raw != null && raw.matches(DURATION_REGEX)) {
      try {
        Matcher m = DURATION_PATTERN.matcher(raw);
        if (m.find()) {
          return new String[]{m.group(1), m.group(2)};
        }
      } catch (PatternSyntaxException ex) {
        // fall through
      }
    }
    return new String[]{raw, ""};
  }

  @Override
  public void execute() {
    args.remove(0); // remove target
    if (args.size() < 1) {
      Util.message(sender, "Command incomplete.");
      return;
    }

    String duration;
    String measure;
    String reason;

    String firstArg = args.remove(0);

    if (firstArg.startsWith("#")) {
      // Preset shortcut: /tban <player> #preset [optional reason override]
      String presetKey = firstArg.substring(1);
      String defaultDur = config.getPresetDefaultDuration(presetKey);
      if (defaultDur == null) {
        Util.message(sender, "Preset '" + firstArg + "' has no default duration. "
                + "Use: /tban <player> <duration> " + firstArg);
        return;
      }
      String[] parsed = parseDuration(defaultDur);
      duration = parsed[0];
      measure  = parsed[1];
      reason = config.resolveReason(firstArg);
      if (args.size() > 0) {
        reason = config.resolveReason(Util.join(args, " "));
      }
    } else {
      // Normal flow: firstArg is the duration
      String[] parsed = parseDuration(firstArg);
      duration = parsed[0];
      measure  = parsed[1];
      reason = config.getDefaultTemp();
      if (args.size() > 0) {
        reason = config.resolveReason(Util.join(args, " "));
      }
    }

    // Start
    Ban banControl = new Ban(plugin, BanType.TEMP.getActionName(), target, targetUUID, targetIP, senderName, senderUUID, reason, duration, measure, null, false);
    banControl.run();
  }

  @Override
  public boolean permission(CommandSender sender) {
    return BanType.TEMP.getPermission().has(sender);
  }
}
