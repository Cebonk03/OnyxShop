package com.onyxshop.util;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

public enum PotionTypes {
   WATER("WATER", 0, false, 0, false, "EMPTY"),
   MUNDANE("MUNDANE", 0, false, 0, false),
   THICK("THICK", 0, false, 0, false),
   AWKWARD("AWKWARD", 0, false, 0, false),
   FIRE_RESISTANCE("FIRE_RESISTANCE", 3600, false, 0, false, "RESISTANCE_FIRE"),
   LONG_FIRE_RESISTANCE("FIRE_RESISTANCE", 9600, true, 0, false, "FIRE_RESISTANCE_LONG", "RESISTANCE_FIRE_LONG"),
   HARMING("INSTANT_DAMAGE", 1, false, 0, false, "INSTANT_DAMAGE", "HARM", "DAMAGE_INSTANT"),
   STRONG_HARMING("INSTANT_DAMAGE", 1, false, 1, true, "STRONG_INSTANT_DAMAGE", "STRONG_HARM", "STRONG_DAMAGE_INSTANT"),
   HEALING("INSTANT_HEALTH", "INSTANT_HEAL", 1, false, 0, false, "INSTANT_HEALTH", "HEAL"),
   STRONG_HEALING("INSTANT_HEALTH", "INSTANT_HEAL", 1, false, 1, true, "STRONG_INSTANT_HEALTH", "STRONG_HEAL"),
   INVISIBILITY("INVISIBILITY", 3600, false, 0, false, "INVIS"),
   LONG_INVISIBILITY("INVISIBILITY", 9600, true, 0, false, "LONG_INVIS"),
   LEAPING("JUMP_BOOST", "JUMP", 3600, false, 0, false, "JUMP_BOOST", "JUMP"),
   LONG_LEAPING("JUMP_BOOST", "JUMP", 9600, true, 0, false, "LONG_JUMP_BOOST", "LONG_JUMP"),
   STRONG_LEAPING("JUMP_BOOST", "JUMP", 1800, false, 1, true, "STRONG_JUMP_BOOST", "STRONG_JUMP"),
   LUCK("LUCK", 6000, false, 0, false, "LUCKY"),
   NIGHT_VISION("NIGHT_VISION", 3600, false, 0, false),
   LONG_NIGHT_VISION("NIGHT_VISION", 9600, true, 0, false),
   POISON("POISON", 900, false, 0, false),
   LONG_POISON("POISON", 2400, true, 0, false),
   STRONG_POISON("POISON", 440, false, 0, true),
   REGENERATION("REGENERATION", "REGEN", 900, false, 0, false, "REGEN"),
   LONG_REGENERATION("REGENERATION", "REGEN", 2400, true, 0, false, "LONG_REGEN"),
   STRONG_REGENERATION("REGENERATION", "REGEN", 440, false, 1, true, "STRONG_REGEN"),
   SLOW_FALLING("SLOW_FALLING", 1800, false, 0, false),
   LONG_SLOW_FALLING("SLOW_FALLING", 4800, true, 0, false),
   SLOWNESS("SLOWNESS", 1800, false, 0, false, "SLOW"),
   LONG_SLOWNESS("SLOWNESS", 4800, true, 0, false, "LONG_SLOW"),
   STRONG_SLOWNESS("SLOWNESS", 200, false, 3, true, "STRONG_SLOW"),
   STRENGTH("STRENGTH", 3600, false, 0, false),
   LONG_STRENGTH("STRENGTH", 9600, true, 0, false),
   STRONG_STRENGTH("STRENGTH", 1800, false, 1, true),
   SWIFTNESS("SPEED", 3600, false, 0, false, "SPEED"),
   LONG_SWIFTNESS("SPEED", 9600, true, 0, false, "LONG_SPEED"),
   STRONG_SWIFTNESS("SPEED", 1800, false, 1, true, "STRONG_SPEED"),
   TURTLE_MASTER("TURTLE_MASTER", 400, false, 0, false),
   LONG_TURTLE_MASTER("TURTLE_MASTER", 800, true, 0, false),
   STRONG_TURTLE_MASTER("TURTLE_MASTER", 400, false, 1, true),
   WATER_BREATHING("WATER_BREATHING", 3600, false, 0, false),
   LONG_WATER_BREATHING("WATER_BREATHING", 9600, true, 0, false),
   WEAKNESS("WEAKNESS", 1800, false, 0, false),
   LONG_WEAKNESS("WEAKNESS", 4800, true, 0, false),
   INFESTED("INFESTED", 3600, false, 0, false),
   OOZING("OOZING", 3600, false, 0, false),
   WEAVING("WEAVING", 3600, false, 0, false),
   WIND_CHARGED("WIND_CHARGED", 3600, false, 0, false);

   private final String type;
   private final int ticks;
   private final boolean extended;
   private final int level;
   private final boolean upgraded;
   private final String[] aliases;
   public static final EnumSet<PotionTypes> VALUES = EnumSet.allOf(PotionTypes.class);

   private PotionTypes(String nullxx, int nullxxx, boolean nullxxxx, int nullxxxxx, boolean nullxxxxxx, String... nullxxxxxxx) {
      this.type = nullxx;
      this.ticks = nullxxx;
      this.extended = nullxxxx;
      this.level = nullxxxxx;
      this.upgraded = nullxxxxxx;
      this.aliases = nullxxxxxxx;
   }

   private PotionTypes(String nullxx, String nullxxx, int nullxxxx, boolean nullxxxxx, int nullxxxxxx, boolean nullxxxxxxx, String... nullxxxxxxxx) {
      this.type = ServerInfo.supportsComponents() ? nullxx : nullxxx;
      this.ticks = nullxxxx;
      this.extended = nullxxxxx;
      this.level = nullxxxxxx;
      this.upgraded = nullxxxxxxx;
      this.aliases = nullxxxxxxxx;
   }

   public static Optional<PotionTypes> matchPotionType(String var0) {
      var0 = var0.replace(" ", "_").toUpperCase(Locale.ENGLISH);

      for (PotionTypes var2 : VALUES) {
         if (var2.name().equalsIgnoreCase(var0) || var2.anyMatchAliases(var0)) {
            return Optional.of(var2);
         }
      }

      return Optional.empty();
   }

   public static Optional<PotionTypes> getFromType(String var0) {
      var0 = var0.replace(" ", "_").toUpperCase(Locale.ENGLISH);

      for (PotionTypes var2 : VALUES) {
         if (var2.type.equalsIgnoreCase(var0) || var2.anyMatchAliases(var0)) {
            return Optional.of(var2);
         }
      }

      return Optional.empty();
   }

   public static PotionTypes matchPotionType(PotionEffect var0) {
      for (PotionTypes var2 : VALUES) {
         if (var2.isSupported()
            && var2.hasEffect()
            && var2.parsePotionEffectType().equals(var0.getType())
            && var2.ticks == var0.getDuration()
            && var2.level == var0.getAmplifier()) {
            return var2;
         }
      }

      return WATER;
   }

   public static PotionTypes matchPotionType(PotionType var0) {
      for (PotionTypes var2 : VALUES) {
         if (var2.isSupported() && var2.parsePotionType().equals(var0)) {
            return var2;
         }
      }

      return WATER;
   }

   public static Optional<PotionTypes> matchPotionDataLegacy(PotionType var0, boolean var1, boolean var2) {
      for (PotionTypes var4 : VALUES) {
         if (var4.isSupported() && var4.parsePotionType().equals(var0) && var4.extended == var1 && var4.upgraded == var2) {
            return Optional.of(var4);
         }
      }

      return Optional.empty();
   }

   public static Optional<PotionTypes> matchPotionData(PotionType var0) {
      for (PotionTypes var2 : VALUES) {
         if (var2.isSupported() && var2.name().equals(var0.name())) {
            return Optional.of(var2);
         }
      }

      return Optional.empty();
   }

   public List<String> getNames(ItemStack var1) {
      HashSet<String> var2 = new HashSet<>();
      var2.add(XMaterial.matchXMaterial(var1).name() + " of " + this.name());

      for (String var6 : this.aliases) {
         if (!this.name().contains(var6) && !var2.stream().anyMatch(var1x -> var1x.contains(var6)) && var6.length() >= 4) {
            var2.add(XMaterial.matchXMaterial(var1).name() + " of " + var6);
         }
      }

      return var2.stream().map(var0 -> OnyxShop.getInstance().getMaterialName(var0)).collect(Collectors.toList());
   }

   public static boolean canHaveEffects(Material var0) {
      return XMaterial.matchXMaterial(var0).equals(XMaterial.POTION)
         || XMaterial.matchXMaterial(var0).equals(XMaterial.LINGERING_POTION)
         || XMaterial.matchXMaterial(var0).equals(XMaterial.SPLASH_POTION)
         || XMaterial.matchXMaterial(var0).equals(XMaterial.TIPPED_ARROW);
   }

   public static boolean canHaveEffects(ItemStack var0) {
      switch (XMaterial.matchXMaterial(var0)) {
         case POTION:
         case LINGERING_POTION:
         case SPLASH_POTION:
            return true;
         default:
            return false;
      }
   }

   public PotionEffectType parsePotionEffectType() {
      return this.hasEffect() ? this.parsePotionType().getEffectType() : null;
   }

   public PotionType parsePotionType() {
      return PotionType.valueOf(ServerInfo.supportsComponents() ? this.name() : this.type);
   }

   public PotionEffect parsePotionEffect() {
      return this.hasEffect() ? new PotionEffect(this.parsePotionEffectType(), this.ticks, this.level, false) : null;
   }

   public static List<PotionTypes> getPotionTypes(ItemStack var0) {
      ArrayList var1 = new ArrayList();
      if (var0 == null) {
         return var1;
      } else {
         PotionMeta var2 = (PotionMeta)var0.getItemMeta();
         if (var2 == null) {
            return var1;
         } else {
            if (OnyxShop.getInstance().version >= 19) {
               Optional var3 = OnyxShop.getInstance().versionHandler.getBasePotion(var0);
               if (var3.isPresent()) {
                  var1.add((PotionTypes)var3.get());
               }

               for (PotionEffect var5 : var2.getCustomEffects()) {
                  var1.add(matchPotionType(var5));
               }
            } else {
               for (PotionEffect var7 : var2.getCustomEffects()) {
                  var1.add(matchPotionType(var7));
               }
            }

            return var1;
         }
      }
   }

   public boolean isExtended() {
      return this.extended;
   }

   public int getDuration() {
      return this.ticks;
   }

   public int getAmplifier() {
      return this.level;
   }

   public boolean isUpgraded() {
      return this.upgraded;
   }

   public boolean isSupported() {
      try {
         return this.parsePotionType() != null || !this.hasEffect();
      } catch (Exception var2) {
         return false;
      }
   }

   private boolean anyMatchAliases(String var1) {
      for (String var5 : this.aliases) {
         if (var1.equals(var5) || var1.equals(StringUtils.remove(var5, '_'))) {
            return true;
         }
      }

      return false;
   }

   public static List<String> getNames() {
      ArrayList var0 = new ArrayList();

      for (PotionTypes var4 : values()) {
         if (var4.isSupported()) {
            var0.add(var4.type);
         }
      }

      return var0;
   }

   public ItemStack addEffect(ItemStack var1, boolean var2) {
      if (OnyxShop.getInstance().version != 18) {
         if (OnyxShop.getInstance().versionHandler != null) {
            var1 = OnyxShop.getInstance().versionHandler.setPotionType(var1, this);
         } else {
            PotionMeta var3 = (PotionMeta)var1.getItemMeta();
            if (ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_20_R2) && var3.getBasePotionType().getEffectType() == null) {
               var3.setBasePotionType(PotionType.valueOf(this.name()));
            } else {
               if (!this.hasEffect()) {
                  return var1;
               }

               var3.addCustomEffect(new PotionEffect(this.parsePotionEffectType(), this.getDuration(), this.getAmplifier(), false, true, true), false);
            }

            var1.setItemMeta(var3);
         }
      } else if (this.hasEffect()) {
         this.createOldPotion(var1, var2);
      }

      return var1;
   }

   private ItemStack createOldPotion(ItemStack var1, boolean var2) {
      PotionMeta var3 = (PotionMeta)var1.getItemMeta();
      if (!this.hasEffect()) {
         return var1;
      } else {
         if (var3.hasCustomEffects()) {
            var3.addCustomEffect(new PotionEffect(this.parsePotionEffectType(), this.getDuration(), this.getAmplifier()), false);
         } else {
            var3.setBasePotionType(this.parsePotionType());
            var3.addCustomEffect(new PotionEffect(this.parsePotionEffectType(), this.getDuration(), this.getAmplifier()), false);
            var3.setMainEffect(this.parsePotionEffectType());
         }

         return var1;
      }
   }

   public static ItemStack getPotion(String var0) {
      ItemStack var1 = new ItemStack(Material.POTION);
      return OnyxShop.getInstance().versionHandler.setPotionType(var1, matchPotionType(var0).get());
   }

   public ItemStack getPotion() {
      ItemStack var1 = new ItemStack(Material.POTION);
      return OnyxShop.getInstance().versionHandler.setPotionType(var1, this);
   }

   public static List<PotionTypes> getSupportedPotions() {
      ArrayList var0 = new ArrayList();

      for (PotionTypes var4 : values()) {
         if (var4.isSupported()) {
            var0.add(var4);
         }
      }

      return var0;
   }

   public boolean hasEffect() {
      return this.parsePotionType().getEffectType() != null;
   }
}
