package com.onyxshop.objects.stands;

import java.util.Objects;
import com.onyxshop.files.Lang;
import com.onyxshop.util.XMaterial;
import com.onyxshop.util.exceptions.StandLoadException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;

public class StandLoc {
   public final String world;
   public final int x;
   public final double y;
   public final int z;

   public StandLoc(String text, int value) throws StandLoadException {
      String[] values = text.split(":");
      if (Bukkit.getWorld(values[0]) != null) {
         this.world = values[0];

         try {
            this.x = Integer.parseInt(values[1]);
            this.y = Double.parseDouble(values[2]);
            this.z = Integer.parseInt(values[3]);
         } catch (ArrayIndexOutOfBoundsException | NumberFormatException exception) {
            throw new StandLoadException(
               Lang.INVALID_SHOP_STAND_FORMAT
                  .get()
                  .replace("%pos_x%", String.valueOf(values[1]))
                  .replace("%pos_y%", String.valueOf(values[2]))
                  .replace("%pos_z%", String.valueOf(values[3]))
                  .replace("%id%", String.valueOf(value))
            );
         }
      } else {
         throw new StandLoadException(Lang.INVALID_SHOP_STAND_WORLD.get().replace("%world%", values[0]).replace("%id%", String.valueOf(value)));
      }
   }

   public StandLoc(Block block) {
      this.world = block.getLocation().getWorld().getName();
      this.x = block.getLocation().getBlockX();
      this.y = XMaterial.getBlockY(block);
      this.z = block.getLocation().getBlockZ();
   }

   public StandLoc(Location location) {
      this.world = location.getWorld().getName();
      this.x = location.getBlockX();
      this.y = location.getY();
      this.z = location.getBlockZ();
   }

   public Location toBukkit() {
      return new Location(Bukkit.getWorld(this.world), this.x, this.y, this.z);
   }

   public String getSimple() {
      return this.world + ":" + this.x + ":" + this.y + ":" + this.z;
   }

   @Override
   public String toString() {
      return Lang.SHOP_STAND_LOCATION_FORMAT
         .get()
         .getLegacy()
         .replace("%world%", this.world)
         .replace("%pos_x%", String.valueOf(this.x))
         .replace("%pos_y%", String.valueOf(this.y))
         .replace("%pos_z%", String.valueOf(this.z));
   }

   @Override
   public boolean equals(Object value) {
      if (this == value) {
         return true;
      } else if (!(value instanceof StandLoc)) {
         return false;
      } else {
         StandLoc standLoc = (StandLoc)value;
         return this.world.equals(standLoc.world) && this.x == standLoc.x && this.y == standLoc.y && this.z == standLoc.z;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.world, this.x, this.y, this.z);
   }
}
