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

   public Stand(int value, SimpleLocation simpleLocation, String type, StandType standType, StandSettings standSettings) {
      this.loc = simpleLocation;
      this.item = type;
      this.type = standType;
      this.id = value;
      this.settings = standSettings;
   }

   public UUID load() throws StandLoadException {
      if (this.loaded) {
         return this.stand;
      } else {
         Location location = this.loc.toBukkit();
         if (!this.isStillValid(location)) {
            throw new StandLoadException(
               Lang.BLOCK_FROM_SHOP_STAND_NOT_FOUND.get().replace("%location%", this.loc.toString()).replace("%type%", this.type.name())
            );
         } else {
            ShopItem shopItem;
            try {
               shopItem = OnyxShop.getInstance().getShopItem(this.item);
               if (shopItem == null) {
                  throw new StandLoadException(Lang.ITEM_FROM_SHOP_STAND_NOT_FOUND.get().replace("%item%", this.item));
               }
            } catch (NullPointerException exception) {
               throw new StandLoadException(Lang.ITEM_FROM_SHOP_STAND_NOT_FOUND.get().replace("%item%", this.item));
            }

            this.displayItem = this.getDisplayItem(this.loc.toBukkit());
            if (this.displayItem == null) {
               this.loadDisplayItem(shopItem, this.loc.toBukkit());
            }

            this.stand = this.getStand(this.loc.toBukkit());
            if (this.stand == null) {
               this.loadStand(shopItem, this.loc.toBukkit());
            }

            this.loaded = true;
            return this.stand;
         }
      }
   }

   public void unload() throws StandUnloadException {
      if (this.loaded) {
         Location location = this.loc.toBukkit();
         if (!this.isStillValid(location)) {
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

   public boolean isStillValid(Location location) {
      Block block = location.getWorld().getBlockAt(location);
      return this.type.getType() == XMaterial.matchXMaterial(block);
   }

   private void loadDisplayItem(ShopItem shopItem, Location location) {
      location.add(0.5, 0.5, 0.5);
      Item item = location.getWorld().dropItem(location, new ItemStack(shopItem.getShopItem()));
      item.setPickupDelay(Integer.MAX_VALUE);
      item.setVelocity(new Vector(0, 0, 0));
      item.setMetadata("SHOP_STAND", new FixedMetadataValue(OnyxShop.getInstance(), this.id));
      item.setCustomName(shopItem.getDisplayname());
      if (this.settings.isHologram() && ConfigManager.getConfig().getBoolean("shop-stands.holograms")) {
         item.setCustomNameVisible(true);
      }

      if (ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_21_R2)) {
         item.setUnlimitedLifetime(true);
      } else {
         this.setLived(item, -32768);
      }

      if (ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_10_R1)) {
         item.setGravity(false);
      }

      if (ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_13_R1)) {
         item.setPersistent(false);
      }

      this.displayItem = item.getUniqueId();
   }

   private void setLived(Item item, int value) {
      try {
         Class type = !ServerInfo.isNMS()
            ? Class.forName("net.minecraft.server." + ServerInfo.getVersion().name() + ".EntityItem")
            : Class.forName("net.minecraft.world.entity.item.EntityItem");
         Field field = type.getDeclaredField(
            ServerInfo.supportsComponents()
               ? "i"
               : (
                  ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_19_R1)
                     ? "g"
                     : (ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_17_R1) ? "ao" : "age")
               )
         );
         field.setAccessible(true);
         Class type2 = Class.forName("org.bukkit.craftbukkit." + ServerInfo.getVersion().name() + ".entity.CraftEntity");
         Field field2 = ServerInfo.getVersion().getSimple() >= ServerInfo.Version.v1_20_R1.getSimple()
            ? type2.getDeclaredField("entity")
            : item.getClass().getDeclaredField("item");
         field2.setAccessible(true);
         Object value2 = field2.get(item);
         field.set(value2, value);
      } catch (ClassNotFoundException | IllegalAccessException | NoSuchFieldException exception) {
         SendMessage.errorMessage("Failed to create display item for shop stand");
         exception.printStackTrace();
      }
   }

   private UUID getDisplayItem(Location location) {
      location.add(0.5, 0.5, 0.5);

      for (Entity entity : location.getWorld().getNearbyEntities(location, 1.0, 1.0, 1.0)) {
         if (entity instanceof Item && this.matchesLoc(entity.getLocation(), location) && this.matchTag(entity)) {
            return entity.getUniqueId();
         }
      }

      return null;
   }

   private void loadStand(ShopItem shopItem, Location location) {
      location.add(0.5, 0.0, 0.5);
      ArmorStand armorStand = (ArmorStand)location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
      armorStand.setVisible(false);
      armorStand.setArms(false);
      armorStand.setBasePlate(false);
      armorStand.setMetadata("SHOP_STAND", new FixedMetadataValue(OnyxShop.getInstance(), this.id));
      if (ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_10_R1)) {
         armorStand.setGravity(false);
      }

      if (ServerInfo.getVersion().newerOrEqualAs(ServerInfo.Version.v1_13_R1)) {
         armorStand.setPersistent(false);
      }

      this.stand = armorStand.getUniqueId();
   }

   private UUID getStand(Location location) {
      location.add(0.5, 0.0, 0.5);

      for (Entity entity : location.getWorld().getNearbyEntities(location, 1.0, 1.0, 1.0)) {
         if (entity instanceof ArmorStand && entity.getLocation().equals(location) && this.matchTag(entity)) {
            return entity.getUniqueId();
         }
      }

      return null;
   }

   private void unloadDisplayItem() {
      for (Entity entity : Bukkit.getWorld(this.loc.world).getNearbyEntities(this.loc.toBukkit(), 1.0, 1.0, 1.0)) {
         if (entity.getUniqueId().equals(this.displayItem)) {
            entity.remove();
         }
      }
   }

   private void unloadStand() {
      for (Entity entity : Bukkit.getWorld(this.loc.world).getNearbyEntities(this.loc.toBukkit(), 1.0, 1.0, 1.0)) {
         if (entity.getUniqueId().equals(this.stand)) {
            entity.remove();
         }
      }
   }

   private boolean matchesLoc(Location location, Location location2) {
      return location.getX() == location2.getX() && location.getY() == location2.getY() && location.getZ() == location2.getZ();
   }

   private boolean matchTag(Entity entity) {
      if (ServerInfo.getVersion().olderOrEqualAs(ServerInfo.Version.v1_12_R1)) {
         return true;
      } else {
         for (MetadataValue metadataValue : entity.getMetadata("SHOP_STAND")) {
            if (metadataValue.asInt() == this.id) {
               return true;
            }
         }

         return false;
      }
   }

   public JSONObject serialize() {
      HashMap map = new HashMap();
      map.put("stand_id", this.id);
      map.put("location", this.loc.getSimple());
      map.put("item", this.item);
      map.put("type", this.type.name());
      map.put("settings", this.settings.toString());
      return new JSONObject(map);
   }
}
