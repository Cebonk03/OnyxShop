package com.onyxshop.objects.bedrock;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.util.NBTMaps;
import com.onyxshop.util.ServerInfo;
import com.onyxshop.util.SkullUtil;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.platform.spigot.shaded.net.kyori.adventure.key.Key;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.type.CustomSkull;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.registry.type.ItemMappings;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.item.ItemTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.CustomModelData;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;

public final class GeyserCustomIcons {
   private static final int ENCHANTED_OFFSET = 32768;
   private static final Pattern SKIN_URL = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");
   private static final boolean enabled = canEnable();

   private GeyserCustomIcons() {
   }

   private static boolean canEnable() {
      try {
         Class.forName("org.geysermc.geyser.GeyserImpl");
         return true;
      } catch (Throwable exception) {
         return false;
      }
   }

   public static String getIcon(ItemStack itemStack, String itemId, Player player, boolean flag) {
      if (!enabled) {
         return null;
      } else {
         Integer rawId = getRawId(itemStack, itemId, player);
         return rawId == null ? null : String.valueOf(rawId * 65536 + (flag ? '耀' : 0));
      }
   }

   public static Integer getRawId(ItemStack itemStack, String id, Player player) {
      try {
         GeyserSession geyserSession = GeyserImpl.getInstance().connectionByUuid(player.getUniqueId());
         if (geyserSession == null) {
            return null;
         } else {
            ItemMappings itemMappings = geyserSession.getItemMappings();
            ItemMapping itemMapping = itemMappings.getMapping(id);
            if (itemMapping == null) {
               return null;
            } else {
               GeyserItemStack geyserItemStack = GeyserItemStack.of(geyserSession, itemMapping.getJavaItem().javaId(), itemStack.getAmount(), buildComponents(itemStack));
               ItemDefinition itemDefinition = ItemTranslator.getBedrockItemDefinition(geyserSession, geyserItemStack);
               int runtimeId = itemDefinition.getRuntimeId();
               if (itemStack.getType() == Material.PLAYER_HEAD && runtimeId == itemMapping.getBedrockDefinition().getRuntimeId()) {
                  Integer skullId = getSkullId(itemMappings, itemStack);
                  if (skullId != null) {
                     runtimeId = skullId;
                  }
               }

               return runtimeId;
            }
         }
      } catch (Throwable exception) {
         return null;
      }
   }

   private static DataComponents buildComponents(ItemStack itemStack) {
      DataComponents dataComponents = new DataComponents(new HashMap());
      if (ServerInfo.supportsComponents() && OnyxShop.getInstance().versionHandler.hasData(itemStack, NBTMaps.ITEM_MODEL.getTag())) {
         String itemModel = getItemModel(itemStack);
         if (itemModel != null) {
            dataComponents.put(DataComponentTypes.ITEM_MODEL, Key.key(itemModel));
         }
      }

      if (OnyxShop.getInstance().versionHandler.hasData(itemStack, NBTMaps.CUSTOM_MODEL_DATA.getTag())) {
         ItemMeta itemMeta = itemStack.getItemMeta();
         if (ServerInfo.supportsComponents()) {
            CustomModelDataComponent customModelDataComponent = itemMeta.getCustomModelDataComponent();
            dataComponents.put(
               DataComponentTypes.CUSTOM_MODEL_DATA,
               new CustomModelData(
                  customModelDataComponent.getFloats(), customModelDataComponent.getFlags(), customModelDataComponent.getStrings(), customModelDataComponent.getColors().stream().map(Color::asARGB).collect(Collectors.toList())
               )
            );
         } else {
            dataComponents.put(
               DataComponentTypes.CUSTOM_MODEL_DATA,
               new CustomModelData(
                  Collections.singletonList((float)itemMeta.getCustomModelData()), Collections.emptyList(), Collections.emptyList(), Collections.emptyList()
               )
            );
         }
      }

      return dataComponents;
   }

   private static Integer getSkullId(ItemMappings itemMappings, ItemStack itemStack) {
      String skinHash = getSkinHash(itemStack);
      if (skinHash == null) {
         return null;
      } else {
         CustomSkull customSkull = (CustomSkull)BlockRegistries.CUSTOM_SKULLS.get(skinHash);
         if (customSkull == null) {
            return null;
         } else {
            ItemDefinition itemDefinition = (ItemDefinition)itemMappings.getCustomBlockItemDefinitions().get(customSkull.getCustomBlockData());
            return itemDefinition == null ? null : itemDefinition.getRuntimeId();
         }
      }
   }

   private static String getItemModel(ItemStack itemStack) {
      try {
         if (!itemStack.hasItemMeta()) {
            return null;
         } else {
            NamespacedKey namespacedKey = itemStack.getItemMeta().getItemModel();
            return namespacedKey == null ? null : namespacedKey.toString();
         }
      } catch (Throwable exception) {
         return null;
      }
   }

   private static String getSkinHash(ItemStack itemStack) {
      try {
         ItemMeta itemMeta = itemStack.getItemMeta();
         String skullTexture = SkullUtil.getSkullTexture((SkullMeta)itemMeta);
         if (skullTexture != null && !skullTexture.isEmpty()) {
            String text = new String(Base64.getDecoder().decode(skullTexture), StandardCharsets.UTF_8);
            Matcher matcher = SKIN_URL.matcher(text);
            if (!matcher.find()) {
               return null;
            } else {
               String text2 = matcher.group(1);
               int index = text2.lastIndexOf(47);
               return index >= 0 ? text2.substring(index + 1) : text2;
            }
         } else {
            return null;
         }
      } catch (Throwable exception) {
         return null;
      }
   }
}
