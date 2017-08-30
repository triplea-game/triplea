package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.IAttachment;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.triplea.delegate.LongRangeAircraftAdvance;

/**
 * A serializable proxy for the {@link LongRangeAircraftAdvance} class.
 */
@Immutable
public final class LongRangeAircraftAdvanceProxy implements Proxy {
  private static final long serialVersionUID = -1150066578587095424L;

  public static final ProxyFactory FACTORY =
      ProxyFactory.newInstance(LongRangeAircraftAdvance.class, LongRangeAircraftAdvanceProxy::new);

  private final Map<String, IAttachment> attachments;
  private final GameData gameData;

  public LongRangeAircraftAdvanceProxy(final LongRangeAircraftAdvance longRangeAircraftAdvance) {
    checkNotNull(longRangeAircraftAdvance);

    attachments = longRangeAircraftAdvance.getAttachments();
    gameData = longRangeAircraftAdvance.getData();
  }

  @Override
  public Object readResolve() {
    final LongRangeAircraftAdvance longRangeAircraftAdvance = new LongRangeAircraftAdvance(gameData);
    attachments.forEach(longRangeAircraftAdvance::addAttachment);
    return longRangeAircraftAdvance;
  }
}
