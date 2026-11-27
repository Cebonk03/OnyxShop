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

   private PotionTypes(String text, int value, boolean flag, int value2, boolean flag2, String... values) {
      this.type = text;
      this.ticks = value;
      this.extended = flag;
      this.level = value2;
      this.upgraded = flag2;
      this.aliases = values;
   }

   private PotionTypes(String text, String text2, int value, boolean flag, int value2, boolean flag2, String... values) {
      this.type = ServerInfo.supportsComponents() ? text : text2;
      this.ticks = value;
      this.extended = flag;
      this.level = value2;
      this.upgraded = flag2;
      this.aliases = values;
   }

   public static Optional<PotionTypes> matchPotionType(String type) {
      type = type.replace(" ", "_").toUpperCase(Locale.ENGLISH);

      for (PotionTypes potionTypes : VALUES) {
         if (potionTypes.name().equalsIgnoreCase(type) || potionTypes.anyMatchAliases(type)) {
            return Optional.of(potionTypes);
         }
      }

      return Optional.empty();
   }

   public static Optional<PotionTypes> getFromType(String type) {
      type = type.replace(" ", "_").toUpperCase(Locale.ENGLISH);

      for (PotionTypes potionTypes : VALUES) {
         if (potionTypes.type.equalsIgnoreCase(type) || potionTypes.anyMatchAliases(type)) {
            return Optional.of(potionTypes);
         }
      }

      return Optional.empty();
   }

   public static PotionTypes matchPotionType(PotionEffect potionEffect) {
      for (PotionTypes potionTypes : VALUES) {
         if (potionTypes.isSupported()
            && potionTypes.hasEffect()
            && potionTypes.parsePotionEffectType().equals(potionEffect.getType())
            && potionTypes.ticks == potionEffect.getDuration()
            && potionTypes.level == potionEffect.getAmplifier()) {
            return potionTypes;
         }
      }

      return WATER;
   }

   public static PotionTypes matchPotionType(PotionType potionType) {
      for (PotionTypes potionTypes : VALUES) {
         if (potionTypes.isSupported() && potionTypes.parsePotionType().equals(potionType)) {
            return potionTypes;
         }
      }

      return WATER;
   }

   public static Optional<PotionTypes> matchPotionDataLegacy(PotionType potionType, boolean flag, boolean flag2) {
      for (PotionTypes potionTypes : VALUES) {
         if (potionTypes.isSupported() && potionTypes.parsePotionType().equals(potionType) && potionTypes.extended == flag && potionTypes.upgraded == flag2) {
            return Optional.of(potionTypes);
         }
      }

      return Optional.empty();
   }

   public static Optional<PotionTypes> matchPotionData(PotionType potionType) {
      for (PotionTypes potionTypes : VALUES) {
         if (potionTypes.isSupported() && potionTypes.name().equals(potionType.name())) {
            return Optional.of(potionTypes);
         }
      }

      return Optional.empty();
   }

   public List<String> getNames(ItemStack itemStack) {
      HashSet<String> values = new HashSet<>();
      values.add(XMaterial.matchXMaterial(itemStack).name() + " of " + this.name());

      for (String text : this.aliases) {
         if (!this.name().contains(text) && !values.stream().anyMatch(element -> element.contains(text)) && text.length() >= 4) {
            values.add(XMaterial.matchXMaterial(itemStack).name() + " of " + text);
         }
      }

      return values.stream().map(element -> OnyxShop.getInstance().getMaterialName(element)).collect(Collectors.toList());
   }

   public static boolean canHaveEffects(Material material) {
      return XMaterial.matchXMaterial(material).equals(XMaterial.POTION)
         || XMaterial.matchXMaterial(material).equals(XMaterial.LINGERING_POTION)
         || XMaterial.matchXMaterial(material).equals(XMaterial.SPLASH_POTION)
         || XMaterial.matchXMaterial(material).equals(XMaterial.TIPPED_ARROW);
   }

   public static boolean canHaveEffects(ItemStack itemStack) {
      switch (XMaterial.matchXMaterial(itemStack)) {
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

   public static List<PotionTypes> getPotionTypes(ItemStack itemStack) {
      ArrayList values = new ArrayList();
      if (itemStack == null) {
         return values;
      } else {
         PotionMeta potionMeta = (PotionMeta)itemStack.getItemMeta();
         if (potionMeta == null) {
            return values;
         } else {
            if (OnyxShop.getInstance().version >= 19) {
               Optional optional = OnyxShop.getInstance().versionHandler.getBasePotion(itemStack);
               if (optional.isPresent()) {
                  values.add((PotionTypes)optional.get());
               }

               for (PotionEffect potionEffect : potionMeta.getCustomEffects()) {
                  values.add(matchPotionType(potionEffect));
               }
            } else {
               for (PotionEffect potionEffect : potionMeta.getCustomEffects()) {
                  values.add(matchPotionType(potionEffect));
               }
            }

            return values;
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
      } catch (Exception exception) {
         return false;
      }
   }

   private boolean anyMatchAliases(String text) {
      for (String text2 : this.aliases) {
         if (text.equals(text2) || text.equals(StringUtils.remove(text2, '_'))) {
            return true;
         }
      }

      return false;
   }

   public static List<String> getNames() {
      ArrayList values = new ArrayList();

      for (PotionTypes potionTypes : values()) {
         if (potionTypes.isSupported()) {
            values.add(potionTypes.type);
         }
      }

      return values;
   }

   public ItemStack addEffect(ItemStack itemStack, boolean flag) {
      if (OnyxShop.getInstance().version != 18) {
         if (OnyxShop.getInstance().versionHandler != null) {
            itemStack = OnyxShop.getInstance().versionHandler.setPotionType(itemStack, this);
         } else {
            PotionMeta potionMeta = (PotionMeta)itemStack.getItemMeta();
            if (ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_20_R2) && potionMeta.getBasePotionType().getEffectType() == null) {
               potionMeta.setBasePotionType(PotionType.valueOf(this.name()));
            } else {
               if (!this.hasEffect()) {
                  return itemStack;
               }

               potionMeta.addCustomEffect(new PotionEffect(this.parsePotionEffectType(), this.getDuration(), this.getAmplifier(), false, true, true), false);
            }

            itemStack.setItemMeta(potionMeta);
         }
      } else if (this.hasEffect()) {
         this.createOldPotion(itemStack, flag);
      }

      return itemStack;
   }

   private ItemStack createOldPotion(ItemStack itemStack, boolean flag) {
      PotionMeta potionMeta = (PotionMeta)itemStack.getItemMeta();
      if (!this.hasEffect()) {
         return itemStack;
      } else {
         if (potionMeta.hasCustomEffects()) {
            potionMeta.addCustomEffect(new PotionEffect(this.parsePotionEffectType(), this.getDuration(), this.getAmplifier()), false);
         } else {
            potionMeta.setBasePotionType(this.parsePotionType());
            potionMeta.addCustomEffect(new PotionEffect(this.parsePotionEffectType(), this.getDuration(), this.getAmplifier()), false);
            potionMeta.setMainEffect(this.parsePotionEffectType());
         }

         return itemStack;
      }
   }

   public static ItemStack getPotion(String itemId) {
      ItemStack itemStack = new ItemStack(Material.POTION);
      return OnyxShop.getInstance().versionHandler.setPotionType(itemStack, matchPotionType(itemId).get());
   }

   public ItemStack getPotion() {
      ItemStack itemStack = new ItemStack(Material.POTION);
      return OnyxShop.getInstance().versionHandler.setPotionType(itemStack, this);
   }

   public static List<PotionTypes> getSupportedPotions() {
      ArrayList values = new ArrayList();

      for (PotionTypes potionTypes : values()) {
         if (potionTypes.isSupported()) {
            values.add(potionTypes);
         }
      }

      return values;
   }

   public boolean hasEffect() {
      return this.parsePotionType().getEffectType() != null;
   }
}
