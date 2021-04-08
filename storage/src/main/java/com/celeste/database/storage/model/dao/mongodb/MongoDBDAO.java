package com.celeste.database.storage.model.dao.mongodb;

import com.celeste.database.shared.exceptions.dao.DAOException;
import com.celeste.database.shared.exceptions.dao.ValueNotFoundException;
import com.celeste.database.shared.exceptions.database.FailedConnectionException;
import com.celeste.database.storage.model.dao.StorageDAO;
import com.celeste.database.storage.model.database.provider.mongodb.MongoDB;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Getter(AccessLevel.PRIVATE)
public final class MongoDBDAO<T> implements StorageDAO<T> {

  @Getter
  private final MongoDB provider;
  private final Class<T> clazz;

  private MongoCollection<T> collection;

  public MongoDBDAO(@NotNull final MongoDB provider, @NotNull final Class<T> clazz) throws DAOException {
    try {
      this.provider = provider;
      this.clazz = clazz;
    } catch (Throwable throwable) {
      throw new DAOException(throwable);
    }
  }

  @Override @SneakyThrows
  public void createTable(@NotNull final String name) {
    final MongoDatabase database = getProvider().getDatabase();

    if (!collectionExists(name)) {
      database.createCollection(name);
    }

    this.collection = database.getCollection(name, clazz);
  }

  @Override @SneakyThrows
  public final void save(@NotNull final Object key, @NotNull final T value) {
    final ReplaceOptions options = new ReplaceOptions()
        .upsert(true);

    final Bson bson = Filters.eq(key);
    collection.replaceOne(bson, value, options);
  }

  @Override
  public void delete(@NotNull Object key) {
    final Bson bson = Filters.eq(key);
    collection.deleteOne(bson);
  }

  @Override @SneakyThrows
  public boolean contains(@NotNull final Object key) {
    final Bson bson = Filters.eq(key);
    return collection.countDocuments(bson) > 0;
  }

  @Override @NotNull
  public T find(@NotNull final Object key) throws ValueNotFoundException {
    final Bson bson = Filters.eq(key);

    final T argument = collection.find(bson).first();

    if (argument == null) {
      throw new ValueNotFoundException("Value not found");
    }

    return argument;
  }

  @Override @NotNull
  public List<T> findAll() {
    final List<T> arguments = new ArrayList<>();

    try (MongoCursor<T> cursor = collection.find().cursor()) {
      while (cursor.hasNext()) {
        arguments.add(cursor.next());
      }
    }

    return arguments;
  }

  private boolean collectionExists(final String collectionName) throws FailedConnectionException {
    for (final String name : provider.getDatabase().listCollectionNames()) {
      if (collectionName.equalsIgnoreCase(name)) return true;
    }

    return false;
  }

}