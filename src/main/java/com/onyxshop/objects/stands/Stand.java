package com.onyxshop.objects.stands;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.UUID;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.util.ServerInfo;
import com.onyxshop.util.SimpleLocation;
import com.onyxshop.util.XMaterial;
import com.onyxshop.util.exceptions.StandLoadException;
import com.onyxshop.util.exceptions.StandUnloadException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.util.Vector;
import org.json.simple.JSONObject;

public class Stand {
   private final int id;
   private final String item;
   private final SimpleLocation loc;
   private final StandType type;
   private final StandSettings settings;
   private UUID displayItem;
   private UUID stand;
   private boolean loaded;

   public Stand(int var1, SimpleLocation var2, String var3, StandType var4, StandSettings var5) {
      this.loc = var2;
      this.item = var3;
      this.type = var4;
      this.id = var1;
      this.settings = var5;
   }

   public UUID load() throws StandLoadException {
      if (this.loaded) {
         return this.stand;
      } else {
         Location var1 = this.loc.toBukkit();
         if (!this.isStillValid(var1)) {
            throw new StandLoadException(
               Lang.BLOCK_FROM_SHOP_STAND_NOT_FOUND.get().replace("%location%", this.loc.toString()).replace("%type%", this.type.name())
            );
         } else {
            ShopItem var2;
            try {
               var2 = OnyxShop.getInstance().getShopItem(this.item);
               if (var2 == null) {
                  throw new StandLoadException(Lang.ITEM_FROM_SHOP_STAND_NOT_FOUND.get().replace("%item%", this.item));
               }
            } catch (NullPointerException var4) {
               throw new StandLoadException(Lang.ITEM_FROM_SHOP_STAND_NOT_FOUND.get().replace("%item%", this.item));
            }

            this.displayItem = this.getDisplayItem(this.loc.toBukkit());
            if (this.displayItem == null) {
               this.loadDisplayItem(var2, this.loc.toBukkit());
            }

            this.stand = this.getStand(this.loc.toBukkit());
            if (this.stand == null) {
               this.loadStand(var2, this.loc.toBukkit());
            }

            this.loaded = true;
            return this.stand;
         }
      }
   }

   public void unload() throws StandUnloadException {
      if (this.loaded) {
         Location var1 = this.loc.toBukkit();
         if (!this.isStillValid(var1)) {
            throw new StandUnloadException(
               Lang.BLOCK_FROM_SHOP_STAND_NOT_FOUND.get().replace("%location%", this.loc.toString()).replace("%type%", this.type.name())
            );
         } else if (!Bukkit.getWorld(this.loc.world).isChunkLoaded(this.loc.toBukkit().getBlockX() >> 4, this.loc.toBukkit().getBlockZ() >> 4)) {
            throw new StandUnloadException(Lang.SHOP_STAND_INSIDE_UNLOADED_CHUNK.get());
         } else {
            this.unloadDisplayItem();
            this.unloadStand();
            this.loaded = false;
         }
      }
   }

   public boolean isLoaded() {
      return this.loaded;
   }

   public int getId() {
      return this.id;
   }

   public SimpleLocation getLoc() {
      return this.loc;
   }

   public String getItem() {
      return this.item;
   }

   public StandType getType() {
      return this.type;
   }

   public UUID getStand() {
      return this.stand;
   }

   public UUID getDisplayItem() {
      return this.displayItem;
   }

   public StandSettings getSettings() {
      return this.settings;
   }

   public boolean isStillValid(Location var1) {
      Block var2 = var1.getWorld().getBlockAt(var1);
      return this.type.getType() == XMaterial.matchXMaterial(var2);
   }

   private void loadDisplayItem(ShopItem var1, Location var2) {
      var2.add(0.5, 0.5, 0.5);
      Item var3 = var2.getWorld().dropItem(var2, new ItemStack(var1.getShopItem()));
      var3.setPickupDelay(Integer.MAX_VALUE);
      var3.setVelocity(new Vector(0, 0, 0));
      var3.setMetadata("SHOP_STAND", new FixedMetadataValue(OnyxShop.getInstance(), this.id));
      var3.setCustomName(var1.getDisplayname());
      if (this.settings.isHologram() && ConfigManager.getConfig().getBoolean("shop-stands.holograms")) {
         var3.setCustomNameVisible(true);
      }

      if (ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_21_R2)) {
         var3.setUnlimitedLifetime(true);
      } else {
         this.setLived(var3, -32768);
      }

      if (ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_10_R1)) {
         var3.setGravity(false);
      }

      if (ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_13_R1)) {
         var3.setPersistent(false);
      }

      this.displayItem = var3.getUniqueId();
   }

   private void setLived(Item var1, int var2) {
      try {
         Class var3 = !ServerInfo.isNMS()
            ? Class.forName("net.minecraft.server." + ServerInfo.getVersion().name() + ".EntityItem")
            : Class.forName("net.minecraft.world.entity.item.EntityItem");
         Field var4 = var3.getDeclaredField(
            ServerInfo.supportsComponents()
               ? "i"
               : (
                  ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_19_R1)
                     ? "g"
                     : (ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_17_R1) ? "ao" : "age")
               )
         );
         var4.setAccessible(true);
         Class var5 = Class.forName("org.bukkit.craftbukkit." + ServerInfo.getVersion().name() + ".entity.CraftEntity");
         Field var6 = ServerInfo.getVersion().getSimple() >= ServerInfo.Version.v1_20_R1.getSimple()
            ? var5.getDeclaredField("entity")
            : var1.getClass().getDeclaredField("item");
         var6.setAccessible(true);
         Object var7 = var6.get(var1);
         var4.set(var7, var2);
      } catch (ClassNotFoundException | IllegalAccessException | NoSuchFieldException var8) {
         SendMessage.errorMessage("Failed to create display item for shop stand");
         var8.printStackTrace();
      }
   }

   private UUID getDisplayItem(Location var1) {
      var1.add(0.5, 0.5, 0.5);

      for (Entity var4 : var1.getWorld().getNearbyEntities(var1, 1.0, 1.0, 1.0)) {
         if (var4 instanceof Item && this.matchesLoc(var4.getLocation(), var1) && this.matchTag(var4)) {
            return var4.getUniqueId();
         }
      }

      return null;
   }

   private void loadStand(ShopItem var1, Location var2) {
      var2.add(0.5, 0.0, 0.5);
      ArmorStand var3 = (ArmorStand)var2.getWorld().spawnEntity(var2, EntityType.ARMOR_STAND);
      var3.setVisible(false);
      var3.setArms(false);
      var3.setBasePlate(false);
      var3.setMetadata("SHOP_STAND", new FixedMetadataValue(OnyxShop.getInstance(), this.id));
      if (ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_10_R1)) {
         var3.setGravity(false);
      }

      if (ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_13_R1)) {
         var3.setPersistent(false);
      }

      this.stand = var3.getUniqueId();
   }

   private UUID getStand(Location var1) {
      var1.add(0.5, 0.0, 0.5);

      for (Entity var4 : var1.getWorld().getNearbyEntities(var1, 1.0, 1.0, 1.0)) {
         if (var4 instanceof ArmorStand && var4.getLocation().equals(var1) && this.matchTag(var4)) {
            return var4.getUniqueId();
         }
      }

      return null;
   }

   private void unloadDisplayItem() {
      for (Entity var2 : Bukkit.getWorld(this.loc.world).getNearbyEntities(this.loc.toBukkit(), 1.0, 1.0, 1.0)) {
         if (var2.getUniqueId().equals(this.displayItem)) {
            var2.remove();
         }
      }
   }

   private void unloadStand() {
      for (Entity var2 : Bukkit.getWorld(this.loc.world).getNearbyEntities(this.loc.toBukkit(), 1.0, 1.0, 1.0)) {
         if (var2.getUniqueId().equals(this.stand)) {
            var2.remove();
         }
      }
   }

   private boolean matchesLoc(Location var1, Location var2) {
      return var1.getX() == var2.getX() && var1.getY() == var2.getY() && var1.getZ() == var2.getZ();
   }

   private boolean matchTag(Entity var1) {
      if (ServerInfo.getVersion().olderOrEqualAs(ServerInfo.Version.v1_12_R1)) {
         return true;
      } else {
         for (MetadataValue var3 : var1.getMetadata("SHOP_STAND")) {
            if (var3.asInt() == this.id) {
               return true;
            }
         }

         return false;
      }
   }

   public JSONObject serialize() {
      HashMap var1 = new HashMap();
      var1.put("stand_id", this.id);
      var1.put("location", this.loc.getSimple());
      var1.put("item", this.item);
      var1.put("type", this.type.name());
      var1.put("settings", this.settings.toString());
      return new JSONObject(var1);
   }
}
