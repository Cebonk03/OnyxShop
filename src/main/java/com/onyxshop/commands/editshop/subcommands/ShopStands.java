package com.onyxshop.commands.editshop.subcommands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.onyxshop.OnyxShop;
import com.onyxshop.commands.editshop.Methods;
import com.onyxshop.commands.editshop.SubCommad;
import com.onyxshop.commands.editshop.subcommands.shopstands.Browse;
import com.onyxshop.commands.editshop.subcommands.shopstands.Destroy;
import com.onyxshop.commands.editshop.subcommands.shopstands.Give;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class ShopStands extends SubCommad {
   private final OnyxShop plugin;
   private final ArrayList<SubCommad> subCommads = new ArrayList<>();

   public ShopStands(OnyxShop var1, Methods var2) {
      this.plugin = var1;
      this.subCommads.add(new Give(var1, var2));
      this.subCommads.add(new Destroy(var1, var2));
      this.subCommads.add(new Browse(var1, var2));
   }

   @Override
   public void perform(Object var1, String[] var2) {
      if (!this.plugin.shopStands) {
         SendMessage.warnMessage(var1, Lang.SHOP_STANDS_MODULE_NOT_ENABLED.get());
      } else {
         if (var1 instanceof Player) {
            Player var6 = (Player)var1;
            if (var2.length > 1) {
               for (SubCommad var5 : this.subCommads) {
                  if (var2[1].equalsIgnoreCase(var5.getName())) {
                     if (var5.hasPermission(var6)) {
                        var5.perform(var1, var2);
                     } else {
                        SendMessage.chatToPlayer(var6, Lang.NO_PERMISSIONS.get());
                     }

                     return;
                  }
               }
            }

            if (this.hasPermission(var6)) {
               this.sendAllSyntaxes(var1);
            } else {
               SendMessage.chatToPlayer(var6, Lang.NO_PERMISSIONS.get());
            }
         } else {
            if (var1 instanceof org.bukkit.command.BlockCommandSender) {
               SendMessage.warnMessage(var1, Lang.NO_PERMISSIONS.get());
               return;
            }

            if (var2.length > 1) {
               if (var2[1].equalsIgnoreCase("browse") || var2[1].equalsIgnoreCase("give")) {
                  SendMessage.warnMessage(var1, Lang.REAL_PLAYER.get());
                  return;
               }

               for (SubCommad var4 : this.subCommads) {
                  if (var2[1].equalsIgnoreCase(var4.getName())) {
                     var4.perform(var1, var2);
                     return;
                  }
               }
            }

            this.sendAllSyntaxes(var1);
         }
      }
   }

   @Override
   public String getName() {
      return "shopstands";
   }

   @Override
   public String getDescription() {
      return Lang.EDITSHOP_SHOPSTANDS_SUBCOMMAND_DESC.get().getLegacy();
   }

   @Override
   public String getSyntax() {
      return Lang.EDITSHOP_SHOPSTANDS_SUBCOMMAND_SYNTAX.get().getLegacy();
   }

   @Override
   public boolean hasPermission(CommandSender var1) {
      return this.subCommads.stream().anyMatch(var1x -> var1x.hasPermission(var1));
   }

   private void sendAllSyntaxes(Object var1) {
      SendMessage.sendMessage(var1, Lang.SELLALL_COMMAND_USAGES.get());
      StringBuilder var2 = new StringBuilder();

      for (int var3 = 0; var3 < this.subCommads.size(); var3++) {
         SubCommad var4 = this.subCommads.get(var3);
         if (var4.hasPermission((CommandSender)var1) && (!(var1 instanceof ConsoleCommandSender) || !(var4 instanceof Give) && !(var4 instanceof Browse))) {
            var2.append(ChatColor.WHITE + " - " + ChatColor.RESET);
            var2.append(var4.getSyntax()).append(ChatColor.DARK_GRAY).append(" - ").append(ChatColor.RESET).append(var4.getDescription());
            if (var3 != this.subCommads.size()) {
               var2.append("\n");
            }
         }
      }

      SendMessage.sendMessage(var1, var2.toString());
   }

   @Override
   public List<String> getTabCompletion(String[] var1) {
      if (var1.length != 2) {
         for (SubCommad var7 : this.subCommads) {
            if (var1[1].equalsIgnoreCase(var7.getName())) {
               return var7.getTabCompletion(var1);
            }
         }

         return null;
      } else {
         ArrayList var2 = new ArrayList();

         for (SubCommad var4 : this.subCommads) {
            var2.add(var4.getName());
         }

         if (!var1[1].isEmpty()) {
            ArrayList var6 = new ArrayList();
            StringUtil.copyPartialMatches(var1[1], var2, var6);
            Collections.sort(var6);
            return var6;
         } else {
            return var2;
         }
      }
   }
}
