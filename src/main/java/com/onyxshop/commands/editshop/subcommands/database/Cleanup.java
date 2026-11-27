package com.onyxshop.commands.editshop.subcommands.database;

import java.io.File;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.commands.editshop.SubCommad;
import com.onyxshop.database.Database;
import com.onyxshop.database.types.MySQL;
import com.onyxshop.database.types.PostgreSQL;
import com.onyxshop.database.types.SQLite;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.util.ConfigUtil;
import com.onyxshop.util.ConfirmRequest;
import com.onyxshop.util.PermissionsCache;
import com.onyxshop.util.TimeUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Cleanup extends SubCommad {
   private final OnyxShop plugin;

   public Cleanup(OnyxShop plugin) {
      this.plugin = plugin;
   }

   @Override
   public String getName() {
      return "cleanup";
   }

   @Override
   public String getDescription() {
      return ChatColor.GREEN + "Cleanup old player/item data from the database which isn't used anymore";
   }

   @Override
   public String getSyntax() {
      return ChatColor.GREEN + "/editshop database cleanup <dataType>";
   }

   @Override
   public boolean hasPermission(CommandSender sender) {
      return PermissionsCache.hasPermission(sender, "OnyxShop.editshop.database." + this.getName());
   }

   @Override
   public void perform(Object value, String[] values) {
      if (ConfigManager.getConfig().getString("storage-type", "SQLite").equals("SQLite") && !new File(this.plugin.getDataFolder(), "database.db").exists()) {
         SendMessage.errorMessage(value, "Database file does not exist, no database to cleanup...");
      } else {
         if (values.length > 2) {
            Cleanup.DataType dataType = Cleanup.DataType.get(values[2]);
            if (dataType == null) {
               SendMessage.warnMessage(value, "Invalid data type for '" + values[2] + "'");
               return;
            }

            new ConfirmRequest(
               this.plugin,
               ChatColor.RED
                  + "Are you sure you want to continue?\n"
                  + ChatColor.RED
                  + "This command will remove expired player/item data from the database.\n"
                  + ChatColor.RED
                  + (
                     ConfigManager.getConfig().getString("storage-type", "SQLite").equals("SQLite")
                        ? "A backup of the database file will be saved to /OnyxShop/backups/database-<time>.db, please make sure you know what you are doing."
                        : "Backups from your MySQL/PostgreSQL database won't be taken automatically, please make sure you know what you are doing."
                  )
                  + ChatColor.GREEN
                  + "\nEnter 'yes' or 'no' in chat to continue.",
               (Player)value,
               element -> {
                  if (element) {
                     boolean flag = true;
                     Database database;
                     if (this.plugin.getProviderManager().getDatabase() == null) {
                        SendMessage.logDebugMessage("Database driver not running, starting temporarily connection now...");
                        flag = false;

                        try {
                           database = this.getTemporarilyDBConnection();
                        } catch (IllegalArgumentException exception) {
                           SendMessage.errorMessage(value, "Failed to establish temporarily database connection");
                           SendMessage.errorMessage("Failed to establish temporarily database connection");
                           exception.printStackTrace();
                           return;
                        }
                     } else {
                        database = this.plugin.getProviderManager().getDatabase();
                     }

                     this.plugin.runTaskAsync(() -> this.cleanExpiredData((Player)value, database, dataType));
                     if (!flag) {
                        SendMessage.logDebugMessage("Closing temporarily database connection...");
                        database.closeConnection();
                     }
                  } else {
                     SendMessage.warnMessage(value, "Action cancelled");
                  }
               }
            );
         } else {
            SendMessage.sendMessage(value, this.getSyntax());
         }
      }
   }

   @Override
   public List<String> getTabCompletion(String[] values) {
      return values.length == 3 ? Arrays.stream(Cleanup.DataType.values()).map(Enum::name).collect(Collectors.toList()) : null;
   }

   private Database getTemporarilyDBConnection() {
      String text = ConfigManager.getConfig().getString("storage-type", "");
      Object value;
      switch (text) {
         case "MySQL":
            value = new MySQL();
            break;
         case "PostgreSQL":
            value = new PostgreSQL();
            break;
         case "SQLite":
            value = new SQLite();
            break;
         default:
            SendMessage.warnMessage(Lang.INVALID_STORAGE_TYPE.get().replace("%type%", text));
            value = new SQLite();
      }

      ((Database)value).connect();
      return (Database)value;
   }

   private void cleanExpiredData(Player player, Database database, Cleanup.DataType dataType) {
      ConfigUtil.createBackup("database.db", false);
      if (dataType == Cleanup.DataType.ALL || dataType == Cleanup.DataType.PLAYER) {
         SendMessage.infoMessage(player, "Starting cleanup of expired player data...");
         String text = ConfigManager.getConfig().getString("database-cleanup.remove-player-data-after");

         try {
            long time = TimeUtils.getTime(text);
            int value = database.removeExpiredPlayerData(time);
            SendMessage.infoMessage(player, "Removed " + value + " unique expired player entries from database");
         } catch (IllegalArgumentException exception) {
            SendMessage.errorMessage(player, "Failed to remove expired player data, failed to parse expire date for player data from config as '" + text + "'");
            exception.printStackTrace();
            return;
         } catch (Exception exception) {
            SendMessage.warnMessage(player, "Error occurred while removing expired player data from database, see console for error.");
            SendMessage.errorMessage("Error occurred while removing expired player data from database with error:");
            exception.printStackTrace();
            return;
         }
      }

      if (dataType == Cleanup.DataType.ALL || dataType == Cleanup.DataType.ITEM) {
         SendMessage.infoMessage(player, "Starting cleanup of expired item data...");

         try {
            Map<String, Map<String, Set<String>>> expiredItemData = database.getExpiredItemData();
            long value = expiredItemData.entrySet()
               .stream()
               .flatMap(entry -> entry.getValue().keySet().stream().map(element -> entry.getKey() + "." + element))
               .distinct()
               .count();
            if (value < 1L) {
               SendMessage.warnMessage(player, "No expired item data found in database to remove");
               return;
            }

            new ConfirmRequest(
               this.plugin,
               ChatColor.RED
                  + "Found "
                  + value
                  + " unique shop items which aren't loaded and can be removed. Are you sure you want to remove this data from the database for limited stock, sell limits and/or dynamic pricing?"
                  + ChatColor.GREEN
                  + "\nEnter 'yes' or 'no' in chat to continue.",
               player,
               element -> {
                  try {
                     if (element) {
                        database.removeExpiredItemData(expiredItemData);
                        SendMessage.infoMessage(player, "Successfully removed " + value + " unique expired item entries from database");
                     } else {
                        SendMessage.warnMessage(player, "Action cancelled");
                     }
                  } catch (Exception exception) {
                     SendMessage.warnMessage(player, "Error occurred while removing expired item data from database, see console for error.");
                     SendMessage.errorMessage("Error occurred while removing expired player data from database with error:");
                     exception.printStackTrace();
                  }
               }
            );
         } catch (Exception exception) {
            SendMessage.warnMessage(player, "Error occurred while removing expired item data from database, see console for error.");
            SendMessage.errorMessage("Error occurred while removing expired player data from database with error:");
            exception.printStackTrace();
            return;
         }
      }
   }

   public static enum DataType {
      PLAYER,
      ITEM,
      ALL;

      private DataType() {
      }

      public static Cleanup.DataType get(String type) {
         try {
            return valueOf(type.toUpperCase());
         } catch (IllegalArgumentException exception) {
            return null;
         }
      }
   }
}
