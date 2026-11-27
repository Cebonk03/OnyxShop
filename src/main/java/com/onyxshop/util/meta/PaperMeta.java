package com.onyxshop.util.meta;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.files.Translatable;
import com.onyxshop.files.lang.TranslatableComponent;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.util.AdventureUtil;
import com.onyxshop.util.ChatUtil;
import com.onyxshop.util.EcoType;
import com.onyxshop.util.MetaUtils;
import com.onyxshop.util.ServerInfo;
import com.onyxshop.util.Transaction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class PaperMeta implements MetaUtils {
   public static final Map<Character, TextColor> COLOR_MAP = new HashMap<>();
   private static final Component RESET = ((TextComponent)((TextComponent)Component.text("").color(null)).decoration(TextDecoration.BOLD, false))
      .decoration(TextDecoration.ITALIC, false);
   private static final PaperMeta.Config config = new PaperMeta.Config();
   private final ConsoleCommandSender logger;
   private final OnyxShop plugin;
   private PaperMeta.Utils utils;

   public PaperMeta(OnyxShop plugin) {
      this.logger = plugin.getServer().getConsoleSender();
      this.plugin = plugin;
   }

   @Override
   public void update() {
      if (ConfigManager.getConfig().getBoolean("use-paper-meta", true) && ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_21_R2)) {
         this.utils = new PaperMeta.PaperUtils();
         this.plugin.paperMeta = true;
      } else {
         this.utils = new PaperMeta.BukkitUtils();
         this.plugin.paperMeta = false;
      }

      config.update();
   }

   private void send(Player player, Component component) {
      player.sendMessage(config.prefix.append(component));
   }

   @Override
   public void send(Player player, Translatable text) {
      player.sendMessage(config.prefix.append((Component)text.get()));
   }

   @Override
   public void send(Object value, Translatable text) {
      if (value instanceof Player) {
         ((Player)value).sendMessage(config.prefix.append((Component)text.get()));
      } else {
         ((CommandSender)value).sendMessage((Component)text.get());
      }
   }

   @Override
   public void send(Translatable text) {
      this.logger.sendMessage((Component)text.get());
   }

   @Override
   public void sendLegacy(Player player, String playerName) {
      player.sendMessage(AdventureUtil.toLegacy(config.prefix) + playerName);
   }

   @Override
   public void sendLegacy(Object value, String text) {
      if (value instanceof Player) {
         ((Player)value).sendMessage(AdventureUtil.toLegacy(config.prefix) + text);
      } else {
         ((CommandSender)value).sendMessage(text);
      }
   }

   @Override
   public void sendLegacy(String text) {
      this.logger.sendMessage(text);
   }

   @Override
   public void sendTransactionMessage(Player player, int value, double amount, ShopItem shopItem, Transaction.Mode mode, Transaction.Type type, boolean flag) {
      this.sendTransactionMessage(player, value, Collections.singletonMap(shopItem.getEcoType(), amount), shopItem, mode, type, flag);
   }

   @Override
   public void sendTransactionMessage(
      Player player, int value, Map<EcoType, Double> map, ShopItem shopItem, Transaction.Mode mode, Transaction.Type type, boolean flag
   ) {
      Translatable text = mode == Transaction.Mode.BUY
         ? (flag ? Lang.PAY_CONFIRMATION_COMMAND.get(player) : Lang.PAY_CONFIRMATION.get(player))
         : (flag ? Lang.SELL_CONFIRMATION_COMMAND.get(player) : Lang.SELL_CONFIRMATION.get(player));
      String lastColors = ChatUtil.getAdventureUtils().getLastColors(text.toString().split("%material%")[0]);
      this.send(
         player,
         (Component)text.replace("%amounttopay%", this.getCurrencyDisplay(lastColors, map))
            .replace("%amount%", String.valueOf(value))
            .replace(
               "%material%",
               (
                     shopItem instanceof ShopItem.Shulker
                        ? this.translate(shopItem)
                           + ChatColor.GRAY
                           + ((ShopItem.Shulker)shopItem)
                              .getContents()
                              .entrySet()
                              .stream()
                              .map(entry -> entry.getValue() + "x " + this.translate(entry.getKey()) + ChatColor.GRAY)
                              .collect(Collectors.toList())
                        : this.translate(shopItem)
                  )
                  + lastColors
            )
            .get()
      );
   }

   private String translate(ShopItem shopItem) {
      return shopItem.hasCustomName() ? shopItem.getDisplayname() : "<lang:" + shopItem.getTranslationKey() + ">";
   }

   @Override
   public void sendTransactionMessage(Player player, int value, Map<EcoType, Double> map, Map<ShopItem, Integer> map2, Transaction.Type type) {
      Translatable text = Lang.SELL_CONFIRMATION_MULTIPLE_ITEMS.get(player);
      String lastColors = ChatUtil.getAdventureUtils().getLastColors(text.toString().split("%amounttopay%")[0]);
      HoverEvent hoverEvent = HoverEvent.showText(ChatUtil.getAdventureUtils().formatLegacyAndMini("§7" + this.getItemDisplay("§7", map2))).asHoverEvent();
      this.send(
         player,
         ((Component)text.replace("%items%", this.getItemDisplay(lastColors, map2))
               .replace("%amount%", String.valueOf(value))
               .replace("%amounttopay%", this.getCurrencyDisplay(lastColors, map))
               .get())
            .hoverEvent(hoverEvent)
      );
   }

   private String getItemDisplay(String itemId, Map<ShopItem, Integer> map) {
      StringBuilder builder = new StringBuilder();
      int value = 0;

      for (Entry<ShopItem, Integer> entry : map.entrySet()) {
         if (entry.getKey() instanceof ShopItem.Shulker) {
            Map<ShopItem, Integer> contents = ((ShopItem.Shulker)entry.getKey()).getContents();
            builder.append(entry.getValue()).append("x ").append(this.translate((ShopItem)entry.getKey()));
            builder.append(contents.entrySet().stream().map(entry2 -> entry2.getValue() + "x " + this.translate((ShopItem)entry2.getKey())).collect(Collectors.toList()));
         } else {
            builder.append(entry.getValue()).append("x ").append(this.translate((ShopItem)entry.getKey()));
         }

         builder.append(itemId);
         if (value != map.size() - 1) {
            builder.append(", ");
         }

         value++;
      }

      return builder.toString();
   }

   private String getCurrencyDisplay(String type, Map<EcoType, Double> map) {
      StringBuilder builder = new StringBuilder();
      int value = 0;

      for (Entry<EcoType, Double> entry : map.entrySet()) {
         builder.append(this.plugin.formatPrice(entry.getKey(), entry.getValue()));
         builder.append(type);
         if (value != map.size() - 1) {
            builder.append(", ");
         }

         value++;
      }

      return builder.toString();
   }

   @Override
   public Inventory createInventory(InventoryHolder holder, int value, Translatable text) {
      return this.utils.createInventory(holder, value, text);
   }

   @Override
   public void setRawDisplayName(ItemMeta itemMeta, String name) {
      this.utils.setRawDisplayName(itemMeta, name);
   }

   @Override
   public void setDisplayName(ItemMeta itemMeta, Translatable text) {
      this.utils.setDisplayName(itemMeta, text);
   }

   @Override
   public String getRawDisplayName(ItemMeta itemMeta) {
      return this.utils.getRawDisplayName(itemMeta);
   }

   @Override
   public String getRawItemName(ItemMeta itemMeta) {
      return this.utils.getRawItemName(itemMeta);
   }

   @Override
   public Translatable getDisplayName(ItemMeta itemMeta) {
      return this.utils.getDisplayName(itemMeta);
   }

   public static ItemStack setDisplayname(ItemStack itemStack, String itemId) {
      itemStack.setData(DataComponentTypes.ITEM_NAME, AdventureUtil.fromGson(itemId).decoration(TextDecoration.ITALIC, false));
      return itemStack;
   }

   @Override
   public void setRawLore(ItemMeta itemMeta, List<String> lores) {
      this.utils.setRawLore(itemMeta, lores);
   }

   @Override
   public void setLore(ItemMeta itemMeta, List<Translatable> values) {
      this.utils.setLore(itemMeta, values);
   }

   @Override
   public void addLore(ItemMeta itemMeta, Translatable text) {
      this.utils.addLore(itemMeta, text);
   }

   @Override
   public List<String> getRawLore(ItemMeta itemMeta) {
      return this.utils.getRawLore(itemMeta);
   }

   @Override
   public List<Translatable> getLore(ItemMeta itemMeta) {
      return this.utils.getLore(itemMeta);
   }

   public static ItemStack setLore(ItemStack itemStack, ArrayList<String> lores) {
      itemStack.setData(
         DataComponentTypes.LORE,
         ItemLore.lore(lores.stream().map(element -> AdventureUtil.fromGson(element).decoration(TextDecoration.ITALIC, false)).collect(Collectors.toList()))
      );
      return itemStack;
   }

   public static ItemStack setRawLore(ItemStack itemStack, ArrayList<String> lores) {
      itemStack.setData(
         DataComponentTypes.LORE,
         ItemLore.lore(
            lores.stream()
               .map(element -> ChatUtil.getAdventureUtils().formatLegacyAndMini(element).decoration(TextDecoration.ITALIC, false))
               .collect(Collectors.toList())
         )
      );
      return itemStack;
   }

   @Override
   public void broadcast(Translatable text) {
      this.plugin.getServer().broadcast(config.prefix.append((Component)text.get()));
   }

   @Override
   public void logDevDebugMessage(Translatable text) {
      this.logger.sendMessage(config.consolePrefix.append(config.debug).append((Component)text.get()));
   }

   @Override
   public void logPlayerTransaction(Translatable text) {
      this.logger.sendMessage(config.prefix.append((Component)text.get()));
   }

   @Override
   public void logPlayerTransaction(String playerName) {
      this.logger.sendMessage(config.prefix.append(Component.text(playerName)));
   }

   @Override
   public void infoMessage(Translatable text) {
      this.logger.sendMessage(config.consolePrefix.append(config.info).append((Component)text.get()));
   }

   @Override
   public void logDebugMessage(Translatable text) {
      this.logger.sendMessage(config.consolePrefix.append(config.debug).append((Component)text.get()));
   }

   @Override
   public void warnMessage(Translatable text) {
      this.logger.sendMessage(config.consolePrefix.append(config.warn).append((Component)text.get()));
   }

   @Override
   public void errorMessage(Translatable text) {
      this.logger.sendMessage(config.consolePrefix.append(config.error).append((Component)text.get()));
   }

   @Override
   public void infoLegacyMessage(String message) {
      this.logger.sendMessage(config.consolePrefix.append(config.info).append(Component.text(message)));
   }

   @Override
   public void logLegacyDebugMessage(String message) {
      this.logger.sendMessage(config.consolePrefix.append(config.debug).append(Component.text(message)));
   }

   @Override
   public void warnLegacyMessage(String message) {
      this.logger.sendMessage(config.consolePrefix.append(config.warn).append(Component.text(message)));
   }

   @Override
   public void errorLegacyMessage(String message) {
      this.logger.sendMessage(config.consolePrefix.append(config.error).append(Component.text(message)));
   }

   @Override
   public void infoMessage(Object value, Translatable text) {
      if (value instanceof Player) {
         ((Player)value).sendMessage(config.prefix.color(COLOR_MAP.get('a')).append((Component)text.get()));
      } else {
         ((CommandSender)value).sendMessage(config.consolePrefix.append(config.info).append((Component)text.get()));
      }
   }

   @Override
   public void warnMessage(Object value, Translatable text) {
      if (value instanceof Player) {
         ((Player)value).sendMessage(config.prefix.color(COLOR_MAP.get('c')).append((Component)text.get()));
      } else {
         ((CommandSender)value).sendMessage(config.consolePrefix.append(config.warn).append((Component)text.get()));
      }
   }

   @Override
   public void errorMessage(Object value, Translatable text) {
      if (value instanceof Player) {
         ((Player)value).sendMessage(config.prefix.color(COLOR_MAP.get('c')).append((Component)text.get()));
      } else {
         ((CommandSender)value).sendMessage(config.consolePrefix.append(config.error).append((Component)text.get()));
      }
   }

   @Override
   public void infoLegacyMessage(Object value, String message) {
      if (value instanceof Player) {
         ((Player)value).sendMessage(AdventureUtil.toLegacy(config.prefix) + ChatColor.GREEN + message);
      } else if (value instanceof CommandSender) {
         ((CommandSender)value).sendMessage(AdventureUtil.toLegacy(config.consolePrefix.append(config.info)) + message);
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
         ((Player)value).sendMessage(AdventureUtil.toLegacy(config.prefix) + ChatColor.RED + message);
      } else if (value instanceof CommandSender) {
         ((CommandSender)value).sendMessage(AdventureUtil.toLegacy(config.consolePrefix.append(config.warn)) + message);
      }
   }

   @Override
   public void errorLegacyMessage(Object value, String message) {
      if (value instanceof Player) {
         ((Player)value).sendMessage(AdventureUtil.toLegacy(config.prefix) + ChatColor.RED + message);
      } else if (value instanceof CommandSender) {
         ((CommandSender)value).sendMessage(AdventureUtil.toLegacy(config.consolePrefix.append(config.error)) + message);
      }
   }

   static {
      COLOR_MAP.put('0', TextColor.fromHexString("#000000"));
      COLOR_MAP.put('1', TextColor.fromHexString("#0000AA"));
      COLOR_MAP.put('2', TextColor.fromHexString("#00AA00"));
      COLOR_MAP.put('3', TextColor.fromHexString("#00AAAA"));
      COLOR_MAP.put('4', TextColor.fromHexString("#AA0000"));
      COLOR_MAP.put('5', TextColor.fromHexString("#AA00AA"));
      COLOR_MAP.put('6', TextColor.fromHexString("#FFAA00"));
      COLOR_MAP.put('7', TextColor.fromHexString("#A9A9A9"));
      COLOR_MAP.put('8', TextColor.fromHexString("#555555"));
      COLOR_MAP.put('9', TextColor.fromHexString("#5555FF"));
      COLOR_MAP.put('a', TextColor.fromHexString("#55FF55"));
      COLOR_MAP.put('b', TextColor.fromHexString("#55FFFF"));
      COLOR_MAP.put('c', TextColor.fromHexString("#FF5555"));
      COLOR_MAP.put('d', TextColor.fromHexString("#FF55FF"));
      COLOR_MAP.put('e', TextColor.fromHexString("#FFFF55"));
      COLOR_MAP.put('f', TextColor.fromHexString("#FFFFFF"));
   }

   private static final class BukkitUtils implements PaperMeta.Utils {
      private BukkitUtils() {
      }

      @Override
      public Inventory createInventory(InventoryHolder holder, int value, Translatable text) {
         return Bukkit.createInventory(holder, value, text.getLegacy());
      }

      @Override
      public void setRawDisplayName(ItemMeta itemMeta, String name) {
         itemMeta.setDisplayName(name);
      }

      @Override
      public void setDisplayName(ItemMeta itemMeta, Translatable text) {
         itemMeta.setDisplayName(text.getLegacy());
      }

      @Override
      public String getRawDisplayName(ItemMeta itemMeta) {
         return itemMeta.getDisplayName();
      }

      @Override
      public Translatable getDisplayName(ItemMeta itemMeta) {
         return new TranslatableComponent(itemMeta.getDisplayName());
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
         itemMeta.setLore(values.stream().map(Translatable::getLegacy).collect(Collectors.toList()));
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
         return itemMeta.getLore().stream().map(TranslatableComponent::new).collect(Collectors.toList());
      }
   }

   private static final class Config {
      public final Component info = ChatUtil.getAdventureUtils().formatLegacyAndMini("§8[§7INFO§8]§r: ");
      public final Component debug = ChatUtil.getAdventureUtils().formatLegacyAndMini("§8[§6DEBUG§8]§r: ");
      public final Component warn = ChatUtil.getAdventureUtils().formatLegacyAndMini("§8[§cWARN§8]§r: ");
      public final Component error = ChatUtil.getAdventureUtils().formatLegacyAndMini("§8[§4ERROR§8]§r: ");
      public final Component consolePrefix = ChatUtil.getAdventureUtils().formatLegacyAndMini("§8[§3OnyxShop§8]§r ");
      public Component prefix;

      private Config() {
      }

      public void update() {
         this.prefix = ((Component)Lang.SHOP_PREFIX.get().get()).append(PaperMeta.RESET);
      }
   }

   private static final class PaperUtils implements PaperMeta.Utils {
      private static final Method GET_DISPLAYNAME;
      private static final Method SET_DISPLAYNAME;
      private static final Method GET_ITEMNAME;
      private static final Method GET_LORE;
      private static final Method SET_LORE;

      private PaperUtils() {
      }

      @Override
      public Inventory createInventory(InventoryHolder holder, int value, Translatable text) {
         return Bukkit.createInventory(holder, value, (Component)text.get());
      }

      @Override
      public void setRawDisplayName(ItemMeta itemMeta, String name) {
         try {
            SET_DISPLAYNAME.invoke(itemMeta, ChatUtil.getAdventureUtils().formatLegacyAndMini(name).decoration(TextDecoration.ITALIC, false));
         } catch (InvocationTargetException | IllegalAccessException exception) {
            exception.printStackTrace();
         }
      }

      @Override
      public void setDisplayName(ItemMeta itemMeta, Translatable text) {
         try {
            SET_DISPLAYNAME.invoke(itemMeta, ((Component)text.get()).decoration(TextDecoration.ITALIC, false));
         } catch (InvocationTargetException | IllegalAccessException exception) {
            exception.printStackTrace();
         }
      }

      @Override
      public String getRawDisplayName(ItemMeta itemMeta) {
         try {
            return AdventureUtil.serialize((Component)GET_DISPLAYNAME.invoke(itemMeta));
         } catch (InvocationTargetException | IllegalAccessException exception) {
            exception.printStackTrace();
            return null;
         }
      }

      @Override
      public Translatable getDisplayName(ItemMeta itemMeta) {
         try {
            return new TranslatableComponent((Component)GET_DISPLAYNAME.invoke(itemMeta));
         } catch (InvocationTargetException | IllegalAccessException exception) {
            exception.printStackTrace();
            return null;
         }
      }

      @Override
      public String getRawItemName(ItemMeta itemMeta) {
         try {
            return AdventureUtil.serialize((Component)GET_ITEMNAME.invoke(itemMeta));
         } catch (InvocationTargetException | IllegalAccessException exception) {
            exception.printStackTrace();
            return null;
         }
      }

      @Override
      public void setRawLore(ItemMeta itemMeta, List<String> lores) {
         try {
            SET_LORE.invoke(
               itemMeta,
               lores.stream()
                  .map(element -> ChatUtil.getAdventureUtils().formatLegacyAndMini(element).decoration(TextDecoration.ITALIC, false))
                  .collect(Collectors.toList())
            );
         } catch (InvocationTargetException | IllegalAccessException exception) {
            exception.printStackTrace();
         }
      }

      @Override
      public void setLore(ItemMeta itemMeta, List<Translatable> values) {
         try {
            SET_LORE.invoke(itemMeta, values.stream().map(element -> ((Component)element.get()).decoration(TextDecoration.ITALIC, false)).collect(Collectors.toList()));
         } catch (InvocationTargetException | IllegalAccessException exception) {
            exception.printStackTrace();
         }
      }

      @Override
      public void addLore(ItemMeta itemMeta, Translatable text) {
         try {
            List<Component> values = (List<Component>)GET_LORE.invoke(itemMeta);
            if (values == null) {
               values = new ArrayList();
            }

            values.add(((Component)text.get()).decoration(TextDecoration.ITALIC, false));
            SET_LORE.invoke(itemMeta, values);
         } catch (InvocationTargetException | IllegalAccessException exception) {
            exception.printStackTrace();
         }
      }

      @Override
      public List<String> getRawLore(ItemMeta itemMeta) {
         try {
            List<Component> values = (List<Component>)GET_LORE.invoke(itemMeta);
            if (values == null) {
               values = new ArrayList<>();
            }

            return values.stream().map(element -> AdventureUtil.serialize(element)).collect(Collectors.toList());
         } catch (InvocationTargetException | IllegalAccessException exception) {
            exception.printStackTrace();
            return null;
         }
      }

      @Override
      public List<Translatable> getLore(ItemMeta itemMeta) {
         try {
            List<Component> values = (List<Component>)GET_LORE.invoke(itemMeta);
            if (values == null) {
               values = new ArrayList<>();
            }

            return values.stream().map(TranslatableComponent::new).collect(Collectors.toList());
         } catch (InvocationTargetException | IllegalAccessException exception) {
            exception.printStackTrace();
            return null;
         }
      }

      static {
         try {
            SET_DISPLAYNAME = ItemMeta.class.getDeclaredMethod("displayName", Component.class);
            SET_DISPLAYNAME.setAccessible(true);
            GET_DISPLAYNAME = ItemMeta.class.getDeclaredMethod("displayName");
            GET_DISPLAYNAME.setAccessible(true);
            GET_ITEMNAME = ItemMeta.class.getDeclaredMethod("itemName");
            SET_LORE = ItemMeta.class.getDeclaredMethod("lore", List.class);
            SET_LORE.setAccessible(true);
            GET_LORE = ItemMeta.class.getDeclaredMethod("lore");
            GET_LORE.setAccessible(true);
         } catch (NoSuchMethodException exception) {
            throw new RuntimeException(exception);
         }
      }
   }

   private interface Utils {
      Inventory createInventory(InventoryHolder holder, int value, Translatable text);

      void setRawDisplayName(ItemMeta itemMeta, String name);

      void setDisplayName(ItemMeta itemMeta, Translatable text);

      String getRawDisplayName(ItemMeta itemMeta);

      Translatable getDisplayName(ItemMeta itemMeta);

      String getRawItemName(ItemMeta itemMeta);

      void setRawLore(ItemMeta itemMeta, List<String> lores);

      void setLore(ItemMeta itemMeta, List<Translatable> values);

      void addLore(ItemMeta itemMeta, Translatable text);

      List<String> getRawLore(ItemMeta itemMeta);

      List<Translatable> getLore(ItemMeta itemMeta);
   }
}
