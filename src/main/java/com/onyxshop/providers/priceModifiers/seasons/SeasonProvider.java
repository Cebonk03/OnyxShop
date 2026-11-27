package com.onyxshop.providers.priceModifiers.seasons;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import me.casperge.realisticseasons.api.SeasonChangeEvent;
import me.casperge.realisticseasons.api.SeasonsAPI;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.shops.ShopSection;
import com.onyxshop.objects.shops.ShopType;
import com.onyxshop.util.ChatUtil;
import com.onyxshop.util.exceptions.ModifierLoadException;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class SeasonProvider implements Listener {
   private final OnyxShop plugin;
   private final Map<String, SeasonProvider.SeasonModifiers> LOADED_MODIFIERS = new HashMap<>();
   private static final Map<String, Season> LOADED_SEASONS = new HashMap<>();
   private boolean ready = false;
   private SeasonsAPI api;

   public SeasonProvider(OnyxShop plugin) throws ModifierLoadException {
      this.plugin = plugin;
      if (plugin.getServer().getPluginManager().getPlugin("RealisticSeasons") != null) {
         try {
            this.api = SeasonsAPI.getInstance();
            this.loadModifiers(plugin);
            SendMessage.infoMessage(Lang.ENABLED_PLUGIN_HOOK.get().replace("%plugin%", "RealisticSeasons"));
            this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
         } catch (NullPointerException exception) {
            SendMessage.infoMessage("RealisticSeasons found, waiting...");
            Consumer<SeasonsAPI> task = element -> {
               if (element != null) {
                  SendMessage.infoMessage(Lang.ENABLED_PLUGIN_HOOK.get().replace("%plugin%", "RealisticSeasons"));
                  this.api = element;
                  this.loadModifiers(plugin);
                  this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
               } else {
                  SendMessage.warnMessage(Lang.FAILED_PLUGIN_INTEGRATION.get().replace("%plugin%", "RealisticSeasons"));
               }
            };
            plugin.runTaskLater(() -> task.accept(SeasonsAPI.getInstance()), 1L);
         }
      } else {
         throw new ModifierLoadException("Failed to find RealisticSeasons");
      }
   }

   public static Season getSeason(String text) {
      return LOADED_SEASONS.get(text);
   }

   private void setSeason(SeasonType seasonType, String type) {
      Season season = this.getSeason(seasonType);
      LOADED_SEASONS.put(type, season);
   }

   private Season getSeason(SeasonType seasonType) {
      String text = this.matchConfig(seasonType);
      if (text == null) {
         return new Season("null", "null", seasonType);
      } else {
         String text2 = ConfigManager.getConfig().getString("seasonal-lore." + text + ".icon", "null");
         String text3 = ConfigManager.getConfig().getString("seasonal-lore." + text + ".name", "null");
         return new Season(ChatUtil.formatColors(text2), ChatUtil.formatColors(text3), seasonType);
      }
   }

   private void loadModifiers(OnyxShop plugin) {
      SendMessage.infoMessage(Lang.LOADING_SEASON_MODIFIERS.get());

      for (World world : plugin.getServer().getWorlds()) {
         boolean flag = false;
         if (world.getEnvironment() != Environment.NETHER && world.getEnvironment() != Environment.THE_END) {
            SeasonType seasonType = SeasonType.get(this.api.getSeason(world));
            if (seasonType != null && seasonType != SeasonType.DISABLED) {
               String text = this.matchConfig(seasonType);
               if (text != null) {
                  this.setSeason(seasonType, world.getName());
                  Map newSeasonModifiers = this.getNewSeasonModifiers(world, seasonType, true);
                  if (newSeasonModifiers == null) {
                     SendMessage.warnMessage(Lang.NO_SEASON_MODIFIERS_FOUND.get().replace("%season%", seasonType.name()));
                     return;
                  }

                  if (!this.plugin.getSections().isEmpty()) {
                     this.enableModifiers(newSeasonModifiers);
                  } else {
                     plugin.runTaskLater(() -> this.enableModifiers(newSeasonModifiers), 1L);
                  }
               }
            }
         }
      }

      this.ready = true;
   }

   private void enableModifiers(Map<String, SeasonModifier> map) {
      for (String key : map.keySet()) {
         try {
            SeasonModifier seasonModifier = (SeasonModifier)map.get(key);
            if (key.split("\\.").length <= 2) {
               throw new NullPointerException();
            }

            this.plugin.getShopItem(key).addPriceModifier(seasonModifier);
            this.LOADED_MODIFIERS.computeIfAbsent(key, key2 -> new SeasonProvider.SeasonModifiers()).add(seasonModifier);
         } catch (NullPointerException exception) {
            SendMessage.warnMessage(Lang.CANNOT_LOAD_SEASON_MODIFIER.get().replace("%path%", key));
         }
      }
   }

   private void disableModifiers(String text) {
      for (Entry entry : this.LOADED_MODIFIERS.entrySet()) {
         SeasonModifier seasonModifier = ((SeasonProvider.SeasonModifiers)entry.getValue()).getByWorld(text);
         if (seasonModifier != null) {
            try {
               if (((String)entry.getKey()).split("\\.").length > 2) {
                  this.plugin.getShopItem((String)entry.getKey()).removePriceModifier(seasonModifier);
               } else {
                  ShopSection section = this.plugin.getSection(((String)entry.getKey()).split("\\.")[0]);
                  section.getShopItems().forEach(entry2 -> entry2.removePriceModifier(seasonModifier));
               }

               ((SeasonProvider.SeasonModifiers)entry.getValue()).removeByWorld(text);
            } catch (NullPointerException exception) {
               SendMessage.warnMessage("Failed to disable season modifier for '" + (String)entry.getKey() + "' because there was no loaded section/item found.");
            }
         }
      }
   }

   private String matchConfig(SeasonType seasonType) {
      for (String sectionId : ConfigManager.getConfig().getConfigurationSection("season-price-modifiers").getKeys(false)) {
         if (seasonType.name().equalsIgnoreCase(sectionId)) {
            return sectionId;
         }
      }

      return null;
   }

   private int getModifier(String text) {
      String text2 = ConfigManager.getConfig().getString(text, null);
      if (text2 == null) {
         SendMessage.errorMessage(Lang.INVALID_SEASON_MODIFIER.get().replace("%path%", text).replace("%current%", ConfigManager.getConfig().get(text) + ""));
         return 0;
      } else {
         text2 = text2.replace("%", "");

         try {
            int value = Integer.parseInt(text2);
            if (value <= 100 && value >= -100) {
               return value;
            }
         } catch (NumberFormatException exception) {
         }

         SendMessage.errorMessage(Lang.INVALID_SEASON_MODIFIER.get().replace("%path%", text).replace("%current%", ConfigManager.getConfig().get(text) + ""));
         return 0;
      }
   }

   private boolean exists(String text) {
      if (!this.plugin.getShopSections().contains(text.split("\\.")[0])
         || (text.split("\\.").length > 2 ? this.plugin.getShopItem(text) == null : !text.split("\\.")[1].equalsIgnoreCase("all"))) {
         SendMessage.warnMessage(Lang.CANNOT_LOAD_SEASON_MODIFIER.get().replace("%path%", text));
         return false;
      } else {
         return true;
      }
   }

   private boolean existsSoft(String text) {
      String text2 = text.split("\\.")[0];
      if (this.plugin.getShopSections().contains(text2)
         && (
            text.split("\\.").length > 2
               ? this.plugin.getConfigManager().getItemsRaw(text2).contains(text.split("\\.", 2)[1])
               : text.split("\\.")[1].equalsIgnoreCase("all")
         )) {
         if (!ConfigManager.getShop(text2)
            .contains((this.plugin.getConfigManager().getShopType(text2) == ShopType.CATEGORY ? "pages." : "") + text.split("\\.", 2)[1] + ".action")) {
            return true;
         } else {
            SendMessage.warnMessage(Lang.CANNOT_LOAD_SEASON_MODIFIER_ACTION_ITEM.get().replace("%path%", text));
            return false;
         }
      } else {
         SendMessage.warnMessage(Lang.CANNOT_LOAD_SEASON_MODIFIER.get().replace("%path%", text));
         return false;
      }
   }

   private Map<String, SeasonModifier> getNewSeasonModifiers(World world, SeasonType seasonType, boolean flag) {
      String text = this.matchConfig(seasonType);
      if (text == null) {
         return null;
      } else {
         LinkedHashMap map = new LinkedHashMap();
         int value = 0;

         for (String sectionId : ConfigManager.getConfig().getConfigurationSection("season-price-modifiers." + text).getKeys(false)) {
            for (String price : ConfigManager.getConfig().getConfigurationSection("season-price-modifiers." + text + "." + sectionId).getKeys(false)) {
               String text2 = sectionId + "." + price.replace(":", ".");
               if (flag ? this.existsSoft(text2) : this.exists(text2)) {
                  int modifier = this.getModifier("season-price-modifiers." + text + "." + sectionId + "." + price);
                  if (modifier != 0) {
                     value++;
                     SeasonModifier seasonModifier = new SeasonModifier(world.getName(), modifier);
                     if (price.equalsIgnoreCase("all")) {
                        this.plugin.getSection(sectionId).getShopItems().forEach(entry -> map.put(entry.getItemPath(), seasonModifier));
                     } else {
                        map.put(text2, seasonModifier);
                     }
                  }
               }
            }
         }

         if (value != 0) {
            SendMessage.infoMessage(
               Lang.LOADED_SEASON_MODIFIERS.get().replace("%total%", String.valueOf(value)).replace("%season%", seasonType.name()).replace("%world%", world.getName())
            );
         } else {
            SendMessage.warnMessage(Lang.NO_SEASON_MODIFIERS_FOUND.get().replace("%season%", seasonType.name()));
         }

         return map;
      }
   }

   public void reloadModifiers() {
      this.LOADED_MODIFIERS.clear();
      LOADED_SEASONS.clear();
      this.loadModifiers(this.plugin);
   }

   @EventHandler
   public void onSeasonChange(SeasonChangeEvent event) {
      if (!event.isCancelled()) {
         World world = event.getWorld();
         SeasonType seasonType = SeasonType.get(event.getNewSeason());
         SendMessage.infoMessage(Lang.SEASON_CHANGE.get().replace("%world%", world.getName()).replace("%season%", event.getNewSeason().name()));
         this.disableModifiers(world.getName());
         this.setSeason(seasonType, world.getName());
         if (seasonType != SeasonType.DISABLED) {
            Map newSeasonModifiers = this.getNewSeasonModifiers(world, seasonType, false);
            if (newSeasonModifiers == null) {
               SendMessage.warnMessage(Lang.NO_SEASON_MODIFIERS_FOUND.get().replace("%season%", seasonType.name()));
            } else {
               this.enableModifiers(newSeasonModifiers);
            }
         }
      }
   }

   private class SeasonModifiers extends ArrayList<SeasonModifier> {
      public SeasonModifiers() {
      }

      public SeasonProvider.SeasonModifiers append(SeasonModifier seasonModifier) {
         this.add(seasonModifier);
         return this;
      }

      public SeasonModifier getByWorld(String worldName) {
         for (SeasonModifier seasonModifier : this) {
            if (seasonModifier.getWorld().equals(worldName)) {
               return seasonModifier;
            }
         }

         return null;
      }

      public boolean removeByWorld(String worldName) {
         for (SeasonModifier seasonModifier : this) {
            if (seasonModifier.getWorld().equals(worldName)) {
               return this.remove(seasonModifier);
            }
         }

         return false;
      }
   }
}
