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

   public PaperMeta(OnyxShop var1) {
      this.logger = var1.getServer().getConsoleSender();
      this.plugin = var1;
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

   private void send(Player var1, Component var2) {
      var1.sendMessage(config.prefix.append(var2));
   }

   @Override
   public void send(Player var1, Translatable var2) {
      var1.sendMessage(config.prefix.append((Component)var2.get()));
   }

   @Override
   public void send(Object var1, Translatable var2) {
      if (var1 instanceof Player) {
         ((Player)var1).sendMessage(config.prefix.append((Component)var2.get()));
      } else {
         ((CommandSender)var1).sendMessage((Component)var2.get());
      }
   }

   @Override
   public void send(Translatable var1) {
      this.logger.sendMessage((Component)var1.get());
   }

   @Override
   public void sendLegacy(Player var1, String var2) {
      var1.sendMessage(AdventureUtil.toLegacy(config.prefix) + var2);
   }

   @Override
   public void sendLegacy(Object var1, String var2) {
      if (var1 instanceof Player) {
         ((Player)var1).sendMessage(AdventureUtil.toLegacy(config.prefix) + var2);
      } else {
         ((CommandSender)var1).sendMessage(var2);
      }
   }

   @Override
   public void sendLegacy(String var1) {
      this.logger.sendMessage(var1);
   }

   @Override
   public void sendTransactionMessage(Player var1, int var2, double var3, ShopItem var5, Transaction.Mode var6, Transaction.Type var7, boolean var8) {
      this.sendTransactionMessage(var1, var2, Collections.singletonMap(var5.getEcoType(), var3), var5, var6, var7, var8);
   }

   @Override
   public void sendTransactionMessage(
      Player var1, int var2, Map<EcoType, Double> var3, ShopItem var4, Transaction.Mode var5, Transaction.Type var6, boolean var7
   ) {
      Translatable var8 = var5 == Transaction.Mode.BUY
         ? (var7 ? Lang.PAY_CONFIRMATION_COMMAND.get(var1) : Lang.PAY_CONFIRMATION.get(var1))
         : (var7 ? Lang.SELL_CONFIRMATION_COMMAND.get(var1) : Lang.SELL_CONFIRMATION.get(var1));
      String var9 = ChatUtil.getAdventureUtils().getLastColors(var8.toString().split("%material%")[0]);
      this.send(
         var1,
         (Component)var8.replace("%amounttopay%", this.getCurrencyDisplay(var9, var3))
            .replace("%amount%", String.valueOf(var2))
            .replace(
               "%material%",
               (
                     var4 instanceof ShopItem.Shulker
                        ? this.translate(var4)
                           + ChatColor.GRAY
                           + ((ShopItem.Shulker)var4)
                              .getContents()
                              .entrySet()
                              .stream()
                              .map(var1x -> var1x.getValue() + "x " + this.translate(var1x.getKey()) + ChatColor.GRAY)
                              .collect(Collectors.toList())
                        : this.translate(var4)
                  )
                  + var9
            )
            .get()
      );
   }

   private String translate(ShopItem var1) {
      return var1.hasCustomName() ? var1.getDisplayname() : "<lang:" + var1.getTranslationKey() + ">";
   }

   @Override
   public void sendTransactionMessage(Player var1, int var2, Map<EcoType, Double> var3, Map<ShopItem, Integer> var4, Transaction.Type var5) {
      Translatable var6 = Lang.SELL_CONFIRMATION_MULTIPLE_ITEMS.get(var1);
      String var7 = ChatUtil.getAdventureUtils().getLastColors(var6.toString().split("%amounttopay%")[0]);
      HoverEvent var8 = HoverEvent.showText(ChatUtil.getAdventureUtils().formatLegacyAndMini("§7" + this.getItemDisplay("§7", var4))).asHoverEvent();
      this.send(
         var1,
         ((Component)var6.replace("%items%", this.getItemDisplay(var7, var4))
               .replace("%amount%", String.valueOf(var2))
               .replace("%amounttopay%", this.getCurrencyDisplay(var7, var3))
               .get())
            .hoverEvent(var8)
      );
   }

   private String getItemDisplay(String var1, Map<ShopItem, Integer> var2) {
      StringBuilder var3 = new StringBuilder();
      int var4 = 0;

      for (Entry<ShopItem, Integer> var6 : var2.entrySet()) {
         if (var6.getKey() instanceof ShopItem.Shulker) {
            Map<ShopItem, Integer> var7 = ((ShopItem.Shulker)var6.getKey()).getContents();
            var3.append(var6.getValue()).append("x ").append(this.translate((ShopItem)var6.getKey()));
            var3.append(var7.entrySet().stream().map(var1x -> var1x.getValue() + "x " + this.translate((ShopItem)var1x.getKey())).collect(Collectors.toList()));
         } else {
            var3.append(var6.getValue()).append("x ").append(this.translate((ShopItem)var6.getKey()));
         }

         var3.append(var1);
         if (var4 != var2.size() - 1) {
            var3.append(", ");
         }

         var4++;
      }

      return var3.toString();
   }

   private String getCurrencyDisplay(String var1, Map<EcoType, Double> var2) {
      StringBuilder var3 = new StringBuilder();
      int var4 = 0;

      for (Entry<EcoType, Double> var6 : var2.entrySet()) {
         var3.append(this.plugin.formatPrice(var6.getKey(), var6.getValue()));
         var3.append(var1);
         if (var4 != var2.size() - 1) {
            var3.append(", ");
         }

         var4++;
      }

      return var3.toString();
   }

   @Override
   public Inventory createInventory(InventoryHolder var1, int var2, Translatable var3) {
      return this.utils.createInventory(var1, var2, var3);
   }

   @Override
   public void setRawDisplayName(ItemMeta var1, String var2) {
      this.utils.setRawDisplayName(var1, var2);
   }

   @Override
   public void setDisplayName(ItemMeta var1, Translatable var2) {
      this.utils.setDisplayName(var1, var2);
   }

   @Override
   public String getRawDisplayName(ItemMeta var1) {
      return this.utils.getRawDisplayName(var1);
   }

   @Override
   public String getRawItemName(ItemMeta var1) {
      return this.utils.getRawItemName(var1);
   }

   @Override
   public Translatable getDisplayName(ItemMeta var1) {
      return this.utils.getDisplayName(var1);
   }

   public static ItemStack setDisplayname(ItemStack var0, String var1) {
      var0.setData(DataComponentTypes.ITEM_NAME, AdventureUtil.fromGson(var1).decoration(TextDecoration.ITALIC, false));
      return var0;
   }

   @Override
   public void setRawLore(ItemMeta var1, List<String> var2) {
      this.utils.setRawLore(var1, var2);
   }

   @Override
   public void setLore(ItemMeta var1, List<Translatable> var2) {
      this.utils.setLore(var1, var2);
   }

   @Override
   public void addLore(ItemMeta var1, Translatable var2) {
      this.utils.addLore(var1, var2);
   }

   @Override
   public List<String> getRawLore(ItemMeta var1) {
      return this.utils.getRawLore(var1);
   }

   @Override
   public List<Translatable> getLore(ItemMeta var1) {
      return this.utils.getLore(var1);
   }

   public static ItemStack setLore(ItemStack var0, ArrayList<String> var1) {
      var0.setData(
         DataComponentTypes.LORE,
         ItemLore.lore(var1.stream().map(var0x -> AdventureUtil.fromGson(var0x).decoration(TextDecoration.ITALIC, false)).collect(Collectors.toList()))
      );
      return var0;
   }

   public static ItemStack setRawLore(ItemStack var0, ArrayList<String> var1) {
      var0.setData(
         DataComponentTypes.LORE,
         ItemLore.lore(
            var1.stream()
               .map(var0x -> ChatUtil.getAdventureUtils().formatLegacyAndMini(var0x).decoration(TextDecoration.ITALIC, false))
               .collect(Collectors.toList())
         )
      );
      return var0;
   }

   @Override
   public void broadcast(Translatable var1) {
      this.plugin.getServer().broadcast(config.prefix.append((Component)var1.get()));
   }

   @Override
   public void logDevDebugMessage(Translatable var1) {
      this.logger.sendMessage(config.consolePrefix.append(config.debug).append((Component)var1.get()));
   }

   @Override
   public void logPlayerTransaction(Translatable var1) {
      this.logger.sendMessage(config.prefix.append((Component)var1.get()));
   }

   @Override
   public void logPlayerTransaction(String var1) {
      this.logger.sendMessage(config.prefix.append(Component.text(var1)));
   }

   @Override
   public void infoMessage(Translatable var1) {
      this.logger.sendMessage(config.consolePrefix.append(config.info).append((Component)var1.get()));
   }

   @Override
   public void logDebugMessage(Translatable var1) {
      this.logger.sendMessage(config.consolePrefix.append(config.debug).append((Component)var1.get()));
   }

   @Override
   public void warnMessage(Translatable var1) {
      this.logger.sendMessage(config.consolePrefix.append(config.warn).append((Component)var1.get()));
   }

   @Override
   public void errorMessage(Translatable var1) {
      this.logger.sendMessage(config.consolePrefix.append(config.error).append((Component)var1.get()));
   }

   @Override
   public void infoLegacyMessage(String var1) {
      this.logger.sendMessage(config.consolePrefix.append(config.info).append(Component.text(var1)));
   }

   @Override
   public void logLegacyDebugMessage(String var1) {
      this.logger.sendMessage(config.consolePrefix.append(config.debug).append(Component.text(var1)));
   }

   @Override
   public void warnLegacyMessage(String var1) {
      this.logger.sendMessage(config.consolePrefix.append(config.warn).append(Component.text(var1)));
   }

   @Override
   public void errorLegacyMessage(String var1) {
      this.logger.sendMessage(config.consolePrefix.append(config.error).append(Component.text(var1)));
   }

   @Override
   public void infoMessage(Object var1, Translatable var2) {
      if (var1 instanceof Player) {
         ((Player)var1).sendMessage(config.prefix.color(COLOR_MAP.get('a')).append((Component)var2.get()));
      } else {
         ((CommandSender)var1).sendMessage(config.consolePrefix.append(config.info).append((Component)var2.get()));
      }
   }

   @Override
   public void warnMessage(Object var1, Translatable var2) {
      if (var1 instanceof Player) {
         ((Player)var1).sendMessage(config.prefix.color(COLOR_MAP.get('c')).append((Component)var2.get()));
      } else {
         ((CommandSender)var1).sendMessage(config.consolePrefix.append(config.warn).append((Component)var2.get()));
      }
   }

   @Override
   public void errorMessage(Object var1, Translatable var2) {
      if (var1 instanceof Player) {
         ((Player)var1).sendMessage(config.prefix.color(COLOR_MAP.get('c')).append((Component)var2.get()));
      } else {
         ((CommandSender)var1).sendMessage(config.consolePrefix.append(config.error).append((Component)var2.get()));
      }
   }

   @Override
   public void infoLegacyMessage(Object var1, String var2) {
      if (var1 instanceof Player) {
         ((Player)var1).sendMessage(AdventureUtil.toLegacy(config.prefix) + ChatColor.GREEN + var2);
      } else if (var1 instanceof CommandSender) {
         ((CommandSender)var1).sendMessage(AdventureUtil.toLegacy(config.consolePrefix.append(config.info)) + var2);
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
         ((Player)var1).sendMessage(AdventureUtil.toLegacy(config.prefix) + ChatColor.RED + var2);
      } else if (var1 instanceof CommandSender) {
         ((CommandSender)var1).sendMessage(AdventureUtil.toLegacy(config.consolePrefix.append(config.warn)) + var2);
      }
   }

   @Override
   public void errorLegacyMessage(Object var1, String var2) {
      if (var1 instanceof Player) {
         ((Player)var1).sendMessage(AdventureUtil.toLegacy(config.prefix) + ChatColor.RED + var2);
      } else if (var1 instanceof CommandSender) {
         ((CommandSender)var1).sendMessage(AdventureUtil.toLegacy(config.consolePrefix.append(config.error)) + var2);
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
      public Inventory createInventory(InventoryHolder var1, int var2, Translatable var3) {
         return Bukkit.createInventory(var1, var2, var3.getLegacy());
      }

      @Override
      public void setRawDisplayName(ItemMeta var1, String var2) {
         var1.setDisplayName(var2);
      }

      @Override
      public void setDisplayName(ItemMeta var1, Translatable var2) {
         var1.setDisplayName(var2.getLegacy());
      }

      @Override
      public String getRawDisplayName(ItemMeta var1) {
         return var1.getDisplayName();
      }

      @Override
      public Translatable getDisplayName(ItemMeta var1) {
         return new TranslatableComponent(var1.getDisplayName());
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
         var1.setLore(var2.stream().map(Translatable::getLegacy).collect(Collectors.toList()));
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
         return var1.getLore().stream().map(TranslatableComponent::new).collect(Collectors.toList());
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
      public Inventory createInventory(InventoryHolder var1, int var2, Translatable var3) {
         return Bukkit.createInventory(var1, var2, (Component)var3.get());
      }

      @Override
      public void setRawDisplayName(ItemMeta var1, String var2) {
         try {
            SET_DISPLAYNAME.invoke(var1, ChatUtil.getAdventureUtils().formatLegacyAndMini(var2).decoration(TextDecoration.ITALIC, false));
         } catch (InvocationTargetException | IllegalAccessException var4) {
            var4.printStackTrace();
         }
      }

      @Override
      public void setDisplayName(ItemMeta var1, Translatable var2) {
         try {
            SET_DISPLAYNAME.invoke(var1, ((Component)var2.get()).decoration(TextDecoration.ITALIC, false));
         } catch (InvocationTargetException | IllegalAccessException var4) {
            var4.printStackTrace();
         }
      }

      @Override
      public String getRawDisplayName(ItemMeta var1) {
         try {
            return AdventureUtil.serialize((Component)GET_DISPLAYNAME.invoke(var1));
         } catch (InvocationTargetException | IllegalAccessException var3) {
            var3.printStackTrace();
            return null;
         }
      }

      @Override
      public Translatable getDisplayName(ItemMeta var1) {
         try {
            return new TranslatableComponent((Component)GET_DISPLAYNAME.invoke(var1));
         } catch (InvocationTargetException | IllegalAccessException var3) {
            var3.printStackTrace();
            return null;
         }
      }

      @Override
      public String getRawItemName(ItemMeta var1) {
         try {
            return AdventureUtil.serialize((Component)GET_ITEMNAME.invoke(var1));
         } catch (InvocationTargetException | IllegalAccessException var3) {
            var3.printStackTrace();
            return null;
         }
      }

      @Override
      public void setRawLore(ItemMeta var1, List<String> var2) {
         try {
            SET_LORE.invoke(
               var1,
               var2.stream()
                  .map(var0 -> ChatUtil.getAdventureUtils().formatLegacyAndMini(var0).decoration(TextDecoration.ITALIC, false))
                  .collect(Collectors.toList())
            );
         } catch (InvocationTargetException | IllegalAccessException var4) {
            var4.printStackTrace();
         }
      }

      @Override
      public void setLore(ItemMeta var1, List<Translatable> var2) {
         try {
            SET_LORE.invoke(var1, var2.stream().map(var0 -> ((Component)var0.get()).decoration(TextDecoration.ITALIC, false)).collect(Collectors.toList()));
         } catch (InvocationTargetException | IllegalAccessException var4) {
            var4.printStackTrace();
         }
      }

      @Override
      public void addLore(ItemMeta var1, Translatable var2) {
         try {
            List<Component> var3 = (List<Component>)GET_LORE.invoke(var1);
            if (var3 == null) {
               var3 = new ArrayList();
            }

            var3.add(((Component)var2.get()).decoration(TextDecoration.ITALIC, false));
            SET_LORE.invoke(var1, var3);
         } catch (InvocationTargetException | IllegalAccessException var4) {
            var4.printStackTrace();
         }
      }

      @Override
      public List<String> getRawLore(ItemMeta var1) {
         try {
            List<Component> var2 = (List<Component>)GET_LORE.invoke(var1);
            if (var2 == null) {
               var2 = new ArrayList<>();
            }

            return var2.stream().map(var0 -> AdventureUtil.serialize(var0)).collect(Collectors.toList());
         } catch (InvocationTargetException | IllegalAccessException var3) {
            var3.printStackTrace();
            return null;
         }
      }

      @Override
      public List<Translatable> getLore(ItemMeta var1) {
         try {
            List<Component> var2 = (List<Component>)GET_LORE.invoke(var1);
            if (var2 == null) {
               var2 = new ArrayList<>();
            }

            return var2.stream().map(TranslatableComponent::new).collect(Collectors.toList());
         } catch (InvocationTargetException | IllegalAccessException var3) {
            var3.printStackTrace();
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
         } catch (NoSuchMethodException var1) {
            throw new RuntimeException(var1);
         }
      }
   }

   private interface Utils {
      Inventory createInventory(InventoryHolder var1, int var2, Translatable var3);

      void setRawDisplayName(ItemMeta var1, String var2);

      void setDisplayName(ItemMeta var1, Translatable var2);

      String getRawDisplayName(ItemMeta var1);

      Translatable getDisplayName(ItemMeta var1);

      String getRawItemName(ItemMeta var1);

      void setRawLore(ItemMeta var1, List<String> var2);

      void setLore(ItemMeta var1, List<Translatable> var2);

      void addLore(ItemMeta var1, Translatable var2);

      List<String> getRawLore(ItemMeta var1);

      List<Translatable> getLore(ItemMeta var1);
   }
}
