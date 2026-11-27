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
   public boolean contains(Object value) {
      return this.requirements.contains(value);
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
   public <T> T[] toArray(@NotNull T[] values) {
      return null;
   }

   public boolean add(ItemRequirement itemRequirement) {
      return this.requirements.add(itemRequirement);
   }

   @Override
   public boolean remove(Object value) {
      return this.requirements.remove(value);
   }

   @Override
   public boolean containsAll(@NotNull Collection<?> values) {
      return this.requirements.containsAll(values);
   }

   @Override
   public boolean addAll(@NotNull Collection<? extends ItemRequirement> values) {
      return this.requirements.addAll(values);
   }

   @Override
   public boolean removeAll(@NotNull Collection<?> values) {
      return this.requirements.removeAll(values);
   }

   @Override
   public boolean retainAll(@NotNull Collection<?> values) {
      return this.requirements.retainAll(values);
   }

   @Override
   public void clear() {
      this.requirements.clear();
   }

   public ItemRequirement getByType(RequirementType requirementType) {
      for (ItemRequirement itemRequirement : this.requirements) {
         if (itemRequirement.getClass() == requirementType.getClazz()) {
            return itemRequirement;
         }
      }

      return null;
   }

   public boolean hasType(RequirementType requirementType) {
      for (ItemRequirement itemRequirement : this.requirements) {
         if (itemRequirement.getClass() == requirementType.getClazz()) {
            return true;
         }
      }

      return false;
   }

   public boolean removeType(RequirementType requirementType) {
      for (ItemRequirement itemRequirement : this.requirements) {
         if (itemRequirement.getClass() == requirementType.getClazz()) {
            return this.requirements.remove(itemRequirement);
         }
      }

      return false;
   }
}
