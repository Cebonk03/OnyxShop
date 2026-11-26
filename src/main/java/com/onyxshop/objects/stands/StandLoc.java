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

   public StandLoc(String var1, int var2) throws StandLoadException {
      String[] var3 = var1.split(":");
      if (Bukkit.getWorld(var3[0]) != null) {
         this.world = var3[0];

         try {
            this.x = Integer.parseInt(var3[1]);
            this.y = Double.parseDouble(var3[2]);
            this.z = Integer.parseInt(var3[3]);
         } catch (ArrayIndexOutOfBoundsException | NumberFormatException var5) {
            throw new StandLoadException(
               Lang.INVALID_SHOP_STAND_FORMAT
                  .get()
                  .replace("%pos_x%", String.valueOf(var3[1]))
                  .replace("%pos_y%", String.valueOf(var3[2]))
                  .replace("%pos_z%", String.valueOf(var3[3]))
                  .replace("%id%", String.valueOf(var2))
            );
         }
      } else {
         throw new StandLoadException(Lang.INVALID_SHOP_STAND_WORLD.get().replace("%world%", var3[0]).replace("%id%", String.valueOf(var2)));
      }
   }

   public StandLoc(Block var1) {
      this.world = var1.getLocation().getWorld().getName();
      this.x = var1.getLocation().getBlockX();
      this.y = XMaterial.getBlockY(var1);
      this.z = var1.getLocation().getBlockZ();
   }

   public StandLoc(Location var1) {
      this.world = var1.getWorld().getName();
      this.x = var1.getBlockX();
      this.y = var1.getY();
      this.z = var1.getBlockZ();
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
   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (!(var1 instanceof StandLoc)) {
         return false;
      } else {
         StandLoc var2 = (StandLoc)var1;
         return this.world.equals(var2.world) && this.x == var2.x && this.y == var2.y && this.z == var2.z;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.world, this.x, this.y, this.z);
   }
}
