package org.quartz.utils;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class DirtyFlagMap<K, V> implements Map<K, V>, Cloneable, Serializable {
   private static final long serialVersionUID = 1433884852607126222L;
   private boolean dirty = false;
   private Map<K, V> map;

   public DirtyFlagMap() {
      this.map = new HashMap<>();
   }

   public DirtyFlagMap(int var1) {
      this.map = new HashMap<>(var1);
   }

   public DirtyFlagMap(int var1, float var2) {
      this.map = new HashMap<>(var1, var2);
   }

   public void clearDirtyFlag() {
      this.dirty = false;
   }

   public boolean isDirty() {
      return this.dirty;
   }

   public Map<K, V> getWrappedMap() {
      return this.map;
   }

   @Override
   public void clear() {
      if (!this.map.isEmpty()) {
         this.dirty = true;
      }

      this.map.clear();
   }

   @Override
   public boolean containsKey(Object var1) {
      return this.map.containsKey(var1);
   }

   @Override
   public boolean containsValue(Object var1) {
      return this.map.containsValue(var1);
   }

   @Override
   public Set<Entry<K, V>> entrySet() {
      return new DirtyFlagMap.DirtyFlagMapEntrySet(this.map.entrySet());
   }

   @Override
   public boolean equals(Object var1) {
      return var1 != null && var1 instanceof DirtyFlagMap ? this.map.equals(((DirtyFlagMap)var1).getWrappedMap()) : false;
   }

   @Override
   public int hashCode() {
      return this.map.hashCode();
   }

   @Override
   public V get(Object var1) {
      return this.map.get(var1);
   }

   @Override
   public boolean isEmpty() {
      return this.map.isEmpty();
   }

   @Override
   public Set<K> keySet() {
      return new DirtyFlagSet<K>(this.map.keySet());
   }

   @Override
   public V put(K var1, V var2) {
      this.dirty = true;
      return this.map.put((K)var1, (V)var2);
   }

   @Override
   public void putAll(Map<? extends K, ? extends V> var1) {
      if (!var1.isEmpty()) {
         this.dirty = true;
      }

      this.map.putAll(var1);
   }

   @Override
   public V remove(Object var1) {
      Object var2 = this.map.remove(var1);
      if (var2 != null) {
         this.dirty = true;
      }

      return (V)var2;
   }

   @Override
   public int size() {
      return this.map.size();
   }

   @Override
   public Collection<V> values() {
      return new DirtyFlagCollection<V>(this.map.values());
   }

   @Override
   public Object clone() {
      try {
         DirtyFlagMap var1 = (DirtyFlagMap)super.clone();
         if (this.map instanceof HashMap) {
            var1.map = (Map<K, V>)((HashMap)this.map).clone();
         }

         return var1;
      } catch (CloneNotSupportedException var3) {
         throw new IncompatibleClassChangeError("Not Cloneable.");
      }
   }

   private class DirtyFlagCollection<T> implements Collection<T> {
      private Collection<T> collection;

      public DirtyFlagCollection(Collection<T> var2) {
         this.collection = var2;
      }

      protected Collection<T> getWrappedCollection() {
         return this.collection;
      }

      @Override
      public Iterator<T> iterator() {
         return DirtyFlagMap.this.new DirtyFlagIterator<>(this.collection.iterator());
      }

      @Override
      public boolean remove(Object var1) {
         boolean var2 = this.collection.remove(var1);
         if (var2) {
            DirtyFlagMap.this.dirty = true;
         }

         return var2;
      }

      @Override
      public boolean removeAll(Collection<?> var1) {
         boolean var2 = this.collection.removeAll(var1);
         if (var2) {
            DirtyFlagMap.this.dirty = true;
         }

         return var2;
      }

      @Override
      public boolean retainAll(Collection<?> var1) {
         boolean var2 = this.collection.retainAll(var1);
         if (var2) {
            DirtyFlagMap.this.dirty = true;
         }

         return var2;
      }

      @Override
      public void clear() {
         if (!this.collection.isEmpty()) {
            DirtyFlagMap.this.dirty = true;
         }

         this.collection.clear();
      }

      @Override
      public int size() {
         return this.collection.size();
      }

      @Override
      public boolean isEmpty() {
         return this.collection.isEmpty();
      }

      @Override
      public boolean contains(Object var1) {
         return this.collection.contains(var1);
      }

      @Override
      public boolean add(T var1) {
         return this.collection.add((T)var1);
      }

      @Override
      public boolean addAll(Collection<? extends T> var1) {
         return this.collection.addAll(var1);
      }

      @Override
      public boolean containsAll(Collection<?> var1) {
         return this.collection.containsAll(var1);
      }

      @Override
      public Object[] toArray() {
         return this.collection.toArray();
      }

      @Override
      public <U> U[] toArray(U[] var1) {
         return (U[])this.collection.toArray(var1);
      }
   }

   private class DirtyFlagIterator<T> implements Iterator<T> {
      private Iterator<T> iterator;

      public DirtyFlagIterator(Iterator<T> var2) {
         this.iterator = var2;
      }

      @Override
      public void remove() {
         DirtyFlagMap.this.dirty = true;
         this.iterator.remove();
      }

      @Override
      public boolean hasNext() {
         return this.iterator.hasNext();
      }

      @Override
      public T next() {
         return this.iterator.next();
      }
   }

   private class DirtyFlagMapEntry implements Entry<K, V> {
      private Entry<K, V> entry;

      public DirtyFlagMapEntry(Entry<K, V> var2) {
         this.entry = var2;
      }

      @Override
      public V setValue(V var1) {
         DirtyFlagMap.this.dirty = true;
         return this.entry.setValue((V)var1);
      }

      @Override
      public K getKey() {
         return this.entry.getKey();
      }

      @Override
      public V getValue() {
         return this.entry.getValue();
      }

      @Override
      public boolean equals(Object var1) {
         return this.entry.equals(var1);
      }
   }

   private class DirtyFlagMapEntryIterator extends DirtyFlagMap<K, V>.DirtyFlagIterator<Entry<K, V>> {
      public DirtyFlagMapEntryIterator(Iterator<Entry<K, V>> var2) {
         super(var2);
      }

      public DirtyFlagMap<K, V>.DirtyFlagMapEntry next() {
         return DirtyFlagMap.this.new DirtyFlagMapEntry((Entry<K, V>)super.next());
      }
   }

   private class DirtyFlagMapEntrySet extends DirtyFlagMap<K, V>.DirtyFlagSet<Entry<K, V>> {
      public DirtyFlagMapEntrySet(Set<Entry<K, V>> var2) {
         super(var2);
      }

      @Override
      public Iterator<Entry<K, V>> iterator() {
         return DirtyFlagMap.this.new DirtyFlagMapEntryIterator(this.getWrappedSet().iterator());
      }

      @Override
      public Object[] toArray() {
         return this.toArray(new Object[super.size()]);
      }

      @Override
      public <U> U[] toArray(U[] var1) {
         if (!var1.getClass().getComponentType().isAssignableFrom(Entry.class)) {
            throw new IllegalArgumentException("Array must be of type assignable from Map.Entry");
         } else {
            int var2 = super.size();
            Object[] var3 = var1.length < var2 ? (Object[])Array.newInstance(var1.getClass().getComponentType(), var2) : var1;
            Iterator var4 = this.iterator();

            for (int var5 = 0; var5 < var2; var5++) {
               var3[var5] = var4.next();
            }

            if (var3.length > var2) {
               var3[var2] = null;
            }

            return (U[])var3;
         }
      }
   }

   private class DirtyFlagSet<T> extends DirtyFlagMap<K, V>.DirtyFlagCollection<T> implements Set<T> {
      public DirtyFlagSet(Set<T> var2) {
         super(var2);
      }

      protected Set<T> getWrappedSet() {
         return (Set<T>)this.getWrappedCollection();
      }
   }
}
