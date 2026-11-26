package com.onyxshop.commands.editshop.subcommands.modifiers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.commands.editshop.Methods;
import com.onyxshop.commands.editshop.SubCommad;
import com.onyxshop.commands.editshop.subcommands.Modifiers;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.PriceModifierType;
import com.onyxshop.util.PermissionsCache;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class RemoveModifier extends SubCommad {
   private final Methods methods;
   private final OnyxShop plugin;

   public RemoveModifier(OnyxShop var1, Methods var2) {
      this.plugin = var1;
      this.methods = var2;
   }

   @Override
   public String getName() {
      return "remove";
   }

   @Override
   public String getDescription() {
      return ChatColor.GREEN + "Remove a price modifier for a player for a specific shop or item";
   }

   @Override
   public String getSyntax() {
      return ChatColor.GREEN + "/editshop modifiers remove <player> <section/item> <section> [item] <type>";
   }

   @Override
   public boolean hasPermission(CommandSender var1) {
      return PermissionsCache.hasPermission(var1, "OnyxShop.editshop.modifiers." + this.getName());
   }

   @Override
   public void perform(Object var1, String[] var2) {
      if (var2.length > 2) {
         OfflinePlayer var3 = Bukkit.getOfflinePlayer(var2[2]);
         if (var2.length > 3) {
            Modifiers.ModifierLocation var4;
            try {
               var4 = Modifiers.ModifierLocation.valueOf(var2[3].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException var10) {
               SendMessage.warnMessage(var1, ChatColor.RED + "Invalid location, should be SECTION or ITEM");
               return;
            }

            if (var2.length > 4) {
               String var5 = this.methods.getSection(var1, var2[4]);
               if (var5 != null) {
                  String var6 = null;
                  if (var4 == Modifiers.ModifierLocation.ITEM) {
                     if (var2.length <= 5) {
                        SendMessage.sendMessage(var1, this.getSyntax());
                        return;
                     }

                     var6 = this.methods.getShopItemLoc(var1, var5, var2[5]);
                     if (var6 == null) {
                        return;
                     }
                  }

                  if (var2.length > (var4 == Modifiers.ModifierLocation.SECTION ? 5 : 6)) {
                     PriceModifierType var7;
                     try {
                        var7 = PriceModifierType.valueOf(var2[var4 == Modifiers.ModifierLocation.SECTION ? 5 : 6].toUpperCase(Locale.ROOT));
                     } catch (IllegalArgumentException var9) {
                        SendMessage.warnMessage(var1, ChatColor.RED + "Invalid modifier type, should be BOTH, BUY or SELL");
                        return;
                     }

                     boolean var8 = false;
                     if (var4 == Modifiers.ModifierLocation.SECTION) {
                        var8 = this.plugin.getModifierManager().removeCommandSectionModifier(var3.getUniqueId(), var5, var7);
                     } else if (var4 == Modifiers.ModifierLocation.ITEM) {
                        var8 = this.plugin.getModifierManager().removeCommandItemModifier(var3.getUniqueId(), var5 + "." + var6, var7);
                     }

                     if (var8) {
                        SendMessage.sendMessage(
                           var1,
                           Lang.PLAYER_PRICE_MODIFIER_REMOVED
                              .get()
                              .replace("%location%", var4.name())
                              .replace("%player_name%", var3.getName() == null ? "null" : var3.getName())
                              .replace("%player_uuid%", var3.getUniqueId().toString())
                              .replace("%type%", var7 == PriceModifierType.BOTH ? "BUY and SELL" : var7.name())
                        );
                     } else if (this.plugin.getModifierManager().getCommandModifiersForPlayer(var3.getUniqueId()) == null) {
                        SendMessage.sendMessage(var1, Lang.NO_PLAYER_PRICE_MODIFIERS.get());
                     } else {
                        SendMessage.sendMessage(var1, Lang.NO_PLAYER_PRICE_MODIFIERS_FOR_FILTERS.get());
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
      } else {
         SendMessage.sendMessage(var1, this.getSyntax());
      }
   }

   @Override
   public List<String> getTabCompletion(String[] var1) {
      switch (var1.length) {
         case 3:
            if (!var1[2].isEmpty()) {
               List var9 = this.plugin.getServer().getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
               ArrayList var11 = new ArrayList();
               StringUtil.copyPartialMatches(var1[2], var9, var11);
               Collections.sort(var11);
               return var11;
            }

            return this.plugin.getServer().getOnlinePlayers().stream().<String>map(Player::getName).collect(Collectors.toList());
         case 4:
            if (!var1[3].isEmpty()) {
               ArrayList var8 = new ArrayList();
               StringUtil.copyPartialMatches(var1[3], Arrays.stream(Modifiers.ModifierLocation.values()).map(Enum::name).collect(Collectors.toList()), var8);
               Collections.sort(var8);
               return var8;
            }

            return Arrays.stream(Modifiers.ModifierLocation.values()).map(Enum::name).collect(Collectors.toList());
         case 5:
            if (!var1[4].isEmpty()) {
               ArrayList var7 = new ArrayList();
               StringUtil.copyPartialMatches(var1[4], this.plugin.getShopSections(), var7);
               Collections.sort(var7);
               return var7;
            }

            return this.plugin.getShopSections();
         case 6:
            Modifiers.ModifierLocation var2;
            try {
               var2 = Modifiers.ModifierLocation.valueOf(var1[3].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException var6) {
               return null;
            }

            if (var2 == Modifiers.ModifierLocation.ITEM && !this.plugin.getShopSections().contains(var1[4])) {
               return null;
            } else {
               if (!var1[5].isEmpty()) {
                  ArrayList var10 = new ArrayList();
                  StringUtil.copyPartialMatches(
                     var1[5],
                     (Iterable)(var2 == Modifiers.ModifierLocation.SECTION
                        ? Arrays.stream(PriceModifierType.values()).map(Enum::name).collect(Collectors.toList())
                        : this.plugin.getSection(var1[4]).getShopItemLocs()),
                     var10
                  );
                  Collections.sort(var10);
                  return var10;
               }

               return var2 == Modifiers.ModifierLocation.SECTION
                  ? Arrays.stream(PriceModifierType.values()).map(Enum::name).collect(Collectors.toList())
                  : this.plugin.getSection(var1[4]).getShopItemLocs();
            }
         case 7:
            Modifiers.ModifierLocation var3;
            try {
               var3 = Modifiers.ModifierLocation.valueOf(var1[3].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException var5) {
               return null;
            }

            return var3 == Modifiers.ModifierLocation.SECTION ? null : Arrays.stream(PriceModifierType.values()).map(Enum::name).collect(Collectors.toList());
         default:
            return null;
      }
   }
}
