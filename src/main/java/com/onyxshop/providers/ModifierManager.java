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

   public ModifierManager(OnyxShop plugin) {
      this.plugin = plugin;
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
         } catch (ModifierLoadException exception) {
            SendMessage.errorMessage("Failed to load SeasonModifiers from config for reason: " + exception.getMessage());
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

   private PlayerPriceModifier loadCommandModifiersFromDB(UUID playerId) {
      if (this.plugin.getProviderManager().getDatabase() != null) {
         try {
            return this.plugin.getProviderManager().getDatabase().loadPlayerPriceModifiers(playerId);
         } catch (SQLException exception) {
            SendMessage.warnMessage("Failed to load command modifiers for player because database error occurred: " + exception.getMessage());
            return null;
         }
      } else {
         SendMessage.warnMessage("Failed to load command modifiers for player because database connection isn't loaded");
         return null;
      }
   }

   private void reloadForOnlinePlayersAsync() {
      Collection<? extends Player> onlinePlayers = this.plugin.getServer().getOnlinePlayers();
      SendMessage.logDebugMessage("Loading item price modifiers for " + onlinePlayers.size() + " online player(s)");
      this.plugin.runTaskAsync(() -> {
         for (Player player : onlinePlayers) {
            this.loadCommandModifiersForPlayer(player);
         }
      });
   }

   public void stop() {
      if (!this.COMMAND_MODIFIERS.isEmpty()) {
         SendMessage.logDebugMessage(Lang.SAVING_PRICE_MODIFIERS.get().replace("%count%", String.valueOf(this.COMMAND_MODIFIERS.size())));

         try {
            for (Entry entry : this.COMMAND_MODIFIERS.entrySet()) {
               this.plugin.getProviderManager().getDatabase().savePlayerPriceModifiers((UUID)entry.getKey(), (PlayerPriceModifier)entry.getValue());
            }
         } catch (SQLException exception) {
            SendMessage.errorMessage(Lang.ERROR_SAVING_PRICE_MODIFIERS.get().replace("%message%", exception.getMessage()));
            exception.printStackTrace();
         }
      }
   }

   public void loadCommandModifiersForPlayer(Player player) {
      PlayerPriceModifier playerPriceModifier = this.COMMAND_MODIFIERS.getOrDefault(player.getUniqueId(), this.loadCommandModifiersFromDB(player.getUniqueId()));
      if (playerPriceModifier != null) {
         for (String command : playerPriceModifier.getSectionModifiers().keySet()) {
            PriceModifierData priceModifierData = playerPriceModifier.getSectionModifiers().get(command);

            try {
               if (!this.plugin.getShopSections().contains(command)) {
                  throw new ModifierLoadException("Shop " + command + " doesn't exist");
               }

               Double price = priceModifierData.getOr(PriceModifierType.BOTH, -1.0) > priceModifierData.getOr(PriceModifierType.BUY, -1.0)
                  ? priceModifierData.get(PriceModifierType.BOTH)
                  : priceModifierData.get(PriceModifierType.BUY);
               Double amount = priceModifierData.getOr(PriceModifierType.BOTH, -1.0) > priceModifierData.getOr(PriceModifierType.SELL, -1.0)
                  ? priceModifierData.get(PriceModifierType.BOTH)
                  : priceModifierData.get(PriceModifierType.SELL);

               for (ShopItem shopItem : this.plugin.getSection(command).getShopItems()) {
                  if (price != null && price >= 0.0) {
                     ((CommandModifier)shopItem.getBuyModifiers().putIfAbsent(ModifierType.COMMAND, new CommandModifier()))
                        .setBestModifier(player.getUniqueId(), price);
                  }

                  if (amount != null && amount >= 0.0) {
                     ((CommandModifier)shopItem.getSellModifiers().putIfAbsent(ModifierType.COMMAND, new CommandModifier()))
                        .setBestModifier(player.getUniqueId(), amount);
                  }
               }
            } catch (ModifierLoadException exception) {
               SendMessage.warnMessage(
                  "Failed to load command modifier for player '" + player.getName() + "' for shop section '" + command + "': " + exception.getMessage()
               );
            }
         }

         for (String itemId : playerPriceModifier.getItemModifiers().keySet()) {
            PriceModifierData priceModifierData = playerPriceModifier.getItemModifiers().get(itemId);

            try {
               ShopItem shopItem = this.plugin.getShopItem(itemId);
               if (shopItem == null) {
                  throw new ModifierLoadException("Item " + itemId + " doesn't exist");
               }

               Double amount = priceModifierData.getOr(PriceModifierType.BOTH, -1.0) > priceModifierData.getOr(PriceModifierType.BUY, -1.0)
                  ? priceModifierData.get(PriceModifierType.BOTH)
                  : priceModifierData.get(PriceModifierType.BUY);
               Double amount2 = priceModifierData.getOr(PriceModifierType.BOTH, -1.0) > priceModifierData.getOr(PriceModifierType.SELL, -1.0)
                  ? priceModifierData.get(PriceModifierType.BOTH)
                  : priceModifierData.get(PriceModifierType.SELL);
               if (amount != null && amount >= 0.0) {
                  ((CommandModifier)shopItem.getBuyModifiers().putIfAbsent(ModifierType.COMMAND, new CommandModifier()))
                     .setBestModifier(player.getUniqueId(), amount);
               }

               if (amount2 != null && amount2 >= 0.0) {
                  ((CommandModifier)shopItem.getSellModifiers().putIfAbsent(ModifierType.COMMAND, new CommandModifier()))
                     .setBestModifier(player.getUniqueId(), amount2);
               }
            } catch (ModifierLoadException exception) {
               SendMessage.warnMessage("Failed to load command modifier for player '" + player.getName() + "' for item '" + itemId + "': " + exception.getMessage());
            }
         }

         this.COMMAND_MODIFIERS.put(player.getUniqueId(), playerPriceModifier);
      }
   }

   public void unloadAndSaveCommandModifiers(UUID uniqueId) {
      PlayerPriceModifier playerPriceModifier = this.COMMAND_MODIFIERS.get(uniqueId);
      if (playerPriceModifier != null) {
         for (String command : playerPriceModifier.getSectionModifiers().keySet()) {
            if (this.plugin.getShopSections().contains(command)) {
               for (ShopItem shopItem : this.plugin.getSection(command).getShopItems()) {
                  CommandModifier commandModifier = (CommandModifier)shopItem.getBuyModifiers().getByType(ModifierType.COMMAND);
                  if (commandModifier != null) {
                     commandModifier.removeModifier(uniqueId);
                  }

                  CommandModifier commandModifier2 = (CommandModifier)shopItem.getSellModifiers().getByType(ModifierType.COMMAND);
                  if (commandModifier2 != null) {
                     commandModifier2.removeModifier(uniqueId);
                  }
               }
            }
         }

         for (String itemId : playerPriceModifier.getItemModifiers().keySet()) {
            ShopItem shopItem = this.plugin.getShopItem(itemId);
            if (shopItem != null) {
               CommandModifier commandModifier = (CommandModifier)shopItem.getBuyModifiers().getByType(ModifierType.COMMAND);
               if (commandModifier != null) {
                  commandModifier.removeModifier(uniqueId);
               }

               CommandModifier commandModifier2 = (CommandModifier)shopItem.getSellModifiers().getByType(ModifierType.COMMAND);
               if (commandModifier2 != null) {
                  commandModifier2.removeModifier(uniqueId);
               }
            }
         }

         this.COMMAND_MODIFIERS.remove(uniqueId);

         try {
            this.plugin.getProviderManager().getDatabase().savePlayerPriceModifiers(uniqueId, playerPriceModifier);
         } catch (SQLException exception) {
            SendMessage.warnMessage("Failed to save player command modifiers to database for " + Bukkit.getOfflinePlayer(uniqueId) + ":" + exception.getMessage());
         }
      }
   }

   public void setCommandSectionModifier(UUID uniqueId, String command, PriceModifierType priceModifierType, double price) {
      price = (int)price;
      PlayerPriceModifier playerPriceModifier = this.COMMAND_MODIFIERS.getOrDefault(uniqueId, new PlayerPriceModifier());
      playerPriceModifier.setSectionModifier(command, priceModifierType, price);

      for (ShopItem shopItem : this.plugin.getSection(command).getShopItems()) {
         if (priceModifierType == PriceModifierType.BOTH || priceModifierType == PriceModifierType.BUY) {
            ((CommandModifier)shopItem.getBuyModifiers().putIfAbsent(ModifierType.COMMAND, new CommandModifier())).setModifier(uniqueId, price);
         }

         if (priceModifierType == PriceModifierType.BOTH || priceModifierType == PriceModifierType.SELL) {
            ((CommandModifier)shopItem.getSellModifiers().putIfAbsent(ModifierType.COMMAND, new CommandModifier())).setModifier(uniqueId, price);
         }
      }

      this.COMMAND_MODIFIERS.put(uniqueId, playerPriceModifier);
   }

   public boolean removeCommandSectionModifier(UUID uniqueId, String command, PriceModifierType priceModifierType) {
      PlayerPriceModifier playerPriceModifier = this.COMMAND_MODIFIERS.get(uniqueId);
      if (priceModifierType == PriceModifierType.BOTH) {
         playerPriceModifier.removeSectionModifier(command);
      } else {
         playerPriceModifier.removeSectionModifier(command, priceModifierType);
      }

      boolean flag = false;

      for (ShopItem shopItem : this.plugin.getSection(command).getShopItems()) {
         if (priceModifierType == PriceModifierType.BOTH || priceModifierType == PriceModifierType.BUY) {
            CommandModifier commandModifier = (CommandModifier)shopItem.getBuyModifiers().getByType(ModifierType.COMMAND);
            if (commandModifier != null) {
               flag = commandModifier.removeModifier(uniqueId);
            }
         }

         if (priceModifierType == PriceModifierType.BOTH || priceModifierType == PriceModifierType.SELL) {
            CommandModifier commandModifier = (CommandModifier)shopItem.getSellModifiers().getByType(ModifierType.COMMAND);
            if (commandModifier != null) {
               flag = commandModifier.removeModifier(uniqueId);
            }
         }
      }

      if (playerPriceModifier.getSectionModifiers().isEmpty() && playerPriceModifier.getItemModifiers().isEmpty()) {
         this.COMMAND_MODIFIERS.remove(uniqueId);
      }

      return flag;
   }

   public void setCommandItemModifier(UUID uniqueId, String command, PriceModifierType priceModifierType, double price) {
      price = (int)price;
      PlayerPriceModifier playerPriceModifier = this.COMMAND_MODIFIERS.getOrDefault(uniqueId, new PlayerPriceModifier());
      playerPriceModifier.setItemModifier(command, priceModifierType, price);
      ShopItem shopItem = this.plugin.getShopItem(command);
      if (priceModifierType == PriceModifierType.BOTH || priceModifierType == PriceModifierType.BUY) {
         ((CommandModifier)shopItem.getBuyModifiers().putIfAbsent(ModifierType.COMMAND, new CommandModifier())).setModifier(uniqueId, price);
      }

      if (priceModifierType == PriceModifierType.BOTH || priceModifierType == PriceModifierType.SELL) {
         ((CommandModifier)shopItem.getSellModifiers().putIfAbsent(ModifierType.COMMAND, new CommandModifier())).setModifier(uniqueId, price);
      }

      this.COMMAND_MODIFIERS.put(uniqueId, playerPriceModifier);
   }

   public boolean removeCommandItemModifier(UUID uniqueId, String command, PriceModifierType priceModifierType) {
      PlayerPriceModifier playerPriceModifier = this.COMMAND_MODIFIERS.get(uniqueId);
      if (priceModifierType == PriceModifierType.BOTH) {
         playerPriceModifier.removeItemModifier(command);
      } else {
         playerPriceModifier.removeItemModifier(command, priceModifierType);
      }

      ShopItem shopItem = this.plugin.getShopItem(command);
      boolean flag = false;
      if (priceModifierType == PriceModifierType.BOTH || priceModifierType == PriceModifierType.BUY) {
         CommandModifier commandModifier = (CommandModifier)shopItem.getBuyModifiers().getByType(ModifierType.COMMAND);
         if (commandModifier != null) {
            flag = commandModifier.removeModifier(uniqueId);
         }
      }

      if (priceModifierType == PriceModifierType.BOTH || priceModifierType == PriceModifierType.SELL) {
         CommandModifier commandModifier = (CommandModifier)shopItem.getSellModifiers().getByType(ModifierType.COMMAND);
         if (commandModifier != null) {
            flag = commandModifier.removeModifier(uniqueId);
         }
      }

      if (playerPriceModifier.getSectionModifiers().isEmpty() && playerPriceModifier.getItemModifiers().isEmpty()) {
         this.COMMAND_MODIFIERS.remove(uniqueId);
      }

      return flag;
   }

   public PlayerPriceModifier getCommandModifiersForPlayer(UUID playerId) {
      return this.COMMAND_MODIFIERS.get(playerId);
   }

   public PlayerPriceModifier loadCommandModifiersForPlayer(UUID playerId) {
      return this.COMMAND_MODIFIERS.computeIfAbsent(playerId, this::loadCommandModifiersFromDB);
   }
}
