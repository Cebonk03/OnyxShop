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

   public SeasonProvider(OnyxShop var1) throws ModifierLoadException {
      this.plugin = var1;
      if (var1.getServer().getPluginManager().getPlugin("RealisticSeasons") != null) {
         try {
            this.api = SeasonsAPI.getInstance();
            this.loadModifiers(var1);
            SendMessage.infoMessage(Lang.ENABLED_PLUGIN_HOOK.get().replace("%plugin%", "RealisticSeasons"));
            this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
         } catch (NullPointerException var4) {
            SendMessage.infoMessage("RealisticSeasons found, waiting...");
            Consumer<SeasonsAPI> var3 = var2 -> {
               if (var2 != null) {
                  SendMessage.infoMessage(Lang.ENABLED_PLUGIN_HOOK.get().replace("%plugin%", "RealisticSeasons"));
                  this.api = var2;
                  this.loadModifiers(var1);
                  this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
               } else {
                  SendMessage.warnMessage(Lang.FAILED_PLUGIN_INTEGRATION.get().replace("%plugin%", "RealisticSeasons"));
               }
            };
            var1.runTaskLater(() -> var3.accept(SeasonsAPI.getInstance()), 1L);
         }
      } else {
         throw new ModifierLoadException("Failed to find RealisticSeasons");
      }
   }

   public static Season getSeason(String var0) {
      return LOADED_SEASONS.get(var0);
   }

   private void setSeason(SeasonType var1, String var2) {
      Season var3 = this.getSeason(var1);
      LOADED_SEASONS.put(var2, var3);
   }

   private Season getSeason(SeasonType var1) {
      String var2 = this.matchConfig(var1);
      if (var2 == null) {
         return new Season("null", "null", var1);
      } else {
         String var3 = ConfigManager.getConfig().getString("seasonal-lore." + var2 + ".icon", "null");
         String var4 = ConfigManager.getConfig().getString("seasonal-lore." + var2 + ".name", "null");
         return new Season(ChatUtil.formatColors(var3), ChatUtil.formatColors(var4), var1);
      }
   }

   private void loadModifiers(OnyxShop var1) {
      SendMessage.infoMessage(Lang.LOADING_SEASON_MODIFIERS.get());

      for (World var3 : var1.getServer().getWorlds()) {
         boolean var4 = false;
         if (var3.getEnvironment() != Environment.NETHER && var3.getEnvironment() != Environment.THE_END) {
            SeasonType var5 = SeasonType.get(this.api.getSeason(var3));
            if (var5 != null && var5 != SeasonType.DISABLED) {
               String var6 = this.matchConfig(var5);
               if (var6 != null) {
                  this.setSeason(var5, var3.getName());
                  Map var7 = this.getNewSeasonModifiers(var3, var5, true);
                  if (var7 == null) {
                     SendMessage.warnMessage(Lang.NO_SEASON_MODIFIERS_FOUND.get().replace("%season%", var5.name()));
                     return;
                  }

                  if (!this.plugin.getSections().isEmpty()) {
                     this.enableModifiers(var7);
                  } else {
                     var1.runTaskLater(() -> this.enableModifiers(var7), 1L);
                  }
               }
            }
         }
      }

      this.ready = true;
   }

   private void enableModifiers(Map<String, SeasonModifier> var1) {
      for (String var3 : var1.keySet()) {
         try {
            SeasonModifier var4 = (SeasonModifier)var1.get(var3);
            if (var3.split("\\.").length <= 2) {
               throw new NullPointerException();
            }

            this.plugin.getShopItem(var3).addPriceModifier(var4);
            this.LOADED_MODIFIERS.computeIfAbsent(var3, var1x -> new SeasonProvider.SeasonModifiers()).add(var4);
         } catch (NullPointerException var5) {
            SendMessage.warnMessage(Lang.CANNOT_LOAD_SEASON_MODIFIER.get().replace("%path%", var3));
         }
      }
   }

   private void disableModifiers(String var1) {
      for (Entry var3 : this.LOADED_MODIFIERS.entrySet()) {
         SeasonModifier var4 = ((SeasonProvider.SeasonModifiers)var3.getValue()).getByWorld(var1);
         if (var4 != null) {
            try {
               if (((String)var3.getKey()).split("\\.").length > 2) {
                  this.plugin.getShopItem((String)var3.getKey()).removePriceModifier(var4);
               } else {
                  ShopSection var5 = this.plugin.getSection(((String)var3.getKey()).split("\\.")[0]);
                  var5.getShopItems().forEach(var1x -> var1x.removePriceModifier(var4));
               }

               ((SeasonProvider.SeasonModifiers)var3.getValue()).removeByWorld(var1);
            } catch (NullPointerException var6) {
               SendMessage.warnMessage("Failed to disable season modifier for '" + (String)var3.getKey() + "' because there was no loaded section/item found.");
            }
         }
      }
   }

   private String matchConfig(SeasonType var1) {
      for (String var3 : ConfigManager.getConfig().getConfigurationSection("season-price-modifiers").getKeys(false)) {
         if (var1.name().equalsIgnoreCase(var3)) {
            return var3;
         }
      }

      return null;
   }

   private int getModifier(String var1) {
      String var2 = ConfigManager.getConfig().getString(var1, null);
      if (var2 == null) {
         SendMessage.errorMessage(Lang.INVALID_SEASON_MODIFIER.get().replace("%path%", var1).replace("%current%", ConfigManager.getConfig().get(var1) + ""));
         return 0;
      } else {
         var2 = var2.replace("%", "");

         try {
            int var3 = Integer.parseInt(var2);
            if (var3 <= 100 && var3 >= -100) {
               return var3;
            }
         } catch (NumberFormatException var4) {
         }

         SendMessage.errorMessage(Lang.INVALID_SEASON_MODIFIER.get().replace("%path%", var1).replace("%current%", ConfigManager.getConfig().get(var1) + ""));
         return 0;
      }
   }

   private boolean exists(String var1) {
      if (!this.plugin.getShopSections().contains(var1.split("\\.")[0])
         || (var1.split("\\.").length > 2 ? this.plugin.getShopItem(var1) == null : !var1.split("\\.")[1].equalsIgnoreCase("all"))) {
         SendMessage.warnMessage(Lang.CANNOT_LOAD_SEASON_MODIFIER.get().replace("%path%", var1));
         return false;
      } else {
         return true;
      }
   }

   private boolean existsSoft(String var1) {
      String var2 = var1.split("\\.")[0];
      if (this.plugin.getShopSections().contains(var2)
         && (
            var1.split("\\.").length > 2
               ? this.plugin.getConfigManager().getItemsRaw(var2).contains(var1.split("\\.", 2)[1])
               : var1.split("\\.")[1].equalsIgnoreCase("all")
         )) {
         if (!ConfigManager.getShop(var2)
            .contains((this.plugin.getConfigManager().getShopType(var2) == ShopType.CATEGORY ? "pages." : "") + var1.split("\\.", 2)[1] + ".action")) {
            return true;
         } else {
            SendMessage.warnMessage(Lang.CANNOT_LOAD_SEASON_MODIFIER_ACTION_ITEM.get().replace("%path%", var1));
            return false;
         }
      } else {
         SendMessage.warnMessage(Lang.CANNOT_LOAD_SEASON_MODIFIER.get().replace("%path%", var1));
         return false;
      }
   }

   private Map<String, SeasonModifier> getNewSeasonModifiers(World var1, SeasonType var2, boolean var3) {
      String var4 = this.matchConfig(var2);
      if (var4 == null) {
         return null;
      } else {
         LinkedHashMap var5 = new LinkedHashMap();
         int var6 = 0;

         for (String var8 : ConfigManager.getConfig().getConfigurationSection("season-price-modifiers." + var4).getKeys(false)) {
            for (String var10 : ConfigManager.getConfig().getConfigurationSection("season-price-modifiers." + var4 + "." + var8).getKeys(false)) {
               String var11 = var8 + "." + var10.replace(":", ".");
               if (var3 ? this.existsSoft(var11) : this.exists(var11)) {
                  int var12 = this.getModifier("season-price-modifiers." + var4 + "." + var8 + "." + var10);
                  if (var12 != 0) {
                     var6++;
                     SeasonModifier var13 = new SeasonModifier(var1.getName(), var12);
                     if (var10.equalsIgnoreCase("all")) {
                        this.plugin.getSection(var8).getShopItems().forEach(var2x -> var5.put(var2x.getItemPath(), var13));
                     } else {
                        var5.put(var11, var13);
                     }
                  }
               }
            }
         }

         if (var6 != 0) {
            SendMessage.infoMessage(
               Lang.LOADED_SEASON_MODIFIERS.get().replace("%total%", String.valueOf(var6)).replace("%season%", var2.name()).replace("%world%", var1.getName())
            );
         } else {
            SendMessage.warnMessage(Lang.NO_SEASON_MODIFIERS_FOUND.get().replace("%season%", var2.name()));
         }

         return var5;
      }
   }

   public void reloadModifiers() {
      this.LOADED_MODIFIERS.clear();
      LOADED_SEASONS.clear();
      this.loadModifiers(this.plugin);
   }

   @EventHandler
   public void onSeasonChange(SeasonChangeEvent var1) {
      if (!var1.isCancelled()) {
         World var2 = var1.getWorld();
         SeasonType var3 = SeasonType.get(var1.getNewSeason());
         SendMessage.infoMessage(Lang.SEASON_CHANGE.get().replace("%world%", var2.getName()).replace("%season%", var1.getNewSeason().name()));
         this.disableModifiers(var2.getName());
         this.setSeason(var3, var2.getName());
         if (var3 != SeasonType.DISABLED) {
            Map var4 = this.getNewSeasonModifiers(var2, var3, false);
            if (var4 == null) {
               SendMessage.warnMessage(Lang.NO_SEASON_MODIFIERS_FOUND.get().replace("%season%", var3.name()));
            } else {
               this.enableModifiers(var4);
            }
         }
      }
   }

   private class SeasonModifiers extends ArrayList<SeasonModifier> {
      public SeasonModifiers() {
      }

      public SeasonProvider.SeasonModifiers append(SeasonModifier var1) {
         this.add(var1);
         return this;
      }

      public SeasonModifier getByWorld(String var1) {
         for (SeasonModifier var3 : this) {
            if (var3.getWorld().equals(var1)) {
               return var3;
            }
         }

         return null;
      }

      public boolean removeByWorld(String var1) {
         for (SeasonModifier var3 : this) {
            if (var3.getWorld().equals(var1)) {
               return this.remove(var3);
            }
         }

         return false;
      }
   }
}
