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
   public static ItemStack createItemMaterialFromString(ShopItem shopItem, String itemId) throws ItemLoadException {
      if (itemId != null && !itemId.isEmpty()) {
         ItemStack itemStack = OnyxShop.getInstance().getItemManager().getSPM(shopItem, itemId);
         if (itemStack == null) {
            Optional optional = XMaterial.matchXMaterial(itemId);
            if (!optional.isPresent()) {
               String[] materialNames = itemId.split(":");
               Material material = Material.getMaterial(materialNames[0]);
               if (material == null) {
                  throw new ItemLoadException(Lang.ITEM_MATERIAL_NULL.get().replace("%material%", itemId));
               }

               try {
                  itemStack = materialNames.length == 1 ? new ItemStack(material) : new ItemStack(material, 1, Short.parseShort(materialNames[1]));
               } catch (NumberFormatException exception) {
                  throw new ItemLoadException(ChatColor.RED + "Unable to get data value for '%material%'".replace("%material%", itemId));
               }
            } else {
               itemStack = ((XMaterial)optional.get()).parseItem();
            }

            if (itemStack == null) {
               throw new ItemLoadException(Lang.MATERIAL_NOT_SUPPORTED.get());
            }
         }

         return itemStack;
      } else {
         throw new ItemLoadException(Lang.NEED_ITEM_MATERIAL.get());
      }
   }

   public static ItemStack setNBTData(ItemStack itemStack, String itemId, boolean flag) throws ItemLoadException {
      if (itemId != null && !itemId.isEmpty()) {
         if (itemId.charAt(0) == '{' && itemId.charAt(itemId.length() - 1) == '}') {
            return OnyxShop.getInstance().versionHandler.addNBTtag(itemStack, itemId, flag);
         } else {
            throw new ItemLoadException(Lang.NBTDATA_NEEDS_CURLY_BRACKETS.get());
         }
      } else {
         throw new ItemLoadException(Lang.NBTDATA_NULL.get());
      }
   }

   public ItemStack setOption(ItemStack itemStack, ConfigurationSection section, DisplayItem displayItem, String itemId, boolean flag) throws ItemLoadException {
      if (section.contains("enchantments")) {
         itemStack = setEnchantments(itemStack, section.getStringList(".enchantments"), itemId, "pages." + section.getCurrentPath(), flag ? displayItem : null);
      }

      if (section.contains("spawnertype")) {
         if (itemStack.getType() != XMaterial.SPAWNER.parseMaterial()) {
            throw new ItemLoadException(Lang.MATERIAL_NEEDS_TO_BE_SPAWNER.get());
         }

         itemStack = this.addSpawnerType(flag && displayItem instanceof ShopItem ? (ShopItem)displayItem : null, itemStack, section.getString("spawnertype"), section.getString("name"));
      } else if (section.contains("potiontypes")) {
         boolean flag2 = section.getString("material").equalsIgnoreCase("SPLASH_POTION");
         itemStack = setPotionTypes(itemStack, section.getStringList("potiontypes"), itemId, "pages." + section.getCurrentPath(), flag2);
      } else if (section.contains("recipes")) {
         if (itemStack.getType() != XMaterial.KNOWLEDGE_BOOK.parseMaterial()) {
            throw new ItemLoadException(Lang.MATERIAL_NEEDS_TO_BE_BOOK.get());
         }

         for (String text : section.getStringList("recipes")) {
            if (!text.isEmpty()) {
               itemStack = this.addRecipe(itemStack, new NamespacedKey("minecraft", this.getRecipeMaterial(text).toString().toLowerCase(Locale.ENGLISH)));
            }
         }
      } else if (!section.contains("skullowner") && !section.contains("skull-texture")) {
         if (itemStack.getType().equals(XMaterial.FIREWORK_ROCKET.parseMaterial())) {
            itemStack = FireworkUtil.addEffect(itemStack, section);
         } else if (section.contains("instrument")) {
            itemStack = setInstrument(itemStack, section.getString("instrument"));
         } else if (section.contains("stew-effect")) {
            itemStack = setStewEffect(itemStack, section.getString("stew-effect"));
         } else if (section.getBoolean("autosell")) {
            if (OnyxShop.getInstance().version < 116) {
               throw new ItemLoadException("AutoSellChests are only supported on 1.16 and above");
            }

            if (Bukkit.getPluginManager().getPlugin("AutoSellChests") == null) {
               throw new ItemLoadException("AutoSellChests needs to be installed to create this item");
            }

            ItemMeta itemMeta = itemStack.getItemMeta();
            itemMeta.getPersistentDataContainer()
               .set(new NamespacedKey(Bukkit.getPluginManager().getPlugin("AutoSellChests"), "autosell"), PersistentDataType.INTEGER, 1);
            itemStack.setItemMeta(itemMeta);
         } else if (section.contains("armor-trim")) {
            if (OnyxShop.getInstance().version < 120) {
               throw new ItemLoadException("ArmorTrim's are only supported in version 1.20 and above");
            }

            new CreateItemMethodes.ArmorTrim(section.getString("armor-trim.type"), section.getString("armor-trim.pattern")).build(itemStack);
         } else if (section.contains("spawn-potentials")) {
            if (XMaterial.matchXMaterial(itemStack.getType()) != XMaterial.TRIAL_SPAWNER) {
               throw new ItemLoadException(
                  Lang.INVALID_MATERIAL_FOR_ITEM_OPTION.get().replace("%material%", "TRIAL_SPAWNER").replace("%item-option%", "spawn-potentials")
               );
            }

            itemStack = this.addSpawnPotentials(flag && displayItem instanceof ShopItem ? (ShopItem)displayItem : null, section, itemStack, itemId, "pages." + section.getCurrentPath());
         } else if (section.contains("ominous-strength")) {
            itemStack = this.setOminousStrength(itemStack, section.getInt("ominous-strength"));
         }
      } else {
         if (itemStack.getType() != XMaterial.PLAYER_HEAD.parseMaterial()) {
            throw new ItemLoadException(Lang.MATERIAL_NEEDS_TO_BE_SKULL.get());
         }

         SkullMeta skullMeta = (SkullMeta)itemStack.getItemMeta();
         if (section.contains("skullowner")) {
            String text = section.getString("skullowner");
            if (text == null || text.isEmpty()) {
               throw new ItemLoadException(Lang.NEED_SKULLOWNER.get());
            }

            SkullUtil.setSkullTexture(element -> displayItem.updateSkullTexture(element, !flag), itemStack, skullMeta, text, section.getString("skull-texture"));
         } else if (section.contains("skull-texture")) {
            String text = section.getString("skull-texture");
            if (text == null || text.isEmpty()) {
               throw new ItemLoadException(Lang.NEED_SKULLOWNER.get());
            }

            SkullUtil.setSkullTextureFromHash(skullMeta, text);
         }

         itemStack.setItemMeta(skullMeta);
      }

      if (section.contains("armorcolor")) {
         if (!(itemStack.getItemMeta() instanceof LeatherArmorMeta)) {
            throw new ItemLoadException(Lang.MATERIAL_NEEDS_TO_BE_LEATHER_ARMOR.get());
         }

         LeatherArmorMeta leatherArmorMeta = (LeatherArmorMeta)itemStack.getItemMeta();
         leatherArmorMeta.setColor(this.getRGBColor(section.getString("armorcolor"), section.getCurrentPath() + ".armorcolor"));
         itemStack.setItemMeta(leatherArmorMeta);
      }

      if (section.contains("enchantment-glint") && section.getBoolean("enchantment-glint")) {
         ItemMeta itemMeta = itemStack.getItemMeta();
         if (ServerInfo.supportsComponents()) {
            itemMeta.setEnchantmentGlintOverride(true);
         } else {
            itemMeta.addEnchant(XEnchantment.WATER_WORKER.parseEnchantment(), 1, false);
            itemMeta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
         }

         itemStack.setItemMeta(itemMeta);
      }

      if (section.contains("item-flags")) {
         itemStack = this.addItemFlags(itemStack, section);
      }

      if (section.getBoolean("hide-tooltip") && ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_21_R3)) {
         itemStack = new ItemBuilder(itemStack).hideToolTip().build();
      }

      if (section.contains("CustomModelData")) {
         itemStack = setCustomModelData(itemStack, section);
      }

      if (section.contains("stack-size")) {
         int size = section.getInt("stack-size");
         if (size < 1 || size > 64) {
            throw new ItemLoadException(
               "Cannot set the stack size to %stack-size%, it needs to be a number between 1 and 64.".replace("%stack-size%", String.valueOf(size))
            );
         }

         if (!OnyxShop.getInstance().allowIllegalStacks && size > itemStack.getMaxStackSize()) {
            throw new ItemLoadException(
               "Cannot set the stack size of item %material% to %stack-size% because it would exceed the vanilla max stack size."
                  .replace("%material%", itemStack.getType().toString())
                  .replace("%stack-size%", String.valueOf(size))
            );
         }

         itemStack.setAmount(size);
      } else if (displayItem instanceof ShopItem && ((ShopItem)displayItem).getStackSize() > 1) {
         itemStack.setAmount(Math.max(Math.min(((ShopItem)displayItem).getStackSize(), 64), 1));
      }

      String nbt = NBTUtils.getNBT(itemStack, section, new ConfigManager.CFile(ConfigManager.CFile.Type.SHOP, displayItem.section()), displayItem.itemLoc(), flag);
      if (nbt != null) {
         itemStack = setNBTData(itemStack, nbt, true);
      }

      if (flag) {
         for (CommandType commandType : CommandType.values()) {
            List stringList = section.getStringList(commandType.getCompound());
            if (!stringList.isEmpty()) {
               itemStack = OnyxShop.getInstance().versionHandler.addNBTCommands(itemStack, stringList, commandType);
            }
         }
      }

      return itemStack;
   }

   private static PotionEffect getStewEffect(String itemId) throws ItemLoadException {
      CreateItemMethodes.Stew stew = CreateItemMethodes.Stew.get(itemId);
      if (stew == null) {
         throw new ItemLoadException("Could not find a stew effect with name/id '" + itemId + "'");
      } else {
         return stew.parseEffect();
      }
   }

   public static ItemStack setInstrument(ItemStack itemStack, String itemId) throws ItemLoadException {
      if (OnyxShop.getInstance().version >= 119) {
         if (itemStack.getType() == Material.GOAT_HORN) {
            try {
               return CreateItemMethodes.Horn.setInstrument(itemStack, itemId);
            } catch (IllegalArgumentException exception) {
               throw new ItemLoadException("Could not find a instrument called by that name '%instrument%'".replace("%instrument%", itemId));
            }
         } else {
            throw new ItemLoadException("To apply a horn sound type, the material needs to be a goat horn");
         }
      } else {
         throw new ItemLoadException("The instrument item option is only supported in version 1.19 and above");
      }
   }

   public static ItemStack setStewEffect(ItemStack itemStack, String itemId) throws ItemLoadException {
      if (OnyxShop.getInstance().version >= 114) {
         if (itemStack.getItemMeta() instanceof SuspiciousStewMeta) {
            SuspiciousStewMeta suspiciousStewMeta = (SuspiciousStewMeta)itemStack.getItemMeta();
            suspiciousStewMeta.addCustomEffect(getStewEffect(itemId), true);
            itemStack.setItemMeta(suspiciousStewMeta);
            return itemStack;
         } else {
            throw new ItemLoadException("To add a stew effect to an item, the material needs to be a 'SUSPICIOUS_STEW'.");
         }
      } else {
         throw new ItemLoadException("Effects on SuspiciousStews are only supported in version 1.14 and above");
      }
   }

   private Material getRecipeMaterial(String materialName) throws ItemLoadException {
      if (!materialName.isEmpty()) {
         Optional optional = XMaterial.matchXMaterial(materialName);
         if (!optional.isPresent()) {
            throw new ItemLoadException(Lang.CANNOT_GET_RECIPE_MATERIAL.get());
         } else {
            ItemStack itemStack = ((XMaterial)optional.get()).parseItem();
            if (itemStack == null) {
               throw new ItemLoadException(Lang.RECIPE_MATERIAL_NOT_SUPPORTED.get());
            } else {
               return itemStack.getType();
            }
         }
      } else {
         throw new ItemLoadException(Lang.NEED_RECIPE_MATERIAL.get());
      }
   }

   public Color getRGBColor(String itemId, String text) throws ItemLoadException {
      itemId = itemId.replace("#", "");
      if (itemId.startsWith("0x")) {
         itemId = itemId.substring(2);
      }

      try {
         java.awt.Color color = java.awt.Color.decode("#" + itemId);
         return Color.fromRGB(color.getRed(), color.getGreen(), color.getBlue());
      } catch (IllegalArgumentException exception) {
         throw new ItemLoadException(Lang.RGB_COLOR_FORMATTED_WRONG.get().replace("%path%", text));
      }
   }

   private ItemStack addSpawnerType(@Nullable ShopItem shopItem, ItemStack itemStack, String type, String itemId) throws ItemLoadException {
      if (type != null) {
         if (!type.isEmpty()) {
            Optional optional = EntityTypes.matchEntityType(type);
            if (!optional.isPresent()) {
               throw new ItemLoadException(Lang.ITEM_SPAWNERTYPE_NULL.get());
            } else {
               itemStack = OnyxShop.getInstance().getSpawnerManager().getProvider().setSpawnedType(shopItem, itemStack, (EntityType)optional.get());
               if (itemId != null && !itemId.isEmpty()) {
                  OnyxShop.getInstance().addSpawnerName((EntityType)optional.get(), SendMessage.fromConfig(itemId));
               }

               return itemStack;
            }
         } else {
            throw new ItemLoadException(Lang.NEED_ITEM_SPAWNERTYPE.get());
         }
      } else {
         throw new ItemLoadException(Lang.ITEM_SPAWNERTYPE_NULL.get());
      }
   }

   private ItemStack addSpawnPotentials(@Nullable ShopItem shopItem, ConfigurationSection section, ItemStack itemStack, String itemId, String shopId) throws ItemLoadException {
      if (shopItem != null) {
         return itemStack;
      } else {
         List<String> stringList = section.getStringList("spawn-potentials");

         for (String text : stringList) {
            try {
               if (!text.contains(":")) {
                  throw new ItemLoadException(Lang.INVALID_SPAWN_POTENTIAL.get().replace("%spawn_potential%", text));
               }

               Optional optional = EntityTypes.matchEntityType(text.split(":")[0]);
               if (!optional.isPresent()) {
                  throw new ItemLoadException(Lang.ITEM_SPAWNERTYPE_NULL.get());
               }

               int value;
               try {
                  value = Integer.parseInt(text.split(":")[1]);
               } catch (NumberFormatException exception) {
                  throw new ItemLoadException(Lang.INVALID_SPAWN_POTENTIAL_WEIGHT.get());
               }

               try {
                  itemStack = (ItemStack)ReflectUtil.invokeModernVersionMethod("addSpawnPotential", itemStack, optional.get(), value);
               } catch (Exception exception) {
                  StringWriter stringWriter = new StringWriter();
                  PrintWriter printWriter = new PrintWriter(stringWriter);
                  exception.printStackTrace(printWriter);
                  throw new ItemLoadException(Lang.ERROR_SETTING_ITEM_OPTION.get().replace("%item-option%", "spawn-potentials") + "\n" + stringWriter.toString());
               }
            } catch (ItemLoadException exception) {
               if (stringList.size() == 1) {
                  throw exception;
               }

               SendMessage.warnMessage(exception.getMessage());
               if (section != null) {
                  if (!shopId.startsWith("pages") && !shopId.startsWith("items")) {
                     SendMessage.errorSections(itemId, shopId + ".spawn-potentials." + text);
                  } else {
                     SendMessage.errorShops(itemId, shopId + ".spawn-potentials." + text);
                  }
               } else {
                  SendMessage.errorItemConfig(shopId);
               }
            }
         }

         return itemStack;
      }
   }

   public ItemStack setOminousStrength(ItemStack itemStack, int value) throws ItemLoadException {
      if (itemStack.getType() != Material.OMINOUS_BOTTLE) {
         throw new ItemLoadException(
            Lang.INVALID_MATERIAL_FOR_ITEM_OPTION.get().replace("%material%", "OMINOUS_BOTTLE").replace("%item-option%", "ominous-strength")
         );
      } else if (value >= 1 && value <= 5) {
         try {
            return (ItemStack)ReflectUtil.invokeModernVersionMethod("setOminousStrength", itemStack, value - 1);
         } catch (Exception exception) {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            exception.printStackTrace(printWriter);
            throw new ItemLoadException(Lang.ERROR_SETTING_ITEM_OPTION.get().replace("%item-option%", "ominous-strength") + "\n" + stringWriter.toString());
         }
      } else {
         throw new ItemLoadException(Lang.INVALID_OMINOUS_BOTTLE_STRENGTH.get().replace("%strength%", String.valueOf(value)));
      }
   }

   public ItemStack addItemFlags(ItemStack itemStack, ConfigurationSection section) throws ItemLoadException {
      ItemMeta itemMeta = itemStack.getItemMeta();

      for (String itemId : section.getStringList("item-flags")) {
         if (itemId.equals("*") || itemId.equalsIgnoreCase("ALL")) {
            itemMeta.addItemFlags(ItemFlag.values());
            break;
         }

         try {
            ItemFlag itemFlag = ItemFlag.valueOf(itemId.toUpperCase(Locale.ROOT));
            itemMeta.addItemFlags(new ItemFlag[]{itemFlag});
         } catch (IllegalArgumentException exception) {
            throw new ItemLoadException(
               "Invalid item flag for " + itemId + ", valid values are: " + Arrays.toString(Arrays.stream(ItemFlag.values()).map(Enum::name).toArray())
            );
         }
      }

      itemStack.setItemMeta(itemMeta);
      return itemStack;
   }

   private ItemStack addRecipe(ItemStack itemStack, NamespacedKey namespacedKey) {
      if (OnyxShop.getInstance().version >= 112) {
         KnowledgeBookMeta knowledgeBookMeta = (KnowledgeBookMeta)itemStack.getItemMeta();
         if (knowledgeBookMeta != null) {
            knowledgeBookMeta.addRecipe(new NamespacedKey[]{namespacedKey});
         }

         itemStack.setItemMeta(knowledgeBookMeta);
      }

      return itemStack;
   }

   public static ItemStack setCustomModelData(ItemStack itemStack, ConfigurationSection section) throws ItemLoadException {
      int value = section.getInt("CustomModelData");
      if (value != 0) {
         if (ServerInfo.supportsComponents()) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            itemMeta.setCustomModelData(value);
            itemStack.setItemMeta(itemMeta);
            return itemStack;
         } else {
            return OnyxShop.getInstance().versionHandler.setNBTInt(itemStack, "CustomModelData", value);
         }
      } else {
         throw new ItemLoadException("Invalid CustomModelData, should be a number");
      }
   }

   public static ItemStack setEnchantments(ItemStack itemStack, List<String> itemIds, String text, String text2, @Nullable DisplayItem displayItem) throws ItemLoadException {
      if (itemIds.isEmpty()) {
         return itemStack;
      } else {
         for (String text3 : itemIds) {
            try {
               SimpleEnchantment simpleEnchantment = OnyxShop.getInstance().enchantsProvider.getEnchant(text3);
               if (!simpleEnchantment.canEnchant(itemStack)) {
                  throw new ItemLoadException("Could not apply enchantment '" + text3 + "' on item with material '" + itemStack.getType().name() + "'");
               }

               itemStack = simpleEnchantment.enchantItem(itemStack, displayItem);
            } catch (ItemLoadException exception) {
               if (itemIds.size() == 1) {
                  throw exception;
               }

               SendMessage.warnMessage(exception.getMessage());
               if (text != null) {
                  if (!text2.startsWith("pages") && !text2.startsWith("items")) {
                     SendMessage.errorSections(text, text2 + ".enchantments." + text3);
                  } else {
                     SendMessage.errorShops(text, text2 + ".enchantments." + text3);
                  }
               } else {
                  SendMessage.errorItemConfig(text2);
               }
            }
         }

         return itemStack;
      }
   }

   public static ItemStack setPotionTypes(ItemStack itemStack, List<String> itemIds, String text, String text2, boolean flag) throws ItemLoadException {
      if (itemIds.isEmpty()) {
         return itemStack;
      } else {
         for (String text3 : itemIds) {
            try {
               if (!PotionTypes.canHaveEffects(itemStack.getType())) {
                  throw new ItemLoadException(Lang.MATERIAL_NEEDS_TO_BE_POTION.get());
               }

               Optional optional = PotionTypes.matchPotionType(text3);
               if (!optional.isPresent()) {
                  throw new ItemLoadException(Lang.ITEM_POTIONTYPE_NULL.get());
               }

               if (!((PotionTypes)optional.get()).isSupported()) {
                  throw new ItemLoadException(Lang.POTIONTYPE_NOT_SUPPORTED.get());
               }

               itemStack = ((PotionTypes)optional.get()).addEffect(itemStack, flag);
            } catch (ItemLoadException exception) {
               if (itemIds.size() == 1) {
                  throw exception;
               }

               SendMessage.warnMessage(exception.getMessage());
               if (text != null) {
                  if (!text2.startsWith("pages") && !text2.startsWith("items")) {
                     SendMessage.errorSections(text, text2 + ".potiontypes." + text3);
                  } else {
                     SendMessage.errorShops(text, text2 + ".potiontypes." + text3);
                  }
               } else {
                  SendMessage.errorItemConfig(text2);
               }
            }
         }

         return itemStack;
      }
   }

   public void useLatestVersion(OnyxShop plugin, boolean flag) {
      plugin.commandItems = flag;
      plugin.nbtItems = flag;
      plugin.useItemEditor = flag;
      plugin.placeholders = flag;
      plugin.requirements = flag;
      plugin.rotatingShops = flag;
      if (!flag) {
         plugin.shopSearch = flag;
      }

      if (!flag) {
         plugin.getEcoHandler().getEcon();
      }

      if (!flag) {
         plugin.itemStock = flag;
      }

      if (!flag) {
         plugin.dynamicPricing = flag;
      }

      if (!flag) {
         plugin.sellLimits = flag;
      }
   }

   public List<String> getDisplayLore(ConfigurationSection section, @Nullable ShopItem shopItem) {
      Object value = new ArrayList();
      if (section.contains("displaylore")) {
         value = this.getLore(section.getStringList("displaylore"), section.getCurrentPath(), shopItem);
      } else if (section.contains("lore")) {
         value = this.getLore(section.getStringList("lore"), section.getCurrentPath(), shopItem);
      }

      return (List<String>)value;
   }

   public List<String> setLore(List<String> lores, ConfigurationSection section, @Nullable ShopItem shopItem) {
      return section.contains("lore") ? this.getLore(section.getStringList("lore"), section.getCurrentPath(), shopItem) : lores;
   }

   private List<String> getLore(List<String> lores, String shopId, @Nullable ShopItem shopItem) {
      ArrayList values = new ArrayList();
      if (!lores.isEmpty()) {
         lores.forEach(
            entry -> {
               if (entry.contains("%buyPrice%") && OnyxShop.getInstance().createItem.getBaseBuyPrice(shopId) != null) {
                  entry = entry.replace(
                     "%buyPrice%",
                     OnyxShop.getInstance()
                        .formatPrice(shopItem != null ? shopItem.getEcoType() : null, OnyxShop.getInstance().createItem.getBaseBuyPrice(shopId))
                  );
               }

               if (entry.contains("%sellPrice%") && OnyxShop.getInstance().createItem.getBaseSellPrice(shopId) != null) {
                  entry = entry.replace(
                     "%sellPrice%",
                     OnyxShop.getInstance()
                        .formatPrice(shopItem != null ? shopItem.getEcoType() : null, OnyxShop.getInstance().createItem.getBaseSellPrice(shopId))
                  );
               }

               values.add(ChatUtil.formatColors(entry));
            }
         );
      }

      return values;
   }

   public ItemStack setName(ItemStack itemStack, ConfigurationSection section) throws ItemLoadException {
      if (section.contains("name")) {
         String text = section.getString("name");
         if (text != null) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            itemMeta.setDisplayName(ChatUtil.formatColors(text));
            itemStack.setItemMeta(itemMeta);
            return itemStack;
         } else {
            throw new ItemLoadException(Lang.ITEM_NAME_NULL.get());
         }
      } else {
         return itemStack;
      }
   }

   public Translatable getDisplayname(ConfigurationSection section) {
      if (section.contains("displayname")) {
         return SendMessage.fromConfig(section.getString("displayname"));
      } else {
         return section.contains("name") ? SendMessage.fromConfig(section.getString("name")) : null;
      }
   }

   public ItemStack setSectionItemOption(ItemStack itemStack, String sectionId, ConfigurationSection section) throws ItemLoadException {
      if (section.contains("potion-glow") && section.getBoolean("potion-glow")) {
         if (!PotionTypes.canHaveEffects(itemStack.getType())) {
            throw new ItemLoadException(Lang.MATERIAL_NEEDS_TO_BE_POTION.get());
         }

         boolean flag = XMaterial.matchXMaterial(section.getString("material")).get() == XMaterial.SPLASH_POTION;
         PotionTypes.HEALING.addEffect(itemStack, flag);
      }

      if (section.contains("potiontypes")) {
         setPotionTypes(itemStack, section.getStringList("potiontypes"), sectionId, section.getCurrentPath(), XMaterial.matchXMaterial(itemStack) == XMaterial.SPLASH_POTION);
      }

      if (section.contains("armor-trim")) {
         if (OnyxShop.getInstance().version < 120) {
            throw new ItemLoadException("ArmorTrim's are only supported in version 1.20 and above");
         }

         new CreateItemMethodes.ArmorTrim(section.getString("armor-trim.type"), section.getString("armor-trim.pattern")).build(itemStack);
      }

      if (section.contains("enchantment-glint") && section.getBoolean("enchantment-glint")) {
         ItemMeta itemMeta = itemStack.getItemMeta();
         if (ServerInfo.supportsComponents()) {
            itemMeta.setEnchantmentGlintOverride(true);
         } else {
            itemMeta.addEnchant(XEnchantment.WATER_WORKER.parseEnchantment(), 1, false);
         }

         itemStack.setItemMeta(itemMeta);
      }

      if (section.contains("skullowner") || section.contains("skull-texture")) {
         if (itemStack.getType() != XMaterial.PLAYER_HEAD.parseMaterial()) {
            throw new ItemLoadException(Lang.MATERIAL_NEEDS_TO_BE_SKULL.get());
         }

         if (section.contains("skullowner")) {
            String text = section.getString("skullowner");
            if (text == null || text.isEmpty()) {
               throw new ItemLoadException(Lang.NEED_SKULLOWNER.get());
            }

            SkullUtil.setSkullTexture(itemStack, null, text, true);
         }

         if (section.contains("skull-texture")) {
            String text = section.getString("skull-texture");
            if (text == null || text.isEmpty()) {
               throw new ItemLoadException(Lang.NEED_SKULLOWNER.get());
            }

            SkullMeta skullMeta = (SkullMeta)itemStack.getItemMeta();
            SkullUtil.setSkullTextureFromHash(skullMeta, text);
            itemStack.setItemMeta(skullMeta);
         }
      }

      if (section.contains("armorcolor")) {
         if (!(itemStack.getItemMeta() instanceof LeatherArmorMeta)) {
            throw new ItemLoadException(Lang.MATERIAL_NEEDS_TO_BE_LEATHER_ARMOR.get());
         }

         LeatherArmorMeta leatherArmorMeta = (LeatherArmorMeta)itemStack.getItemMeta();
         leatherArmorMeta.setColor(this.getRGBColor(section.getString("armorcolor"), section.getCurrentPath() + ".armorcolor"));
         itemStack.setItemMeta(leatherArmorMeta);
      }

      if (section.contains("stack-size")) {
         int size = section.getInt("stack-size");
         if (!OnyxShop.getInstance().allowIllegalStacks && size > itemStack.getMaxStackSize()) {
            throw new ItemLoadException(
               "Cannot set the stack size of item %material% to %stack-size% because it would exceed the vanilla max stack size."
                  .replace("%material%", itemStack.getType().toString())
                  .replace("%stack-size%", String.valueOf(size))
            );
         }

         itemStack.setAmount(size);
      }

      if (section.contains("item-flags")) {
         itemStack = this.addItemFlags(itemStack, section);
      }

      if (section.contains("CustomModelData")) {
         itemStack = setCustomModelData(itemStack, section);
      }

      if (section.getBoolean("hide-tooltip") && ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_21_R3)) {
         itemStack = new ItemBuilder(itemStack).hideToolTip().build();
      }

      return itemStack;
   }

   public static class ArmorTrim {
      private final TrimMaterial type;
      private final TrimPattern pattern;

      public ArrayList<String> getTypes() {
         ArrayList values = new ArrayList();

         for (Field field : TrimMaterial.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getName().equals(field.getName().toUpperCase(Locale.ROOT))) {
               values.add(field.getName());
            }
         }

         return values;
      }

      public ArrayList<String> getPatterns() {
         ArrayList values = new ArrayList();

         for (Field field : TrimPattern.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getName().equals(field.getName().toUpperCase(Locale.ROOT))) {
               values.add(field.getName().toUpperCase(Locale.ROOT));
            }
         }

         return values;
      }

      public ArmorTrim() {
         this.type = null;
         this.pattern = null;
      }

      public ArmorTrim(String itemId, String text) throws ItemLoadException {
         if (itemId != null && text != null) {
            this.type = this.getTrimType(itemId);
            this.pattern = this.getTrimPattern(text);
         } else {
            throw new ItemLoadException("To apply a armor trim, it is required to specify both trim material and trim pattern!");
         }
      }

      public TrimMaterial getTrimType(String type) throws ItemLoadException {
         try {
            Field field = TrimMaterial.class.getDeclaredField(type.toUpperCase(Locale.ROOT).replace(" ", "_"));
            field.setAccessible(true);
            return (TrimMaterial)field.get(null);
         } catch (IllegalAccessException | NoSuchFieldException exception) {
            throw new ItemLoadException("Invalid armor trim type such as '" + type + "'. Valid values are: " + Arrays.toString(this.getTypes().toArray()));
         }
      }

      public TrimPattern getTrimPattern(String itemId) throws ItemLoadException {
         try {
            Field field = TrimPattern.class.getDeclaredField(itemId.toUpperCase(Locale.ROOT).replace(" ", "_"));
            field.setAccessible(true);
            return (TrimPattern)field.get(null);
         } catch (IllegalAccessException | NoSuchFieldException exception) {
            throw new ItemLoadException("Invalid armor trim pattern such as '" + itemId + "'. Valid values are: " + Arrays.toString(this.getPatterns().toArray()));
         }
      }

      public ItemStack build(ItemStack itemStack) throws ItemLoadException {
         if (itemStack.getItemMeta() instanceof ArmorMeta) {
            ArmorMeta armorMeta = (ArmorMeta)itemStack.getItemMeta();
            armorMeta.setTrim(new org.bukkit.inventory.meta.trim.ArmorTrim(this.type, this.pattern));
            itemStack.setItemMeta(armorMeta);
            return itemStack;
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

      private Horn(String text) {
         this.key = text;
      }

      public String getKey() {
         return "minecraft:" + this.key;
      }

      public static CreateItemMethodes.Horn getFromString(String itemId) {
         return valueOf(itemId.toUpperCase(Locale.ENGLISH));
      }

      public static ItemStack setInstrument(ItemStack itemStack, String itemId) throws ItemLoadException {
         if (ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_20_R2)) {
            itemId = itemId.toLowerCase(Locale.ROOT);
            MusicInstrument musicInstrument = (MusicInstrument)Registry.INSTRUMENT.get(NamespacedKey.minecraft(itemId + "_goat_horn"));
            if (musicInstrument == null) {
               throw new ItemLoadException("Could not find a instrument called by that name '%instrument%'".replace("%instrument%", itemId));
            }

            MusicInstrumentMeta musicInstrumentMeta = (MusicInstrumentMeta)itemStack.getItemMeta();
            musicInstrumentMeta.setInstrument(musicInstrument);
            itemStack.setItemMeta(musicInstrumentMeta);
         } else {
            try {
               CreateItemMethodes.Horn horn = getFromString(itemId);
               itemStack = new ItemStack(OnyxShop.getInstance().versionHandler.setNBTString(itemStack, "instrument", horn.getKey()));
            } catch (IllegalArgumentException exception) {
               throw new ItemLoadException("Could not find a instrument called by that name '%instrument%'".replace("%instrument%", itemId));
            }
         }

         return itemStack;
      }

      public static List<String> getValues() {
         return ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_20_R2)
            ? Registry.INSTRUMENT.stream().map(entry -> entry.getKey().getKey().split("_goat_horn")[0]).collect(Collectors.toList())
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

      private Stew(int value, int value2, String... values) {
         this.id = value;
         this.duration = value2;
         this.aliases = values;
      }

      public static CreateItemMethodes.Stew get(String itemId) {
         for (CreateItemMethodes.Stew stew : values()) {
            if (stew.name().equalsIgnoreCase(itemId.replace(" ", "_")) || stew.anyMatchAliases(itemId)) {
               return stew;
            }
         }

         try {
            int value = Integer.parseInt(itemId);

            for (CreateItemMethodes.Stew stew : values()) {
               if (stew.id == value) {
                  return stew;
               }
            }
         } catch (NumberFormatException exception) {
         }

         return null;
      }

      private boolean anyMatchAliases(String text) {
         for (String text2 : this.aliases) {
            if (text.equalsIgnoreCase(text2.replace(" ", "_"))) {
               return true;
            }
         }

         return false;
      }

      public PotionEffect parseEffect() {
         try {
            PotionEffectType potionEffectType = PotionEffectType.getByName(this.name());
            if (potionEffectType == null) {
               potionEffectType = PotionTypes.matchPotionType(this.name()).get().parsePotionEffectType();
            }

            return new PotionEffect(potionEffectType, this.duration, 1);
         } catch (NullPointerException exception) {
            return null;
         }
      }
   }
}
