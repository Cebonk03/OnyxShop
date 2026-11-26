package com.onyxshop.providers;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import com.onyxshop.OnyxShop;
import com.onyxshop.api.events.ItemProviderPreLoadEvent;
import com.onyxshop.events.api.ProviderLoadEvent;
import com.onyxshop.events.delayed.DelayedItemHandler;
import com.onyxshop.events.delayed.DelayedItemProvider;
import com.onyxshop.events.delayed.DelayedLoadListener;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.providers.items.BreweryHook;
import com.onyxshop.providers.items.CraftEngineHook;
import com.onyxshop.providers.items.EcoItemsHook;
import com.onyxshop.providers.items.ExecutableItemsHook;
import com.onyxshop.providers.items.ExternalItemsProvider;
import com.onyxshop.providers.items.HeadDatabaseHook;
import com.onyxshop.providers.items.ItemsAdderHook;
import com.onyxshop.providers.items.MMOItemsHook;
import com.onyxshop.providers.items.MythicMobsHook;
import com.onyxshop.providers.items.NexoItemsHook;
import com.onyxshop.providers.items.OraxenItemsHook;
import com.onyxshop.providers.items.SlimefunHook;
import com.onyxshop.util.ExternalItemType;
import com.onyxshop.util.ItemProviderType;
import com.onyxshop.util.ItemType;
import com.onyxshop.util.exceptions.ItemLoadException;
import com.onyxshop.util.exceptions.ItemProviderLoadException;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;

public class ItemManager {
   private final OnyxShop plugin;
   private static DelayedItemHandler delayedItemHandler;
   private final ItemProviderPreLoadEvent preLoadEvent;
   private final Map<ItemType, ItemProvider> providers = new HashMap<>();
   private final Map<String, ItemManager.CustomItem> loadedItems = new HashMap<>();

   public ItemManager(OnyxShop var1) {
      this.plugin = var1;
      this.preLoadEvent = new ItemProviderPreLoadEvent(var1);
   }

   public void registerHooks() {
      PluginManager var1 = this.plugin.getServer().getPluginManager();
      var1.callEvent(this.preLoadEvent);
      if (var1.getPlugin("ItemsAdder") != null) {
         this.providers.put(new ItemType(ItemProviderType.ITEMS_ADDER), new ItemsAdderHook(this.plugin));
      }

      if (var1.getPlugin("HeadDatabase") != null) {
         try {
            this.providers.put(new ItemType(ItemProviderType.HEAD_DATABASE), new HeadDatabaseHook(this.plugin));
         } catch (ItemProviderLoadException var3) {
            SendMessage.warnMessage("Failed to register HeadDatabase hook: " + var3.getMessage());
         }
      }

      if (var1.getPlugin("MMOItems") != null) {
         this.providers.put(new ItemType(ItemProviderType.MMO_ITEMS), new MMOItemsHook(this.plugin));
      }

      if (var1.getPlugin("ExecutableItems") != null) {
         this.providers.put(new ItemType(ItemProviderType.EXECUTEABLE_ITEMS), new ExecutableItemsHook(this.plugin));
      }

      if (var1.getPlugin("Oraxen") != null) {
         this.providers.put(new ItemType(ItemProviderType.ORAXEN), new OraxenItemsHook(this.plugin));
      }

      if (var1.getPlugin("Brewery") != null || var1.getPlugin("BreweryX") != null) {
         this.providers.put(new ItemType(ItemProviderType.BREWERY), new BreweryHook(this.plugin, var1.getPlugin("Brewery") == null));
      }

      if (var1.getPlugin("Nexo") != null) {
         this.providers.put(new ItemType(ItemProviderType.NEXO), new NexoItemsHook(this.plugin));
      }

      if (var1.getPlugin("Slimefun") != null) {
         this.providers.put(new ItemType(ItemProviderType.SLIMEFUN), new SlimefunHook(this.plugin));
      }

      if (var1.getPlugin("MythicMobs") != null) {
         this.providers.put(new ItemType(ItemProviderType.MYTHIC_MOBS), new MythicMobsHook(this.plugin));
      }

      if (var1.getPlugin("EcoItems") != null) {
         this.providers.put(new ItemType(ItemProviderType.ECOITEMS), new EcoItemsHook(this.plugin));
      }

      if (var1.getPlugin("CraftEngine") != null) {
         this.providers.put(new ItemType(ItemProviderType.CRAFT_ENGINE), new CraftEngineHook(this.plugin));
      }
   }

   public boolean doWait() {
      return !this.providers.isEmpty();
   }

   public HeadDatabaseHook getHdbHook() {
      return (HeadDatabaseHook)this.providers.get(ItemProviderType.HEAD_DATABASE);
   }

   public static void registerDelayed(DelayedItemProvider var0) {
      if (delayedItemHandler == null) {
         delayedItemHandler = new DelayedItemHandler(OnyxShop.getInstance());
      }

      delayedItemHandler.registerProvider(var0);
   }

   public void registerExternalProvider(ExternalItemsProvider var1) {
      this.providers.put(var1.getType(), var1);
   }

   public ItemStack getSPM(ShopItem var1, String var2) throws ItemLoadException {
      if (!var2.contains(":")) {
         return null;
      } else {
         String[] var3 = var2.split(":", 2);
         ItemProviderType var4 = ItemProviderType.fromPrefix(var3[0].toLowerCase(Locale.ROOT));
         if (var4 == null) {
            return null;
         } else if (var4 != ItemProviderType.EXTERNAL) {
            if (!this.providers.containsKey(var4)) {
               throw new ItemLoadException(Lang.FAILED_PLUGIN_INTEGRATION.get().replace("%plugin%", var4.name));
            } else {
               if (var1 != null) {
                  if (var4.taggable != null) {
                     var1.addTaggable(var4.taggable);
                  }

                  if (var4.serializer != null) {
                     var1.setSerializer(var4.serializer);
                  }
               } else if (!this.providers.get(var4).isReady()) {
                  throw new ItemLoadException("Item provider not enabled yet!");
               }

               return this.providers.get(var4).getItem(var3[1]);
            }
         } else {
            ItemProvider var5 = this.providers.get(new ExternalItemType(var2.split(":")[1]));
            if (var5 == null) {
               SendMessage.warnMessage("External item provider for " + var2.split(":")[1] + " not loaded or found");
               return null;
            } else {
               return var5.getItem(var2.split(":", 3)[2]);
            }
         }
      }
   }

   public String getItem(ItemStack var1) {
      for (ItemProvider var3 : this.providers.values()) {
         String var4 = var3.getItem(var1);
         if (var4 != null) {
            return var3.getType().getPrefix() + ":" + var4;
         }
      }

      return null;
   }

   public void clearShopItemIds() {
      this.getAllLoaded().clear();
   }

   public void registerItem(String var1, ItemType var2) {
      String var3 = this.providers.get(var2).getID(var1);
      this.loadedItems.put(var3, new ItemManager.CustomItem(var1, var2));
   }

   public Set<String> getAllLoaded() {
      return this.loadedItems.keySet();
   }

   public ItemManager.CustomItem getRegistered(String var1) {
      return this.loadedItems.get(var1);
   }

   public void check() {
      for (ItemProvider var2 : this.providers.values()) {
         if (!var2.isReady()) {
            return;
         }
      }

      Bukkit.getPluginManager().callEvent(new ProviderLoadEvent(DelayedLoadListener.Type.ITEMS));
   }

   public final class CustomItem {
      private final ItemType type;
      private final String id;

      public CustomItem(String nullx, ItemType nullxx) {
         this.id = nullx;
         this.type = nullxx;
      }

      public ItemStack get() throws ItemLoadException {
         return ItemManager.this.providers.get(this.type).getItem(this.id);
      }

      public String getId() {
         return ItemManager.this.providers.get(this.type).getID(this.id);
      }

      public boolean match(ItemStack var1) {
         String var2 = ItemManager.this.providers.get(this.type).getItem(var1);
         return var2 != null && var2.equals(this.id);
      }
   }
}
