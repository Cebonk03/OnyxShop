package com.onyxshop.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.api.events.EconomyPreLoadEvent;
import com.onyxshop.events.api.ProviderLoadEvent;
import com.onyxshop.events.delayed.DelayedLoadListener;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.methodes.UpdateChecker;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.providers.EconomyProvider;
import com.onyxshop.providers.ItemManager;
import com.onyxshop.providers.economys.CoinsEngineEconomyV5;
import com.onyxshop.providers.economys.EcoBitsEconomy;
import com.onyxshop.providers.economys.ExpEconomy;
import com.onyxshop.providers.economys.ExternalEconomyProvider;
import com.onyxshop.providers.economys.GemsEconomy;
import com.onyxshop.providers.economys.ItemEconomy;
import com.onyxshop.providers.economys.LevelsEconomy;
import com.onyxshop.providers.economys.PlayerPointsEconomy;
import com.onyxshop.providers.economys.UltraEconomy;
import com.onyxshop.providers.economys.VaultEconomy;
import com.onyxshop.providers.economys.VaultUnlockedEconomy;
import com.onyxshop.providers.economys.VotingPluginEconomy;
import com.onyxshop.providers.economys.ZEssentialsEconomy;
import com.onyxshop.providers.economys.formatter.PriceFormatter;
import com.onyxshop.util.exceptions.EconomyLoadException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class EconomyHandler {
   private final OnyxShop plugin;
   private EconomyProvider defaultProvider;
   private Map<String, EconomyProvider> shopPaymentTypes = new HashMap<>();
   private Map<EcoType, EconomyProvider> registeredPaymentOptions = new HashMap<>();
   private final EconomyPreLoadEvent preLoadEvent;
   private static PriceFormatter.Format defaultFormatter;

   public EconomyHandler(OnyxShop plugin) {
      this.plugin = plugin;
      this.preLoadEvent = new EconomyPreLoadEvent(plugin);
   }

   public boolean doWait() {
      boolean flag = false;

      for (EconomyProvider economy : this.registeredPaymentOptions.values()) {
         if (economy.doWait()) {
            flag = true;
            if (economy.getType().getType() == EconomyType.ITEM) {
               ItemManager.registerDelayed((ItemEconomy)economy);
            }
         }
      }

      return flag;
   }

   public Set<EcoType> getRegistered() {
      return this.registeredPaymentOptions.keySet();
   }

   public static PriceFormatter.Format getDefaultFormatter() {
      return defaultFormatter;
   }

   public boolean registerPaymentSolutions() {
      defaultFormatter = PriceFormatter.getFormatter();
      Bukkit.getPluginManager().callEvent(this.preLoadEvent);
      if (this.loadDefaultEconomy() == null) {
         return false;
      } else {
         int value = 0;

         for (String shopId : this.plugin.getShopSections()) {
            EcoType ecoType = this.getEconomyType(shopId);
            if (ecoType != null) {
               EconomyProvider economy = this.registeredPaymentOptions.get(ecoType);
               if (economy == null) {
                  if (ecoType.getType() == EconomyType.EXTERNAL) {
                     SendMessage.warnMessage("Tried to register unknown external economy provider for 'EXTERNAL:" + ecoType.getCurrency() + "' for shop " + shopId);
                     SendMessage.warnMessage(
                        "Please make sure the spelling of the name of the external provider matches and that it is registered during the EconomyPreLoadEvent"
                     );
                     SendMessage.warnMessage(
                        "Currently registered EXTERNAL economy providers: "
                           + this.registeredPaymentOptions
                              .keySet()
                              .stream()
                              .filter(element -> element.getType() == EconomyType.EXTERNAL)
                              .map(element -> element.getType().name() + ":" + element.getCurrency())
                              .collect(Collectors.joining(", "))
                     );
                     continue;
                  }

                  try {
                     economy = this.initProvider(ecoType, shopId + ".economy", 0);
                     this.shopPaymentTypes.put(shopId, economy);
                     if (economy.doWait()) {
                        SendMessage.logDebugMessage(
                           "Loaded economy '%economy%' for shop '%shop%', waiting to register it..."
                              .replace("%economy%", ecoType.toString())
                              .replace("%shop%", shopId)
                        );
                     } else {
                        SendMessage.logDebugMessage(Lang.SECTION_ECONOMY_PROVIDER_LOADED.get().replace("%economy%", ecoType.toString()).replace("%shop%", shopId));
                     }
                  } catch (EconomyLoadException exception) {
                     SendMessage.errorMessage(Lang.CANNOT_LOAD_ECONOMY_PROVIDER.get().replace("%shop%", shopId).replace("%reason%", exception.getMessage()));
                  }
               } else {
                  this.shopPaymentTypes.put(shopId, economy);
                  SendMessage.logDebugMessage(Lang.SECTION_ECONOMY_PROVIDER_LOADED.get().replace("%economy%", ecoType.toString()).replace("%shop%", shopId));
               }
            }

            value++;
         }

         SendMessage.infoMessage(
            Lang.ECONOMY_PROVIDERS_LOADED
               .get()
               .replace("%total%", String.valueOf(this.registeredPaymentOptions.size()))
               .replace("%shops%", String.valueOf(value))
         );
         return true;
      }
   }

   private EcoType getEconomyType(String type) {
      String text = ConfigManager.getSection(type).getString("economy");
      if (text != null && !text.isEmpty()) {
         EcoType ecoType = EconomyType.getFromString(text);
         if (ecoType == null) {
            SendMessage.warnMessage(Lang.CANNOT_FIND_ECONOMY_PROVIDER.get().replace("%shop%", type).replace("%type%", text));
            return null;
         } else {
            return ecoType;
         }
      } else {
         return null;
      }
   }

   public EcoType getEcoType(String type, String text, int value) {
      if (text == null) {
         return null;
      } else {
         EcoType ecoType = EconomyType.getFromString(text);
         if (ecoType == null) {
            SendMessage.errorMessage(Lang.CANNOT_FIND_ITEM_ECONOMY_PROVIDER.get().replace("%path%", type).replace("%type%", text));
            return null;
         } else if (!this.registeredPaymentOptions.containsKey(ecoType)) {
            try {
               return this.initProvider(ecoType, type, value).getType();
            } catch (EconomyLoadException exception) {
               SendMessage.errorMessage(
                  Lang.CANNOT_LOAD_ITEM_ECONOMY_PROVIDER.get().replace("%path%", type).replace("%type%", text).replace("%reason%", exception.getMessage())
               );
               return null;
            }
         } else {
            return ecoType;
         }
      }
   }

   private EconomyProvider loadDefaultEconomy() {
      EcoType ecoType = EconomyType.getFromString(ConfigManager.getConfig().getString("economy-provider"));
      if (ecoType == null) {
         SendMessage.warnMessage(Lang.CANNOT_FIND_DEFAULT_ECONOMY_PROVIDER.get().replace("%economy%", ConfigManager.getConfig().getString("economy-provider")));
         return (this.defaultProvider = this.getAvailableEcon()) != null ? this.defaultProvider : null;
      } else {
         try {
            this.defaultProvider = this.initProvider(ecoType, "economy-provider", 0);
         } catch (EconomyLoadException exception) {
            SendMessage.warnMessage(Lang.CANNOT_LOAD_DEFAULT_ECONOMY_PROVIDER.get().replace("%reason%", exception.getMessage()));
            return (this.defaultProvider = this.getAvailableEcon()) != null ? this.defaultProvider : null;
         }

         return this.defaultProvider;
      }
   }

   private EconomyProvider initProvider(EcoType ecoType, String type, int value) throws EconomyLoadException {
      EconomyProvider economy = this.getProvider(ecoType, type, value);
      if (economy == null) {
         return null;
      } else {
         economy.setup(this.plugin);
         this.registeredPaymentOptions.put(ecoType, economy);
         return economy;
      }
   }

   private EconomyProvider getProvider(EcoType ecoType, String type, int value) throws EconomyLoadException {
      if (ecoType == null) {
         return null;
      } else {
         switch (ecoType.getType()) {
            case PLAYER_POINTS:
               return new PlayerPointsEconomy();
            case VAULT:
               return new VaultEconomy();
            case VAULTUNLOCKED:
               return new VaultUnlockedEconomy(ecoType.getCurrency());
            case EXCELLENT_ECONOMY:
               return this.getCoinsEngineInstance(ecoType);
            case GEMS_ECONOMY:
               return ecoType.getCurrency() != null ? new GemsEconomy(ecoType.getCurrency()) : new GemsEconomy(null);
            case ULTRA_ECONOMY:
               return ecoType.getCurrency() != null ? new UltraEconomy(ecoType.getCurrency()) : new UltraEconomy(null);
            case ECOBITS:
               return ecoType.getCurrency() != null ? new EcoBitsEconomy(ecoType.getCurrency()) : new EcoBitsEconomy(null);
            case VOTING_PLUGIN:
               return new VotingPluginEconomy();
            case ITEM:
               return new ItemEconomy(ecoType.getCurrency(), type, value);
            case LEVELS:
               return new LevelsEconomy();
            case EXP:
               return new ExpEconomy();
            case ZESSENTIALS:
               return new ZEssentialsEconomy(ecoType.getCurrency());
            case EXTERNAL:
               EconomyProvider economy = this.registeredPaymentOptions.get(ecoType);
               if (economy == null) {
                  SendMessage.warnMessage("Tried to register unknown external economy provider for 'EXTERNAL:" + ecoType.getCurrency() + "' at " + type);
                  SendMessage.warnMessage(
                     "Please make sure the spelling of the name of the external provider matches and that it is registered during the EconomyPreLoadEvent"
                  );
                  SendMessage.warnMessage(
                     "Currently registered EXTERNAL economy providers: "
                        + this.registeredPaymentOptions
                           .keySet()
                           .stream()
                           .filter(element -> element.getType() == EconomyType.EXTERNAL)
                           .map(element -> element.getType().name() + ":" + element.getCurrency())
                           .collect(Collectors.joining(", "))
                  );
               }

               return economy;
            default:
               return null;
         }
      }
   }

   private EconomyProvider getCoinsEngineInstance(EcoType ecoType) throws EconomyLoadException {
      if (Bukkit.getPluginManager().getPlugin("CoinsEngine") == null && Bukkit.getPluginManager().getPlugin("ExcellentEconomy") == null) {
         throw new EconomyLoadException("CoinsEngine is not installed");
      } else {
         String text = this.plugin.getServer().getPluginManager().getPlugin("CoinsEngine") != null
            ? "CoinsEngine"
            : (this.plugin.getServer().getPluginManager().getPlugin("ExcellentEconomy") != null ? "ExcellentEconomy" : null);
         UpdateChecker.Version version = new UpdateChecker.Version(Bukkit.getPluginManager().getPlugin(text).getDescription().getVersion());
         if (version.isGreater(new UpdateChecker.Version("2.6.1"))) {
            return ecoType.getCurrency() != null ? new CoinsEngineEconomyV5(ecoType.getCurrency()) : new CoinsEngineEconomyV5(null);
         } else {
            String text2 = "CoinsEngineEconomyV4";
            if (version.isGreater(new UpdateChecker.Version("2.5.3"))) {
               text2 = "CoinsEngineEconomyV4";
            } else if (version.isGreater(new UpdateChecker.Version("2.3.5"))) {
               text2 = "CoinsEngineEconomyV3";
            } else if (version.isGreater(new UpdateChecker.Version("2.1.9"))) {
               text2 = "CoinsEngineEconomyV2";
            } else {
               text2 = "CoinsEngineEconomyV1";
            }

            try {
               Class type = Class.forName("com.onyxshop.providers.economys." + text2);
               Constructor constructor = type.getConstructor(String.class);
               return ecoType.getCurrency() != null ? (EconomyProvider)constructor.newInstance(ecoType.getCurrency()) : (EconomyProvider)constructor.newInstance((String)null);
            } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException | ClassNotFoundException exception) {
               SendMessage.errorMessage("Failed to initialize " + text2 + "\nDetected CoinsEngine version: " + version.toString());
               exception.printStackTrace();
               return null;
            }
         }
      }
   }

   private EcoType getType() {
      this.deleteUnknown(
         this.registeredPaymentOptions
            .keySet()
            .stream()
            .filter(element -> element.getType() == EconomyType.LEVELS || element.getType() == EconomyType.ITEM)
            .collect(Collectors.toList())
      );
      return null;
   }

   private EconomyProvider getAvailableEcon() {
      Object value = null;
      if (this.plugin.getServer().getPluginManager().isPluginEnabled("Vault")) {
         SendMessage.infoMessage("Detected Vault, initializing...");
         value = new VaultEconomy();
      } else if (this.plugin.getServer().getPluginManager().isPluginEnabled("UltraEconomy")) {
         SendMessage.infoMessage("Detected UltraEconomy, initializing...");
         value = new UltraEconomy(null);
      } else if (this.plugin.getServer().getPluginManager().isPluginEnabled("CoinsEngine")
         || this.plugin.getServer().getPluginManager().isPluginEnabled("ExcellentEconomy")) {
         SendMessage.infoMessage("Detected ExcellentEconomy, initializing...");

         try {
            value = this.getCoinsEngineInstance(new EcoType(EconomyType.EXCELLENT_ECONOMY));
         } catch (EconomyLoadException exception) {
         }
      } else if (this.plugin.getServer().getPluginManager().isPluginEnabled("EcoBits")) {
         SendMessage.infoMessage("Detected EcoBits, initializing...");
         value = new EcoBitsEconomy(null);
      } else if (this.plugin.getServer().getPluginManager().isPluginEnabled("GemsEconomy")) {
         SendMessage.infoMessage("Detected GemsEconomy, initializing...");
         value = new GemsEconomy(null);
      } else if (this.plugin.getServer().getPluginManager().isPluginEnabled("PlayerPoints")) {
         SendMessage.infoMessage("Detected PlayerPoints, initializing...");
         value = new PlayerPointsEconomy();
      } else if (this.plugin.getServer().getPluginManager().isPluginEnabled("VotingPlugin")) {
         SendMessage.infoMessage("Detected VotingPlugin, initializing...");
         value = new VotingPluginEconomy();
      } else if (this.plugin.getServer().getPluginManager().isPluginEnabled("zEssentials")) {
         SendMessage.infoMessage("Detected zEssentials, initializing...");
         value = new ZEssentialsEconomy(null);
      } else {
         SendMessage.infoMessage("Failed to find economy plugin, using exp points as default economy, initializing...");
         value = new ExpEconomy();
      }

      if (value != null) {
         try {
            ((EconomyProvider)value).setup(this.plugin);
            this.registeredPaymentOptions.put(((EconomyProvider)value).getType(), (EconomyProvider)value);
            return (EconomyProvider)value;
         } catch (EconomyLoadException exception) {
         }
      }

      SendMessage.errorMessage("===================================================");
      SendMessage.errorMessage(" ");
      SendMessage.errorMessage("Failed to automatically load an available economy provider, this plugin requires one, disabling...");
      SendMessage.errorMessage(
         "See the list of available economy providers in the wiki."
      );
      SendMessage.errorMessage(" ");
      SendMessage.errorMessage("===================================================");
      return null;
   }

   public boolean relead() {
      if (this.shopPaymentTypes.size() > 0) {
         this.shopPaymentTypes.clear();
      }

      if (this.registeredPaymentOptions.size() > 0) {
         this.registeredPaymentOptions.clear();
      }

      this.defaultProvider = null;
      return this.registerPaymentSolutions();
   }

   public void registerExternalProvider(ExternalEconomyProvider externalEconomyProvider) {
      try {
         externalEconomyProvider.setup(this.plugin);
         this.registeredPaymentOptions.put(externalEconomyProvider.getType(), externalEconomyProvider);
      } catch (EconomyLoadException exception) {
         throw new RuntimeException(exception);
      }
   }

   public void translatePlaceholdersDelayed() {
      this.registeredPaymentOptions.forEach((key, value) -> value.formatPlaceholders());
   }

   private void deleteUnknown(List<EcoType> values) {
      values.forEach(entry -> this.registeredPaymentOptions.remove(entry));
   }

   public EconomyProvider getEcon(String text) {
      return this.shopPaymentTypes.containsKey(text) ? this.shopPaymentTypes.get(text) : this.defaultProvider;
   }

   public EcoType getEcon() {
      return this.getType();
   }

   public EconomyProvider getEconomy(ShopItem shopItem) {
      String text = ConfigManager.getShop(shopItem.section).getString((shopItem.rotates() ? "" : "pages.") + shopItem.itemLoc + ".economy");
      if (text == null) {
         return this.getEcon(shopItem.section);
      } else {
         EcoType ecoType = EconomyType.getFromString(text);
         if (ecoType == null) {
            SendMessage.warnMessage("Could not find a economy provider for item '" + shopItem.getItemPath() + "' such as '" + text + "', using section default...");
            return this.getEcon(shopItem.section);
         } else if (this.registeredPaymentOptions.containsKey(ecoType)) {
            return this.registeredPaymentOptions.get(ecoType);
         } else {
            try {
               return this.initProvider(ecoType, shopItem.section + "." + (shopItem.rotates() ? "" : "pages.") + shopItem.itemLoc + ".economy", 0);
            } catch (EconomyLoadException exception) {
               SendMessage.errorMessage(
                  "Failed to load economy provider for item '" + shopItem.getItemPath() + "' with reason '" + exception.getMessage() + "', using section default..."
               );
               return this.getEcon(shopItem.section);
            }
         }
      }
   }

   public void mayContinue() {
      for (EconomyProvider economy : this.registeredPaymentOptions.values()) {
         if (economy.doWait()) {
            return;
         }

         try {
            economy.isDecimal();
         } catch (NullPointerException exception) {
            EconomyHandler.ProviderType providerType = this.getType(economy.getType());
            if (providerType == EconomyHandler.ProviderType.DEFAULT) {
               SendMessage.errorMessage("===================================================");
               SendMessage.errorMessage(" ");
               SendMessage.errorMessage(
                  "Failed to register default economy provider(" + economy.getType().toString() + ") because it has not been enabled yet, disabling..."
               );
               SendMessage.errorMessage(" ");
               SendMessage.errorMessage("===================================================");
               this.plugin.getServer().getPluginManager().disablePlugin(this.plugin);
               return;
            }

            if (providerType == EconomyHandler.ProviderType.SHOP) {
               for (String text : providerType.s.split("::")) {
                  SendMessage.errorMessage(
                     Lang.CANNOT_LOAD_ECONOMY_PROVIDER.get().replace("%shop%", text).replace("%reason%", "Economy provider did not finish loading in time")
                  );
                  this.shopPaymentTypes.remove(text);
               }
            }
         }
      }

      Bukkit.getPluginManager().callEvent(new ProviderLoadEvent(DelayedLoadListener.Type.ECONOMY));
   }

   private EconomyHandler.ProviderType getType(EcoType ecoType) {
      if (this.defaultProvider.getType().equals(ecoType)) {
         return EconomyHandler.ProviderType.DEFAULT;
      } else {
         EconomyHandler.ProviderType providerType = EconomyHandler.ProviderType.SHOP;
         StringBuilder builder = new StringBuilder(providerType.s);

         for (Entry entry : this.shopPaymentTypes.entrySet().stream().filter(entry2 -> entry2.getValue().getType().equals(ecoType)).collect(Collectors.toList())) {
            builder.append((String)entry.getKey()).append("::");
         }

         providerType.s = builder.toString();
         return !providerType.s.isEmpty() ? providerType : null;
      }
   }

   public EconomyProvider getEcon(ShopItem shopItem) {
      return this.registeredPaymentOptions.get(shopItem.getEcoType());
   }

   public EconomyProvider getEcon(EcoType ecoType) {
      return ecoType == null ? this.defaultProvider : this.registeredPaymentOptions.get(ecoType);
   }

   public EconomyProvider getDefaultProvider() {
      return this.defaultProvider;
   }

   public EconomyProvider getLoaded(String text) {
      EcoType ecoType = EconomyType.getFromString(text);
      return this.registeredPaymentOptions.get(ecoType);
   }

   public Map<EcoType, Double> has(Map<EcoType, Double> map, Player player) {
      return map.entrySet()
         .stream()
         .filter(entry -> this.getEcon((EcoType)entry.getKey()).getBalance(player) < (Double)entry.getValue())
         .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
   }

   public boolean withdraw(Map<EcoType, Double> map, Player player) {
      HashMap<EcoType, Double> map2 = new HashMap<>();

      for (Entry<EcoType, Double> entry : map.entrySet()) {
         if (!this.getEcon(entry.getKey()).withdrawBalance(player, entry.getValue())) {
            for (Entry<EcoType, Double> entry2 : map2.entrySet()) {
               this.getEcon(entry2.getKey()).depositBalance(player, entry2.getValue());
            }

            return false;
         }

         map2.put(entry.getKey(), entry.getValue());
      }

      return true;
   }

   public void deposit(Map<EcoType, Double> map, Player player) {
      map.forEach((key, value) -> this.getEcon(key).depositBalance(player, value));
   }

   private static enum ProviderType {
      DEFAULT,
      SHOP,
      ITEM;

      String s = "";

      private ProviderType() {
      }
   }
}
