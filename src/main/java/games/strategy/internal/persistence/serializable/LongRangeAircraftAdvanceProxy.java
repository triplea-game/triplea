package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.IAttachment;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.triplea.delegate.LongRangeAircraftAdvance;

/**
 * A serializable proxy for the {@link LongRangeAircraftAdvance} class.
 *
 * <p>
 * This proxy does not serialize the game data owner to avoid a circular reference. Instances of
 * {@link LongRangeAircraftAdvance} created from this proxy will always have their game data set to {@code null}.
 * Proxies that may compose instances of this proxy are required to manually restore the game data in their
 * {@code readResolve()} method via a context-dependent mechanism.
 * </p>
 */
@Immutable
public final class LongRangeAircraftAdvanceProxy implements Proxy {
  private static final long serialVersionUID = -1150066578587095424L;

  public static final ProxyFactory FACTORY =
      ProxyFactory.newInstance(LongRangeAircraftAdvance.class, LongRangeAircraftAdvanceProxy::new);

  private final Map<String, IAttachment> attachments;

  public LongRangeAircraftAdvanceProxy(final LongRangeAircraftAdvance longRangeAircraftAdvance) {
    checkNotNull(longRangeAircraftAdvance);

    attachments = longRangeAircraftAdvance.getAttachments();
  }

  @Override
  public Object readResolve() {
    final LongRangeAircraftAdvance longRangeAircraftAdvance = new LongRangeAircraftAdvance(null);
    attachments.forEach(longRangeAircraftAdvance::addAttachment);
    return longRangeAircraftAdvance;
  }
}
