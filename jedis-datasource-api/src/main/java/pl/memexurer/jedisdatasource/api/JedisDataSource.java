package pl.memexurer.jedisdatasource.api;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class JedisDataSource extends BinaryJedisPubSub implements DataSource<Jedis> {
  private final JedisPool pool;

  private final Map<String, JedisPubSubHandler> handlers = new HashMap<>();
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


  public void subscribe(JedisPubSubHandler handler, String... channels) {
    super.subscribe(convertToBytes(channels));
    for(var channel: channels)
      this.handlers.put(channel, handler);
  }

  public void psubscribe(JedisPubSubHandler handler, String... patterns) {
    super.psubscribe(convertToBytes(patterns));
    for(var pattern: patterns)
      this.handlers.put(pattern, handler);
  }

  private static byte[][] convertToBytes(String[] strings) {
    byte[][] data = new byte[strings.length][];
    for (int i = 0; i < strings.length; i++) {
      String string = strings[i];
      data[i] = string.getBytes(Charset.defaultCharset());
    }
    return data;
  }

  @Override
  public void onMessage(byte[] channelRaw, byte[] message) {
    var channel = new String(channelRaw);
    var handler = handlers.get(channel);
    if(handler != null) {
      handler.handle(channel, message);
    }
  }

  @Override
  public void onPMessage(byte[] patternRaw, byte[] channel, byte[] message) {
    var pattern = new String(patternRaw);
    var handler = handlers.get(pattern);
    if(handler != null) {
      handler.handle(pattern, message);
    }
  }


}
