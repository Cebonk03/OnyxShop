package com.onyxshop.providers.requirements;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import org.jetbrains.annotations.NotNull;

public class Requirements implements Collection<ItemRequirement> {
   private final Collection<ItemRequirement> requirements = new ArrayList<>();

   public Requirements() {
   }

   @Override
   public int size() {
      return this.requirements.size();
   }

   @Override
   public boolean isEmpty() {
      return this.requirements.isEmpty();
   }

   @Override
   public boolean contains(Object var1) {
      return this.requirements.contains(var1);
   }

   @NotNull
   @Override
   public Iterator<ItemRequirement> iterator() {
      return this.requirements.iterator();
   }

   @NotNull
   @Override
   public Object[] toArray() {
      return this.requirements.toArray();
   }

   @NotNull
   @Override
   public <T> T[] toArray(@NotNull T[] var1) {
      return null;
   }

   public boolean add(ItemRequirement var1) {
      return this.requirements.add(var1);
   }

   @Override
   public boolean remove(Object var1) {
      return this.requirements.remove(var1);
   }

   @Override
   public boolean containsAll(@NotNull Collection<?> var1) {
      return this.requirements.containsAll(var1);
   }

   @Override
   public boolean addAll(@NotNull Collection<? extends ItemRequirement> var1) {
      return this.requirements.addAll(var1);
   }

   @Override
   public boolean removeAll(@NotNull Collection<?> var1) {
      return this.requirements.removeAll(var1);
   }

   @Override
   public boolean retainAll(@NotNull Collection<?> var1) {
      return this.requirements.retainAll(var1);
   }

   @Override
   public void clear() {
      this.requirements.clear();
   }

   public ItemRequirement getByType(RequirementType var1) {
      for (ItemRequirement var3 : this.requirements) {
         if (var3.getClass() == var1.getClazz()) {
            return var3;
         }
      }

      return null;
   }

   public boolean hasType(RequirementType var1) {
      for (ItemRequirement var3 : this.requirements) {
         if (var3.getClass() == var1.getClazz()) {
            return true;
         }
      }

      return false;
   }

   public boolean removeType(RequirementType var1) {
      for (ItemRequirement var3 : this.requirements) {
         if (var3.getClass() == var1.getClazz()) {
            return this.requirements.remove(var3);
         }
      }

      return false;
   }
}
