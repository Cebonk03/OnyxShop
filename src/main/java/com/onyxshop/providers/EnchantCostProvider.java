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

   public EnchantCostProvider(OnyxShop plugin) {
      this.plugin = plugin;
   }

   public void reload() {
      this.costs.clear();
      File file = new File(this.plugin.getDataFolder(), "enchant-costs.yml");
      if (!file.exists()) {
         this.plugin.saveResource("enchant-costs.yml", false);
      }

      try {
         YamlConfiguration config = OnyxShop.getInstance().loadConfiguration(file);
         ConfigurationSection section = config.getConfigurationSection("enchants");
         if (section == null) {
            return;
         }

         for (String text : section.getKeys(false)) {
            try {
               EnchantCost enchantCost = EnchantCost.load(section.getConfigurationSection(text));
               this.costs.put(text.toLowerCase(Locale.ENGLISH), enchantCost);
            } catch (EnchantCostLoadException exception) {
               SendMessage.warnMessage("Failed to load enchantment cost from enchant-costs.yml: " + exception.getMessage());
            }
         }

         SendMessage.infoMessage(Lang.ENCHANTMENT_COSTS_LOADED.get().replace("%amount%", String.valueOf(this.costs.size())));
      } catch (ConfigLoadException exception) {
         SendMessage.errorMessage(Lang.ERROR_LOADING_ENCHANTMENT_COSTS.get());
         exception.printStackTrace();
      }
   }

   public boolean isEmpty() {
      return this.costs.isEmpty();
   }

   public double getEnchantAddition(ItemStack itemStack, ItemStack itemStack2, double amount) {
      if (this.costs.isEmpty()) {
         return 0.0;
      } else {
         Map<String, Integer> enchantLevels = this.getEnchantLevels(itemStack2);
         double amount2 = 0.0;

         for (Entry entry : this.getEnchantLevels(itemStack).entrySet()) {
            EnchantCost enchantCost = this.costs.get(entry.getKey());
            if (enchantCost != null) {
               amount2 += enchantCost.getCost(amount, (Integer)entry.getValue(), enchantLevels.getOrDefault(entry.getKey(), 0));
            }
         }

         return amount2;
      }
   }

   private Map<String, Integer> getEnchantLevels(ItemStack itemStack) {
      HashMap map = new HashMap();

      for (String text : this.plugin.enchantsProvider.getEnchantKeys(itemStack)) {
         int index = text.lastIndexOf(58);
         String text2 = text;
         int value = 1;
         if (index > 0) {
            try {
               text2 = text.substring(0, index);
               value = Integer.parseInt(text.substring(index + 1));
            } catch (NumberFormatException exception) {
            }
         }

         map.put(text2.toLowerCase(Locale.ENGLISH), value);
      }

      return map;
   }
}
