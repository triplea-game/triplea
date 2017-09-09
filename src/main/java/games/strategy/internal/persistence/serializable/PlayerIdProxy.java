package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.PlayerID;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;

/**
 * A serializable proxy for the {@link PlayerID} class.
 */
@Immutable
public final class PlayerIdProxy implements Proxy {
  private static final long serialVersionUID = -8737466123516860123L;

  public static final ProxyFactory FACTORY = ProxyFactory.newInstance(PlayerID.class, PlayerIdProxy::new);

  private final Map<String, IAttachment> attachments;
  private final GameData gameData;
  private final String name;
  // TODO: add other attributes

  public PlayerIdProxy(final PlayerID playerId) {
    checkNotNull(playerId);

    attachments = playerId.getAttachments();
    gameData = playerId.getData();
    name = playerId.getName();
  }

  @Override
  public Object readResolve() {
    final PlayerID playerId = new PlayerID(name, gameData);
    attachments.forEach(playerId::addAttachment);
    return playerId;
  }
}
