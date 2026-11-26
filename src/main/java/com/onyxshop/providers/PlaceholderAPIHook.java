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
   public void onExpansionsLoad(ExpansionsLoadedEvent var1) {
      this.enable(true, false);
      ExpansionsLoadedEvent.getHandlerList().unregister(this);
   }

   @EventHandler
   public void onExpansionLoad(ExpansionRegisterEvent var1) {
      if (requiredExpansions.isEmpty()) {
         ExpansionRegisterEvent.getHandlerList().unregister(this);
      } else {
         requiredExpansions.remove(var1.getExpansion().getIdentifier());
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

   private void enable(boolean var1, boolean var2) {
      if (var2 || requiredExpansions.isEmpty()) {
         enabled = var1;
         if (var1) {
            SendMessage.infoMessage(Lang.ENABLED_PLUGIN_HOOK.get().replace("%plugin%", "PlaceholderAPI"));
         } else {
            SendMessage.warnMessage(Lang.FAILED_PLUGIN_INTEGRATION.get().replace("%plugin%", "PlaceholderAPI"));
         }

         Bukkit.getPluginManager().callEvent(new ProviderLoadEvent(DelayedLoadListener.Type.PLACEHOLDERS));
      }
   }

   public static boolean hasPlaceholders(String var0) {
      if (!enabled) {
         return false;
      } else {
         Matcher var1 = placeholderPattern.matcher(var0);
         return var1.find();
      }
   }

   public static boolean hasPlaceholders(List<String> var0) {
      if (!enabled) {
         return false;
      } else {
         new ArrayList();

         for (String var4 : var0) {
            Matcher var1 = placeholderPattern.matcher(var4);
            if (var1.find()) {
               return true;
            }
         }

         return false;
      }
   }

   public static Map<Integer, String> getPlaceholders(ItemStack var0) {
      if (enabled && var0.hasItemMeta() && (var0.getItemMeta().hasDisplayName() || var0.getItemMeta().hasLore())) {
         ItemMeta var1 = var0.getItemMeta();
         HashMap var2 = new HashMap();
         if (var1.hasDisplayName() && hasPlaceholders(var1.getDisplayName())) {
            var2.put(-1, var1.getDisplayName());
         }

         if (var1.hasLore()) {
            int var3 = 0;

            for (String var5 : var1.getLore()) {
               if (hasPlaceholders(var5)) {
                  var2.put(var3, var5);
               }

               var3++;
            }
         }

         return var2;
      } else {
         return Collections.EMPTY_MAP;
      }
   }

   public static Map<Integer, String> getPlaceholders(ItemMeta var0) {
      if (!enabled) {
         return Collections.EMPTY_MAP;
      } else {
         HashMap var1 = new HashMap();
         if (var0.hasDisplayName() && hasPlaceholders(var0.getDisplayName())) {
            var1.put(-1, var0.getDisplayName());
         }

         if (var0.hasLore()) {
            int var2 = 0;

            for (String var4 : var0.getLore()) {
               if (hasPlaceholders(var4)) {
                  var1.put(var2, var4);
               }

               var2++;
            }
         }

         return var1;
      }
   }

   public static Map<Integer, Placeholder> getPlaceholders(ItemStack var0, String[] var1) {
      if (var0.hasItemMeta() && (var0.getItemMeta().hasDisplayName() || var0.getItemMeta().hasLore())) {
         ItemMeta var2 = var0.getItemMeta();
         HashMap var3 = new HashMap();
         if (var2.hasDisplayName()) {
            String var4 = plugin.getMetaUtils().getRawDisplayName(var2);
            if (Placeholder.isPlaceholder(var4, var1)) {
               var3.put(-1, new Placeholder(var4, var1));
            }
         }

         if (var2.hasLore()) {
            int var7 = 0;

            for (String var6 : plugin.getMetaUtils().getRawLore(var2)) {
               if (Placeholder.isPlaceholder(var6, var1)) {
                  var3.put(var7, new Placeholder(var6, var1));
               }

               var7++;
            }
         }

         return var3;
      } else {
         return Collections.EMPTY_MAP;
      }
   }

   public static void parsePlaceholders(ShopItem var0, ItemStack var1, Player var2) {
      if (enabled && plugin.placeholders) {
         try {
            ItemMeta var3 = var1.getItemMeta();
            if (var0.getItgPlaceholders().containsKey(-1)) {
               plugin.getMetaUtils().setRawDisplayName(var3, var0.getItgPlaceholders().get(-1).translate(var2, new String[0]));
            }

            if (var3.hasLore()) {
               List var4 = plugin.getMetaUtils().getRawLore(var3);

               for (Entry var6 : var0.getItgPlaceholders().entrySet()) {
                  if ((Integer)var6.getKey() != -1) {
                     var4.set((Integer)var6.getKey(), ((Placeholder)var6.getValue()).translate(var2, new String[0]));
                  }
               }

               plugin.getMetaUtils().setRawLore(var3, var4);
            }

            var1.setItemMeta(var3);
            if (var0.needsUpdate()) {
               StringSerializer.updateItem(var0, var1);
            }
         } catch (Exception var7) {
            SendMessage.warnMessage("Failed to parse placeholders for item with reason: " + var7.getMessage());
            var7.printStackTrace();
         }
      }
   }

   public static void parsePlaceholders(ItemStack var0, Player var1) {
      if (enabled && plugin.placeholders && var0.hasItemMeta() && (var0.getItemMeta().hasDisplayName() || var0.getItemMeta().hasLore())) {
         ItemMeta var2 = var0.getItemMeta();
         if (var2.hasDisplayName()) {
            var2.setDisplayName(translate(var1, var2.getDisplayName()));
         }

         if (var2.hasLore()) {
            List var3 = var2.getLore();

            for (int var4 = 0; var4 < var3.size(); var4++) {
               String var5 = (String)var3.get(var4);
               if (hasPlaceholders(var5)) {
                  var3.set(var4, translate(var1, var5));
               }
            }

            var2.setLore(var3);
         }

         var0.setItemMeta(var2);
      }
   }

   public static String translate(OfflinePlayer var0, String var1) {
      return enabled && plugin.placeholders ? PlaceholderAPI.setPlaceholders(var0, var1) : var1;
   }

   public static List<String> translate(Player var0, List<String> var1, int[] var2) {
      if (enabled && plugin.placeholders) {
         for (int var6 : var2) {
            var1.set(var6, PlaceholderAPI.setPlaceholders(var0, (String)var1.get(var6)));
         }

         return var1;
      } else {
         return var1;
      }
   }
}
