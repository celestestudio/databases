package com.celeste.database.messenger.factory;

import com.celeste.database.messenger.model.database.provider.Messenger;
import com.celeste.database.shared.exceptions.database.FailedConnectionException;
import com.celeste.database.messenger.model.database.type.MessengerType;
import com.celeste.database.shared.model.type.ConnectionType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.util.Properties;

/**
 * The MessengerFactory creates the connection between the
 * application and the server. It has support to RabbitMQ
 * and Redis, working on including Kafka soon.
 *
 * <p>To estabilish connection with the Messenger, you should always provide
 * the Driver and Credentials in the Properties and the ConnectionType.</p>
 *
 * <p>The type of the connection are LOCAL (The Messenger is installed) in
 * the machine the program will execute or in another machine that doesn't block
 * remote access. Or CLUSTER, a connection created by a cluster in another machine
 * that is only used for the Messenger.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MessengerFactory {

  private static final MessengerFactory INSTANCE = new MessengerFactory();

  /**
   * Creates a new instance of the provider provided in the field
   * driver of the properties.
   * @param properties Properties with Driver and Credentials
   * @param connectionType ConnectionType
   *
   * @return Messenger
   * @throws FailedConnectionException Throws if the connection failed
   */
  public Messenger<?> start(@NotNull final Properties properties, final ConnectionType connectionType) throws FailedConnectionException {
    try {
      final String driver = properties.getProperty("driver");
      final MessengerType cache = MessengerType.getMessenger(driver);

      final Constructor<? extends Messenger<?>> constructor = cache.getProvider().getConstructor(Properties.class, ConnectionType.class);
      return constructor.newInstance(properties, connectionType);
    } catch (Throwable throwable) {
      throw new FailedConnectionException(throwable);
    }
  }

  /**
   * Returns the instance of the MessengerFactory
   * @return MessengerFactory
   */
  public static MessengerFactory getInstance() {
    return INSTANCE;
  }

}