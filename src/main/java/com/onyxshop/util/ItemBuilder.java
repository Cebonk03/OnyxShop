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

   public ItemBuilder(ItemStack var1) {
      this.item = var1.clone();
      this.meta = var1.getItemMeta();
   }

   public ItemBuilder(Material var1) {
      this.item = new ItemStack(var1);
      this.meta = this.item.getItemMeta();
   }

   public ItemBuilder withType(Material var1) {
      this.item.setType(var1);
      return this;
   }

   public ItemBuilder withSimpleMeta(ItemStack var1) {
      ItemMeta var2 = var1.getItemMeta();
      if (var2.hasDisplayName()) {
         this.setDisplayName(var2.getDisplayName());
      }

      if (var2.hasLore()) {
         this.setLore(var2.getLore());
      }

      this.meta.addItemFlags(var2.getItemFlags().toArray(new ItemFlag[0]));
      this.setAmount(var1.getAmount());
      return this;
   }

   public boolean hasDisplayName() {
      return this.meta.hasDisplayName();
   }

   public String getRawDisplayName() {
      return OnyxShop.getInstance().getMetaUtils().getRawDisplayName(this.meta);
   }

   public void setRawDisplayName(String var1) {
      OnyxShop.getInstance().getMetaUtils().setRawDisplayName(this.meta, var1);
   }

   public void setDisplayName(Translatable var1) {
      OnyxShop.getInstance().getMetaUtils().setDisplayName(this.meta, var1);
   }

   public void setDisplayName(String var1) {
      this.meta.setDisplayName(var1);
   }

   public void setCDisplayName(Translatable var1) {
      OnyxShop.getInstance().getMetaUtils().setDisplayName(this.meta, var1);
   }

   public ItemBuilder withDisplayName(Translatable var1) {
      OnyxShop.getInstance().getMetaUtils().setDisplayName(this.meta, var1);
      return this;
   }

   public ItemBuilder withDisplayName(String var1) {
      this.meta.setDisplayName(var1);
      return this;
   }

   public boolean hasLore() {
      return this.meta.hasLore();
   }

   public List<String> getRawLore() {
      return OnyxShop.getInstance().getMetaUtils().getRawLore(this.meta);
   }

   public ItemBuilder withLore(List<String> var1) {
      this.meta.setLore(var1);
      return this;
   }

   public ItemBuilder withCLore(List<Translatable> var1) {
      OnyxShop.getInstance().getMetaUtils().setLore(this.meta, var1);
      return this;
   }

   public void setCLore(List<Translatable> var1) {
      OnyxShop.getInstance().getMetaUtils().setLore(this.meta, var1);
   }

   public void setLore(List<String> var1) {
      this.meta.setLore(var1);
   }

   public void setRawLore(List<String> var1) {
      OnyxShop.getInstance().getMetaUtils().setRawLore(this.meta, var1);
   }

   public ItemBuilder addLore(String... var1) {
      List<String> var2 = this.meta.hasLore() ? this.meta.getLore() : new ArrayList<>();
      var2.addAll(0, OnyxShop.getInstance().itemEditor.getMultipleItemLore(var1));
      this.meta.setLore((List)var2);
      return this;
   }

   public ItemBuilder addLore(List<String> var1) {
      List<String> var2 = this.meta.hasLore() ? this.meta.getLore() : new ArrayList<>();
      var2.addAll(var1);
      this.meta.setLore((List)var2);
      return this;
   }

   public ItemBuilder addLore(String var1) {
      List<String> var2 = this.meta.hasLore() ? this.meta.getLore() : new ArrayList<>();
      var2.add(var1);
      this.meta.setLore((List)var2);
      return this;
   }

   public ItemBuilder addLore(Translatable var1) {
      OnyxShop.getInstance().getMetaUtils().addLore(this.meta, var1);
      return this;
   }

   public ItemBuilder withLore(Translatable... var1) {
      OnyxShop.getInstance().getMetaUtils().setLore(this.meta, Arrays.asList(var1));
      return this;
   }

   public ItemBuilder withLore(String... var1) {
      this.meta.setLore(OnyxShop.getInstance().itemEditor.getMultipleItemLore(var1));
      return this;
   }

   public ItemBuilder removeLore() {
      this.meta.setLore(Collections.EMPTY_LIST);
      return this;
   }

   public ItemBuilder withItemFlags(List<String> var1) throws ItemLoadException {
      for (String var3 : var1) {
         if (var3.equals("*") || var3.equalsIgnoreCase("ALL")) {
            this.hideFlags();
            return this;
         }

         try {
            ItemFlag var4 = ItemFlag.valueOf(var3.toUpperCase(Locale.ROOT));
            this.meta.addItemFlags(new ItemFlag[]{var4});
         } catch (IllegalArgumentException var5) {
            throw new ItemLoadException(
               "Invalid item flag for " + var3 + ", valid values are: " + Arrays.toString(Arrays.stream(ItemFlag.values()).map(Enum::name).toArray())
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
         ItemFlag var1 = ItemFlag.valueOf("HIDE_BLOCK_ENTITY_DATA");
         this.meta.addItemFlags(new ItemFlag[]{var1});
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
      for (Enchantment var2 : this.meta.getEnchants().keySet()) {
         this.meta.removeEnchant(var2);
      }

      return this;
   }

   public ItemBuilder replaceText(String var1, String var2) {
      if (this.meta.hasDisplayName()) {
         this.meta.setDisplayName(this.meta.getDisplayName().replace(var1, var2));
      }

      if (this.meta.hasLore()) {
         this.meta.setLore(this.meta.getLore().stream().map(var2x -> var2x.replace(var1, var2)).collect(Collectors.toList()));
      }

      return this;
   }

   public ItemBuilder replaceName(String var1, String var2) {
      if (!this.meta.hasDisplayName()) {
         return this;
      } else {
         this.meta.setDisplayName(this.meta.getDisplayName().replace(var1, var2));
         return this;
      }
   }

   public ItemBuilder withCustomModelData(int var1) {
      this.meta.setCustomModelData(var1);
      return this;
   }

   public ItemBuilder replaceLore(String var1, String var2) {
      if (!this.meta.hasLore()) {
         return this;
      } else {
         this.meta.setLore(this.meta.getLore().stream().map(var2x -> var2x.replace(var1, var2)).collect(Collectors.toList()));
         return this;
      }
   }

   public void setOwner(String var1) throws ItemLoadException {
      if (this.item.getType().equals(XMaterial.PLAYER_HEAD.parseMaterial())) {
         SkullUtil.setSkullTexture(this.item, (SkullMeta)this.meta, var1, false);
      } else {
         throw new ItemLoadException(Lang.MATERIAL_NEEDS_TO_BE_SKULL.get());
      }
   }

   public void setTexture(String var1) throws ItemLoadException {
      if (this.item.getType().equals(XMaterial.PLAYER_HEAD.parseMaterial())) {
         SkullUtil.setSkullTextureFromHash((SkullMeta)this.meta, var1);
      } else {
         throw new ItemLoadException(Lang.MATERIAL_NEEDS_TO_BE_SKULL.get());
      }
   }

   public ItemBuilder withSkullOwner(String var1) {
      SkullUtil.setSkullTexture(this.item, (SkullMeta)this.meta, var1, false);
      return this;
   }

   public ItemBuilder withSkullTexture(String var1) {
      SkullUtil.setSkullTextureFromHash((SkullMeta)this.meta, var1);
      return this;
   }

   public ItemBuilder setAmount(int var1) {
      this.item.setAmount(var1);
      return this;
   }

   public ItemBuilder withAmount(int var1) {
      this.item.setAmount(var1);
      return this;
   }

   public ItemBuilder hideFlags() {
      this.meta.addItemFlags(ItemFlag.values());
      return this;
   }

   public ItemBuilder hideInteractLore() {
      if (ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_21_R4) && !ServerInfo.supportsPaper()) {
         ItemFlag var1 = ItemFlag.valueOf("HIDE_BLOCK_ENTITY_DATA");
         this.meta.addItemFlags(new ItemFlag[]{var1});
      } else {
         this.meta.addItemFlags(ItemFlag.values());
      }

      return this;
   }

   public ItemBuilder setArmorColor(Color var1) {
      LeatherArmorMeta var2 = (LeatherArmorMeta)this.meta;
      var2.setColor(var1);
      this.meta = var2;
      return this;
   }

   public ItemStack build() {
      this.item.setItemMeta(this.meta);
      return this.item;
   }
}
