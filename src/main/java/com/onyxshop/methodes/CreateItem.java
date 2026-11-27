package com.onyxshop.methodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.files.Translatable;
import com.onyxshop.files.lang.TranslatableRaw;
import com.onyxshop.objects.ActionItem;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.objects.TransactionItem;
import com.onyxshop.objects.TransactionScreen;
import com.onyxshop.providers.SpawnerManager;
import com.onyxshop.util.ChatUtil;
import com.onyxshop.util.FireworkUtil;
import com.onyxshop.util.ItemBuilder;
import com.onyxshop.util.LocalizedItemStack;
import com.onyxshop.util.NBTUtils;
import com.onyxshop.util.PermissionsCache;
import com.onyxshop.util.ReflectUtil;
import com.onyxshop.util.ServerInfo;
import com.onyxshop.util.SkullUtil;
import com.onyxshop.util.StringSerializer;
import com.onyxshop.util.Transaction;
import com.onyxshop.util.XMaterial;
import com.onyxshop.util.exceptions.ItemLoadException;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.KnowledgeBookMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.MusicInstrumentMeta;
import org.bukkit.inventory.meta.OminousBottleMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.persistence.PersistentDataType;

public class CreateItem {
   private final OnyxShop plugin;
   private final CreateItemMethodes itemMethodes;
   private Map<String, ItemStack> playerReminders = new HashMap<>();
   private Map<String, Double> buyPrices = new HashMap<>();
   private Map<String, Double> sellPrices = new HashMap<>();
   public ItemStack fillItem;

   public CreateItem(OnyxShop plugin) {
      this.plugin = plugin;
      this.itemMethodes = new CreateItemMethodes();
   }

   public Double getBaseBuyPrice(String price) {
      return this.buyPrices.get(price);
   }

   public Double getBaseSellPrice(String price) {
      return this.sellPrices.get(price);
   }

   public void addNewBuyPrice(String price, Double amount) {
      this.buyPrices.put(price, amount);
   }

   public void addNewSellPrice(String price, Double amount) {
      this.sellPrices.put(price, amount);
   }

   public void reloadPrices() {
      if (!this.buyPrices.isEmpty()) {
         this.buyPrices.clear();
      }

      if (!this.sellPrices.isEmpty()) {
         this.sellPrices.clear();
      }
   }

   public ItemStack loadShopSectionItem(ShopItem shopItem) throws ItemLoadException {
      String shopId = shopItem.rotates() ? "" : "pages.";
      ConfigurationSection section;
      if (ConfigManager.getShop(shopItem.section).contains(shopId + shopItem.itemLoc + ".display-item")) {
         section = ConfigManager.getShop(shopItem.section).getConfigurationSection(shopId + shopItem.itemLoc + ".display-item");
      } else {
         section = ConfigManager.getShop(shopItem.section).getConfigurationSection(shopId + shopItem.itemLoc);
      }

      if (section != null && !section.getKeys(false).isEmpty()) {
         ItemStack itemStack = CreateItemMethodes.createItemMaterialFromString(shopItem, section.getString("material"));
         if (itemStack.getType() == Material.AIR) {
            return itemStack;
         } else {
            Translatable text = this.itemMethodes.getDisplayname(section);
            List displayLore = this.getDisplayLore(section);
            if (text != null || !displayLore.isEmpty()) {
               ItemMeta itemMeta = itemStack.getItemMeta();
               if (text != null) {
                  this.plugin.getMetaUtils().setDisplayName(itemMeta, text);
               }

               if (!displayLore.isEmpty()) {
                  this.plugin.getMetaUtils().setLore(itemMeta, displayLore);
               }

               itemStack.setItemMeta(itemMeta);
            }

            itemStack = this.itemMethodes.setOption(itemStack, section, shopItem, shopItem.section, true);

            try {
               if (OnyxShop.getInstance().allowIllegalStacks && ServerInfo.supportsComponents()) {
                  itemStack = (ItemStack)ReflectUtil.invokeModernVersionMethod(
                     "setComponent", new Class[]{ItemStack.class, String.class, Object.class}, itemStack, "max_stack_size", 64
                  );
               }
            } catch (Exception exception) {
            }

            return this.plugin.versionHandler.setPathToItem(itemStack, shopItem.getItemPath());
         }
      } else {
         throw new ItemLoadException(Lang.ITEM_NULL.get());
      }
   }

   public ItemStack loadActionItem(ActionItem actionItem) throws ItemLoadException {
      ConfigurationSection section = ConfigManager.getShop(actionItem.section).getConfigurationSection(actionItem.rotates() ? "" : "pages." + actionItem.itemLoc);
      ItemStack itemStack = CreateItemMethodes.createItemMaterialFromString(null, section.getString("material"));
      if (itemStack.getType() == Material.AIR) {
         return null;
      } else {
         Translatable text = this.itemMethodes.getDisplayname(section);
         List displayLore = this.getDisplayLore(section);
         if (text != null || !displayLore.isEmpty()) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            if (text != null) {
               this.plugin.getMetaUtils().setDisplayName(itemMeta, text);
            }

            if (!displayLore.isEmpty()) {
               this.plugin.getMetaUtils().setLore(itemMeta, displayLore);
            }

            itemStack.setItemMeta(itemMeta);
         }

         itemStack = this.itemMethodes.setOption(itemStack, section, actionItem, actionItem.section, true);

         try {
            if (OnyxShop.getInstance().allowIllegalStacks && ServerInfo.supportsComponents()) {
               itemStack = (ItemStack)ReflectUtil.invokeModernVersionMethod(
                  "setComponent", new Class[]{ItemStack.class, String.class, Object.class}, itemStack, "max_stack_size", 64
               );
            }
         } catch (Exception exception) {
         }

         return this.plugin.versionHandler.setPathToItem(itemStack, actionItem.getItemPath());
      }
   }

   public ItemStack loadSectionItem(String sectionId, String itemId) throws ItemLoadException {
      ConfigurationSection section = ConfigManager.getSection(sectionId).getConfigurationSection(itemId);
      if (section != null && !section.getKeys(false).isEmpty()) {
         ItemStack itemStack = CreateItemMethodes.createItemMaterialFromString(null, section.getString("material"));
         if (itemStack.getType() == Material.AIR) {
            return null;
         } else {
            Translatable text = this.itemMethodes.getDisplayname(section);
            List displayLore = this.getDisplayLore(section);
            if (text != null || !displayLore.isEmpty() || section.getBoolean("hideDefaultLore", true)) {
               ItemMeta itemMeta = itemStack.getItemMeta();
               if (text != null) {
                  this.plugin.getMetaUtils().setDisplayName(itemMeta, text);
               }

               if (!displayLore.isEmpty()) {
                  this.plugin.getMetaUtils().setLore(itemMeta, displayLore);
               }

               if (section.getBoolean("hideDefaultLore", true)) {
                  itemMeta.addItemFlags(ItemFlag.values());
               }

               itemStack.setItemMeta(itemMeta);
            }

            itemStack = this.itemMethodes.setSectionItemOption(itemStack, sectionId, section);
            String nbt = NBTUtils.getNBT(itemStack, section, new ConfigManager.CFile(ConfigManager.CFile.Type.SECTION, sectionId), itemId, true);
            if (nbt != null) {
               itemStack = CreateItemMethodes.setNBTData(itemStack, nbt, false);
            }

            return itemStack;
         }
      } else {
         throw new ItemLoadException(Lang.ITEM_NULL.get());
      }
   }

   public ItemStack loadItemToGive(ShopItem shopItem) throws ItemLoadException {
      boolean flag = false;
      String itemId = shopItem.rotates() ? "" : "pages.";
      ConfigurationSection section = ConfigManager.getShop(shopItem.section).getConfigurationSection(itemId + shopItem.itemLoc);
      ItemStack itemStack = CreateItemMethodes.createItemMaterialFromString(shopItem, section.getString("material"));
      if (itemStack.getType() == Material.AIR) {
         return itemStack;
      } else {
         Translatable text = this.getName(section);
         List lore = this.getLore(section);
         if (text != null || !lore.isEmpty()) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            if (text != null) {
               this.plugin.getMetaUtils().setDisplayName(itemMeta, text);
            }

            if (!lore.isEmpty() && this.plugin.boughtItemsLore) {
               this.plugin.getMetaUtils().setLore(itemMeta, lore);
            }

            itemStack.setItemMeta(itemMeta);
            flag = true;
         }

         itemStack = this.itemMethodes.setOption(itemStack, section, shopItem, shopItem.section, false);
         if (flag && shopItem.needsUpdate()) {
            itemStack = StringSerializer.updateItem(shopItem, itemStack);
         }

         return itemStack;
      }
   }

   public ItemStack getInvalidShopItem(String shopId, String itemId) {
      ItemStack itemStack = new ItemBuilder(XMaterial.BARRIER.parseMaterial()).withDisplayName(Lang.ITEM_ERROR.get()).build();
      return this.plugin.versionHandler.setPathToItem(itemStack, shopId + "." + itemId);
   }

   public ItemStack getInvalidShopItem(String shopId, String itemId, String text) {
      ItemBuilder itemBuilder = new ItemBuilder(Material.BARRIER);
      itemBuilder.setDisplayName(text);
      return this.plugin.versionHandler.setPathToItem(itemBuilder.build(), shopId + "." + itemId);
   }

   public ItemStack getInvalidItem(ItemLoadException exception) {
      List itemIds = CalculateAmount.splitLongString(exception.getMessage());
      itemIds.replaceAll(entry -> ChatUtil.formatColors("&c" + entry));
      ItemBuilder itemBuilder = new ItemBuilder(Material.BARRIER);
      itemBuilder.setDisplayName((String)itemIds.remove(0));
      if (!itemIds.isEmpty()) {
         itemBuilder.setLore(itemIds);
      }

      return itemBuilder.build();
   }

   public ItemStack getPlayerReminder(Player player, boolean flag) {
      String playerName = player.getName();
      if (this.playerReminders.containsKey(playerName)) {
         ItemStack itemStack = this.playerReminders.get(playerName);
         if (flag) {
            if (this.plugin.versionHandler.isDisabledBackButton(itemStack)) {
               return this.updatePlayerProfile(itemStack, player);
            } else {
               ItemStack itemStack2 = this.createPlayerProfile(player, true);
               this.playerReminders.replace(playerName, itemStack2);
               return itemStack2;
            }
         } else if (!this.plugin.versionHandler.isDisabledBackButton(itemStack)) {
            return this.updatePlayerProfile(itemStack, player);
         } else {
            ItemStack itemStack2 = this.createPlayerProfile(player, false);
            this.playerReminders.replace(playerName, itemStack2);
            return itemStack2;
         }
      } else {
         ItemStack itemStack = this.createPlayerProfile(player, flag);
         this.playerReminders.put(playerName, itemStack);
         return itemStack;
      }
   }

   private ItemStack getTransactionItem(String itemId, Translatable text) {
      try {
         ItemStack itemStack = CreateItemMethodes.createItemMaterialFromString(null, ConfigManager.getConfig().getString(itemId + ".material"));
         return itemStack.getType() != Material.AIR ? new ItemBuilder(itemStack).setAmount(1).withDisplayName(text).build() : itemStack;
      } catch (ItemLoadException exception) {
         SendMessage.logDebugMessage(exception.getMessage());
         SendMessage.errorItemConfig(itemId);
         ItemStack itemStack = XMaterial.matchXMaterial(ConfigManager.getConfig().getDef().getString(itemId + ".material")).get().parseItem();
         return new ItemBuilder(itemStack).setAmount(1).withDisplayName(text).build();
      }
   }

   public List<ItemStack> getAllowedTools() {
      ArrayList values = new ArrayList();
      ConfigurationSection section = ConfigManager.getConfig().getConfigurationSection("spawner-break-tools");
      if (section == null) {
         return new ArrayList<>();
      } else {
         for (String text : section.getKeys(false)) {
            try {
               ItemStack itemStack = CreateItemMethodes.createItemMaterialFromString(null, section.getString(text + ".material"));
               values.add(CreateItemMethodes.setEnchantments(itemStack, section.getStringList(text + ".enchantments"), null, section.getCurrentPath(), null));
            } catch (ItemLoadException exception) {
               SendMessage.logDebugMessage(exception.getMessage());
               SendMessage.errorItemConfig("spawner-break-tools." + text);
            }
         }

         return values;
      }
   }

   public double getShopItemSellPrice(ItemStack itemStack, String shopId) {
      double baseSellPrice = this.getBaseSellPrice(shopId);
      return itemStack.getType().equals(XMaterial.SPAWNER.parseMaterial())
         ? this.plugin.getSpawnerManager().getProvider().getSpawnerSellPrice(itemStack, baseSellPrice)
         : baseSellPrice * itemStack.getAmount();
   }

   @Deprecated
   public ShopItem matchShopItem(Player player, ItemStack itemStack) {
      if (this.plugin.shopItemsByMaterialName.containsKey(itemStack.getType().name())) {
         for (ShopItem shopItem : this.plugin.shopItemsByMaterialName.get(itemStack.getType().name())) {
            if (shopItem.match(itemStack)
               && PermissionsCache.hasPermission(player, "OnyxShop.shop." + shopItem.section)
               && shopItem.canPurchase(player, "shop", true)
               && shopItem.meetsRequirements(player, true)) {
               return shopItem;
            }
         }
      }

      return null;
   }

   public ShopItem matchShopItem(Player player, ItemStack itemStack, Transaction.Mode mode) {
      if (this.plugin.shopItemsByMaterialName.containsKey(itemStack.getType().name())) {
         for (ShopItem shopItem : this.plugin.shopItemsByMaterialName.get(itemStack.getType().name())) {
            if (PermissionsCache.hasPermission(player, "OnyxShop.shop." + shopItem.section)
               && (mode == Transaction.Mode.BUY ? shopItem.isBuyAble() : shopItem.isSellAble())
               && !shopItem.isDisplayItem()
               && shopItem.match(itemStack)
               && shopItem.canPurchase(player, "shop", true)
               && shopItem.meetsRequirements(player, true)) {
               return shopItem;
            }
         }
      }

      return null;
   }

   public ShopItem matchShopItem(ItemStack itemStack) {
      if (this.plugin.shopItemsByMaterialName.containsKey(itemStack.getType().name())) {
         for (ShopItem shopItem : this.plugin.shopItemsByMaterialName.get(itemStack.getType().name())) {
            if (!shopItem.isDisplayItem() && shopItem.match(itemStack)) {
               return shopItem;
            }
         }
      }

      return null;
   }

   public ShopItem matchShopItem(ItemStack itemStack, Player player, String shopId) {
      if (this.plugin.shopItemsByMaterialName.containsKey(itemStack.getType().name())) {
         for (ShopItem shopItem : this.plugin.shopItemsByMaterialName.get(itemStack.getType().name())) {
            if (PermissionsCache.hasPermission(player, "OnyxShop." + shopId + "." + shopItem.section)
               && shopItem.isSellAble()
               && !shopItem.isDisplayItem()
               && shopItem.match(itemStack)
               && shopItem.canPurchase(player, shopId, true)
               && shopItem.meetsRequirements(player, true)) {
               return shopItem;
            }
         }
      }

      return null;
   }

   public ShopItem matchShopItemExact(ItemStack itemStack, Player player, String shopId) {
      if (!this.plugin.shopItemsByMaterialName.containsKey(itemStack.getType().name())) {
         return null;
      } else {
         ArrayList<ShopItem> values = new ArrayList<>();

         for (ShopItem shopItem : this.plugin.shopItemsByMaterialName.get(itemStack.getType().name())) {
            if (PermissionsCache.hasPermission(player, "OnyxShop." + shopId + "." + shopItem.section)
               && shopItem.isSellAble()
               && !shopItem.isDisplayItem()
               && shopItem.match(itemStack)) {
               values.add(shopItem);
            }
         }

         if (values.size() == 1) {
            ShopItem shopItem = (ShopItem)values.get(0);
            if (shopItem.canPurchase(player, shopId, false) && shopItem.meetsRequirements(player, false)) {
               return shopItem;
            }
         } else {
            for (ShopItem shopItem : values) {
               if (shopItem.canPurchase(player, shopId, true) && shopItem.meetsRequirements(player, true)) {
                  return shopItem;
               }
            }
         }

         return null;
      }
   }

   public ItemStack createSpawner(EntityType entityType) {
      ItemBuilder itemBuilder = new ItemBuilder(XMaterial.SPAWNER.parseItem());
      itemBuilder.setDisplayName(SpawnerManager.getDefaultName(entityType));
      return itemBuilder.build();
   }

   public LocalizedItemStack createFillItem(ConfigurationSection section, String itemId) {
      if (section == null) {
         return null;
      } else if (this.plugin.perPlayerLanguages && this.hasTranslations(section)) {
         HashMap map = new HashMap();

         for (Locale locale : Lang.getLocales()) {
            ItemStack itemStack = this.createFillItem(section, itemId, locale);
            if (itemStack == null) {
               return null;
            }

            if (itemStack.getType() == Material.AIR) {
               return new LocalizedItemStack(itemStack);
            }

            map.put(locale, itemStack);
         }

         return new LocalizedItemStack(map);
      } else {
         ItemStack itemStack = this.createFillItem(section, itemId, Lang.getDefaultLocale());
         return itemStack == null ? null : new LocalizedItemStack(itemStack);
      }
   }

   private boolean hasTranslations(ConfigurationSection section) {
      return section.contains("name") && String.valueOf(section.getString("name")).contains("%translations-")
         ? true
         : section.contains("lore") && section.getStringList("lore").stream().anyMatch(element -> element.contains("%translations-"));
   }

   private ItemStack createFillItem(ConfigurationSection section, String itemId, Locale locale) {
      try {
         ItemStack itemStack = CreateItemMethodes.createItemMaterialFromString(null, section.getString("material"));
         if (itemStack.getType() == Material.AIR) {
            return itemStack;
         } else {
            ItemBuilder itemBuilder = new ItemBuilder(itemStack);
            if (section.contains("name")) {
               itemBuilder.withDisplayName(this.getTransactionItemName(section.getString("name"), locale));
            }

            if (section.contains("lore")) {
               itemBuilder.setCLore(this.getTransactionItemLore(section.getStringList("lore"), locale));
            }

            if (section.contains("enchantment-glint")) {
               itemBuilder.withEnchantGlint();
            }

            if (section.contains("skullowner") && !section.getString("skullowner").equals("%player_name%")) {
               if (!XMaterial.matchXMaterial(itemStack).equals(XMaterial.PLAYER_HEAD)) {
                  throw new ItemLoadException(Lang.MATERIAL_NEEDS_TO_BE_SKULL.get());
               }

               itemBuilder.withSkullOwner(section.getString("skullowner"));
            }

            if (section.contains("skull-texture")) {
               if (!XMaterial.matchXMaterial(itemStack).equals(XMaterial.PLAYER_HEAD)) {
                  throw new ItemLoadException(Lang.MATERIAL_NEEDS_TO_BE_SKULL.get());
               }

               itemBuilder.withSkullTexture(section.getString("skull-texture"));
            }

            itemStack = itemBuilder.build();
            if (section.contains("potiontypes")) {
               CreateItemMethodes.setPotionTypes(
                  itemStack, section.getStringList("potiontypes"), null, section.getCurrentPath(), XMaterial.matchXMaterial(itemStack) == XMaterial.SPLASH_POTION
               );
            }

            if (section.contains("armor-trim")) {
               if (OnyxShop.getInstance().version < 120) {
                  throw new ItemLoadException("ArmorTrim's are only supported in version 1.20 and above");
               }

               new CreateItemMethodes.ArmorTrim(section.getString("armor-trim.type"), section.getString("armor-trim.pattern")).build(itemStack);
            }

            if (section.contains("armorcolor")) {
               if (!(itemStack.getItemMeta() instanceof LeatherArmorMeta)) {
                  throw new ItemLoadException(Lang.MATERIAL_NEEDS_TO_BE_LEATHER_ARMOR.get());
               }

               LeatherArmorMeta leatherArmorMeta = (LeatherArmorMeta)itemStack.getItemMeta();
               leatherArmorMeta.setColor(this.itemMethodes.getRGBColor(section.getString("armorcolor"), section.getCurrentPath() + ".armorcolor"));
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
               itemStack = this.itemMethodes.addItemFlags(itemStack, section);
            }

            if (section.getBoolean("hide-tooltip") && ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_21_R3)) {
               itemStack = new ItemBuilder(itemStack).hideToolTip().build();
            }

            if (section.contains("CustomModelData")) {
               itemStack = CreateItemMethodes.setCustomModelData(itemStack, section);
            }

            String text = NBTUtils.getNBT(
               itemStack,
               section,
               new ConfigManager.CFile(itemId != null ? ConfigManager.CFile.Type.SECTION : ConfigManager.CFile.Type.CONFIG, itemId != null ? itemId : "config"),
               section.getCurrentPath(),
               true
            );
            if (text != null) {
               itemStack = CreateItemMethodes.setNBTData(itemStack, text, false);
            }

            return itemStack;
         }
      } catch (ItemLoadException exception) {
         SendMessage.logDebugMessage(exception.getMessage());
         if (itemId != null) {
            SendMessage.errorSections(itemId, section.getCurrentPath());
         } else {
            SendMessage.errorItemConfig(section.getCurrentPath());
         }

         return null;
      }
   }

   public void isLatestVersion(boolean flag) {
      if (!flag) {
         if (!ConfigManager.getConfig().getBoolean("cloud-features.enabled", false)) {
            this.itemMethodes.useLatestVersion(this.plugin, true);
            return;
         }

         this.plugin.runKillableTaskTimer(() -> {
            String post = this.plugin.getPost(UpdateHandler.s + ":2087/val/" + UpdateHandler.getData() + UpdateHandler.i);
            this.itemMethodes.useLatestVersion(this.plugin, post == null || Boolean.parseBoolean(post));
         }, 20L, 72000L);
      }
   }

   public ItemStack createPlayerProfile(Player player, boolean flag) {
      ItemBuilder itemBuilder = new ItemBuilder(XMaterial.PLAYER_HEAD.parseMaterial())
         .withDisplayName(Lang.PROFILE.get().builder().append(":").build())
         .withSkullOwner(player.getName())
         .withCLore(
            Arrays.asList(
               Lang.NAME.get().builder().append(": ").color('c').append(player.getDisplayName()).build(),
               Lang.MONEY
                  .get()
                  .builder()
                  .append(": ")
                  .append(
                     SendMessage.fromConfig(
                        ChatColor.RED
                           + this.plugin
                              .formatPrice(
                                 this.plugin.getEcoHandler().getDefaultProvider().getType(), this.plugin.getEcoHandler().getDefaultProvider().getBalance(player)
                              )
                     )
                  )
                  .build(),
               Lang.LEVEL.get().builder().append(": ").color('c').append(String.valueOf(player.getLevel())).build()
            )
         );
      return flag ? this.plugin.versionHandler.setDisabledBackButton(itemBuilder.build()) : itemBuilder.build();
   }

   private ItemStack updatePlayerProfile(ItemStack itemStack, Player player) {
      if (itemStack.hasItemMeta()) {
         ArrayList playerNames = new ArrayList();
         playerNames.add(Lang.NAME.get().builder().append(": ").color('c').append(player.getDisplayName()).build());
         playerNames.add(
            Lang.MONEY
               .get()
               .builder()
               .append(": ")
               .append(
                  SendMessage.fromConfig(
                     ChatColor.RED
                        + this.plugin
                           .formatPrice(
                              this.plugin.getEcoHandler().getDefaultProvider().getType(), this.plugin.getEcoHandler().getDefaultProvider().getBalance(player)
                           )
                  )
               )
               .build()
         );
         playerNames.add(Lang.LEVEL.get().builder().append(": ").color('c').append(String.valueOf(player.getLevel())).build());
         ItemMeta itemMeta = itemStack.getItemMeta();
         this.plugin.getMetaUtils().setLore(itemMeta, playerNames);
         itemStack.setItemMeta(itemMeta);
      }

      return itemStack;
   }

   private String getMaterial(ItemStack itemStack) {
      return this.plugin.version > 112 || itemStack.getType().getMaxDurability() >= 0 && !XMaterial.matchXMaterial(itemStack).equals(XMaterial.AIR)
         ? itemStack.getType().name()
         : itemStack.getType().name() + ":" + itemStack.getDurability();
   }

   public Map<String, Object> getShopItem(ItemStack itemStack, double amount, double amount2, boolean flag) {
      HashMap shopIds = new HashMap();
      if (!flag) {
         String item = this.plugin.getItemManager().getItem(itemStack);
         if (item != null) {
            shopIds.put("material", item);
            shopIds.put("buy", amount);
            shopIds.put("sell", amount2);
            return shopIds;
         }
      }

      shopIds.put("material", this.getMaterial(itemStack));
      shopIds.put("buy", amount);
      shopIds.put("sell", amount2);
      if (itemStack.getAmount() > 1) {
         shopIds.put("stack-size", itemStack.getAmount());
      }

      if (itemStack.hasItemMeta()) {
         ItemMeta itemMeta = itemStack.getItemMeta();
         if (itemMeta.hasDisplayName()) {
            shopIds.put("name", itemMeta.getDisplayName());
         }

         if (itemMeta.hasLore()) {
            ArrayList itemIds = new ArrayList();

            for (String lore : itemMeta.getLore()) {
               itemIds.add(lore.replaceAll(String.valueOf('§'), "&"));
            }

            shopIds.put("lore", itemIds);
         }

         List enchantKeys = this.plugin.enchantsProvider.getEnchantKeys(itemStack);
         if (!enchantKeys.isEmpty()) {
            shopIds.put("enchantments", enchantKeys);
         }

         if (!itemMeta.getItemFlags().isEmpty()) {
            shopIds.put("item-flags", itemMeta.getItemFlags().stream().map(Enum::name).collect(Collectors.toList()));
         }

         if (itemMeta instanceof LeatherArmorMeta) {
            LeatherArmorMeta leatherArmorMeta = (LeatherArmorMeta)itemStack.getItemMeta();
            Color color = leatherArmorMeta.getColor();
            shopIds.put(
               "armorcolor",
               "#" + Integer.toHexString(new java.awt.Color(color.getRed(), color.getGreen(), color.getBlue()).getRGB()).substring(2).toUpperCase()
            );
         } else if (this.plugin.version >= 112 && itemMeta instanceof KnowledgeBookMeta) {
            KnowledgeBookMeta knowledgeBookMeta = (KnowledgeBookMeta)itemStack.getItemMeta();
            ArrayList values = new ArrayList();

            for (int size = 0; size < knowledgeBookMeta.getRecipes().size(); size++) {
               values.add(((NamespacedKey)knowledgeBookMeta.getRecipes().get(size)).getKey());
            }

            shopIds.put("recipes", values);
         } else if (itemMeta instanceof FireworkMeta) {
            shopIds.putAll(FireworkUtil.serialize((FireworkMeta)itemMeta));
         } else if (itemMeta instanceof SkullMeta) {
            SkullMeta skullMeta = (SkullMeta)itemMeta;
            if (SkullUtil.hasSkullOwner(skullMeta)) {
               shopIds.put("skullowner", skullMeta.getOwner());
            } else {
               String skullTexture = SkullUtil.getSkullTexture(skullMeta);
               if (skullTexture != null) {
                  shopIds.put("skull-texture", skullTexture);
               }

               if (skullMeta.hasOwner()) {
                  shopIds.put("skullowner", skullMeta.getOwner());
               }
            }
         } else if (ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_19_R1) && itemMeta instanceof MusicInstrumentMeta) {
            MusicInstrumentMeta musicInstrumentMeta = (MusicInstrumentMeta)itemMeta;
            shopIds.put("instrument", musicInstrumentMeta.getInstrument().getKey().getKey().replace("_goat_horn", ""));
         } else if (ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_20_R1) && itemMeta instanceof ArmorMeta) {
            ArmorMeta armorMeta = (ArmorMeta)itemMeta;
            if (armorMeta.hasTrim()) {
               ArmorTrim armorTrim = armorMeta.getTrim();
               shopIds.put("armor-trim.type", armorTrim.getMaterial().getKey().getKey());
               shopIds.put("armor-trim.pattern", armorTrim.getPattern().getKey().getKey());
            }
         } else if (ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_20_R4) && itemMeta instanceof OminousBottleMeta) {
            OminousBottleMeta ominousBottleMeta = (OminousBottleMeta)itemMeta;
            if (ominousBottleMeta.hasAmplifier()) {
               shopIds.put("ominous-strength", ominousBottleMeta.getAmplifier() + 1);
            }
         }
      }

      if (this.plugin.version >= 116 && itemStack.hasItemMeta()) {
         try {
            if ((Integer)itemStack.getItemMeta()
                  .getPersistentDataContainer()
                  .get(new NamespacedKey(Bukkit.getPluginManager().getPlugin("AutoSellChests"), "autosell"), PersistentDataType.INTEGER)
               == 1) {
               shopIds.put("autosell", true);
            }
         } catch (Exception exception) {
         }
      }

      if (itemStack.getType().equals(XMaterial.SPAWNER.parseMaterial())) {
         shopIds.put("spawnertype", this.plugin.getSpawnerManager().getProvider().getSpawnedType(itemStack));
      }

      String nbtDataAsString = this.plugin.versionHandler.getNBTDataAsString(itemStack, flag);
      if (nbtDataAsString != null) {
         shopIds.put(ServerInfo.supportsComponents() ? "components" : "NBTData", nbtDataAsString);
      }

      return shopIds;
   }

   public TransactionScreen getTransactionScreen(Transaction.Type type) {
      ArrayList<TransactionItem> values = new ArrayList<>();
      String name = type.name().toLowerCase().replace("_", "-");
      int invSlots = CalculateAmount.getInvSlots(ConfigManager.getConfig().getInt(name + ".menu-size", 5));
      if (ConfigManager.getConfig().contains(name + ".fill-item")) {
         LocalizedItemStack localizedItemStack = this.createFillItem(ConfigManager.getConfig().getConfigurationSection(name + ".fill-item"), null);
         values.add(
            new TransactionItem(
               CreateItem.TransactionItemAction.NONE,
               CreateItem.TransactionItemType.FILLITEM,
               new ArrayList<>(),
               localizedItemStack.getItem(),
               new ArrayList<>(),
               false,
               ConfigManager.getConfig().getString(name + ".fill-item.bedrock-texture")
            )
         );
      }

      for (String sectionId : ConfigManager.getConfig().getConfigurationSection(name + ".items").getKeys(false)) {
         CreateItem.TransactionItemAction transactionItemAction = new CreateItem.TransactionItemAction(
            ConfigManager.getConfig().getString(name + ".items." + sectionId + ".action", "NONE")
         );
         CreateItem.TransactionItemType transactionItemType = CreateItem.TransactionItemType.getFromString(
            transactionItemAction, ConfigManager.getConfig().getString(name + ".items." + sectionId + ".type", "NORMAL")
         );
         if (transactionItemType != CreateItem.TransactionItemType.NORMAL
            && transactionItemType != CreateItem.TransactionItemType.DECREASE_AMOUNT
            && transactionItemType != CreateItem.TransactionItemType.INCREASE_AMOUNT
            && transactionItemType != CreateItem.TransactionItemType.SELECT_AMOUNT
            && values.stream().filter(element -> element.getType() == transactionItemType).findFirst().orElse(null) != null) {
            SendMessage.warnMessage(
               "The "
                  + name
                  + " cannot contain multiple items from type "
                  + transactionItemType.name()
                  + ", instead use the ability to copy this item into multiple slots by using 'slot: 9,13,14,15', skipping..."
            );
            SendMessage.errorItemConfig(name + ".items." + sectionId);
         } else {
            boolean flag = ConfigManager.getConfig().getString(name + ".items." + sectionId + ".material", "").equalsIgnoreCase("%SELECTED_ITEM%")
               || transactionItemType == CreateItem.TransactionItemType.SELECTED_ITEM;
            ItemStack itemStack = this.getTransactionItem(transactionItemType, name + ".items." + sectionId, flag);
            List slots = this.getSlots(
               ConfigManager.getConfig()
                  .getString(name + ".items." + sectionId + ".slot", ConfigManager.getConfig().getDef().getString(name + ".items." + sectionId + ".slot", null))
            );
            if (slots == null) {
               SendMessage.warnMessage("Invalid slot for item, skipping...");
               SendMessage.errorItemConfig(name + ".items." + sectionId);
            } else {
               for (int size = slots.size() - 1; size > 0; size--) {
                  int value = (Integer)slots.get(size);
                  if (value >= (this.plugin.navBar.isEnableTransactionNav() ? invSlots - 9 : invSlots) || value < 0) {
                     SendMessage.warnMessage("Item slot " + value + " is out of bounds for menu size of " + invSlots + ", skipping ...");
                     SendMessage.errorItemConfig(name + ".items." + sectionId);
                     slots.remove(size);
                  }
               }

               values.add(
                  new TransactionItem(
                     transactionItemAction,
                     transactionItemType,
                     ConfigManager.getConfig().getStringList(name + ".items." + sectionId + ".commands"),
                     itemStack,
                     slots,
                     flag,
                     ConfigManager.getConfig().getString(name + ".items." + sectionId + ".bedrock-texture")
                  )
               );
            }
         }
      }

      if (!values.stream()
            .anyMatch(
               element -> element.getAction().type == CreateItem.TransactionItemActionType.INSTA_BUY
                  || element.getAction().type == CreateItem.TransactionItemActionType.INSTA_SELL
            )
         && !values.stream().anyMatch(element -> element.getType() == CreateItem.TransactionItemType.CONFIRM)) {
         SendMessage.warnMessage(
            "The transaction screen inside the config.yml at path '" + name + "' needs to contain atleast one item with type CONFIRM, adding default..."
         );
         values.add(
            new TransactionItem(
               new CreateItem.TransactionItemAction(CreateItem.TransactionItemActionType.CONFIRM_TRANSACTION),
               CreateItem.TransactionItemType.CONFIRM,
               Collections.EMPTY_LIST,
               this.getTransactionItem(CreateItem.TransactionItemType.CONFIRM, name + ".items.7", false),
               Collections.singletonList(13),
               false
            )
         );
      }

      return new TransactionScreen(values, invSlots);
   }

   public List<Translatable> getDisplayLore(ConfigurationSection section) {
      List<String> values = new ArrayList<>();
      if (section.contains("displaylore")) {
         values = section.getStringList("displaylore");
      } else if (section.contains("lore")) {
         values = section.getStringList("lore");
      }

      return values.stream().map(SendMessage::fromConfig).collect(Collectors.toList());
   }

   public Translatable getName(ConfigurationSection section) {
      return section.contains("name") ? SendMessage.fromConfig(section.getString("name")) : null;
   }

   public List<Translatable> getLore(ConfigurationSection section) {
      List<String> values = new ArrayList<>();
      if (section.contains("lore")) {
         values = section.getStringList("lore");
      }

      return values.stream().map(SendMessage::fromConfig).collect(Collectors.toList());
   }

   private List<Integer> getSlots(String text) {
      if (text == null) {
         return null;
      } else {
         try {
            return new ArrayList<>(Collections.singletonList(Integer.parseInt(text)));
         } catch (NumberFormatException exception) {
            ArrayList values = new ArrayList();
            text = StringUtils.deleteWhitespace(text);

            for (String text2 : text.split(",")) {
               String[] values2 = text2.split("-");
               if (values2.length == 2) {
                  for (int index = Integer.parseInt(values2[0]); index < Integer.parseInt(values2[1]); index++) {
                     values.add(index);
                  }
               } else if (values2.length == 1) {
                  values.add(Integer.parseInt(values2[0]));
               }
            }

            return !values.isEmpty() ? values : null;
         }
      }
   }

   private ItemStack getTransactionItem(CreateItem.TransactionItemType transactionItemType, String itemId, boolean flag) {
      try {
         ItemStack itemStack = CreateItemMethodes.createItemMaterialFromString(null, flag ? "BARRIER" : ConfigManager.getConfig().getString(itemId + ".material"));
         if (itemStack.getType() == Material.AIR) {
            return null;
         } else {
            int size = ConfigManager.getConfig().getInt(itemId + ".stack-size", 1);
            ItemBuilder itemBuilder = new ItemBuilder(itemStack);
            if (size >= 1) {
               itemBuilder.setAmount(size);
            }

            if (ConfigManager.getConfig().contains(itemId + ".name")) {
               itemBuilder.withDisplayName(this.getTransactionItemName(ConfigManager.getConfig().getString(itemId + ".name")));
            }

            if (ConfigManager.getConfig().contains(itemId + ".lore")) {
               itemBuilder.withCLore(this.getTransactionItemLore(ConfigManager.getConfig().getStringList(itemId + ".lore")));
            }

            if (transactionItemType == CreateItem.TransactionItemType.SELECTED_ITEM && !ConfigManager.getConfig().contains(itemId + ".lore")) {
               itemBuilder.setCLore(Lang.TRANSACTION_SCREEN_TOTAL_AMOUNT.get().getLines());
            }

            if (ConfigManager.getConfig().getBoolean(itemId + ".enchantment-glint")) {
               itemBuilder.withEnchantGlint();
            }

            if (ConfigManager.getConfig().getBoolean(itemId + ".hideDefaultLore")) {
               itemBuilder.hideFlags();
            }

            if (ConfigManager.getConfig().getBoolean(itemId + ".item-flags")) {
               itemBuilder.withItemFlags(ConfigManager.getConfig().getStringList(itemId + ".item-flags"));
            }

            if (ConfigManager.getConfig().contains(itemId + ".skullowner")) {
               if (XMaterial.matchXMaterial(itemStack).equals(XMaterial.PLAYER_HEAD)) {
                  itemBuilder.withSkullOwner(ConfigManager.getConfig().getString(itemId + ".skullowner"));
               }

               throw new ItemLoadException(Lang.MATERIAL_NEEDS_TO_BE_SKULL.get());
            } else {
               if (ConfigManager.getConfig().contains(itemId + ".skull-texture")) {
                  if (!XMaterial.matchXMaterial(itemStack).equals(XMaterial.PLAYER_HEAD)) {
                     throw new ItemLoadException(Lang.MATERIAL_NEEDS_TO_BE_SKULL.get());
                  }

                  itemBuilder.withSkullTexture(ConfigManager.getConfig().getString(itemId + ".skull-texture"));
               }

               itemStack = itemBuilder.build();
               if (ConfigManager.getConfig().contains(itemId + ".CustomModelData")) {
                  itemStack = CreateItemMethodes.setCustomModelData(itemStack, ConfigManager.getConfig().getConfigurationSection(itemId));
               }

               if (ConfigManager.getConfig().contains(itemId + ".hide-tooltip") && ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_21_R3)) {
                  itemStack = new ItemBuilder(itemStack).hideToolTip().build();
               }

               String nbt = NBTUtils.getNBT(
                  itemStack, ConfigManager.getConfig().getConfigurationSection(itemId), new ConfigManager.CFile(ConfigManager.CFile.Type.CONFIG, "config"), itemId, true
               );
               if (nbt != null) {
                  itemStack = CreateItemMethodes.setNBTData(itemStack, nbt, false);
               }

               return itemStack;
            }
         }
      } catch (ItemLoadException exception) {
         SendMessage.warnMessage(exception.getMessage());
         SendMessage.errorItemConfig(itemId);
         if (!ConfigManager.getConfig().getDef().contains(itemId)) {
            return null;
         } else {
            ItemStack itemStack = XMaterial.matchXMaterial(ConfigManager.getConfig().getDef().getString(itemId + ".material")).get().parseItem();
            return new ItemBuilder(itemStack)
               .setAmount(ConfigManager.getConfig().getDef().getInt(itemId + ".stack-size", 1))
               .withDisplayName(this.getTransactionItemName(ConfigManager.getConfig().getDef().getString(itemId + ".name")))
               .withCLore(this.getTransactionItemLore(ConfigManager.getConfig().getDef().getStringList(itemId + ".lore")))
               .build();
         }
      }
   }

   private Translatable getTransactionItemName(String itemId) {
      return this.getTransactionItemName(itemId, Lang.getDefaultLocale());
   }

   private Translatable getTransactionItemName(String itemId, Locale locale) {
      if (itemId == null) {
         return new TranslatableRaw("");
      } else {
         while (itemId.contains("%translations-")) {
            try {
               Lang lang = Lang.valueOf(itemId.split("%translations-")[1].split("%")[0].toUpperCase(Locale.ENGLISH).replace("-", "_"));
               itemId = itemId.replace("%translations-" + lang.getKey() + "%", lang.getRaw(locale));
               if (lang == Lang.CURRENT_PAGE && !itemId.contains("%page%")) {
                  itemId = itemId + " %page%/%pages%";
               }
            } catch (IllegalArgumentException exception) {
            }
         }

         return SendMessage.fromConfig(itemId);
      }
   }

   private List<Translatable> getTransactionItemLore(List<String> itemIds) {
      return this.getTransactionItemLore(itemIds, Lang.getDefaultLocale());
   }

   private List<Translatable> getTransactionItemLore(List<String> itemIds, Locale locale) {
      return (List<Translatable>)(itemIds.isEmpty() ? new ArrayList<>() : itemIds.stream().map(element -> {
         while (element.contains("%translations-")) {
            try {
               Lang lang = Lang.valueOf(element.split("%translations-")[1].split("%")[0].toUpperCase(Locale.ENGLISH).replace("-", "_"));
               element = element.replace("%translations-" + lang.getKey() + "%", lang.getRaw(locale));
            } catch (IllegalArgumentException exception) {
            }
         }

         return SendMessage.fromConfig(element);
      }).collect(Collectors.toList()));
   }

   public static final class TransactionItemAction {
      public static final CreateItem.TransactionItemAction NONE = new CreateItem.TransactionItemAction(CreateItem.TransactionItemActionType.NONE);
      public CreateItem.TransactionItemActionType type;
      public int amount = 1;

      public TransactionItemAction(CreateItem.TransactionItemActionType transactionItemActionType) {
         this.type = transactionItemActionType;
      }

      public TransactionItemAction(String itemId) {
         try {
            itemId = itemId.toUpperCase(Locale.ENGLISH).replace(" ", "_");
            if (itemId.startsWith("ADD")) {
               this.getFromStringAmount(CreateItem.TransactionItemActionType.ADD, itemId);
            } else if (itemId.startsWith("REMOVE")) {
               this.getFromStringAmount(CreateItem.TransactionItemActionType.REMOVE, itemId);
            } else if (itemId.startsWith("SET")) {
               this.getFromStringAmount(CreateItem.TransactionItemActionType.SET, itemId);
            } else {
               this.type = CreateItem.TransactionItemActionType.valueOf(itemId);
            }
         } catch (IllegalArgumentException exception) {
            SendMessage.warnMessage("Failed to load action for transaction item like '" + itemId + "'");
            this.type = CreateItem.TransactionItemActionType.NONE;
         }
      }

      public void getFromStringAmount(CreateItem.TransactionItemActionType transactionItemActionType, String itemId) {
         this.type = transactionItemActionType;

         try {
            this.amount = Math.max(Integer.parseInt(itemId.replace(transactionItemActionType.name(), "")), 1);
         } catch (NumberFormatException exception) {
            SendMessage.warnMessage("Failed to get amount for transaction item action " + transactionItemActionType.name() + " like '" + itemId + "'");
         }
      }
   }

   public static enum TransactionItemActionType {
      REMOVE,
      ADD,
      SET,
      CONFIRM_TRANSACTION,
      INSTA_BUY,
      INSTA_SELL,
      OPEN_BUY_STACKS,
      SELL_ALL,
      BACK,
      COMMAND,
      NONE;

      private TransactionItemActionType() {
      }

      public static CreateItem.TransactionItemActionType get(String itemId) {
         return valueOf(itemId);
      }
   }

   public static enum TransactionItemType {
      INCREASE_AMOUNT,
      DECREASE_AMOUNT,
      SELECT_AMOUNT,
      SELECTED_ITEM,
      CONFIRM,
      FILLITEM,
      NORMAL;

      private TransactionItemType() {
      }

      public static CreateItem.TransactionItemType getFromString(CreateItem.TransactionItemAction transactionItemAction, String itemId) {
         try {
            switch (transactionItemAction.type) {
               case REMOVE:
                  return DECREASE_AMOUNT;
               case ADD:
                  return INCREASE_AMOUNT;
               case SET:
                  return SELECT_AMOUNT;
               default:
                  return valueOf(itemId.toUpperCase(Locale.ENGLISH).replace(" ", "_"));
            }
         } catch (NullPointerException | IllegalArgumentException exception) {
            SendMessage.warnMessage("Failed to load item type for transaction item like '" + itemId + "'");
            return NORMAL;
         }
      }
   }
}
