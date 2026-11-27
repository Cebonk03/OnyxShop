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
      String text = ConfigManager.getConfig().getString("update-checker.endpoint", "");
      this.endpoint = text;
      this.enabled = ConfigManager.getConfig().getBoolean("update-checker.enabled", false) && !text.isEmpty();
      if (!this.enabled) {
         this.LATEST_VERSION = this.CURRENT_VERSION;
         this.updateAvailable = false;
         return;
      }

      OnyxShop.getInstance()
         .runKillableTask(
            () -> {
               String text2 = null;
               try {
                  JSONObject jsonObject = JsonUtil.parseJson(OnyxShop.getInstance().getPost(this.endpoint));
                  text2 = (String)jsonObject.get("current_version");
               } catch (Exception exception) {
                  SendMessage.logDebugMessage("Failed to check for updates: " + exception.getMessage());
               }

               if (text2 != null) {
                  this.LATEST_VERSION = new UpdateChecker.Version(text2);
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

      public Version(String text) {
         this.ver = text;
         String[] values;
         if (text.matches(".*[a-zA-Z].*")) {
            values = text.replace("b", "").replace("a", "").replace("r", "").replace("-", "").split("\\.");
            this.dev = true;
         } else {
            values = text.split("\\.");
            this.dev = false;
         }

         try {
            this.major = values.length >= 1 ? parsePart(values[0]) : 0;
            this.minor = values.length >= 2 ? parsePart(values[1]) : 0;
            this.patch = values.length >= 3 ? parsePart(values[2]) : 0;
            this.beta = values.length >= 4 ? parsePart(values[3]) : 0;
         } catch (NumberFormatException exception) {
            OnyxShop.getInstance().getServer().getLogger().warning("Invalid version numbering for '" + text + "'");
         }
      }

      // Version parts may carry suffixes such as "1-SNAPSHOT"; only the leading digits count.
      private static int parsePart(String text) {
         String text2 = text.replaceAll("[^0-9]", "");
         return text2.isEmpty() ? 0 : Integer.parseInt(text2);
      }

      public boolean isGreater(UpdateChecker.Version version) {
         return this.major > version.major
            || this.major == version.major && this.minor > version.minor
            || this.major == version.major && this.minor == version.minor && this.patch > version.patch
            || this.major == version.major && this.minor == version.minor && this.patch == version.patch && this.beta > version.beta;
      }

      public boolean isSameOrHigher(UpdateChecker.Version version) {
         return this.major == version.major && this.minor == version.minor && this.patch == version.patch && this.beta >= version.beta
            || this.major == version.major && this.minor == version.minor && this.patch >= version.patch
            || this.major == version.major && this.minor >= version.minor
            || this.major >= version.major;
      }

      public boolean isSmaller(UpdateChecker.Version version) {
         return this.major < version.major
            || this.major == version.major && this.minor < version.minor
            || this.major == version.major && this.minor == version.minor && this.patch < version.patch
            || this.major == version.major && this.minor == version.minor && this.patch == version.patch && this.beta < version.beta;
      }

      public boolean isSame(UpdateChecker.Version version) {
         return this.major == version.major && this.minor == version.minor && this.patch == version.patch && this.beta == version.beta;
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
