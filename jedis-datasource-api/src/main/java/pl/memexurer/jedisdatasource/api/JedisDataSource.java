package pl.memexurer.jedisdatasource.api;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class JedisDataSource extends JedisPubSub implements DataSource<Jedis> {
  private final JedisPool pool;

  private final List<JedisPubSubHandler> handlers = new ArrayList<>();

  JedisDataSource(JedisPool pool) {
    this.pool = pool;

    var resource = pool.getResource();

    try {
      var clientField = JedisPubSub.class.getDeclaredField("client");
      clientField.setAccessible(true);

      clientField.set(this, resource.getConnection());
    } catch (ReflectiveOperationException throwable) {
      throw new RuntimeException(throwable);
    }

    new Thread(() -> {
      try {
        var processMethod = JedisPubSub.class.getDeclaredMethod("process");
        processMethod.setAccessible(true);

        processMethod.invoke(JedisDataSource.this);
      } catch (ReflectiveOperationException throwable) {
        throw new RuntimeException(throwable);
      }
    }).start();
  }

  @Override
  public boolean isSubscribed() {
    return true;
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
