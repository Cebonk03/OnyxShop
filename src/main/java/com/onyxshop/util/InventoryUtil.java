package com.onyxshop.util;

import java.lang.reflect.Method;
import java.util.function.Function;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class InventoryUtil {
   private static final Function<Inventory, InventoryHolder> holderFunction = getHolderFunction();
   private static final Function<Player, Inventory> topFunction = getTopFunction();

   public InventoryUtil() {
   }

   public static InventoryHolder getHolder(Inventory var0) {
      return holderFunction.apply(var0);
   }

   public static Inventory getTopInventory(Player var0) {
      return topFunction.apply(var0);
   }

   public static InventoryHolder getTopHolder(Player var0) {
      return holderFunction.apply(topFunction.apply(var0));
   }

   private static Function<Inventory, InventoryHolder> getHolderFunction() {
      try {
         Method var0 = Inventory.class.getMethod("getHolder", boolean.class);
         return var1x -> {
            try {
               return (InventoryHolder)var0.invoke(var1x, false);
            } catch (Exception var3) {
               throw new RuntimeException(var3);
            }
         };
      } catch (NoSuchMethodException var1) {
         return Inventory::getHolder;
      }
   }

   private static Function<Player, Inventory> getTopFunction() {
      try {
         Method var0 = Player.class.getMethod("getOpenInventory");
         Method var1 = var0.getReturnType().getDeclaredMethod("getTopInventory");
         var1.setAccessible(true);
         return var2x -> {
            try {
               Object var3 = var0.invoke(var2x);
               return (Inventory)var1.invoke(var3);
            } catch (Exception var4) {
               throw new RuntimeException(var4);
            }
         };
      } catch (NoSuchMethodException var2) {
         return var0x -> var0x.getOpenInventory().getTopInventory();
      }
   }
}
