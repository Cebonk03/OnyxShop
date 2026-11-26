package com.onyxshop.providers;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.EnchantCost;
import com.onyxshop.util.exceptions.ConfigLoadException;
import com.onyxshop.util.exceptions.EnchantCostLoadException;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

public class EnchantCostProvider {
   private final OnyxShop plugin;
   private final Map<String, EnchantCost> costs = new HashMap<>();

   public EnchantCostProvider(OnyxShop var1) {
      this.plugin = var1;
   }

   public void reload() {
      this.costs.clear();
      File var1 = new File(this.plugin.getDataFolder(), "enchant-costs.yml");
      if (!var1.exists()) {
         this.plugin.saveResource("enchant-costs.yml", false);
      }

      try {
         YamlConfiguration var2 = OnyxShop.getInstance().loadConfiguration(var1);
         ConfigurationSection var3 = var2.getConfigurationSection("enchants");
         if (var3 == null) {
            return;
         }

         for (String var5 : var3.getKeys(false)) {
            try {
               EnchantCost var6 = EnchantCost.load(var3.getConfigurationSection(var5));
               this.costs.put(var5.toLowerCase(Locale.ENGLISH), var6);
            } catch (EnchantCostLoadException var7) {
               SendMessage.warnMessage("Failed to load enchantment cost from enchant-costs.yml: " + var7.getMessage());
            }
         }

         SendMessage.infoMessage(Lang.ENCHANTMENT_COSTS_LOADED.get().replace("%amount%", String.valueOf(this.costs.size())));
      } catch (ConfigLoadException var8) {
         SendMessage.errorMessage(Lang.ERROR_LOADING_ENCHANTMENT_COSTS.get());
         var8.printStackTrace();
      }
   }

   public boolean isEmpty() {
      return this.costs.isEmpty();
   }

   public double getEnchantAddition(ItemStack var1, ItemStack var2, double var3) {
      if (this.costs.isEmpty()) {
         return 0.0;
      } else {
         Map<String, Integer> var5 = this.getEnchantLevels(var2);
         double var6 = 0.0;

         for (Entry var9 : this.getEnchantLevels(var1).entrySet()) {
            EnchantCost var10 = this.costs.get(var9.getKey());
            if (var10 != null) {
               var6 += var10.getCost(var3, (Integer)var9.getValue(), var5.getOrDefault(var9.getKey(), 0));
            }
         }

         return var6;
      }
   }

   private Map<String, Integer> getEnchantLevels(ItemStack var1) {
      HashMap var2 = new HashMap();

      for (String var4 : this.plugin.enchantsProvider.getEnchantKeys(var1)) {
         int var5 = var4.lastIndexOf(58);
         String var6 = var4;
         int var7 = 1;
         if (var5 > 0) {
            try {
               var6 = var4.substring(0, var5);
               var7 = Integer.parseInt(var4.substring(var5 + 1));
            } catch (NumberFormatException var9) {
            }
         }

         var2.put(var6.toLowerCase(Locale.ENGLISH), var7);
      }

      return var2;
   }
}
