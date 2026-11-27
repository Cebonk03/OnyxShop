package com.onyxshop.commands.editshop.subcommands.shopstands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.onyxshop.OnyxShop;
import com.onyxshop.commands.editshop.Methods;
import com.onyxshop.commands.editshop.SubCommad;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.stands.StandType;
import com.onyxshop.util.ItemBuilder;
import com.onyxshop.util.PermissionsCache;
import com.onyxshop.util.exceptions.StandLoadException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

public class Give extends SubCommad {
   private final Methods methods;
   private final OnyxShop plugin;
   StandType type;
   String section;
   String itemLoc;

   public Give(OnyxShop plugin, Methods methods) {
      this.plugin = plugin;
      this.methods = methods;
   }

   @Override
   public String getName() {
      return "give";
   }

   @Override
   public String getDescription() {
      return Lang.EDITSHOP_SHOPSTANDS_GIVE_SUBCOMMAND_DESC.get().getLegacy();
   }

   @Override
   public String getSyntax() {
      return Lang.EDITSHOP_SHOPSTANDS_GIVE_SUBCOMMAND_SYNTAX.get().getLegacy();
   }

   @Override
   public boolean hasPermission(CommandSender sender) {
      return PermissionsCache.hasPermission(sender, "OnyxShop.editshop.shopstands." + this.getName());
   }

   @Override
   public void perform(Object value, String[] values) {
      if (value instanceof Player) {
         if (values.length > 2) {
            try {
               this.type = StandType.fromName(values[2]);
            } catch (StandLoadException exception) {
               SendMessage.sendMessage(value, Lang.INVALID_SHOP_STAND_TYPE.get().replace("%values%", String.join(",", StandType.getSupported())));
               return;
            }

            if (!this.type.getType().isSupported()) {
               SendMessage.sendMessage(value, Lang.STAND_TYPE_NOT_SUPPORTED.get().replace("%type%", this.type.name()));
            } else if (values.length > 3) {
               this.section = this.methods.getSection(value, values[3]);
               if (this.section != null) {
                  if (values.length > 4) {
                     this.itemLoc = this.methods.getShopItemLoc(value, this.section, values[4]);
                     if (this.itemLoc != null) {
                        String sectionId = this.section + "." + this.itemLoc;
                        ItemBuilder itemBuilder = new ItemBuilder(this.type.getType().parseItem());
                        itemBuilder.setDisplayName(Lang.DEFAULT_SHOP_STAND_ITEM_NAME.get());
                        itemBuilder.withLore(Lang.DEFAULT_SHOP_STAND_ITEM_LORE.get().replace("%item%", sectionId));
                        ItemStack itemStack = this.plugin.versionHandler.setNBTString(itemBuilder.build(), "shopstand_item", sectionId);
                        Player player = (Player)value;
                        if (player.getInventory().firstEmpty() == -1) {
                           SendMessage.chatToPlayer(player, Lang.NOT_ENOUGH_SPACE_INSIDE_INVENTORY.get());
                        } else {
                           player.getInventory().addItem(new ItemStack[]{itemStack});
                           SendMessage.chatToPlayer(player, Lang.SHOP_STAND_ITEM_RECEIVED.get().replace("%amount%", String.valueOf(1)).replace("%item%", sectionId));
                        }
                     }
                  } else {
                     SendMessage.sendMessage(value, this.getSyntax());
                  }
               }
            } else {
               SendMessage.sendMessage(value, this.getSyntax());
            }
         } else {
            SendMessage.sendMessage(value, this.getSyntax());
         }
      }
   }

   private boolean isSection(String sectionId) {
      if (this.plugin.getShopSections().contains(sectionId)) {
         this.section = sectionId;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public List<String> getTabCompletion(String[] values) {
      switch (values.length) {
         case 3:
            if (!values[2].isEmpty()) {
               ArrayList values2 = new ArrayList();
               StringUtil.copyPartialMatches(values[2], StandType.getSupported(), values2);
               Collections.sort(values2);
               return values2;
            }

            return StandType.getSupported();
         case 4:
            if (!values[3].isEmpty()) {
               ArrayList values2 = new ArrayList();
               StringUtil.copyPartialMatches(values[3], this.plugin.getShopSections(), values2);
               Collections.sort(values2);
               return values2;
            }

            return this.plugin.getShopSections();
         case 5:
            if (!this.isSection(values[3])) {
               return null;
            } else {
               if (!values[2].isEmpty()) {
                  ArrayList values2 = new ArrayList();
                  StringUtil.copyPartialMatches(values[4], this.plugin.getSection(values[3]).getItemLocs(), values2);
                  Collections.sort(values2);
                  return values2;
               }

               return this.plugin.getSection(values[1]).getShopItemLocs();
            }
         default:
            return null;
      }
   }
}
