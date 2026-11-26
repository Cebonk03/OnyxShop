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

   public CheckModifier(OnyxShop var1, Methods var2) {
      this.plugin = var1;
      this.methods = var2;
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
   public boolean hasPermission(CommandSender var1) {
      return PermissionsCache.hasPermission(var1, "OnyxShop.editshop.modifiers." + this.getName());
   }

   @Override
   public void perform(Object var1, String[] var2) {
      if (var2.length > 2) {
         OfflinePlayer var3 = Bukkit.getOfflinePlayer(var2[2]);
         Modifiers.ModifierLocation var4 = null;
         PriceModifierType var5 = null;
         if (var2.length > 3) {
            try {
               var4 = Modifiers.ModifierLocation.valueOf(var2[3].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException var13) {
               try {
                  var5 = PriceModifierType.valueOf(var2[3].toUpperCase(Locale.ROOT));
               } catch (IllegalArgumentException var12) {
                  SendMessage.warnMessage(var1, ChatColor.RED + "Invalid type, should be SECTION, ITEM, BOTH, BUY or SELL");
                  return;
               }
            }
         }

         String var6 = null;
         if (var4 != null && var2.length > 4) {
            var6 = this.methods.getSection(var1, var2[4]);
            if (var6 == null) {
               return;
            }
         }

         String var7 = null;
         if (var6 != null && var4 == Modifiers.ModifierLocation.ITEM && var2.length > 5) {
            var7 = this.methods.getShopItemLoc(var1, var6, var2[5]);
            if (var7 == null) {
               return;
            }
         }

         Modifiers.ModifierLocation var8 = var4;
         PriceModifierType var9 = var5;
         String var10 = var6;
         String var11 = var7;
         this.plugin
            .runTaskAsync(
               () -> {
                  PlayerPriceModifier var7x = this.plugin.getModifierManager().loadCommandModifiersForPlayer(var3.getUniqueId());
                  if (var7x == null) {
                     SendMessage.infoMessage(var1, ChatColor.RED + "No modifiers found for this player");
                  } else {
                     int var8x = 0;

                     for (String var10x : var7x.getSectionModifiers().keySet()) {
                        var8x += var7x.getSectionModifiers().get(var10x).keySet().size();
                     }

                     for (String var12x : var7x.getItemModifiers().keySet()) {
                        var8x += var7x.getItemModifiers().get(var12x).keySet().size();
                     }

                     SendMessage.infoMessage(
                        var1,
                        Lang.CHECK_PLAYER_PRICE_MODIFIERS
                           .get()
                           .replace("%player_name%", var3.getName() == null ? "null" : var3.getName())
                           .replace("%player_uuid%", var3.getUniqueId().toString())
                           .replace("%amount%", String.valueOf(var8x))
                     );
                     this.printModifiers(var1, var7x, var8, var10, var11, var9);
                  }
               }
            );
      } else {
         SendMessage.sendMessage(var1, this.getSyntax());
      }
   }

   private void printModifiers(Object var1, PlayerPriceModifier var2, Modifiers.ModifierLocation var3, String var4, String var5, PriceModifierType var6) {
      StringBuilder var7 = new StringBuilder();
      if ((var3 == null || var3 == Modifiers.ModifierLocation.SECTION) && !var2.getSectionModifiers().isEmpty()) {
         var7.append(ChatColor.GOLD + "=== SECTION modifiers ===\n");

         for (Entry var9 : var2.getSectionModifiers().entrySet()) {
            if (var4 == null || ((String)var9.getKey()).equalsIgnoreCase(var4)) {
               var7.append(ChatColor.YELLOW + (String)var9.getKey() + ":\n");
               PriceModifierData var10 = (PriceModifierData)var9.getValue();
               Double var11 = var10.get(PriceModifierType.BOTH);
               Double var12 = var10.get(PriceModifierType.BUY);
               Double var13 = var10.get(PriceModifierType.SELL);

               for (PriceModifierType var15 : var10.keySet()) {
                  if (var6 == null || var15 == var6) {
                     String var16 = "";
                     double var17 = var10.get(var15) - 100.0;
                     if (var15 == PriceModifierType.BOTH) {
                        if (var12 != null && var11 != null && var11 >= var12) {
                           var16 = " (overrides BUY)";
                        } else if (var13 != null && var11 != null && var11 >= var13) {
                           var16 = " (overrides SELL)";
                        }
                     } else if (var15 == PriceModifierType.BUY) {
                        if (var11 != null && var12 >= var11) {
                           var16 = " (overrides BOTH)";
                        }
                     } else if (var15 == PriceModifierType.SELL && var11 != null && var13 >= var11) {
                        var16 = " (overrides BOTH)";
                     }

                     var7.append(
                        ChatColor.GREEN
                           + "  - "
                           + var15.name()
                           + ": "
                           + (var17 < 0.0 ? ChatColor.RED : ChatColor.GREEN + "+")
                           + var17
                           + "%"
                           + ChatColor.GRAY
                           + var16
                           + "\n"
                     );
                  }
               }

               var7.append("\n");
            }
         }
      }

      if ((var3 == null || var3 == Modifiers.ModifierLocation.ITEM) && !var2.getItemModifiers().isEmpty()) {
         var7.append(ChatColor.GOLD + "=== ITEM modifiers ===\n");

         for (Entry var22 : var2.getItemModifiers().entrySet()) {
            if ((var4 == null || ((String)var22.getKey()).startsWith(var4)) && (var5 == null || ((String)var22.getKey()).equalsIgnoreCase(var5))) {
               var7.append(ChatColor.YELLOW + (String)var22.getKey() + ":\n");
               PriceModifierData var23 = (PriceModifierData)var22.getValue();
               Double var24 = var23.get(PriceModifierType.BOTH);
               Double var25 = var23.get(PriceModifierType.BUY);
               Double var26 = var23.get(PriceModifierType.SELL);

               for (PriceModifierType var28 : var23.keySet()) {
                  if (var6 == null || var28 == var6) {
                     String var29 = "";
                     double var30 = var23.get(var28) - 100.0;
                     boolean var19 = false;
                     if (var2.getSectionModifiers().containsKey(((String)var22.getKey()).split("\\.", 2)[0])) {
                        PriceModifierData var20 = var2.getSectionModifiers().get(((String)var22.getKey()).split("\\.", 2)[0]);
                        if (var20 != null && (var28 == PriceModifierType.BOTH || var20.keySet().contains(var28))) {
                           var19 = true;
                        }
                     }

                     if (var19) {
                        var29 = " (overrides SECTION modifier)";
                     } else if (var28 == PriceModifierType.BOTH) {
                        if (var25 != null && var24 != null && var24 >= var25) {
                           var29 = " (overrides BUY)";
                        } else if (var26 != null && var24 != null && var24 >= var26) {
                           var29 = " (overrides SELL)";
                        }
                     } else if (var28 == PriceModifierType.BUY) {
                        if (var24 != null && var25 >= var24) {
                           var29 = " (overrides BOTH)";
                        }
                     } else if (var28 == PriceModifierType.SELL && var24 != null && var26 >= var24) {
                        var29 = " (overrides BOTH)";
                     }

                     var7.append(
                        ChatColor.GREEN
                           + "  - "
                           + var28.name()
                           + ": "
                           + (var30 < 0.0 ? ChatColor.RED : ChatColor.GREEN + "+")
                           + var30
                           + "%"
                           + ChatColor.GRAY
                           + var29
                           + "\n"
                     );
                  }
               }

               var7.append("\n");
            }
         }
      }

      SendMessage.infoMessageWithoutPrefix(var1, var7.toString());
   }

   @Override
   public List<String> getTabCompletion(String[] var1) {
      switch (var1.length) {
         case 3:
            if (!var1[2].isEmpty()) {
               List var7 = this.plugin.getServer().getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
               ArrayList var10 = new ArrayList();
               StringUtil.copyPartialMatches(var1[2], var7, var10);
               Collections.sort(var10);
               return var10;
            }

            return this.plugin.getServer().getOnlinePlayers().stream().<String>map(Player::getName).collect(Collectors.toList());
         case 4:
            List var2 = Arrays.stream(PriceModifierType.values()).map(Enum::name).collect(Collectors.toList());
            var2.addAll(Arrays.stream(Modifiers.ModifierLocation.values()).map(Enum::name).collect(Collectors.toList()));
            if (!var1[3].isEmpty()) {
               ArrayList var9 = new ArrayList();
               StringUtil.copyPartialMatches(var1[3], var2, var9);
               Collections.sort(var9);
               return var9;
            }

            return var2;
         case 5:
            try {
               Modifiers.ModifierLocation.valueOf(var1[3].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException var6) {
               return null;
            }

            if (!var1[4].isEmpty()) {
               ArrayList var8 = new ArrayList();
               StringUtil.copyPartialMatches(var1[4], this.plugin.getShopSections(), var8);
               Collections.sort(var8);
               return var8;
            }

            return this.plugin.getShopSections();
         case 6:
            Modifiers.ModifierLocation var3;
            try {
               var3 = Modifiers.ModifierLocation.valueOf(var1[3].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException var5) {
               return null;
            }

            if (var3 == Modifiers.ModifierLocation.ITEM && !this.plugin.getShopSections().contains(var1[4])) {
               return null;
            } else {
               if (!var1[5].isEmpty()) {
                  ArrayList var4 = new ArrayList();
                  StringUtil.copyPartialMatches(
                     var1[5],
                     (Iterable)(var3 == Modifiers.ModifierLocation.SECTION
                        ? Arrays.stream(PriceModifierType.values()).map(Enum::name).collect(Collectors.toList())
                        : this.plugin.getSection(var1[4]).getShopItemLocs()),
                     var4
                  );
                  Collections.sort(var4);
                  return var4;
               }

               return var3 == Modifiers.ModifierLocation.SECTION
                  ? Arrays.stream(PriceModifierType.values()).map(Enum::name).collect(Collectors.toList())
                  : this.plugin.getSection(var1[4]).getShopItemLocs();
            }
         default:
            return null;
      }
   }
}
