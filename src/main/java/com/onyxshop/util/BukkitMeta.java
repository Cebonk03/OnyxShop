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

   public BukkitMeta(OnyxShop plugin) {
      this.plugin = plugin;
      this.logger = plugin.getServer().getConsoleSender();
   }

   @Override
   public void update() {
      config.update();
   }

   @Override
   public void send(Player player, Translatable text) {
      player.sendMessage(config.prefix + text.get());
   }

   @Override
   public void send(Object value, Translatable text) {
      if (value instanceof Player) {
         this.send((Player)value, text);
      } else if (value instanceof CommandSender) {
         ((CommandSender)value).sendMessage((String)text.get());
      }
   }

   @Override
   public void send(Translatable text) {
      this.logger.sendMessage((String)text.get());
   }

   @Override
   public void sendLegacy(Player player, String playerName) {
      player.sendMessage(config.prefix + playerName);
   }

   @Override
   public void sendLegacy(Object value, String text) {
      if (value instanceof Player) {
         this.sendLegacy((Player)value, config.prefix + text);
      } else if (value instanceof CommandSender) {
         ((CommandSender)value).sendMessage(text);
      }
   }

   @Override
   public void sendLegacy(String text) {
      this.logger.sendMessage(text);
   }

   private void send(Player player, TextComponent textComponent) {
      TextComponent textComponent2 = new TextComponent(TextComponent.fromLegacyText((String)Lang.SHOP_PREFIX.get().get()));
      textComponent2.addExtra(textComponent);
      player.spigot().sendMessage(textComponent2);
   }

   @Override
   public void sendTransactionMessage(Player player, int value, double amount, ShopItem shopItem, Transaction.Mode mode, Transaction.Type type, boolean flag) {
      this.sendTransactionMessage(player, value, Collections.singletonMap(shopItem.getEcoType(), amount), shopItem, mode, type, flag);
   }

   @Override
   public void sendTransactionMessage(
      Player player, int value, Map<EcoType, Double> map, ShopItem shopItem, Transaction.Mode mode, Transaction.Type type, boolean flag
   ) {
      String message = (String)(
         mode == Transaction.Mode.BUY
            ? (flag ? Lang.PAY_CONFIRMATION_COMMAND.get(player).get() : Lang.PAY_CONFIRMATION.get(player).get())
            : (flag ? Lang.SELL_CONFIRMATION_COMMAND.get(player).get() : Lang.SELL_CONFIRMATION.get(player).get())
      );
      this.sendLegacy(
         player,
         message.replace("%amount%", String.valueOf(value))
            .replace(
               "%material%",
               shopItem instanceof ShopItem.Shulker
                  ? shopItem.getDisplayname()
                     + ChatColor.GRAY
                     + ((ShopItem.Shulker)shopItem)
                        .getContents()
                        .entrySet()
                        .stream()
                        .map(entry -> entry.getValue() + "x " + entry.getKey().getDisplayname() + ChatColor.GRAY)
                        .collect(Collectors.toList())
                  : shopItem.getDisplayname()
            )
            .replace("%amounttopay%", this.getCurrencyDisplay(message.split("%amounttopay%")[0], map, false))
      );
   }

   @Override
   public void sendTransactionMessage(Player player, int value, Map<EcoType, Double> map, Map<ShopItem, Integer> map2, Transaction.Type type) {
      String itemDisplay = this.getItemDisplay(map2, false);
      String text = (String)Lang.SELL_CONFIRMATION_MULTIPLE_ITEMS.get(player).get();
      String message = text.replace("%items%", (CharSequence)itemDisplay)
         .replace("%amount%", String.valueOf(value))
         .replace("%amounttopay%", this.getCurrencyDisplay(text.split("%amountofmoney%")[0], map, false));
      TextComponent textComponent = new TextComponent(TextComponent.fromLegacyText(message));
      textComponent.setHoverEvent(new HoverEvent(Action.SHOW_TEXT, TextComponent.fromLegacyText(ChatColor.GRAY + itemDisplay)));
      if (this.plugin.spigot) {
         this.send(player, textComponent);
      } else {
         this.sendLegacy(player, message);
      }
   }

   private String getItemDisplay(Map<ShopItem, Integer> map, boolean flag) {
      return this.getItemDisplay(map, flag, false);
   }

   private String getItemDisplay(Map<ShopItem, Integer> map, boolean flag, boolean flag2) {
      StringBuilder builder = new StringBuilder();
      int value = 0;

      for (Entry<ShopItem, Integer> entry : map.entrySet()) {
         if (entry.getKey() instanceof ShopItem.Shulker) {
            Map<ShopItem, Integer> contents = ((ShopItem.Shulker)entry.getKey()).getContents();
            builder.append(entry.getValue()).append("x ").append(((ShopItem)entry.getKey()).getDisplayname());
            builder.append(contents.entrySet().stream().map(entry2 -> entry2.getValue() + "x " + ((ShopItem)entry2.getKey()).getDisplayname()).collect(Collectors.toList()));
         } else {
            builder.append(entry.getValue()).append("x ").append(((ShopItem)entry.getKey()).getDisplayname());
         }

         builder.append("§r");
         if (flag2) {
            builder.append("(").append(((ShopItem)entry.getKey()).getItemPath()).append(")");
         }

         if (!flag) {
            builder.append("§7");
         }

         if (value != map.size() - 1) {
            builder.append(", ");
         }

         value++;
      }

      return builder.toString();
   }

   private String getCurrencyDisplay(String type, Map<EcoType, Double> map, boolean flag) {
      String lastColors = ChatColor.getLastColors(type);
      StringBuilder builder = new StringBuilder();
      int value = 0;

      for (Entry<EcoType, Double> entry : map.entrySet()) {
         builder.append(this.plugin.formatPrice((EcoType)entry.getKey(), (Double)entry.getValue()));
         builder.append("§r");
         if (!flag) {
            builder.append(lastColors);
         }

         if (value != map.size() - 1) {
            builder.append(", ");
         }

         value++;
      }

      return builder.toString();
   }

   @Override
   public Inventory createInventory(InventoryHolder holder, int value, Translatable text) {
      return Bukkit.createInventory(holder, value, (String)text.get());
   }

   @Override
   public void setRawDisplayName(ItemMeta itemMeta, String name) {
      itemMeta.setDisplayName(name);
   }

   @Override
   public void setDisplayName(ItemMeta itemMeta, Translatable text) {
      itemMeta.setDisplayName((String)text.get());
   }

   @Override
   public String getRawDisplayName(ItemMeta itemMeta) {
      return itemMeta.getDisplayName();
   }

   @Override
   public Translatable getDisplayName(ItemMeta itemMeta) {
      return new TranslatableRaw(itemMeta.getDisplayName());
   }

   @Override
   public String getRawItemName(ItemMeta itemMeta) {
      return itemMeta.getItemName();
   }

   @Override
   public void setRawLore(ItemMeta itemMeta, List<String> lores) {
      itemMeta.setLore(lores);
   }

   @Override
   public void setLore(ItemMeta itemMeta, List<Translatable> values) {
      itemMeta.setLore(values.stream().map(element -> (String)element.get()).collect(Collectors.toList()));
   }

   @Override
   public void addLore(ItemMeta itemMeta, Translatable text) {
      List<String> lores = itemMeta.hasLore() ? itemMeta.getLore() : new ArrayList<>();
      lores.add(text.getLegacy());
      itemMeta.setLore((List)lores);
   }

   @Override
   public List<String> getRawLore(ItemMeta itemMeta) {
      return itemMeta.getLore();
   }

   @Override
   public List<Translatable> getLore(ItemMeta itemMeta) {
      return itemMeta.getLore().stream().map(TranslatableRaw::new).collect(Collectors.toList());
   }

   @Override
   public void broadcast(Translatable text) {
      this.plugin.getServer().broadcastMessage(config.prefix + text.get());
   }

   @Override
   public void logDevDebugMessage(Translatable text) {
      this.logger.sendMessage("§8[§6DEBUG§8]§r: " + text.get());
   }

   @Override
   public void logPlayerTransaction(Translatable text) {
      this.logger.sendMessage(config.prefix + text.get());
   }

   @Override
   public void logPlayerTransaction(String playerName) {
      this.logger.sendMessage(config.prefix + playerName);
   }

   @Override
   public void infoMessage(Translatable text) {
      this.logger.sendMessage("§8[§3OnyxShop§8]§r " + "§8[§7INFO§8]§r: " + text.get());
   }

   @Override
   public void logDebugMessage(Translatable text) {
      this.logger.sendMessage("§8[§3OnyxShop§8]§r " + "§8[§6DEBUG§8]§r: " + text.get());
   }

   @Override
   public void warnMessage(Translatable text) {
      this.logger.sendMessage("§8[§3OnyxShop§8]§r " + "§8[§cWARN§8]§r: " + text.get());
   }

   @Override
   public void errorMessage(Translatable text) {
      this.logger.sendMessage("§8[§3OnyxShop§8]§r " + "§8[§4ERROR§8]§r: " + text.get());
   }

   @Override
   public void infoLegacyMessage(String message) {
      this.logger.sendMessage("§8[§3OnyxShop§8]§r " + "§8[§7INFO§8]§r: " + message);
   }

   @Override
   public void logLegacyDebugMessage(String message) {
      this.logger.sendMessage("§8[§3OnyxShop§8]§r " + "§8[§6DEBUG§8]§r: " + message);
   }

   @Override
   public void warnLegacyMessage(String message) {
      this.logger.sendMessage("§8[§3OnyxShop§8]§r " + "§8[§cWARN§8]§r: " + message);
   }

   @Override
   public void errorLegacyMessage(String message) {
      this.logger.sendMessage("§8[§3OnyxShop§8]§r " + "§8[§4ERROR§8]§r: " + message);
   }

   @Override
   public void infoMessage(Object value, Translatable text) {
      if (value instanceof Player) {
         this.sendLegacy((Player)value, ChatColor.GREEN.toString() + text.get());
      } else if (value instanceof CommandSender) {
         ((CommandSender)value).sendMessage("§8[§3OnyxShop§8]§r " + "§8[§7INFO§8]§r: " + text.get());
      }
   }

   @Override
   public void warnMessage(Object value, Translatable text) {
      if (value instanceof Player) {
         this.sendLegacy((Player)value, ChatColor.RED.toString() + text.get());
      } else if (value instanceof CommandSender) {
         ((CommandSender)value).sendMessage("§8[§3OnyxShop§8]§r " + "§8[§cWARN§8]§r: " + text.get());
      }
   }

   @Override
   public void errorMessage(Object value, Translatable text) {
      if (value instanceof Player) {
         this.sendLegacy((Player)value, ChatColor.RED.toString() + text.get());
      } else if (value instanceof CommandSender) {
         ((CommandSender)value).sendMessage("§8[§3OnyxShop§8]§r " + "§8[§4ERROR§8]§r: " + text.get());
      }
   }

   @Override
   public void infoLegacyMessage(Object value, String message) {
      if (value instanceof Player) {
         this.sendLegacy((Player)value, ChatColor.GREEN + message);
      } else if (value instanceof CommandSender) {
         ((CommandSender)value).sendMessage("§8[§3OnyxShop§8]§r " + "§8[§7INFO§8]§r: " + message);
      }
   }

   @Override
   public void infoLegacyMessageWithoutPrefix(Object value, String message) {
      if (value instanceof Player) {
         ((Player)value).sendMessage(ChatColor.GREEN + message);
      } else if (value instanceof CommandSender) {
         ((CommandSender)value).sendMessage((String)message);
      }
   }

   @Override
   public void warnLegacyMessage(Object value, String message) {
      if (value instanceof Player) {
         this.sendLegacy((Player)value, ChatColor.RED + message);
      } else if (value instanceof CommandSender) {
         ((CommandSender)value).sendMessage("§8[§3OnyxShop§8]§r " + "§8[§cWARN§8]§r: " + message);
      }
   }

   @Override
   public void errorLegacyMessage(Object value, String message) {
      if (value instanceof Player) {
         this.sendLegacy((Player)value, ChatColor.RED + message);
      } else if (value instanceof CommandSender) {
         ((CommandSender)value).sendMessage("§8[§3OnyxShop§8]§r " + "§8[§4ERROR§8]§r: " + message);
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
