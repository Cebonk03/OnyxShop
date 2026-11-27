package com.onyxshop.commands.base;

import java.util.List;
import com.onyxshop.OnyxShop;

public abstract class PluginCommands extends PluginCommand {
   public PluginCommands(
      OnyxShop plugin, String command, String description, String usage, String permission, List<String> aliases, List<String> disabledWorlds
   ) {
      super(plugin, command, description, usage, permission, disabledWorlds);
      this.setAliases(aliases);
   }
}
