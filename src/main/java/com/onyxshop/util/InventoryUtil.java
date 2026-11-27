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

   public static InventoryHolder getHolder(Inventory inventory) {
      return holderFunction.apply(inventory);
   }

   public static Inventory getTopInventory(Player player) {
      return topFunction.apply(player);
   }

   public static InventoryHolder getTopHolder(Player player) {
      return holderFunction.apply(topFunction.apply(player));
   }

   private static Function<Inventory, InventoryHolder> getHolderFunction() {
      try {
         Method method = Inventory.class.getMethod("getHolder", boolean.class);
         return element -> {
            try {
               return (InventoryHolder)method.invoke(element, false);
            } catch (Exception exception) {
               throw new RuntimeException(exception);
            }
         };
      } catch (NoSuchMethodException exception) {
         return Inventory::getHolder;
      }
   }

   private static Function<Player, Inventory> getTopFunction() {
      try {
         Method method = Player.class.getMethod("getOpenInventory");
         Method method2 = method.getReturnType().getDeclaredMethod("getTopInventory");
         method2.setAccessible(true);
         return element -> {
            try {
               Object value = method.invoke(element);
               return (Inventory)method2.invoke(value);
            } catch (Exception exception) {
               throw new RuntimeException(exception);
            }
         };
      } catch (NoSuchMethodException exception) {
         return element -> element.getOpenInventory().getTopInventory();
      }
   }
}
