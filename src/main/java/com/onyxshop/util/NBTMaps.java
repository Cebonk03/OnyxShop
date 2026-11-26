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

   private NBTMaps(String nullxx, String nullxxx) {
      this.NBT = nullxx;
      this.COMPOUND = nullxxx;
   }

   private NBTMaps(String nullxx, String... nullxxx) {
      this.NBT = nullxx;
      String var5 = null;

      for (String var9 : nullxxx) {
         if (!var9.contains("|")) {
            var5 = var9;
         } else if (ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.getFromSimple(var9.split("\\|")[0]))) {
            var5 = var9.split("\\|")[1];
         }
      }

      this.COMPOUND = var5 == null ? nullxxx[0] : var5;
   }

   public String getTag() {
      return ServerInfo.supportsComponents() ? this.COMPOUND : this.NBT;
   }

   public static List<String> getTags(boolean var0, String... var1) {
      return Arrays.stream(var1).map(var1x -> getTag(var1x, null, null, var0)).collect(Collectors.toList());
   }

   public static String getTag(String var0) {
      return getTag(var0, null, null, true);
   }

   public static String getTag(String var0, String var1, String var2) {
      return getTag(var0, var1, var2, true);
   }

   public static String getTag(String var0, String var1, String var2, boolean var3) {
      boolean var4 = ServerInfo.supportsComponents();
      NBTMaps var5 = null;
      boolean var6 = false;
      if (var0.contains("::")) {
         try {
            var5 = getFromSimple(var0);
         } catch (IllegalArgumentException var12) {
         }
      } else {
         try {
            var5 = getFromSimple(var0);
         } catch (IllegalArgumentException var13) {
            for (NBTMaps var11 : values()) {
               if (var11.NBT != null && var11.NBT.contains("::") && var11.NBT.split("::")[0].equals(var0)) {
                  var5 = var11;
                  var6 = true;
                  break;
               }

               if (var11.COMPOUND != null && var11.COMPOUND.contains("::") && var11.COMPOUND.split("::")[0].equals(var0)) {
                  var5 = var11;
                  var6 = true;
                  break;
               }
            }
         }
      }

      if (var5 != null) {
         String var7 = !var4 ? var5.NBT : var5.COMPOUND;
         if (var7 == null) {
            if (var3) {
               SendMessage.warnMessage("Found incompatible NBT tag: NBT tag for given component tag '" + var0 + "' doesn't exist for current MC version");
               if (var2 != null) {
                  logPath(var2, var1);
               }
            }

            return var0;
         } else {
            var7 = var6 ? var7.split("::")[0] : var7;
            if (!var0.equals(var7) && var3) {
               SendMessage.warnMessage(
                  "Found incompatible NBT/component tag for current minecraft version: Did you mean '"
                     + var7
                     + "' instead of '"
                     + var0
                     + "'?\nPlease correct the value manually in config to get rid of this message"
               );
               if (var2 != null) {
                  logPath(var2, var1);
               }
            }

            return var7;
         }
      } else if (!var4 || !var0.startsWith(CUSTOM_DATA.COMPOUND) && !registered.contains(var0.split("::")[0])) {
         if (var4 && !var0.startsWith(CUSTOM_DATA.COMPOUND)) {
            if (var3) {
               SendMessage.warnMessage("Found unknown component tag '" + var0 + "' for current minecraft version, check spelling or make sure it exists");
               if (var2 != null) {
                  logPath(var2, var1);
               }
            }

            return CUSTOM_DATA.COMPOUND + var0;
         } else {
            return var0;
         }
      } else {
         return var0;
      }
   }

   private static NBTMaps getFromSimple(String var0) {
      for (NBTMaps var4 : values()) {
         if (var4.NBT != null && var4.NBT.equals(var0) || var4.COMPOUND != null && var4.COMPOUND.equals(var0)) {
            return var4;
         }
      }

      throw new IllegalArgumentException();
   }

   private static void logPath(String var0, String var1) {
      if (var1 == null) {
         SendMessage.errorItemConfig(Lang.ITEMS_PATH_IN_CONFIG.get().toString().replace("%location%", var0));
      } else {
         SendMessage.errorItemShops(Lang.ITEMS_LOCATION_IN_SHOPS.get().toString().replace("%section%", var1).replace("%location%", var0));
      }
   }

   private static Set<String> getRegistered() {
      if (!ServerInfo.supportsComponents()) {
         return new HashSet<>();
      } else {
         try {
            String var0 = OnyxShop.getInstance().versionHandler.getClass().getName();
            return (Set<String>)ReflectUtil.invokeLegacyVersionMethod("getRegistered", var0 + "$Components");
         } catch (Exception var1) {
            return new HashSet<>();
         }
      }
   }

   private static void doExistCheck() {
      for (NBTMaps var3 : values()) {
         if (!registered.contains(var3.COMPOUND.split("::")[0])) {
            SendMessage.errorMessage("!! Mapping for '" + var3.COMPOUND + "' doesn't exist anymore or got renamed in current minecraft version!");
         }
      }
   }
}
