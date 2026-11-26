package com.onyxshop.providers.items;

import java.util.Optional;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.ItemProvider;
import com.onyxshop.util.ItemProviderType;
import com.onyxshop.util.ItemType;
import com.onyxshop.util.exceptions.ItemLoadException;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.bukkit.api.event.CraftEngineReloadEvent;
import net.momirealms.craftengine.bukkit.item.BukkitItemDefinition;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class CraftEngineHook implements ItemProvider, Listener {
   private static final ItemType TYPE = new ItemType(ItemProviderType.CRAFT_ENGINE);
   private final OnyxShop plugin;
   private boolean isReady = false;

   public CraftEngineHook(OnyxShop var1) {
      this.plugin = var1;
      var1.getServer().getPluginManager().registerEvents(this, var1);
   }

   @EventHandler
   public void onItemsLoad(CraftEngineReloadEvent var1) {
      SendMessage.infoMessage(Lang.ENABLED_PLUGIN_HOOK.get().replace("%plugin%", this.getType().getName()));
      this.isReady = true;
      this.plugin.getItemManager().check();
      CraftEngineReloadEvent.getHandlerList().unregister(this);
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
      if (!this.isReady) {
         return null;
      } else {
         try {
            BukkitItemDefinition var2 = CraftEngineItems.byId(Key.of(var1));
            if (var2 == null) {
               throw new ItemLoadException(Lang.UNKNOWN_THIRD_PARTY_ITEM.get().replace("%id%", var1).replace("%plugin%", "CraftEngine"));
            } else {
               return var2.buildBukkitItem();
            }
         } catch (Exception var4) {
            String var3;
            if (!(var4 instanceof ItemLoadException)) {
               var3 = Lang.ERROR_CREATING_THIRD_PARTY_ITEM.get().getLegacy().replace("%id%", var1).replace("%plugin%", "CraftEngine");
               var4.printStackTrace();
            } else {
               var3 = var4.getMessage();
            }

            throw new ItemLoadException("Failed to load CraftEngine item with reason: " + var3);
         }
      }
   }

   @Override
   public String getItem(ItemStack var1) {
      return Optional.ofNullable(CraftEngineItems.getCustomItemId(var1)).<String>map(Key::asString).orElse(null);
   }

   public static ItemStack tag(ItemStack var0, Player var1) {
      return CraftEngineItems.byItemStack(var0).buildBukkitItem(ItemBuildContext.of(BukkitAdaptor.adapt(var1)), var0.getAmount());
   }
}
