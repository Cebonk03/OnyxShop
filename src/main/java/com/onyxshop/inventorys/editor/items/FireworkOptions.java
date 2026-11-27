package com.onyxshop.inventorys.editor.items;

import java.util.ArrayList;
import java.util.Collections;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.inventorys.editor.ItemEditor;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.providers.TitleManager;
import com.onyxshop.util.FireworkUtil;
import com.onyxshop.util.ItemBuilder;
import com.onyxshop.util.XMaterial;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class FireworkOptions extends ItemEditor {
   private final ShopItem item;

   public FireworkOptions(Player player, ShopItem shopItem) {
      this.item = shopItem;
      this.initInventory(player);
      this.fillInventory();
      this.open(player);
   }

   private void initInventory(Player player) {
      this.inv = OnyxShop.getInstance().getMetaUtils().createInventory(this, 18, TitleManager.get(TitleManager.Menu.ITEM_EDITOR_FIREWORK, player));
      int value = ConfigManager.getShop(this.item.section).getInt("pages." + this.item.itemLoc + ".duration", 1);
      ItemBuilder itemBuilder = new ItemBuilder(XMaterial.GUNPOWDER.parseItem())
         .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_FIREWORK_DURATION.get()))
         .withLore(Lang.CURRENT_VALUE.get().getLegacy().replace("%option%", Lang.OPTION_FIREWORK_DURATION.get().getLegacy()) + ChatColor.GREEN + value)
         .setAmount(value);
      this.inv
         .setItem(
            0,
            this.setEditOption(
               this.setNBTString(itemBuilder.build(), "value", String.valueOf(value)), com.onyxshop.methodes.ItemEditor.Option.FIREWORK_DURATION
            )
         );
      ItemBuilder itemBuilder2 = new ItemBuilder(XMaterial.BLUE_DYE.parseItem())
         .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_FIREWORK_COLORS.get()));
      if (!ConfigManager.getShop(this.item.section).getStringList("pages." + this.item.itemLoc + ".colors").isEmpty()) {
         ArrayList values = new ArrayList();
         values.add(Lang.CURRENT_VALUE.get().getLegacy().replace("%option%", Lang.OPTION_FIREWORK_COLORS.get().getLegacy()));
         ConfigManager.getShop(this.item.section).getStringList("pages." + this.item.itemLoc + ".colors").forEach(entry -> {
            if (FireworkUtil.FireworkColor.matchColor(entry) != null) {
               if (OnyxShop.getInstance().version >= 116) {
                  values.add("- " + net.md_5.bungee.api.ChatColor.of(FireworkUtil.FireworkColor.matchColor(entry).getColor()) + entry);
               } else {
                  values.add("- " + entry);
               }
            }
         });
         itemBuilder2.setLore(values);
      }

      this.inv.setItem(1, this.setEditOption(itemBuilder2.build(), com.onyxshop.methodes.ItemEditor.Option.FIREWORK_COLORS));
      ItemBuilder itemBuilder3 = new ItemBuilder(XMaterial.GREEN_DYE.parseItem())
         .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_FIREWORK_FADE_COLORS.get()));
      if (!ConfigManager.getShop(this.item.section).getStringList("pages." + this.item.itemLoc + ".fade-colors").isEmpty()) {
         ArrayList values = new ArrayList();
         values.add(Lang.CURRENT_VALUE.get().getLegacy().replace("%option%", Lang.OPTION_FIREWORK_FADE_COLORS.get().getLegacy()));
         ConfigManager.getShop(this.item.section).getStringList("pages." + this.item.itemLoc + ".fade-colors").forEach(entry -> {
            if (FireworkUtil.FireworkColor.matchColor(entry) != null) {
               if (OnyxShop.getInstance().version >= 116) {
                  values.add("- " + net.md_5.bungee.api.ChatColor.of(FireworkUtil.FireworkColor.matchColor(entry).getColor()) + entry);
               } else {
                  values.add("- " + entry);
               }
            }
         });
         itemBuilder3.setLore(values);
      }

      this.inv.setItem(2, this.setEditOption(itemBuilder3.build(), com.onyxshop.methodes.ItemEditor.Option.FIREWORK_FADE_COLORS));
      boolean flag = ConfigManager.getShop(this.item.section).getBoolean("pages." + this.item.itemLoc + ".flicker", false);
      ItemBuilder itemBuilder4 = new ItemBuilder(flag ? XMaterial.GLOWSTONE_DUST.parseItem() : XMaterial.GUNPOWDER.parseItem())
         .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_FIREWORK_FLICKER.get()))
         .withCLore(
            Collections.singletonList(
               Lang.CURRENT_VALUE
                  .get()
                  .replace("%option%", Lang.OPTION_FIREWORK_FLICKER.get())
                  .builder()
                  .color((char)(flag ? 'a' : 'c'))
                  .append(flag ? "true" : "false")
                  .build()
            )
         );
      this.inv
         .setItem(
            3,
            this.setEditOption(
               this.setNBTString(itemBuilder4.build(), "value", String.valueOf(flag)), com.onyxshop.methodes.ItemEditor.Option.FIREWORK_FLICKER
            )
         );
      boolean flag2 = ConfigManager.getShop(this.item.section).getBoolean("pages." + this.item.itemLoc + ".trail");
      ItemBuilder itemBuilder5 = new ItemBuilder(flag2 ? XMaterial.DIAMOND_BLOCK.parseItem() : XMaterial.DIAMOND.parseItem())
         .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_FIREWORK_TRAIL.get()))
         .withCLore(
            Collections.singletonList(
               Lang.CURRENT_VALUE
                  .get()
                  .replace("%option%", Lang.OPTION_FIREWORK_TRAIL.get())
                  .builder()
                  .color((char)(flag2 ? 'a' : 'c'))
                  .append(flag2 ? "true" : "false")
                  .build()
            )
         );
      this.inv
         .setItem(
            4,
            this.setEditOption(
               this.setNBTString(itemBuilder5.build(), "value", String.valueOf(flag2)), com.onyxshop.methodes.ItemEditor.Option.FIREWORK_TRAIL
            )
         );
      String text = ConfigManager.getShop(this.item.section).getString("pages." + this.item.itemLoc + ".shape", "BALL");
      ItemBuilder itemBuilder6 = new ItemBuilder(
            text.equalsIgnoreCase("BALL_LARGE")
               ? XMaterial.FIRE_CHARGE.parseItem()
               : (
                  text.equalsIgnoreCase("STAR")
                     ? XMaterial.GOLD_NUGGET.parseItem()
                     : (
                        text.equalsIgnoreCase("BURST")
                           ? XMaterial.FEATHER.parseItem()
                           : (text.equalsIgnoreCase("CREEPER") ? XMaterial.CREEPER_HEAD.parseItem() : XMaterial.FIREWORK_STAR.parseItem())
                     )
               )
         )
         .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_FIREWORK_SHAPE.get()))
         .withLore(Lang.CURRENT_VALUE.get().getLegacy().replace("%option%", Lang.OPTION_FIREWORK_SHAPE.get().getLegacy()) + ChatColor.GREEN + text);
      this.inv
         .setItem(5, this.setEditOption(this.setNBTString(itemBuilder6.build(), "value", text), com.onyxshop.methodes.ItemEditor.Option.FIREWORK_SHAPE));
      this.inv.setItem(9, OnyxShop.getInstance().createItem.getPlayerReminder(player, false));
      this.inv.setItem(17, new ItemBuilder(Material.BARRIER).withDisplayName(Lang.BACK.get()).build());
   }

   private ItemStack setNBTString(ItemStack itemStack, String itemId, String text) {
      return OnyxShop.getInstance().versionHandler.setOnyxNbtString(itemStack, itemId, text);
   }

   private ItemStack setEditOption(ItemStack itemStack, com.onyxshop.methodes.ItemEditor.Option option) {
      return OnyxShop.getInstance().versionHandler.setEditOption(itemStack, option.getKey());
   }

   public ShopItem getItem() {
      return this.item;
   }
}
