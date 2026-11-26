package com.onyxshop.objects.stands;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.onyxshop.files.Lang;
import com.onyxshop.util.XMaterial;
import com.onyxshop.util.exceptions.StandLoadException;
import org.bukkit.Material;
import org.bukkit.block.Block;

public enum StandType {
   CHEST,
   ENDER_CHEST,
   RESIN_BRICK_SLAB,
   PALE_OAK_SLAB,
   TUFF_SLAB,
   POLISHED_TUFF_SLAB,
   TUFF_BRICK_SLAB,
   ACACIA_SLAB,
   ANDESITE_SLAB,
   BAMBOO_MOSAIC_SLAB,
   BAMBOO_SLAB,
   BIRCH_SLAB,
   BLACKSTONE_SLAB,
   BRICK_SLAB,
   CHERRY_SLAB,
   COBBLED_DEEPSLATE_SLAB,
   COBBLESTONE_SLAB,
   CRIMSON_SLAB,
   CUT_COPPER_SLAB,
   CUT_RED_SANDSTONE_SLAB,
   CUT_SANDSTONE_SLAB,
   DARK_OAK_SLAB,
   DARK_PRISMARINE_SLAB,
   DEEPSLATE_BRICK_SLAB,
   DEEPSLATE_TILE_SLAB,
   DIORITE_SLAB,
   END_STONE_BRICK_SLAB,
   EXPOSED_CUT_COPPER_SLAB,
   GRANITE_SLAB,
   JUNGLE_SLAB,
   MANGROVE_SLAB,
   MOSSY_COBBLESTONE_SLAB,
   MOSSY_STONE_BRICK_SLAB,
   MUD_BRICK_SLAB,
   NETHER_BRICK_SLAB,
   OAK_SLAB,
   OXIDIZED_CUT_COPPER_SLAB,
   PETRIFIED_OAK_SLAB,
   POLISHED_ANDESITE_SLAB,
   POLISHED_BLACKSTONE_BRICK_SLAB,
   POLISHED_BLACKSTONE_SLAB,
   POLISHED_DEEPSLATE_SLAB,
   POLISHED_DIORITE_SLAB,
   POLISHED_GRANITE_SLAB,
   PRISMARINE_BRICK_SLAB,
   PRISMARINE_SLAB,
   PURPUR_SLAB,
   QUARTZ_SLAB,
   RED_NETHER_BRICK_SLAB,
   RED_SANDSTONE_SLAB,
   SANDSTONE_SLAB,
   SMOOTH_QUARTZ_SLAB,
   SMOOTH_RED_SANDSTONE_SLAB,
   SMOOTH_SANDSTONE_SLAB,
   SMOOTH_STONE_SLAB,
   SPRUCE_SLAB,
   STONE_BRICK_SLAB,
   STONE_SLAB,
   WARPED_SLAB,
   WAXED_CUT_COPPER_SLAB,
   WAXED_EXPOSED_CUT_COPPER_SLAB,
   WAXED_WEATHERED_CUT_COPPER_SLAB,
   WEATHERED_CUT_COPPER_SLAB;

   private final XMaterial type = XMaterial.valueOf(this.name());

   private StandType() {
   }

   public boolean isOfType(Material var1) {
      try {
         return this.type == XMaterial.matchXMaterial(var1.name()).get();
      } catch (IllegalArgumentException var3) {
         return false;
      }
   }

   public static List<String> getSupported() {
      return Arrays.stream(values()).filter(var0 -> var0.type.isSupported()).map(Enum::name).collect(Collectors.toList());
   }

   public XMaterial getType() {
      return this.type;
   }

   public static boolean isValidType(Material var0) {
      return var0 == Material.CHEST || var0 == Material.ENDER_CHEST || var0.name().endsWith("_SLAB");
   }

   public static StandType fromType(Block var0) {
      try {
         return valueOf(XMaterial.matchXMaterial(var0).name());
      } catch (IllegalArgumentException var2) {
         return null;
      }
   }

   public static StandType fromName(String var0) throws StandLoadException {
      try {
         return valueOf(var0);
      } catch (IllegalArgumentException var2) {
         throw new StandLoadException(Lang.INVALID_STAND_TYPE.get().replace("%type%", var0));
      }
   }
}
