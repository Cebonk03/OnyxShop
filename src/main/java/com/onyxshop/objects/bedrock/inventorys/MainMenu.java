package com.onyxshop.objects.bedrock.inventorys;

import java.util.LinkedHashSet;
import java.util.Set;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.SectionItem;
import com.onyxshop.objects.bedrock.BedrockInventory;
import com.onyxshop.providers.TitleManager;
import com.onyxshop.util.CommandHelper;
import com.onyxshop.util.PermissionsCache;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.form.SimpleForm.Builder;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

public class MainMenu extends BedrockInventory {
   private static final Set<String> SECTIONS = getSections();

   public MainMenu(OnyxShop plugin, boolean flag) {
      super(flag ? plugin.mainMenuSize : SECTIONS.size() + (plugin.navBar.isEnableMainNav() ? 9 : 0));
   }

   @Override
   public void openForm(OnyxShop plugin, Player player, FloodgatePlayer floodgatePlayer) {
      this.p = player;
      Builder builder = (Builder)SimpleForm.builder().title(TitleManager.get(TitleManager.Menu.MAIN_MENU, player).getBedrockLegacy());
      String[] text = new String[this.items.length];
      int value = 0;

      for (String text2 : SECTIONS) {
         SectionItem sectionItem = plugin.getSectionItem(text2);
         if (sectionItem != null && (!sectionItem.isHideIfNoPerms() || PermissionsCache.hasPermission(player, "OnyxShop.shop." + text2.toLowerCase()))) {
            text[value] = text2;
            this.addItem(builder, sectionItem.getItem(player), value++, sectionItem.getBedrockTexture());
         }
      }

      int value2 = value;
      if (plugin.navBar.isEnableMainNav()) {
         ItemStack[] mainMenuNavBar = plugin.navBar.getMainMenuNavBar(player);
         String[] mainMenuNavTextures = plugin.navBar.getMainMenuNavTextures();

         for (int index = 0; index < mainMenuNavBar.length; index++) {
            if (mainMenuNavBar[index] != null) {
               this.addItem(builder, mainMenuNavBar[index], value++, mainMenuNavTextures[index]);
            }
         }
      }

      builder.validResultHandler(element -> {
         int id = element.clickedButtonId();
         if (this.items[id] != null) {
            if (!plugin.navBar.isEnableMainNav() || id < value2) {
               String text2 = text[id];
               if (text2 != null && plugin.getSections().containsKey(text2)) {
                  if (PermissionsCache.hasPermission(player, "OnyxShop.shop." + text2.toLowerCase())) {
                     com.onyxshop.objects.shops.ShopSection section = plugin.getSection(text2);
                     if (!section.isCommandItem()) {
                        section.openShopSection(player, false);
                     } else {
                        section.getClickCommands().forEach(entry -> CommandHelper.executeCommand(null, player, entry));
                        if (!section.isCloseMenu()) {
                           this.openForm(plugin, player, floodgatePlayer);
                        }
                     }

                     plugin.getSoundEffects().openShop(player);
                  } else if (!plugin.getSectionItem(text2).isHideIfNoPerms()) {
                     SendMessage.chatToPlayer(player, Lang.NO_PERMISSIONS_TO_OPEN_SHOP.get(player));
                  }
               }
            } else if (!plugin.navBar.execute(this, player, id - value2)) {
               this.openForm(plugin, player, floodgatePlayer);
            }
         }
      });
      floodgatePlayer.sendForm(builder);
   }

   @Override
   public void openGridForm(OnyxShop plugin, Player player, FloodgatePlayer floodgatePlayer, boolean flag) {
      this.p = player;
      this.buildGridForm(plugin, player, floodgatePlayer, flag);
   }

   private void buildGridForm(OnyxShop plugin, Player player, FloodgatePlayer floodgatePlayer, boolean flag) {
      Builder builder = (Builder)SimpleForm.builder().title(this.getTitle(TitleManager.get(TitleManager.Menu.MAIN_MENU, player), this.items.length, 99, flag));

      for (int index = 0; index < (plugin.navBar.isEnableMainNav() ? this.items.length - 9 : this.items.length); index++) {
         String mainMenuSectionForSlot = plugin.getMainMenuSectionForSlot(index);
         SectionItem sectionItem = mainMenuSectionForSlot == null ? null : plugin.getSectionItem(mainMenuSectionForSlot);
         if (mainMenuSectionForSlot == null || sectionItem == null || sectionItem.isHideIfNoPerms() && !PermissionsCache.hasPermission(player, "OnyxShop.shop." + mainMenuSectionForSlot.toLowerCase())) {
            if (plugin.createItem.fillItem != null && index < (plugin.navBar.isEnableMainNav() ? this.items.length - 9 : this.items.length)) {
               this.addGridItem(builder, plugin.createItem.fillItem, true, index, false);
            } else {
               builder.button("null");
            }
         } else {
            this.addGridItem(builder, sectionItem.getItem(player), false, index, false, sectionItem.getBedrockTexture());
         }
      }

      if (plugin.navBar.isEnableMainNav()) {
         int value = this.items.length - 9;
         ItemStack[] mainMenuNavBar = plugin.navBar.getMainMenuNavBar(player);
         String[] mainMenuNavTextures = plugin.navBar.getMainMenuNavTextures();

         for (int index = 0; index < mainMenuNavBar.length; index++) {
            if (mainMenuNavBar[index] != null) {
               this.addGridItem(builder, mainMenuNavBar[index], false, value, false, mainMenuNavTextures[index]);
            } else {
               builder.button("null");
            }

            value++;
         }
      }

      builder.validResultHandler(element -> {
         int id = element.clickedButtonId();
         if (this.items[id] != null) {
            this.handleClick(builder, plugin, player, floodgatePlayer, id, -1, flag);
         }
      });
      floodgatePlayer.sendForm(builder);
   }

   private void reopenGridForm(Builder builder, OnyxShop plugin, Player player, FloodgatePlayer floodgatePlayer, boolean flag, int value, int value2) {
      if (flag && value != value2) {
         builder = (Builder)SimpleForm.builder().title(this.getTitle(TitleManager.get(TitleManager.Menu.MAIN_MENU, player), this.items.length, value, flag));

         for (int index = 0; index < this.items.length; index++) {
            if (this.items[index] != null) {
               this.readdGridItem(builder, index, value == index);
            } else {
               builder.button("null");
            }
         }

         value2 = value;
      }

      int value3 = value2;
      Builder builder2 = builder;
      builder.validResultHandler(element -> {
         int id = element.clickedButtonId();
         if (this.items[id] != null) {
            this.handleClick(builder2, plugin, player, floodgatePlayer, id, value3, flag);
         }
      });
      floodgatePlayer.sendForm(builder);
   }

   private void handleClick(Builder builder, OnyxShop plugin, Player player, FloodgatePlayer floodgatePlayer, int value, int value2, boolean flag) {
      if (flag && value2 != value) {
         this.reopenGridForm(builder, plugin, player, floodgatePlayer, flag, value, value2);
      } else {
         if (this.items[value].isFillItem()) {
            this.reopenGridForm(builder, plugin, player, floodgatePlayer, flag, 99, value2);
         }

         if (!plugin.navBar.isEnableMainNav() || value < this.items.length - 9 || value > this.items.length - 1) {
            String mainMenuSectionForSlot = plugin.getMainMenuSectionForSlot(value);
            if (mainMenuSectionForSlot != null && plugin.getSections().containsKey(mainMenuSectionForSlot)) {
               if (PermissionsCache.hasPermission(player, "OnyxShop.shop." + mainMenuSectionForSlot.toLowerCase())) {
                  com.onyxshop.objects.shops.ShopSection section = plugin.getSection(mainMenuSectionForSlot);
                  if (!section.isCommandItem()) {
                     section.openShopSection(player, false);
                  } else {
                     section.getClickCommands().forEach(entry -> CommandHelper.executeCommand(null, player, entry));
                     if (!section.isCloseMenu()) {
                        this.reopenGridForm(builder, plugin, player, floodgatePlayer, flag, 99, value2);
                     }
                  }

                  plugin.getSoundEffects().openShop(player);
               } else if (!plugin.getSectionItem(mainMenuSectionForSlot).isHideIfNoPerms()) {
                  SendMessage.chatToPlayer(player, Lang.NO_PERMISSIONS_TO_OPEN_SHOP.get(player));
               }
            }
         } else if (!plugin.navBar.execute(this, player, value - (this.items.length - 9))) {
            this.reopenGridForm(builder, plugin, player, floodgatePlayer, flag, 99, value2);
         }
      }
   }

   private static Set<String> getSections() {
      LinkedHashSet values = new LinkedHashSet();

      for (int size = 0; size < OnyxShop.getInstance().mainMenuSize; size++) {
         String mainMenuSectionForSlot = OnyxShop.getInstance().getMainMenuSectionForSlot(size);
         if (mainMenuSectionForSlot != null) {
            values.add(mainMenuSectionForSlot);
         }
      }

      return values;
   }
}
