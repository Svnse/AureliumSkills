package com.archyx.aureliumskills.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import com.archyx.aureliumskills.AureliumSkills;
import com.archyx.aureliumskills.configuration.Option;
import com.archyx.aureliumskills.configuration.OptionL;
import com.archyx.aureliumskills.data.PlayerData;
import com.archyx.aureliumskills.data.backup.BackupProvider;
import com.archyx.aureliumskills.data.storage.StorageProvider;
import com.archyx.aureliumskills.item.UnclaimedItemsMenu;
import com.archyx.aureliumskills.lang.CommandMessage;
import com.archyx.aureliumskills.lang.Lang;
import com.archyx.aureliumskills.lang.LevelerMessage;
import com.archyx.aureliumskills.leaderboard.SkillValue;
import com.archyx.aureliumskills.menu.SkillsMenu;
import com.archyx.aureliumskills.modifier.ModifierType;
import com.archyx.aureliumskills.modifier.Modifiers;
import com.archyx.aureliumskills.modifier.StatModifier;
import com.archyx.aureliumskills.requirement.Requirements;
import com.archyx.aureliumskills.skills.Skill;
import com.archyx.aureliumskills.stats.Stat;
import com.archyx.aureliumskills.ui.ActionBar;
import com.archyx.aureliumskills.util.item.ItemUtils;
import com.archyx.aureliumskills.util.math.NumberUtil;
import com.archyx.aureliumskills.util.misc.KeyIntPair;
import com.archyx.aureliumskills.util.text.TextUtil;
import com.archyx.aureliumskills.util.version.UpdateChecker;
import de.tr7zw.changeme.nbtapi.NBTCompoundList;
import de.tr7zw.changeme.nbtapi.NBTFile;
import de.tr7zw.changeme.nbtapi.NBTListCompound;
import fr.minuskube.inv.SmartInventory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@CommandAlias("skills|sk|skill")
public class SkillsCommand extends BaseCommand {
 
	private final AureliumSkills plugin;
	private final ReloadManager reloadManager;

	public SkillsCommand(AureliumSkills plugin) {
		this.plugin = plugin;
		this.reloadManager = new ReloadManager(plugin);
	}

	@Default
	@CommandPermission("aureliumskills.skills")
	@Description("Opens the Skills menu, where you can browse skills, progress, and abilities.")
	public void onSkills(Player player) {
		SmartInventory inventory = SkillsMenu.getInventory(player, plugin);
		if (inventory != null) {
			inventory.open(player);
		} else {
			player.sendMessage(Lang.getMessage(CommandMessage.NO_PROFILE, Lang.getDefaultLanguage()));
		}
	}
	
	@Subcommand("xp add")
	@CommandCompletion("@players @skills")
	@CommandPermission("aureliumskills.xp.add")
	@Description("Adds skill XP to a player for a certain skill.")
	public void onXpAdd(CommandSender sender, @Flags("other") Player player, Skill skill, double amount, @Default("false") boolean silent) {
		Locale locale = plugin.getLang().getLocale(player);
		if (OptionL.isEnabled(skill)) {
			plugin.getLeveler().addXp(player, skill, amount);
			if (!silent) {
				sender.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(CommandMessage.XP_ADD, locale).replace("{amount}", String.valueOf(amount)).replace("{skill}", skill.getDisplayName(locale)).replace("{player}", player.getName()));
			}
		} else {
			sender.sendMessage(AureliumSkills.getPrefix(locale) + ChatColor.YELLOW + Lang.getMessage(CommandMessage.UNKNOWN_SKILL, locale));
		}
	}

	@Subcommand("xp set")
	@CommandCompletion("@players @skills")
	@CommandPermission("aureliumskills.xp.set")
	@Description("Sets a player's skill XP for a certain skill to an amount.")
	public void onXpSet(CommandSender sender, @Flags("other") Player player, Skill skill, double amount, @Default("false") boolean silent) {
		Locale locale = plugin.getLang().getLocale(player);
		if (OptionL.isEnabled(skill)) {
			plugin.getLeveler().setXp(player, skill, amount);
			if (!silent) {
				sender.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(CommandMessage.XP_SET, locale).replace("{amount}", String.valueOf(amount)).replace("{skill}", skill.getDisplayName(locale)).replace("{player}", player.getName()));
			}
		} else {
			sender.sendMessage(AureliumSkills.getPrefix(locale) + ChatColor.YELLOW + Lang.getMessage(CommandMessage.UNKNOWN_SKILL, locale));
		}
	}

	@Subcommand("xp remove")
	@CommandCompletion("@players @skills")
	@CommandPermission("aureliumskills.xp.remove")
	@Description("Removes skill XP from a player in a certain skill.")
	public void onXpRemove(CommandSender sender, @Flags("other") Player player, Skill skill, double amount, @Default("false") boolean silent) {
		Locale locale = plugin.getLang().getLocale(player);
		if (OptionL.isEnabled(skill)) {
			PlayerData playerData = plugin.getPlayerManager().getPlayerData(player);
			if (playerData == null) return;
			if (playerData.getSkillXp(skill) - amount >= 0) {
				plugin.getLeveler().setXp(player, skill, playerData.getSkillXp(skill) - amount);
				if (!silent) {
					sender.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(CommandMessage.XP_REMOVE, locale).replace("{amount}", String.valueOf(amount)).replace("{skill}", skill.getDisplayName(locale)).replace("{player}", player.getName()));
				}
			} else {
				if (!silent) {
					sender.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(CommandMessage.XP_REMOVE, locale).replace("{amount}", String.valueOf(playerData.getSkillXp(skill))).replace("{skill}", skill.getDisplayName(locale)).replace("{player}", player.getName()));
				}
				plugin.getLeveler().setXp(player, skill, 0);
			}
		} else {
			sender.sendMessage(AureliumSkills.getPrefix(locale) + ChatColor.YELLOW + Lang.getMessage(CommandMessage.UNKNOWN_SKILL, locale));
		}
	}

	@Subcommand("top")
	@CommandAlias("skilltop")
	@CommandCompletion("@skillTop")
	@CommandPermission("aureliumskills.top")
	@Description("Shows the top players in a skill")
	@Syntax("Usage: /sk top <page> or /sk top [skill] <page>")
	public void onTop(CommandSender sender, String[] args) {
		Locale locale = plugin.getLang().getLocale(sender);
		if (args.length == 0) {
			List<SkillValue> lb = plugin.getLeaderboardManager().getPowerLeaderboard(1, 10);
			sender.sendMessage(Lang.getMessage(CommandMessage.TOP_POWER_HEADER, locale));
			for (SkillValue skillValue : lb) {
				String name = Bukkit.getOfflinePlayer(skillValue.getId()).getName();
				sender.sendMessage(Lang.getMessage(CommandMessage.TOP_POWER_ENTRY, locale)
						.replace("{rank}", String.valueOf(lb.indexOf(skillValue) + 1))
						.replace("{player}", name != null ? name : "?")
						.replace("{level}", String.valueOf(skillValue.getLevel())));
			}
		}
		else if (args.length == 1) {
			if (args[0].equalsIgnoreCase("average")) {
				List<SkillValue> lb = plugin.getLeaderboardManager().getAverageLeaderboard(1, 10);
				sender.sendMessage(Lang.getMessage(CommandMessage.TOP_AVERAGE_HEADER, locale));
				for (SkillValue skillValue : lb) {
					String name = Bukkit.getOfflinePlayer(skillValue.getId()).getName();
					sender.sendMessage(TextUtil.replace(Lang.getMessage(CommandMessage.TOP_AVERAGE_ENTRY, locale),
							"{rank}", String.valueOf(lb.indexOf(skillValue) + 1),
							"{player}", name != null ? name : "?",
							"{level}", NumberUtil.format2(skillValue.getXp())));
				}
			} else {
				try {
					int page = Integer.parseInt(args[0]);
					List<SkillValue> lb = plugin.getLeaderboardManager().getPowerLeaderboard(page, 10);
					sender.sendMessage(Lang.getMessage(CommandMessage.TOP_POWER_HEADER_PAGE, locale).replace("{page}", String.valueOf(page)));
					for (SkillValue skillValue : lb) {
						String name = Bukkit.getOfflinePlayer(skillValue.getId()).getName();
						sender.sendMessage(Lang.getMessage(CommandMessage.TOP_POWER_ENTRY, locale)
								.replace("{rank}", String.valueOf((page - 1) * 10 + lb.indexOf(skillValue) + 1))
								.replace("{player}", name != null ? name : "?")
								.replace("{level}", String.valueOf(skillValue.getLevel())));
					}
				} catch (Exception e) {
					Skill skill = plugin.getSkillRegistry().getSkill(args[0]);
					if (skill != null) {
						List<SkillValue> lb = plugin.getLeaderboardManager().getLeaderboard(skill, 1, 10);
						sender.sendMessage(Lang.getMessage(CommandMessage.TOP_SKILL_HEADER, locale).replace("{skill}", skill.getDisplayName(locale)));
						for (SkillValue skillValue : lb) {
							String name = Bukkit.getOfflinePlayer(skillValue.getId()).getName();
							sender.sendMessage(Lang.getMessage(CommandMessage.TOP_SKILL_ENTRY, locale)
									.replace("{rank}", String.valueOf(lb.indexOf(skillValue) + 1))
									.replace("{player}", name != null ? name : "?")
									.replace("{level}", String.valueOf(skillValue.getLevel())));
						}
					} else {
						sender.sendMessage(Lang.getMessage(CommandMessage.TOP_USAGE, locale));
					}
				}
			}
		}
		else if (args.length == 2) {
			if (args[0].equalsIgnoreCase("average")) {
				try {
					int page = Integer.parseInt(args[1]);
					List<SkillValue> lb = plugin.getLeaderboardManager().getAverageLeaderboard(page, 10);
					sender.sendMessage(TextUtil.replace(Lang.getMessage(CommandMessage.TOP_AVERAGE_HEADER_PAGE, locale),
							"{page}", String.valueOf(page)));
					for (SkillValue skillValue : lb) {
						String name = Bukkit.getOfflinePlayer(skillValue.getId()).getName();
						sender.sendMessage(TextUtil.replace(Lang.getMessage(CommandMessage.TOP_AVERAGE_ENTRY, locale),
								"{rank}", String.valueOf(lb.indexOf(skillValue) + 1),
								"{player}", name != null ? name : "?",
								"{level}", NumberUtil.format2(skillValue.getXp())));
					}
				} catch (Exception e) {
					sender.sendMessage(Lang.getMessage(CommandMessage.TOP_USAGE, locale));
				}
			} else {
				Skill skill = plugin.getSkillRegistry().getSkill(args[0]);
				if (skill != null) {
					try {
						int page = Integer.parseInt(args[1]);
						List<SkillValue> lb = plugin.getLeaderboardManager().getLeaderboard(skill, page, 10);
						sender.sendMessage(Lang.getMessage(CommandMessage.TOP_SKILL_HEADER_PAGE, locale).replace("{page}", String.valueOf(page)).replace("{skill}", skill.getDisplayName(locale)));
						for (SkillValue skillValue : lb) {
							String name = Bukkit.getOfflinePlayer(skillValue.getId()).getName();
							sender.sendMessage(Lang.getMessage(CommandMessage.TOP_SKILL_ENTRY, locale)
									.replace("{rank}", String.valueOf((page - 1) * 10 + lb.indexOf(skillValue) + 1))
									.replace("{player}", name != null ? name : "?")
									.replace("{level}", String.valueOf(skillValue.getLevel())));
						}
					} catch (Exception e) {
						sender.sendMessage(Lang.getMessage(CommandMessage.TOP_USAGE, locale));
					}
				} else {
					sender.sendMessage(Lang.getMessage(CommandMessage.TOP_USAGE, locale));
				}
			}
		}
	}


	@Subcommand("save")
	@CommandPermission("aureliumskills.save")
	@Description("Saves skill data")
	public void onSave(CommandSender sender) {
		new BukkitRunnable() {
			@Override
			public void run() {
				Locale locale = plugin.getLang().getLocale(sender);
				for (Player player : Bukkit.getOnlinePlayers()) {
					plugin.getStorageProvider().save(player, false);
				}
				new BukkitRunnable() {
					@Override
					public void run() {
						sender.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(CommandMessage.SAVE_SAVED, locale));
					}
				}.runTask(plugin);
			}
		}.runTaskAsynchronously(plugin);
	}

	@Subcommand("updateleaderboards")
	@CommandPermission("aureliumskills.updateleaderboards")
	@Description("Updates and sorts the leaderboards")
	public void onUpdateLeaderboards(CommandSender sender) {
		Locale locale = plugin.getLang().getLocale(sender);
		if (plugin.getLeaderboardManager().isNotSorting()) {
			new BukkitRunnable() {
				@Override
				public void run() {
					plugin.getStorageProvider().updateLeaderboards();
					new BukkitRunnable() {
						@Override
						public void run() {
							sender.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(CommandMessage.UPDATELEADERBOARDS_UPDATED, locale));
						}
					}.runTask(plugin);
				}
			}.runTaskAsynchronously(plugin);
		} else {
			sender.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(CommandMessage.UPDATELEADERBOARDS_ALREADY_UPDATING, locale));
		}
	}

	@Subcommand("toggle")
	@CommandAlias("abtoggle")
	@CommandPermission("aureliumskills.abtoggle")
	@Description("Toggle your own action bar")
	public void onActionBarToggle(Player player) {
		Locale locale = plugin.getLang().getLocale(player);
		ActionBar actionBar = plugin.getActionBar();
		if (OptionL.getBoolean(Option.ACTION_BAR_ENABLED)) {
			if (actionBar.getActionBarDisabled().contains(player.getUniqueId())) {
				actionBar.getActionBarDisabled().remove(player.getUniqueId());
				player.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(CommandMessage.TOGGLE_ENABLED, locale));
			}
			else {
				actionBar.getActionBarDisabled().add(player.getUniqueId());
				player.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(CommandMessage.TOGGLE_DISABLED, locale));
			}
		}
		else {
			player.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(CommandMessage.TOGGLE_NOT_ENABLED, locale));
		}
	}

	@Subcommand("rank")
	@CommandAlias("skillrank")
	@CommandPermission("aureliumskills.rank")
	@Description("Shows your skill rankings")
	public void onRank(Player player) {
		Locale locale = plugin.getLang().getLocale(player);
		player.sendMessage(Lang.getMessage(CommandMessage.RANK_HEADER, locale));
		player.sendMessage(Lang.getMessage(CommandMessage.RANK_POWER, locale)
				.replace("{rank}", String.valueOf(plugin.getLeaderboardManager().getPowerRank(player.getUniqueId())))
				.replace("{total}", String.valueOf(plugin.getLeaderboardManager().getPowerLeaderboard().size())));
		for (Skill skill : plugin.getSkillRegistry().getSkills()) {
			if (OptionL.isEnabled(skill)) {
				player.sendMessage(Lang.getMessage(CommandMessage.RANK_ENTRY, locale)
						.replace("{skill}", String.valueOf(skill.getDisplayName(locale)))
						.replace("{rank}", String.valueOf(plugin.getLeaderboardManager().getSkillRank(skill, player.getUniqueId())))
						.replace("{total}", String.valueOf(plugin.getLeaderboardManager().getLeaderboard(skill).size())));
			}
		}
	}

	@Subcommand("lang")
	@CommandCompletion("@lang")
	@CommandPermission("aureliumskills.lang")
	@Description("Changes your player language")
	public void onLanguage(Player player, String language) {
		Locale locale = new Locale(language.toLowerCase(Locale.ENGLISH));
		if (Lang.hasLocale(locale)) {
			PlayerData playerData = plugin.getPlayerManager().getPlayerData(player);
			if (playerData == null) return;
			playerData.setLocale(locale);
			plugin.getCommandManager().setPlayerLocale(player, locale);
			player.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(CommandMessage.LANG_SET, locale).replace("{lang}", Lang.getDefinedLanguages().get(locale)));
		}
		else {
			player.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(CommandMessage.LANG_NOT_FOUND, plugin.getLang().getLocale(player)));
		}
	}
	
	@Subcommand("reload")
	@CommandPermission("aureliumskills.reload")
	@Description("Reloads the config, messages, menus, loot tables, action bars, boss bars, and health and luck stats.")
	public void reload(CommandSender sender) {
		reloadManager.reload(sender);
	}
	
	@Subcommand("skill setlevel")
	@CommandCompletion("@players @skills")
	@CommandPermission("aureliumskills.skill.setlevel")
	@Description("Sets a specific skill to a level for a player.")
	public void onSkillSetlevel(CommandSender sender, @Flags("other") Player player, Skill skill, int level) {
		Locale locale = plugin.getLang().getLocale(sender);
		if (OptionL.isEnabled(skill)) {
			if (level > 0) {
				PlayerData playerData = plugin.getPlayerManager().getPlayerData(player);
				if (playerData == null) return;
				int oldLevel = playerData.getSkillLevel(skill);
				playerData.setSkillLevel(skill, level);
				playerData.setSkillXp(skill, 0);
				plugin.getLeveler().updateStats(player);
				plugin.getLeveler().updatePermissions(player);
				plugin.getLeveler().applyRevertCommands(player, skill, oldLevel, level);
				plugin.getLeveler().applyLevelUpCommands(player, skill, oldLevel, level);
				// Reload items and armor to check for newly met requirements
				this.plugin.getModifierManager().reloadPlayer(player);
				sender.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(CommandMessage.SKILL_SETLEVEL_SET, locale)
						.replace("{skill}", skill.getDisplayName(locale))
						.replace("{level}", String.valueOf(level))
						.replace("{player}", player.getName()));
			} else {
				sender.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(CommandMessage.SKILL_SETLEVEL_AT_LEAST_ONE, locale));
			}
		}
		else {
			sender.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(CommandMessage.UNKNOWN_SKILL, locale));
		}
	}

	@Subcommand("skill setall")
	@CommandCompletion("@players")
	@CommandPermission("aureliumskills.skill.setlevel")
	@Description("Sets all of a player's skills to a level.")
	public void onSkillSetall(CommandSender sender, @Flags("other") Player player, int level) {
		Locale locale = plugin.getLang().getLocale(sender);
		if (level > 0) {
			PlayerData playerData = plugin.getPlayerManager().getPlayerData(player);
			if (playerData == null) return;
			for (Skill skill : plugin.getSkillRegistry().getSkills()) {
				if (OptionL.isEnabled(skill)) {
					int oldLevel = playerData.getSkillLevel(skill);
					playerData.setSkillLevel(skill, level);
					playerData.setSkillXp(skill, 0);
					// Reload items and armor to check for newly met requirements
					plugin.getModifierManager().reloadPlayer(player);
					plugin.getLeveler().applyRevertCommands(player, skill, oldLevel, level);
					plugin.getLeveler().applyLevelUpCommands(player, skill, oldLevel, level);
				}
			}
			plugin.getLeveler().updateStats(player);
			plugin.getLeveler().updatePermissions(player);
			sender.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(CommandMessage.SKILL_SETALL_SET, locale)
					.replace("{level}", String.valueOf(level))
					.replace("{player}", player.getName()));
		} else {
			sender.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(CommandMessage.SKILL_SETALL_AT_LEAST_ONE, locale));
		}
	}


	@Subcommand("skill reset")
	@CommandCompletion("@players @skills")
	@CommandPermission("aureliumskills.skill.reset")
	@Description("Resets all skills or a specific skill to level 1 for a player.")
	public void onSkillReset(CommandSender sender, @Flags("other") Player player, @Optional Skill skill) {
		Locale locale = plugin.getLang().getLocale(sender);
		if (skill != null) {
			if (OptionL.isEnabled(skill)) {
				PlayerData playerData = plugin.getPlayerManager().getPlayerData(player);
				if (playerData == null) return;
				int oldLevel = playerData.getSkillLevel(skill);
				playerData.setSkillLevel(skill, 1);
				playerData.setSkillXp(skill, 0);
				plugin.getLeveler().updateStats(player);
				plugin.getLeveler().updatePermissions(player);
				plugin.getLeveler().applyRevertCommands(player, skill, oldLevel, 1);
				// Reload items and armor to check for newly met requirements
				this.plugin.getModifierManager().reloadPlayer(player);
				sender.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(CommandMessage.SKILL_RESET_RESET_SKILL, locale)
						.replace("{skill}", skill.getDisplayName(locale))
						.replace("{player}", player.getName()));
			} else {
				sender.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(CommandMessage.UNKNOWN_SKILL, locale));
			}
		}
		else {
			PlayerData playerData = plugin.getPlayerManager().getPlayerData(player);
			if (playerData == null) return;
			for (Skill s : plugin.getSkillRegistry().getSkills()) {
				int oldLevel = playerData.getSkillLevel(s);
				playerData.setSkillLevel(s, 1);
				playerData.setSkillXp(s, 0);
				plugin.getLeveler().updateStats(player);
				plugin.getLeveler().updatePermissions(player);
				plugin.getLeveler().applyRevertCommands(player, s, oldLevel, 1);
			}
			sender.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(CommandMessage.SKILL_RESET_RESET_ALL, locale)
					.replace("{player}", player.getName()));
		}
	}

	@Subcommand("modifier add")
	@CommandPermission("aureliumskills.modifier.add")
	@CommandCompletion("@players @stats @nothing @nothing true")
	@Description("Adds a stat modifier to a player.")
	public void onAdd(CommandSender sender, @Flags("other") Player player, Stat stat, String name, double value, @Default("false") boolean silent) {
		Locale locale = plugin.getLang().getLocale(sender);
		PlayerData playerData = plugin.getPlayerManager().getPlayerData(player);
		if (playerData != null) {
			StatModifier modifier = new StatModifier(name, stat, value);
			if (!playerData.getStatModifiers().containsKey(name)) {
				playerData.addStatModifier(modifier);
				if (!silent) {
					sender.sendMessage(AureliumSkills.getPrefix(locale) + StatModifier.applyPlaceholders(Lang.getMessage(CommandMessage.MODIFIER_ADD_ADDED, locale), modifier, player, locale));
				}
			} else {
				if (!silent) {
					sender.sendMessage(AureliumSkills.getPrefix(locale) + StatModifier.applyPlaceholders(Lang.getMessage(CommandMessage.MODIFIER_ADD_ALREADY_EXISTS, locale), modifier, player, locale));
				}
			}
		} else {
			if (!silent) {
				sender.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(CommandMessage.NO_PROFILE, locale));
			}
		}
	}

	@Subcommand("modifier remove")
	@CommandPermission("aureliumskills.modifier.remove")
	@CommandCompletion("@players @modifiers true")
	@Description("Removes a specific stat modifier from a player.")
	public void onRemove(CommandSender sender, @Flags("other") Player player, String name, @Default("false") boolean silent) {
		Locale locale = plugin.getLang().getLocale(sender);
		PlayerData playerData = plugin.getPlayerManager().getPlayerData(player);
		if (playerData != null) {
			if (playerData.removeStatModifier(name)) {
				if (!silent) {
					sender.sendMessage(AureliumSkills.getPrefix(locale) + StatModifier.applyPlaceholders(Lang.getMessage(CommandMessage.MODIFIER_REMOVE_REMOVED, locale), name, player));
				}
			}
			else {
				if (!silent) {
					sender.sendMessage(AureliumSkills.getPrefix(locale) + StatModifier.applyPlaceholders(Lang.getMessage(CommandMessage.MODIFIER_REMOVE_NOT_FOUND, locale), name, player));
				}
			}
		}
		else {
			if (!silent) {
				sender.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(CommandMessage.NO_PROFILE, locale));
			}
		}
	}

	@Subcommand("modifier list")
	@CommandCompletion("@players @stats")
	@CommandPermission("aureliumskills.modifier.list")
	@Description("Lists all or a specific stat's modifiers for a player.")
	public void onList(CommandSender sender, @Flags("other") @Optional Player player, @Optional Stat stat) {
		Locale locale = plugin.getLang().getLocale(sender);
		if (player == null) {
			if (sender instanceof Player) {
				Player target = (Player) sender;
				listModifiers(sender, target, stat, locale);
			}
			else {
				sender.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(CommandMessage.MODIFIER_LIST_PLAYERS_ONLY, locale));
			}
		}
		else {
			listModifiers(sender, player, stat, locale);
		}
	}

	private void listModifiers(CommandSender sender, @Optional @Flags("other") Player player, @Optional Stat stat, Locale locale) {
		PlayerData playerData = plugin.getPlayerManager().getPlayerData(player);
		if (playerData != null) {
			StringBuilder message;
			if (stat == null) {
				message = new StringBuilder(StatModifier.applyPlaceholders(Lang.getMessage(CommandMessage.MODIFIER_LIST_ALL_STATS_HEADER, locale), player));
				for (String key : playerData.getStatModifiers().keySet()) {
					StatModifier modifier = playerData.getStatModifiers().get(key);
					message.append("\n").append(StatModifier.applyPlaceholders(Lang.getMessage(CommandMessage.MODIFIER_LIST_ALL_STATS_ENTRY, locale), modifier, player, locale));
				}
			} else {
				message = new StringBuilder(StatModifier.applyPlaceholders(Lang.getMessage(CommandMessage.MODIFIER_LIST_ONE_STAT_HEADER, locale), stat, player, locale));
				for (String key : playerData.getStatModifiers().keySet()) {
					StatModifier modifier = playerData.getStatModifiers().get(key);
					if (modifier.getStat() == stat) {
						message.append("\n").append(StatModifier.applyPlaceholders(Lang.getMessage(CommandMessage.MODIFIER_LIST_ONE_STAT_ENTRY, locale), modifier, player, locale));
					}
				}
			}
			sender.sendMessage(message.toString());
		} else {
			sender.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(CommandMessage.NO_PROFILE, locale));
		}
	}

	@Subcommand("modifier removeall")
	@CommandCompletion("@players @stats")
	@CommandPermission("aureliumskills.modifier.removeall")
	@Description("Removes all stat modifiers from a player.")
	public void onRemoveAll(CommandSender sender, @Flags("other") @Optional Player player, @Optional Stat stat, @Default("false") boolean silent) {
		Locale locale = plugin.getLang().getLocale(sender);
		if (player == null) {
			if (sender instanceof Player) {
				Player target = (Player) sender;
				PlayerData playerData = plugin.getPlayerManager().getPlayerData(target);
				removeAllModifiers(sender, stat, silent, locale, target, playerData);
			}
			else {
				if (!silent) {
					sender.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(CommandMessage.MODIFIER_REMOVEALL_PLAYERS_ONLY, locale));
				}
			}
		}
		else {
			PlayerData playerData = plugin.getPlayerManager().getPlayerData(player);
			removeAllModifiers(sender, stat, silent, locale, player, playerData);
		}
	}

	private void removeAllModifiers(CommandSender sender, @Optional Stat stat, @Default("false") boolean silent, Locale locale, Player target, PlayerData playerData) {
		if (playerData != null) {
			int removed = 0;
			List<String> toRemove = new ArrayList<>();
			for (String key : playerData.getStatModifiers().keySet()) {
				if (stat == null) {
					toRemove.add(key);
					removed++;
				}
				else if (playerData.getStatModifiers().get(key).getStat() == stat) {
					toRemove.add(key);
					removed++;
				}
			}
			for (String key : toRemove) {
				playerData.removeStatModifier(key);
			}
			if (!silent) {
				if (stat == null) {
					sender.sendMessage(AureliumSkills.getPrefix(locale) + StatModifier.applyPlaceholders(Lang.getMessage(CommandMessage.MODIFIER_REMOVEALL_REMOVED_ALL_STATS, locale), target).replace("{num}", String.valueOf(removed)));
				}
				else {
					sender.sendMessage(AureliumSkills.getPrefix(locale) + StatModifier.applyPlaceholders(Lang.getMessage(CommandMessage.MODIFIER_REMOVEALL_REMOVED_ONE_STAT, locale), stat, target, locale).replace("{num}", String.valueOf(removed)));
				}
			}
		}
		else {
			if (!silent) {
				sender.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(CommandMessage.NO_PROFILE, locale));
			}
		}
	}

	@Subcommand("item modifier add")
	@CommandCompletion("@stats @nothing false|true")
	@CommandPermission("aureliumskills.item.modifier.add")
	@Description("Adds an item stat modifier to the item held, along with lore by default.")
	public void onItemModifierAdd(@Flags("itemheld") Player player, Stat stat, double value, @Default("true") boolean lore) {
		Locale locale = plugin.getLang().getLocale(player);
		ItemStack item = player.getInventory().getItemInMainHand();
		Modifiers modifiers = new Modifiers(plugin);
		for (StatModifier statModifier : modifiers.getModifiers(ModifierType.ITEM, item)) {
			if (statModifier.getStat() == stat) {
				player.sendMessage(AureliumSkills.getPrefix(locale) + StatModifier.applyPlaceholders(Lang.getMessage(CommandMessage.ITEM_MODIFIER_ADD_ALREADY_EXISTS, locale), stat, locale));
				return;
			}
		}
		if (lore) {
			modifiers.addLore(ModifierType.ITEM, item, stat, value, locale);
		}
		ItemStack newItem = modifiers.addModifier(ModifierType.ITEM, item, stat, value);
		player.getInventory().setItemInMainHand(newItem);
		player.sendMessage(AureliumSkills.getPrefix(locale) + StatModifier.applyPlaceholders(Lang.getMessage(CommandMessage.ITEM_MODIFIER_ADD_ADDED, locale), stat, value, locale));
	}

	@Subcommand("item modifier remove")
	@CommandCompletion("@stats false|true")
	@CommandPermission("aureliumskills.item.modifier.remove")
	@Description("Removes an item stat modifier from the item held, and the lore associated with it by default.")
	public void onItemModifierRemove(@Flags("itemheld") Player player, Stat stat, @Default("true") boolean lore) {
		Locale locale = plugin.getLang().getLocale(player);
		ItemStack item = player.getInventory().getItemInMainHand();
		boolean removed = false;
		Modifiers modifiers = new Modifiers(plugin);
		for (StatModifier modifier : modifiers.getModifiers(ModifierType.ITEM, item)) {
			if (modifier.getStat() == stat) {
				item = modifiers.removeModifier(ModifierType.ITEM, item, stat);
				removed = true;
				break;
			}
		}
		if (lore) {
			modifiers.removeLore(item, stat, locale);
		}
		player.getInventory().setItemInMainHand(item);
		if (removed) {
			player.sendMessage(AureliumSkills.getPrefix(locale) + StatModifier.applyPlaceholders(Lang.getMessage(CommandMessage.ITEM_MODIFIER_REMOVE_REMOVED, locale), stat, locale));
		}
		else {
			player.sendMessage(AureliumSkills.getPrefix(locale) + StatModifier.applyPlaceholders(Lang.getMessage(CommandMessage.ITEM_MODIFIER_REMOVE_DOES_NOT_EXIST, locale), stat, locale));
		}
	}

	@Subcommand("item modifier list")
	@CommandPermission("aureliumskills.item.modifier.list")
	@Description("Lists all item stat modifiers on the item held.")
	public void onItemModifierList(@Flags("itemheld") Player player) {
		Locale locale = plugin.getLang().getLocale(player);
		ItemStack item = player.getInventory().getItemInMainHand();
		StringBuilder message = new StringBuilder(Lang.getMessage(CommandMessage.ITEM_MODIFIER_LIST_HEADER, locale));
		Modifiers modifiers = new Modifiers(plugin);
		for (StatModifier modifier : modifiers.getModifiers(ModifierType.ITEM, item)) {
			message.append("\n").append(StatModifier.applyPlaceholders(Lang.getMessage(CommandMessage.ITEM_MODIFIER_LIST_ENTRY, locale), modifier, locale));
		}
		player.sendMessage(message.toString());
	}

	@Subcommand("item modifier removeall")
	@CommandPermission("aureliumskills.item.modifier.removall")
	@Description("Removes all item stat modifiers from the item held.")
	public void onItemModifierRemoveAll(@Flags("itemheld") Player player) {
		Locale locale = plugin.getLang().getLocale(player);
		Modifiers modifiers = new Modifiers(plugin);
		ItemStack item = modifiers.removeAllModifiers(ModifierType.ITEM, player.getInventory().getItemInMainHand());
		player.getInventory().setItemInMainHand(item);
		player.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(CommandMessage.ITEM_MODIFIER_REMOVEALL_REMOVED, locale));
	}

	@Subcommand("armor modifier add")
	@CommandCompletion("@stats @nothing false|true")
	@CommandPermission("aureliumskills.armor.modifier.add")
	@Description("Adds an armor stat modifier to the item held, along with lore by default.")
	public void onArmorModifierAdd(@Flags("itemheld") Player player, Stat stat, int value, @Default("true") boolean lore) {
		Locale locale = plugin.getLang().getLocale(player);
		ItemStack item = player.getInventory().getItemInMainHand();
		Modifiers modifiers = new Modifiers(plugin);
		for (StatModifier statModifier : modifiers.getModifiers(ModifierType.ARMOR, item)) {
			if (statModifier.getStat() == stat) {
				player.sendMessage(AureliumSkills.getPrefix(locale) + StatModifier.applyPlaceholders(Lang.getMessage(CommandMessage.ARMOR_MODIFIER_ADD_ALREADY_EXISTS, locale), stat, locale));
				return;
			}
		}
		if (lore) {
			modifiers.addLore(ModifierType.ARMOR, item, stat, value, locale);
		}
		ItemStack newItem = modifiers.addModifier(ModifierType.ARMOR, item, stat, value);
		player.getInventory().setItemInMainHand(newItem);
		player.sendMessage(AureliumSkills.getPrefix(locale) + StatModifier.applyPlaceholders(Lang.getMessage(CommandMessage.ARMOR_MODIFIER_ADD_ADDED, locale), stat, value, locale));

	}

	@Subcommand("armor modifier remove")
	@CommandCompletion("@stats false|true")
	@CommandPermission("aureliumskills.armor.modifier.remove")
	@Description("Removes an armor stat modifier from the item held, and the lore associated with it by default.")
	public void onArmorModifierRemove(@Flags("itemheld") Player player, Stat stat, @Default("true") boolean lore) {
		Locale locale = plugin.getLang().getLocale(player);
		ItemStack item = player.getInventory().getItemInMainHand();
		boolean removed = false;
		Modifiers modifiers = new Modifiers(plugin);
		for (StatModifier modifier : modifiers.getModifiers(ModifierType.ARMOR, item)) {
			if (modifier.getStat() == stat) {
				item = modifiers.removeModifier(ModifierType.ARMOR, item, stat);
				removed = true;
				break;
			}
		}
		if (lore) {
			modifiers.removeLore(item, stat, locale);
		}
		player.getInventory().setItemInMainHand(item);
		if (removed) {
			player.sendMessage(AureliumSkills.getPrefix(locale) + StatModifier.applyPlaceholders(Lang.getMessage(CommandMessage.ARMOR_MODIFIER_REMOVE_REMOVED, locale), stat, locale));
		}
		else {
			player.sendMessage(AureliumSkills.getPrefix(locale) + StatModifier.applyPlaceholders(Lang.getMessage(CommandMessage.ARMOR_MODIFIER_REMOVE_DOES_NOT_EXIST, locale), stat, locale));
		}
	}

	@Subcommand("armor modifier list")
	@CommandPermission("aureliumskills.armor.modifier.list")
	@Description("Lists all armor stat modifiers on the item held.")
	public void onArmorModifierList(@Flags("itemheld") Player player) {
		Locale locale = plugin.getLang().getLocale(player);
		ItemStack item = player.getInventory().getItemInMainHand();
		StringBuilder message = new StringBuilder(Lang.getMessage(CommandMessage.ARMOR_MODIFIER_LIST_HEADER, locale));
		Modifiers modifiers = new Modifiers(plugin);
		for (StatModifier modifier : modifiers.getModifiers(ModifierType.ARMOR, item)) {
			message.append("\n").append(StatModifier.applyPlaceholders(Lang.getMessage(CommandMessage.ARMOR_MODIFIER_LIST_ENTRY, locale), modifier, locale));
		}
		player.sendMessage(message.toString());
	}

	@Subcommand("armor modifier removeall")
	@CommandPermission("aureliumskills.armor.modifier.removeall")
	@Description("Removes all armor stat modifiers from the item held.")
	public void onArmorModifierRemoveAll(@Flags("itemheld") Player player) {
		Locale locale = plugin.getLang().getLocale(player);
		Modifiers modifiers = new Modifiers(plugin);
		ItemStack item = modifiers.removeAllModifiers(ModifierType.ARMOR, player.getInventory().getItemInMainHand());
		player.getInventory().setItemInMainHand(item);
		player.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(CommandMessage.ARMOR_MODIFIER_REMOVEALL_REMOVED, locale));
	}

	@Subcommand("item requirement add")
	@CommandPermission("aureliumskills.item.requirement.add")
	@CommandCompletion("@skills @nothing false|true")
	@Description("Adds an item requirement to the item held, along with lore by default.")
	public void onItemRequirementAdd(@Flags("itemheld") Player player, Skill skill, int level, @Default("true") boolean lore) {
		Locale locale = plugin.getLang().getLocale(player);
		ItemStack item = player.getInventory().getItemInMainHand();
		Requirements requirements = new Requirements(plugin);
		if (requirements.hasRequirement(ModifierType.ITEM, item, skill)) {
			player.sendMessage(AureliumSkills.getPrefix(locale) + TextUtil.replace(Lang.getMessage(CommandMessage.ITEM_REQUIREMENT_ADD_ALREADY_EXISTS, locale), "{skill}", skill.getDisplayName(locale)));
			return;
		}
		item = requirements.addRequirement(ModifierType.ITEM, item, skill, level);
		if (lore) {
			requirements.addLore(ModifierType.ITEM, item, skill, level, locale);
		}
		player.getInventory().setItemInMainHand(item);
		player.sendMessage(AureliumSkills.getPrefix(locale) + TextUtil.replace(Lang.getMessage(CommandMessage.ITEM_REQUIREMENT_ADD_ADDED, locale),
				"{skill}", skill.getDisplayName(locale),
				"{level}", String.valueOf(level)));
	}

	@Subcommand("item requirement remove")
	@CommandPermission("aureliumskills.item.requirement.remove")
	@CommandCompletion("@skills false|true")
	@Description("Removes an item requirement from the item held, and the lore associated with it by default.")
	public void onItemRequirementRemove(@Flags("itemheld") Player player, Skill skill, @Default("true") boolean lore) {
		Locale locale = plugin.getLang().getLocale(player);
		ItemStack item = player.getInventory().getItemInMainHand();
		Requirements requirements = new Requirements(plugin);
		if (requirements.hasRequirement(ModifierType.ITEM, item, skill)) {
			item = requirements.removeRequirement(ModifierType.ITEM, item, skill);
			if (lore) {
				requirements.removeLore(item, skill);
			}
			player.getInventory().setItemInMainHand(item);
			player.sendMessage(AureliumSkills.getPrefix(locale) + TextUtil.replace(Lang.getMessage(CommandMessage.ITEM_REQUIREMENT_REMOVE_REMOVED, locale),
					"{skill}", skill.getDisplayName(locale)));
		}
		else {
			player.sendMessage(AureliumSkills.getPrefix(locale) + TextUtil.replace(Lang.getMessage(CommandMessage.ITEM_REQUIREMENT_REMOVE_DOES_NOT_EXIST, locale),
					"{skill}", skill.getDisplayName(locale)));
		}
	}

	@Subcommand("item requirement list")
	@CommandPermission("aureliumskills.item.requirement.list")
	@Description("Lists the item requirements on the item held.")
	public void onItemRequirementList(@Flags("itemheld") Player player) {
		Locale locale = plugin.getLang().getLocale(player);
		player.sendMessage(Lang.getMessage(CommandMessage.ITEM_REQUIREMENT_LIST_HEADER, locale));
		Requirements requirements = new Requirements(plugin);
		for (Map.Entry<Skill, Integer> entry : requirements.getRequirements(ModifierType.ITEM, player.getInventory().getItemInMainHand()).entrySet()) {
			player.sendMessage(TextUtil.replace(Lang.getMessage(CommandMessage.ITEM_REQUIREMENT_LIST_ENTRY, locale),
					"{skill}", entry.getKey().getDisplayName(locale),
					"{level}", String.valueOf(entry.getValue())));
		}
	}

	@Subcommand("item requirement removeall")
	@CommandPermission("aureliumskills.item.requirement.removeall")
	@Description("Removes all item requirements from the item held.")
	public void onItemRequirementRemoveAll(@Flags("itemheld") Player player) {
		Locale locale = plugin.getLang().getLocale(player);
		Requirements requirements = new Requirements(plugin);
		ItemStack item = requirements.removeAllRequirements(ModifierType.ITEM, player.getInventory().getItemInMainHand());
		player.getInventory().setItemInMainHand(item);
		player.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(CommandMessage.ITEM_REQUIREMENT_REMOVEALL_REMOVED, locale));
	}

	@Subcommand("armor requirement add")
	@CommandPermission("aureliumskills.armor.requirement.add")
	@CommandCompletion("@skills @nothing false|true")
	@Description("Adds an armor requirement to the item held, along with lore by default")
	public void onArmorRequirementAdd(@Flags("itemheld") Player player, Skill skill, int level, @Default("true") boolean lore) {
		Locale locale = plugin.getLang().getLocale(player);
		ItemStack item = player.getInventory().getItemInMainHand();
		Requirements requirements = new Requirements(plugin);
		if (requirements.hasRequirement(ModifierType.ARMOR, item, skill)) {
			player.sendMessage(AureliumSkills.getPrefix(locale) + TextUtil.replace(Lang.getMessage(CommandMessage.ARMOR_REQUIREMENT_ADD_ALREADY_EXISTS, locale),
					"{skill}", skill.getDisplayName(locale)));
			return;
		}
		item = requirements.addRequirement(ModifierType.ARMOR, item, skill, level);
		if (lore) {
			requirements.addLore(ModifierType.ARMOR, item, skill, level, locale);
		}
		player.getInventory().setItemInMainHand(item);
		player.sendMessage(AureliumSkills.getPrefix(locale) + TextUtil.replace(Lang.getMessage(CommandMessage.ARMOR_REQUIREMENT_ADD_ADDED, locale),
				"{skill}", skill.getDisplayName(locale),
				"{level}", String.valueOf(level)));
	}

	@Subcommand("armor requirement remove")
	@CommandPermission("aureliumskills.armor.requirement.remove")
	@CommandCompletion("@skills false|true")
	@Description("Removes an armor requirement from the item held, along with the lore associated it by default.")
	public void onArmorRequirementRemove(@Flags("itemheld") Player player, Skill skill, @Default("true") boolean lore) {
		Locale locale = plugin.getLang().getLocale(player);
		ItemStack item = player.getInventory().getItemInMainHand();
		Requirements requirements = new Requirements(plugin);
		if (requirements.hasRequirement(ModifierType.ARMOR, item, skill)) {
			item = requirements.removeRequirement(ModifierType.ARMOR, item, skill);
			if (lore) {
				requirements.removeLore(item, skill);
			}
			player.getInventory().setItemInMainHand(item);
			player.sendMessage(AureliumSkills.getPrefix(locale) + TextUtil.replace(Lang.getMessage(CommandMessage.ARMOR_REQUIREMENT_REMOVE_REMOVED, locale),
					"{skill}", skill.getDisplayName(locale)));
		}
		else {
			player.sendMessage(AureliumSkills.getPrefix(locale) + TextUtil.replace(Lang.getMessage(CommandMessage.ARMOR_REQUIREMENT_REMOVE_DOES_NOT_EXIST, locale),
					"{skill}", skill.getDisplayName(locale)));
		}
	}

	@Subcommand("armor requirement list")
	@CommandPermission("aureliumskills.armor.requirement.list")
	@Description("Lists the armor requirements on the item held.")
	public void onArmorRequirementList(@Flags("itemheld") Player player) {
		Locale locale = plugin.getLang().getLocale(player);
		player.sendMessage(Lang.getMessage(CommandMessage.ARMOR_REQUIREMENT_LIST_HEADER, locale));
		Requirements requirements = new Requirements(plugin);
		for (Map.Entry<Skill, Integer> entry : requirements.getRequirements(ModifierType.ARMOR, player.getInventory().getItemInMainHand()).entrySet()) {
			player.sendMessage(TextUtil.replace(Lang.getMessage(CommandMessage.ARMOR_REQUIREMENT_LIST_ENTRY, locale),
					"{skill}", entry.getKey().getDisplayName(locale),
					"{level}", String.valueOf(entry.getValue())));
		}
	}

	@Subcommand("armor requirement removeall")
	@CommandPermission("aureliumskills.armor.requirement.removeall")
	@Description("Removes all armor requirements from the item held.")
	public void onArmorRequirementRemoveAll(@Flags("itemheld") Player player) {
		Locale locale = plugin.getLang().getLocale(player);
		Requirements requirements = new Requirements(plugin);
		ItemStack item = requirements.removeAllRequirements(ModifierType.ARMOR, player.getInventory().getItemInMainHand());
		player.getInventory().setItemInMainHand(item);
		player.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(CommandMessage.ARMOR_REQUIREMENT_REMOVEALL_REMOVED, locale));
	}

	@Subcommand("multiplier")
	@CommandCompletion("@players")
	@CommandPermission("aureliumskills.multipliercommand")
	@Description("Shows a player's current XP multiplier based on their permissions.")
	public void onMultiplier(CommandSender sender, @Optional @Flags("other") Player player) {
		if (player == null) {
			if (sender instanceof Player) {
				Player target = (Player) sender;
				Locale locale = plugin.getLang().getLocale(sender);
				double multiplier = plugin.getLeveler().getMultiplier(target);
				sender.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(CommandMessage.MULTIPLIER_LIST, locale)
						.replace("{player}", target.getName())
						.replace("{multiplier}", NumberUtil.format2(multiplier))
						.replace("{percent}", NumberUtil.format2((multiplier - 1) * 100)));
			}
			else {
				Locale locale = Locale.ENGLISH;
				sender.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(CommandMessage.MULTIPLIER_PLAYERS_ONLY, locale));
			}
		}
		else {
			Locale locale = plugin.getLang().getLocale(player);
			double multiplier = plugin.getLeveler().getMultiplier(player);
			sender.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(CommandMessage.MULTIPLIER_LIST, locale)
					.replace("{player}", player.getName())
					.replace("{multiplier}", NumberUtil.format2(multiplier))
					.replace("{percent}", NumberUtil.format2((multiplier - 1) * 100)));
		}
	}

	@Subcommand("resethealth")
	@CommandPermission("aureliumskills.*")
	@Description("Removes all attribute modifiers by Aurelium Skills for easy uninstalling. This only works on offline players.")
	public void onResetHealth(CommandSender sender) {
		if (sender instanceof ConsoleCommandSender) {
			File playerDataFolder = new File(Bukkit.getWorlds().get(0).getWorldFolder(), "playerdata");
			int successful = 0;
			int error = 0;
			int total = 0;
			for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
				if (!player.isOnline()) {
					total++;
					File playerFile = new File(playerDataFolder, player.getUniqueId() + ".dat");
					if (playerFile.exists() && playerFile.canWrite()) {
						try {
							NBTFile nbtFile = new NBTFile(playerFile);
							NBTCompoundList compoundList = nbtFile.getCompoundList("Attributes");
							if (compoundList != null) {
								boolean save = false;
								for (NBTListCompound listCompound : compoundList.subList(0, compoundList.size())) {
									switch (listCompound.getString("Name")) {
										case "generic.maxHealth":
										case "minecraft:generic.max_health": {
											NBTCompoundList modifierList = listCompound.getCompoundList("Modifiers");
											if (modifierList != null) {
												for (NBTListCompound modifier : modifierList.subList(0, modifierList.size())) {
													if (modifier.getString("Name").equals("skillsHealth")) {
														modifierList.remove(modifier);
														if (modifierList.size() == 0) {
															listCompound.removeKey("Modifiers");
														}
														save = true;
													}
												}
											}
											break;
										}
										case "generic.luck":
										case "minecraft:generic.luck": {
											NBTCompoundList modifierList = listCompound.getCompoundList("Modifiers");
											if (modifierList != null) {
												for (NBTListCompound modifier : modifierList.subList(0, modifierList.size())) {
													if (modifier.getString("Name").equals("AureliumSkills-Luck")) {
														modifierList.remove(modifier);
														if (modifierList.size() == 0) {
															listCompound.removeKey("Modifiers");
														}
														save = true;
													}
												}
											}
											break;
										}
										case "generic.attackSpeed":
										case "minecraft:generic.attack_speed": {
											NBTCompoundList modifierList = listCompound.getCompoundList("Modifiers");
											if (modifierList != null) {
												for (NBTListCompound modifier : modifierList.subList(0, modifierList.size())) {
													if (modifier.getString("Name").equals("AureliumSkills-LightningBlade")) {
														modifierList.remove(modifier);
														if (modifierList.size() == 0) {
															listCompound.removeKey("Modifiers");
														}
														save = true;
													}
												}
											}
											break;
										}
									}
								}
								if (save) {
									nbtFile.save();
									successful++;
								}
							}
						} catch (Exception e) {
							error++;
						}
					}
				}
			}
			sender.sendMessage("Searched " + total + " offline players. Successfully removed attributes from " + successful + " players. Failed to remove on " + error + " players.");
		} else {
			sender.sendMessage(ChatColor.RED + "Only console may execute this command!");
		}
	}

	@Subcommand("backup save")
	@CommandPermission("aureliumskills.backup.save")
	public void onBackupSave(CommandSender sender) {
		BackupProvider backupProvider = plugin.getBackupProvider();
		if (backupProvider != null) {
			Locale locale = plugin.getLang().getLocale(sender);
			sender.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(CommandMessage.BACKUP_SAVE_SAVING, locale));
			backupProvider.saveBackup(sender, true);
		}
	}

	@Subcommand("backup load")
	@CommandPermission("aureliumskills.backup.load")
	public void onBackupLoad(CommandSender sender, String fileName) {
		StorageProvider storageProvider = plugin.getStorageProvider();
		Locale locale = plugin.getLang().getLocale(sender);
		if (storageProvider != null) {
			File file = new File(plugin.getDataFolder() + "/backups/" + fileName);
			if (file.exists()) {
				if (file.getName().endsWith(".yml")) {
					// Require player to double type command
					if (sender instanceof Player) {
						PlayerData playerData = plugin.getPlayerManager().getPlayerData((Player) sender);
						if (playerData == null) return;
						Object typed = playerData.getMetadata().get("backup_command");
						if (typed != null) {
							if (typed instanceof String) {
								String typedFile = (String) typed;
								if (typedFile.equals(file.getName())) {
									sender.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(CommandMessage.BACKUP_LOAD_LOADING, locale));
									storageProvider.loadBackup(YamlConfiguration.loadConfiguration(file), sender);
									playerData.getMetadata().remove("backup_command");
								} else {
									backupLoadConfirm(playerData, sender, file);
								}
							} else {
								backupLoadConfirm(playerData, sender, file);
							}
						} else {
							backupLoadConfirm(playerData, sender, file);
						}
					} else {
						sender.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(CommandMessage.BACKUP_LOAD_LOADING, locale));
						storageProvider.loadBackup(YamlConfiguration.loadConfiguration(file), sender);
					}
				} else {
					sender.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(CommandMessage.BACKUP_LOAD_MUST_BE_YAML, locale));
				}
			} else { // If file does not exist
				sender.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(CommandMessage.BACKUP_LOAD_FILE_NOT_FOUND, locale));
			}
		}
	}

	private void backupLoadConfirm(PlayerData playerData, CommandSender sender, File file) {
		Locale locale = playerData.getLocale();
		sender.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(CommandMessage.BACKUP_LOAD_CONFIRM, locale));
		playerData.getMetadata().put("backup_command", file.getName());
		new BukkitRunnable() {
			@Override
			public void run() {
				playerData.getMetadata().remove("backup_command");
			}
		}.runTaskLater(plugin, 20 * 60);
	}

	@Subcommand("claimitems")
	@CommandPermission("aureliumskills.claimitems")
	public void onClaimItems(Player player) {
		PlayerData playerData = plugin.getPlayerManager().getPlayerData(player);
		Locale locale = Lang.getDefaultLanguage();
		if (playerData != null) {
			locale = playerData.getLocale();
		}
		if (playerData == null || playerData.getUnclaimedItems().size() == 0) {
			player.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(CommandMessage.CLAIMITEMS_NO_ITEMS, locale));
			return;
		}
		UnclaimedItemsMenu.getInventory(plugin, playerData).open(player);
	}

	@Subcommand("item register")
	@CommandPermission("aureliumskills.item.register")
	public void onItemRegister(@Flags("itemheld") Player player, String key) {
		Locale locale = plugin.getLang().getLocale(player);
		if (key.contains(" ")) { // Disallow spaces in key name
			player.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(CommandMessage.ITEM_REGISTER_NO_SPACES, locale));
			return;
		}
		ItemStack item = player.getInventory().getItemInMainHand();
		if (plugin.getItemRegistry().getItem(key) == null) { // Check that no item has been registered on the key
			plugin.getItemRegistry().register(key, item);
			player.sendMessage(AureliumSkills.getPrefix(locale) + TextUtil.replace(Lang.getMessage(CommandMessage.ITEM_REGISTER_REGISTERED, locale), "{key}", key));
		} else {
			player.sendMessage(AureliumSkills.getPrefix(locale) + TextUtil.replace(Lang.getMessage(CommandMessage.ITEM_REGISTER_ALREADY_REGISTERED, locale), "{key}", key));
		}
	}

	@Subcommand("item unregister")
	@CommandPermission("aureliumskills.item.register")
	@CommandCompletion("@item_keys")
	public void onItemUnregister(Player player, String key) {
		Locale locale = plugin.getLang().getLocale(player);
		if (plugin.getItemRegistry().getItem(key) != null) { // Check that there is an item registered on the key
			plugin.getItemRegistry().unregister(key);
			player.sendMessage(AureliumSkills.getPrefix(locale) + TextUtil.replace(Lang.getMessage(CommandMessage.ITEM_UNREGISTER_UNREGISTERED, locale), "{key}", key));
		} else {
			player.sendMessage(AureliumSkills.getPrefix(locale) + TextUtil.replace(Lang.getMessage(CommandMessage.ITEM_UNREGISTER_NOT_REGISTERED, locale), "{key}", key));
		}
	}

	@Subcommand("item give")
	@CommandPermission("aureliumskills.item.give")
	@CommandCompletion("@players @item_keys")
	public void onItemGive(CommandSender sender, @Flags("other") Player player, String key, @Default("-1") int amount) {
		ItemStack item = plugin.getItemRegistry().getItem(key);
		Locale locale = plugin.getLang().getLocale(sender);
		if (item != null) {
			if (amount != -1) {
				item.setAmount(amount);
			}
			ItemStack leftoverItem = ItemUtils.addItemToInventory(player, item);
			sender.sendMessage(AureliumSkills.getPrefix(locale) + TextUtil.replace(Lang.getMessage(CommandMessage.ITEM_GIVE_SENDER, locale),
					"{amount}", String.valueOf(item.getAmount()), "{key}", key, "{player}", player.getName()));
			if (!sender.equals(player)) {
				player.sendMessage(AureliumSkills.getPrefix(locale) + TextUtil.replace(Lang.getMessage(CommandMessage.ITEM_GIVE_RECEIVER, locale),
						"{amount}", String.valueOf(item.getAmount()), "{key}", key));
			}
			// Add to unclaimed items if leftover
			if (leftoverItem != null) {
				PlayerData playerData = plugin.getPlayerManager().getPlayerData(player);
				if (playerData != null) {
					playerData.getUnclaimedItems().add(new KeyIntPair(key, leftoverItem.getAmount()));
					player.sendMessage(AureliumSkills.getPrefix(locale) + Lang.getMessage(LevelerMessage.UNCLAIMED_ITEM, locale));
				}
			}
		} else {
			sender.sendMessage(AureliumSkills.getPrefix(locale) + TextUtil.replace(Lang.getMessage(CommandMessage.ITEM_UNREGISTER_NOT_REGISTERED, locale), "{key}", key));
		}
	}

	@Subcommand("version")
	@CommandPermission("aureliumskills.version")
	public void onVersion(CommandSender sender) {
		Locale locale = plugin.getLang().getLocale(sender);
		new UpdateChecker(plugin, 81069).getVersion(latestVersion -> sender.sendMessage(AureliumSkills.getPrefix(locale) +
				TextUtil.replace(Lang.getMessage(CommandMessage.VERSION, locale),
				"{current_version}", plugin.getDescription().getVersion(),
				"{latest_version}", latestVersion)));
	}

	@Subcommand("help")
	@CommandPermission("aureliumskills.help")
	public void onHelp(CommandSender sender, CommandHelp help) {
		help.showHelp();
	}
}
