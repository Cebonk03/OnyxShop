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

public class SetModifier extends SubCommad {
   private final Methods methods;
   private final OnyxShop plugin;

   public SetModifier(OnyxShop plugin, Methods methods) {
      this.plugin = plugin;
      this.methods = methods;
   }

   @Override
   public String getName() {
      return "set";
   }

   @Override
   public String getDescription() {
      return ChatColor.GREEN + "Set a price modifier for a player for a specific shop or item";
   }

   @Override
   public String getSyntax() {
      return ChatColor.GREEN + "/editshop modifiers set <player> <section/item> <section> [item] <type> <amount>";
   }

   @Override
   public boolean hasPermission(CommandSender sender) {
      return PermissionsCache.hasPermission(sender, "OnyxShop.editshop.modifiers." + this.getName());
   }

   @Override
   public void perform(Object value, String[] values) {
      if (values.length > 2) {
         OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(values[2]);
         if (values.length > 3) {
            Modifiers.ModifierLocation modifierLocation;
            try {
               modifierLocation = Modifiers.ModifierLocation.valueOf(values[3].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
               SendMessage.warnMessage(value, ChatColor.RED + "Invalid location, should be SECTION or ITEM");
               return;
            }

            if (values.length > 4) {
               String section = this.methods.getSection(value, values[4]);
               if (section != null) {
                  String text = null;
                  if (modifierLocation == Modifiers.ModifierLocation.ITEM) {
                     if (values.length <= 5) {
                        SendMessage.sendMessage(value, this.getSyntax());
                        return;
                     }

                     text = this.methods.getShopItemLoc(value, section, values[5]);
                     if (text == null) {
                        return;
                     }
                  }

                  if (values.length > (modifierLocation == Modifiers.ModifierLocation.SECTION ? 5 : 6)) {
                     PriceModifierType priceModifierType;
                     try {
                        priceModifierType = PriceModifierType.valueOf(values[modifierLocation == Modifiers.ModifierLocation.SECTION ? 5 : 6].toUpperCase(Locale.ROOT));
                     } catch (IllegalArgumentException exception) {
                        SendMessage.warnMessage(value, ChatColor.RED + "Invalid modifier type, should be BOTH, BUY or SELL");
                        return;
                     }

                     if (values.length > (modifierLocation == Modifiers.ModifierLocation.SECTION ? 6 : 7)) {
                        String text2 = values[modifierLocation == Modifiers.ModifierLocation.SECTION ? 6 : 7].replace("+", "").replace("%", "");

                        double amount;
                        try {
                           amount = 100.0 + Math.round(Double.parseDouble(text2) * 100.0) / 100.0;
                        } catch (NumberFormatException exception) {
                           SendMessage.sendMessage(value, Lang.NO_VALID_AMOUNT.get());
                           return;
                        }

                        if (amount < 0.0) {
                           SendMessage.warnMessage(value, "Amount cannot be below -100%");
                        } else {
                           if (modifierLocation == Modifiers.ModifierLocation.SECTION) {
                              this.plugin.getModifierManager().setCommandSectionModifier(offlinePlayer.getUniqueId(), section, priceModifierType, amount);
                           } else if (modifierLocation == Modifiers.ModifierLocation.ITEM) {
                              this.plugin.getModifierManager().setCommandItemModifier(offlinePlayer.getUniqueId(), section + "." + text, priceModifierType, amount);
                           }

                           amount -= 100.0;
                           SendMessage.infoMessage(
                              value,
                              Lang.PLAYER_PRICE_MODIFIER_SET
                                 .get()
                                 .replace("%location%", modifierLocation.name())
                                 .replace("%player_name%", offlinePlayer.getName() == null ? "null" : offlinePlayer.getName())
                                 .replace("%player_uuid%", offlinePlayer.getUniqueId().toString())
                                 .replace("%type%", priceModifierType == PriceModifierType.BOTH ? "BUY and SELL" : priceModifierType.name())
                                 .replace("%amount%", String.valueOf(amount < 0.0 ? amount : "+" + amount))
                           );
                        }
                     } else {
                        SendMessage.sendMessage(value, this.getSyntax());
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
      } else {
         SendMessage.sendMessage(value, this.getSyntax());
      }
   }

   @Override
   public List<String> getTabCompletion(String[] values) {
      switch (values.length) {
         case 3:
            if (!values[2].isEmpty()) {
               List playerNames = this.plugin.getServer().getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
               ArrayList values2 = new ArrayList();
               StringUtil.copyPartialMatches(values[2], playerNames, values2);
               Collections.sort(values2);
               return values2;
            }

            return this.plugin.getServer().getOnlinePlayers().stream().<String>map(Player::getName).collect(Collectors.toList());
         case 4:
            if (!values[3].isEmpty()) {
               ArrayList values2 = new ArrayList();
               StringUtil.copyPartialMatches(values[3], Arrays.stream(Modifiers.ModifierLocation.values()).map(Enum::name).collect(Collectors.toList()), values2);
               Collections.sort(values2);
               return values2;
            }

            return Arrays.stream(Modifiers.ModifierLocation.values()).map(Enum::name).collect(Collectors.toList());
         case 5:
            if (!values[4].isEmpty()) {
               ArrayList values2 = new ArrayList();
               StringUtil.copyPartialMatches(values[4], this.plugin.getShopSections(), values2);
               Collections.sort(values2);
               return values2;
            }

            return this.plugin.getShopSections();
         case 6:
            Modifiers.ModifierLocation modifierLocation;
            try {
               modifierLocation = Modifiers.ModifierLocation.valueOf(values[3].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
               return null;
            }

            if (modifierLocation == Modifiers.ModifierLocation.ITEM && !this.plugin.getShopSections().contains(values[4])) {
               return null;
            } else {
               if (!values[5].isEmpty()) {
                  ArrayList values2 = new ArrayList();
                  StringUtil.copyPartialMatches(
                     values[5],
                     (Iterable)(modifierLocation == Modifiers.ModifierLocation.SECTION
                        ? Arrays.stream(PriceModifierType.values()).map(Enum::name).collect(Collectors.toList())
                        : this.plugin.getSection(values[4]).getShopItemLocs()),
                     values2
                  );
                  Collections.sort(values2);
                  return values2;
               }

               return modifierLocation == Modifiers.ModifierLocation.SECTION
                  ? Arrays.stream(PriceModifierType.values()).map(Enum::name).collect(Collectors.toList())
                  : this.plugin.getSection(values[4]).getShopItemLocs();
            }
         case 7:
            Modifiers.ModifierLocation modifierLocation2;
            try {
               modifierLocation2 = Modifiers.ModifierLocation.valueOf(values[3].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
               return null;
            }

            return modifierLocation2 == Modifiers.ModifierLocation.SECTION
               ? Arrays.asList("+15%", "+33.33%", "-20%", "-72.5%")
               : Arrays.stream(PriceModifierType.values()).map(Enum::name).collect(Collectors.toList());
         case 8:
            Modifiers.ModifierLocation modifierLocation3;
            try {
               modifierLocation3 = Modifiers.ModifierLocation.valueOf(values[3].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
               return null;
            }

            return modifierLocation3 == Modifiers.ModifierLocation.ITEM ? Arrays.asList("+15%", "+33.33%", "-20%", "-72.5%") : null;
         default:
            return null;
      }
   }
}
