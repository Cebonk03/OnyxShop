package com.onyxshop.providers;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.PlayerPriceModifier;
import com.onyxshop.objects.PriceModifierData;
import com.onyxshop.objects.PriceModifierType;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.providers.priceModifiers.CommandModifier;
import com.onyxshop.providers.priceModifiers.ModifierType;
import com.onyxshop.providers.priceModifiers.seasons.SeasonProvider;
import com.onyxshop.util.exceptions.ModifierLoadException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ModifierManager {
   private final OnyxShop plugin;
   private static SeasonProvider seasonProvider;
   private final Map<UUID, PlayerPriceModifier> COMMAND_MODIFIERS = new HashMap<>();

   public ModifierManager(OnyxShop var1) {
      this.plugin = var1;
      this.loadModifiers();
   }

   private void loadModifiers() {
      this.loadSeasonModifier();
   }

   private void loadSeasonModifier() {
      if (ConfigManager.getConfig().getBoolean("enable-seasonal-pricing", false)) {
         try {
            SendMessage.infoMessage("Loading season modifiers...");
            seasonProvider = new SeasonProvider(this.plugin);
         } catch (ModifierLoadException var2) {
            SendMessage.errorMessage("Failed to load SeasonModifiers from config for reason: " + var2.getMessage());
         }
      }
   }

   public boolean hasSeasonalPrices() {
      return seasonProvider != null;
   }

   public void reloadModifiers() {
      if (this.hasSeasonalPrices()) {
         seasonProvider.reloadModifiers();
      } else {
         this.loadSeasonModifier();
      }

      this.reloadForOnlinePlayersAsync();
   }

   private PlayerPriceModifier loadCommandModifiersFromDB(UUID var1) {
      if (this.plugin.getProviderManager().getDatabase() != null) {
         try {
            return this.plugin.getProviderManager().getDatabase().loadPlayerPriceModifiers(var1);
         } catch (SQLException var3) {
            SendMessage.warnMessage("Failed to load command modifiers for player because database error occurred: " + var3.getMessage());
            return null;
         }
      } else {
         SendMessage.warnMessage("Failed to load command modifiers for player because database connection isn't loaded");
         return null;
      }
   }

   private void reloadForOnlinePlayersAsync() {
      Collection<? extends Player> var1 = this.plugin.getServer().getOnlinePlayers();
      SendMessage.logDebugMessage("Loading item price modifiers for " + var1.size() + " online player(s)");
      this.plugin.runTaskAsync(() -> {
         for (Player var3 : var1) {
            this.loadCommandModifiersForPlayer(var3);
         }
      });
   }

   public void stop() {
      if (!this.COMMAND_MODIFIERS.isEmpty()) {
         SendMessage.logDebugMessage(Lang.SAVING_PRICE_MODIFIERS.get().replace("%count%", String.valueOf(this.COMMAND_MODIFIERS.size())));

         try {
            for (Entry var2 : this.COMMAND_MODIFIERS.entrySet()) {
               this.plugin.getProviderManager().getDatabase().savePlayerPriceModifiers((UUID)var2.getKey(), (PlayerPriceModifier)var2.getValue());
            }
         } catch (SQLException var3) {
            SendMessage.errorMessage(Lang.ERROR_SAVING_PRICE_MODIFIERS.get().replace("%message%", var3.getMessage()));
            var3.printStackTrace();
         }
      }
   }

   public void loadCommandModifiersForPlayer(Player var1) {
      PlayerPriceModifier var2 = this.COMMAND_MODIFIERS.getOrDefault(var1.getUniqueId(), this.loadCommandModifiersFromDB(var1.getUniqueId()));
      if (var2 != null) {
         for (String var4 : var2.getSectionModifiers().keySet()) {
            PriceModifierData var5 = var2.getSectionModifiers().get(var4);

            try {
               if (!this.plugin.getShopSections().contains(var4)) {
                  throw new ModifierLoadException("Shop " + var4 + " doesn't exist");
               }

               Double var6 = var5.getOr(PriceModifierType.BOTH, -1.0) > var5.getOr(PriceModifierType.BUY, -1.0)
                  ? var5.get(PriceModifierType.BOTH)
                  : var5.get(PriceModifierType.BUY);
               Double var7 = var5.getOr(PriceModifierType.BOTH, -1.0) > var5.getOr(PriceModifierType.SELL, -1.0)
                  ? var5.get(PriceModifierType.BOTH)
                  : var5.get(PriceModifierType.SELL);

               for (ShopItem var9 : this.plugin.getSection(var4).getShopItems()) {
                  if (var6 != null && var6 >= 0.0) {
                     ((CommandModifier)var9.getBuyModifiers().putIfAbsent(ModifierType.COMMAND, new CommandModifier()))
                        .setBestModifier(var1.getUniqueId(), var6);
                  }

                  if (var7 != null && var7 >= 0.0) {
                     ((CommandModifier)var9.getSellModifiers().putIfAbsent(ModifierType.COMMAND, new CommandModifier()))
                        .setBestModifier(var1.getUniqueId(), var7);
                  }
               }
            } catch (ModifierLoadException var11) {
               SendMessage.warnMessage(
                  "Failed to load command modifier for player '" + var1.getName() + "' for shop section '" + var4 + "': " + var11.getMessage()
               );
            }
         }

         for (String var13 : var2.getItemModifiers().keySet()) {
            PriceModifierData var14 = var2.getItemModifiers().get(var13);

            try {
               ShopItem var15 = this.plugin.getShopItem(var13);
               if (var15 == null) {
                  throw new ModifierLoadException("Item " + var13 + " doesn't exist");
               }

               Double var16 = var14.getOr(PriceModifierType.BOTH, -1.0) > var14.getOr(PriceModifierType.BUY, -1.0)
                  ? var14.get(PriceModifierType.BOTH)
                  : var14.get(PriceModifierType.BUY);
               Double var17 = var14.getOr(PriceModifierType.BOTH, -1.0) > var14.getOr(PriceModifierType.SELL, -1.0)
                  ? var14.get(PriceModifierType.BOTH)
                  : var14.get(PriceModifierType.SELL);
               if (var16 != null && var16 >= 0.0) {
                  ((CommandModifier)var15.getBuyModifiers().putIfAbsent(ModifierType.COMMAND, new CommandModifier()))
                     .setBestModifier(var1.getUniqueId(), var16);
               }

               if (var17 != null && var17 >= 0.0) {
                  ((CommandModifier)var15.getSellModifiers().putIfAbsent(ModifierType.COMMAND, new CommandModifier()))
                     .setBestModifier(var1.getUniqueId(), var17);
               }
            } catch (ModifierLoadException var10) {
               SendMessage.warnMessage("Failed to load command modifier for player '" + var1.getName() + "' for item '" + var13 + "': " + var10.getMessage());
            }
         }

         this.COMMAND_MODIFIERS.put(var1.getUniqueId(), var2);
      }
   }

   public void unloadAndSaveCommandModifiers(UUID var1) {
      PlayerPriceModifier var2 = this.COMMAND_MODIFIERS.get(var1);
      if (var2 != null) {
         for (String var4 : var2.getSectionModifiers().keySet()) {
            if (this.plugin.getShopSections().contains(var4)) {
               for (ShopItem var6 : this.plugin.getSection(var4).getShopItems()) {
                  CommandModifier var7 = (CommandModifier)var6.getBuyModifiers().getByType(ModifierType.COMMAND);
                  if (var7 != null) {
                     var7.removeModifier(var1);
                  }

                  CommandModifier var8 = (CommandModifier)var6.getSellModifiers().getByType(ModifierType.COMMAND);
                  if (var8 != null) {
                     var8.removeModifier(var1);
                  }
               }
            }
         }

         for (String var11 : var2.getItemModifiers().keySet()) {
            ShopItem var12 = this.plugin.getShopItem(var11);
            if (var12 != null) {
               CommandModifier var13 = (CommandModifier)var12.getBuyModifiers().getByType(ModifierType.COMMAND);
               if (var13 != null) {
                  var13.removeModifier(var1);
               }

               CommandModifier var14 = (CommandModifier)var12.getSellModifiers().getByType(ModifierType.COMMAND);
               if (var14 != null) {
                  var14.removeModifier(var1);
               }
            }
         }

         this.COMMAND_MODIFIERS.remove(var1);

         try {
            this.plugin.getProviderManager().getDatabase().savePlayerPriceModifiers(var1, var2);
         } catch (SQLException var9) {
            SendMessage.warnMessage("Failed to save player command modifiers to database for " + Bukkit.getOfflinePlayer(var1) + ":" + var9.getMessage());
         }
      }
   }

   public void setCommandSectionModifier(UUID var1, String var2, PriceModifierType var3, double var4) {
      var4 = (int)var4;
      PlayerPriceModifier var6 = this.COMMAND_MODIFIERS.getOrDefault(var1, new PlayerPriceModifier());
      var6.setSectionModifier(var2, var3, var4);

      for (ShopItem var8 : this.plugin.getSection(var2).getShopItems()) {
         if (var3 == PriceModifierType.BOTH || var3 == PriceModifierType.BUY) {
            ((CommandModifier)var8.getBuyModifiers().putIfAbsent(ModifierType.COMMAND, new CommandModifier())).setModifier(var1, var4);
         }

         if (var3 == PriceModifierType.BOTH || var3 == PriceModifierType.SELL) {
            ((CommandModifier)var8.getSellModifiers().putIfAbsent(ModifierType.COMMAND, new CommandModifier())).setModifier(var1, var4);
         }
      }

      this.COMMAND_MODIFIERS.put(var1, var6);
   }

   public boolean removeCommandSectionModifier(UUID var1, String var2, PriceModifierType var3) {
      PlayerPriceModifier var4 = this.COMMAND_MODIFIERS.get(var1);
      if (var3 == PriceModifierType.BOTH) {
         var4.removeSectionModifier(var2);
      } else {
         var4.removeSectionModifier(var2, var3);
      }

      boolean var5 = false;

      for (ShopItem var7 : this.plugin.getSection(var2).getShopItems()) {
         if (var3 == PriceModifierType.BOTH || var3 == PriceModifierType.BUY) {
            CommandModifier var8 = (CommandModifier)var7.getBuyModifiers().getByType(ModifierType.COMMAND);
            if (var8 != null) {
               var5 = var8.removeModifier(var1);
            }
         }

         if (var3 == PriceModifierType.BOTH || var3 == PriceModifierType.SELL) {
            CommandModifier var9 = (CommandModifier)var7.getSellModifiers().getByType(ModifierType.COMMAND);
            if (var9 != null) {
               var5 = var9.removeModifier(var1);
            }
         }
      }

      if (var4.getSectionModifiers().isEmpty() && var4.getItemModifiers().isEmpty()) {
         this.COMMAND_MODIFIERS.remove(var1);
      }

      return var5;
   }

   public void setCommandItemModifier(UUID var1, String var2, PriceModifierType var3, double var4) {
      var4 = (int)var4;
      PlayerPriceModifier var6 = this.COMMAND_MODIFIERS.getOrDefault(var1, new PlayerPriceModifier());
      var6.setItemModifier(var2, var3, var4);
      ShopItem var7 = this.plugin.getShopItem(var2);
      if (var3 == PriceModifierType.BOTH || var3 == PriceModifierType.BUY) {
         ((CommandModifier)var7.getBuyModifiers().putIfAbsent(ModifierType.COMMAND, new CommandModifier())).setModifier(var1, var4);
      }

      if (var3 == PriceModifierType.BOTH || var3 == PriceModifierType.SELL) {
         ((CommandModifier)var7.getSellModifiers().putIfAbsent(ModifierType.COMMAND, new CommandModifier())).setModifier(var1, var4);
      }

      this.COMMAND_MODIFIERS.put(var1, var6);
   }

   public boolean removeCommandItemModifier(UUID var1, String var2, PriceModifierType var3) {
      PlayerPriceModifier var4 = this.COMMAND_MODIFIERS.get(var1);
      if (var3 == PriceModifierType.BOTH) {
         var4.removeItemModifier(var2);
      } else {
         var4.removeItemModifier(var2, var3);
      }

      ShopItem var5 = this.plugin.getShopItem(var2);
      boolean var6 = false;
      if (var3 == PriceModifierType.BOTH || var3 == PriceModifierType.BUY) {
         CommandModifier var7 = (CommandModifier)var5.getBuyModifiers().getByType(ModifierType.COMMAND);
         if (var7 != null) {
            var6 = var7.removeModifier(var1);
         }
      }

      if (var3 == PriceModifierType.BOTH || var3 == PriceModifierType.SELL) {
         CommandModifier var8 = (CommandModifier)var5.getSellModifiers().getByType(ModifierType.COMMAND);
         if (var8 != null) {
            var6 = var8.removeModifier(var1);
         }
      }

      if (var4.getSectionModifiers().isEmpty() && var4.getItemModifiers().isEmpty()) {
         this.COMMAND_MODIFIERS.remove(var1);
      }

      return var6;
   }

   public PlayerPriceModifier getCommandModifiersForPlayer(UUID var1) {
      return this.COMMAND_MODIFIERS.get(var1);
   }

   public PlayerPriceModifier loadCommandModifiersForPlayer(UUID var1) {
      return this.COMMAND_MODIFIERS.computeIfAbsent(var1, this::loadCommandModifiersFromDB);
   }
}
