package com.onyxshop.providers;

import java.text.ParseException;
import com.onyxshop.OnyxShop;
import com.onyxshop.database.types.PostgreSQL;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.util.ReflectUtil;
import com.onyxshop.util.TimeUtils;
import com.onyxshop.util.scheduler.ScheduledTask;
import org.bukkit.configuration.file.YamlConfiguration;
import org.spigotmc.SpigotConfig;

public class DisplayCacheProvider {
   private final OnyxShop plugin;
   private ScheduledTask task;

   public DisplayCacheProvider(OnyxShop plugin) {
      this.plugin = plugin;
      this.plugin.displayCache = true;
      this.task = this.setupTask();
   }

   public void reload() {
      this.task.cancel();
      this.task = this.setupTask();
   }

   public void stop() {
      this.task.cancel();
   }

   private ScheduledTask setupTask() {
      try {
         String text = ConfigManager.getConfig()
            .getString((this.plugin.getProviderManager().getDatabase() instanceof PostgreSQL ? "postgresql" : "mysql") + ".display-cache", "AUTO");
         long time = text.equals("AUTO") ? this.getBestInterval() : TimeUtils.getTime(text) / 1000L * 20L;
         SendMessage.infoMessage("Successfully enabled database display cache with interval of " + TimeUtils.getReadableTime(time * 50L));
         return this.plugin.runTaskAsyncTimer(this::loadDisplayCache, 300L, time);
      } catch (ParseException exception) {
         SendMessage.warnMessage("Failed to enable display cache with reason: ");
         exception.printStackTrace();
         return null;
      }
   }

   private long getBestInterval() {
      long value = 72000L;
      if (this.isBehindNetwork()) {
         value = 1200L;
      }

      return value;
   }

   private boolean isBehindNetwork() {
      try {
         return SpigotConfig.bungee
            || ((YamlConfiguration)ReflectUtil.invokeLegacyVersionMethod("getPaperConfig", this.plugin.getServer().spigot()))
               .getBoolean("proxies.velocity.enabled");
      } catch (Exception exception) {
         SendMessage.warnMessage("Failed to get paper config");
         exception.printStackTrace();
         return false;
      }
   }

   private void loadDisplayCache() {
      long startTime = System.currentTimeMillis();
      if (this.plugin.getLimitedStockProvider() != null) {
         this.plugin.getLimitedStockProvider().reloadDisplayCache();
      }

      if (this.plugin.getLimitedSellProvider() != null) {
         this.plugin.getLimitedSellProvider().reloadDisplayCache();
      }

      if (this.plugin.getDynamicPricingProvider() != null) {
         this.plugin.getDynamicPricingProvider().reloadDisplayCache();
      }
   }
}
