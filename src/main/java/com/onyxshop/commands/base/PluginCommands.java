package com.onyxshop.commands.base;

import java.util.List;
import com.onyxshop.OnyxShop;

public abstract class PluginCommands extends PluginCommand {
   public PluginCommands(OnyxShop var1, String var2, String var3, String var4, String var5, List<String> var6, List<String> var7) {
      super(var1, var2, var3, var4, var5, var7);
      this.setAliases(var6);
   }
}
