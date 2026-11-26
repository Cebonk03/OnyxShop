package com.onyxshop.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.files.Translatable;
import com.onyxshop.files.lang.TranslatableRaw;
import com.onyxshop.objects.ShopItem;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.HoverEvent.Action;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.meta.ItemMeta;

public class BukkitMeta implements MetaUtils {
   private static final BukkitMeta.Config config = new BukkitMeta.Config();
   private final ConsoleCommandSender logger;
   private final OnyxShop plugin;

   public BukkitMeta(OnyxShop var1) {
      this.plugin = var1;
      this.logger = var1.getServer().getConsoleSender();
   }

   @Override
   public void update() {
      config.update();
   }

   @Override
   public void send(Player var1, Translatable var2) {
      var1.sendMessage(config.prefix + var2.get());
   }

   @Override
   public void send(Object var1, Translatable var2) {
      if (var1 instanceof Player) {
         this.send((Player)var1, var2);
      } else if (var1 instanceof CommandSender) {
         ((CommandSender)var1).sendMessage((String)var2.get());
      }
   }

   @Override
   public void send(Translatable var1) {
      this.logger.sendMessage((String)var1.get());
   }

   @Override
   public void sendLegacy(Player var1, String var2) {
      var1.sendMessage(config.prefix + var2);
   }

   @Override
   public void sendLegacy(Object var1, String var2) {
      if (var1 instanceof Player) {
         this.sendLegacy((Player)var1, config.prefix + var2);
      } else if (var1 instanceof CommandSender) {
         ((CommandSender)var1).sendMessage(var2);
      }
   }

   @Override
   public void sendLegacy(String var1) {
      this.logger.sendMessage(var1);
   }

   private void send(Player var1, TextComponent var2) {
      TextComponent var3 = new TextComponent(TextComponent.fromLegacyText((String)Lang.SHOP_PREFIX.get().get()));
      var3.addExtra(var2);
      var1.spigot().sendMessage(var3);
   }

   @Override
   public void sendTransactionMessage(Player var1, int var2, double var3, ShopItem var5, Transaction.Mode var6, Transaction.Type var7, boolean var8) {
      this.sendTransactionMessage(var1, var2, Collections.singletonMap(var5.getEcoType(), var3), var5, var6, var7, var8);
   }

   @Override
   public void sendTransactionMessage(
      Player var1, int var2, Map<EcoType, Double> var3, ShopItem var4, Transaction.Mode var5, Transaction.Type var6, boolean var7
   ) {
      String var8 = (String)(
         var5 == Transaction.Mode.BUY
            ? (var7 ? Lang.PAY_CONFIRMATION_COMMAND.get(var1).get() : Lang.PAY_CONFIRMATION.get(var1).get())
            : (var7 ? Lang.SELL_CONFIRMATION_COMMAND.get(var1).get() : Lang.SELL_CONFIRMATION.get(var1).get())
      );
      this.sendLegacy(
         var1,
         var8.replace("%amount%", String.valueOf(var2))
            .replace(
               "%material%",
               var4 instanceof ShopItem.Shulker
                  ? var4.getDisplayname()
                     + ChatColor.GRAY
                     + ((ShopItem.Shulker)var4)
                        .getContents()
                        .entrySet()
                        .stream()
                        .map(var0 -> var0.getValue() + "x " + var0.getKey().getDisplayname() + ChatColor.GRAY)
                        .collect(Collectors.toList())
                  : var4.getDisplayname()
            )
            .replace("%amounttopay%", this.getCurrencyDisplay(var8.split("%amounttopay%")[0], var3, false))
      );
   }

   @Override
   public void sendTransactionMessage(Player var1, int var2, Map<EcoType, Double> var3, Map<ShopItem, Integer> var4, Transaction.Type var5) {
      String var6 = this.getItemDisplay(var4, false);
      String var7 = (String)Lang.SELL_CONFIRMATION_MULTIPLE_ITEMS.get(var1).get();
      String var8 = var7.replace("%items%", (CharSequence)var6)
         .replace("%amount%", String.valueOf(var2))
         .replace("%amounttopay%", this.getCurrencyDisplay(var7.split("%amountofmoney%")[0], var3, false));
      TextComponent var9 = new TextComponent(TextComponent.fromLegacyText(var8));
      var9.setHoverEvent(new HoverEvent(Action.SHOW_TEXT, TextComponent.fromLegacyText(ChatColor.GRAY + var6)));
      if (this.plugin.spigot) {
         this.send(var1, var9);
      } else {
         this.sendLegacy(var1, var8);
      }
   }

   private String getItemDisplay(Map<ShopItem, Integer> var1, boolean var2) {
      return this.getItemDisplay(var1, var2, false);
   }

   private String getItemDisplay(Map<ShopItem, Integer> var1, boolean var2, boolean var3) {
      StringBuilder var4 = new StringBuilder();
      int var5 = 0;

      for (Entry<ShopItem, Integer> var7 : var1.entrySet()) {
         if (var7.getKey() instanceof ShopItem.Shulker) {
            Map<ShopItem, Integer> var8 = ((ShopItem.Shulker)var7.getKey()).getContents();
            var4.append(var7.getValue()).append("x ").append(((ShopItem)var7.getKey()).getDisplayname());
            var4.append(var8.entrySet().stream().map(var0 -> var0.getValue() + "x " + ((ShopItem)var0.getKey()).getDisplayname()).collect(Collectors.toList()));
         } else {
            var4.append(var7.getValue()).append("x ").append(((ShopItem)var7.getKey()).getDisplayname());
         }

         var4.append("§r");
         if (var3) {
            var4.append("(").append(((ShopItem)var7.getKey()).getItemPath()).append(")");
         }

         if (!var2) {
            var4.append("§7");
         }

         if (var5 != var1.size() - 1) {
            var4.append(", ");
         }

         var5++;
      }

      return var4.toString();
   }

   private String getCurrencyDisplay(String var1, Map<EcoType, Double> var2, boolean var3) {
      String var4 = ChatColor.getLastColors(var1);
      StringBuilder var5 = new StringBuilder();
      int var6 = 0;

      for (Entry<EcoType, Double> var8 : var2.entrySet()) {
         var5.append(this.plugin.formatPrice((EcoType)var8.getKey(), (Double)var8.getValue()));
         var5.append("§r");
         if (!var3) {
            var5.append(var4);
         }

         if (var6 != var2.size() - 1) {
            var5.append(", ");
         }

         var6++;
      }

      return var5.toString();
   }

   @Override
   public Inventory createInventory(InventoryHolder var1, int var2, Translatable var3) {
      return Bukkit.createInventory(var1, var2, (String)var3.get());
   }

   @Override
   public void setRawDisplayName(ItemMeta var1, String var2) {
      var1.setDisplayName(var2);
   }

   @Override
   public void setDisplayName(ItemMeta var1, Translatable var2) {
      var1.setDisplayName((String)var2.get());
   }

   @Override
   public String getRawDisplayName(ItemMeta var1) {
      return var1.getDisplayName();
   }

   @Override
   public Translatable getDisplayName(ItemMeta var1) {
      return new TranslatableRaw(var1.getDisplayName());
   }

   @Override
   public String getRawItemName(ItemMeta var1) {
      return var1.getItemName();
   }

   @Override
   public void setRawLore(ItemMeta var1, List<String> var2) {
      var1.setLore(var2);
   }

   @Override
   public void setLore(ItemMeta var1, List<Translatable> var2) {
      var1.setLore(var2.stream().map(var0 -> (String)var0.get()).collect(Collectors.toList()));
   }

   @Override
   public void addLore(ItemMeta var1, Translatable var2) {
      List<String> var3 = var1.hasLore() ? var1.getLore() : new ArrayList<>();
      var3.add(var2.getLegacy());
      var1.setLore((List)var3);
   }

   @Override
   public List<String> getRawLore(ItemMeta var1) {
      return var1.getLore();
   }

   @Override
   public List<Translatable> getLore(ItemMeta var1) {
      return var1.getLore().stream().map(TranslatableRaw::new).collect(Collectors.toList());
   }

   @Override
   public void broadcast(Translatable var1) {
      this.plugin.getServer().broadcastMessage(config.prefix + var1.get());
   }

   @Override
   public void logDevDebugMessage(Translatable var1) {
      this.logger.sendMessage("§8[§6DEBUG§8]§r: " + var1.get());
   }

   @Override
   public void logPlayerTransaction(Translatable var1) {
      this.logger.sendMessage(config.prefix + var1.get());
   }

   @Override
   public void logPlayerTransaction(String var1) {
      this.logger.sendMessage(config.prefix + var1);
   }

   @Override
   public void infoMessage(Translatable var1) {
      this.logger.sendMessage("§8[§3OnyxShop§8]§r " + "§8[§7INFO§8]§r: " + var1.get());
   }

   @Override
   public void logDebugMessage(Translatable var1) {
      this.logger.sendMessage("§8[§3OnyxShop§8]§r " + "§8[§6DEBUG§8]§r: " + var1.get());
   }

   @Override
   public void warnMessage(Translatable var1) {
      this.logger.sendMessage("§8[§3OnyxShop§8]§r " + "§8[§cWARN§8]§r: " + var1.get());
   }

   @Override
   public void errorMessage(Translatable var1) {
      this.logger.sendMessage("§8[§3OnyxShop§8]§r " + "§8[§4ERROR§8]§r: " + var1.get());
   }

   @Override
   public void infoLegacyMessage(String var1) {
      this.logger.sendMessage("§8[§3OnyxShop§8]§r " + "§8[§7INFO§8]§r: " + var1);
   }

   @Override
   public void logLegacyDebugMessage(String var1) {
      this.logger.sendMessage("§8[§3OnyxShop§8]§r " + "§8[§6DEBUG§8]§r: " + var1);
   }

   @Override
   public void warnLegacyMessage(String var1) {
      this.logger.sendMessage("§8[§3OnyxShop§8]§r " + "§8[§cWARN§8]§r: " + var1);
   }

   @Override
   public void errorLegacyMessage(String var1) {
      this.logger.sendMessage("§8[§3OnyxShop§8]§r " + "§8[§4ERROR§8]§r: " + var1);
   }

   @Override
   public void infoMessage(Object var1, Translatable var2) {
      if (var1 instanceof Player) {
         this.sendLegacy((Player)var1, ChatColor.GREEN.toString() + var2.get());
      } else if (var1 instanceof CommandSender) {
         ((CommandSender)var1).sendMessage("§8[§3OnyxShop§8]§r " + "§8[§7INFO§8]§r: " + var2.get());
      }
   }

   @Override
   public void warnMessage(Object var1, Translatable var2) {
      if (var1 instanceof Player) {
         this.sendLegacy((Player)var1, ChatColor.RED.toString() + var2.get());
      } else if (var1 instanceof CommandSender) {
         ((CommandSender)var1).sendMessage("§8[§3OnyxShop§8]§r " + "§8[§cWARN§8]§r: " + var2.get());
      }
   }

   @Override
   public void errorMessage(Object var1, Translatable var2) {
      if (var1 instanceof Player) {
         this.sendLegacy((Player)var1, ChatColor.RED.toString() + var2.get());
      } else if (var1 instanceof CommandSender) {
         ((CommandSender)var1).sendMessage("§8[§3OnyxShop§8]§r " + "§8[§4ERROR§8]§r: " + var2.get());
      }
   }

   @Override
   public void infoLegacyMessage(Object var1, String var2) {
      if (var1 instanceof Player) {
         this.sendLegacy((Player)var1, ChatColor.GREEN + var2);
      } else if (var1 instanceof CommandSender) {
         ((CommandSender)var1).sendMessage("§8[§3OnyxShop§8]§r " + "§8[§7INFO§8]§r: " + var2);
      }
   }

   @Override
   public void infoLegacyMessageWithoutPrefix(Object var1, String var2) {
      if (var1 instanceof Player) {
         ((Player)var1).sendMessage(ChatColor.GREEN + var2);
      } else if (var1 instanceof CommandSender) {
         ((CommandSender)var1).sendMessage((String)var2);
      }
   }

   @Override
   public void warnLegacyMessage(Object var1, String var2) {
      if (var1 instanceof Player) {
         this.sendLegacy((Player)var1, ChatColor.RED + var2);
      } else if (var1 instanceof CommandSender) {
         ((CommandSender)var1).sendMessage("§8[§3OnyxShop§8]§r " + "§8[§cWARN§8]§r: " + var2);
      }
   }

   @Override
   public void errorLegacyMessage(Object var1, String var2) {
      if (var1 instanceof Player) {
         this.sendLegacy((Player)var1, ChatColor.RED + var2);
      } else if (var1 instanceof CommandSender) {
         ((CommandSender)var1).sendMessage("§8[§3OnyxShop§8]§r " + "§8[§4ERROR§8]§r: " + var2);
      }
   }

   private static final class Config {
      public final String info = "§8[§7INFO§8]§r: ";
      public final String debug = "§8[§6DEBUG§8]§r: ";
      public final String warn = "§8[§cWARN§8]§r: ";
      public final String error = "§8[§4ERROR§8]§r: ";
      public final String consolePrefix = "§8[§3OnyxShop§8]§r ";
      public String prefix;

      private Config() {
      }

      public void update() {
         this.prefix = Lang.SHOP_PREFIX.get().get() + "§r";
      }
   }
}
