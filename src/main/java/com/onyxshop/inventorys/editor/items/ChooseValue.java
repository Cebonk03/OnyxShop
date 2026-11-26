package com.onyxshop.inventorys.editor.items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.inventorys.editor.ItemEditor;
import com.onyxshop.methodes.CalculateAmount;
import com.onyxshop.objects.SectionItem;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.providers.TitleManager;
import com.onyxshop.providers.enchantments.SimpleEnchantment;
import com.onyxshop.util.FireworkUtil;
import com.onyxshop.util.ItemBuilder;
import com.onyxshop.util.PotionTypes;
import com.onyxshop.util.XMaterial;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

public class ChooseValue extends ItemEditor {
   private final ShopItem item;
   private final com.onyxshop.methodes.ItemEditor.Option option;
   private final com.onyxshop.methodes.ItemEditor.Action action;
   private final int index;
   private final int page;
   private int allPages;
   private final String query;

   public ChooseValue(ChooseValue var1, Player var2, int var3) {
      this(var2, var1.item, var1.option, var1.action, var1.index, var3, var1.query);
   }

   public ChooseValue(
      Player var1,
      ShopItem var2,
      com.onyxshop.methodes.ItemEditor.Option var3,
      com.onyxshop.methodes.ItemEditor.Action var4,
      int var5,
      int var6
   ) {
      this(var1, var2, var3, var4, var5, var6, "");
   }

   public ChooseValue(
      Player var1,
      ShopItem var2,
      com.onyxshop.methodes.ItemEditor.Option var3,
      com.onyxshop.methodes.ItemEditor.Action var4,
      int var5,
      int var6,
      String var7
   ) {
      this.item = var2;
      this.option = var3;
      this.action = var4;
      this.index = var5;
      this.page = var6;
      this.query = var7;
      this.initInventory(var1, var7);
      this.fillInventory();
      this.open(var1);
   }

   private void initInventory(Player var1, String var2) {
      ItemStack var3 = this.action != com.onyxshop.methodes.ItemEditor.Action.ADD_ITEM && !this.item.hasItemError()
         ? this.item.getShopItem()
         : null;
      ArrayList<ItemStack> var4 = new ArrayList<>();
      int var5 = this.page * 45 - 45;
      int var6 = 0;
      int var7 = 0;
      int var8 = 0;
      switch (this.option) {
         case MATERIAL:
         case RECIPES:
            List<ItemStack> var9 = OnyxShop.getInstance().getSupportedItems();
            if (!var2.isEmpty()) {
               var9 = var9.stream().filter(var1x -> var1x.getType().name().toLowerCase(Locale.ROOT).contains(var2)).collect(Collectors.toList());
            }

            var6 = var9.size() - (var3 == null ? 0 : 1);
            var7 = var6 - var5;

            for (int var24 = 0; var24 < var7 && var8 < 45; var24++) {
               ItemStack var26 = (ItemStack)var9.get(var5 + var24);
               if (var3 == null || !var26.getType().equals(var3.getType())) {
                  var4.add(var26);
                  var8++;
               }
            }
            break;
         case ENCHANTMENTS:
            List<SimpleEnchantment> var10 = OnyxShop.getInstance().enchantsProvider.getAvailableEnchants();
            if (!var2.isEmpty()) {
               var10 = var10.stream().filter(var1x -> var1x.getKey().toLowerCase(Locale.ROOT).contains(var2)).collect(Collectors.toList());
            }

            List<SimpleEnchantment> var11 = OnyxShop.getInstance().enchantsProvider.getEnchantsOfItem(var3);
            SimpleEnchantment var12 = this.itemEditor.getChosenEnchantment(this.item.section, this.item.itemLoc, this.index);
            var6 = var10.size() - var11.size();
            var7 = var6 - var5;

            for (int var28 = 0; var28 < var7 + var11.size() && var8 < 45; var28++) {
               SimpleEnchantment var29 = (SimpleEnchantment)var10.get(var5 + var28);
               if (var11.isEmpty()
                  || var12 != null && var12.getKey().equals(var29.getKey())
                  || var11.stream().noneMatch(var1x -> var1x.getKey().equals(var29.getKey()))) {
                  var4.add(var29.getBook());
                  var8++;
               }
            }
            break;
         case POTIONTYPES:
            List<PotionTypes> var13 = OnyxShop.getInstance().supportedPots;
            if (!var2.isEmpty()) {
               var13 = var13.stream().filter(var1x -> var1x.name().toLowerCase(Locale.ROOT).contains(var2)).collect(Collectors.toList());
            }

            List<PotionTypes> var14 = PotionTypes.getPotionTypes(var3);
            PotionEffect var15 = this.itemEditor.getChosenPotionEffectType(this.item.section, this.item.itemLoc, this.index);
            var6 = var13.size() - var14.size();
            var7 = var6 - var5;

            for (int var30 = 0; var30 < var7 + var14.size() && var8 <= 45; var30++) {
               PotionTypes var31 = (PotionTypes)var13.get(var5 + var30);
               if (var14.isEmpty() || var15 != null && var15.equals(var31.parsePotionEffect()) || !var14.contains(var31)) {
                  var4.add(var31.getPotion());
                  var8++;
               }
            }
            break;
         case SPAWNERTYPE:
            List<ItemStack> var16 = OnyxShop.getInstance().supportedEntities;
            if (!var2.isEmpty()) {
               var16 = var16.stream()
                  .filter(var1x -> var1x.getItemMeta().getDisplayName().toLowerCase(Locale.ROOT).contains(var2))
                  .collect(Collectors.toList());
            }

            String var17 = OnyxShop.getInstance().getSpawnerManager().getProvider().getSpawnedType(var3);
            var6 = var16.size() - (var17 == null ? 0 : 1);
            var7 = var6 - var5;

            for (int var32 = 0; var32 < var7 && var8 <= 45; var32++) {
               ItemStack var33 = (ItemStack)var16.get(var5 + var32);
               if (var3 != null && !var33.getItemMeta().getDisplayName().equalsIgnoreCase(var17)) {
                  var4.add(var33);
                  var8++;
               }
            }
            break;
         case FIREWORK_COLORS:
         case FIREWORK_FADE_COLORS:
            List<FireworkUtil.FireworkColor> var18 = Arrays.asList(FireworkUtil.FireworkColor.values());
            if (!var2.isEmpty()) {
               var18 = var18.stream().filter(var1x -> var1x.name().toLowerCase(Locale.ROOT).contains(var2)).collect(Collectors.toList());
            }

            List<FireworkUtil.FireworkColor> var19 = FireworkUtil.FireworkColor.matchColorsNames(
               ConfigManager.getShop(this.item.section).getStringList("pages." + this.item.itemLoc + "." + this.option.getKey())
            );
            var6 = 16 - var19.size();
            var7 = var6;

            for (FireworkUtil.FireworkColor var35 : var18) {
               if (!var19.contains(var35)) {
                  ItemBuilder var36 = new ItemBuilder(XMaterial.matchXMaterial("WOOL:" + var35.getId()).get().parseItem())
                     .withDisplayName(
                        OnyxShop.getInstance().version >= 116
                           ? ChatColor.of(var35.getColor()) + OnyxShop.getInstance().getMaterialName(var35.name())
                           : OnyxShop.getInstance().getMaterialName(var35.name())
                     );
                  var4.add(var36.build());
               }
            }
            break;
         case SUB_SECTION:
            Collection<SectionItem> var20 = OnyxShop.getInstance().getSectionItems().values();
            if (!var2.isEmpty()) {
               var20 = var20.stream().filter(var1x -> var1x.section.toLowerCase(Locale.ROOT).contains(var2)).collect(Collectors.toList());
            }

            var6 = var20.size() - (this.item.isLinked() ? 1 : 0);
            var7 = var6;

            for (SectionItem var22 : var20) {
               var4.add(
                  OnyxShop.getInstance().versionHandler.setNBTString(new ItemBuilder(var22.getItem(var1)).removeLore().build(), "section", var22.section)
               );
            }
      }

      this.allPages = (int)Math.ceil(var6 / 45.0);
      int var23 = CalculateAmount.getSlots(var7);
      this.inv = OnyxShop.getInstance().getMetaUtils().createInventory(this, var23, TitleManager.get(TitleManager.Menu.ITEM_EDITOR_CHOOSE_VALUE, var1));

      for (ItemStack var27 : var4) {
         this.inv.addItem(new ItemStack[]{var27});
      }

      this.inv.setItem(var23 - 5, this.getCompass());
      this.inv.setItem(var23 - 9, OnyxShop.getInstance().createItem.getPlayerReminder(var1, false));
      this.inv.setItem(var23 - 1, new ItemBuilder(Material.BARRIER).withDisplayName(Lang.BACK.get()).build());
      this.inv.setItem(var23 - 6, new ItemBuilder(XMaterial.RED_STAINED_GLASS_PANE.parseItem()).withDisplayName(Lang.PREVIOUS_PAGE.get()).build());
      this.inv.setItem(var23 - 4, new ItemBuilder(XMaterial.LIGHT_BLUE_STAINED_GLASS_PANE.parseItem()).withDisplayName(Lang.NEXT_PAGE.get()).build());
   }

   private ItemStack getCompass() {
      ItemStack var1 = new ItemBuilder(XMaterial.COMPASS.parseItem())
         .withDisplayName(Lang.CURRENT_PAGE.get().getLegacy() + " " + this.page + "/" + this.allPages)
         .build();
      ItemBuilder var2 = new ItemBuilder(var1).addLore(org.bukkit.ChatColor.GREEN + "Click to search by name");
      if (!this.query.isEmpty()) {
         var2.addLore(org.bukkit.ChatColor.BLUE + "Current search: " + org.bukkit.ChatColor.WHITE + this.query);
      }

      return var2.build();
   }

   public ShopItem getItem() {
      return this.item;
   }

   public com.onyxshop.methodes.ItemEditor.Option getOption() {
      return this.option;
   }

   public com.onyxshop.methodes.ItemEditor.Action getAction() {
      return this.action;
   }

   public int getIndex() {
      return this.index;
   }

   public int getPage() {
      return this.page;
   }

   public int getAllPages() {
      return this.allPages;
   }

   public String getQuery() {
      return this.query;
   }
}
