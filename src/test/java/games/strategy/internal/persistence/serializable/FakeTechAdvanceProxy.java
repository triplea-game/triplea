package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.IAttachment;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.triplea.delegate.FakeTechAdvance;

/**
 * A serializable proxy for the {@link FakeTechAdvance} class.
 */
@Immutable
public final class FakeTechAdvanceProxy implements Proxy {
  private static final long serialVersionUID = 8486465678813843350L;

  public static final ProxyFactory FACTORY = ProxyFactory.newInstance(FakeTechAdvance.class, FakeTechAdvanceProxy::new);

  private final Map<String, IAttachment> attachments;
  private final GameData gameData;
  private final String name;

  public FakeTechAdvanceProxy(final FakeTechAdvance fakeTechAdvance) {
    checkNotNull(fakeTechAdvance);

    attachments = fakeTechAdvance.getAttachments();
    gameData = fakeTechAdvance.getData();
    name = fakeTechAdvance.getName();
  }

  @Override
  public Object readResolve() {
    final FakeTechAdvance fakeTechAdvance = new FakeTechAdvance(gameData, name);
    attachments.forEach(fakeTechAdvance::addAttachment);
    return fakeTechAdvance;
  }
}
