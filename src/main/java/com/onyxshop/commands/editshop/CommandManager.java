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

   public CommandManager(OnyxShop plugin, List<String> commands) {
      super(
         plugin,
         "editshop",
         "Allows to edit the shop with ingame commands",
         "/editshop <subCommand> ...",
         "OnyxShop.editshop",
         Collections.singletonList("onyx"),
         commands
      );
      Methods methods = new Methods(plugin);
      this.subCommands.add(new AddItem(plugin, methods));
      this.subCommands.add(new EditItem(plugin, methods));
      this.subCommands.add(new DeleteItem(plugin, methods));
      this.subCommands.add(new AddHandItem(plugin, methods));
      this.subCommands.add(new Import(plugin, methods));
      this.subCommands.add(new Migrate(plugin, methods));
      this.subCommands.add(new AddSection(plugin, methods));
      this.subCommands.add(new EditSection(plugin, methods));
      this.subCommands.add(new DeleteSection(plugin, methods));
      this.subCommands.add(new Update());
      this.subCommands.add(new UploadLayout());
      this.subCommands.add(new InstallLayout());
      this.subCommands.add(new UpdateLayout());
      this.subCommands.add(new GlobalStock(plugin, methods));
      this.subCommands.add(new PlayerStock(plugin, methods));
      this.subCommands.add(new GlobalSell(plugin, methods));
      this.subCommands.add(new PlayerSell(plugin, methods));
      this.subCommands.add(new DynamicPricing(plugin, methods));
      this.subCommands.add(new RotatingShop(plugin, methods));
      this.subCommands.add(new ShopStands(plugin, methods));
      this.subCommands.add(new TransactionLogs(plugin, methods));
      this.subCommands.add(new Modifiers(plugin, methods));
      this.subCommands.add(new Database(plugin));
   }

   @Override
   public boolean execute(CommandSender sender, String command, String[] values) {
      if (ConfigManager.badYaml != null) {
         SendMessage.sendMessage(sender, ChatColor.DARK_RED + "This command cannot be executed now, please fix the configuration formatting first!");
         return true;
      } else {
         if (sender instanceof Player) {
            Player player = (Player)sender;
            if (this.hasAccessInWorld(player)) {
               if (values.length > 0) {
                  for (SubCommad subCommad : this.subCommands) {
                     if (values[0].equalsIgnoreCase(subCommad.getName())) {
                        if (subCommad.hasPermission(sender)) {
                           subCommad.perform(sender, values);
                        } else {
                           SendMessage.chatToPlayer(player, Lang.NO_PERMISSIONS.get());
                        }

                        return true;
                     }
                  }
               }

               if (this.hasPermission(player)) {
                  this.sendAllSyntaxes(sender);
               } else {
                  SendMessage.chatToPlayer(player, Lang.NO_PERMISSIONS.get());
               }
            }
         } else if (sender instanceof ConsoleCommandSender || sender instanceof BlockCommandSender || sender instanceof RemoteConsoleCommandSender) {
            if (sender instanceof BlockCommandSender) {
               SendMessage.warnMessage(sender, Lang.NO_PERMISSIONS.get());
               return true;
            }

            if (values.length > 0) {
               if (values[0].equalsIgnoreCase("addhanditem")
                  || values[0].equalsIgnoreCase("update")
                  || values[0].equalsIgnoreCase("uploadLayout")
                  || values[0].equalsIgnoreCase("installLayout")) {
                  SendMessage.warnMessage(sender, Lang.REAL_PLAYER.get());
                  return true;
               }

               for (SubCommad subCommad : this.subCommands) {
                  if (values[0].equalsIgnoreCase(subCommad.getName())) {
                     if (subCommad.hasPermission(sender)) {
                        subCommad.perform(sender, values);
                     } else {
                        SendMessage.warnMessage(sender, Lang.NO_PERMISSIONS.get());
                     }

                     return true;
                  }
               }
            }

            this.sendAllSyntaxes(sender);
         }

         return true;
      }
   }

   private boolean hasPermission(CommandSender sender) {
      return this.subCommands.stream().anyMatch(element -> element.hasPermission(sender));
   }

   @Override
   public List<String> tabComplete(CommandSender sender, String command, String[] values) {
      if (!this.hasPermission(sender)) {
         return Collections.emptyList();
      } else {
         ArrayList values2 = new ArrayList();
         if (values.length == 1) {
            for (SubCommad subCommad : this.subCommands) {
               if (subCommad.hasPermission(sender)) {
                  values2.add(subCommad.getName());
               }
            }

            if (!values[0].isEmpty()) {
               ArrayList values3 = new ArrayList();
               StringUtil.copyPartialMatches(values[0], values2, values3);
               Collections.sort(values3);
               return values3;
            } else {
               return values2;
            }
         } else {
            if (values.length >= 2) {
               for (SubCommad subCommad : this.subCommands) {
                  if (values[0].equalsIgnoreCase(subCommad.getName()) && subCommad.hasPermission(sender)) {
                     return subCommad.getTabCompletion(values);
                  }
               }
            }

            return Collections.emptyList();
         }
      }
   }

   private void sendAllSyntaxes(Object value) {
      SendMessage.sendMessage(value, Lang.SELLALL_COMMAND_USAGES.get());
      StringBuilder builder = new StringBuilder();

      for (int size = 0; size < this.subCommands.size(); size++) {
         SubCommad subCommad = this.subCommands.get(size);
         if (subCommad.hasPermission((CommandSender)value)
            && (
               !(value instanceof ConsoleCommandSender)
                  || !(subCommad instanceof AddHandItem) && !(subCommad instanceof Update) && !(subCommad instanceof InstallLayout) && !(subCommad instanceof UploadLayout)
            )) {
            builder.append(ChatColor.WHITE + " - " + ChatColor.RESET);
            builder.append(subCommad.getSyntax()).append(ChatColor.DARK_GRAY).append(" - ").append(ChatColor.RESET).append(subCommad.getDescription());
            if (size != this.subCommands.size()) {
               builder.append("\n");
            }
         }
      }

      SendMessage.sendMessage(value, builder.toString());
   }
}
