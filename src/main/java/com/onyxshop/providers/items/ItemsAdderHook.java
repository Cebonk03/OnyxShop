package com.onyxshop.providers.items;

import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.Events.ItemsAdderFirstLoadEvent;
import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent.Cause;
import java.lang.reflect.Field;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.ItemProvider;
import com.onyxshop.util.ItemProviderType;
import com.onyxshop.util.ItemType;
import com.onyxshop.util.exceptions.ItemLoadException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class ItemsAdderHook implements ItemProvider, Listener {
   private static final ItemType TYPE = new ItemType(ItemProviderType.ITEMS_ADDER);
   private final OnyxShop plugin;
   private boolean isReady = false;

   public ItemsAdderHook(OnyxShop var1) {
      this.plugin = var1;
      var1.getServer().getPluginManager().registerEvents(this, var1);
   }

   @EventHandler
   public void onIALoad(ItemsAdderFirstLoadEvent var1) {
      SendMessage.infoMessage(Lang.ENABLED_PLUGIN_HOOK.get().replace("%plugin%", "ItemsAdder"));
      this.isReady = true;
      this.plugin.getItemManager().check();
      ItemsAdderFirstLoadEvent.getHandlerList().unregister(this);
   }

   @Override
   public ItemType getType() {
      return TYPE;
   }

   @Override
   public boolean isReady() {
      return this.isReady;
   }

   private Cause getCause(ItemsAdderLoadDataEvent var1) {
      try {
         Field var2 = var1.getClass().getDeclaredField("cause");
         var2.setAccessible(true);
         return (Cause)var2.get(var1);
      } catch (SecurityException | IllegalAccessException | NoSuchFieldException var3) {
         SendMessage.warnMessage("Failed to subscribe ItemsAdder load event");
         var3.printStackTrace();
         return Cause.FIRST_LOAD;
      }
   }

   @Override
   public ItemStack getItem(String var1) throws ItemLoadException {
      CustomStack var2 = CustomStack.getInstance(var1);
      if (var2 == null) {
         throw new ItemLoadException(Lang.INVALID_ITEMSADDER_ITEM.get().replace("%id%", var1));
      } else {
         return var2.getItemStack().clone();
      }
   }

   @Override
   public String getItem(ItemStack var1) {
      CustomStack var2 = CustomStack.byItemStack(var1);
      return var2 == null ? null : var2.getNamespacedID();
   }

   @Override
   public String getID(String var1) {
      return var1.contains(":") ? var1.split(":")[1] : var1;
   }
}
