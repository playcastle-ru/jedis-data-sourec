package pl.memexurer.jedisdatasource.api;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class JedisDataSource extends BinaryJedisPubSub implements DataSource<Jedis> {
  private final JedisPool pool;

  private final List<JedisPubSubHandler> handlers = new ArrayList<>();
  private boolean isSubscribed = true;

  JedisDataSource(JedisPool pool) {
    this.pool = pool;

    var resource = pool.getResource();

    try {
      var clientField = BinaryJedisPubSub.class.getDeclaredField("client");
      clientField.setAccessible(true);

      clientField.set(this, resource.getConnection());
    } catch (ReflectiveOperationException throwable) {
      throw new RuntimeException(throwable);
    }

    new Thread(() -> {
      try {
        var processMethod = BinaryJedisPubSub.class.getDeclaredMethod("process");
        processMethod.setAccessible(true);

        processMethod.invoke(JedisDataSource.this);
      } catch (ReflectiveOperationException throwable) {
        throw new RuntimeException(throwable);
      }
    }).start();
  }

  @Override
  public boolean isSubscribed() {
    return isSubscribed;
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
    isSubscribed = false;
    this.unsubscribe();
    pool.close();
  }

  public void addHandler(JedisPubSubHandler handler) {
    this.handlers.add(handler);
  }

  @Override
  public void onMessage(byte[] channel, byte[] message) {
    for (var handler : handlers) {
      handler.handle(new String(channel), message);
    }
  }

  @Override
  public void onPMessage(byte[] pattern, byte[] channel, byte[] message) {
    for (var handler : handlers) {
      handler.handle(new String(channel), message);
    }
  }


}
