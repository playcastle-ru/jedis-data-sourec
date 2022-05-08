package pl.memexurer.jedisdatasource;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;
import pl.memexurer.jedisdatasource.api.JedisDataSource;
import pl.memexurer.jedisdatasource.api.JedisDataSourceConfiguration;
import pl.memexurer.jedisdatasource.api.JedisDataSourceProvider;

@Plugin(
    id = "jedis-data-source",
    name = "datasource",
    version = "1.0"
)
public class JedisPlugin implements JedisDataSourceProvider {

  @Inject
  private Logger logger;

  @Inject
  private ProxyServer server;

  private JedisDataSource dataSource;

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) {
    this.dataSource = JedisDataSourceConfiguration.createEnv().create();

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      logger.info("Jedis data source is finally shutting down!");
      JedisPlugin.this.dataSource.close();
    }));
  }

  @Override
  public JedisDataSource getDataSource() {
    return dataSource;
  }
}
