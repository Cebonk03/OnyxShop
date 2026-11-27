package com.onyxshop.objects;

import com.onyxshop.util.exceptions.EnchantCostLoadException;
import org.bukkit.configuration.ConfigurationSection;

public class EnchantCost {
   private final double cost;
   private final double percent;
   private final double additionalCost;
   private final double levelMultiplier;
   private final double levelCost;
   private final boolean percentBased;
   private final boolean staticLevelCost;

   private EnchantCost(double amount, double amount2, double amount3, double amount4, double amount5, boolean flag, boolean flag2) {
      this.cost = amount;
      this.percent = amount2;
      this.additionalCost = amount3;
      this.levelMultiplier = amount4;
      this.levelCost = amount5;
      this.percentBased = flag;
      this.staticLevelCost = flag2;
   }

   public static EnchantCost load(ConfigurationSection section) throws EnchantCostLoadException {
      if (section == null) {
         return null;
      } else {
         boolean flag = section.contains("percent");
         if (!flag && !section.contains("cost")) {
            throw new EnchantCostLoadException("Failed to load cost, needs either the 'cost' or 'percent' option");
         } else {
            double amount = section.getDouble("percent", -1.0);
            double amount2 = section.getDouble("cost", -1.0);
            if (flag ? !(amount < 0.0) : !(amount2 < 0.0)) {
               double amount3 = Math.max(0.0, section.getDouble("additional-cost-percent", 0.0));
               boolean flag2 = section.contains("level-cost");
               double amount4 = Math.max(0.0, section.getDouble("level-cost", 0.0));
               double amount5 = Math.max(0.0, section.getDouble("level-multiplier", 1.0));
               return new EnchantCost(amount2, amount, amount3, amount5, amount4, flag, flag2);
            } else {
               throw new EnchantCostLoadException("Cost/percent for enchantment needs to be a positive number");
            }
         }
      }
   }

   public double getCost(double amount, int value) {
      double amount2;
      if (this.percentBased) {
         amount2 = amount * (this.percent / 100.0);
      } else {
         amount2 = this.cost + (this.additionalCost > 0.0 ? amount * (this.additionalCost / 100.0) : 0.0);
      }

      if (value <= 1) {
         return amount2;
      } else {
         return this.staticLevelCost ? amount2 + this.levelCost * (value - 1) : amount2 * (1.0 + (value - 1) * this.levelMultiplier);
      }
   }

   public double getCost(double amount, int value, int value2) {
      if (value2 >= value) {
         return 0.0;
      } else {
         double cost = this.getCost(amount, value);
         if (value2 >= 1) {
            cost -= this.getCost(amount, value2);
         }

         return Math.max(0.0, cost);
      }
   }
}
