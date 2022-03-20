package pl.memexurer.jedisdatasource.api;

import java.util.concurrent.Executors;
import java.util.function.Consumer;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class JedisDataSourceConfiguration {
  private final String address;
  private final short port;
  private final String password;
  private final int maxConnections;
  private final boolean useSSL;
  private final int timeout;

  public JedisDataSourceConfiguration(String address, short port, String password, int maxConnections,
      boolean useSSL,
      int timeout) {
    this.address = address;
    this.port = port;
    this.password = password != null && password.isEmpty() ? null : password;
    this.maxConnections = maxConnections;
    this.useSSL = useSSL;
    this.timeout = timeout;
  }

  public JedisDataSource create() {
    JedisPoolConfig config = new JedisPoolConfig();

    config.setMaxTotal(maxConnections);

    JedisPool pool = new JedisPool(config, address, port, timeout, password, useSSL);
    return new JedisDataSource(pool);
  }
}
