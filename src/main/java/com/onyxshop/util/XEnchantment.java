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

   private XEnchantment(String... nullxx) {
      this(false, nullxx);
   }

   private XEnchantment(boolean nullxx, String... nullxxx) {
      this.self = nullxx;
      this.aliases = nullxxx;
   }

   private XEnchantment(boolean nullxx, boolean nullxxx, String... nullxxxx) {
      this.self = ServerInfo.supportsComponents();
      this.aliases = nullxxxx;
   }

   public static boolean isSmiteEffectiveAgainst(EntityType var0) {
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
         .contains(var0);
   }

   public static boolean isArthropodsEffectiveAgainst(EntityType var0) {
      if (Arrays.asList(EntityType.SPIDER, EntityType.CAVE_SPIDER, EntityType.SILVERFISH, EntityType.ENDERMITE).contains(var0)) {
         return true;
      } else {
         return Enums.getIfPresent(EntityType.class, "BEE").isPresent() ? var0 == EntityType.BEE : false;
      }
   }

   @Nonnull
   private static String format(@Nonnull String var0) {
      return FORMAT_PATTERN.matcher(var0.trim().replace('-', '_').replace(' ', '_')).replaceAll("").toUpperCase(Locale.ENGLISH);
   }

   @Nonnull
   public static Optional<XEnchantment> matchXEnchantment(@Nonnull String var0) {
      Validate.notEmpty(var0, "Enchantment name cannot be null or empty");
      var0 = format(var0);

      for (XEnchantment var2 : VALUES) {
         if (var2.name().equals(var0) || var2.anyMatchAliases(var0)) {
            return Optional.of(var2);
         }
      }

      return Optional.empty();
   }

   public static XEnchantment matchXEnchantment(@Nonnull Enchantment var0) {
      Objects.requireNonNull(var0, "Cannot parse XEnchantment of a null enchantment");

      try {
         return ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_20_R4) ? matchCorrectEnch(var0) : valueOf(var0.getName());
      } catch (IllegalArgumentException var2) {
         SendMessage.errorMessage("Could not find enchantment '" + var0.getName() + "'");
         return null;
      }
   }

   private static XEnchantment matchCorrectEnch(@Nonnull Enchantment var0) {
      String var1 = var0.getKey().getKey().toUpperCase(Locale.ROOT);

      for (XEnchantment var3 : VALUES) {
         if (!var3.self ? var3.aliases[0].equals(var1) : var3.name().equals(var1)) {
            return var3;
         }
      }

      return null;
   }

   @Nonnull
   public static ItemStack addEnchantFromString(ItemStack var0, String var1) {
      Objects.requireNonNull(var0, "Cannot add enchantment to null ItemStack");
      if (!Strings.isNullOrEmpty(var1) && !var1.equalsIgnoreCase("none")) {
         String[] var2 = StringUtils.split(StringUtils.deleteWhitespace(var1), ',');
         if (var2.length == 0) {
            var2 = StringUtils.split(var1, ' ');
         }

         Optional var3 = matchXEnchantment(var2[0]);
         if (var3.isPresent()) {
            return var0;
         } else {
            Enchantment var4 = ((XEnchantment)var3.get()).parseEnchantment();
            if (var4 == null) {
               return null;
            } else {
               int var5 = 1;

               try {
                  if (var2.length > 1) {
                     var5 = Integer.parseInt(var2[1]);
                  }
               } catch (NumberFormatException var7) {
               }

               var0.addUnsafeEnchantment(var4, var5);
               return var0;
            }
         }
      } else {
         return var0;
      }
   }

   @Nonnull
   public ItemStack getBook(int var1) {
      ItemStack var2 = new ItemStack(Material.ENCHANTED_BOOK);
      EnchantmentStorageMeta var3 = (EnchantmentStorageMeta)var2.getItemMeta();
      var3.addStoredEnchant(this.parseEnchantment(), var1, true);
      var2.setItemMeta(var3);
      return var2;
   }

   public List<String> getNames(ItemStack var1) {
      HashSet<String> var2 = new HashSet<>();
      var2.add(Material.ENCHANTED_BOOK.name() + " of " + this.name());

      for (String var6 : this.aliases) {
         if (!this.name().contains(var6) && !var2.stream().anyMatch(var1x -> var1x.contains(var6)) && var6.length() >= 4) {
            var2.add(Material.ENCHANTED_BOOK.name() + " of " + var6);
         }
      }

      return var2.stream().map(var0 -> OnyxShop.getInstance().getMaterialName(var0)).collect(Collectors.toList());
   }

   private boolean anyMatchAliases(String var1) {
      for (String var5 : this.aliases) {
         if (var1.equals(var5) || var1.equals(StringUtils.remove(var5, '_'))) {
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
      ArrayList var0 = new ArrayList();

      for (XEnchantment var4 : values()) {
         if (var4.isSupported()) {
            var0.add(var4.getVanillaName() + ":1");
         }
      }

      return var0;
   }

   static {
      boolean var0;
      try {
         Class var1 = Class.forName("org.bukkit.NamespacedKey");
         Class var2 = Class.forName("org.bukkit.enchantments.Enchantment");
         var2.getDeclaredMethod("getByKey", var1);
         var0 = true;
      } catch (NoSuchMethodException | ClassNotFoundException var3) {
         var0 = false;
      }

      ISFLAT = var0;
   }
}
