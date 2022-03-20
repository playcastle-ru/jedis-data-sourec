package pl.memexurer.jedisdatasource.api;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public interface DataSource<T> extends Closeable {
  CompletableFuture<T> open();

  CompletableFuture<T> open(ExecutorService service);
}
