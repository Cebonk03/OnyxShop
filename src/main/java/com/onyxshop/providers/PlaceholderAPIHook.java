package com.onyxshop.providers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.events.ExpansionRegisterEvent;
import me.clip.placeholderapi.events.ExpansionsLoadedEvent;
import com.onyxshop.OnyxShop;
import com.onyxshop.events.api.ProviderLoadEvent;
import com.onyxshop.events.delayed.DelayedLoadListener;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.util.Placeholder;
import com.onyxshop.util.StringSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class PlaceholderAPIHook implements Listener {
   private static final OnyxShop plugin = OnyxShop.getInstance();
   private static final Pattern placeholderPattern = Pattern.compile("%([^% ]+)%");
   private static final ArrayList<String> requiredExpansions = new ArrayList<>();
   public static boolean enabled;

   public PlaceholderAPIHook() {
      if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
         plugin.getServer().getPluginManager().registerEvents(this, plugin);
         if (plugin.getServer().getPluginManager().getPlugin("ItemsAdder") != null) {
            requiredExpansions.addAll(Arrays.asList("img", "iaplayerstat"));
         }

         if (plugin.getServer().getPluginManager().getPlugin("Nexo") != null && !PlaceholderAPI.isRegistered("nexo")) {
            requiredExpansions.addAll(Collections.singletonList("nexo"));
         }
      }
   }

   public void reload() {
      this.integrate();
   }

   public boolean doWait() {
      return plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null && !enabled;
   }

   @EventHandler
   public void onExpansionsLoad(ExpansionsLoadedEvent event) {
      this.enable(true, false);
      ExpansionsLoadedEvent.getHandlerList().unregister(this);
   }

   @EventHandler
   public void onExpansionLoad(ExpansionRegisterEvent event) {
      if (requiredExpansions.isEmpty()) {
         ExpansionRegisterEvent.getHandlerList().unregister(this);
      } else {
         requiredExpansions.remove(event.getExpansion().getIdentifier());
         if (requiredExpansions.isEmpty()) {
            plugin.runTaskLater(() -> this.enable(true, false), 1L);
         }
      }
   }

   private void integrate() {
      if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
         if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            this.enable(true, true);
         } else {
            plugin.runTaskLater(() -> this.enable(plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI"), true), 1L);
         }
      }
   }

   private void enable(boolean flag, boolean flag2) {
      if (flag2 || requiredExpansions.isEmpty()) {
         enabled = flag;
         if (flag) {
            SendMessage.infoMessage(Lang.ENABLED_PLUGIN_HOOK.get().replace("%plugin%", "PlaceholderAPI"));
         } else {
            SendMessage.warnMessage(Lang.FAILED_PLUGIN_INTEGRATION.get().replace("%plugin%", "PlaceholderAPI"));
         }

         Bukkit.getPluginManager().callEvent(new ProviderLoadEvent(DelayedLoadListener.Type.PLACEHOLDERS));
      }
   }

   public static boolean hasPlaceholders(String text) {
      if (!enabled) {
         return false;
      } else {
         Matcher matcher = placeholderPattern.matcher(text);
         return matcher.find();
      }
   }

   public static boolean hasPlaceholders(List<String> values) {
      if (!enabled) {
         return false;
      } else {
         new ArrayList();

         for (String text : values) {
            Matcher matcher = placeholderPattern.matcher(text);
            if (matcher.find()) {
               return true;
            }
         }

         return false;
      }
   }

   public static Map<Integer, String> getPlaceholders(ItemStack itemStack) {
      if (enabled && itemStack.hasItemMeta() && (itemStack.getItemMeta().hasDisplayName() || itemStack.getItemMeta().hasLore())) {
         ItemMeta itemMeta = itemStack.getItemMeta();
         HashMap map = new HashMap();
         if (itemMeta.hasDisplayName() && hasPlaceholders(itemMeta.getDisplayName())) {
            map.put(-1, itemMeta.getDisplayName());
         }

         if (itemMeta.hasLore()) {
            int value = 0;

            for (String lore : itemMeta.getLore()) {
               if (hasPlaceholders(lore)) {
                  map.put(value, lore);
               }

               value++;
            }
         }

         return map;
      } else {
         return Collections.EMPTY_MAP;
      }
   }

   public static Map<Integer, String> getPlaceholders(ItemMeta itemMeta) {
      if (!enabled) {
         return Collections.EMPTY_MAP;
      } else {
         HashMap map = new HashMap();
         if (itemMeta.hasDisplayName() && hasPlaceholders(itemMeta.getDisplayName())) {
            map.put(-1, itemMeta.getDisplayName());
         }

         if (itemMeta.hasLore()) {
            int value = 0;

            for (String lore : itemMeta.getLore()) {
               if (hasPlaceholders(lore)) {
                  map.put(value, lore);
               }

               value++;
            }
         }

         return map;
      }
   }

   public static Map<Integer, Placeholder> getPlaceholders(ItemStack itemStack, String[] itemIds) {
      if (itemStack.hasItemMeta() && (itemStack.getItemMeta().hasDisplayName() || itemStack.getItemMeta().hasLore())) {
         ItemMeta itemMeta = itemStack.getItemMeta();
         HashMap map = new HashMap();
         if (itemMeta.hasDisplayName()) {
            String rawDisplayName = plugin.getMetaUtils().getRawDisplayName(itemMeta);
            if (Placeholder.isPlaceholder(rawDisplayName, itemIds)) {
               map.put(-1, new Placeholder(rawDisplayName, itemIds));
            }
         }

         if (itemMeta.hasLore()) {
            int value = 0;

            for (String lore : plugin.getMetaUtils().getRawLore(itemMeta)) {
               if (Placeholder.isPlaceholder(lore, itemIds)) {
                  map.put(value, new Placeholder(lore, itemIds));
               }

               value++;
            }
         }

         return map;
      } else {
         return Collections.EMPTY_MAP;
      }
   }

   public static void parsePlaceholders(ShopItem shopItem, ItemStack itemStack, Player player) {
      if (enabled && plugin.placeholders) {
         try {
            ItemMeta itemMeta = itemStack.getItemMeta();
            if (shopItem.getItgPlaceholders().containsKey(-1)) {
               plugin.getMetaUtils().setRawDisplayName(itemMeta, shopItem.getItgPlaceholders().get(-1).translate(player, new String[0]));
            }

            if (itemMeta.hasLore()) {
               List rawLore = plugin.getMetaUtils().getRawLore(itemMeta);

               for (Entry entry : shopItem.getItgPlaceholders().entrySet()) {
                  if ((Integer)entry.getKey() != -1) {
                     rawLore.set((Integer)entry.getKey(), ((Placeholder)entry.getValue()).translate(player, new String[0]));
                  }
               }

               plugin.getMetaUtils().setRawLore(itemMeta, rawLore);
            }

            itemStack.setItemMeta(itemMeta);
            if (shopItem.needsUpdate()) {
               StringSerializer.updateItem(shopItem, itemStack);
            }
         } catch (Exception exception) {
            SendMessage.warnMessage("Failed to parse placeholders for item with reason: " + exception.getMessage());
            exception.printStackTrace();
         }
      }
   }

   public static void parsePlaceholders(ItemStack itemStack, Player player) {
      if (enabled && plugin.placeholders && itemStack.hasItemMeta() && (itemStack.getItemMeta().hasDisplayName() || itemStack.getItemMeta().hasLore())) {
         ItemMeta itemMeta = itemStack.getItemMeta();
         if (itemMeta.hasDisplayName()) {
            itemMeta.setDisplayName(translate(player, itemMeta.getDisplayName()));
         }

         if (itemMeta.hasLore()) {
            List lore = itemMeta.getLore();

            for (int size = 0; size < lore.size(); size++) {
               String text = (String)lore.get(size);
               if (hasPlaceholders(text)) {
                  lore.set(size, translate(player, text));
               }
            }

            itemMeta.setLore(lore);
         }

         itemStack.setItemMeta(itemMeta);
      }
   }

   public static String translate(OfflinePlayer offlinePlayer, String playerName) {
      return enabled && plugin.placeholders ? PlaceholderAPI.setPlaceholders(offlinePlayer, playerName) : playerName;
   }

   public static List<String> translate(Player player, List<String> playerNames, int[] values) {
      if (enabled && plugin.placeholders) {
         for (int value : values) {
            playerNames.set(value, PlaceholderAPI.setPlaceholders(player, (String)playerNames.get(value)));
         }

         return playerNames;
      } else {
         return playerNames;
      }
   }
}
