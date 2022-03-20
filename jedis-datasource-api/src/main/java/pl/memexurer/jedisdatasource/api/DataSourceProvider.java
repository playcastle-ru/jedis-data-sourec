package pl.memexurer.jedisdatasource.api;

public interface DataSourceProvider<T> {
  DataSource<T> getDataSource();
}
