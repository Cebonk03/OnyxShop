package com.onyxshop.util;

import com.google.common.base.Enums;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import com.onyxshop.OnyxShop;
import com.onyxshop.methodes.SendMessage;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.jetbrains.annotations.Nullable;

public enum XEnchantment {
   ARROW_DAMAGE("POWER", "ARROW_DAMAGE", "ARROW_POWER", "AD"),
   ARROW_FIRE("FLAME", "FLAME_ARROW", "FIRE_ARROW", "AF"),
   ARROW_INFINITE("INFINITY", "INF_ARROWS", "INFINITE_ARROWS", "INFINITE", "UNLIMITED", "UNLIMITED_ARROWS", "AI"),
   ARROW_KNOCKBACK("PUNCH", "ARROW_KNOCKBACK", "ARROWKB", "ARROW_PUNCH", "AK"),
   BINDING_CURSE(true, "BINDING_CURSE", "BIND_CURSE", "BINDING", "BIND"),
   CHANNELING(true, "CHANNELLING", "CHANELLING", "CHANELING", "CHANNEL"),
   DAMAGE_ALL("SHARPNESS", "ALL_DAMAGE", "ALL_DMG", "SHARP", "DAL"),
   DAMAGE_ARTHROPODS("BANE_OF_ARTHROPODS", "ARDMG", "BANE_OF_ARTHROPOD", "ARTHROPOD", "DAR"),
   DAMAGE_UNDEAD("SMITE", "UNDEAD_DAMAGE", "DU"),
   DEPTH_STRIDER(true, "DEPTH", "STRIDER"),
   DIG_SPEED("EFFICIENCY", "MINE_SPEED", "CUT_SPEED", "DS", "EFF"),
   DURABILITY("UNBREAKING", "DURA"),
   FIRE_ASPECT(true, "FIRE", "MELEE_FIRE", "MELEE_FLAME", "FA"),
   FROST_WALKER(true, "FROST", "WALKER"),
   IMPALING(true, "IMPALE", "OCEAN_DAMAGE", "OCEAN_DMG"),
   SOUL_SPEED(true, "SPEED_SOUL", "SOUL_RUNNER"),
   SWIFT_SNEAK(true, "SPEED_SNEAK"),
   KNOCKBACK(true, "K_BACK", "KB"),
   LOOT_BONUS_BLOCKS("FORTUNE", "BLOCKS_LOOT_BONUS", "FORT", "LBB"),
   LOOT_BONUS_MOBS("LOOTING", "MOB_LOOT", "MOBS_LOOT_BONUS", "LBM"),
   LOYALTY(true, "LOYAL", "RETURN"),
   LUCK("LUCK_OF_THE_SEA", "LUCK_OF_SEA", "LUCK_OF_SEAS", "ROD_LUCK"),
   LURE(true, "ROD_LURE"),
   MENDING(true),
   MULTISHOT(true, "TRIPLE_SHOT"),
   OXYGEN("RESPIRATION", "BREATH", "BREATHING", "O2", "O"),
   PIERCING(true),
   PROTECTION_ENVIRONMENTAL("PROTECTION", "PROTECT", "PROT"),
   PROTECTION_EXPLOSIONS("BLAST_PROTECTION", "BLAST_PROTECT", "EXPLOSIONS_PROTECTION", "EXPLOSION_PROTECTION", "BLAST_PROTECTION", "PE"),
   PROTECTION_FALL("FEATHER_FALLING", "FALL_PROT", "FEATHER_FALL", "FALL_PROTECTION", "FEATHER_FALLING", "PFA"),
   PROTECTION_FIRE("FIRE_PROTECTION", "FIRE_PROT", "FIRE_PROTECT", "FIRE_PROTECTION", "FLAME_PROTECTION", "FLAME_PROTECT", "FLAME_PROT", "PF"),
   PROTECTION_PROJECTILE("PROJECTILE_PROTECTION", "PROJECTILE_PROTECTION", "PROJ_PROT", "PP"),
   QUICK_CHARGE(true, "QUICKCHARGE", "QUICK_DRAW", "FAST_CHARGE", "FAST_DRAW"),
   RIPTIDE(true, "RIP", "TIDE", "LAUNCH"),
   SILK_TOUCH(true, "SOFT_TOUCH", "ST"),
   SWEEPING_EDGE(true, false, "SWEEPING", "SWEEP_EDGE"),
   THORNS(true, "HIGHCRIT", "THORN", "HIGHERCRIT", "T"),
   VANISHING_CURSE(true, "VANISHING_CURSE", "VANISH_CURSE", "VANISHING", "VANISH"),
   WATER_WORKER("AQUA_AFFINITY", "WATER_WORKER", "AQUA_AFFINITY", "WATER_MINE", "WW"),
   BREACH(true),
   DENSITY(true),
   WIND_BURST(true),
   LUNGE(true);

   public static final EnumSet<XEnchantment> VALUES = EnumSet.allOf(XEnchantment.class);
   private static final boolean ISFLAT;
   private static final Pattern FORMAT_PATTERN = Pattern.compile("\\d+|\\W+");
   private final boolean self;
   private final String[] aliases;

   private XEnchantment(String... values) {
      this(false, values);
   }

   private XEnchantment(boolean flag, String... values) {
      this.self = flag;
      this.aliases = values;
   }

   private XEnchantment(boolean flag, boolean flag2, String... values) {
      this.self = ServerInfo.supportsComponents();
      this.aliases = values;
   }

   public static boolean isSmiteEffectiveAgainst(EntityType entityType) {
      return Arrays.asList(
            EntityType.ZOMBIE,
            EntityType.SKELETON,
            EntityType.WITHER,
            EntityType.WITHER_SKELETON,
            EntityType.SKELETON_HORSE,
            EntityType.STRAY,
            EntityType.HUSK,
            EntityType.PHANTOM,
            EntityType.DROWNED
         )
         .contains(entityType);
   }

   public static boolean isArthropodsEffectiveAgainst(EntityType entityType) {
      if (Arrays.asList(EntityType.SPIDER, EntityType.CAVE_SPIDER, EntityType.SILVERFISH, EntityType.ENDERMITE).contains(entityType)) {
         return true;
      } else {
         return Enums.getIfPresent(EntityType.class, "BEE").isPresent() ? entityType == EntityType.BEE : false;
      }
   }

   @Nonnull
   private static String format(@Nonnull String text) {
      return FORMAT_PATTERN.matcher(text.trim().replace('-', '_').replace(' ', '_')).replaceAll("").toUpperCase(Locale.ENGLISH);
   }

   @Nonnull
   public static Optional<XEnchantment> matchXEnchantment(@Nonnull String text) {
      Validate.notEmpty(text, "Enchantment name cannot be null or empty");
      text = format(text);

      for (XEnchantment xEnchantment : VALUES) {
         if (xEnchantment.name().equals(text) || xEnchantment.anyMatchAliases(text)) {
            return Optional.of(xEnchantment);
         }
      }

      return Optional.empty();
   }

   public static XEnchantment matchXEnchantment(@Nonnull Enchantment enchantment) {
      Objects.requireNonNull(enchantment, "Cannot parse XEnchantment of a null enchantment");

      try {
         return ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_20_R4) ? matchCorrectEnch(enchantment) : valueOf(enchantment.getName());
      } catch (IllegalArgumentException exception) {
         SendMessage.errorMessage("Could not find enchantment '" + enchantment.getName() + "'");
         return null;
      }
   }

   private static XEnchantment matchCorrectEnch(@Nonnull Enchantment enchantment) {
      String key = enchantment.getKey().getKey().toUpperCase(Locale.ROOT);

      for (XEnchantment xEnchantment : VALUES) {
         if (!xEnchantment.self ? xEnchantment.aliases[0].equals(key) : xEnchantment.name().equals(key)) {
            return xEnchantment;
         }
      }

      return null;
   }

   @Nonnull
   public static ItemStack addEnchantFromString(ItemStack itemStack, String itemId) {
      Objects.requireNonNull(itemStack, "Cannot add enchantment to null ItemStack");
      if (!Strings.isNullOrEmpty(itemId) && !itemId.equalsIgnoreCase("none")) {
         String[] values = StringUtils.split(StringUtils.deleteWhitespace(itemId), ',');
         if (values.length == 0) {
            values = StringUtils.split(itemId, ' ');
         }

         Optional optional = matchXEnchantment(values[0]);
         if (optional.isPresent()) {
            return itemStack;
         } else {
            Enchantment enchantment = ((XEnchantment)optional.get()).parseEnchantment();
            if (enchantment == null) {
               return null;
            } else {
               int value = 1;

               try {
                  if (values.length > 1) {
                     value = Integer.parseInt(values[1]);
                  }
               } catch (NumberFormatException exception) {
               }

               itemStack.addUnsafeEnchantment(enchantment, value);
               return itemStack;
            }
         }
      } else {
         return itemStack;
      }
   }

   @Nonnull
   public ItemStack getBook(int value) {
      ItemStack itemStack = new ItemStack(Material.ENCHANTED_BOOK);
      EnchantmentStorageMeta enchantmentStorageMeta = (EnchantmentStorageMeta)itemStack.getItemMeta();
      enchantmentStorageMeta.addStoredEnchant(this.parseEnchantment(), value, true);
      itemStack.setItemMeta(enchantmentStorageMeta);
      return itemStack;
   }

   public List<String> getNames(ItemStack itemStack) {
      HashSet<String> values = new HashSet<>();
      values.add(Material.ENCHANTED_BOOK.name() + " of " + this.name());

      for (String text : this.aliases) {
         if (!this.name().contains(text) && !values.stream().anyMatch(element -> element.contains(text)) && text.length() >= 4) {
            values.add(Material.ENCHANTED_BOOK.name() + " of " + text);
         }
      }

      return values.stream().map(element -> OnyxShop.getInstance().getMaterialName(element)).collect(Collectors.toList());
   }

   private boolean anyMatchAliases(String text) {
      for (String text2 : this.aliases) {
         if (text.equals(text2) || text.equals(StringUtils.remove(text2, '_'))) {
            return true;
         }
      }

      return false;
   }

   @Nonnull
   public String getVanillaName() {
      return this.self ? this.name() : this.aliases[0];
   }

   @Nullable
   public Enchantment parseEnchantment() {
      return ISFLAT ? Enchantment.getByKey(NamespacedKey.minecraft(this.getVanillaName().toLowerCase(Locale.ENGLISH))) : Enchantment.getByName(this.name());
   }

   public boolean isSupported() {
      return this.parseEnchantment() != null;
   }

   @Nonnull
   public String[] getAliases() {
      return this.aliases;
   }

   @Override
   public String toString() {
      return WordUtils.capitalize(this.name().replace('_', ' ').toLowerCase(Locale.ENGLISH));
   }

   public static List<String> getNames() {
      ArrayList values = new ArrayList();

      for (XEnchantment xEnchantment : values()) {
         if (xEnchantment.isSupported()) {
            values.add(xEnchantment.getVanillaName() + ":1");
         }
      }

      return values;
   }

   static {
      boolean flag;
      try {
         Class type = Class.forName("org.bukkit.NamespacedKey");
         Class type2 = Class.forName("org.bukkit.enchantments.Enchantment");
         type2.getDeclaredMethod("getByKey", type);
         flag = true;
      } catch (NoSuchMethodException | ClassNotFoundException exception) {
         flag = false;
      }

      ISFLAT = flag;
   }
}
