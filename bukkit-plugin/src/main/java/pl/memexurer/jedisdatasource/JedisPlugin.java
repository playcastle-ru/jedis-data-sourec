package pl.memexurer.jedisdatasource;

import org.bukkit.plugin.java.JavaPlugin;
import pl.memexurer.jedisdatasource.api.JedisDataSource;
import pl.memexurer.jedisdatasource.api.JedisDataSourceConfiguration;
import pl.memexurer.jedisdatasource.api.JedisDataSourceProvider;

public class JedisPlugin extends JavaPlugin implements JedisDataSourceProvider {

  private JedisDataSource dataSource;

  @Override
  public void onDisable() {
    this.dataSource.close();
  }

  @Override
  public void onEnable() {
    this.dataSource = new JedisDataSourceConfiguration("localhost", (short) 6379, "", 8, false, 0)
        .create();
  }

  @Override
  public JedisDataSource getDataSource() {
    return dataSource;
  }
}
