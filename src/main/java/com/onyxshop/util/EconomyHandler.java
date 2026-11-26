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

   public EconomyHandler(OnyxShop var1) {
      this.plugin = var1;
      this.preLoadEvent = new EconomyPreLoadEvent(var1);
   }

   public boolean doWait() {
      boolean var1 = false;

      for (EconomyProvider var3 : this.registeredPaymentOptions.values()) {
         if (var3.doWait()) {
            var1 = true;
            if (var3.getType().getType() == EconomyType.ITEM) {
               ItemManager.registerDelayed((ItemEconomy)var3);
            }
         }
      }

      return var1;
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
         int var1 = 0;

         for (String var3 : this.plugin.getShopSections()) {
            EcoType var4 = this.getEconomyType(var3);
            if (var4 != null) {
               EconomyProvider var5 = this.registeredPaymentOptions.get(var4);
               if (var5 == null) {
                  if (var4.getType() == EconomyType.EXTERNAL) {
                     SendMessage.warnMessage("Tried to register unknown external economy provider for 'EXTERNAL:" + var4.getCurrency() + "' for shop " + var3);
                     SendMessage.warnMessage(
                        "Please make sure the spelling of the name of the external provider matches and that it is registered during the EconomyPreLoadEvent"
                     );
                     SendMessage.warnMessage(
                        "Currently registered EXTERNAL economy providers: "
                           + this.registeredPaymentOptions
                              .keySet()
                              .stream()
                              .filter(var0 -> var0.getType() == EconomyType.EXTERNAL)
                              .map(var0 -> var0.getType().name() + ":" + var0.getCurrency())
                              .collect(Collectors.joining(", "))
                     );
                     continue;
                  }

                  try {
                     var5 = this.initProvider(var4, var3 + ".economy", 0);
                     this.shopPaymentTypes.put(var3, var5);
                     if (var5.doWait()) {
                        SendMessage.logDebugMessage(
                           "Loaded economy '%economy%' for shop '%shop%', waiting to register it..."
                              .replace("%economy%", var4.toString())
                              .replace("%shop%", var3)
                        );
                     } else {
                        SendMessage.logDebugMessage(Lang.SECTION_ECONOMY_PROVIDER_LOADED.get().replace("%economy%", var4.toString()).replace("%shop%", var3));
                     }
                  } catch (EconomyLoadException var7) {
                     SendMessage.errorMessage(Lang.CANNOT_LOAD_ECONOMY_PROVIDER.get().replace("%shop%", var3).replace("%reason%", var7.getMessage()));
                  }
               } else {
                  this.shopPaymentTypes.put(var3, var5);
                  SendMessage.logDebugMessage(Lang.SECTION_ECONOMY_PROVIDER_LOADED.get().replace("%economy%", var4.toString()).replace("%shop%", var3));
               }
            }

            var1++;
         }

         SendMessage.infoMessage(
            Lang.ECONOMY_PROVIDERS_LOADED
               .get()
               .replace("%total%", String.valueOf(this.registeredPaymentOptions.size()))
               .replace("%shops%", String.valueOf(var1))
         );
         return true;
      }
   }

   private EcoType getEconomyType(String var1) {
      String var2 = ConfigManager.getSection(var1).getString("economy");
      if (var2 != null && !var2.isEmpty()) {
         EcoType var3 = EconomyType.getFromString(var2);
         if (var3 == null) {
            SendMessage.warnMessage(Lang.CANNOT_FIND_ECONOMY_PROVIDER.get().replace("%shop%", var1).replace("%type%", var2));
            return null;
         } else {
            return var3;
         }
      } else {
         return null;
      }
   }

   public EcoType getEcoType(String var1, String var2, int var3) {
      if (var2 == null) {
         return null;
      } else {
         EcoType var4 = EconomyType.getFromString(var2);
         if (var4 == null) {
            SendMessage.errorMessage(Lang.CANNOT_FIND_ITEM_ECONOMY_PROVIDER.get().replace("%path%", var1).replace("%type%", var2));
            return null;
         } else if (!this.registeredPaymentOptions.containsKey(var4)) {
            try {
               return this.initProvider(var4, var1, var3).getType();
            } catch (EconomyLoadException var6) {
               SendMessage.errorMessage(
                  Lang.CANNOT_LOAD_ITEM_ECONOMY_PROVIDER.get().replace("%path%", var1).replace("%type%", var2).replace("%reason%", var6.getMessage())
               );
               return null;
            }
         } else {
            return var4;
         }
      }
   }

   private EconomyProvider loadDefaultEconomy() {
      EcoType var1 = EconomyType.getFromString(ConfigManager.getConfig().getString("economy-provider"));
      if (var1 == null) {
         SendMessage.warnMessage(Lang.CANNOT_FIND_DEFAULT_ECONOMY_PROVIDER.get().replace("%economy%", ConfigManager.getConfig().getString("economy-provider")));
         return (this.defaultProvider = this.getAvailableEcon()) != null ? this.defaultProvider : null;
      } else {
         try {
            this.defaultProvider = this.initProvider(var1, "economy-provider", 0);
         } catch (EconomyLoadException var3) {
            SendMessage.warnMessage(Lang.CANNOT_LOAD_DEFAULT_ECONOMY_PROVIDER.get().replace("%reason%", var3.getMessage()));
            return (this.defaultProvider = this.getAvailableEcon()) != null ? this.defaultProvider : null;
         }

         return this.defaultProvider;
      }
   }

   private EconomyProvider initProvider(EcoType var1, String var2, int var3) throws EconomyLoadException {
      EconomyProvider var4 = this.getProvider(var1, var2, var3);
      if (var4 == null) {
         return null;
      } else {
         var4.setup(this.plugin);
         this.registeredPaymentOptions.put(var1, var4);
         return var4;
      }
   }

   private EconomyProvider getProvider(EcoType var1, String var2, int var3) throws EconomyLoadException {
      if (var1 == null) {
         return null;
      } else {
         switch (var1.getType()) {
            case PLAYER_POINTS:
               return new PlayerPointsEconomy();
            case VAULT:
               return new VaultEconomy();
            case VAULTUNLOCKED:
               return new VaultUnlockedEconomy(var1.getCurrency());
            case EXCELLENT_ECONOMY:
               return this.getCoinsEngineInstance(var1);
            case GEMS_ECONOMY:
               return var1.getCurrency() != null ? new GemsEconomy(var1.getCurrency()) : new GemsEconomy(null);
            case ULTRA_ECONOMY:
               return var1.getCurrency() != null ? new UltraEconomy(var1.getCurrency()) : new UltraEconomy(null);
            case ECOBITS:
               return var1.getCurrency() != null ? new EcoBitsEconomy(var1.getCurrency()) : new EcoBitsEconomy(null);
            case VOTING_PLUGIN:
               return new VotingPluginEconomy();
            case ITEM:
               return new ItemEconomy(var1.getCurrency(), var2, var3);
            case LEVELS:
               return new LevelsEconomy();
            case EXP:
               return new ExpEconomy();
            case ZESSENTIALS:
               return new ZEssentialsEconomy(var1.getCurrency());
            case EXTERNAL:
               EconomyProvider var4 = this.registeredPaymentOptions.get(var1);
               if (var4 == null) {
                  SendMessage.warnMessage("Tried to register unknown external economy provider for 'EXTERNAL:" + var1.getCurrency() + "' at " + var2);
                  SendMessage.warnMessage(
                     "Please make sure the spelling of the name of the external provider matches and that it is registered during the EconomyPreLoadEvent"
                  );
                  SendMessage.warnMessage(
                     "Currently registered EXTERNAL economy providers: "
                        + this.registeredPaymentOptions
                           .keySet()
                           .stream()
                           .filter(var0 -> var0.getType() == EconomyType.EXTERNAL)
                           .map(var0 -> var0.getType().name() + ":" + var0.getCurrency())
                           .collect(Collectors.joining(", "))
                  );
               }

               return var4;
            default:
               return null;
         }
      }
   }

   private EconomyProvider getCoinsEngineInstance(EcoType var1) throws EconomyLoadException {
      if (Bukkit.getPluginManager().getPlugin("CoinsEngine") == null && Bukkit.getPluginManager().getPlugin("ExcellentEconomy") == null) {
         throw new EconomyLoadException("CoinsEngine is not installed");
      } else {
         String var2 = this.plugin.getServer().getPluginManager().getPlugin("CoinsEngine") != null
            ? "CoinsEngine"
            : (this.plugin.getServer().getPluginManager().getPlugin("ExcellentEconomy") != null ? "ExcellentEconomy" : null);
         UpdateChecker.Version var3 = new UpdateChecker.Version(Bukkit.getPluginManager().getPlugin(var2).getDescription().getVersion());
         if (var3.isGreater(new UpdateChecker.Version("2.6.1"))) {
            return var1.getCurrency() != null ? new CoinsEngineEconomyV5(var1.getCurrency()) : new CoinsEngineEconomyV5(null);
         } else {
            String var4 = "CoinsEngineEconomyV4";
            if (var3.isGreater(new UpdateChecker.Version("2.5.3"))) {
               var4 = "CoinsEngineEconomyV4";
            } else if (var3.isGreater(new UpdateChecker.Version("2.3.5"))) {
               var4 = "CoinsEngineEconomyV3";
            } else if (var3.isGreater(new UpdateChecker.Version("2.1.9"))) {
               var4 = "CoinsEngineEconomyV2";
            } else {
               var4 = "CoinsEngineEconomyV1";
            }

            try {
               Class var5 = Class.forName("com.onyxshop.providers.economys." + var4);
               Constructor var6 = var5.getConstructor(String.class);
               return var1.getCurrency() != null ? (EconomyProvider)var6.newInstance(var1.getCurrency()) : (EconomyProvider)var6.newInstance((String)null);
            } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException | ClassNotFoundException var7) {
               SendMessage.errorMessage("Failed to initialize " + var4 + "\nDetected CoinsEngine version: " + var3.toString());
               var7.printStackTrace();
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
            .filter(var0 -> var0.getType() == EconomyType.LEVELS || var0.getType() == EconomyType.ITEM)
            .collect(Collectors.toList())
      );
      return null;
   }

   private EconomyProvider getAvailableEcon() {
      Object var1 = null;
      if (this.plugin.getServer().getPluginManager().isPluginEnabled("Vault")) {
         SendMessage.infoMessage("Detected Vault, initializing...");
         var1 = new VaultEconomy();
      } else if (this.plugin.getServer().getPluginManager().isPluginEnabled("UltraEconomy")) {
         SendMessage.infoMessage("Detected UltraEconomy, initializing...");
         var1 = new UltraEconomy(null);
      } else if (this.plugin.getServer().getPluginManager().isPluginEnabled("CoinsEngine")
         || this.plugin.getServer().getPluginManager().isPluginEnabled("ExcellentEconomy")) {
         SendMessage.infoMessage("Detected ExcellentEconomy, initializing...");

         try {
            var1 = this.getCoinsEngineInstance(new EcoType(EconomyType.EXCELLENT_ECONOMY));
         } catch (EconomyLoadException var3) {
         }
      } else if (this.plugin.getServer().getPluginManager().isPluginEnabled("EcoBits")) {
         SendMessage.infoMessage("Detected EcoBits, initializing...");
         var1 = new EcoBitsEconomy(null);
      } else if (this.plugin.getServer().getPluginManager().isPluginEnabled("GemsEconomy")) {
         SendMessage.infoMessage("Detected GemsEconomy, initializing...");
         var1 = new GemsEconomy(null);
      } else if (this.plugin.getServer().getPluginManager().isPluginEnabled("PlayerPoints")) {
         SendMessage.infoMessage("Detected PlayerPoints, initializing...");
         var1 = new PlayerPointsEconomy();
      } else if (this.plugin.getServer().getPluginManager().isPluginEnabled("VotingPlugin")) {
         SendMessage.infoMessage("Detected VotingPlugin, initializing...");
         var1 = new VotingPluginEconomy();
      } else if (this.plugin.getServer().getPluginManager().isPluginEnabled("zEssentials")) {
         SendMessage.infoMessage("Detected zEssentials, initializing...");
         var1 = new ZEssentialsEconomy(null);
      } else {
         SendMessage.infoMessage("Failed to find economy plugin, using exp points as default economy, initializing...");
         var1 = new ExpEconomy();
      }

      if (var1 != null) {
         try {
            ((EconomyProvider)var1).setup(this.plugin);
            this.registeredPaymentOptions.put(((EconomyProvider)var1).getType(), (EconomyProvider)var1);
            return (EconomyProvider)var1;
         } catch (EconomyLoadException var4) {
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

   public void registerExternalProvider(ExternalEconomyProvider var1) {
      try {
         var1.setup(this.plugin);
         this.registeredPaymentOptions.put(var1.getType(), var1);
      } catch (EconomyLoadException var3) {
         throw new RuntimeException(var3);
      }
   }

   public void translatePlaceholdersDelayed() {
      this.registeredPaymentOptions.forEach((var0, var1) -> var1.formatPlaceholders());
   }

   private void deleteUnknown(List<EcoType> var1) {
      var1.forEach(var1x -> this.registeredPaymentOptions.remove(var1x));
   }

   public EconomyProvider getEcon(String var1) {
      return this.shopPaymentTypes.containsKey(var1) ? this.shopPaymentTypes.get(var1) : this.defaultProvider;
   }

   public EcoType getEcon() {
      return this.getType();
   }

   public EconomyProvider getEconomy(ShopItem var1) {
      String var2 = ConfigManager.getShop(var1.section).getString((var1.rotates() ? "" : "pages.") + var1.itemLoc + ".economy");
      if (var2 == null) {
         return this.getEcon(var1.section);
      } else {
         EcoType var3 = EconomyType.getFromString(var2);
         if (var3 == null) {
            SendMessage.warnMessage("Could not find a economy provider for item '" + var1.getItemPath() + "' such as '" + var2 + "', using section default...");
            return this.getEcon(var1.section);
         } else if (this.registeredPaymentOptions.containsKey(var3)) {
            return this.registeredPaymentOptions.get(var3);
         } else {
            try {
               return this.initProvider(var3, var1.section + "." + (var1.rotates() ? "" : "pages.") + var1.itemLoc + ".economy", 0);
            } catch (EconomyLoadException var5) {
               SendMessage.errorMessage(
                  "Failed to load economy provider for item '" + var1.getItemPath() + "' with reason '" + var5.getMessage() + "', using section default..."
               );
               return this.getEcon(var1.section);
            }
         }
      }
   }

   public void mayContinue() {
      for (EconomyProvider var2 : this.registeredPaymentOptions.values()) {
         if (var2.doWait()) {
            return;
         }

         try {
            var2.isDecimal();
         } catch (NullPointerException var9) {
            EconomyHandler.ProviderType var4 = this.getType(var2.getType());
            if (var4 == EconomyHandler.ProviderType.DEFAULT) {
               SendMessage.errorMessage("===================================================");
               SendMessage.errorMessage(" ");
               SendMessage.errorMessage(
                  "Failed to register default economy provider(" + var2.getType().toString() + ") because it has not been enabled yet, disabling..."
               );
               SendMessage.errorMessage(" ");
               SendMessage.errorMessage("===================================================");
               this.plugin.getServer().getPluginManager().disablePlugin(this.plugin);
               return;
            }

            if (var4 == EconomyHandler.ProviderType.SHOP) {
               for (String var8 : var4.s.split("::")) {
                  SendMessage.errorMessage(
                     Lang.CANNOT_LOAD_ECONOMY_PROVIDER.get().replace("%shop%", var8).replace("%reason%", "Economy provider did not finish loading in time")
                  );
                  this.shopPaymentTypes.remove(var8);
               }
            }
         }
      }

      Bukkit.getPluginManager().callEvent(new ProviderLoadEvent(DelayedLoadListener.Type.ECONOMY));
   }

   private EconomyHandler.ProviderType getType(EcoType var1) {
      if (this.defaultProvider.getType().equals(var1)) {
         return EconomyHandler.ProviderType.DEFAULT;
      } else {
         EconomyHandler.ProviderType var2 = EconomyHandler.ProviderType.SHOP;
         StringBuilder var3 = new StringBuilder(var2.s);

         for (Entry var5 : this.shopPaymentTypes.entrySet().stream().filter(var1x -> var1x.getValue().getType().equals(var1)).collect(Collectors.toList())) {
            var3.append((String)var5.getKey()).append("::");
         }

         var2.s = var3.toString();
         return !var2.s.isEmpty() ? var2 : null;
      }
   }

   public EconomyProvider getEcon(ShopItem var1) {
      return this.registeredPaymentOptions.get(var1.getEcoType());
   }

   public EconomyProvider getEcon(EcoType var1) {
      return var1 == null ? this.defaultProvider : this.registeredPaymentOptions.get(var1);
   }

   public EconomyProvider getDefaultProvider() {
      return this.defaultProvider;
   }

   public EconomyProvider getLoaded(String var1) {
      EcoType var2 = EconomyType.getFromString(var1);
      return this.registeredPaymentOptions.get(var2);
   }

   public Map<EcoType, Double> has(Map<EcoType, Double> var1, Player var2) {
      return var1.entrySet()
         .stream()
         .filter(var2x -> this.getEcon((EcoType)var2x.getKey()).getBalance(var2) < (Double)var2x.getValue())
         .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
   }

   public boolean withdraw(Map<EcoType, Double> var1, Player var2) {
      HashMap<EcoType, Double> var3 = new HashMap<>();

      for (Entry<EcoType, Double> var4 : var1.entrySet()) {
         if (!this.getEcon(var4.getKey()).withdrawBalance(var2, var4.getValue())) {
            for (Entry<EcoType, Double> var5 : var3.entrySet()) {
               this.getEcon(var5.getKey()).depositBalance(var2, var5.getValue());
            }

            return false;
         }

         var3.put(var4.getKey(), var4.getValue());
      }

      return true;
   }

   public void deposit(Map<EcoType, Double> var1, Player var2) {
      var1.forEach((var2x, var3) -> this.getEcon(var2x).depositBalance(var2, var3));
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
