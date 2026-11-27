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

   public ChooseValue(ChooseValue chooseValue, Player player, int value) {
      this(player, chooseValue.item, chooseValue.option, chooseValue.action, chooseValue.index, value, chooseValue.query);
   }

   public ChooseValue(
      Player player,
      ShopItem shopItem,
      com.onyxshop.methodes.ItemEditor.Option option,
      com.onyxshop.methodes.ItemEditor.Action action,
      int value,
      int value2
   ) {
      this(player, shopItem, option, action, value, value2, "");
   }

   public ChooseValue(
      Player player,
      ShopItem shopItem,
      com.onyxshop.methodes.ItemEditor.Option option,
      com.onyxshop.methodes.ItemEditor.Action action,
      int value,
      int value2,
      String text
   ) {
      this.item = shopItem;
      this.option = option;
      this.action = action;
      this.index = value;
      this.page = value2;
      this.query = text;
      this.initInventory(player, text);
      this.fillInventory();
      this.open(player);
   }

   private void initInventory(Player player, String playerName) {
      ItemStack itemStack = this.action != com.onyxshop.methodes.ItemEditor.Action.ADD_ITEM && !this.item.hasItemError()
         ? this.item.getShopItem()
         : null;
      ArrayList<ItemStack> values = new ArrayList<>();
      int page = this.page * 45 - 45;
      int value = 0;
      int value2 = 0;
      int value3 = 0;
      switch (this.option) {
         case MATERIAL:
         case RECIPES:
            List<ItemStack> supportedItems = OnyxShop.getInstance().getSupportedItems();
            if (!playerName.isEmpty()) {
               supportedItems = supportedItems.stream().filter(element -> element.getType().name().toLowerCase(Locale.ROOT).contains(playerName)).collect(Collectors.toList());
            }

            value = supportedItems.size() - (itemStack == null ? 0 : 1);
            value2 = value - page;

            for (int index = 0; index < value2 && value3 < 45; index++) {
               ItemStack itemStack2 = (ItemStack)supportedItems.get(page + index);
               if (itemStack == null || !itemStack2.getType().equals(itemStack.getType())) {
                  values.add(itemStack2);
                  value3++;
               }
            }
            break;
         case ENCHANTMENTS:
            List<SimpleEnchantment> availableEnchants = OnyxShop.getInstance().enchantsProvider.getAvailableEnchants();
            if (!playerName.isEmpty()) {
               availableEnchants = availableEnchants.stream().filter(entry -> entry.getKey().toLowerCase(Locale.ROOT).contains(playerName)).collect(Collectors.toList());
            }

            List<SimpleEnchantment> enchantsOfItem = OnyxShop.getInstance().enchantsProvider.getEnchantsOfItem(itemStack);
            SimpleEnchantment simpleEnchantment = this.itemEditor.getChosenEnchantment(this.item.section, this.item.itemLoc, this.index);
            value = availableEnchants.size() - enchantsOfItem.size();
            value2 = value - page;

            for (int size = 0; size < value2 + enchantsOfItem.size() && value3 < 45; size++) {
               SimpleEnchantment simpleEnchantment2 = (SimpleEnchantment)availableEnchants.get(page + size);
               if (enchantsOfItem.isEmpty()
                  || simpleEnchantment != null && simpleEnchantment.getKey().equals(simpleEnchantment2.getKey())
                  || enchantsOfItem.stream().noneMatch(entry -> entry.getKey().equals(simpleEnchantment2.getKey()))) {
                  values.add(simpleEnchantment2.getBook());
                  value3++;
               }
            }
            break;
         case POTIONTYPES:
            List<PotionTypes> instance = OnyxShop.getInstance().supportedPots;
            if (!playerName.isEmpty()) {
               instance = instance.stream().filter(element -> element.name().toLowerCase(Locale.ROOT).contains(playerName)).collect(Collectors.toList());
            }

            List<PotionTypes> potionTypes = PotionTypes.getPotionTypes(itemStack);
            PotionEffect potionEffect = this.itemEditor.getChosenPotionEffectType(this.item.section, this.item.itemLoc, this.index);
            value = instance.size() - potionTypes.size();
            value2 = value - page;

            for (int index = 0; index < value2 + potionTypes.size() && value3 <= 45; index++) {
               PotionTypes potionTypes2 = (PotionTypes)instance.get(page + index);
               if (potionTypes.isEmpty() || potionEffect != null && potionEffect.equals(potionTypes2.parsePotionEffect()) || !potionTypes.contains(potionTypes2)) {
                  values.add(potionTypes2.getPotion());
                  value3++;
               }
            }
            break;
         case SPAWNERTYPE:
            List<ItemStack> instance2 = OnyxShop.getInstance().supportedEntities;
            if (!playerName.isEmpty()) {
               instance2 = instance2.stream()
                  .filter(element -> element.getItemMeta().getDisplayName().toLowerCase(Locale.ROOT).contains(playerName))
                  .collect(Collectors.toList());
            }

            String spawnedType = OnyxShop.getInstance().getSpawnerManager().getProvider().getSpawnedType(itemStack);
            value = instance2.size() - (spawnedType == null ? 0 : 1);
            value2 = value - page;

            for (int index = 0; index < value2 && value3 <= 45; index++) {
               ItemStack itemStack2 = (ItemStack)instance2.get(page + index);
               if (itemStack != null && !itemStack2.getItemMeta().getDisplayName().equalsIgnoreCase(spawnedType)) {
                  values.add(itemStack2);
                  value3++;
               }
            }
            break;
         case FIREWORK_COLORS:
         case FIREWORK_FADE_COLORS:
            List<FireworkUtil.FireworkColor> values2 = Arrays.asList(FireworkUtil.FireworkColor.values());
            if (!playerName.isEmpty()) {
               values2 = values2.stream().filter(element -> element.name().toLowerCase(Locale.ROOT).contains(playerName)).collect(Collectors.toList());
            }

            List<FireworkUtil.FireworkColor> values3 = FireworkUtil.FireworkColor.matchColorsNames(
               ConfigManager.getShop(this.item.section).getStringList("pages." + this.item.itemLoc + "." + this.option.getKey())
            );
            value = 16 - values3.size();
            value2 = value;

            for (FireworkUtil.FireworkColor fireworkColor : values2) {
               if (!values3.contains(fireworkColor)) {
                  ItemBuilder itemBuilder = new ItemBuilder(XMaterial.matchXMaterial("WOOL:" + fireworkColor.getId()).get().parseItem())
                     .withDisplayName(
                        OnyxShop.getInstance().version >= 116
                           ? ChatColor.of(fireworkColor.getColor()) + OnyxShop.getInstance().getMaterialName(fireworkColor.name())
                           : OnyxShop.getInstance().getMaterialName(fireworkColor.name())
                     );
                  values.add(itemBuilder.build());
               }
            }
            break;
         case SUB_SECTION:
            Collection<SectionItem> values4 = OnyxShop.getInstance().getSectionItems().values();
            if (!playerName.isEmpty()) {
               values4 = values4.stream().filter(element -> element.section.toLowerCase(Locale.ROOT).contains(playerName)).collect(Collectors.toList());
            }

            value = values4.size() - (this.item.isLinked() ? 1 : 0);
            value2 = value;

            for (SectionItem sectionItem : values4) {
               values.add(
                  OnyxShop.getInstance().versionHandler.setNBTString(new ItemBuilder(sectionItem.getItem(player)).removeLore().build(), "section", sectionItem.section)
               );
            }
      }

      this.allPages = (int)Math.ceil(value / 45.0);
      int slots = CalculateAmount.getSlots(value2);
      this.inv = OnyxShop.getInstance().getMetaUtils().createInventory(this, slots, TitleManager.get(TitleManager.Menu.ITEM_EDITOR_CHOOSE_VALUE, player));

      for (ItemStack itemStack2 : values) {
         this.inv.addItem(new ItemStack[]{itemStack2});
      }

      this.inv.setItem(slots - 5, this.getCompass());
      this.inv.setItem(slots - 9, OnyxShop.getInstance().createItem.getPlayerReminder(player, false));
      this.inv.setItem(slots - 1, new ItemBuilder(Material.BARRIER).withDisplayName(Lang.BACK.get()).build());
      this.inv.setItem(slots - 6, new ItemBuilder(XMaterial.RED_STAINED_GLASS_PANE.parseItem()).withDisplayName(Lang.PREVIOUS_PAGE.get()).build());
      this.inv.setItem(slots - 4, new ItemBuilder(XMaterial.LIGHT_BLUE_STAINED_GLASS_PANE.parseItem()).withDisplayName(Lang.NEXT_PAGE.get()).build());
   }

   private ItemStack getCompass() {
      ItemStack itemStack = new ItemBuilder(XMaterial.COMPASS.parseItem())
         .withDisplayName(Lang.CURRENT_PAGE.get().getLegacy() + " " + this.page + "/" + this.allPages)
         .build();
      ItemBuilder itemBuilder = new ItemBuilder(itemStack).addLore(org.bukkit.ChatColor.GREEN + "Click to search by name");
      if (!this.query.isEmpty()) {
         itemBuilder.addLore(org.bukkit.ChatColor.BLUE + "Current search: " + org.bukkit.ChatColor.WHITE + this.query);
      }

      return itemBuilder.build();
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
