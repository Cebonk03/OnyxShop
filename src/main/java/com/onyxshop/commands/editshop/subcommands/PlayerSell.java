package com.onyxshop.commands.editshop.subcommands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.onyxshop.OnyxShop;
import com.onyxshop.commands.editshop.Methods;
import com.onyxshop.commands.editshop.SubCommad;
import com.onyxshop.commands.editshop.subcommands.playersell.ResetLimit;
import com.onyxshop.commands.editshop.subcommands.playersell.SetLimit;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class PlayerSell extends SubCommad {
   private final ArrayList<SubCommad> subCommads = new ArrayList<>();

   public PlayerSell(OnyxShop plugin, Methods methods) {
      this.subCommads.add(new SetLimit(plugin, methods));
      this.subCommads.add(new ResetLimit(plugin, methods));
   }

   @Override
   public void perform(Object value, String[] values) {
      if (value instanceof Player) {
         Player player = (Player)value;
         if (values.length > 1) {
            for (SubCommad subCommad : this.subCommads) {
               if (values[1].equalsIgnoreCase(subCommad.getName())) {
                  if (subCommad.hasPermission(player)) {
                     subCommad.perform(value, values);
                  } else {
                     SendMessage.chatToPlayer(player, Lang.NO_PERMISSIONS.get());
                  }

                  return;
               }
            }
         }

         if (this.hasPermission(player)) {
            this.sendAllSyntaxes(value);
         } else {
            SendMessage.chatToPlayer(player, Lang.NO_PERMISSIONS.get());
         }
      } else {
         if (values.length > 1) {
            for (SubCommad subCommad : this.subCommads) {
               if (values[1].equalsIgnoreCase(subCommad.getName())) {
                  subCommad.perform(value, values);
                  return;
               }
            }
         }

         this.sendAllSyntaxes(value);
      }
   }

   @Override
   public String getName() {
      return "playersell";
   }

   @Override
   public String getDescription() {
      return ChatColor.GREEN + "Edit player sell limits for a shop item";
   }

   @Override
   public String getSyntax() {
      return ChatColor.GREEN + "/editshop playersell <set> ...";
   }

   @Override
   public boolean hasPermission(CommandSender sender) {
      return this.subCommads.stream().anyMatch(element -> element.hasPermission(sender));
   }

   private void sendAllSyntaxes(Object value) {
      SendMessage.sendMessage(value, Lang.SELLALL_COMMAND_USAGES.get());
      StringBuilder builder = new StringBuilder();

      for (int size = 0; size < this.subCommads.size(); size++) {
         SubCommad subCommad = this.subCommads.get(size);
         if (subCommad.hasPermission((CommandSender)value)
            && (!(value instanceof ConsoleCommandSender) || !(subCommad instanceof AddHandItem) && !(subCommad instanceof Update))) {
            builder.append(ChatColor.WHITE + " - " + ChatColor.RESET);
            builder.append(subCommad.getSyntax()).append(ChatColor.DARK_GRAY).append(" - ").append(ChatColor.RESET).append(subCommad.getDescription());
            if (size != this.subCommads.size()) {
               builder.append("\n");
            }
         }
      }

      SendMessage.sendMessage(value, builder.toString());
   }

   @Override
   public List<String> getTabCompletion(String[] values) {
      if (values.length != 2) {
         for (SubCommad subCommad : this.subCommads) {
            if (values[1].equalsIgnoreCase(subCommad.getName())) {
               return subCommad.getTabCompletion(values);
            }
         }

         return null;
      } else {
         ArrayList values2 = new ArrayList();

         for (SubCommad subCommad : this.subCommads) {
            values2.add(subCommad.getName());
         }

         if (!values[1].isEmpty()) {
            ArrayList values3 = new ArrayList();
            StringUtil.copyPartialMatches(values[1], values2, values3);
            Collections.sort(values3);
            return values3;
         } else {
            return values2;
         }
      }
   }
}
