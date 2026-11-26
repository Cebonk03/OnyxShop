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

   private EnchantCost(double var1, double var3, double var5, double var7, double var9, boolean var11, boolean var12) {
      this.cost = var1;
      this.percent = var3;
      this.additionalCost = var5;
      this.levelMultiplier = var7;
      this.levelCost = var9;
      this.percentBased = var11;
      this.staticLevelCost = var12;
   }

   public static EnchantCost load(ConfigurationSection var0) throws EnchantCostLoadException {
      if (var0 == null) {
         return null;
      } else {
         boolean var1 = var0.contains("percent");
         if (!var1 && !var0.contains("cost")) {
            throw new EnchantCostLoadException("Failed to load cost, needs either the 'cost' or 'percent' option");
         } else {
            double var2 = var0.getDouble("percent", -1.0);
            double var4 = var0.getDouble("cost", -1.0);
            if (var1 ? !(var2 < 0.0) : !(var4 < 0.0)) {
               double var6 = Math.max(0.0, var0.getDouble("additional-cost-percent", 0.0));
               boolean var8 = var0.contains("level-cost");
               double var9 = Math.max(0.0, var0.getDouble("level-cost", 0.0));
               double var11 = Math.max(0.0, var0.getDouble("level-multiplier", 1.0));
               return new EnchantCost(var4, var2, var6, var11, var9, var1, var8);
            } else {
               throw new EnchantCostLoadException("Cost/percent for enchantment needs to be a positive number");
            }
         }
      }
   }

   public double getCost(double var1, int var3) {
      double var4;
      if (this.percentBased) {
         var4 = var1 * (this.percent / 100.0);
      } else {
         var4 = this.cost + (this.additionalCost > 0.0 ? var1 * (this.additionalCost / 100.0) : 0.0);
      }

      if (var3 <= 1) {
         return var4;
      } else {
         return this.staticLevelCost ? var4 + this.levelCost * (var3 - 1) : var4 * (1.0 + (var3 - 1) * this.levelMultiplier);
      }
   }

   public double getCost(double var1, int var3, int var4) {
      if (var4 >= var3) {
         return 0.0;
      } else {
         double var5 = this.getCost(var1, var3);
         if (var4 >= 1) {
            var5 -= this.getCost(var1, var4);
         }

         return Math.max(0.0, var5);
      }
   }
}
