package org.terracotta.quartz;

import org.terracotta.toolkit.internal.ToolkitInternal;

public class TerracottaJobStore extends AbstractTerracottaJobStore {
   public TerracottaJobStore() {
   }

   @Override
   TerracottaJobStoreExtensions getRealStore(ToolkitInternal var1) {
      return new PlainTerracottaJobStore(var1);
   }
}
