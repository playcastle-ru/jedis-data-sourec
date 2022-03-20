package pl.memexurer.jedisdatasource.api;

import redis.clients.jedis.Jedis;

public interface JedisDataSourceProvider extends DataSourceProvider<Jedis> {
  @Override
  JedisDataSource getDataSource();
}
