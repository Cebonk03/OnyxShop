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

   public PluginCommand(OnyxShop var1, String var2, String var3, String var4, String var5, List<String> var6) {
      super(var2);
      this.description = var3;
      this.usageMessage = var4;
      if (var5 != null) {
         this.setPermission(var5);
      }

      this.plugin = var1;
      this.disabledWorlds = var6;
   }

   public abstract boolean execute(@NotNull CommandSender var1, @NotNull String var2, @NotNull String[] var3);

   public abstract List<String> tabComplete(CommandSender var1, String var2, String[] var3);

   public boolean canUse(Player var1) {
      return this.hasAccessInWorld(var1) && this.isAllowedGamemode(var1);
   }

   public boolean hasAccessInWorld(Player var1) {
      if (!this.disabledWorlds.contains(var1.getWorld().getName())) {
         return true;
      } else {
         SendMessage.chatToPlayer(var1, Lang.COMMAND_DISABLED_IN_WORLD.get());
         return false;
      }
   }

   private boolean isAllowedGamemode(Player var1) {
      if (this.plugin.bannedGamemodes.contains(var1.getGameMode()) && !PermissionsCache.hasPermission(var1, "OnyxShop.bypassgamemode")) {
         SendMessage.chatToPlayer(var1, Lang.CANNOT_USE_COMMAND_BANNED_GAMEMODE.get().replace("%gamemode%", var1.getGameMode().name().toLowerCase()));
         return false;
      } else {
         return true;
      }
   }
}
