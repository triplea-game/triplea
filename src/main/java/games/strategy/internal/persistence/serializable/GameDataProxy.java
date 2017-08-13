package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.InvalidObjectException;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameDataMemento;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.util.memento.Memento;
import games.strategy.util.memento.MementoExportException;
import games.strategy.util.memento.MementoImportException;
import net.jcip.annotations.Immutable;

/**
 * A serializable proxy for the {@link GameData} class.
 */
@Immutable
public final class GameDataProxy implements Proxy {
  private static final long serialVersionUID = 8249846682421871173L;

  public static final ProxyFactory FACTORY = ProxyFactory.newInstance(GameData.class, GameDataProxy::new);

  private final Memento memento;

  public GameDataProxy(final GameData gameData) {
    checkNotNull(gameData);

    try {
      memento = GameDataMemento.newExporter().exportMemento(gameData);
    } catch (final MementoExportException e) {
      throw new IllegalArgumentException("failed to create proxy from GameData", e);
    }
  }

  @Override
  public Object readResolve() throws InvalidObjectException {
    try {
      return GameDataMemento.newImporter().importMemento(memento);
    } catch (final MementoImportException e) {
      throw (InvalidObjectException) new InvalidObjectException("failed to create GameData from proxy").initCause(e);
    }
  }
}
