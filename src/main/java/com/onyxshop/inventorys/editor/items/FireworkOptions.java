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

   public FireworkOptions(Player var1, ShopItem var2) {
      this.item = var2;
      this.initInventory(var1);
      this.fillInventory();
      this.open(var1);
   }

   private void initInventory(Player var1) {
      this.inv = OnyxShop.getInstance().getMetaUtils().createInventory(this, 18, TitleManager.get(TitleManager.Menu.ITEM_EDITOR_FIREWORK, var1));
      int var2 = ConfigManager.getShop(this.item.section).getInt("pages." + this.item.itemLoc + ".duration", 1);
      ItemBuilder var3 = new ItemBuilder(XMaterial.GUNPOWDER.parseItem())
         .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_FIREWORK_DURATION.get()))
         .withLore(Lang.CURRENT_VALUE.get().getLegacy().replace("%option%", Lang.OPTION_FIREWORK_DURATION.get().getLegacy()) + ChatColor.GREEN + var2)
         .setAmount(var2);
      this.inv
         .setItem(
            0,
            this.setEditOption(
               this.setNBTString(var3.build(), "value", String.valueOf(var2)), com.onyxshop.methodes.ItemEditor.Option.FIREWORK_DURATION
            )
         );
      ItemBuilder var4 = new ItemBuilder(XMaterial.BLUE_DYE.parseItem())
         .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_FIREWORK_COLORS.get()));
      if (!ConfigManager.getShop(this.item.section).getStringList("pages." + this.item.itemLoc + ".colors").isEmpty()) {
         ArrayList var5 = new ArrayList();
         var5.add(Lang.CURRENT_VALUE.get().getLegacy().replace("%option%", Lang.OPTION_FIREWORK_COLORS.get().getLegacy()));
         ConfigManager.getShop(this.item.section).getStringList("pages." + this.item.itemLoc + ".colors").forEach(var1x -> {
            if (FireworkUtil.FireworkColor.matchColor(var1x) != null) {
               if (OnyxShop.getInstance().version >= 116) {
                  var5.add("- " + net.md_5.bungee.api.ChatColor.of(FireworkUtil.FireworkColor.matchColor(var1x).getColor()) + var1x);
               } else {
                  var5.add("- " + var1x);
               }
            }
         });
         var4.setLore(var5);
      }

      this.inv.setItem(1, this.setEditOption(var4.build(), com.onyxshop.methodes.ItemEditor.Option.FIREWORK_COLORS));
      ItemBuilder var12 = new ItemBuilder(XMaterial.GREEN_DYE.parseItem())
         .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_FIREWORK_FADE_COLORS.get()));
      if (!ConfigManager.getShop(this.item.section).getStringList("pages." + this.item.itemLoc + ".fade-colors").isEmpty()) {
         ArrayList var6 = new ArrayList();
         var6.add(Lang.CURRENT_VALUE.get().getLegacy().replace("%option%", Lang.OPTION_FIREWORK_FADE_COLORS.get().getLegacy()));
         ConfigManager.getShop(this.item.section).getStringList("pages." + this.item.itemLoc + ".fade-colors").forEach(var1x -> {
            if (FireworkUtil.FireworkColor.matchColor(var1x) != null) {
               if (OnyxShop.getInstance().version >= 116) {
                  var6.add("- " + net.md_5.bungee.api.ChatColor.of(FireworkUtil.FireworkColor.matchColor(var1x).getColor()) + var1x);
               } else {
                  var6.add("- " + var1x);
               }
            }
         });
         var12.setLore(var6);
      }

      this.inv.setItem(2, this.setEditOption(var12.build(), com.onyxshop.methodes.ItemEditor.Option.FIREWORK_FADE_COLORS));
      boolean var13 = ConfigManager.getShop(this.item.section).getBoolean("pages." + this.item.itemLoc + ".flicker", false);
      ItemBuilder var7 = new ItemBuilder(var13 ? XMaterial.GLOWSTONE_DUST.parseItem() : XMaterial.GUNPOWDER.parseItem())
         .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_FIREWORK_FLICKER.get()))
         .withCLore(
            Collections.singletonList(
               Lang.CURRENT_VALUE
                  .get()
                  .replace("%option%", Lang.OPTION_FIREWORK_FLICKER.get())
                  .builder()
                  .color((char)(var13 ? 'a' : 'c'))
                  .append(var13 ? "true" : "false")
                  .build()
            )
         );
      this.inv
         .setItem(
            3,
            this.setEditOption(
               this.setNBTString(var7.build(), "value", String.valueOf(var13)), com.onyxshop.methodes.ItemEditor.Option.FIREWORK_FLICKER
            )
         );
      boolean var8 = ConfigManager.getShop(this.item.section).getBoolean("pages." + this.item.itemLoc + ".trail");
      ItemBuilder var9 = new ItemBuilder(var8 ? XMaterial.DIAMOND_BLOCK.parseItem() : XMaterial.DIAMOND.parseItem())
         .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_FIREWORK_TRAIL.get()))
         .withCLore(
            Collections.singletonList(
               Lang.CURRENT_VALUE
                  .get()
                  .replace("%option%", Lang.OPTION_FIREWORK_TRAIL.get())
                  .builder()
                  .color((char)(var8 ? 'a' : 'c'))
                  .append(var8 ? "true" : "false")
                  .build()
            )
         );
      this.inv
         .setItem(
            4,
            this.setEditOption(
               this.setNBTString(var9.build(), "value", String.valueOf(var8)), com.onyxshop.methodes.ItemEditor.Option.FIREWORK_TRAIL
            )
         );
      String var10 = ConfigManager.getShop(this.item.section).getString("pages." + this.item.itemLoc + ".shape", "BALL");
      ItemBuilder var11 = new ItemBuilder(
            var10.equalsIgnoreCase("BALL_LARGE")
               ? XMaterial.FIRE_CHARGE.parseItem()
               : (
                  var10.equalsIgnoreCase("STAR")
                     ? XMaterial.GOLD_NUGGET.parseItem()
                     : (
                        var10.equalsIgnoreCase("BURST")
                           ? XMaterial.FEATHER.parseItem()
                           : (var10.equalsIgnoreCase("CREEPER") ? XMaterial.CREEPER_HEAD.parseItem() : XMaterial.FIREWORK_STAR.parseItem())
                     )
               )
         )
         .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_FIREWORK_SHAPE.get()))
         .withLore(Lang.CURRENT_VALUE.get().getLegacy().replace("%option%", Lang.OPTION_FIREWORK_SHAPE.get().getLegacy()) + ChatColor.GREEN + var10);
      this.inv
         .setItem(5, this.setEditOption(this.setNBTString(var11.build(), "value", var10), com.onyxshop.methodes.ItemEditor.Option.FIREWORK_SHAPE));
      this.inv.setItem(9, OnyxShop.getInstance().createItem.getPlayerReminder(var1, false));
      this.inv.setItem(17, new ItemBuilder(Material.BARRIER).withDisplayName(Lang.BACK.get()).build());
   }

   private ItemStack setNBTString(ItemStack var1, String var2, String var3) {
      return OnyxShop.getInstance().versionHandler.setOnyxNbtString(var1, var2, var3);
   }

   private ItemStack setEditOption(ItemStack var1, com.onyxshop.methodes.ItemEditor.Option var2) {
      return OnyxShop.getInstance().versionHandler.setEditOption(var1, var2.getKey());
   }

   public ShopItem getItem() {
      return this.item;
   }
}
