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

   public Give(OnyxShop var1, Methods var2) {
      this.plugin = var1;
      this.methods = var2;
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
   public boolean hasPermission(CommandSender var1) {
      return PermissionsCache.hasPermission(var1, "OnyxShop.editshop.shopstands." + this.getName());
   }

   @Override
   public void perform(Object var1, String[] var2) {
      if (var1 instanceof Player) {
         if (var2.length > 2) {
            try {
               this.type = StandType.fromName(var2[2]);
            } catch (StandLoadException var7) {
               SendMessage.sendMessage(var1, Lang.INVALID_SHOP_STAND_TYPE.get().replace("%values%", String.join(",", StandType.getSupported())));
               return;
            }

            if (!this.type.getType().isSupported()) {
               SendMessage.sendMessage(var1, Lang.STAND_TYPE_NOT_SUPPORTED.get().replace("%type%", this.type.name()));
            } else if (var2.length > 3) {
               this.section = this.methods.getSection(var1, var2[3]);
               if (this.section != null) {
                  if (var2.length > 4) {
                     this.itemLoc = this.methods.getShopItemLoc(var1, this.section, var2[4]);
                     if (this.itemLoc != null) {
                        String var3 = this.section + "." + this.itemLoc;
                        ItemBuilder var4 = new ItemBuilder(this.type.getType().parseItem());
                        var4.setDisplayName(Lang.DEFAULT_SHOP_STAND_ITEM_NAME.get());
                        var4.withLore(Lang.DEFAULT_SHOP_STAND_ITEM_LORE.get().replace("%item%", var3));
                        ItemStack var5 = this.plugin.versionHandler.setNBTString(var4.build(), "shopstand_item", var3);
                        Player var6 = (Player)var1;
                        if (var6.getInventory().firstEmpty() == -1) {
                           SendMessage.chatToPlayer(var6, Lang.NOT_ENOUGH_SPACE_INSIDE_INVENTORY.get());
                        } else {
                           var6.getInventory().addItem(new ItemStack[]{var5});
                           SendMessage.chatToPlayer(var6, Lang.SHOP_STAND_ITEM_RECEIVED.get().replace("%amount%", String.valueOf(1)).replace("%item%", var3));
                        }
                     }
                  } else {
                     SendMessage.sendMessage(var1, this.getSyntax());
                  }
               }
            } else {
               SendMessage.sendMessage(var1, this.getSyntax());
            }
         } else {
            SendMessage.sendMessage(var1, this.getSyntax());
         }
      }
   }

   private boolean isSection(String var1) {
      if (this.plugin.getShopSections().contains(var1)) {
         this.section = var1;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public List<String> getTabCompletion(String[] var1) {
      switch (var1.length) {
         case 3:
            if (!var1[2].isEmpty()) {
               ArrayList var4 = new ArrayList();
               StringUtil.copyPartialMatches(var1[2], StandType.getSupported(), var4);
               Collections.sort(var4);
               return var4;
            }

            return StandType.getSupported();
         case 4:
            if (!var1[3].isEmpty()) {
               ArrayList var3 = new ArrayList();
               StringUtil.copyPartialMatches(var1[3], this.plugin.getShopSections(), var3);
               Collections.sort(var3);
               return var3;
            }

            return this.plugin.getShopSections();
         case 5:
            if (!this.isSection(var1[3])) {
               return null;
            } else {
               if (!var1[2].isEmpty()) {
                  ArrayList var2 = new ArrayList();
                  StringUtil.copyPartialMatches(var1[4], this.plugin.getSection(var1[3]).getItemLocs(), var2);
                  Collections.sort(var2);
                  return var2;
               }

               return this.plugin.getSection(var1[1]).getShopItemLocs();
            }
         default:
            return null;
      }
   }
}
