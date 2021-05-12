package com.celeste.databases.storage.model.database.provider.impl.sql.mysql;

import com.celeste.databases.core.model.database.provider.exception.FailedConnectionException;
import com.celeste.databases.core.model.entity.RemoteCredentials;
import com.celeste.databases.storage.model.database.provider.impl.sql.Sql;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MySqlProvider implements Sql {

  private static final String URI;

  private static final Pattern HOSTNAME;
  private static final Pattern PORT;
  private static final Pattern DATABASE;
  private static final Pattern SSL;

  static {
    URI = "jdbc:mysql://<hostname>:<port>/<database>?useSSL=<ssl>";

    HOSTNAME = Pattern.compile("<hostname>", Pattern.LITERAL);
    PORT = Pattern.compile("<port>", Pattern.LITERAL);
    DATABASE = Pattern.compile("<database>", Pattern.LITERAL);
    SSL = Pattern.compile("<ssl>", Pattern.LITERAL);
  }

  private final RemoteCredentials credentials;
  private HikariDataSource hikari;

  public MySqlProvider(final RemoteCredentials credentials) throws FailedConnectionException {
    this.credentials = credentials;

    init();
  }

  @Override
  public synchronized void init() throws FailedConnectionException {
    try {
      final HikariConfig config = new HikariConfig();

      config.setDriverClassName("com.mysql.cj.jdbc.Driver");

      final String hostname = credentials.getHostname();
      final int port = credentials.getPort();

      final String database = credentials.getDatabase();

      final String username = credentials.getUsername();
      final String password = credentials.getPassword();

      final boolean ssl = credentials.isSsl();

      final String newUri = SSL.matcher(DATABASE.matcher(PORT.matcher(HOSTNAME.matcher(URI)
          .replaceAll(Matcher.quoteReplacement(hostname)))
          .replaceAll(Matcher.quoteReplacement(String.valueOf(port))))
          .replaceAll(Matcher.quoteReplacement(database)))
          .replaceAll(Matcher.quoteReplacement(String.valueOf(ssl)));

      config.setJdbcUrl(newUri);

      config.setUsername(username);
      config.setPassword(password);

      config.setMinimumIdle(1);
      config.setMaximumPoolSize(20);

      config.setConnectionTimeout(30000);
      config.setIdleTimeout(600000);
      config.setMaxLifetime(1800000);

      config.addDataSourceProperty("alwaysSendSetIsolation", "false");
      config.addDataSourceProperty("autoReconnect", "true");
      config.addDataSourceProperty("cachePrepStmts", "true");
      config.addDataSourceProperty("prepStmtCacheSize", "250");
      config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
      config.addDataSourceProperty("useServerPrepStmts", "true");
      config.addDataSourceProperty("useLocalSessionState", "true");
      config.addDataSourceProperty("rewriteBatchedStatements", "true");
      config.addDataSourceProperty("cacheResultSetMetadata", "true");
      config.addDataSourceProperty("cacheServerConfiguration", "true");
      config.addDataSourceProperty("elideSetAutoCommits", "true");
      config.addDataSourceProperty("maintainTimeStats", "false");
      config.addDataSourceProperty("cacheCallableStmts", "true");
      config.addDataSourceProperty("serverTimezone", "UTC");
      config.addDataSourceProperty("socketTimeout", String.valueOf(TimeUnit.SECONDS.toMillis(30)));

      this.hikari = new HikariDataSource(config);
    } catch (Throwable throwable) {
      throw new FailedConnectionException(throwable);
    }
  }

  @Override
  public synchronized void shutdown() {
    hikari.close();
  }

  @Override
  public boolean isClosed() {
    return hikari.isClosed();
  }

  @Override
  public Connection getConnection() throws FailedConnectionException {
    try {
      return hikari.getConnection();
    } catch (SQLException exception) {
      throw new FailedConnectionException(exception);
    }
  }

}