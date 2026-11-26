package com.onyxshop.methodes;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.files.Translatable;
import com.onyxshop.objects.DisplayItem;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.providers.enchantments.SimpleEnchantment;
import com.onyxshop.util.ChatUtil;
import com.onyxshop.util.CommandType;
import com.onyxshop.util.EntityTypes;
import com.onyxshop.util.FireworkUtil;
import com.onyxshop.util.ItemBuilder;
import com.onyxshop.util.NBTUtils;
import com.onyxshop.util.PotionTypes;
import com.onyxshop.util.ReflectUtil;
import com.onyxshop.util.ServerInfo;
import com.onyxshop.util.SkullUtil;
import com.onyxshop.util.XEnchantment;
import com.onyxshop.util.XMaterial;
import com.onyxshop.util.exceptions.ItemLoadException;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.MusicInstrument;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.KnowledgeBookMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.MusicInstrumentMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.SuspiciousStewMeta;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public class CreateItemMethodes {
   public static ItemStack createItemMaterialFromString(ShopItem var0, String var1) throws ItemLoadException {
      if (var1 != null && !var1.isEmpty()) {
         ItemStack var2 = OnyxShop.getInstance().getItemManager().getSPM(var0, var1);
         if (var2 == null) {
            Optional var3 = XMaterial.matchXMaterial(var1);
            if (!var3.isPresent()) {
               String[] var4 = var1.split(":");
               Material var5 = Material.getMaterial(var4[0]);
               if (var5 == null) {
                  throw new ItemLoadException(Lang.ITEM_MATERIAL_NULL.get().replace("%material%", var1));
               }

               try {
                  var2 = var4.length == 1 ? new ItemStack(var5) : new ItemStack(var5, 1, Short.parseShort(var4[1]));
               } catch (NumberFormatException var7) {
                  throw new ItemLoadException(ChatColor.RED + "Unable to get data value for '%material%'".replace("%material%", var1));
               }
            } else {
               var2 = ((XMaterial)var3.get()).parseItem();
            }

            if (var2 == null) {
               throw new ItemLoadException(Lang.MATERIAL_NOT_SUPPORTED.get());
            }
         }

         return var2;
      } else {
         throw new ItemLoadException(Lang.NEED_ITEM_MATERIAL.get());
      }
   }

   public static ItemStack setNBTData(ItemStack var0, String var1, boolean var2) throws ItemLoadException {
      if (var1 != null && !var1.isEmpty()) {
         if (var1.charAt(0) == '{' && var1.charAt(var1.length() - 1) == '}') {
            return OnyxShop.getInstance().versionHandler.addNBTtag(var0, var1, var2);
         } else {
            throw new ItemLoadException(Lang.NBTDATA_NEEDS_CURLY_BRACKETS.get());
         }
      } else {
         throw new ItemLoadException(Lang.NBTDATA_NULL.get());
      }
   }

   public ItemStack setOption(ItemStack var1, ConfigurationSection var2, DisplayItem var3, String var4, boolean var5) throws ItemLoadException {
      if (var2.contains("enchantments")) {
         var1 = setEnchantments(var1, var2.getStringList(".enchantments"), var4, "pages." + var2.getCurrentPath(), var5 ? var3 : null);
      }

      if (var2.contains("spawnertype")) {
         if (var1.getType() != XMaterial.SPAWNER.parseMaterial()) {
            throw new ItemLoadException(Lang.MATERIAL_NEEDS_TO_BE_SPAWNER.get());
         }

         var1 = this.addSpawnerType(var5 && var3 instanceof ShopItem ? (ShopItem)var3 : null, var1, var2.getString("spawnertype"), var2.getString("name"));
      } else if (var2.contains("potiontypes")) {
         boolean var14 = var2.getString("material").equalsIgnoreCase("SPLASH_POTION");
         var1 = setPotionTypes(var1, var2.getStringList("potiontypes"), var4, "pages." + var2.getCurrentPath(), var14);
      } else if (var2.contains("recipes")) {
         if (var1.getType() != XMaterial.KNOWLEDGE_BOOK.parseMaterial()) {
            throw new ItemLoadException(Lang.MATERIAL_NEEDS_TO_BE_BOOK.get());
         }

         for (String var20 : var2.getStringList("recipes")) {
            if (!var20.isEmpty()) {
               var1 = this.addRecipe(var1, new NamespacedKey("minecraft", this.getRecipeMaterial(var20).toString().toLowerCase(Locale.ENGLISH)));
            }
         }
      } else if (!var2.contains("skullowner") && !var2.contains("skull-texture")) {
         if (var1.getType().equals(XMaterial.FIREWORK_ROCKET.parseMaterial())) {
            var1 = FireworkUtil.addEffect(var1, var2);
         } else if (var2.contains("instrument")) {
            var1 = setInstrument(var1, var2.getString("instrument"));
         } else if (var2.contains("stew-effect")) {
            var1 = setStewEffect(var1, var2.getString("stew-effect"));
         } else if (var2.getBoolean("autosell")) {
            if (OnyxShop.getInstance().version < 116) {
               throw new ItemLoadException("AutoSellChests are only supported on 1.16 and above");
            }

            if (Bukkit.getPluginManager().getPlugin("AutoSellChests") == null) {
               throw new ItemLoadException("AutoSellChests needs to be installed to create this item");
            }

            ItemMeta var12 = var1.getItemMeta();
            var12.getPersistentDataContainer()
               .set(new NamespacedKey(Bukkit.getPluginManager().getPlugin("AutoSellChests"), "autosell"), PersistentDataType.INTEGER, 1);
            var1.setItemMeta(var12);
         } else if (var2.contains("armor-trim")) {
            if (OnyxShop.getInstance().version < 120) {
               throw new ItemLoadException("ArmorTrim's are only supported in version 1.20 and above");
            }

            new CreateItemMethodes.ArmorTrim(var2.getString("armor-trim.type"), var2.getString("armor-trim.pattern")).build(var1);
         } else if (var2.contains("spawn-potentials")) {
            if (XMaterial.matchXMaterial(var1.getType()) != XMaterial.TRIAL_SPAWNER) {
               throw new ItemLoadException(
                  Lang.INVALID_MATERIAL_FOR_ITEM_OPTION.get().replace("%material%", "TRIAL_SPAWNER").replace("%item-option%", "spawn-potentials")
               );
            }

            var1 = this.addSpawnPotentials(var5 && var3 instanceof ShopItem ? (ShopItem)var3 : null, var2, var1, var4, "pages." + var2.getCurrentPath());
         } else if (var2.contains("ominous-strength")) {
            var1 = this.setOminousStrength(var1, var2.getInt("ominous-strength"));
         }
      } else {
         if (var1.getType() != XMaterial.PLAYER_HEAD.parseMaterial()) {
            throw new ItemLoadException(Lang.MATERIAL_NEEDS_TO_BE_SKULL.get());
         }

         SkullMeta var6 = (SkullMeta)var1.getItemMeta();
         if (var2.contains("skullowner")) {
            String var7 = var2.getString("skullowner");
            if (var7 == null || var7.isEmpty()) {
               throw new ItemLoadException(Lang.NEED_SKULLOWNER.get());
            }

            SkullUtil.setSkullTexture(var2x -> var3.updateSkullTexture(var2x, !var5), var1, var6, var7, var2.getString("skull-texture"));
         } else if (var2.contains("skull-texture")) {
            String var19 = var2.getString("skull-texture");
            if (var19 == null || var19.isEmpty()) {
               throw new ItemLoadException(Lang.NEED_SKULLOWNER.get());
            }

            SkullUtil.setSkullTextureFromHash(var6, var19);
         }

         var1.setItemMeta(var6);
      }

      if (var2.contains("armorcolor")) {
         if (!(var1.getItemMeta() instanceof LeatherArmorMeta)) {
            throw new ItemLoadException(Lang.MATERIAL_NEEDS_TO_BE_LEATHER_ARMOR.get());
         }

         LeatherArmorMeta var15 = (LeatherArmorMeta)var1.getItemMeta();
         var15.setColor(this.getRGBColor(var2.getString("armorcolor"), var2.getCurrentPath() + ".armorcolor"));
         var1.setItemMeta(var15);
      }

      if (var2.contains("enchantment-glint") && var2.getBoolean("enchantment-glint")) {
         ItemMeta var16 = var1.getItemMeta();
         if (ServerInfo.supportsComponents()) {
            var16.setEnchantmentGlintOverride(true);
         } else {
            var16.addEnchant(XEnchantment.WATER_WORKER.parseEnchantment(), 1, false);
            var16.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
         }

         var1.setItemMeta(var16);
      }

      if (var2.contains("item-flags")) {
         var1 = this.addItemFlags(var1, var2);
      }

      if (var2.getBoolean("hide-tooltip") && ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_21_R3)) {
         var1 = new ItemBuilder(var1).hideToolTip().build();
      }

      if (var2.contains("CustomModelData")) {
         var1 = setCustomModelData(var1, var2);
      }

      if (var2.contains("stack-size")) {
         int var17 = var2.getInt("stack-size");
         if (var17 < 1 || var17 > 64) {
            throw new ItemLoadException(
               "Cannot set the stack size to %stack-size%, it needs to be a number between 1 and 64.".replace("%stack-size%", String.valueOf(var17))
            );
         }

         if (!OnyxShop.getInstance().allowIllegalStacks && var17 > var1.getMaxStackSize()) {
            throw new ItemLoadException(
               "Cannot set the stack size of item %material% to %stack-size% because it would exceed the vanilla max stack size."
                  .replace("%material%", var1.getType().toString())
                  .replace("%stack-size%", String.valueOf(var17))
            );
         }

         var1.setAmount(var17);
      } else if (var3 instanceof ShopItem && ((ShopItem)var3).getStackSize() > 1) {
         var1.setAmount(Math.max(Math.min(((ShopItem)var3).getStackSize(), 64), 1));
      }

      String var18 = NBTUtils.getNBT(var1, var2, new ConfigManager.CFile(ConfigManager.CFile.Type.SHOP, var3.section()), var3.itemLoc(), var5);
      if (var18 != null) {
         var1 = setNBTData(var1, var18, true);
      }

      if (var5) {
         for (CommandType var10 : CommandType.values()) {
            List var11 = var2.getStringList(var10.getCompound());
            if (!var11.isEmpty()) {
               var1 = OnyxShop.getInstance().versionHandler.addNBTCommands(var1, var11, var10);
            }
         }
      }

      return var1;
   }

   private static PotionEffect getStewEffect(String var0) throws ItemLoadException {
      CreateItemMethodes.Stew var1 = CreateItemMethodes.Stew.get(var0);
      if (var1 == null) {
         throw new ItemLoadException("Could not find a stew effect with name/id '" + var0 + "'");
      } else {
         return var1.parseEffect();
      }
   }

   public static ItemStack setInstrument(ItemStack var0, String var1) throws ItemLoadException {
      if (OnyxShop.getInstance().version >= 119) {
         if (var0.getType() == Material.GOAT_HORN) {
            try {
               return CreateItemMethodes.Horn.setInstrument(var0, var1);
            } catch (IllegalArgumentException var3) {
               throw new ItemLoadException("Could not find a instrument called by that name '%instrument%'".replace("%instrument%", var1));
            }
         } else {
            throw new ItemLoadException("To apply a horn sound type, the material needs to be a goat horn");
         }
      } else {
         throw new ItemLoadException("The instrument item option is only supported in version 1.19 and above");
      }
   }

   public static ItemStack setStewEffect(ItemStack var0, String var1) throws ItemLoadException {
      if (OnyxShop.getInstance().version >= 114) {
         if (var0.getItemMeta() instanceof SuspiciousStewMeta) {
            SuspiciousStewMeta var2 = (SuspiciousStewMeta)var0.getItemMeta();
            var2.addCustomEffect(getStewEffect(var1), true);
            var0.setItemMeta(var2);
            return var0;
         } else {
            throw new ItemLoadException("To add a stew effect to an item, the material needs to be a 'SUSPICIOUS_STEW'.");
         }
      } else {
         throw new ItemLoadException("Effects on SuspiciousStews are only supported in version 1.14 and above");
      }
   }

   private Material getRecipeMaterial(String var1) throws ItemLoadException {
      if (!var1.isEmpty()) {
         Optional var3 = XMaterial.matchXMaterial(var1);
         if (!var3.isPresent()) {
            throw new ItemLoadException(Lang.CANNOT_GET_RECIPE_MATERIAL.get());
         } else {
            ItemStack var2 = ((XMaterial)var3.get()).parseItem();
            if (var2 == null) {
               throw new ItemLoadException(Lang.RECIPE_MATERIAL_NOT_SUPPORTED.get());
            } else {
               return var2.getType();
            }
         }
      } else {
         throw new ItemLoadException(Lang.NEED_RECIPE_MATERIAL.get());
      }
   }

   public Color getRGBColor(String var1, String var2) throws ItemLoadException {
      var1 = var1.replace("#", "");
      if (var1.startsWith("0x")) {
         var1 = var1.substring(2);
      }

      try {
         java.awt.Color var3 = java.awt.Color.decode("#" + var1);
         return Color.fromRGB(var3.getRed(), var3.getGreen(), var3.getBlue());
      } catch (IllegalArgumentException var4) {
         throw new ItemLoadException(Lang.RGB_COLOR_FORMATTED_WRONG.get().replace("%path%", var2));
      }
   }

   private ItemStack addSpawnerType(@Nullable ShopItem var1, ItemStack var2, String var3, String var4) throws ItemLoadException {
      if (var3 != null) {
         if (!var3.isEmpty()) {
            Optional var5 = EntityTypes.matchEntityType(var3);
            if (!var5.isPresent()) {
               throw new ItemLoadException(Lang.ITEM_SPAWNERTYPE_NULL.get());
            } else {
               var2 = OnyxShop.getInstance().getSpawnerManager().getProvider().setSpawnedType(var1, var2, (EntityType)var5.get());
               if (var4 != null && !var4.isEmpty()) {
                  OnyxShop.getInstance().addSpawnerName((EntityType)var5.get(), SendMessage.fromConfig(var4));
               }

               return var2;
            }
         } else {
            throw new ItemLoadException(Lang.NEED_ITEM_SPAWNERTYPE.get());
         }
      } else {
         throw new ItemLoadException(Lang.ITEM_SPAWNERTYPE_NULL.get());
      }
   }

   private ItemStack addSpawnPotentials(@Nullable ShopItem var1, ConfigurationSection var2, ItemStack var3, String var4, String var5) throws ItemLoadException {
      if (var1 != null) {
         return var3;
      } else {
         List<String> var6 = var2.getStringList("spawn-potentials");

         for (String var8 : var6) {
            try {
               if (!var8.contains(":")) {
                  throw new ItemLoadException(Lang.INVALID_SPAWN_POTENTIAL.get().replace("%spawn_potential%", var8));
               }

               Optional var9 = EntityTypes.matchEntityType(var8.split(":")[0]);
               if (!var9.isPresent()) {
                  throw new ItemLoadException(Lang.ITEM_SPAWNERTYPE_NULL.get());
               }

               int var10;
               try {
                  var10 = Integer.parseInt(var8.split(":")[1]);
               } catch (NumberFormatException var15) {
                  throw new ItemLoadException(Lang.INVALID_SPAWN_POTENTIAL_WEIGHT.get());
               }

               try {
                  var3 = (ItemStack)ReflectUtil.invokeModernVersionMethod("addSpawnPotential", var3, var9.get(), var10);
               } catch (Exception var14) {
                  StringWriter var12 = new StringWriter();
                  PrintWriter var13 = new PrintWriter(var12);
                  var14.printStackTrace(var13);
                  throw new ItemLoadException(Lang.ERROR_SETTING_ITEM_OPTION.get().replace("%item-option%", "spawn-potentials") + "\n" + var12.toString());
               }
            } catch (ItemLoadException var16) {
               if (var6.size() == 1) {
                  throw var16;
               }

               SendMessage.warnMessage(var16.getMessage());
               if (var2 != null) {
                  if (!var5.startsWith("pages") && !var5.startsWith("items")) {
                     SendMessage.errorSections(var4, var5 + ".spawn-potentials." + var8);
                  } else {
                     SendMessage.errorShops(var4, var5 + ".spawn-potentials." + var8);
                  }
               } else {
                  SendMessage.errorItemConfig(var5);
               }
            }
         }

         return var3;
      }
   }

   public ItemStack setOminousStrength(ItemStack var1, int var2) throws ItemLoadException {
      if (var1.getType() != Material.OMINOUS_BOTTLE) {
         throw new ItemLoadException(
            Lang.INVALID_MATERIAL_FOR_ITEM_OPTION.get().replace("%material%", "OMINOUS_BOTTLE").replace("%item-option%", "ominous-strength")
         );
      } else if (var2 >= 1 && var2 <= 5) {
         try {
            return (ItemStack)ReflectUtil.invokeModernVersionMethod("setOminousStrength", var1, var2 - 1);
         } catch (Exception var6) {
            StringWriter var4 = new StringWriter();
            PrintWriter var5 = new PrintWriter(var4);
            var6.printStackTrace(var5);
            throw new ItemLoadException(Lang.ERROR_SETTING_ITEM_OPTION.get().replace("%item-option%", "ominous-strength") + "\n" + var4.toString());
         }
      } else {
         throw new ItemLoadException(Lang.INVALID_OMINOUS_BOTTLE_STRENGTH.get().replace("%strength%", String.valueOf(var2)));
      }
   }

   public ItemStack addItemFlags(ItemStack var1, ConfigurationSection var2) throws ItemLoadException {
      ItemMeta var3 = var1.getItemMeta();

      for (String var5 : var2.getStringList("item-flags")) {
         if (var5.equals("*") || var5.equalsIgnoreCase("ALL")) {
            var3.addItemFlags(ItemFlag.values());
            break;
         }

         try {
            ItemFlag var6 = ItemFlag.valueOf(var5.toUpperCase(Locale.ROOT));
            var3.addItemFlags(new ItemFlag[]{var6});
         } catch (IllegalArgumentException var7) {
            throw new ItemLoadException(
               "Invalid item flag for " + var5 + ", valid values are: " + Arrays.toString(Arrays.stream(ItemFlag.values()).map(Enum::name).toArray())
            );
         }
      }

      var1.setItemMeta(var3);
      return var1;
   }

   private ItemStack addRecipe(ItemStack var1, NamespacedKey var2) {
      if (OnyxShop.getInstance().version >= 112) {
         KnowledgeBookMeta var3 = (KnowledgeBookMeta)var1.getItemMeta();
         if (var3 != null) {
            var3.addRecipe(new NamespacedKey[]{var2});
         }

         var1.setItemMeta(var3);
      }

      return var1;
   }

   public static ItemStack setCustomModelData(ItemStack var0, ConfigurationSection var1) throws ItemLoadException {
      int var2 = var1.getInt("CustomModelData");
      if (var2 != 0) {
         if (ServerInfo.supportsComponents()) {
            ItemMeta var3 = var0.getItemMeta();
            var3.setCustomModelData(var2);
            var0.setItemMeta(var3);
            return var0;
         } else {
            return OnyxShop.getInstance().versionHandler.setNBTInt(var0, "CustomModelData", var2);
         }
      } else {
         throw new ItemLoadException("Invalid CustomModelData, should be a number");
      }
   }

   public static ItemStack setEnchantments(ItemStack var0, List<String> var1, String var2, String var3, @Nullable DisplayItem var4) throws ItemLoadException {
      if (var1.isEmpty()) {
         return var0;
      } else {
         for (String var6 : var1) {
            try {
               SimpleEnchantment var7 = OnyxShop.getInstance().enchantsProvider.getEnchant(var6);
               if (!var7.canEnchant(var0)) {
                  throw new ItemLoadException("Could not apply enchantment '" + var6 + "' on item with material '" + var0.getType().name() + "'");
               }

               var0 = var7.enchantItem(var0, var4);
            } catch (ItemLoadException var8) {
               if (var1.size() == 1) {
                  throw var8;
               }

               SendMessage.warnMessage(var8.getMessage());
               if (var2 != null) {
                  if (!var3.startsWith("pages") && !var3.startsWith("items")) {
                     SendMessage.errorSections(var2, var3 + ".enchantments." + var6);
                  } else {
                     SendMessage.errorShops(var2, var3 + ".enchantments." + var6);
                  }
               } else {
                  SendMessage.errorItemConfig(var3);
               }
            }
         }

         return var0;
      }
   }

   public static ItemStack setPotionTypes(ItemStack var0, List<String> var1, String var2, String var3, boolean var4) throws ItemLoadException {
      if (var1.isEmpty()) {
         return var0;
      } else {
         for (String var6 : var1) {
            try {
               if (!PotionTypes.canHaveEffects(var0.getType())) {
                  throw new ItemLoadException(Lang.MATERIAL_NEEDS_TO_BE_POTION.get());
               }

               Optional var7 = PotionTypes.matchPotionType(var6);
               if (!var7.isPresent()) {
                  throw new ItemLoadException(Lang.ITEM_POTIONTYPE_NULL.get());
               }

               if (!((PotionTypes)var7.get()).isSupported()) {
                  throw new ItemLoadException(Lang.POTIONTYPE_NOT_SUPPORTED.get());
               }

               var0 = ((PotionTypes)var7.get()).addEffect(var0, var4);
            } catch (ItemLoadException var8) {
               if (var1.size() == 1) {
                  throw var8;
               }

               SendMessage.warnMessage(var8.getMessage());
               if (var2 != null) {
                  if (!var3.startsWith("pages") && !var3.startsWith("items")) {
                     SendMessage.errorSections(var2, var3 + ".potiontypes." + var6);
                  } else {
                     SendMessage.errorShops(var2, var3 + ".potiontypes." + var6);
                  }
               } else {
                  SendMessage.errorItemConfig(var3);
               }
            }
         }

         return var0;
      }
   }

   public void useLatestVersion(OnyxShop var1, boolean var2) {
      var1.commandItems = var2;
      var1.nbtItems = var2;
      var1.useItemEditor = var2;
      var1.placeholders = var2;
      var1.requirements = var2;
      var1.rotatingShops = var2;
      if (!var2) {
         var1.shopSearch = var2;
      }

      if (!var2) {
         var1.getEcoHandler().getEcon();
      }

      if (!var2) {
         var1.itemStock = var2;
      }

      if (!var2) {
         var1.dynamicPricing = var2;
      }

      if (!var2) {
         var1.sellLimits = var2;
      }
   }

   public List<String> getDisplayLore(ConfigurationSection var1, @Nullable ShopItem var2) {
      Object var3 = new ArrayList();
      if (var1.contains("displaylore")) {
         var3 = this.getLore(var1.getStringList("displaylore"), var1.getCurrentPath(), var2);
      } else if (var1.contains("lore")) {
         var3 = this.getLore(var1.getStringList("lore"), var1.getCurrentPath(), var2);
      }

      return (List<String>)var3;
   }

   public List<String> setLore(List<String> var1, ConfigurationSection var2, @Nullable ShopItem var3) {
      return var2.contains("lore") ? this.getLore(var2.getStringList("lore"), var2.getCurrentPath(), var3) : var1;
   }

   private List<String> getLore(List<String> var1, String var2, @Nullable ShopItem var3) {
      ArrayList var4 = new ArrayList();
      if (!var1.isEmpty()) {
         var1.forEach(
            var3x -> {
               if (var3x.contains("%buyPrice%") && OnyxShop.getInstance().createItem.getBaseBuyPrice(var2) != null) {
                  var3x = var3x.replace(
                     "%buyPrice%",
                     OnyxShop.getInstance()
                        .formatPrice(var3 != null ? var3.getEcoType() : null, OnyxShop.getInstance().createItem.getBaseBuyPrice(var2))
                  );
               }

               if (var3x.contains("%sellPrice%") && OnyxShop.getInstance().createItem.getBaseSellPrice(var2) != null) {
                  var3x = var3x.replace(
                     "%sellPrice%",
                     OnyxShop.getInstance()
                        .formatPrice(var3 != null ? var3.getEcoType() : null, OnyxShop.getInstance().createItem.getBaseSellPrice(var2))
                  );
               }

               var4.add(ChatUtil.formatColors(var3x));
            }
         );
      }

      return var4;
   }

   public ItemStack setName(ItemStack var1, ConfigurationSection var2) throws ItemLoadException {
      if (var2.contains("name")) {
         String var3 = var2.getString("name");
         if (var3 != null) {
            ItemMeta var4 = var1.getItemMeta();
            var4.setDisplayName(ChatUtil.formatColors(var3));
            var1.setItemMeta(var4);
            return var1;
         } else {
            throw new ItemLoadException(Lang.ITEM_NAME_NULL.get());
         }
      } else {
         return var1;
      }
   }

   public Translatable getDisplayname(ConfigurationSection var1) {
      if (var1.contains("displayname")) {
         return SendMessage.fromConfig(var1.getString("displayname"));
      } else {
         return var1.contains("name") ? SendMessage.fromConfig(var1.getString("name")) : null;
      }
   }

   public ItemStack setSectionItemOption(ItemStack var1, String var2, ConfigurationSection var3) throws ItemLoadException {
      if (var3.contains("potion-glow") && var3.getBoolean("potion-glow")) {
         if (!PotionTypes.canHaveEffects(var1.getType())) {
            throw new ItemLoadException(Lang.MATERIAL_NEEDS_TO_BE_POTION.get());
         }

         boolean var4 = XMaterial.matchXMaterial(var3.getString("material")).get() == XMaterial.SPLASH_POTION;
         PotionTypes.HEALING.addEffect(var1, var4);
      }

      if (var3.contains("potiontypes")) {
         setPotionTypes(var1, var3.getStringList("potiontypes"), var2, var3.getCurrentPath(), XMaterial.matchXMaterial(var1) == XMaterial.SPLASH_POTION);
      }

      if (var3.contains("armor-trim")) {
         if (OnyxShop.getInstance().version < 120) {
            throw new ItemLoadException("ArmorTrim's are only supported in version 1.20 and above");
         }

         new CreateItemMethodes.ArmorTrim(var3.getString("armor-trim.type"), var3.getString("armor-trim.pattern")).build(var1);
      }

      if (var3.contains("enchantment-glint") && var3.getBoolean("enchantment-glint")) {
         ItemMeta var6 = var1.getItemMeta();
         if (ServerInfo.supportsComponents()) {
            var6.setEnchantmentGlintOverride(true);
         } else {
            var6.addEnchant(XEnchantment.WATER_WORKER.parseEnchantment(), 1, false);
         }

         var1.setItemMeta(var6);
      }

      if (var3.contains("skullowner") || var3.contains("skull-texture")) {
         if (var1.getType() != XMaterial.PLAYER_HEAD.parseMaterial()) {
            throw new ItemLoadException(Lang.MATERIAL_NEEDS_TO_BE_SKULL.get());
         }

         if (var3.contains("skullowner")) {
            String var7 = var3.getString("skullowner");
            if (var7 == null || var7.isEmpty()) {
               throw new ItemLoadException(Lang.NEED_SKULLOWNER.get());
            }

            SkullUtil.setSkullTexture(var1, null, var7, true);
         }

         if (var3.contains("skull-texture")) {
            String var8 = var3.getString("skull-texture");
            if (var8 == null || var8.isEmpty()) {
               throw new ItemLoadException(Lang.NEED_SKULLOWNER.get());
            }

            SkullMeta var5 = (SkullMeta)var1.getItemMeta();
            SkullUtil.setSkullTextureFromHash(var5, var8);
            var1.setItemMeta(var5);
         }
      }

      if (var3.contains("armorcolor")) {
         if (!(var1.getItemMeta() instanceof LeatherArmorMeta)) {
            throw new ItemLoadException(Lang.MATERIAL_NEEDS_TO_BE_LEATHER_ARMOR.get());
         }

         LeatherArmorMeta var9 = (LeatherArmorMeta)var1.getItemMeta();
         var9.setColor(this.getRGBColor(var3.getString("armorcolor"), var3.getCurrentPath() + ".armorcolor"));
         var1.setItemMeta(var9);
      }

      if (var3.contains("stack-size")) {
         int var10 = var3.getInt("stack-size");
         if (!OnyxShop.getInstance().allowIllegalStacks && var10 > var1.getMaxStackSize()) {
            throw new ItemLoadException(
               "Cannot set the stack size of item %material% to %stack-size% because it would exceed the vanilla max stack size."
                  .replace("%material%", var1.getType().toString())
                  .replace("%stack-size%", String.valueOf(var10))
            );
         }

         var1.setAmount(var10);
      }

      if (var3.contains("item-flags")) {
         var1 = this.addItemFlags(var1, var3);
      }

      if (var3.contains("CustomModelData")) {
         var1 = setCustomModelData(var1, var3);
      }

      if (var3.getBoolean("hide-tooltip") && ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_21_R3)) {
         var1 = new ItemBuilder(var1).hideToolTip().build();
      }

      return var1;
   }

   public static class ArmorTrim {
      private final TrimMaterial type;
      private final TrimPattern pattern;

      public ArrayList<String> getTypes() {
         ArrayList var1 = new ArrayList();

         for (Field var5 : TrimMaterial.class.getDeclaredFields()) {
            if (Modifier.isStatic(var5.getModifiers()) && var5.getName().equals(var5.getName().toUpperCase(Locale.ROOT))) {
               var1.add(var5.getName());
            }
         }

         return var1;
      }

      public ArrayList<String> getPatterns() {
         ArrayList var1 = new ArrayList();

         for (Field var5 : TrimPattern.class.getDeclaredFields()) {
            if (Modifier.isStatic(var5.getModifiers()) && var5.getName().equals(var5.getName().toUpperCase(Locale.ROOT))) {
               var1.add(var5.getName().toUpperCase(Locale.ROOT));
            }
         }

         return var1;
      }

      public ArmorTrim() {
         this.type = null;
         this.pattern = null;
      }

      public ArmorTrim(String var1, String var2) throws ItemLoadException {
         if (var1 != null && var2 != null) {
            this.type = this.getTrimType(var1);
            this.pattern = this.getTrimPattern(var2);
         } else {
            throw new ItemLoadException("To apply a armor trim, it is required to specify both trim material and trim pattern!");
         }
      }

      public TrimMaterial getTrimType(String var1) throws ItemLoadException {
         try {
            Field var2 = TrimMaterial.class.getDeclaredField(var1.toUpperCase(Locale.ROOT).replace(" ", "_"));
            var2.setAccessible(true);
            return (TrimMaterial)var2.get(null);
         } catch (IllegalAccessException | NoSuchFieldException var3) {
            throw new ItemLoadException("Invalid armor trim type such as '" + var1 + "'. Valid values are: " + Arrays.toString(this.getTypes().toArray()));
         }
      }

      public TrimPattern getTrimPattern(String var1) throws ItemLoadException {
         try {
            Field var2 = TrimPattern.class.getDeclaredField(var1.toUpperCase(Locale.ROOT).replace(" ", "_"));
            var2.setAccessible(true);
            return (TrimPattern)var2.get(null);
         } catch (IllegalAccessException | NoSuchFieldException var3) {
            throw new ItemLoadException("Invalid armor trim pattern such as '" + var1 + "'. Valid values are: " + Arrays.toString(this.getPatterns().toArray()));
         }
      }

      public ItemStack build(ItemStack var1) throws ItemLoadException {
         if (var1.getItemMeta() instanceof ArmorMeta) {
            ArmorMeta var2 = (ArmorMeta)var1.getItemMeta();
            var2.setTrim(new org.bukkit.inventory.meta.trim.ArmorTrim(this.type, this.pattern));
            var1.setItemMeta(var2);
            return var1;
         } else {
            throw new ItemLoadException("To add a armor trim to an item, the material needs to be a armor piece.");
         }
      }
   }

   public static enum Horn {
      PONDER("ponder_goat_horn"),
      SING("sing_goat_horn"),
      SEEK("seek_goat_horn"),
      FEEL("feel_goat_horn"),
      ADMIRE("admire_goat_horn"),
      CALL("call_goat_horn"),
      YEARN("yearn_goat_horn"),
      DREAM("dream_goat_horn");

      private String key;

      private Horn(String nullxx) {
         this.key = nullxx;
      }

      public String getKey() {
         return "minecraft:" + this.key;
      }

      public static CreateItemMethodes.Horn getFromString(String var0) {
         return valueOf(var0.toUpperCase(Locale.ENGLISH));
      }

      public static ItemStack setInstrument(ItemStack var0, String var1) throws ItemLoadException {
         if (ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_20_R2)) {
            var1 = var1.toLowerCase(Locale.ROOT);
            MusicInstrument var2 = (MusicInstrument)Registry.INSTRUMENT.get(NamespacedKey.minecraft(var1 + "_goat_horn"));
            if (var2 == null) {
               throw new ItemLoadException("Could not find a instrument called by that name '%instrument%'".replace("%instrument%", var1));
            }

            MusicInstrumentMeta var3 = (MusicInstrumentMeta)var0.getItemMeta();
            var3.setInstrument(var2);
            var0.setItemMeta(var3);
         } else {
            try {
               CreateItemMethodes.Horn var6 = getFromString(var1);
               var0 = new ItemStack(OnyxShop.getInstance().versionHandler.setNBTString(var0, "instrument", var6.getKey()));
            } catch (IllegalArgumentException var4) {
               throw new ItemLoadException("Could not find a instrument called by that name '%instrument%'".replace("%instrument%", var1));
            }
         }

         return var0;
      }

      public static List<String> getValues() {
         return ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_20_R2)
            ? Registry.INSTRUMENT.stream().map(var0 -> var0.getKey().getKey().split("_goat_horn")[0]).collect(Collectors.toList())
            : Arrays.stream(values()).map(Enum::name).collect(Collectors.toList());
      }
   }

   public static enum Stew {
      NIGHT_VISION(0, 100),
      JUMP(1, 120, "JUMP_BOOST"),
      WEAKNESS(2, 180),
      BLINDNESS(3, 160),
      POISON(4, 240),
      SATURATION(6, 12),
      FIRE_RESISTANCE(7, 80),
      REGENERATION(8, 160, "REGEN"),
      WITHER(9, 160);

      private final int id;
      private final int duration;
      private final String[] aliases;

      private Stew(int nullxx, int nullxxx, String... nullxxxx) {
         this.id = nullxx;
         this.duration = nullxxx;
         this.aliases = nullxxxx;
      }

      public static CreateItemMethodes.Stew get(String var0) {
         for (CreateItemMethodes.Stew var4 : values()) {
            if (var4.name().equalsIgnoreCase(var0.replace(" ", "_")) || var4.anyMatchAliases(var0)) {
               return var4;
            }
         }

         try {
            int var7 = Integer.parseInt(var0);

            for (CreateItemMethodes.Stew var5 : values()) {
               if (var5.id == var7) {
                  return var5;
               }
            }
         } catch (NumberFormatException var6) {
         }

         return null;
      }

      private boolean anyMatchAliases(String var1) {
         for (String var5 : this.aliases) {
            if (var1.equalsIgnoreCase(var5.replace(" ", "_"))) {
               return true;
            }
         }

         return false;
      }

      public PotionEffect parseEffect() {
         try {
            PotionEffectType var1 = PotionEffectType.getByName(this.name());
            if (var1 == null) {
               var1 = PotionTypes.matchPotionType(this.name()).get().parsePotionEffectType();
            }

            return new PotionEffect(var1, this.duration, 1);
         } catch (NullPointerException var2) {
            return null;
         }
      }
   }
}
