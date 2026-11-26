package com.onyxshop.commands.editshop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.onyxshop.OnyxShop;
import com.onyxshop.commands.base.PluginCommands;
import com.onyxshop.commands.editshop.subcommands.AddHandItem;
import com.onyxshop.commands.editshop.subcommands.AddItem;
import com.onyxshop.commands.editshop.subcommands.AddSection;
import com.onyxshop.commands.editshop.subcommands.Database;
import com.onyxshop.commands.editshop.subcommands.DeleteItem;
import com.onyxshop.commands.editshop.subcommands.DeleteSection;
import com.onyxshop.commands.editshop.subcommands.DynamicPricing;
import com.onyxshop.commands.editshop.subcommands.EditItem;
import com.onyxshop.commands.editshop.subcommands.EditSection;
import com.onyxshop.commands.editshop.subcommands.GlobalSell;
import com.onyxshop.commands.editshop.subcommands.GlobalStock;
import com.onyxshop.commands.editshop.subcommands.Import;
import com.onyxshop.commands.editshop.subcommands.InstallLayout;
import com.onyxshop.commands.editshop.subcommands.Migrate;
import com.onyxshop.commands.editshop.subcommands.Modifiers;
import com.onyxshop.commands.editshop.subcommands.PlayerSell;
import com.onyxshop.commands.editshop.subcommands.PlayerStock;
import com.onyxshop.commands.editshop.subcommands.RotatingShop;
import com.onyxshop.commands.editshop.subcommands.ShopStands;
import com.onyxshop.commands.editshop.subcommands.TransactionLogs;
import com.onyxshop.commands.editshop.subcommands.Update;
import com.onyxshop.commands.editshop.subcommands.UpdateLayout;
import com.onyxshop.commands.editshop.subcommands.UploadLayout;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import org.bukkit.ChatColor;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class CommandManager extends PluginCommands {
   private final ArrayList<SubCommad> subCommands = new ArrayList<>();

   public CommandManager(OnyxShop var1, List<String> var2) {
      super(
         var1,
         "editshop",
         "Allows to edit the shop with ingame commands",
         "/editshop <subCommand> ...",
         "OnyxShop.editshop",
         Collections.singletonList("onyx"),
         var2
      );
      Methods var3 = new Methods(var1);
      this.subCommands.add(new AddItem(var1, var3));
      this.subCommands.add(new EditItem(var1, var3));
      this.subCommands.add(new DeleteItem(var1, var3));
      this.subCommands.add(new AddHandItem(var1, var3));
      this.subCommands.add(new Import(var1, var3));
      this.subCommands.add(new Migrate(var1, var3));
      this.subCommands.add(new AddSection(var1, var3));
      this.subCommands.add(new EditSection(var1, var3));
      this.subCommands.add(new DeleteSection(var1, var3));
      this.subCommands.add(new Update());
      this.subCommands.add(new UploadLayout());
      this.subCommands.add(new InstallLayout());
      this.subCommands.add(new UpdateLayout());
      this.subCommands.add(new GlobalStock(var1, var3));
      this.subCommands.add(new PlayerStock(var1, var3));
      this.subCommands.add(new GlobalSell(var1, var3));
      this.subCommands.add(new PlayerSell(var1, var3));
      this.subCommands.add(new DynamicPricing(var1, var3));
      this.subCommands.add(new RotatingShop(var1, var3));
      this.subCommands.add(new ShopStands(var1, var3));
      this.subCommands.add(new TransactionLogs(var1, var3));
      this.subCommands.add(new Modifiers(var1, var3));
      this.subCommands.add(new Database(var1));
   }

   @Override
   public boolean execute(CommandSender var1, String var2, String[] var3) {
      if (ConfigManager.badYaml != null) {
         SendMessage.sendMessage(var1, ChatColor.DARK_RED + "This command cannot be executed now, please fix the configuration formatting first!");
         return true;
      } else {
         if (var1 instanceof Player) {
            Player var7 = (Player)var1;
            if (this.hasAccessInWorld(var7)) {
               if (var3.length > 0) {
                  for (SubCommad var6 : this.subCommands) {
                     if (var3[0].equalsIgnoreCase(var6.getName())) {
                        if (var6.hasPermission(var1)) {
                           var6.perform(var1, var3);
                        } else {
                           SendMessage.chatToPlayer(var7, Lang.NO_PERMISSIONS.get());
                        }

                        return true;
                     }
                  }
               }

               if (this.hasPermission(var7)) {
                  this.sendAllSyntaxes(var1);
               } else {
                  SendMessage.chatToPlayer(var7, Lang.NO_PERMISSIONS.get());
               }
            }
         } else if (var1 instanceof ConsoleCommandSender || var1 instanceof BlockCommandSender || var1 instanceof RemoteConsoleCommandSender) {
            if (var1 instanceof BlockCommandSender) {
               SendMessage.warnMessage(var1, Lang.NO_PERMISSIONS.get());
               return true;
            }

            if (var3.length > 0) {
               if (var3[0].equalsIgnoreCase("addhanditem")
                  || var3[0].equalsIgnoreCase("update")
                  || var3[0].equalsIgnoreCase("uploadLayout")
                  || var3[0].equalsIgnoreCase("installLayout")) {
                  SendMessage.warnMessage(var1, Lang.REAL_PLAYER.get());
                  return true;
               }

               for (SubCommad var5 : this.subCommands) {
                  if (var3[0].equalsIgnoreCase(var5.getName())) {
                     if (var5.hasPermission(var1)) {
                        var5.perform(var1, var3);
                     } else {
                        SendMessage.warnMessage(var1, Lang.NO_PERMISSIONS.get());
                     }

                     return true;
                  }
               }
            }

            this.sendAllSyntaxes(var1);
         }

         return true;
      }
   }

   private boolean hasPermission(CommandSender var1) {
      return this.subCommands.stream().anyMatch(var1x -> var1x.hasPermission(var1));
   }

   @Override
   public List<String> tabComplete(CommandSender var1, String var2, String[] var3) {
      if (!this.hasPermission(var1)) {
         return Collections.emptyList();
      } else {
         ArrayList var4 = new ArrayList();
         if (var3.length == 1) {
            for (SubCommad var9 : this.subCommands) {
               if (var9.hasPermission(var1)) {
                  var4.add(var9.getName());
               }
            }

            if (!var3[0].isEmpty()) {
               ArrayList var8 = new ArrayList();
               StringUtil.copyPartialMatches(var3[0], var4, var8);
               Collections.sort(var8);
               return var8;
            } else {
               return var4;
            }
         } else {
            if (var3.length >= 2) {
               for (SubCommad var6 : this.subCommands) {
                  if (var3[0].equalsIgnoreCase(var6.getName()) && var6.hasPermission(var1)) {
                     return var6.getTabCompletion(var3);
                  }
               }
            }

            return Collections.emptyList();
         }
      }
   }

   private void sendAllSyntaxes(Object var1) {
      SendMessage.sendMessage(var1, Lang.SELLALL_COMMAND_USAGES.get());
      StringBuilder var2 = new StringBuilder();

      for (int var3 = 0; var3 < this.subCommands.size(); var3++) {
         SubCommad var4 = this.subCommands.get(var3);
         if (var4.hasPermission((CommandSender)var1)
            && (
               !(var1 instanceof ConsoleCommandSender)
                  || !(var4 instanceof AddHandItem) && !(var4 instanceof Update) && !(var4 instanceof InstallLayout) && !(var4 instanceof UploadLayout)
            )) {
            var2.append(ChatColor.WHITE + " - " + ChatColor.RESET);
            var2.append(var4.getSyntax()).append(ChatColor.DARK_GRAY).append(" - ").append(ChatColor.RESET).append(var4.getDescription());
            if (var3 != this.subCommands.size()) {
               var2.append("\n");
            }
         }
      }

      SendMessage.sendMessage(var1, var2.toString());
   }
}
