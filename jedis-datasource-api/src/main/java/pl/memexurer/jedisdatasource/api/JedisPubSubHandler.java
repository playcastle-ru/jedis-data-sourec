package pl.memexurer.jedisdatasource.api;

public interface JedisPubSubHandler {
  void handle(String channel, String message);
}
