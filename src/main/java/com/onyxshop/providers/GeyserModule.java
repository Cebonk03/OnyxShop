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

   public GeyserModule(OnyxShop plugin) {
      this.plugin = plugin;
      GeyserApi.api().eventBus().register(this, this);
      GeyserApi.api().eventBus().subscribe(this, GeyserPreInitializeEvent.class, this::onPreInitialize);
      GeyserApi.api().eventBus().subscribe(this, GeyserDefineCustomSkullsEvent.class, this::onDefineCustomSkulls);
   }

   public void onPreInitialize(GeyserPreInitializeEvent event) {
      SendMessage.infoMessage("Enabling Geyser module...");
   }

   public void onDefineCustomSkulls(GeyserDefineCustomSkullsEvent event) {
      GeyserModule.SkullTextures skullTextures = this.scrapeSkullTextures();
      SendMessage.infoMessage("Registering " + skullTextures.getSize() + " player head texture(s) for bedrock players...");

      for (String text : skullTextures.getTextures()) {
         event.register(text, SkullTextureType.PROFILE);
      }

      for (String text : skullTextures.getOwners()) {
         event.register(text, SkullTextureType.USERNAME);
      }
   }

   private GeyserModule.SkullTextures scrapeSkullTextures() {
      GeyserModule.SkullTextures skullTextures = new GeyserModule.SkullTextures();
      this.getNavBarTextures(skullTextures, "main-menu-nav-bar");
      this.getNavBarTextures(skullTextures, "shops-nav-bar");
      this.getNavBarTextures(skullTextures, "transaction-screens-nav-bar");
      this.getNavBarTextures(skullTextures, "sellgui-nav-bar");
      this.getNavBarTextures(skullTextures, "shop-search-nav-bar");
      this.getNavBarTextures(skullTextures, "buy-screen");
      this.getNavBarTextures(skullTextures, "sell-screen");
      this.getNavBarTextures(skullTextures, "buy-stacks-screen");

      for (String text : ConfigManager.getSections()) {
         if (ConfigManager.getSection(text).getBoolean("enabled", true)) {
            Config config = ConfigManager.getShop(text);
            if (config != null) {
               this.getShopNavBarTextures(skullTextures, text);
               ShopType shopType = this.plugin.getConfigManager().getShopType(text);

               for (String text2 : this.plugin.getConfigManager().getItemsRaw(text)) {
                  String shopId = shopType == ShopType.ROTATING ? text2 : "pages." + text2;
                  String text3 = config.getString(shopId + ".skullowner");
                  if (text3 != null && !text3.isEmpty() && text3.matches("[a-zA-Z0-9_]{3,16}")) {
                     skullTextures.addOwner(text3);
                  } else {
                     String text4 = config.getString(shopId + ".skull-texture");
                     if (text4 != null && !text4.isEmpty() && Base64.isBase64(text4)) {
                        skullTextures.addTexture(text4);
                     }
                  }
               }
            }
         }
      }

      return skullTextures;
   }

   private void getShopNavBarTextures(GeyserModule.SkullTextures skullTextures, String shopId) {
      if (!ConfigManager.getSection(shopId).getString("nav-bar.mode", "INHERIT").equalsIgnoreCase("DISABLED")) {
         ConfigurationSection section = ConfigManager.getSection(shopId).getConfigurationSection("nav-bar.items");
         if (section != null && !section.getKeys(false).isEmpty()) {
            for (String text : section.getKeys(false)) {
               String text2 = "nav-bar.items." + text;
               String text3 = ConfigManager.getSection(shopId).getString(text2 + ".skullowner");
               if (text3 != null && !text3.isEmpty() && text3.matches("[a-zA-Z0-9_]{3,16}")) {
                  skullTextures.addOwner(text3);
               } else {
                  String text4 = ConfigManager.getSection(shopId).getString(text2 + ".skull-texture");
                  if (text4 != null && !text4.isEmpty() && Base64.isBase64(text4)) {
                     skullTextures.addTexture(text4);
                  }
               }
            }
         }
      }
   }

   private void getNavBarTextures(GeyserModule.SkullTextures skullTextures, String text) {
      if (ConfigManager.getConfig().getBoolean(text + ".enabled", true)) {
         ConfigurationSection section = ConfigManager.getConfig().getConfigurationSection(text + ".items");
         if (section != null && !section.getKeys(false).isEmpty()) {
            for (String text2 : section.getKeys(false)) {
               String text3 = text + ".items." + text2;
               String text4 = ConfigManager.getConfig().getString(text3 + ".skullowner");
               if (text4 != null && !text4.isEmpty() && text4.matches("[a-zA-Z0-9_]{3,16}")) {
                  skullTextures.addOwner(text4);
               } else {
                  String text5 = ConfigManager.getConfig().getString(text3 + ".skull-texture");
                  if (text5 != null && !text5.isEmpty() && Base64.isBase64(text5)) {
                     skullTextures.addTexture(text5);
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

      public void addTexture(String text) {
         this.customTextures.add(text);
      }

      public void addOwner(String text) {
         this.skullOwners.add(text);
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
