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

   public Cleanup(OnyxShop var1) {
      this.plugin = var1;
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
   public boolean hasPermission(CommandSender var1) {
      return PermissionsCache.hasPermission(var1, "OnyxShop.editshop.database." + this.getName());
   }

   @Override
   public void perform(Object var1, String[] var2) {
      if (ConfigManager.getConfig().getString("storage-type", "SQLite").equals("SQLite") && !new File(this.plugin.getDataFolder(), "database.db").exists()) {
         SendMessage.errorMessage(var1, "Database file does not exist, no database to cleanup...");
      } else {
         if (var2.length > 2) {
            Cleanup.DataType var3 = Cleanup.DataType.get(var2[2]);
            if (var3 == null) {
               SendMessage.warnMessage(var1, "Invalid data type for '" + var2[2] + "'");
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
               (Player)var1,
               var3x -> {
                  if (var3x) {
                     boolean var4 = true;
                     Database var5;
                     if (this.plugin.getProviderManager().getDatabase() == null) {
                        SendMessage.logDebugMessage("Database driver not running, starting temporarily connection now...");
                        var4 = false;

                        try {
                           var5 = this.getTemporarilyDBConnection();
                        } catch (IllegalArgumentException var7) {
                           SendMessage.errorMessage(var1, "Failed to establish temporarily database connection");
                           SendMessage.errorMessage("Failed to establish temporarily database connection");
                           var7.printStackTrace();
                           return;
                        }
                     } else {
                        var5 = this.plugin.getProviderManager().getDatabase();
                     }

                     this.plugin.runTaskAsync(() -> this.cleanExpiredData((Player)var1, var5, var3));
                     if (!var4) {
                        SendMessage.logDebugMessage("Closing temporarily database connection...");
                        var5.closeConnection();
                     }
                  } else {
                     SendMessage.warnMessage(var1, "Action cancelled");
                  }
               }
            );
         } else {
            SendMessage.sendMessage(var1, this.getSyntax());
         }
      }
   }

   @Override
   public List<String> getTabCompletion(String[] var1) {
      return var1.length == 3 ? Arrays.stream(Cleanup.DataType.values()).map(Enum::name).collect(Collectors.toList()) : null;
   }

   private Database getTemporarilyDBConnection() {
      String var2 = ConfigManager.getConfig().getString("storage-type", "");
      Object var1;
      switch (var2) {
         case "MySQL":
            var1 = new MySQL();
            break;
         case "PostgreSQL":
            var1 = new PostgreSQL();
            break;
         case "SQLite":
            var1 = new SQLite();
            break;
         default:
            SendMessage.warnMessage(Lang.INVALID_STORAGE_TYPE.get().replace("%type%", var2));
            var1 = new SQLite();
      }

      ((Database)var1).connect();
      return (Database)var1;
   }

   private void cleanExpiredData(Player var1, Database var2, Cleanup.DataType var3) {
      ConfigUtil.createBackup("database.db", false);
      if (var3 == Cleanup.DataType.ALL || var3 == Cleanup.DataType.PLAYER) {
         SendMessage.infoMessage(var1, "Starting cleanup of expired player data...");
         String var4 = ConfigManager.getConfig().getString("database-cleanup.remove-player-data-after");

         try {
            long var5 = TimeUtils.getTime(var4);
            int var7 = var2.removeExpiredPlayerData(var5);
            SendMessage.infoMessage(var1, "Removed " + var7 + " unique expired player entries from database");
         } catch (IllegalArgumentException var9) {
            SendMessage.errorMessage(var1, "Failed to remove expired player data, failed to parse expire date for player data from config as '" + var4 + "'");
            var9.printStackTrace();
            return;
         } catch (Exception var10) {
            SendMessage.warnMessage(var1, "Error occurred while removing expired player data from database, see console for error.");
            SendMessage.errorMessage("Error occurred while removing expired player data from database with error:");
            var10.printStackTrace();
            return;
         }
      }

      if (var3 == Cleanup.DataType.ALL || var3 == Cleanup.DataType.ITEM) {
         SendMessage.infoMessage(var1, "Starting cleanup of expired item data...");

         try {
            Map<String, Map<String, Set<String>>> var11 = var2.getExpiredItemData();
            long var12 = var11.entrySet()
               .stream()
               .flatMap(var0 -> var0.getValue().keySet().stream().map(var1x -> var0.getKey() + "." + var1x))
               .distinct()
               .count();
            if (var12 < 1L) {
               SendMessage.warnMessage(var1, "No expired item data found in database to remove");
               return;
            }

            new ConfirmRequest(
               this.plugin,
               ChatColor.RED
                  + "Found "
                  + var12
                  + " unique shop items which aren't loaded and can be removed. Are you sure you want to remove this data from the database for limited stock, sell limits and/or dynamic pricing?"
                  + ChatColor.GREEN
                  + "\nEnter 'yes' or 'no' in chat to continue.",
               var1,
               var5x -> {
                  try {
                     if (var5x) {
                        var2.removeExpiredItemData(var11);
                        SendMessage.infoMessage(var1, "Successfully removed " + var12 + " unique expired item entries from database");
                     } else {
                        SendMessage.warnMessage(var1, "Action cancelled");
                     }
                  } catch (Exception var7x) {
                     SendMessage.warnMessage(var1, "Error occurred while removing expired item data from database, see console for error.");
                     SendMessage.errorMessage("Error occurred while removing expired player data from database with error:");
                     var7x.printStackTrace();
                  }
               }
            );
         } catch (Exception var8) {
            SendMessage.warnMessage(var1, "Error occurred while removing expired item data from database, see console for error.");
            SendMessage.errorMessage("Error occurred while removing expired player data from database with error:");
            var8.printStackTrace();
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

      public static Cleanup.DataType get(String var0) {
         try {
            return valueOf(var0.toUpperCase());
         } catch (IllegalArgumentException var2) {
            return null;
         }
      }
   }
}
