package com.onyxshop.commands.base;

import java.util.List;
import com.onyxshop.OnyxShop;

public abstract class PluginCommands extends PluginCommand {
   public PluginCommands(OnyxShop plugin, String shopId, String text, String text2, String text3, List<String> values, List<String> values2) {
      super(plugin, shopId, text, text2, text3, values2);
      this.setAliases(values);
   }
}
