package com.onyxshop.providers.items;

import io.lumine.mythic.api.MythicProvider;
import io.lumine.mythic.api.adapters.AbstractPlayer;
import io.lumine.mythic.api.skills.SkillCaster;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.core.drops.DropMetadataImpl;
import io.lumine.mythic.core.items.MythicItem;
import java.util.Optional;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.ItemProvider;
import com.onyxshop.util.ItemProviderType;
import com.onyxshop.util.ItemType;
import com.onyxshop.util.exceptions.ItemLoadException;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class MythicMobsHook implements ItemProvider {
   private static final ItemType TYPE = new ItemType(ItemProviderType.MYTHIC_MOBS);
   private final OnyxShop plugin;
   private boolean isReady = false;

   public MythicMobsHook(OnyxShop var1) {
      this.plugin = var1;
      if (var1.getServer().getPluginManager().isPluginEnabled(this.getType().getName())) {
         this.enable();
      } else {
         var1.runTaskLater(() -> {
            if (!var1.getServer().getPluginManager().isPluginEnabled(this.getType().getName())) {
               SendMessage.errorMessage(Lang.FAILED_PLUGIN_INTEGRATION.get().replace("%plugin%", this.getType().getName()));
            } else {
               this.enable();
            }
         }, 20L);
      }
   }

   private void enable() {
      SendMessage.infoMessage(Lang.ENABLED_PLUGIN_HOOK.get().replace("%plugin%", this.getType().getName()));
      this.isReady = true;
      this.plugin.getItemManager().check();
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
      Optional var2 = MythicProvider.get().getItemManager().getItem(var1);
      if (!var2.isPresent()) {
         throw new ItemLoadException(Lang.UNKNOWN_THIRD_PARTY_ITEM.get().replace("%id%", var1).replace("%plugin%", this.getType().getName()));
      } else {
         return BukkitAdapter.adapt(((MythicItem)var2.get()).generateItemStack(1));
      }
   }

   @Override
   public String getItem(ItemStack var1) {
      return MythicProvider.get().getItemManager().getMythicTypeFromItem(var1);
   }

   public static ItemStack tag(ItemStack var0, Player var1) {
      String var2 = MythicProvider.get().getItemManager().getMythicTypeFromItem(var0);
      Optional var3 = MythicProvider.get().getItemManager().getItem(var2);
      if (!var3.isPresent()) {
         throw new NullPointerException(
            Lang.UNKNOWN_THIRD_PARTY_ITEM.get().getLegacy().replace("%id%", var2).replace("%plugin%", ItemProviderType.MYTHIC_MOBS.getName())
         );
      } else {
         AbstractPlayer var4 = BukkitAdapter.adapt(var1);
         SkillCaster var5 = MythicProvider.get().getSkillManager().getCaster(var4);
         DropMetadataImpl var6 = new DropMetadataImpl(var5, var4);
         return BukkitAdapter.adapt(((MythicItem)var3.get()).generateItemStack(var6, var0.getAmount()));
      }
   }
}
