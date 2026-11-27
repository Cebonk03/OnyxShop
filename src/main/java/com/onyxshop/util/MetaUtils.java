package com.onyxshop.util;

import java.util.List;
import java.util.Map;
import com.onyxshop.files.Translatable;
import com.onyxshop.objects.ShopItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.meta.ItemMeta;

public interface MetaUtils {
   void update();

   void send(Player player, Translatable text);

   void send(Object value, Translatable text);

   void send(Translatable text);

   void sendLegacy(Player player, String playerName);

   void sendLegacy(Object value, String text);

   void sendLegacy(String text);

   void sendTransactionMessage(Player player, int value, double amount, ShopItem shopItem, Transaction.Mode mode, Transaction.Type type, boolean flag);

   void sendTransactionMessage(Player player, int value, Map<EcoType, Double> map, ShopItem shopItem, Transaction.Mode mode, Transaction.Type type, boolean flag);

   void sendTransactionMessage(Player player, int value, Map<EcoType, Double> map, Map<ShopItem, Integer> map2, Transaction.Type type);

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

   void broadcast(Translatable text);

   void logDevDebugMessage(Translatable text);

   void logPlayerTransaction(Translatable text);

   void logPlayerTransaction(String playerName);

   void infoMessage(Translatable text);

   void logDebugMessage(Translatable text);

   void warnMessage(Translatable text);

   void errorMessage(Translatable text);

   void infoLegacyMessage(String message);

   void logLegacyDebugMessage(String message);

   void warnLegacyMessage(String message);

   void errorLegacyMessage(String message);

   void infoMessage(Object value, Translatable text);

   void warnMessage(Object value, Translatable text);

   void errorMessage(Object value, Translatable text);

   void infoLegacyMessage(Object value, String message);

   void infoLegacyMessageWithoutPrefix(Object value, String message);

   void warnLegacyMessage(Object value, String message);

   void errorLegacyMessage(Object value, String message);
}
