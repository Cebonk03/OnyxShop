package org.quartz.core;

public class NullSampledStatisticsImpl implements SampledStatistics {
   public NullSampledStatisticsImpl() {
   }

   @Override
   public long getJobsCompletedMostRecentSample() {
      return 0L;
   }

   @Override
   public long getJobsExecutingMostRecentSample() {
      return 0L;
   }

   @Override
   public long getJobsScheduledMostRecentSample() {
      return 0L;
   }

   @Override
   public void shutdown() {
   }
}
