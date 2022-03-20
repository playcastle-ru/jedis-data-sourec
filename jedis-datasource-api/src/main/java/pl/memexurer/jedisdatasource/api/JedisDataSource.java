package pl.memexurer.jedisdatasource.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class JedisDataSource extends JedisPubSub implements DataSource<Jedis> {
  private final JedisPool pool;

  private final List<JedisPubSubHandler> handlers = new ArrayList<>();

  JedisDataSource(JedisPool pool) {
    this.pool = pool;
    new Thread(() -> pool.getResource().subscribe(this, "")).start(); //idk how to subscribe without specifying any channels
  }

  @Override
  public CompletableFuture<Jedis> open() {
    return open(ForkJoinPool.commonPool());
  }

  @Override
  public CompletableFuture<Jedis> open(ExecutorService service) {
    CompletableFuture<Jedis> future = new CompletableFuture<>();

    service.execute(() -> {
      try (Jedis resource = pool.getResource()) {
        future.complete(resource);
      } catch (JedisConnectionException exception) {
        future.completeExceptionally(exception);
      }
    });

    return future;
  }

  @Override
  public void close() {
    pool.close();
  }

  public void addHandler(JedisPubSubHandler handler) {
    this.handlers.add(handler);
  }

  @Override
  public void onMessage(String channel, String message) {
    for (var handler : handlers) {
      handler.handle(channel, message);
    }
  }

  @Override
  public void onPMessage(String pattern, String channel, String message) {
    for (var handler : handlers) {
      handler.handle(channel, message);
    }
  }
}
