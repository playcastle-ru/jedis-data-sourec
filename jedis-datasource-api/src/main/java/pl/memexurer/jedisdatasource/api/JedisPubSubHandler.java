package pl.memexurer.jedisdatasource.api;

public interface JedisPubSubHandler {
  default void handle(String channel, byte[] message) {
    handle(channel, new String(message));
  }

  default void handle(String channel, String message) {
    throw new UnsupportedOperationException();
  }
}
