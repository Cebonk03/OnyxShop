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

   public static ItemStack updateItem(ShopItem shopItem, ItemStack itemStack) {
      if (shopItem.needsUpdate() && itemStack.hasItemMeta()) {
         if (shopItem.getSerializer().isBukkit()) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            if (itemMeta.hasDisplayName()) {
               itemMeta.setDisplayName(fromBukkitSerializer(shopItem, "§f" + itemMeta.getDisplayName()));
            }

            if (itemMeta.hasLore()) {
               itemMeta.setLore(itemMeta.getLore().stream().map(element -> fromBukkitSerializer(shopItem, element)).collect(Collectors.toList()));
            }

            itemStack.setItemMeta(itemMeta);
         } else {
            try {
               ItemMeta itemMeta = itemStack.getItemMeta();
               if (itemMeta.hasDisplayName()) {
                  ReflectUtil.setMetaValue("displayName", itemMeta, serializeString(shopItem, itemMeta.getDisplayName()));
               }

               if (itemMeta.hasLore()) {
                  ReflectUtil.setMetaValue("lore", itemMeta, itemMeta.getLore().stream().map(element -> serializeString(shopItem, element)).collect(Collectors.toList()));
               }

               itemStack.setItemMeta(itemMeta);
            } catch (Exception exception) {
               SendMessage.warnMessage("Failed to update item with reason: " + exception.getMessage());
               exception.printStackTrace();
               return itemStack;
            }
         }

         return itemStack;
      } else {
         return itemStack;
      }
   }

   public static String fromBukkitSerializer(ShopItem shopItem, String shopId) {
      if (shopId.isEmpty()) {
         return shopId;
      } else {
         switch (shopItem.getSerializer()) {
            case BUKKIT:
               return shopId;
            case BUKKIT_V2:
               if (shopId.startsWith("§f")) {
                  return removeRedundant(shopId, "§f");
               }

               return "§f" + shopId;
            default:
               return shopId;
         }
      }
   }

   private static String removeRedundant(String text, String text2) {
      for (String text3 : formats) {
         text = text.replaceAll("(?<!^)" + text2 + "§" + text3, "§" + text3);
      }

      return text;
   }

   public static String serializeString(ShopItem shopItem, String shopId) {
      switch (shopItem.getSerializer()) {
         case BUKKIT:
            return serializeBukkitMSGToJson(shopId);
         case BUKKIT_V2:
            return serializeBukkitMSGToJson("§f" + shopId);
         case CUSTOM_V1:
            return serializer.serializeToJson(shopId);
         case CUSTOM_V2:
            return serializer.serializeToJson("§f" + shopId);
         default:
            return null;
      }
   }

   private static String serializeBukkitMSGToJson(String text) {
      try {
         return (String)ReflectUtil.invokeStaticLegacyVersionMethod("fromStringToJSON", ".util.CraftChatMessage", true, text);
      } catch (Exception exception) {
         SendMessage.errorMessage("Failed to update item with reason: " + exception.getMessage());
         exception.printStackTrace();
         return null;
      }
   }
}
