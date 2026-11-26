package com.onyxshop.util;

import java.util.stream.Collectors;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.util.serializer.MessageSerializer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class StringSerializer {
   private static final MessageSerializer serializer = new MessageSerializer();
   private static final String[] formats = new String[]{"k", "l", "n", "m", "o"};

   public StringSerializer() {
   }

   public static ItemStack updateItem(ShopItem var0, ItemStack var1) {
      if (var0.needsUpdate() && var1.hasItemMeta()) {
         if (var0.getSerializer().isBukkit()) {
            ItemMeta var2 = var1.getItemMeta();
            if (var2.hasDisplayName()) {
               var2.setDisplayName(fromBukkitSerializer(var0, "§f" + var2.getDisplayName()));
            }

            if (var2.hasLore()) {
               var2.setLore(var2.getLore().stream().map(var1x -> fromBukkitSerializer(var0, var1x)).collect(Collectors.toList()));
            }

            var1.setItemMeta(var2);
         } else {
            try {
               ItemMeta var4 = var1.getItemMeta();
               if (var4.hasDisplayName()) {
                  ReflectUtil.setMetaValue("displayName", var4, serializeString(var0, var4.getDisplayName()));
               }

               if (var4.hasLore()) {
                  ReflectUtil.setMetaValue("lore", var4, var4.getLore().stream().map(var1x -> serializeString(var0, var1x)).collect(Collectors.toList()));
               }

               var1.setItemMeta(var4);
            } catch (Exception var3) {
               SendMessage.warnMessage("Failed to update item with reason: " + var3.getMessage());
               var3.printStackTrace();
               return var1;
            }
         }

         return var1;
      } else {
         return var1;
      }
   }

   public static String fromBukkitSerializer(ShopItem var0, String var1) {
      if (var1.isEmpty()) {
         return var1;
      } else {
         switch (var0.getSerializer()) {
            case BUKKIT:
               return var1;
            case BUKKIT_V2:
               if (var1.startsWith("§f")) {
                  return removeRedundant(var1, "§f");
               }

               return "§f" + var1;
            default:
               return var1;
         }
      }
   }

   private static String removeRedundant(String var0, String var1) {
      for (String var5 : formats) {
         var0 = var0.replaceAll("(?<!^)" + var1 + "§" + var5, "§" + var5);
      }

      return var0;
   }

   public static String serializeString(ShopItem var0, String var1) {
      switch (var0.getSerializer()) {
         case BUKKIT:
            return serializeBukkitMSGToJson(var1);
         case BUKKIT_V2:
            return serializeBukkitMSGToJson("§f" + var1);
         case CUSTOM_V1:
            return serializer.serializeToJson(var1);
         case CUSTOM_V2:
            return serializer.serializeToJson("§f" + var1);
         default:
            return null;
      }
   }

   private static String serializeBukkitMSGToJson(String var0) {
      try {
         return (String)ReflectUtil.invokeStaticLegacyVersionMethod("fromStringToJSON", ".util.CraftChatMessage", true, var0);
      } catch (Exception var2) {
         SendMessage.errorMessage("Failed to update item with reason: " + var2.getMessage());
         var2.printStackTrace();
         return null;
      }
   }
}
