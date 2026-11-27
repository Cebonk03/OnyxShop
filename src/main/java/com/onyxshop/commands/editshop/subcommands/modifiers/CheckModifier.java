package com.onyxshop.commands.editshop.subcommands.modifiers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.commands.editshop.Methods;
import com.onyxshop.commands.editshop.SubCommad;
import com.onyxshop.commands.editshop.subcommands.Modifiers;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.PlayerPriceModifier;
import com.onyxshop.objects.PriceModifierData;
import com.onyxshop.objects.PriceModifierType;
import com.onyxshop.util.PermissionsCache;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class CheckModifier extends SubCommad {
   private final Methods methods;
   private final OnyxShop plugin;

   public CheckModifier(OnyxShop plugin, Methods methods) {
      this.plugin = plugin;
      this.methods = methods;
   }

   @Override
   public String getName() {
      return "check";
   }

   @Override
   public String getDescription() {
      return ChatColor.GREEN + "Check all price modifiers for a specific player";
   }

   @Override
   public String getSyntax() {
      return ChatColor.GREEN + "/editshop modifiers check <player> [section/item/type] [section] [item]";
   }

   @Override
   public boolean hasPermission(CommandSender sender) {
      return PermissionsCache.hasPermission(sender, "OnyxShop.editshop.modifiers." + this.getName());
   }

   @Override
   public void perform(Object value, String[] values) {
      if (values.length > 2) {
         OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(values[2]);
         Modifiers.ModifierLocation modifierLocation = null;
         PriceModifierType priceModifierType = null;
         if (values.length > 3) {
            try {
               modifierLocation = Modifiers.ModifierLocation.valueOf(values[3].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
               try {
                  priceModifierType = PriceModifierType.valueOf(values[3].toUpperCase(Locale.ROOT));
               } catch (IllegalArgumentException exception2) {
                  SendMessage.warnMessage(value, ChatColor.RED + "Invalid type, should be SECTION, ITEM, BOTH, BUY or SELL");
                  return;
               }
            }
         }

         String text = null;
         if (modifierLocation != null && values.length > 4) {
            text = this.methods.getSection(value, values[4]);
            if (text == null) {
               return;
            }
         }

         String text2 = null;
         if (text != null && modifierLocation == Modifiers.ModifierLocation.ITEM && values.length > 5) {
            text2 = this.methods.getShopItemLoc(value, text, values[5]);
            if (text2 == null) {
               return;
            }
         }

         Modifiers.ModifierLocation modifierLocation2 = modifierLocation;
         PriceModifierType priceModifierType2 = priceModifierType;
         String text3 = text;
         String text4 = text2;
         this.plugin
            .runTaskAsync(
               () -> {
                  PlayerPriceModifier playerPriceModifier = this.plugin.getModifierManager().loadCommandModifiersForPlayer(offlinePlayer.getUniqueId());
                  if (playerPriceModifier == null) {
                     SendMessage.infoMessage(value, ChatColor.RED + "No modifiers found for this player");
                  } else {
                     int value2 = 0;

                     for (String sectionId : playerPriceModifier.getSectionModifiers().keySet()) {
                        value2 += playerPriceModifier.getSectionModifiers().get(sectionId).keySet().size();
                     }

                     for (String itemId : playerPriceModifier.getItemModifiers().keySet()) {
                        value2 += playerPriceModifier.getItemModifiers().get(itemId).keySet().size();
                     }

                     SendMessage.infoMessage(
                        value,
                        Lang.CHECK_PLAYER_PRICE_MODIFIERS
                           .get()
                           .replace("%player_name%", offlinePlayer.getName() == null ? "null" : offlinePlayer.getName())
                           .replace("%player_uuid%", offlinePlayer.getUniqueId().toString())
                           .replace("%amount%", String.valueOf(value2))
                     );
                     this.printModifiers(value, playerPriceModifier, modifierLocation2, text3, text4, priceModifierType2);
                  }
               }
            );
      } else {
         SendMessage.sendMessage(value, this.getSyntax());
      }
   }

   private void printModifiers(Object value, PlayerPriceModifier playerPriceModifier, Modifiers.ModifierLocation modifierLocation, String sectionId, String itemLoc, PriceModifierType priceModifierType) {
      StringBuilder builder = new StringBuilder();
      if ((modifierLocation == null || modifierLocation == Modifiers.ModifierLocation.SECTION) && !playerPriceModifier.getSectionModifiers().isEmpty()) {
         builder.append(ChatColor.GOLD + "=== SECTION modifiers ===\n");

         for (Entry entry : playerPriceModifier.getSectionModifiers().entrySet()) {
            if (sectionId == null || ((String)entry.getKey()).equalsIgnoreCase(sectionId)) {
               builder.append(ChatColor.YELLOW + (String)entry.getKey() + ":\n");
               PriceModifierData priceModifierData = (PriceModifierData)entry.getValue();
               Double amount = priceModifierData.get(PriceModifierType.BOTH);
               Double amount2 = priceModifierData.get(PriceModifierType.BUY);
               Double amount3 = priceModifierData.get(PriceModifierType.SELL);

               for (PriceModifierType priceModifierType2 : priceModifierData.keySet()) {
                  if (priceModifierType == null || priceModifierType2 == priceModifierType) {
                     String text = "";
                     double amount4 = priceModifierData.get(priceModifierType2) - 100.0;
                     if (priceModifierType2 == PriceModifierType.BOTH) {
                        if (amount2 != null && amount != null && amount >= amount2) {
                           text = " (overrides BUY)";
                        } else if (amount3 != null && amount != null && amount >= amount3) {
                           text = " (overrides SELL)";
                        }
                     } else if (priceModifierType2 == PriceModifierType.BUY) {
                        if (amount != null && amount2 >= amount) {
                           text = " (overrides BOTH)";
                        }
                     } else if (priceModifierType2 == PriceModifierType.SELL && amount != null && amount3 >= amount) {
                        text = " (overrides BOTH)";
                     }

                     builder.append(
                        ChatColor.GREEN
                           + "  - "
                           + priceModifierType2.name()
                           + ": "
                           + (amount4 < 0.0 ? ChatColor.RED : ChatColor.GREEN + "+")
                           + amount4
                           + "%"
                           + ChatColor.GRAY
                           + text
                           + "\n"
                     );
                  }
               }

               builder.append("\n");
            }
         }
      }

      if ((modifierLocation == null || modifierLocation == Modifiers.ModifierLocation.ITEM) && !playerPriceModifier.getItemModifiers().isEmpty()) {
         builder.append(ChatColor.GOLD + "=== ITEM modifiers ===\n");

         for (Entry entry : playerPriceModifier.getItemModifiers().entrySet()) {
            if ((sectionId == null || ((String)entry.getKey()).startsWith(sectionId)) && (itemLoc == null || ((String)entry.getKey()).equalsIgnoreCase(itemLoc))) {
               builder.append(ChatColor.YELLOW + (String)entry.getKey() + ":\n");
               PriceModifierData priceModifierData = (PriceModifierData)entry.getValue();
               Double amount = priceModifierData.get(PriceModifierType.BOTH);
               Double amount2 = priceModifierData.get(PriceModifierType.BUY);
               Double amount3 = priceModifierData.get(PriceModifierType.SELL);

               for (PriceModifierType priceModifierType2 : priceModifierData.keySet()) {
                  if (priceModifierType == null || priceModifierType2 == priceModifierType) {
                     String text = "";
                     double amount4 = priceModifierData.get(priceModifierType2) - 100.0;
                     boolean flag = false;
                     if (playerPriceModifier.getSectionModifiers().containsKey(((String)entry.getKey()).split("\\.", 2)[0])) {
                        PriceModifierData priceModifierData2 = playerPriceModifier.getSectionModifiers().get(((String)entry.getKey()).split("\\.", 2)[0]);
                        if (priceModifierData2 != null && (priceModifierType2 == PriceModifierType.BOTH || priceModifierData2.keySet().contains(priceModifierType2))) {
                           flag = true;
                        }
                     }

                     if (flag) {
                        text = " (overrides SECTION modifier)";
                     } else if (priceModifierType2 == PriceModifierType.BOTH) {
                        if (amount2 != null && amount != null && amount >= amount2) {
                           text = " (overrides BUY)";
                        } else if (amount3 != null && amount != null && amount >= amount3) {
                           text = " (overrides SELL)";
                        }
                     } else if (priceModifierType2 == PriceModifierType.BUY) {
                        if (amount != null && amount2 >= amount) {
                           text = " (overrides BOTH)";
                        }
                     } else if (priceModifierType2 == PriceModifierType.SELL && amount != null && amount3 >= amount) {
                        text = " (overrides BOTH)";
                     }

                     builder.append(
                        ChatColor.GREEN
                           + "  - "
                           + priceModifierType2.name()
                           + ": "
                           + (amount4 < 0.0 ? ChatColor.RED : ChatColor.GREEN + "+")
                           + amount4
                           + "%"
                           + ChatColor.GRAY
                           + text
                           + "\n"
                     );
                  }
               }

               builder.append("\n");
            }
         }
      }

      SendMessage.infoMessageWithoutPrefix(value, builder.toString());
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
            List prices = Arrays.stream(PriceModifierType.values()).map(Enum::name).collect(Collectors.toList());
            prices.addAll(Arrays.stream(Modifiers.ModifierLocation.values()).map(Enum::name).collect(Collectors.toList()));
            if (!values[3].isEmpty()) {
               ArrayList values2 = new ArrayList();
               StringUtil.copyPartialMatches(values[3], prices, values2);
               Collections.sort(values2);
               return values2;
            }

            return prices;
         case 5:
            try {
               Modifiers.ModifierLocation.valueOf(values[3].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
               return null;
            }

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
         default:
            return null;
      }
   }
}
