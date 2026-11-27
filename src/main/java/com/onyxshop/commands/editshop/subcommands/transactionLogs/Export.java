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

   public Export(OnyxShop plugin) {
      this.plugin = plugin;
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
   public boolean hasPermission(CommandSender sender) {
      return PermissionsCache.hasPermission(sender, "OnyxShop.editshop.logs." + this.getName());
   }

   @Override
   public void perform(Object value, String[] values) {
      SendMessage.infoMessage(value, "Exporting transaction data, this can take some time depending on the size of the transactions database...");
      this.plugin.runTaskAsync(() -> this.retrieveAndCreate(value));
   }

   public void retrieveAndCreate(Object value) {
      long startTime = System.currentTimeMillis();
      String text = this.plugin.getTransactionLog().exportLogs();

      try {
         new Gson().fromJson(text, JsonObject.class);
      } catch (JsonSyntaxException exception) {
         SendMessage.sendMessage(value, "A unknown error occurred while exporting logs data");
         exception.printStackTrace();
         return;
      }

      try {
         String text2 = this.export(text);
         JsonObject jsonObject = (JsonObject)new Gson().fromJson(text2, JsonObject.class);
         String asString = jsonObject.get("response").getAsString();
         if (asString.equals("error")) {
            SendMessage.errorMessage(value, "Failed to export logs at this time with reason: " + asString);
            return;
         }

         String asString2 = jsonObject.get("id").getAsString();
         this.plugin.runTask(() -> {
            SendMessage.infoMessage(value, "Took " + (System.currentTimeMillis() - startTime) + "ms to create logs view");
            SendMessage.infoMessage(value, "Log id: " + ChatColor.WHITE + asString2);
         });
      } catch (NullPointerException exception) {
         SendMessage.errorMessage(value, "Failed to export logs at this point, please try again soon.");
         exception.printStackTrace();
      } catch (Exception exception) {
         SendMessage.errorMessage(value, "Unable to reach API server at this point, see console for more details.");
         exception.printStackTrace();
      }
   }

   private String export(String text) throws IOException {
      URL url = new URL(MarketplaceIntegration.getServer() + "/exportLogs");
      HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
      httpURLConnection.setConnectTimeout(5000);
      httpURLConnection.setDoOutput(true);
      httpURLConnection.setRequestMethod("POST");
      httpURLConnection.setRequestProperty("Content-Type", "application/json");
      OutputStream outputStream = httpURLConnection.getOutputStream();
      OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
      outputStreamWriter.write(text);
      outputStreamWriter.flush();
      outputStreamWriter.close();
      outputStream.close();
      httpURLConnection.connect();
      return new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream())).readLine();
   }

   private File saveToFile(String text) {
      File file = new File(this.plugin.getDataFolder(), "logs.json");

      try {
         if (!file.exists()) {
            file.createNewFile();
         }
      } catch (IOException exception) {
         System.out.println("Failed to create logs.json");
      }

      try {
         FileWriter fileWriter = new FileWriter(file);

         try {
            fileWriter.write(text);
            fileWriter.flush();
         } catch (Throwable exception) {
            try {
               fileWriter.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }

            throw exception;
         }

         fileWriter.close();
      } catch (IOException exception) {
         System.err.println("Failed to save logs to 'logs.json'");
         exception.printStackTrace();
      }

      return file;
   }

   @Override
   public List<String> getTabCompletion(String[] values) {
      return null;
   }

   public final class SimpleTransaction {
      public final long date;
      public final String player;
      public final int amount;
      public final String itemPath;
      public final Transaction.Mode action;

      public SimpleTransaction(TransactionLog.SimpleTransaction simpleTransaction) {
         this.date = simpleTransaction.date;
         this.player = "OnyxShop";
         this.itemPath = simpleTransaction.itemPath;
         this.amount = simpleTransaction.amount;
         this.action = simpleTransaction.action;
      }
   }
}
