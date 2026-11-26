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

   public CreateItem(OnyxShop var1) {
      this.plugin = var1;
      this.itemMethodes = new CreateItemMethodes();
   }

   public Double getBaseBuyPrice(String var1) {
      return this.buyPrices.get(var1);
   }

   public Double getBaseSellPrice(String var1) {
      return this.sellPrices.get(var1);
   }

   public void addNewBuyPrice(String var1, Double var2) {
      this.buyPrices.put(var1, var2);
   }

   public void addNewSellPrice(String var1, Double var2) {
      this.sellPrices.put(var1, var2);
   }

   public void reloadPrices() {
      if (!this.buyPrices.isEmpty()) {
         this.buyPrices.clear();
      }

      if (!this.sellPrices.isEmpty()) {
         this.sellPrices.clear();
      }
   }

   public ItemStack loadShopSectionItem(ShopItem var1) throws ItemLoadException {
      String var3 = var1.rotates() ? "" : "pages.";
      ConfigurationSection var2;
      if (ConfigManager.getShop(var1.section).contains(var3 + var1.itemLoc + ".display-item")) {
         var2 = ConfigManager.getShop(var1.section).getConfigurationSection(var3 + var1.itemLoc + ".display-item");
      } else {
         var2 = ConfigManager.getShop(var1.section).getConfigurationSection(var3 + var1.itemLoc);
      }

      if (var2 != null && !var2.getKeys(false).isEmpty()) {
         ItemStack var4 = CreateItemMethodes.createItemMaterialFromString(var1, var2.getString("material"));
         if (var4.getType() == Material.AIR) {
            return var4;
         } else {
            Translatable var5 = this.itemMethodes.getDisplayname(var2);
            List var6 = this.getDisplayLore(var2);
            if (var5 != null || !var6.isEmpty()) {
               ItemMeta var7 = var4.getItemMeta();
               if (var5 != null) {
                  this.plugin.getMetaUtils().setDisplayName(var7, var5);
               }

               if (!var6.isEmpty()) {
                  this.plugin.getMetaUtils().setLore(var7, var6);
               }

               var4.setItemMeta(var7);
            }

            var4 = this.itemMethodes.setOption(var4, var2, var1, var1.section, true);

            try {
               if (OnyxShop.getInstance().allowIllegalStacks && ServerInfo.supportsComponents()) {
                  var4 = (ItemStack)ReflectUtil.invokeModernVersionMethod(
                     "setComponent", new Class[]{ItemStack.class, String.class, Object.class}, var4, "max_stack_size", 64
                  );
               }
            } catch (Exception var8) {
            }

            return this.plugin.versionHandler.setPathToItem(var4, var1.getItemPath());
         }
      } else {
         throw new ItemLoadException(Lang.ITEM_NULL.get());
      }
   }

   public ItemStack loadActionItem(ActionItem var1) throws ItemLoadException {
      ConfigurationSection var2 = ConfigManager.getShop(var1.section).getConfigurationSection(var1.rotates() ? "" : "pages." + var1.itemLoc);
      ItemStack var3 = CreateItemMethodes.createItemMaterialFromString(null, var2.getString("material"));
      if (var3.getType() == Material.AIR) {
         return null;
      } else {
         Translatable var4 = this.itemMethodes.getDisplayname(var2);
         List var5 = this.getDisplayLore(var2);
         if (var4 != null || !var5.isEmpty()) {
            ItemMeta var6 = var3.getItemMeta();
            if (var4 != null) {
               this.plugin.getMetaUtils().setDisplayName(var6, var4);
            }

            if (!var5.isEmpty()) {
               this.plugin.getMetaUtils().setLore(var6, var5);
            }

            var3.setItemMeta(var6);
         }

         var3 = this.itemMethodes.setOption(var3, var2, var1, var1.section, true);

         try {
            if (OnyxShop.getInstance().allowIllegalStacks && ServerInfo.supportsComponents()) {
               var3 = (ItemStack)ReflectUtil.invokeModernVersionMethod(
                  "setComponent", new Class[]{ItemStack.class, String.class, Object.class}, var3, "max_stack_size", 64
               );
            }
         } catch (Exception var7) {
         }

         return this.plugin.versionHandler.setPathToItem(var3, var1.getItemPath());
      }
   }

   public ItemStack loadSectionItem(String var1, String var2) throws ItemLoadException {
      ConfigurationSection var3 = ConfigManager.getSection(var1).getConfigurationSection(var2);
      if (var3 != null && !var3.getKeys(false).isEmpty()) {
         ItemStack var4 = CreateItemMethodes.createItemMaterialFromString(null, var3.getString("material"));
         if (var4.getType() == Material.AIR) {
            return null;
         } else {
            Translatable var5 = this.itemMethodes.getDisplayname(var3);
            List var6 = this.getDisplayLore(var3);
            if (var5 != null || !var6.isEmpty() || var3.getBoolean("hideDefaultLore", true)) {
               ItemMeta var7 = var4.getItemMeta();
               if (var5 != null) {
                  this.plugin.getMetaUtils().setDisplayName(var7, var5);
               }

               if (!var6.isEmpty()) {
                  this.plugin.getMetaUtils().setLore(var7, var6);
               }

               if (var3.getBoolean("hideDefaultLore", true)) {
                  var7.addItemFlags(ItemFlag.values());
               }

               var4.setItemMeta(var7);
            }

            var4 = this.itemMethodes.setSectionItemOption(var4, var1, var3);
            String var9 = NBTUtils.getNBT(var4, var3, new ConfigManager.CFile(ConfigManager.CFile.Type.SECTION, var1), var2, true);
            if (var9 != null) {
               var4 = CreateItemMethodes.setNBTData(var4, var9, false);
            }

            return var4;
         }
      } else {
         throw new ItemLoadException(Lang.ITEM_NULL.get());
      }
   }

   public ItemStack loadItemToGive(ShopItem var1) throws ItemLoadException {
      boolean var2 = false;
      String var3 = var1.rotates() ? "" : "pages.";
      ConfigurationSection var4 = ConfigManager.getShop(var1.section).getConfigurationSection(var3 + var1.itemLoc);
      ItemStack var5 = CreateItemMethodes.createItemMaterialFromString(var1, var4.getString("material"));
      if (var5.getType() == Material.AIR) {
         return var5;
      } else {
         Translatable var6 = this.getName(var4);
         List var7 = this.getLore(var4);
         if (var6 != null || !var7.isEmpty()) {
            ItemMeta var8 = var5.getItemMeta();
            if (var6 != null) {
               this.plugin.getMetaUtils().setDisplayName(var8, var6);
            }

            if (!var7.isEmpty() && this.plugin.boughtItemsLore) {
               this.plugin.getMetaUtils().setLore(var8, var7);
            }

            var5.setItemMeta(var8);
            var2 = true;
         }

         var5 = this.itemMethodes.setOption(var5, var4, var1, var1.section, false);
         if (var2 && var1.needsUpdate()) {
            var5 = StringSerializer.updateItem(var1, var5);
         }

         return var5;
      }
   }

   public ItemStack getInvalidShopItem(String var1, String var2) {
      ItemStack var3 = new ItemBuilder(XMaterial.BARRIER.parseMaterial()).withDisplayName(Lang.ITEM_ERROR.get()).build();
      return this.plugin.versionHandler.setPathToItem(var3, var1 + "." + var2);
   }

   public ItemStack getInvalidShopItem(String var1, String var2, String var3) {
      ItemBuilder var4 = new ItemBuilder(Material.BARRIER);
      var4.setDisplayName(var3);
      return this.plugin.versionHandler.setPathToItem(var4.build(), var1 + "." + var2);
   }

   public ItemStack getInvalidItem(ItemLoadException var1) {
      List var2 = CalculateAmount.splitLongString(var1.getMessage());
      var2.replaceAll(var0 -> ChatUtil.formatColors("&c" + var0));
      ItemBuilder var3 = new ItemBuilder(Material.BARRIER);
      var3.setDisplayName((String)var2.remove(0));
      if (!var2.isEmpty()) {
         var3.setLore(var2);
      }

      return var3.build();
   }

   public ItemStack getPlayerReminder(Player var1, boolean var2) {
      String var3 = var1.getName();
      if (this.playerReminders.containsKey(var3)) {
         ItemStack var6 = this.playerReminders.get(var3);
         if (var2) {
            if (this.plugin.versionHandler.isDisabledBackButton(var6)) {
               return this.updatePlayerProfile(var6, var1);
            } else {
               ItemStack var7 = this.createPlayerProfile(var1, true);
               this.playerReminders.replace(var3, var7);
               return var7;
            }
         } else if (!this.plugin.versionHandler.isDisabledBackButton(var6)) {
            return this.updatePlayerProfile(var6, var1);
         } else {
            ItemStack var5 = this.createPlayerProfile(var1, false);
            this.playerReminders.replace(var3, var5);
            return var5;
         }
      } else {
         ItemStack var4 = this.createPlayerProfile(var1, var2);
         this.playerReminders.put(var3, var4);
         return var4;
      }
   }

   private ItemStack getTransactionItem(String var1, Translatable var2) {
      try {
         ItemStack var6 = CreateItemMethodes.createItemMaterialFromString(null, ConfigManager.getConfig().getString(var1 + ".material"));
         return var6.getType() != Material.AIR ? new ItemBuilder(var6).setAmount(1).withDisplayName(var2).build() : var6;
      } catch (ItemLoadException var5) {
         SendMessage.logDebugMessage(var5.getMessage());
         SendMessage.errorItemConfig(var1);
         ItemStack var3 = XMaterial.matchXMaterial(ConfigManager.getConfig().getDef().getString(var1 + ".material")).get().parseItem();
         return new ItemBuilder(var3).setAmount(1).withDisplayName(var2).build();
      }
   }

   public List<ItemStack> getAllowedTools() {
      ArrayList var1 = new ArrayList();
      ConfigurationSection var2 = ConfigManager.getConfig().getConfigurationSection("spawner-break-tools");
      if (var2 == null) {
         return new ArrayList<>();
      } else {
         for (String var4 : var2.getKeys(false)) {
            try {
               ItemStack var5 = CreateItemMethodes.createItemMaterialFromString(null, var2.getString(var4 + ".material"));
               var1.add(CreateItemMethodes.setEnchantments(var5, var2.getStringList(var4 + ".enchantments"), null, var2.getCurrentPath(), null));
            } catch (ItemLoadException var6) {
               SendMessage.logDebugMessage(var6.getMessage());
               SendMessage.errorItemConfig("spawner-break-tools." + var4);
            }
         }

         return var1;
      }
   }

   public double getShopItemSellPrice(ItemStack var1, String var2) {
      double var3 = this.getBaseSellPrice(var2);
      return var1.getType().equals(XMaterial.SPAWNER.parseMaterial())
         ? this.plugin.getSpawnerManager().getProvider().getSpawnerSellPrice(var1, var3)
         : var3 * var1.getAmount();
   }

   @Deprecated
   public ShopItem matchShopItem(Player var1, ItemStack var2) {
      if (this.plugin.shopItemsByMaterialName.containsKey(var2.getType().name())) {
         for (ShopItem var4 : this.plugin.shopItemsByMaterialName.get(var2.getType().name())) {
            if (var4.match(var2)
               && PermissionsCache.hasPermission(var1, "OnyxShop.shop." + var4.section)
               && var4.canPurchase(var1, "shop", true)
               && var4.meetsRequirements(var1, true)) {
               return var4;
            }
         }
      }

      return null;
   }

   public ShopItem matchShopItem(Player var1, ItemStack var2, Transaction.Mode var3) {
      if (this.plugin.shopItemsByMaterialName.containsKey(var2.getType().name())) {
         for (ShopItem var5 : this.plugin.shopItemsByMaterialName.get(var2.getType().name())) {
            if (PermissionsCache.hasPermission(var1, "OnyxShop.shop." + var5.section)
               && (var3 == Transaction.Mode.BUY ? var5.isBuyAble() : var5.isSellAble())
               && !var5.isDisplayItem()
               && var5.match(var2)
               && var5.canPurchase(var1, "shop", true)
               && var5.meetsRequirements(var1, true)) {
               return var5;
            }
         }
      }

      return null;
   }

   public ShopItem matchShopItem(ItemStack var1) {
      if (this.plugin.shopItemsByMaterialName.containsKey(var1.getType().name())) {
         for (ShopItem var3 : this.plugin.shopItemsByMaterialName.get(var1.getType().name())) {
            if (!var3.isDisplayItem() && var3.match(var1)) {
               return var3;
            }
         }
      }

      return null;
   }

   public ShopItem matchShopItem(ItemStack var1, Player var2, String var3) {
      if (this.plugin.shopItemsByMaterialName.containsKey(var1.getType().name())) {
         for (ShopItem var5 : this.plugin.shopItemsByMaterialName.get(var1.getType().name())) {
            if (PermissionsCache.hasPermission(var2, "OnyxShop." + var3 + "." + var5.section)
               && var5.isSellAble()
               && !var5.isDisplayItem()
               && var5.match(var1)
               && var5.canPurchase(var2, var3, true)
               && var5.meetsRequirements(var2, true)) {
               return var5;
            }
         }
      }

      return null;
   }

   public ShopItem matchShopItemExact(ItemStack var1, Player var2, String var3) {
      if (!this.plugin.shopItemsByMaterialName.containsKey(var1.getType().name())) {
         return null;
      } else {
         ArrayList<ShopItem> var4 = new ArrayList<>();

         for (ShopItem var6 : this.plugin.shopItemsByMaterialName.get(var1.getType().name())) {
            if (PermissionsCache.hasPermission(var2, "OnyxShop." + var3 + "." + var6.section)
               && var6.isSellAble()
               && !var6.isDisplayItem()
               && var6.match(var1)) {
               var4.add(var6);
            }
         }

         if (var4.size() == 1) {
            ShopItem var7 = (ShopItem)var4.get(0);
            if (var7.canPurchase(var2, var3, false) && var7.meetsRequirements(var2, false)) {
               return var7;
            }
         } else {
            for (ShopItem var9 : var4) {
               if (var9.canPurchase(var2, var3, true) && var9.meetsRequirements(var2, true)) {
                  return var9;
               }
            }
         }

         return null;
      }
   }

   public ItemStack createSpawner(EntityType var1) {
      ItemBuilder var2 = new ItemBuilder(XMaterial.SPAWNER.parseItem());
      var2.setDisplayName(SpawnerManager.getDefaultName(var1));
      return var2.build();
   }

   public LocalizedItemStack createFillItem(ConfigurationSection var1, String var2) {
      if (var1 == null) {
         return null;
      } else if (this.plugin.perPlayerLanguages && this.hasTranslations(var1)) {
         HashMap var7 = new HashMap();

         for (Locale var5 : Lang.getLocales()) {
            ItemStack var6 = this.createFillItem(var1, var2, var5);
            if (var6 == null) {
               return null;
            }

            if (var6.getType() == Material.AIR) {
               return new LocalizedItemStack(var6);
            }

            var7.put(var5, var6);
         }

         return new LocalizedItemStack(var7);
      } else {
         ItemStack var3 = this.createFillItem(var1, var2, Lang.getDefaultLocale());
         return var3 == null ? null : new LocalizedItemStack(var3);
      }
   }

   private boolean hasTranslations(ConfigurationSection var1) {
      return var1.contains("name") && String.valueOf(var1.getString("name")).contains("%translations-")
         ? true
         : var1.contains("lore") && var1.getStringList("lore").stream().anyMatch(var0 -> var0.contains("%translations-"));
   }

   private ItemStack createFillItem(ConfigurationSection var1, String var2, Locale var3) {
      try {
         ItemStack var4 = CreateItemMethodes.createItemMaterialFromString(null, var1.getString("material"));
         if (var4.getType() == Material.AIR) {
            return var4;
         } else {
            ItemBuilder var5 = new ItemBuilder(var4);
            if (var1.contains("name")) {
               var5.withDisplayName(this.getTransactionItemName(var1.getString("name"), var3));
            }

            if (var1.contains("lore")) {
               var5.setCLore(this.getTransactionItemLore(var1.getStringList("lore"), var3));
            }

            if (var1.contains("enchantment-glint")) {
               var5.withEnchantGlint();
            }

            if (var1.contains("skullowner") && !var1.getString("skullowner").equals("%player_name%")) {
               if (!XMaterial.matchXMaterial(var4).equals(XMaterial.PLAYER_HEAD)) {
                  throw new ItemLoadException(Lang.MATERIAL_NEEDS_TO_BE_SKULL.get());
               }

               var5.withSkullOwner(var1.getString("skullowner"));
            }

            if (var1.contains("skull-texture")) {
               if (!XMaterial.matchXMaterial(var4).equals(XMaterial.PLAYER_HEAD)) {
                  throw new ItemLoadException(Lang.MATERIAL_NEEDS_TO_BE_SKULL.get());
               }

               var5.withSkullTexture(var1.getString("skull-texture"));
            }

            var4 = var5.build();
            if (var1.contains("potiontypes")) {
               CreateItemMethodes.setPotionTypes(
                  var4, var1.getStringList("potiontypes"), null, var1.getCurrentPath(), XMaterial.matchXMaterial(var4) == XMaterial.SPLASH_POTION
               );
            }

            if (var1.contains("armor-trim")) {
               if (OnyxShop.getInstance().version < 120) {
                  throw new ItemLoadException("ArmorTrim's are only supported in version 1.20 and above");
               }

               new CreateItemMethodes.ArmorTrim(var1.getString("armor-trim.type"), var1.getString("armor-trim.pattern")).build(var4);
            }

            if (var1.contains("armorcolor")) {
               if (!(var4.getItemMeta() instanceof LeatherArmorMeta)) {
                  throw new ItemLoadException(Lang.MATERIAL_NEEDS_TO_BE_LEATHER_ARMOR.get());
               }

               LeatherArmorMeta var6 = (LeatherArmorMeta)var4.getItemMeta();
               var6.setColor(this.itemMethodes.getRGBColor(var1.getString("armorcolor"), var1.getCurrentPath() + ".armorcolor"));
               var4.setItemMeta(var6);
            }

            if (var1.contains("stack-size")) {
               int var9 = var1.getInt("stack-size");
               if (!OnyxShop.getInstance().allowIllegalStacks && var9 > var4.getMaxStackSize()) {
                  throw new ItemLoadException(
                     "Cannot set the stack size of item %material% to %stack-size% because it would exceed the vanilla max stack size."
                        .replace("%material%", var4.getType().toString())
                        .replace("%stack-size%", String.valueOf(var9))
                  );
               }

               var4.setAmount(var9);
            }

            if (var1.contains("item-flags")) {
               var4 = this.itemMethodes.addItemFlags(var4, var1);
            }

            if (var1.getBoolean("hide-tooltip") && ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_21_R3)) {
               var4 = new ItemBuilder(var4).hideToolTip().build();
            }

            if (var1.contains("CustomModelData")) {
               var4 = CreateItemMethodes.setCustomModelData(var4, var1);
            }

            String var10 = NBTUtils.getNBT(
               var4,
               var1,
               new ConfigManager.CFile(var2 != null ? ConfigManager.CFile.Type.SECTION : ConfigManager.CFile.Type.CONFIG, var2 != null ? var2 : "config"),
               var1.getCurrentPath(),
               true
            );
            if (var10 != null) {
               var4 = CreateItemMethodes.setNBTData(var4, var10, false);
            }

            return var4;
         }
      } catch (ItemLoadException var7) {
         SendMessage.logDebugMessage(var7.getMessage());
         if (var2 != null) {
            SendMessage.errorSections(var2, var1.getCurrentPath());
         } else {
            SendMessage.errorItemConfig(var1.getCurrentPath());
         }

         return null;
      }
   }

   public void isLatestVersion(boolean var1) {
      if (!var1) {
         if (!ConfigManager.getConfig().getBoolean("cloud-features.enabled", false)) {
            this.itemMethodes.useLatestVersion(this.plugin, true);
            return;
         }

         this.plugin.runKillableTaskTimer(() -> {
            String var1x = this.plugin.getPost(UpdateHandler.s + ":2087/val/" + UpdateHandler.getData() + UpdateHandler.i);
            this.itemMethodes.useLatestVersion(this.plugin, var1x == null || Boolean.parseBoolean(var1x));
         }, 20L, 72000L);
      }
   }

   public ItemStack createPlayerProfile(Player var1, boolean var2) {
      ItemBuilder var3 = new ItemBuilder(XMaterial.PLAYER_HEAD.parseMaterial())
         .withDisplayName(Lang.PROFILE.get().builder().append(":").build())
         .withSkullOwner(var1.getName())
         .withCLore(
            Arrays.asList(
               Lang.NAME.get().builder().append(": ").color('c').append(var1.getDisplayName()).build(),
               Lang.MONEY
                  .get()
                  .builder()
                  .append(": ")
                  .append(
                     SendMessage.fromConfig(
                        ChatColor.RED
                           + this.plugin
                              .formatPrice(
                                 this.plugin.getEcoHandler().getDefaultProvider().getType(), this.plugin.getEcoHandler().getDefaultProvider().getBalance(var1)
                              )
                     )
                  )
                  .build(),
               Lang.LEVEL.get().builder().append(": ").color('c').append(String.valueOf(var1.getLevel())).build()
            )
         );
      return var2 ? this.plugin.versionHandler.setDisabledBackButton(var3.build()) : var3.build();
   }

   private ItemStack updatePlayerProfile(ItemStack var1, Player var2) {
      if (var1.hasItemMeta()) {
         ArrayList var3 = new ArrayList();
         var3.add(Lang.NAME.get().builder().append(": ").color('c').append(var2.getDisplayName()).build());
         var3.add(
            Lang.MONEY
               .get()
               .builder()
               .append(": ")
               .append(
                  SendMessage.fromConfig(
                     ChatColor.RED
                        + this.plugin
                           .formatPrice(
                              this.plugin.getEcoHandler().getDefaultProvider().getType(), this.plugin.getEcoHandler().getDefaultProvider().getBalance(var2)
                           )
                  )
               )
               .build()
         );
         var3.add(Lang.LEVEL.get().builder().append(": ").color('c').append(String.valueOf(var2.getLevel())).build());
         ItemMeta var4 = var1.getItemMeta();
         this.plugin.getMetaUtils().setLore(var4, var3);
         var1.setItemMeta(var4);
      }

      return var1;
   }

   private String getMaterial(ItemStack var1) {
      return this.plugin.version > 112 || var1.getType().getMaxDurability() >= 0 && !XMaterial.matchXMaterial(var1).equals(XMaterial.AIR)
         ? var1.getType().name()
         : var1.getType().name() + ":" + var1.getDurability();
   }

   public Map<String, Object> getShopItem(ItemStack var1, double var2, double var4, boolean var6) {
      HashMap var7 = new HashMap();
      if (!var6) {
         String var8 = this.plugin.getItemManager().getItem(var1);
         if (var8 != null) {
            var7.put("material", var8);
            var7.put("buy", var2);
            var7.put("sell", var4);
            return var7;
         }
      }

      var7.put("material", this.getMaterial(var1));
      var7.put("buy", var2);
      var7.put("sell", var4);
      if (var1.getAmount() > 1) {
         var7.put("stack-size", var1.getAmount());
      }

      if (var1.hasItemMeta()) {
         ItemMeta var14 = var1.getItemMeta();
         if (var14.hasDisplayName()) {
            var7.put("name", var14.getDisplayName());
         }

         if (var14.hasLore()) {
            ArrayList var9 = new ArrayList();

            for (String var11 : var14.getLore()) {
               var9.add(var11.replaceAll(String.valueOf('§'), "&"));
            }

            var7.put("lore", var9);
         }

         List var16 = this.plugin.enchantsProvider.getEnchantKeys(var1);
         if (!var16.isEmpty()) {
            var7.put("enchantments", var16);
         }

         if (!var14.getItemFlags().isEmpty()) {
            var7.put("item-flags", var14.getItemFlags().stream().map(Enum::name).collect(Collectors.toList()));
         }

         if (var14 instanceof LeatherArmorMeta) {
            LeatherArmorMeta var17 = (LeatherArmorMeta)var1.getItemMeta();
            Color var23 = var17.getColor();
            var7.put(
               "armorcolor",
               "#" + Integer.toHexString(new java.awt.Color(var23.getRed(), var23.getGreen(), var23.getBlue()).getRGB()).substring(2).toUpperCase()
            );
         } else if (this.plugin.version >= 112 && var14 instanceof KnowledgeBookMeta) {
            KnowledgeBookMeta var22 = (KnowledgeBookMeta)var1.getItemMeta();
            ArrayList var26 = new ArrayList();

            for (int var12 = 0; var12 < var22.getRecipes().size(); var12++) {
               var26.add(((NamespacedKey)var22.getRecipes().get(var12)).getKey());
            }

            var7.put("recipes", var26);
         } else if (var14 instanceof FireworkMeta) {
            var7.putAll(FireworkUtil.serialize((FireworkMeta)var14));
         } else if (var14 instanceof SkullMeta) {
            SkullMeta var18 = (SkullMeta)var14;
            if (SkullUtil.hasSkullOwner(var18)) {
               var7.put("skullowner", var18.getOwner());
            } else {
               String var24 = SkullUtil.getSkullTexture(var18);
               if (var24 != null) {
                  var7.put("skull-texture", var24);
               }

               if (var18.hasOwner()) {
                  var7.put("skullowner", var18.getOwner());
               }
            }
         } else if (ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_19_R1) && var14 instanceof MusicInstrumentMeta) {
            MusicInstrumentMeta var21 = (MusicInstrumentMeta)var14;
            var7.put("instrument", var21.getInstrument().getKey().getKey().replace("_goat_horn", ""));
         } else if (ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_20_R1) && var14 instanceof ArmorMeta) {
            ArmorMeta var20 = (ArmorMeta)var14;
            if (var20.hasTrim()) {
               ArmorTrim var25 = var20.getTrim();
               var7.put("armor-trim.type", var25.getMaterial().getKey().getKey());
               var7.put("armor-trim.pattern", var25.getPattern().getKey().getKey());
            }
         } else if (ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_20_R4) && var14 instanceof OminousBottleMeta) {
            OminousBottleMeta var19 = (OminousBottleMeta)var14;
            if (var19.hasAmplifier()) {
               var7.put("ominous-strength", var19.getAmplifier() + 1);
            }
         }
      }

      if (this.plugin.version >= 116 && var1.hasItemMeta()) {
         try {
            if ((Integer)var1.getItemMeta()
                  .getPersistentDataContainer()
                  .get(new NamespacedKey(Bukkit.getPluginManager().getPlugin("AutoSellChests"), "autosell"), PersistentDataType.INTEGER)
               == 1) {
               var7.put("autosell", true);
            }
         } catch (Exception var13) {
         }
      }

      if (var1.getType().equals(XMaterial.SPAWNER.parseMaterial())) {
         var7.put("spawnertype", this.plugin.getSpawnerManager().getProvider().getSpawnedType(var1));
      }

      String var15 = this.plugin.versionHandler.getNBTDataAsString(var1, var6);
      if (var15 != null) {
         var7.put(ServerInfo.supportsComponents() ? "components" : "NBTData", var15);
      }

      return var7;
   }

   public TransactionScreen getTransactionScreen(Transaction.Type var1) {
      ArrayList<TransactionItem> var2 = new ArrayList<>();
      String var3 = var1.name().toLowerCase().replace("_", "-");
      int var4 = CalculateAmount.getInvSlots(ConfigManager.getConfig().getInt(var3 + ".menu-size", 5));
      if (ConfigManager.getConfig().contains(var3 + ".fill-item")) {
         LocalizedItemStack var5 = this.createFillItem(ConfigManager.getConfig().getConfigurationSection(var3 + ".fill-item"), null);
         var2.add(
            new TransactionItem(
               CreateItem.TransactionItemAction.NONE,
               CreateItem.TransactionItemType.FILLITEM,
               new ArrayList<>(),
               var5.getItem(),
               new ArrayList<>(),
               false,
               ConfigManager.getConfig().getString(var3 + ".fill-item.bedrock-texture")
            )
         );
      }

      for (String var6 : ConfigManager.getConfig().getConfigurationSection(var3 + ".items").getKeys(false)) {
         CreateItem.TransactionItemAction var7 = new CreateItem.TransactionItemAction(
            ConfigManager.getConfig().getString(var3 + ".items." + var6 + ".action", "NONE")
         );
         CreateItem.TransactionItemType var8 = CreateItem.TransactionItemType.getFromString(
            var7, ConfigManager.getConfig().getString(var3 + ".items." + var6 + ".type", "NORMAL")
         );
         if (var8 != CreateItem.TransactionItemType.NORMAL
            && var8 != CreateItem.TransactionItemType.DECREASE_AMOUNT
            && var8 != CreateItem.TransactionItemType.INCREASE_AMOUNT
            && var8 != CreateItem.TransactionItemType.SELECT_AMOUNT
            && var2.stream().filter(var1x -> var1x.getType() == var8).findFirst().orElse(null) != null) {
            SendMessage.warnMessage(
               "The "
                  + var3
                  + " cannot contain multiple items from type "
                  + var8.name()
                  + ", instead use the ability to copy this item into multiple slots by using 'slot: 9,13,14,15', skipping..."
            );
            SendMessage.errorItemConfig(var3 + ".items." + var6);
         } else {
            boolean var9 = ConfigManager.getConfig().getString(var3 + ".items." + var6 + ".material", "").equalsIgnoreCase("%SELECTED_ITEM%")
               || var8 == CreateItem.TransactionItemType.SELECTED_ITEM;
            ItemStack var10 = this.getTransactionItem(var8, var3 + ".items." + var6, var9);
            List var11 = this.getSlots(
               ConfigManager.getConfig()
                  .getString(var3 + ".items." + var6 + ".slot", ConfigManager.getConfig().getDef().getString(var3 + ".items." + var6 + ".slot", null))
            );
            if (var11 == null) {
               SendMessage.warnMessage("Invalid slot for item, skipping...");
               SendMessage.errorItemConfig(var3 + ".items." + var6);
            } else {
               for (int var12 = var11.size() - 1; var12 > 0; var12--) {
                  int var13 = (Integer)var11.get(var12);
                  if (var13 >= (this.plugin.navBar.isEnableTransactionNav() ? var4 - 9 : var4) || var13 < 0) {
                     SendMessage.warnMessage("Item slot " + var13 + " is out of bounds for menu size of " + var4 + ", skipping ...");
                     SendMessage.errorItemConfig(var3 + ".items." + var6);
                     var11.remove(var12);
                  }
               }

               var2.add(
                  new TransactionItem(
                     var7,
                     var8,
                     ConfigManager.getConfig().getStringList(var3 + ".items." + var6 + ".commands"),
                     var10,
                     var11,
                     var9,
                     ConfigManager.getConfig().getString(var3 + ".items." + var6 + ".bedrock-texture")
                  )
               );
            }
         }
      }

      if (!var2.stream()
            .anyMatch(
               var0 -> var0.getAction().type == CreateItem.TransactionItemActionType.INSTA_BUY
                  || var0.getAction().type == CreateItem.TransactionItemActionType.INSTA_SELL
            )
         && !var2.stream().anyMatch(var0 -> var0.getType() == CreateItem.TransactionItemType.CONFIRM)) {
         SendMessage.warnMessage(
            "The transaction screen inside the config.yml at path '" + var3 + "' needs to contain atleast one item with type CONFIRM, adding default..."
         );
         var2.add(
            new TransactionItem(
               new CreateItem.TransactionItemAction(CreateItem.TransactionItemActionType.CONFIRM_TRANSACTION),
               CreateItem.TransactionItemType.CONFIRM,
               Collections.EMPTY_LIST,
               this.getTransactionItem(CreateItem.TransactionItemType.CONFIRM, var3 + ".items.7", false),
               Collections.singletonList(13),
               false
            )
         );
      }

      return new TransactionScreen(var2, var4);
   }

   public List<Translatable> getDisplayLore(ConfigurationSection var1) {
      List<String> var2 = new ArrayList<>();
      if (var1.contains("displaylore")) {
         var2 = var1.getStringList("displaylore");
      } else if (var1.contains("lore")) {
         var2 = var1.getStringList("lore");
      }

      return var2.stream().map(SendMessage::fromConfig).collect(Collectors.toList());
   }

   public Translatable getName(ConfigurationSection var1) {
      return var1.contains("name") ? SendMessage.fromConfig(var1.getString("name")) : null;
   }

   public List<Translatable> getLore(ConfigurationSection var1) {
      List<String> var2 = new ArrayList<>();
      if (var1.contains("lore")) {
         var2 = var1.getStringList("lore");
      }

      return var2.stream().map(SendMessage::fromConfig).collect(Collectors.toList());
   }

   private List<Integer> getSlots(String var1) {
      if (var1 == null) {
         return null;
      } else {
         try {
            return new ArrayList<>(Collections.singletonList(Integer.parseInt(var1)));
         } catch (NumberFormatException var10) {
            ArrayList var3 = new ArrayList();
            var1 = StringUtils.deleteWhitespace(var1);

            for (String var7 : var1.split(",")) {
               String[] var8 = var7.split("-");
               if (var8.length == 2) {
                  for (int var9 = Integer.parseInt(var8[0]); var9 < Integer.parseInt(var8[1]); var9++) {
                     var3.add(var9);
                  }
               } else if (var8.length == 1) {
                  var3.add(Integer.parseInt(var8[0]));
               }
            }

            return !var3.isEmpty() ? var3 : null;
         }
      }
   }

   private ItemStack getTransactionItem(CreateItem.TransactionItemType var1, String var2, boolean var3) {
      try {
         ItemStack var9 = CreateItemMethodes.createItemMaterialFromString(null, var3 ? "BARRIER" : ConfigManager.getConfig().getString(var2 + ".material"));
         if (var9.getType() == Material.AIR) {
            return null;
         } else {
            int var5 = ConfigManager.getConfig().getInt(var2 + ".stack-size", 1);
            ItemBuilder var6 = new ItemBuilder(var9);
            if (var5 >= 1) {
               var6.setAmount(var5);
            }

            if (ConfigManager.getConfig().contains(var2 + ".name")) {
               var6.withDisplayName(this.getTransactionItemName(ConfigManager.getConfig().getString(var2 + ".name")));
            }

            if (ConfigManager.getConfig().contains(var2 + ".lore")) {
               var6.withCLore(this.getTransactionItemLore(ConfigManager.getConfig().getStringList(var2 + ".lore")));
            }

            if (var1 == CreateItem.TransactionItemType.SELECTED_ITEM && !ConfigManager.getConfig().contains(var2 + ".lore")) {
               var6.setCLore(Lang.TRANSACTION_SCREEN_TOTAL_AMOUNT.get().getLines());
            }

            if (ConfigManager.getConfig().getBoolean(var2 + ".enchantment-glint")) {
               var6.withEnchantGlint();
            }

            if (ConfigManager.getConfig().getBoolean(var2 + ".hideDefaultLore")) {
               var6.hideFlags();
            }

            if (ConfigManager.getConfig().getBoolean(var2 + ".item-flags")) {
               var6.withItemFlags(ConfigManager.getConfig().getStringList(var2 + ".item-flags"));
            }

            if (ConfigManager.getConfig().contains(var2 + ".skullowner")) {
               if (XMaterial.matchXMaterial(var9).equals(XMaterial.PLAYER_HEAD)) {
                  var6.withSkullOwner(ConfigManager.getConfig().getString(var2 + ".skullowner"));
               }

               throw new ItemLoadException(Lang.MATERIAL_NEEDS_TO_BE_SKULL.get());
            } else {
               if (ConfigManager.getConfig().contains(var2 + ".skull-texture")) {
                  if (!XMaterial.matchXMaterial(var9).equals(XMaterial.PLAYER_HEAD)) {
                     throw new ItemLoadException(Lang.MATERIAL_NEEDS_TO_BE_SKULL.get());
                  }

                  var6.withSkullTexture(ConfigManager.getConfig().getString(var2 + ".skull-texture"));
               }

               var9 = var6.build();
               if (ConfigManager.getConfig().contains(var2 + ".CustomModelData")) {
                  var9 = CreateItemMethodes.setCustomModelData(var9, ConfigManager.getConfig().getConfigurationSection(var2));
               }

               if (ConfigManager.getConfig().contains(var2 + ".hide-tooltip") && ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_21_R3)) {
                  var9 = new ItemBuilder(var9).hideToolTip().build();
               }

               String var7 = NBTUtils.getNBT(
                  var9, ConfigManager.getConfig().getConfigurationSection(var2), new ConfigManager.CFile(ConfigManager.CFile.Type.CONFIG, "config"), var2, true
               );
               if (var7 != null) {
                  var9 = CreateItemMethodes.setNBTData(var9, var7, false);
               }

               return var9;
            }
         }
      } catch (ItemLoadException var8) {
         SendMessage.warnMessage(var8.getMessage());
         SendMessage.errorItemConfig(var2);
         if (!ConfigManager.getConfig().getDef().contains(var2)) {
            return null;
         } else {
            ItemStack var4 = XMaterial.matchXMaterial(ConfigManager.getConfig().getDef().getString(var2 + ".material")).get().parseItem();
            return new ItemBuilder(var4)
               .setAmount(ConfigManager.getConfig().getDef().getInt(var2 + ".stack-size", 1))
               .withDisplayName(this.getTransactionItemName(ConfigManager.getConfig().getDef().getString(var2 + ".name")))
               .withCLore(this.getTransactionItemLore(ConfigManager.getConfig().getDef().getStringList(var2 + ".lore")))
               .build();
         }
      }
   }

   private Translatable getTransactionItemName(String var1) {
      return this.getTransactionItemName(var1, Lang.getDefaultLocale());
   }

   private Translatable getTransactionItemName(String var1, Locale var2) {
      if (var1 == null) {
         return new TranslatableRaw("");
      } else {
         while (var1.contains("%translations-")) {
            try {
               Lang var3 = Lang.valueOf(var1.split("%translations-")[1].split("%")[0].toUpperCase(Locale.ENGLISH).replace("-", "_"));
               var1 = var1.replace("%translations-" + var3.getKey() + "%", var3.getRaw(var2));
               if (var3 == Lang.CURRENT_PAGE && !var1.contains("%page%")) {
                  var1 = var1 + " %page%/%pages%";
               }
            } catch (IllegalArgumentException var4) {
            }
         }

         return SendMessage.fromConfig(var1);
      }
   }

   private List<Translatable> getTransactionItemLore(List<String> var1) {
      return this.getTransactionItemLore(var1, Lang.getDefaultLocale());
   }

   private List<Translatable> getTransactionItemLore(List<String> var1, Locale var2) {
      return (List<Translatable>)(var1.isEmpty() ? new ArrayList<>() : var1.stream().map(var1x -> {
         while (var1x.contains("%translations-")) {
            try {
               Lang var2x = Lang.valueOf(var1x.split("%translations-")[1].split("%")[0].toUpperCase(Locale.ENGLISH).replace("-", "_"));
               var1x = var1x.replace("%translations-" + var2x.getKey() + "%", var2x.getRaw(var2));
            } catch (IllegalArgumentException var3) {
            }
         }

         return SendMessage.fromConfig(var1x);
      }).collect(Collectors.toList()));
   }

   public static final class TransactionItemAction {
      public static final CreateItem.TransactionItemAction NONE = new CreateItem.TransactionItemAction(CreateItem.TransactionItemActionType.NONE);
      public CreateItem.TransactionItemActionType type;
      public int amount = 1;

      public TransactionItemAction(CreateItem.TransactionItemActionType var1) {
         this.type = var1;
      }

      public TransactionItemAction(String var1) {
         try {
            var1 = var1.toUpperCase(Locale.ENGLISH).replace(" ", "_");
            if (var1.startsWith("ADD")) {
               this.getFromStringAmount(CreateItem.TransactionItemActionType.ADD, var1);
            } else if (var1.startsWith("REMOVE")) {
               this.getFromStringAmount(CreateItem.TransactionItemActionType.REMOVE, var1);
            } else if (var1.startsWith("SET")) {
               this.getFromStringAmount(CreateItem.TransactionItemActionType.SET, var1);
            } else {
               this.type = CreateItem.TransactionItemActionType.valueOf(var1);
            }
         } catch (IllegalArgumentException var3) {
            SendMessage.warnMessage("Failed to load action for transaction item like '" + var1 + "'");
            this.type = CreateItem.TransactionItemActionType.NONE;
         }
      }

      public void getFromStringAmount(CreateItem.TransactionItemActionType var1, String var2) {
         this.type = var1;

         try {
            this.amount = Math.max(Integer.parseInt(var2.replace(var1.name(), "")), 1);
         } catch (NumberFormatException var4) {
            SendMessage.warnMessage("Failed to get amount for transaction item action " + var1.name() + " like '" + var2 + "'");
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

      public static CreateItem.TransactionItemActionType get(String var0) {
         return valueOf(var0);
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

      public static CreateItem.TransactionItemType getFromString(CreateItem.TransactionItemAction var0, String var1) {
         try {
            switch (var0.type) {
               case REMOVE:
                  return DECREASE_AMOUNT;
               case ADD:
                  return INCREASE_AMOUNT;
               case SET:
                  return SELECT_AMOUNT;
               default:
                  return valueOf(var1.toUpperCase(Locale.ENGLISH).replace(" ", "_"));
            }
         } catch (NullPointerException | IllegalArgumentException var3) {
            SendMessage.warnMessage("Failed to load item type for transaction item like '" + var1 + "'");
            return NORMAL;
         }
      }
   }
}
