package com.onyxshop.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;

public enum NBTMaps {
   MAX_STACK_SIZE(null, "max_stack_size"),
   MAX_DAMAGE(null, "max_damage"),
   DAMAGE("Damage", "damage"),
   UNBREAKABLE("Unbreakable", "unbreakable"),
   NAME("display::Name", "item_name"),
   DISPLAYNAME("display::Name", "custom_name"),
   LORE("display::Lore", "lore"),
   RARITY(null, "rarity"),
   ENCHANTMENTS("Enchantments", "enchantments"),
   CAN_PLACE_ON("CanPlaceOn", "can_place_on"),
   CAN_BREAK("CanDestroy", "can_break"),
   ATTRIBUTE_MODIFIERS("AttributeModifiers", "attribute_modifiers"),
   CUSTOM_MODEL_DATA("CustomModelData", "custom_model_data"),
   ITEM_MODEL(null, "item_model"),
   HIDE_FLAGS("HideFlags", "1.20.5|hide_tooltip", "1.21.5|tooltip_display"),
   REPAIR_COST("RepairCost", "repair_cost"),
   CREATIVE_SLOT_LOCK(null, "creative_slot_lock"),
   ENCHANTMENT_GLINT(null, "enchantment_glint_override"),
   INTANGIBLE_PROJECTILE(null, "intangible_projectile"),
   FOOD(null, "food"),
   TOOL(null, "tool"),
   STORED_ENCHANTMENTS("StoredEnchantments", "stored_enchantments"),
   ARMOR_COLOR("display::color", "dyed_color"),
   MAP_COLOR("display::MapColor", "map_color"),
   MAP_ID("map", "map_id"),
   MAP_DECORATIONS("Decorations", "map_decorations"),
   MAP_POST_PROCESSING("map_scale_direction", "map_post_processing"),
   CHARGED_PROJECTILES("ChargedProjectiles", "charged_projectiles"),
   BUNDLE_CONTENTS("Items", "bundle_contents"),
   BASE_POTION("Potion", "potion_contents::potion"),
   CUSTOM_POTION_EFFECTS("custom_potion_effects", "potion_contents::custom_effects"),
   CUSTOM_POTION_COLOR("CustomPotionColor", "potion_contents::custom_color"),
   SUSPICIOUS_STEW_EFFECTS("effects", "suspicious_stew_effects"),
   WRITEABLE_BOOK_CONTENT(null, "writable_book_content"),
   WRITTEN_BOOK_CONTENT("pages", "written_book_content::pages"),
   BOOK_AUTHOR("author", "written_book_content::author"),
   BOOK_TITLE("title", "written_book_content::title"),
   TRIM_MATERIAL("Trim::material", "trim::material"),
   TRIM_PATTERN("Trim::pattern", "trim::pattern"),
   DEBUG_STICK("DebugProperty", "debug_stick_state"),
   ENTITY_DATA("EntityTag", "entity_data"),
   BUCKET_ENTITY_DATA("BucketVariantTag", "bucket_entity_data"),
   BLOCK_ENTITY_DATA("BlockEntityTag", "block_entity_data"),
   INSTRUMENT("instrument", "instrument"),
   RECIPES("Recipes", "recipes"),
   LODESTONE_TRACKER("LodestoneTracked", "lodestone_tracker"),
   FIREWORK_EXPLOSION("Explosion", "firework_explosion"),
   FIREWORK_EXPLOSIONS("Fireworks::Explosions", "fireworks::explosions"),
   FIREWORK_FLIGHT_DURATION("Fireworks::Flight", "fireworks::flight_duration"),
   SKULLOWNER_ID("SkullOwner::Id", "profile::id"),
   SKULLOWNER_NAME("SkullOwner::Name", "profile::name"),
   SKULLOWNER_PROPERTIES("SkullOwner::Properties", "profile::properties"),
   BANNER_PATTERNS("BlockEntityTag::Patterns", "banner_patterns"),
   BASE_COLOR("BlockEntityTag::Base", "base_color"),
   PUBLIC_BUKKIT_VALUES("PublicBukkitValues", "custom_data::PublicBukkitValues"),
   CUSTOM_DATA("", "custom_data::");

   final String NBT;
   final String COMPOUND;
   static final Set<String> registered = getRegistered();

   private NBTMaps(String text, String text2) {
      this.NBT = text;
      this.COMPOUND = text2;
   }

   private NBTMaps(String text, String... values) {
      this.NBT = text;
      String text2 = null;

      for (String text3 : values) {
         if (!text3.contains("|")) {
            text2 = text3;
         } else if (ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.getFromSimple(text3.split("\\|")[0]))) {
            text2 = text3.split("\\|")[1];
         }
      }

      this.COMPOUND = text2 == null ? values[0] : text2;
   }

   public String getTag() {
      return ServerInfo.supportsComponents() ? this.COMPOUND : this.NBT;
   }

   public static List<String> getTags(boolean flag, String... values) {
      return Arrays.stream(values).map(element -> getTag(element, null, null, flag)).collect(Collectors.toList());
   }

   public static String getTag(String text) {
      return getTag(text, null, null, true);
   }

   public static String getTag(String text, String text2, String text3) {
      return getTag(text, text2, text3, true);
   }

   public static String getTag(String text, String text2, String text3, boolean flag) {
      boolean flag2 = ServerInfo.supportsComponents();
      NBTMaps nbtMaps = null;
      boolean flag3 = false;
      if (text.contains("::")) {
         try {
            nbtMaps = getFromSimple(text);
         } catch (IllegalArgumentException exception) {
         }
      } else {
         try {
            nbtMaps = getFromSimple(text);
         } catch (IllegalArgumentException exception) {
            for (NBTMaps nbtMaps2 : values()) {
               if (nbtMaps2.NBT != null && nbtMaps2.NBT.contains("::") && nbtMaps2.NBT.split("::")[0].equals(text)) {
                  nbtMaps = nbtMaps2;
                  flag3 = true;
                  break;
               }

               if (nbtMaps2.COMPOUND != null && nbtMaps2.COMPOUND.contains("::") && nbtMaps2.COMPOUND.split("::")[0].equals(text)) {
                  nbtMaps = nbtMaps2;
                  flag3 = true;
                  break;
               }
            }
         }
      }

      if (nbtMaps != null) {
         String text4 = !flag2 ? nbtMaps.NBT : nbtMaps.COMPOUND;
         if (text4 == null) {
            if (flag) {
               SendMessage.warnMessage("Found incompatible NBT tag: NBT tag for given component tag '" + text + "' doesn't exist for current MC version");
               if (text3 != null) {
                  logPath(text3, text2);
               }
            }

            return text;
         } else {
            text4 = flag3 ? text4.split("::")[0] : text4;
            if (!text.equals(text4) && flag) {
               SendMessage.warnMessage(
                  "Found incompatible NBT/component tag for current minecraft version: Did you mean '"
                     + text4
                     + "' instead of '"
                     + text
                     + "'?\nPlease correct the value manually in config to get rid of this message"
               );
               if (text3 != null) {
                  logPath(text3, text2);
               }
            }

            return text4;
         }
      } else if (!flag2 || !text.startsWith(CUSTOM_DATA.COMPOUND) && !registered.contains(text.split("::")[0])) {
         if (flag2 && !text.startsWith(CUSTOM_DATA.COMPOUND)) {
            if (flag) {
               SendMessage.warnMessage("Found unknown component tag '" + text + "' for current minecraft version, check spelling or make sure it exists");
               if (text3 != null) {
                  logPath(text3, text2);
               }
            }

            return CUSTOM_DATA.COMPOUND + text;
         } else {
            return text;
         }
      } else {
         return text;
      }
   }

   private static NBTMaps getFromSimple(String text) {
      for (NBTMaps nbtMaps : values()) {
         if (nbtMaps.NBT != null && nbtMaps.NBT.equals(text) || nbtMaps.COMPOUND != null && nbtMaps.COMPOUND.equals(text)) {
            return nbtMaps;
         }
      }

      throw new IllegalArgumentException();
   }

   private static void logPath(String path, String text) {
      if (text == null) {
         SendMessage.errorItemConfig(Lang.ITEMS_PATH_IN_CONFIG.get().toString().replace("%location%", path));
      } else {
         SendMessage.errorItemShops(Lang.ITEMS_LOCATION_IN_SHOPS.get().toString().replace("%section%", text).replace("%location%", path));
      }
   }

   private static Set<String> getRegistered() {
      if (!ServerInfo.supportsComponents()) {
         return new HashSet<>();
      } else {
         try {
            String shopId = OnyxShop.getInstance().versionHandler.getClass().getName();
            return (Set<String>)ReflectUtil.invokeLegacyVersionMethod("getRegistered", shopId + "$Components");
         } catch (Exception exception) {
            return new HashSet<>();
         }
      }
   }

   private static void doExistCheck() {
      for (NBTMaps nbtMaps : values()) {
         if (!registered.contains(nbtMaps.COMPOUND.split("::")[0])) {
            SendMessage.errorMessage("!! Mapping for '" + nbtMaps.COMPOUND + "' doesn't exist anymore or got renamed in current minecraft version!");
         }
      }
   }
}
