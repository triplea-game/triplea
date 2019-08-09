package org.triplea.lobby.server.db;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Factory class to create public IDs. These are identifiers to be stored in database, should be
 * unique, they are not primary key, and they can be passed between the front-end and back for
 * identifying table rows. The purpose of this ID is so that we do not expose primary key values to
 * the front-end. For example, if we migrate to a new DB schema we can re-index with new PK values
 * and the existing public ID keys will continue to reference the same data.
 */
public class PublicIdSupplier implements Supplier<String> {

  @Override
  public String get() {
    return UUID.randomUUID().toString();
  }
}
