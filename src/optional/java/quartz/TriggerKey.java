package org.quartz;

import org.quartz.utils.Key;

public final class TriggerKey extends Key<TriggerKey> {
   private static final long serialVersionUID = 8070357886703449660L;

   public TriggerKey(String var1) {
      super(var1, null);
   }

   public TriggerKey(String var1, String var2) {
      super(var1, var2);
   }

   public static TriggerKey triggerKey(String var0) {
      return new TriggerKey(var0, null);
   }

   public static TriggerKey triggerKey(String var0, String var1) {
      return new TriggerKey(var0, var1);
   }
}
