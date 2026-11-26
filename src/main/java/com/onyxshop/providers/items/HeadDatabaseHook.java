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

   public HeadDatabaseHook(OnyxShop var1) throws ItemProviderLoadException {
      try {
         var1.getServer().getPluginManager().registerEvents(this, var1);
         this.plugin = var1;
      } catch (IllegalStateException var3) {
         throw new ItemProviderLoadException("Cannot register head database load event");
      }
   }

   @EventHandler
   public void onDatabaseLoad(DatabaseLoadEvent var1) {
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
   public ItemStack getItem(String var1) throws ItemLoadException {
      if (this.api == null) {
         throw new ItemLoadException(Lang.HEADDATABASE_API_NOT_READY_YET.get());
      } else {
         try {
            return this.api.getItemHead(var1);
         } catch (NullPointerException var3) {
            throw new ItemLoadException(Lang.INVALID_HEADDATABASE_SKULL.get().replace("%id%", var1));
         }
      }
   }

   @Override
   public String getItem(ItemStack var1) {
      return XMaterial.PLAYER_HEAD != XMaterial.matchXMaterial(var1.getType()) ? null : this.api.getItemID(var1);
   }
}
