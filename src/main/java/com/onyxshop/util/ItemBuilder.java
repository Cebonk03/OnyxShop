package com.onyxshop.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.files.Translatable;
import com.onyxshop.util.exceptions.ItemLoadException;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class ItemBuilder {
   private ItemStack item;
   private ItemMeta meta;

   public ItemBuilder(ItemStack itemStack) {
      this.item = itemStack.clone();
      this.meta = itemStack.getItemMeta();
   }

   public ItemBuilder(Material material) {
      this.item = new ItemStack(material);
      this.meta = this.item.getItemMeta();
   }

   public ItemBuilder withType(Material material) {
      this.item.setType(material);
      return this;
   }

   public ItemBuilder withSimpleMeta(ItemStack itemStack) {
      ItemMeta itemMeta = itemStack.getItemMeta();
      if (itemMeta.hasDisplayName()) {
         this.setDisplayName(itemMeta.getDisplayName());
      }

      if (itemMeta.hasLore()) {
         this.setLore(itemMeta.getLore());
      }

      this.meta.addItemFlags(itemMeta.getItemFlags().toArray(new ItemFlag[0]));
      this.setAmount(itemStack.getAmount());
      return this;
   }

   public boolean hasDisplayName() {
      return this.meta.hasDisplayName();
   }

   public String getRawDisplayName() {
      return OnyxShop.getInstance().getMetaUtils().getRawDisplayName(this.meta);
   }

   public void setRawDisplayName(String name) {
      OnyxShop.getInstance().getMetaUtils().setRawDisplayName(this.meta, name);
   }

   public void setDisplayName(Translatable text) {
      OnyxShop.getInstance().getMetaUtils().setDisplayName(this.meta, text);
   }

   public void setDisplayName(String name) {
      this.meta.setDisplayName(name);
   }

   public void setCDisplayName(Translatable text) {
      OnyxShop.getInstance().getMetaUtils().setDisplayName(this.meta, text);
   }

   public ItemBuilder withDisplayName(Translatable text) {
      OnyxShop.getInstance().getMetaUtils().setDisplayName(this.meta, text);
      return this;
   }

   public ItemBuilder withDisplayName(String name) {
      this.meta.setDisplayName(name);
      return this;
   }

   public boolean hasLore() {
      return this.meta.hasLore();
   }

   public List<String> getRawLore() {
      return OnyxShop.getInstance().getMetaUtils().getRawLore(this.meta);
   }

   public ItemBuilder withLore(List<String> lores) {
      this.meta.setLore(lores);
      return this;
   }

   public ItemBuilder withCLore(List<Translatable> values) {
      OnyxShop.getInstance().getMetaUtils().setLore(this.meta, values);
      return this;
   }

   public void setCLore(List<Translatable> values) {
      OnyxShop.getInstance().getMetaUtils().setLore(this.meta, values);
   }

   public void setLore(List<String> lores) {
      this.meta.setLore(lores);
   }

   public void setRawLore(List<String> lores) {
      OnyxShop.getInstance().getMetaUtils().setRawLore(this.meta, lores);
   }

   public ItemBuilder addLore(String... lores) {
      List<String> values = this.meta.hasLore() ? this.meta.getLore() : new ArrayList<>();
      values.addAll(0, OnyxShop.getInstance().itemEditor.getMultipleItemLore(lores));
      this.meta.setLore((List)values);
      return this;
   }

   public ItemBuilder addLore(List<String> lores) {
      List<String> values = this.meta.hasLore() ? this.meta.getLore() : new ArrayList<>();
      values.addAll(lores);
      this.meta.setLore((List)values);
      return this;
   }

   public ItemBuilder addLore(String lore) {
      List<String> values = this.meta.hasLore() ? this.meta.getLore() : new ArrayList<>();
      values.add(lore);
      this.meta.setLore((List)values);
      return this;
   }

   public ItemBuilder addLore(Translatable text) {
      OnyxShop.getInstance().getMetaUtils().addLore(this.meta, text);
      return this;
   }

   public ItemBuilder withLore(Translatable... lores) {
      OnyxShop.getInstance().getMetaUtils().setLore(this.meta, Arrays.asList(lores));
      return this;
   }

   public ItemBuilder withLore(String... lores) {
      this.meta.setLore(OnyxShop.getInstance().itemEditor.getMultipleItemLore(lores));
      return this;
   }

   public ItemBuilder removeLore() {
      this.meta.setLore(Collections.EMPTY_LIST);
      return this;
   }

   public ItemBuilder withItemFlags(List<String> itemIds) throws ItemLoadException {
      for (String text : itemIds) {
         if (text.equals("*") || text.equalsIgnoreCase("ALL")) {
            this.hideFlags();
            return this;
         }

         try {
            ItemFlag itemFlag = ItemFlag.valueOf(text.toUpperCase(Locale.ROOT));
            this.meta.addItemFlags(new ItemFlag[]{itemFlag});
         } catch (IllegalArgumentException exception) {
            throw new ItemLoadException(
               "Invalid item flag for " + text + ", valid values are: " + Arrays.toString(Arrays.stream(ItemFlag.values()).map(Enum::name).toArray())
            );
         }
      }

      return this;
   }

   public ItemBuilder hideToolTip() {
      this.meta.setHideTooltip(true);
      return this;
   }

   public ItemBuilder hideEntityData() {
      if (ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_21_R4) && !ServerInfo.supportsPaper()) {
         ItemFlag itemFlag = ItemFlag.valueOf("HIDE_BLOCK_ENTITY_DATA");
         this.meta.addItemFlags(new ItemFlag[]{itemFlag});
      } else if (ServerInfo.supportsComponents()) {
         this.meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ADDITIONAL_TOOLTIP});
      } else {
         this.meta.addItemFlags(ItemFlag.values());
      }

      return this;
   }

   public ItemBuilder withEnchantGlint() {
      if (ServerInfo.supportsComponents()) {
         this.meta.setEnchantmentGlintOverride(true);
      } else {
         this.meta.addEnchant(XEnchantment.WATER_WORKER.parseEnchantment(), 1, false);
         this.meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
      }

      return this.hideFlags();
   }

   public ItemBuilder removeEnchants() {
      for (Enchantment enchantment : this.meta.getEnchants().keySet()) {
         this.meta.removeEnchant(enchantment);
      }

      return this;
   }

   public ItemBuilder replaceText(String itemId, String text) {
      if (this.meta.hasDisplayName()) {
         this.meta.setDisplayName(this.meta.getDisplayName().replace(itemId, text));
      }

      if (this.meta.hasLore()) {
         this.meta.setLore(this.meta.getLore().stream().map(element -> element.replace(itemId, text)).collect(Collectors.toList()));
      }

      return this;
   }

   public ItemBuilder replaceName(String name, String itemId) {
      if (!this.meta.hasDisplayName()) {
         return this;
      } else {
         this.meta.setDisplayName(this.meta.getDisplayName().replace(name, itemId));
         return this;
      }
   }

   public ItemBuilder withCustomModelData(int value) {
      this.meta.setCustomModelData(value);
      return this;
   }

   public ItemBuilder replaceLore(String lore, String itemId) {
      if (!this.meta.hasLore()) {
         return this;
      } else {
         this.meta.setLore(this.meta.getLore().stream().map(element -> element.replace(lore, itemId)).collect(Collectors.toList()));
         return this;
      }
   }

   public void setOwner(String itemId) throws ItemLoadException {
      if (this.item.getType().equals(XMaterial.PLAYER_HEAD.parseMaterial())) {
         SkullUtil.setSkullTexture(this.item, (SkullMeta)this.meta, itemId, false);
      } else {
         throw new ItemLoadException(Lang.MATERIAL_NEEDS_TO_BE_SKULL.get());
      }
   }

   public void setTexture(String itemId) throws ItemLoadException {
      if (this.item.getType().equals(XMaterial.PLAYER_HEAD.parseMaterial())) {
         SkullUtil.setSkullTextureFromHash((SkullMeta)this.meta, itemId);
      } else {
         throw new ItemLoadException(Lang.MATERIAL_NEEDS_TO_BE_SKULL.get());
      }
   }

   public ItemBuilder withSkullOwner(String itemId) {
      SkullUtil.setSkullTexture(this.item, (SkullMeta)this.meta, itemId, false);
      return this;
   }

   public ItemBuilder withSkullTexture(String itemId) {
      SkullUtil.setSkullTextureFromHash((SkullMeta)this.meta, itemId);
      return this;
   }

   public ItemBuilder setAmount(int amount) {
      this.item.setAmount(amount);
      return this;
   }

   public ItemBuilder withAmount(int amount) {
      this.item.setAmount(amount);
      return this;
   }

   public ItemBuilder hideFlags() {
      this.meta.addItemFlags(ItemFlag.values());
      return this;
   }

   public ItemBuilder hideInteractLore() {
      if (ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_21_R4) && !ServerInfo.supportsPaper()) {
         ItemFlag itemFlag = ItemFlag.valueOf("HIDE_BLOCK_ENTITY_DATA");
         this.meta.addItemFlags(new ItemFlag[]{itemFlag});
      } else {
         this.meta.addItemFlags(ItemFlag.values());
      }

      return this;
   }

   public ItemBuilder setArmorColor(Color color) {
      LeatherArmorMeta leatherArmorMeta = (LeatherArmorMeta)this.meta;
      leatherArmorMeta.setColor(color);
      this.meta = leatherArmorMeta;
      return this;
   }

   public ItemStack build() {
      this.item.setItemMeta(this.meta);
      return this.item;
   }
}
