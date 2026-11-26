package com.onyxshop.providers;

import java.util.HashSet;
import java.util.Set;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.config.Config;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.shops.ShopType;
import org.apache.commons.codec.binary.Base64;
import org.bukkit.configuration.ConfigurationSection;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomSkullsEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPreInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomSkullsEvent.SkullTextureType;

public class GeyserModule implements EventRegistrar {
   private final OnyxShop plugin;

   public GeyserModule(OnyxShop var1) {
      this.plugin = var1;
      GeyserApi.api().eventBus().register(this, this);
      GeyserApi.api().eventBus().subscribe(this, GeyserPreInitializeEvent.class, this::onPreInitialize);
      GeyserApi.api().eventBus().subscribe(this, GeyserDefineCustomSkullsEvent.class, this::onDefineCustomSkulls);
   }

   public void onPreInitialize(GeyserPreInitializeEvent var1) {
      SendMessage.infoMessage("Enabling Geyser module...");
   }

   public void onDefineCustomSkulls(GeyserDefineCustomSkullsEvent var1) {
      GeyserModule.SkullTextures var2 = this.scrapeSkullTextures();
      SendMessage.infoMessage("Registering " + var2.getSize() + " player head texture(s) for bedrock players...");

      for (String var4 : var2.getTextures()) {
         var1.register(var4, SkullTextureType.PROFILE);
      }

      for (String var6 : var2.getOwners()) {
         var1.register(var6, SkullTextureType.USERNAME);
      }
   }

   private GeyserModule.SkullTextures scrapeSkullTextures() {
      GeyserModule.SkullTextures var1 = new GeyserModule.SkullTextures();
      this.getNavBarTextures(var1, "main-menu-nav-bar");
      this.getNavBarTextures(var1, "shops-nav-bar");
      this.getNavBarTextures(var1, "transaction-screens-nav-bar");
      this.getNavBarTextures(var1, "sellgui-nav-bar");
      this.getNavBarTextures(var1, "shop-search-nav-bar");
      this.getNavBarTextures(var1, "buy-screen");
      this.getNavBarTextures(var1, "sell-screen");
      this.getNavBarTextures(var1, "buy-stacks-screen");

      for (String var3 : ConfigManager.getSections()) {
         if (ConfigManager.getSection(var3).getBoolean("enabled", true)) {
            Config var4 = ConfigManager.getShop(var3);
            if (var4 != null) {
               this.getShopNavBarTextures(var1, var3);
               ShopType var5 = this.plugin.getConfigManager().getShopType(var3);

               for (String var7 : this.plugin.getConfigManager().getItemsRaw(var3)) {
                  String var8 = var5 == ShopType.ROTATING ? var7 : "pages." + var7;
                  String var9 = var4.getString(var8 + ".skullowner");
                  if (var9 != null && !var9.isEmpty() && var9.matches("[a-zA-Z0-9_]{3,16}")) {
                     var1.addOwner(var9);
                  } else {
                     String var10 = var4.getString(var8 + ".skull-texture");
                     if (var10 != null && !var10.isEmpty() && Base64.isBase64(var10)) {
                        var1.addTexture(var10);
                     }
                  }
               }
            }
         }
      }

      return var1;
   }

   private void getShopNavBarTextures(GeyserModule.SkullTextures var1, String var2) {
      if (!ConfigManager.getSection(var2).getString("nav-bar.mode", "INHERIT").equalsIgnoreCase("DISABLED")) {
         ConfigurationSection var3 = ConfigManager.getSection(var2).getConfigurationSection("nav-bar.items");
         if (var3 != null && !var3.getKeys(false).isEmpty()) {
            for (String var5 : var3.getKeys(false)) {
               String var6 = "nav-bar.items." + var5;
               String var7 = ConfigManager.getSection(var2).getString(var6 + ".skullowner");
               if (var7 != null && !var7.isEmpty() && var7.matches("[a-zA-Z0-9_]{3,16}")) {
                  var1.addOwner(var7);
               } else {
                  String var8 = ConfigManager.getSection(var2).getString(var6 + ".skull-texture");
                  if (var8 != null && !var8.isEmpty() && Base64.isBase64(var8)) {
                     var1.addTexture(var8);
                  }
               }
            }
         }
      }
   }

   private void getNavBarTextures(GeyserModule.SkullTextures var1, String var2) {
      if (ConfigManager.getConfig().getBoolean(var2 + ".enabled", true)) {
         ConfigurationSection var3 = ConfigManager.getConfig().getConfigurationSection(var2 + ".items");
         if (var3 != null && !var3.getKeys(false).isEmpty()) {
            for (String var5 : var3.getKeys(false)) {
               String var6 = var2 + ".items." + var5;
               String var7 = ConfigManager.getConfig().getString(var6 + ".skullowner");
               if (var7 != null && !var7.isEmpty() && var7.matches("[a-zA-Z0-9_]{3,16}")) {
                  var1.addOwner(var7);
               } else {
                  String var8 = ConfigManager.getConfig().getString(var6 + ".skull-texture");
                  if (var8 != null && !var8.isEmpty() && Base64.isBase64(var8)) {
                     var1.addTexture(var8);
                  }
               }
            }
         }
      }
   }

   private class SkullTextures {
      final Set<String> customTextures = new HashSet<>();
      final Set<String> skullOwners = new HashSet<>();

      public SkullTextures() {
      }

      public void addTexture(String var1) {
         this.customTextures.add(var1);
      }

      public void addOwner(String var1) {
         this.skullOwners.add(var1);
      }

      public int getSize() {
         return this.customTextures.size() + this.skullOwners.size();
      }

      public Set<String> getTextures() {
         return this.customTextures;
      }

      public Set<String> getOwners() {
         return this.skullOwners;
      }
   }
}
