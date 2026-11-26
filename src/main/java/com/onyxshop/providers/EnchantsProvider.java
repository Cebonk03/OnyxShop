package com.onyxshop.providers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.methodes.UpdateChecker;
import com.onyxshop.objects.DisplayItem;
import com.onyxshop.providers.enchantments.SimpleEnchantment;
import com.onyxshop.providers.enchantments.hooks.AdvancedEnchantmentsHook;
import com.onyxshop.providers.enchantments.hooks.CrazyEnchantmentsHook;
import com.onyxshop.providers.enchantments.hooks.EcoEnchantsHook;
import com.onyxshop.providers.enchantments.hooks.ExcellentEnchantsHook;
import com.onyxshop.providers.enchantments.hooks.VanillaEnchant;
import com.onyxshop.util.ServerInfo;
import com.onyxshop.util.exceptions.EnchantHookLoadException;
import com.onyxshop.util.exceptions.ItemLoadException;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.jetbrains.annotations.Nullable;

public class EnchantsProvider {
   private static final Map<EnchantsProvider.Provider, EnchantProvider> PROVIDERS = new HashMap<>();
   private final Pattern FORMAT_PATTERN = Pattern.compile("\\d+|\\W+");

   public EnchantsProvider() {
   }

   public void init() {
      for (EnchantsProvider.Provider var4 : EnchantsProvider.Provider.values()) {
         try {
            if (var4 == EnchantsProvider.Provider.VANILLA || Bukkit.getPluginManager().getPlugin(var4.name) != null) {
               PROVIDERS.put(var4, this.initProvider(var4));
               if (var4 != EnchantsProvider.Provider.VANILLA) {
                  SendMessage.infoMessage(Lang.ENABLED_PLUGIN_HOOK.get().replace("%plugin%", var4.name));
               }
            }
         } catch (EnchantHookLoadException var6) {
            SendMessage.warnMessage("Failed to hook enchantments hook '" + var4.name + "' with reason:\n" + var6.getMessage());
         }
      }
   }

   private EnchantProvider initProvider(EnchantsProvider.Provider var1) throws EnchantHookLoadException {
      switch (var1) {
         case ECO_ENCHANTS:
            UpdateChecker.Version var2 = new UpdateChecker.Version(Bukkit.getPluginManager().getPlugin(var1.name).getDescription().getVersion());
            if (!var2.isGreater(new UpdateChecker.Version("11.0.0")) && !var2.isSame(new UpdateChecker.Version("11.0.0"))) {
               try {
                  Class var8 = Class.forName("com.onyxshop.providers.enchantments.hooks.legacy.EcoEnchantsHook_v10");
                  return (EnchantProvider)var8.newInstance();
               } catch (IllegalAccessException | InstantiationException | ClassNotFoundException var7) {
                  var7.printStackTrace();
                  throw new EnchantHookLoadException("Failed to hook into EcoEnchantsHook_v10");
               }
            }

            return new EcoEnchantsHook();
         case EXCELLENT_ENCHANTS:
            UpdateChecker.Version var3 = new UpdateChecker.Version(Bukkit.getPluginManager().getPlugin(var1.name).getDescription().getVersion());
            if (!var3.isGreater(new UpdateChecker.Version("5.0.0")) && !var3.isSame(new UpdateChecker.Version("5.0.0"))) {
               String var4 = "ExcelentEnchantsHookV3";
               if (var3.isGreater(new UpdateChecker.Version("4.2.0")) || var3.isSame(new UpdateChecker.Version("4.2.0"))) {
                  var4 = "ExcelentEnchantsHookV3";
               } else if (!var3.isGreater(new UpdateChecker.Version("4.1.0")) && !var3.isSame(new UpdateChecker.Version("4.1.0"))) {
                  var4 = "ExcellentEnchantsHookV1";
               } else {
                  var4 = "ExcellentEnchantsHookV2";
               }

               try {
                  Class var5 = Class.forName("com.onyxshop.providers.enchantments.hooks.legacy." + var4);
                  return (EnchantProvider)var5.newInstance();
               } catch (IllegalAccessException | InstantiationException | ClassNotFoundException var6) {
                  var6.printStackTrace();
                  throw new EnchantHookLoadException("Failed to initialize enchant hook '" + var4 + "'");
               }
            } else {
               return new ExcellentEnchantsHook(var3);
            }
         case CRAZY_ENCHANTS:
            return new CrazyEnchantmentsHook();
         case ADVANCED_ENCHANTS:
            return new AdvancedEnchantmentsHook();
         case VANILLA:
            return new VanillaEnchant();
         default:
            return null;
      }
   }

   public SimpleEnchantment getEnchant(String var1) throws ItemLoadException {
      EnchantsProvider.Provider var2 = EnchantsProvider.Provider.byKey(var1);
      if (PROVIDERS.containsKey(var2)) {
         String var3 = this.format(var2 == EnchantsProvider.Provider.VANILLA ? (var1.contains(":") ? var1.split(":")[0] : var1) : var1.split(":")[1]);
         int var4 = this.getEnchantStrength(var2 == EnchantsProvider.Provider.VANILLA ? var1 : var1.split(":", 2)[1]);
         return PROVIDERS.get(var2).getEnchant(var3, var4);
      } else {
         throw new ItemLoadException(Lang.UNKNOWN_THIRD_PARTY_ENCHANTMENT.get().replace("%id%", var1).replace("%plugin%", var2.name));
      }
   }

   private String format(@Nonnull String var1) {
      return this.FORMAT_PATTERN.matcher(var1.trim().replace('-', '_').replace(' ', '_')).replaceAll("").toLowerCase(Locale.ENGLISH);
   }

   private int getEnchantStrength(String var1) {
      int var2;
      try {
         var2 = var1.contains(":") ? Integer.parseInt(var1.split(":")[1]) : 1;
      } catch (NumberFormatException var4) {
         var2 = 1;
         SendMessage.logDebugMessage(Lang.ITEM_ENCHANTMENT_STRENGTH_NULL.get());
      }

      return var2;
   }

   public static boolean canEnchant(ItemStack var0, SimpleEnchantment var1) {
      return PROVIDERS.get(var1.getProvider()).canEnchantItem(var0, var1);
   }

   public static ItemStack enchantItem(ItemStack var0, SimpleEnchantment var1, @Nullable DisplayItem var2) {
      return PROVIDERS.get(var1.getProvider()).addEnchantment(var0, var1, var2);
   }

   public List<String> getEnchantKeys(ItemStack var1) {
      ArrayList var2 = new ArrayList();
      if (PROVIDERS.containsKey(EnchantsProvider.Provider.ADVANCED_ENCHANTS)) {
         var2.addAll(((AdvancedEnchantmentsHook)PROVIDERS.get(EnchantsProvider.Provider.ADVANCED_ENCHANTS)).getAppliedEnchants(var1));
      }

      if (PROVIDERS.containsKey(EnchantsProvider.Provider.CRAZY_ENCHANTS)) {
         var2.addAll(((CrazyEnchantmentsHook)PROVIDERS.get(EnchantsProvider.Provider.CRAZY_ENCHANTS)).getAppliedEnchants(var1));
      }

      if (!var1.getEnchantments().isEmpty() || var1.getType() == Material.ENCHANTED_BOOK) {
         Map<Enchantment, Integer> var3 = var1.getType() == Material.ENCHANTED_BOOK
            ? ((EnchantmentStorageMeta)var1.getItemMeta()).getStoredEnchants()
            : var1.getEnchantments();

         for (Enchantment var5 : var3.keySet()) {
            for (EnchantsProvider.Provider var7 : Arrays.asList(
               EnchantsProvider.Provider.ECO_ENCHANTS, EnchantsProvider.Provider.EXCELLENT_ENCHANTS, EnchantsProvider.Provider.VANILLA
            )) {
               if (PROVIDERS.containsKey(var7)
                  && ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_13_R1)
                  && PROVIDERS.get(var7).isEnchant(var5.getKey())) {
                  var2.add(var7.key + var5.getKey().getKey() + ":" + var3.get(var5));
               }
            }
         }
      }

      return var2;
   }

   public List<SimpleEnchantment> getEnchantsOfItem(ItemStack var1) {
      ArrayList var2 = new ArrayList();
      if (PROVIDERS.containsKey(EnchantsProvider.Provider.ADVANCED_ENCHANTS)) {
         var2.addAll(((AdvancedEnchantmentsHook)PROVIDERS.get(EnchantsProvider.Provider.ADVANCED_ENCHANTS)).getEnchantsOfItem(var1));
      }

      if (PROVIDERS.containsKey(EnchantsProvider.Provider.CRAZY_ENCHANTS)) {
         var2.addAll(((CrazyEnchantmentsHook)PROVIDERS.get(EnchantsProvider.Provider.CRAZY_ENCHANTS)).getEnchantsOfItem(var1));
      }

      if (!var1.getEnchantments().isEmpty() || var1.getType() == Material.ENCHANTED_BOOK) {
         for (Entry var4 : (var1.getType() == Material.ENCHANTED_BOOK
               ? ((EnchantmentStorageMeta)var1.getItemMeta()).getStoredEnchants()
               : var1.getEnchantments())
            .entrySet()) {
            EnchantsProvider.Provider var5 = this.getEnchantProvider((Enchantment)var4.getKey());
            if (var5 == null) {
               SendMessage.warnMessage(
                  "Failed to determine enchantment owner for '"
                     + ((Enchantment)var4.getKey()).getKey().toString()
                     + "', this is most likely a bug, report it to the plugin Author!"
               );
            } else {
               var2.add(new SimpleEnchantment(var5, var4.getKey(), ((Enchantment)var4.getKey()).getKey().getKey(), (Integer)var4.getValue()));
            }
         }
      }

      return var2;
   }

   private EnchantsProvider.Provider getEnchantProvider(Enchantment var1) {
      for (EnchantsProvider.Provider var3 : Arrays.asList(
         EnchantsProvider.Provider.ECO_ENCHANTS, EnchantsProvider.Provider.EXCELLENT_ENCHANTS, EnchantsProvider.Provider.VANILLA
      )) {
         if (PROVIDERS.containsKey(var3) && PROVIDERS.get(var3).isEnchant(var1.getKey())) {
            return var3;
         }
      }

      return null;
   }

   public List<String> getEnchants(ItemStack var1) {
      return this.getEnchantKeys(var1).stream().map(var0 -> var0.substring(0, var0.lastIndexOf(":"))).collect(Collectors.toList());
   }

   public List<String> getAvailableEnchantKeys() {
      ArrayList var1 = new ArrayList();

      for (EnchantProvider var3 : PROVIDERS.values()) {
         var1.addAll(var3.getAvailableEnchantKeys());
      }

      return var1;
   }

   public List<SimpleEnchantment> getAvailableEnchants() {
      ArrayList var1 = new ArrayList();

      for (EnchantProvider var3 : PROVIDERS.values()) {
         var1.addAll(var3.getAvailableEnchants());
      }

      return var1;
   }

   public List<Object> getAvailableEnchants(ItemStack var1) {
      ArrayList var2 = new ArrayList();

      for (EnchantProvider var4 : PROVIDERS.values()) {
         var2.addAll(var4.getAvailableEnchants(var1));
      }

      return var2;
   }

   public static ItemStack getBook(SimpleEnchantment var0) {
      return PROVIDERS.get(var0.getProvider()).getBook(var0);
   }

   public static String getName(SimpleEnchantment var0) {
      return PROVIDERS.get(var0.getProvider()).getName(var0);
   }

   public static enum Provider {
      ECO_ENCHANTS("ecoenchants:", "EcoEnchants"),
      EXCELLENT_ENCHANTS("excellentenchants:", "ExcellentEnchants"),
      CRAZY_ENCHANTS("crazyenchantments:", "CrazyEnchantments"),
      ADVANCED_ENCHANTS("advancedenchantments:", "AdvancedEnchantments"),
      VANILLA("", "Minecraft");

      final String key;
      final String name;

      private Provider(String nullxx, String nullxxx) {
         this.key = nullxx;
         this.name = nullxxx;
      }

      public static EnchantsProvider.Provider byKey(String var0) {
         for (EnchantsProvider.Provider var4 : values()) {
            if (var0.toLowerCase().startsWith(var4.key)) {
               return var4;
            }
         }

         return VANILLA;
      }
   }
}
