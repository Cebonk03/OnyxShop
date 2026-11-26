package org.terracotta.quartz.collections;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.search.QueryBuilder;
import org.terracotta.toolkit.search.attribute.ToolkitAttributeExtractor;
import org.terracotta.toolkit.store.ToolkitStore;

public class SerializedToolkitStore<K, V extends Serializable> implements ToolkitStore<K, V> {
   private final ToolkitStore<String, V> toolkitStore;

   public SerializedToolkitStore(ToolkitStore var1) {
      this.toolkitStore = var1;
   }

   public int size() {
      return this.toolkitStore.size();
   }

   public boolean isEmpty() {
      return this.toolkitStore.isEmpty();
   }

   private static String serializeToString(Object var0) {
      try {
         return SerializationHelper.serializeToString(var0);
      } catch (IOException var2) {
         throw new RuntimeException(var2);
      }
   }

   private static Object deserializeFromString(String var0) {
      try {
         return SerializationHelper.deserializeFromString(var0);
      } catch (IOException var2) {
         throw new RuntimeException(var2);
      } catch (ClassNotFoundException var3) {
         throw new RuntimeException(var3);
      }
   }

   public boolean containsKey(Object var1) {
      return this.toolkitStore.containsKey(serializeToString(var1));
   }

   public V get(Object var1) {
      return (V)this.toolkitStore.get(serializeToString(var1));
   }

   public V put(K var1, V var2) {
      return (V)this.toolkitStore.put(serializeToString(var1), var2);
   }

   public V remove(Object var1) {
      return (V)this.toolkitStore.remove(serializeToString(var1));
   }

   public void putAll(Map<? extends K, ? extends V> var1) {
      HashMap var2 = new HashMap();

      for (Entry var4 : var1.entrySet()) {
         var2.put(serializeToString(var4.getKey()), var4.getValue());
      }

      this.toolkitStore.putAll(var2);
   }

   public void clear() {
      this.toolkitStore.clear();
   }

   public Set<K> keySet() {
      return new SerializedToolkitStore.ToolkitKeySet<>(this.toolkitStore.keySet());
   }

   public boolean isDestroyed() {
      return this.toolkitStore.isDestroyed();
   }

   public void destroy() {
      this.toolkitStore.destroy();
   }

   public String getName() {
      return this.toolkitStore.getName();
   }

   public ToolkitReadWriteLock createLockForKey(K var1) {
      return this.toolkitStore.createLockForKey(serializeToString(var1));
   }

   public void removeNoReturn(Object var1) {
      this.toolkitStore.removeNoReturn(serializeToString(var1));
   }

   public void putNoReturn(K var1, V var2) {
      this.toolkitStore.putNoReturn(serializeToString(var1), var2);
   }

   public Map<K, V> getAll(Collection<? extends K> var1) {
      HashSet var2 = new HashSet();

      for (Object var4 : var1) {
         var2.add(serializeToString(var4));
      }

      Map var7 = this.toolkitStore.getAll(var2);
      Object var8 = var7.isEmpty() ? Collections.EMPTY_MAP : new HashMap();

      for (Entry var6 : var7.entrySet()) {
         var8.put(deserializeFromString((String)var6.getKey()), var6.getValue());
      }

      return (Map<K, V>)var8;
   }

   public Configuration getConfiguration() {
      return this.toolkitStore.getConfiguration();
   }

   public void setConfigField(String var1, Serializable var2) {
      this.toolkitStore.setConfigField(var1, var2);
   }

   public boolean containsValue(Object var1) {
      return this.toolkitStore.containsValue(var1);
   }

   public V putIfAbsent(K var1, V var2) {
      return (V)this.toolkitStore.putIfAbsent(serializeToString(var1), var2);
   }

   public Set<Entry<K, V>> entrySet() {
      return new SerializedToolkitStore.ToolkitEntrySet<>(this.toolkitStore.entrySet());
   }

   public Collection<V> values() {
      return this.toolkitStore.values();
   }

   public boolean remove(Object var1, Object var2) {
      return this.toolkitStore.remove(serializeToString(var1), var2);
   }

   public boolean replace(K var1, V var2, V var3) {
      return this.toolkitStore.replace(serializeToString(var1), var2, var3);
   }

   public V replace(K var1, V var2) {
      return (V)this.toolkitStore.replace(serializeToString(var1), var2);
   }

   public boolean isBulkLoadEnabled() {
      return this.toolkitStore.isBulkLoadEnabled();
   }

   public boolean isNodeBulkLoadEnabled() {
      return this.toolkitStore.isNodeBulkLoadEnabled();
   }

   public void setNodeBulkLoadEnabled(boolean var1) {
      this.toolkitStore.setNodeBulkLoadEnabled(var1);
   }

   public void waitUntilBulkLoadComplete() {
      this.toolkitStore.waitUntilBulkLoadComplete();
   }

   public void setAttributeExtractor(ToolkitAttributeExtractor var1) {
      this.toolkitStore.setAttributeExtractor(var1);
   }

   public QueryBuilder createQueryBuilder() {
      throw new UnsupportedOperationException();
   }

   private static class ToolkitEntryIterator<K, V> implements Iterator<Entry<K, V>> {
      private final Iterator<Entry<String, V>> iter;

      public ToolkitEntryIterator(Iterator<Entry<String, V>> var1) {
         this.iter = var1;
      }

      @Override
      public boolean hasNext() {
         return this.iter.hasNext();
      }

      public Entry<K, V> next() {
         Entry var1 = this.iter.next();
         return var1 == null
            ? null
            : new SerializedToolkitStore.ToolkitMapEntry<>((K)SerializedToolkitStore.deserializeFromString((String)var1.getKey()), (V)var1.getValue());
      }

      @Override
      public void remove() {
         this.iter.remove();
      }
   }

   private static class ToolkitEntrySet<K, V> implements Set<Entry<K, V>> {
      private final Set<Entry<String, V>> set;

      public ToolkitEntrySet(Set<Entry<String, V>> var1) {
         this.set = var1;
      }

      @Override
      public int size() {
         return this.set.size();
      }

      @Override
      public boolean isEmpty() {
         return this.set.isEmpty();
      }

      @Override
      public boolean contains(Object var1) {
         if (!(var1 instanceof Entry)) {
            return false;
         } else {
            Entry var2 = (Entry)var1;
            Object var3 = null;
            var3 = new SerializedToolkitStore.ToolkitMapEntry<>(SerializedToolkitStore.serializeToString(var2.getKey()), var2.getValue());
            return this.set.contains(var3);
         }
      }

      @Override
      public Iterator<Entry<K, V>> iterator() {
         return new SerializedToolkitStore.ToolkitEntryIterator<>(this.set.iterator());
      }

      @Override
      public Object[] toArray() {
         throw new UnsupportedOperationException();
      }

      @Override
      public <T> T[] toArray(T[] var1) {
         throw new UnsupportedOperationException();
      }

      public boolean add(Entry<K, V> var1) {
         throw new UnsupportedOperationException();
      }

      @Override
      public boolean remove(Object var1) {
         throw new UnsupportedOperationException();
      }

      @Override
      public boolean containsAll(Collection<?> var1) {
         throw new UnsupportedOperationException();
      }

      @Override
      public boolean addAll(Collection<? extends Entry<K, V>> var1) {
         throw new UnsupportedOperationException();
      }

      @Override
      public boolean retainAll(Collection<?> var1) {
         throw new UnsupportedOperationException();
      }

      @Override
      public boolean removeAll(Collection<?> var1) {
         throw new UnsupportedOperationException();
      }

      @Override
      public void clear() {
         throw new UnsupportedOperationException();
      }
   }

   private static class ToolkitKeyIterator<K> implements Iterator<K> {
      private final Iterator<String> iter;

      public ToolkitKeyIterator(Iterator<String> var1) {
         this.iter = var1;
      }

      @Override
      public boolean hasNext() {
         return this.iter.hasNext();
      }

      @Override
      public K next() {
         String var1 = this.iter.next();
         return (K)(var1 == null ? null : SerializedToolkitStore.deserializeFromString(var1));
      }

      @Override
      public void remove() {
         this.iter.remove();
      }
   }

   private static class ToolkitKeySet<K> implements Set<K> {
      private final Set<String> set;

      public ToolkitKeySet(Set<String> var1) {
         this.set = var1;
      }

      @Override
      public int size() {
         return this.set.size();
      }

      @Override
      public boolean isEmpty() {
         return this.set.isEmpty();
      }

      @Override
      public boolean contains(Object var1) {
         return this.set.contains(SerializedToolkitStore.serializeToString(var1));
      }

      @Override
      public Iterator<K> iterator() {
         return new SerializedToolkitStore.ToolkitKeyIterator<>(this.set.iterator());
      }

      @Override
      public Object[] toArray() {
         throw new UnsupportedOperationException();
      }

      @Override
      public <T> T[] toArray(T[] var1) {
         throw new UnsupportedOperationException();
      }

      @Override
      public boolean add(K var1) {
         throw new UnsupportedOperationException();
      }

      @Override
      public boolean remove(Object var1) {
         throw new UnsupportedOperationException();
      }

      @Override
      public boolean containsAll(Collection<?> var1) {
         throw new UnsupportedOperationException();
      }

      @Override
      public boolean addAll(Collection<? extends K> var1) {
         throw new UnsupportedOperationException();
      }

      @Override
      public boolean retainAll(Collection<?> var1) {
         throw new UnsupportedOperationException();
      }

      @Override
      public boolean removeAll(Collection<?> var1) {
         throw new UnsupportedOperationException();
      }

      @Override
      public void clear() {
         throw new UnsupportedOperationException();
      }
   }

   private static class ToolkitMapEntry<K, V> implements Entry<K, V> {
      private final K k;
      private final V v;

      public ToolkitMapEntry(K var1, V var2) {
         this.k = (K)var1;
         this.v = (V)var2;
      }

      @Override
      public K getKey() {
         return this.k;
      }

      @Override
      public V getValue() {
         return this.v;
      }

      @Override
      public V setValue(V var1) {
         throw new UnsupportedOperationException();
      }
   }
}
