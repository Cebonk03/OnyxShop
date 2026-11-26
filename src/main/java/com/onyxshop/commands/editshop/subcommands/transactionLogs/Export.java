package com.onyxshop.commands.editshop.subcommands.transactionLogs;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import com.onyxshop.OnyxShop;
import com.onyxshop.commands.editshop.SubCommad;
import com.onyxshop.files.TransactionLog;
import com.onyxshop.methodes.MarketplaceIntegration;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.util.PermissionsCache;
import com.onyxshop.util.Transaction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class Export extends SubCommad {
   private final OnyxShop plugin;

   public Export(OnyxShop var1) {
      this.plugin = var1;
   }

   @Override
   public String getName() {
      return "export";
   }

   @Override
   public String getDescription() {
      return "Create a web based Transaction Log view of the currently logged transaction";
   }

   @Override
   public String getSyntax() {
      return "/editshop logs export";
   }

   @Override
   public boolean hasPermission(CommandSender var1) {
      return PermissionsCache.hasPermission(var1, "OnyxShop.editshop.logs." + this.getName());
   }

   @Override
   public void perform(Object var1, String[] var2) {
      SendMessage.infoMessage(var1, "Exporting transaction data, this can take some time depending on the size of the transactions database...");
      this.plugin.runTaskAsync(() -> this.retrieveAndCreate(var1));
   }

   public void retrieveAndCreate(Object var1) {
      long var2 = System.currentTimeMillis();
      String var4 = this.plugin.getTransactionLog().exportLogs();

      try {
         new Gson().fromJson(var4, JsonObject.class);
      } catch (JsonSyntaxException var11) {
         SendMessage.sendMessage(var1, "A unknown error occurred while exporting logs data");
         var11.printStackTrace();
         return;
      }

      try {
         String var5 = this.export(var4);
         JsonObject var6 = (JsonObject)new Gson().fromJson(var5, JsonObject.class);
         String var7 = var6.get("response").getAsString();
         if (var7.equals("error")) {
            SendMessage.errorMessage(var1, "Failed to export logs at this time with reason: " + var7);
            return;
         }

         String var8 = var6.get("id").getAsString();
         this.plugin.runTask(() -> {
            SendMessage.infoMessage(var1, "Took " + (System.currentTimeMillis() - var2) + "ms to create logs view");
            SendMessage.infoMessage(var1, "Log id: " + ChatColor.WHITE + var8);
         });
      } catch (NullPointerException var9) {
         SendMessage.errorMessage(var1, "Failed to export logs at this point, please try again soon.");
         var9.printStackTrace();
      } catch (Exception var10) {
         SendMessage.errorMessage(var1, "Unable to reach API server at this point, see console for more details.");
         var10.printStackTrace();
      }
   }

   private String export(String var1) throws IOException {
      URL var2 = new URL(MarketplaceIntegration.getServer() + "/exportLogs");
      HttpURLConnection var3 = (HttpURLConnection)var2.openConnection();
      var3.setConnectTimeout(5000);
      var3.setDoOutput(true);
      var3.setRequestMethod("POST");
      var3.setRequestProperty("Content-Type", "application/json");
      OutputStream var4 = var3.getOutputStream();
      OutputStreamWriter var5 = new OutputStreamWriter(var4, StandardCharsets.UTF_8);
      var5.write(var1);
      var5.flush();
      var5.close();
      var4.close();
      var3.connect();
      return new BufferedReader(new InputStreamReader(var3.getInputStream())).readLine();
   }

   private File saveToFile(String var1) {
      File var2 = new File(this.plugin.getDataFolder(), "logs.json");

      try {
         if (!var2.exists()) {
            var2.createNewFile();
         }
      } catch (IOException var9) {
         System.out.println("Failed to create logs.json");
      }

      try {
         FileWriter var3 = new FileWriter(var2);

         try {
            var3.write(var1);
            var3.flush();
         } catch (Throwable var7) {
            try {
               var3.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }

            throw var7;
         }

         var3.close();
      } catch (IOException var8) {
         System.err.println("Failed to save logs to 'logs.json'");
         var8.printStackTrace();
      }

      return var2;
   }

   @Override
   public List<String> getTabCompletion(String[] var1) {
      return null;
   }

   public final class SimpleTransaction {
      public final long date;
      public final String player;
      public final int amount;
      public final String itemPath;
      public final Transaction.Mode action;

      public SimpleTransaction(TransactionLog.SimpleTransaction nullx) {
         this.date = nullx.date;
         this.player = "OnyxShop";
         this.itemPath = nullx.itemPath;
         this.amount = nullx.amount;
         this.action = nullx.action;
      }
   }
}
