package com.onyxshop.methodes;

import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.util.JsonUtil;
import org.json.simple.JSONObject;

public class UpdateChecker {
   private final boolean enabled;
   private final String endpoint;
   public final UpdateChecker.Version CURRENT_VERSION = new UpdateChecker.Version(OnyxShop.getInstance().getDescription().getVersion());
   public UpdateChecker.Version LATEST_VERSION;
   public boolean updateAvailable;

   public UpdateChecker() {
      String var1 = ConfigManager.getConfig().getString("update-checker.endpoint", "");
      this.endpoint = var1;
      this.enabled = ConfigManager.getConfig().getBoolean("update-checker.enabled", false) && !var1.isEmpty();
      if (!this.enabled) {
         this.LATEST_VERSION = this.CURRENT_VERSION;
         this.updateAvailable = false;
         return;
      }

      OnyxShop.getInstance()
         .runKillableTask(
            () -> {
               String var1x = null;
               try {
                  JSONObject var2 = JsonUtil.parseJson(OnyxShop.getInstance().getPost(this.endpoint));
                  var1x = (String)var2.get("current_version");
               } catch (Exception var3) {
                  SendMessage.logDebugMessage("Failed to check for updates: " + var3.getMessage());
               }

               if (var1x != null) {
                  this.LATEST_VERSION = new UpdateChecker.Version(var1x);
                  this.updateAvailable = this.isUpdateAvailable();
                  this.checkForUpdates();
               } else {
                  this.updateAvailable = false;
                  SendMessage.errorMessage(Lang.COULD_NOT_CHECK_FOR_UPDATES.get());
               }
            }
         );
   }

   private boolean isUpdateAvailable() {
      return this.CURRENT_VERSION.isSmaller(this.LATEST_VERSION) ? this.CURRENT_VERSION.isDev() || !this.LATEST_VERSION.isDev() : false;
   }

   public void checkForUpdates() {
      if (this.updateAvailable) {
         SendMessage.infoMessage(
            Lang.UPDATE_AVAILABLE.get().replace("%plugin_version%", this.CURRENT_VERSION.getVer()).replace("%latest_version%", this.LATEST_VERSION.getVer())
         );
         SendMessage.infoMessage("An update is available: v" + this.LATEST_VERSION.getVer());
      }
   }

   public static final class Version {
      private final String ver;
      private final boolean dev;
      private int major = 0;
      private int minor = 0;
      private int patch = 0;
      private int beta = 0;

      public Version(String var1) {
         this.ver = var1;
         String[] var2;
         if (var1.matches(".*[a-zA-Z].*")) {
            var2 = var1.replace("b", "").replace("a", "").replace("r", "").replace("-", "").split("\\.");
            this.dev = true;
         } else {
            var2 = var1.split("\\.");
            this.dev = false;
         }

         try {
            this.major = var2.length >= 1 ? parsePart(var2[0]) : 0;
            this.minor = var2.length >= 2 ? parsePart(var2[1]) : 0;
            this.patch = var2.length >= 3 ? parsePart(var2[2]) : 0;
            this.beta = var2.length >= 4 ? parsePart(var2[3]) : 0;
         } catch (NumberFormatException var4) {
            OnyxShop.getInstance().getServer().getLogger().warning("Invalid version numbering for '" + var1 + "'");
         }
      }

      // Version parts may carry suffixes such as "1-SNAPSHOT"; only the leading digits count.
      private static int parsePart(String var0) {
         String var1 = var0.replaceAll("[^0-9]", "");
         return var1.isEmpty() ? 0 : Integer.parseInt(var1);
      }

      public boolean isGreater(UpdateChecker.Version var1) {
         return this.major > var1.major
            || this.major == var1.major && this.minor > var1.minor
            || this.major == var1.major && this.minor == var1.minor && this.patch > var1.patch
            || this.major == var1.major && this.minor == var1.minor && this.patch == var1.patch && this.beta > var1.beta;
      }

      public boolean isSameOrHigher(UpdateChecker.Version var1) {
         return this.major == var1.major && this.minor == var1.minor && this.patch == var1.patch && this.beta >= var1.beta
            || this.major == var1.major && this.minor == var1.minor && this.patch >= var1.patch
            || this.major == var1.major && this.minor >= var1.minor
            || this.major >= var1.major;
      }

      public boolean isSmaller(UpdateChecker.Version var1) {
         return this.major < var1.major
            || this.major == var1.major && this.minor < var1.minor
            || this.major == var1.major && this.minor == var1.minor && this.patch < var1.patch
            || this.major == var1.major && this.minor == var1.minor && this.patch == var1.patch && this.beta < var1.beta;
      }

      public boolean isSame(UpdateChecker.Version var1) {
         return this.major == var1.major && this.minor == var1.minor && this.patch == var1.patch && this.beta == var1.beta;
      }

      public boolean isDev() {
         return this.dev;
      }

      public String getVer() {
         return this.ver;
      }

      @Override
      public String toString() {
         return this.major + "." + this.minor + "." + this.patch;
      }
   }
}
