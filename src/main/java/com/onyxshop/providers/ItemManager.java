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

   public ItemManager(OnyxShop plugin) {
      this.plugin = plugin;
      this.preLoadEvent = new ItemProviderPreLoadEvent(plugin);
   }

   public void registerHooks() {
      PluginManager pluginManager = this.plugin.getServer().getPluginManager();
      pluginManager.callEvent(this.preLoadEvent);
      if (pluginManager.getPlugin("ItemsAdder") != null) {
         this.providers.put(new ItemType(ItemProviderType.ITEMS_ADDER), new ItemsAdderHook(this.plugin));
      }

      if (pluginManager.getPlugin("HeadDatabase") != null) {
         try {
            this.providers.put(new ItemType(ItemProviderType.HEAD_DATABASE), new HeadDatabaseHook(this.plugin));
         } catch (ItemProviderLoadException exception) {
            SendMessage.warnMessage("Failed to register HeadDatabase hook: " + exception.getMessage());
         }
      }

      if (pluginManager.getPlugin("MMOItems") != null) {
         this.providers.put(new ItemType(ItemProviderType.MMO_ITEMS), new MMOItemsHook(this.plugin));
      }

      if (pluginManager.getPlugin("ExecutableItems") != null) {
         this.providers.put(new ItemType(ItemProviderType.EXECUTEABLE_ITEMS), new ExecutableItemsHook(this.plugin));
      }

      if (pluginManager.getPlugin("Oraxen") != null) {
         this.providers.put(new ItemType(ItemProviderType.ORAXEN), new OraxenItemsHook(this.plugin));
      }

      if (pluginManager.getPlugin("Brewery") != null || pluginManager.getPlugin("BreweryX") != null) {
         this.providers.put(new ItemType(ItemProviderType.BREWERY), new BreweryHook(this.plugin, pluginManager.getPlugin("Brewery") == null));
      }

      if (pluginManager.getPlugin("Nexo") != null) {
         this.providers.put(new ItemType(ItemProviderType.NEXO), new NexoItemsHook(this.plugin));
      }

      if (pluginManager.getPlugin("Slimefun") != null) {
         this.providers.put(new ItemType(ItemProviderType.SLIMEFUN), new SlimefunHook(this.plugin));
      }

      if (pluginManager.getPlugin("MythicMobs") != null) {
         this.providers.put(new ItemType(ItemProviderType.MYTHIC_MOBS), new MythicMobsHook(this.plugin));
      }

      if (pluginManager.getPlugin("EcoItems") != null) {
         this.providers.put(new ItemType(ItemProviderType.ECOITEMS), new EcoItemsHook(this.plugin));
      }

      if (pluginManager.getPlugin("CraftEngine") != null) {
         this.providers.put(new ItemType(ItemProviderType.CRAFT_ENGINE), new CraftEngineHook(this.plugin));
      }
   }

   public boolean doWait() {
      return !this.providers.isEmpty();
   }

   public HeadDatabaseHook getHdbHook() {
      return (HeadDatabaseHook)this.providers.get(ItemProviderType.HEAD_DATABASE);
   }

   public static void registerDelayed(DelayedItemProvider delayedItemProvider) {
      if (delayedItemHandler == null) {
         delayedItemHandler = new DelayedItemHandler(OnyxShop.getInstance());
      }

      delayedItemHandler.registerProvider(delayedItemProvider);
   }

   public void registerExternalProvider(ExternalItemsProvider externalItemsProvider) {
      this.providers.put(externalItemsProvider.getType(), externalItemsProvider);
   }

   public ItemStack getSPM(ShopItem shopItem, String itemId) throws ItemLoadException {
      if (!itemId.contains(":")) {
         return null;
      } else {
         String[] values = itemId.split(":", 2);
         ItemProviderType itemProviderType = ItemProviderType.fromPrefix(values[0].toLowerCase(Locale.ROOT));
         if (itemProviderType == null) {
            return null;
         } else if (itemProviderType != ItemProviderType.EXTERNAL) {
            if (!this.providers.containsKey(itemProviderType)) {
               throw new ItemLoadException(Lang.FAILED_PLUGIN_INTEGRATION.get().replace("%plugin%", itemProviderType.name));
            } else {
               if (shopItem != null) {
                  if (itemProviderType.taggable != null) {
                     shopItem.addTaggable(itemProviderType.taggable);
                  }

                  if (itemProviderType.serializer != null) {
                     shopItem.setSerializer(itemProviderType.serializer);
                  }
               } else if (!this.providers.get(itemProviderType).isReady()) {
                  throw new ItemLoadException("Item provider not enabled yet!");
               }

               return this.providers.get(itemProviderType).getItem(values[1]);
            }
         } else {
            ItemProvider itemProvider = this.providers.get(new ExternalItemType(itemId.split(":")[1]));
            if (itemProvider == null) {
               SendMessage.warnMessage("External item provider for " + itemId.split(":")[1] + " not loaded or found");
               return null;
            } else {
               return itemProvider.getItem(itemId.split(":", 3)[2]);
            }
         }
      }
   }

   public String getItem(ItemStack itemStack) {
      for (ItemProvider itemProvider : this.providers.values()) {
         String item = itemProvider.getItem(itemStack);
         if (item != null) {
            return itemProvider.getType().getPrefix() + ":" + item;
         }
      }

      return null;
   }

   public void clearShopItemIds() {
      this.getAllLoaded().clear();
   }

   public void registerItem(String itemId, ItemType itemType) {
      String text = this.providers.get(itemType).getID(itemId);
      this.loadedItems.put(text, new ItemManager.CustomItem(itemId, itemType));
   }

   public Set<String> getAllLoaded() {
      return this.loadedItems.keySet();
   }

   public ItemManager.CustomItem getRegistered(String itemId) {
      return this.loadedItems.get(itemId);
   }

   public void check() {
      for (ItemProvider itemProvider : this.providers.values()) {
         if (!itemProvider.isReady()) {
            return;
         }
      }

      Bukkit.getPluginManager().callEvent(new ProviderLoadEvent(DelayedLoadListener.Type.ITEMS));
   }

   public final class CustomItem {
      private final ItemType type;
      private final String id;

      public CustomItem(String itemId, ItemType itemType) {
         this.id = itemId;
         this.type = itemType;
      }

      public ItemStack get() throws ItemLoadException {
         return ItemManager.this.providers.get(this.type).getItem(this.id);
      }

      public String getId() {
         return ItemManager.this.providers.get(this.type).getID(this.id);
      }

      public boolean match(ItemStack itemStack) {
         String item = ItemManager.this.providers.get(this.type).getItem(itemStack);
         return item != null && item.equals(this.id);
      }
   }
}
