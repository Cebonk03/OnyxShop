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
      for (EnchantsProvider.Provider provider : EnchantsProvider.Provider.values()) {
         try {
            if (provider == EnchantsProvider.Provider.VANILLA || Bukkit.getPluginManager().getPlugin(provider.name) != null) {
               PROVIDERS.put(provider, this.initProvider(provider));
               if (provider != EnchantsProvider.Provider.VANILLA) {
                  SendMessage.infoMessage(Lang.ENABLED_PLUGIN_HOOK.get().replace("%plugin%", provider.name));
               }
            }
         } catch (EnchantHookLoadException exception) {
            SendMessage.warnMessage("Failed to hook enchantments hook '" + provider.name + "' with reason:\n" + exception.getMessage());
         }
      }
   }

   private EnchantProvider initProvider(EnchantsProvider.Provider provider) throws EnchantHookLoadException {
      switch (provider) {
         case ECO_ENCHANTS:
            UpdateChecker.Version version = new UpdateChecker.Version(Bukkit.getPluginManager().getPlugin(provider.name).getDescription().getVersion());
            if (!version.isGreater(new UpdateChecker.Version("11.0.0")) && !version.isSame(new UpdateChecker.Version("11.0.0"))) {
               try {
                  Class type = Class.forName("com.onyxshop.providers.enchantments.hooks.legacy.EcoEnchantsHook_v10");
                  return (EnchantProvider)type.newInstance();
               } catch (IllegalAccessException | InstantiationException | ClassNotFoundException exception) {
                  exception.printStackTrace();
                  throw new EnchantHookLoadException("Failed to hook into EcoEnchantsHook_v10");
               }
            }

            return new EcoEnchantsHook();
         case EXCELLENT_ENCHANTS:
            UpdateChecker.Version version2 = new UpdateChecker.Version(Bukkit.getPluginManager().getPlugin(provider.name).getDescription().getVersion());
            if (!version2.isGreater(new UpdateChecker.Version("5.0.0")) && !version2.isSame(new UpdateChecker.Version("5.0.0"))) {
               String text = "ExcelentEnchantsHookV3";
               if (version2.isGreater(new UpdateChecker.Version("4.2.0")) || version2.isSame(new UpdateChecker.Version("4.2.0"))) {
                  text = "ExcelentEnchantsHookV3";
               } else if (!version2.isGreater(new UpdateChecker.Version("4.1.0")) && !version2.isSame(new UpdateChecker.Version("4.1.0"))) {
                  text = "ExcellentEnchantsHookV1";
               } else {
                  text = "ExcellentEnchantsHookV2";
               }

               try {
                  Class type = Class.forName("com.onyxshop.providers.enchantments.hooks.legacy." + text);
                  return (EnchantProvider)type.newInstance();
               } catch (IllegalAccessException | InstantiationException | ClassNotFoundException exception) {
                  exception.printStackTrace();
                  throw new EnchantHookLoadException("Failed to initialize enchant hook '" + text + "'");
               }
            } else {
               return new ExcellentEnchantsHook(version2);
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

   public SimpleEnchantment getEnchant(String itemId) throws ItemLoadException {
      EnchantsProvider.Provider provider = EnchantsProvider.Provider.byKey(itemId);
      if (PROVIDERS.containsKey(provider)) {
         String text = this.format(provider == EnchantsProvider.Provider.VANILLA ? (itemId.contains(":") ? itemId.split(":")[0] : itemId) : itemId.split(":")[1]);
         int enchantStrength = this.getEnchantStrength(provider == EnchantsProvider.Provider.VANILLA ? itemId : itemId.split(":", 2)[1]);
         return PROVIDERS.get(provider).getEnchant(text, enchantStrength);
      } else {
         throw new ItemLoadException(Lang.UNKNOWN_THIRD_PARTY_ENCHANTMENT.get().replace("%id%", itemId).replace("%plugin%", provider.name));
      }
   }

   private String format(@Nonnull String text) {
      return this.FORMAT_PATTERN.matcher(text.trim().replace('-', '_').replace(' ', '_')).replaceAll("").toLowerCase(Locale.ENGLISH);
   }

   private int getEnchantStrength(String text) {
      int value;
      try {
         value = text.contains(":") ? Integer.parseInt(text.split(":")[1]) : 1;
      } catch (NumberFormatException exception) {
         value = 1;
         SendMessage.logDebugMessage(Lang.ITEM_ENCHANTMENT_STRENGTH_NULL.get());
      }

      return value;
   }

   public static boolean canEnchant(ItemStack itemStack, SimpleEnchantment simpleEnchantment) {
      return PROVIDERS.get(simpleEnchantment.getProvider()).canEnchantItem(itemStack, simpleEnchantment);
   }

   public static ItemStack enchantItem(ItemStack itemStack, SimpleEnchantment simpleEnchantment, @Nullable DisplayItem displayItem) {
      return PROVIDERS.get(simpleEnchantment.getProvider()).addEnchantment(itemStack, simpleEnchantment, displayItem);
   }

   public List<String> getEnchantKeys(ItemStack itemStack) {
      ArrayList values = new ArrayList();
      if (PROVIDERS.containsKey(EnchantsProvider.Provider.ADVANCED_ENCHANTS)) {
         values.addAll(((AdvancedEnchantmentsHook)PROVIDERS.get(EnchantsProvider.Provider.ADVANCED_ENCHANTS)).getAppliedEnchants(itemStack));
      }

      if (PROVIDERS.containsKey(EnchantsProvider.Provider.CRAZY_ENCHANTS)) {
         values.addAll(((CrazyEnchantmentsHook)PROVIDERS.get(EnchantsProvider.Provider.CRAZY_ENCHANTS)).getAppliedEnchants(itemStack));
      }

      if (!itemStack.getEnchantments().isEmpty() || itemStack.getType() == Material.ENCHANTED_BOOK) {
         Map<Enchantment, Integer> map = itemStack.getType() == Material.ENCHANTED_BOOK
            ? ((EnchantmentStorageMeta)itemStack.getItemMeta()).getStoredEnchants()
            : itemStack.getEnchantments();

         for (Enchantment enchantment : map.keySet()) {
            for (EnchantsProvider.Provider provider : Arrays.asList(
               EnchantsProvider.Provider.ECO_ENCHANTS, EnchantsProvider.Provider.EXCELLENT_ENCHANTS, EnchantsProvider.Provider.VANILLA
            )) {
               if (PROVIDERS.containsKey(provider)
                  && ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_13_R1)
                  && PROVIDERS.get(provider).isEnchant(enchantment.getKey())) {
                  values.add(provider.key + enchantment.getKey().getKey() + ":" + map.get(enchantment));
               }
            }
         }
      }

      return values;
   }

   public List<SimpleEnchantment> getEnchantsOfItem(ItemStack itemStack) {
      ArrayList itemIds = new ArrayList();
      if (PROVIDERS.containsKey(EnchantsProvider.Provider.ADVANCED_ENCHANTS)) {
         itemIds.addAll(((AdvancedEnchantmentsHook)PROVIDERS.get(EnchantsProvider.Provider.ADVANCED_ENCHANTS)).getEnchantsOfItem(itemStack));
      }

      if (PROVIDERS.containsKey(EnchantsProvider.Provider.CRAZY_ENCHANTS)) {
         itemIds.addAll(((CrazyEnchantmentsHook)PROVIDERS.get(EnchantsProvider.Provider.CRAZY_ENCHANTS)).getEnchantsOfItem(itemStack));
      }

      if (!itemStack.getEnchantments().isEmpty() || itemStack.getType() == Material.ENCHANTED_BOOK) {
         for (Entry entry : (itemStack.getType() == Material.ENCHANTED_BOOK
               ? ((EnchantmentStorageMeta)itemStack.getItemMeta()).getStoredEnchants()
               : itemStack.getEnchantments())
            .entrySet()) {
            EnchantsProvider.Provider provider = this.getEnchantProvider((Enchantment)entry.getKey());
            if (provider == null) {
               SendMessage.warnMessage(
                  "Failed to determine enchantment owner for '"
                     + ((Enchantment)entry.getKey()).getKey().toString()
                     + "', this is most likely a bug, report it to the plugin Author!"
               );
            } else {
               itemIds.add(new SimpleEnchantment(provider, entry.getKey(), ((Enchantment)entry.getKey()).getKey().getKey(), (Integer)entry.getValue()));
            }
         }
      }

      return itemIds;
   }

   private EnchantsProvider.Provider getEnchantProvider(Enchantment enchantment) {
      for (EnchantsProvider.Provider provider : Arrays.asList(
         EnchantsProvider.Provider.ECO_ENCHANTS, EnchantsProvider.Provider.EXCELLENT_ENCHANTS, EnchantsProvider.Provider.VANILLA
      )) {
         if (PROVIDERS.containsKey(provider) && PROVIDERS.get(provider).isEnchant(enchantment.getKey())) {
            return provider;
         }
      }

      return null;
   }

   public List<String> getEnchants(ItemStack itemStack) {
      return this.getEnchantKeys(itemStack).stream().map(element -> element.substring(0, element.lastIndexOf(":"))).collect(Collectors.toList());
   }

   public List<String> getAvailableEnchantKeys() {
      ArrayList values = new ArrayList();

      for (EnchantProvider enchantProvider : PROVIDERS.values()) {
         values.addAll(enchantProvider.getAvailableEnchantKeys());
      }

      return values;
   }

   public List<SimpleEnchantment> getAvailableEnchants() {
      ArrayList values = new ArrayList();

      for (EnchantProvider enchantProvider : PROVIDERS.values()) {
         values.addAll(enchantProvider.getAvailableEnchants());
      }

      return values;
   }

   public List<Object> getAvailableEnchants(ItemStack itemStack) {
      ArrayList values = new ArrayList();

      for (EnchantProvider enchantProvider : PROVIDERS.values()) {
         values.addAll(enchantProvider.getAvailableEnchants(itemStack));
      }

      return values;
   }

   public static ItemStack getBook(SimpleEnchantment simpleEnchantment) {
      return PROVIDERS.get(simpleEnchantment.getProvider()).getBook(simpleEnchantment);
   }

   public static String getName(SimpleEnchantment simpleEnchantment) {
      return PROVIDERS.get(simpleEnchantment.getProvider()).getName(simpleEnchantment);
   }

   public static enum Provider {
      ECO_ENCHANTS("ecoenchants:", "EcoEnchants"),
      EXCELLENT_ENCHANTS("excellentenchants:", "ExcellentEnchants"),
      CRAZY_ENCHANTS("crazyenchantments:", "CrazyEnchantments"),
      ADVANCED_ENCHANTS("advancedenchantments:", "AdvancedEnchantments"),
      VANILLA("", "Minecraft");

      final String key;
      final String name;

      private Provider(String text, String text2) {
         this.key = text;
         this.name = text2;
      }

      public static EnchantsProvider.Provider byKey(String key) {
         for (EnchantsProvider.Provider provider : values()) {
            if (key.toLowerCase().startsWith(provider.key)) {
               return provider;
            }
         }

         return VANILLA;
      }
   }
}
