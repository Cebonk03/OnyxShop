package org.quartz.simpl;

import java.util.Date;
import org.quartz.spi.TimeBroker;

public class SimpleTimeBroker implements TimeBroker {
   public SimpleTimeBroker() {
   }

   @Override
   public Date getCurrentTime() {
      return new Date();
   }

   @Override
   public void initialize() {
   }

   @Override
   public void shutdown() {
   }
}
