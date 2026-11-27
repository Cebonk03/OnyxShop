package com.onyxshop.providers.items;

import me.arcaniax.hdb.api.DatabaseLoadEvent;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.ItemProvider;
import com.onyxshop.util.ItemProviderType;
import com.onyxshop.util.ItemType;
import com.onyxshop.util.XMaterial;
import com.onyxshop.util.exceptions.ItemLoadException;
import com.onyxshop.util.exceptions.ItemProviderLoadException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class HeadDatabaseHook implements ItemProvider, Listener {
   private static final ItemType TYPE = new ItemType(ItemProviderType.HEAD_DATABASE);
   private final OnyxShop plugin;
   private HeadDatabaseAPI api;
   private boolean isReady = false;

   public HeadDatabaseHook(OnyxShop plugin) throws ItemProviderLoadException {
      try {
         plugin.getServer().getPluginManager().registerEvents(this, plugin);
         this.plugin = plugin;
      } catch (IllegalStateException exception) {
         throw new ItemProviderLoadException("Cannot register head database load event");
      }
   }

   @EventHandler
   public void onDatabaseLoad(DatabaseLoadEvent event) {
      this.api = new HeadDatabaseAPI();
      SendMessage.infoMessage(Lang.ENABLED_PLUGIN_HOOK.get().replace("%plugin%", "HeadDatabase"));
      this.isReady = true;
      this.plugin.getItemManager().check();
      DatabaseLoadEvent.getHandlerList().unregister(this);
   }

   @Override
   public ItemType getType() {
      return TYPE;
   }

   @Override
   public boolean isReady() {
      return this.isReady;
   }

   @Override
   public ItemStack getItem(String itemId) throws ItemLoadException {
      if (this.api == null) {
         throw new ItemLoadException(Lang.HEADDATABASE_API_NOT_READY_YET.get());
      } else {
         try {
            return this.api.getItemHead(itemId);
         } catch (NullPointerException exception) {
            throw new ItemLoadException(Lang.INVALID_HEADDATABASE_SKULL.get().replace("%id%", itemId));
         }
      }
   }

   @Override
   public String getItem(ItemStack itemStack) {
      return XMaterial.PLAYER_HEAD != XMaterial.matchXMaterial(itemStack.getType()) ? null : this.api.getItemID(itemStack);
   }
}
