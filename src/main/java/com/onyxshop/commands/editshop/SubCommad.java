package com.onyxshop.commands.editshop;

import java.util.List;
import org.bukkit.command.CommandSender;

public abstract class SubCommad {
   public SubCommad() {
   }

   public abstract String getName();

   public abstract String getDescription();

   public abstract String getSyntax();

   public abstract boolean hasPermission(CommandSender sender);

   public abstract void perform(Object value, String[] values);

   public abstract List<String> getTabCompletion(String[] values);
}
