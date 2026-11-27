package com.onyxshop.objects.stands;

import org.bukkit.Chunk;

public class ChunkLoc {
   public final String world;
   public final int x1;
   public final int z1;

   public ChunkLoc(Chunk chunk) {
      this.world = chunk.getWorld().getName();
      this.x1 = chunk.getX();
      this.z1 = chunk.getZ();
   }

   public boolean contains(StandLoc standLoc) {
      return this.world.equals(standLoc.world) && this.x1 == standLoc.x >> 4 && this.z1 == standLoc.z >> 4;
   }
}
