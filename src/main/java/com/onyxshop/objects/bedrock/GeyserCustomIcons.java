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
      } catch (Throwable var1) {
         return false;
      }
   }

   public static String getIcon(ItemStack var0, String var1, Player var2, boolean var3) {
      if (!enabled) {
         return null;
      } else {
         Integer var4 = getRawId(var0, var1, var2);
         return var4 == null ? null : String.valueOf(var4 * 65536 + (var3 ? '耀' : 0));
      }
   }

   public static Integer getRawId(ItemStack var0, String var1, Player var2) {
      try {
         GeyserSession var3 = GeyserImpl.getInstance().connectionByUuid(var2.getUniqueId());
         if (var3 == null) {
            return null;
         } else {
            ItemMappings var4 = var3.getItemMappings();
            ItemMapping var5 = var4.getMapping(var1);
            if (var5 == null) {
               return null;
            } else {
               GeyserItemStack var6 = GeyserItemStack.of(var3, var5.getJavaItem().javaId(), var0.getAmount(), buildComponents(var0));
               ItemDefinition var7 = ItemTranslator.getBedrockItemDefinition(var3, var6);
               int var8 = var7.getRuntimeId();
               if (var0.getType() == Material.PLAYER_HEAD && var8 == var5.getBedrockDefinition().getRuntimeId()) {
                  Integer var9 = getSkullId(var4, var0);
                  if (var9 != null) {
                     var8 = var9;
                  }
               }

               return var8;
            }
         }
      } catch (Throwable var10) {
         return null;
      }
   }

   private static DataComponents buildComponents(ItemStack var0) {
      DataComponents var1 = new DataComponents(new HashMap());
      if (ServerInfo.supportsComponents() && OnyxShop.getInstance().versionHandler.hasData(var0, NBTMaps.ITEM_MODEL.getTag())) {
         String var2 = getItemModel(var0);
         if (var2 != null) {
            var1.put(DataComponentTypes.ITEM_MODEL, Key.key(var2));
         }
      }

      if (OnyxShop.getInstance().versionHandler.hasData(var0, NBTMaps.CUSTOM_MODEL_DATA.getTag())) {
         ItemMeta var4 = var0.getItemMeta();
         if (ServerInfo.supportsComponents()) {
            CustomModelDataComponent var3 = var4.getCustomModelDataComponent();
            var1.put(
               DataComponentTypes.CUSTOM_MODEL_DATA,
               new CustomModelData(
                  var3.getFloats(), var3.getFlags(), var3.getStrings(), var3.getColors().stream().map(Color::asARGB).collect(Collectors.toList())
               )
            );
         } else {
            var1.put(
               DataComponentTypes.CUSTOM_MODEL_DATA,
               new CustomModelData(
                  Collections.singletonList((float)var4.getCustomModelData()), Collections.emptyList(), Collections.emptyList(), Collections.emptyList()
               )
            );
         }
      }

      return var1;
   }

   private static Integer getSkullId(ItemMappings var0, ItemStack var1) {
      String var2 = getSkinHash(var1);
      if (var2 == null) {
         return null;
      } else {
         CustomSkull var3 = (CustomSkull)BlockRegistries.CUSTOM_SKULLS.get(var2);
         if (var3 == null) {
            return null;
         } else {
            ItemDefinition var4 = (ItemDefinition)var0.getCustomBlockItemDefinitions().get(var3.getCustomBlockData());
            return var4 == null ? null : var4.getRuntimeId();
         }
      }
   }

   private static String getItemModel(ItemStack var0) {
      try {
         if (!var0.hasItemMeta()) {
            return null;
         } else {
            NamespacedKey var1 = var0.getItemMeta().getItemModel();
            return var1 == null ? null : var1.toString();
         }
      } catch (Throwable var2) {
         return null;
      }
   }

   private static String getSkinHash(ItemStack var0) {
      try {
         ItemMeta var1 = var0.getItemMeta();
         String var2 = SkullUtil.getSkullTexture((SkullMeta)var1);
         if (var2 != null && !var2.isEmpty()) {
            String var3 = new String(Base64.getDecoder().decode(var2), StandardCharsets.UTF_8);
            Matcher var4 = SKIN_URL.matcher(var3);
            if (!var4.find()) {
               return null;
            } else {
               String var5 = var4.group(1);
               int var6 = var5.lastIndexOf(47);
               return var6 >= 0 ? var5.substring(var6 + 1) : var5;
            }
         } else {
            return null;
         }
      } catch (Throwable var7) {
         return null;
      }
   }
}
