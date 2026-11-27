package com.onyxshop.commands.base;

import java.util.List;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.util.PermissionsCache;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public abstract class PluginCommand extends BukkitCommand {
   public final OnyxShop plugin;
   private final List<String> disabledWorlds;

   public PluginCommand(OnyxShop plugin, String command, String description, String usage, String permission, List<String> disabledWorlds) {
      super(command);
      this.description = description;
      this.usageMessage = usage;
      if (permission != null) {
         this.setPermission(permission);
      }

      this.plugin = plugin;
      this.disabledWorlds = disabledWorlds;
   }

   public abstract boolean execute(@NotNull CommandSender sender, @NotNull String command, @NotNull String[] values);

   public abstract List<String> tabComplete(CommandSender sender, String command, String[] values);

   public boolean canUse(Player player) {
      return this.hasAccessInWorld(player) && this.isAllowedGamemode(player);
   }

   public boolean hasAccessInWorld(Player player) {
      if (!this.disabledWorlds.contains(player.getWorld().getName())) {
         return true;
      } else {
         SendMessage.chatToPlayer(player, Lang.COMMAND_DISABLED_IN_WORLD.get());
         return false;
      }
   }

   private boolean isAllowedGamemode(Player player) {
      if (this.plugin.bannedGamemodes.contains(player.getGameMode()) && !PermissionsCache.hasPermission(player, "OnyxShop.bypassgamemode")) {
         SendMessage.chatToPlayer(player, Lang.CANNOT_USE_COMMAND_BANNED_GAMEMODE.get().replace("%gamemode%", player.getGameMode().name().toLowerCase()));
         return false;
      } else {
         return true;
      }
   }
}
