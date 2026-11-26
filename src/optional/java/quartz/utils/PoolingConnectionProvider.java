package org.quartz.utils;

import javax.sql.DataSource;

public interface PoolingConnectionProvider extends ConnectionProvider {
   String POOLING_PROVIDER = "provider";
   String POOLING_PROVIDER_C3P0 = "c3p0";
   String POOLING_PROVIDER_HIKARICP = "hikaricp";
   String DB_DRIVER = "driver";
   String DB_URL = "URL";
   String DB_USER = "user";
   String DB_PASSWORD = "password";
   String DB_MAX_CONNECTIONS = "maxConnections";
   String DB_VALIDATION_QUERY = "validationQuery";
   int DEFAULT_DB_MAX_CONNECTIONS = 10;

   DataSource getDataSource();
}
