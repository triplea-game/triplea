package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.IAttachment;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.triplea.delegate.ImprovedArtillerySupportAdvance;

/**
 * A serializable proxy for the {@link ImprovedArtillerySupportAdvance} class.
 */
@Immutable
public final class ImprovedArtillerySupportAdvanceProxy implements Proxy {
  private static final long serialVersionUID = -1231680617895343722L;

  public static final ProxyFactory FACTORY =
      ProxyFactory.newInstance(ImprovedArtillerySupportAdvance.class, ImprovedArtillerySupportAdvanceProxy::new);

  private final Map<String, IAttachment> attachments;
  private final GameData gameData;

  public ImprovedArtillerySupportAdvanceProxy(final ImprovedArtillerySupportAdvance improvedArtillerySupportAdvance) {
    checkNotNull(improvedArtillerySupportAdvance);

    attachments = improvedArtillerySupportAdvance.getAttachments();
    gameData = improvedArtillerySupportAdvance.getData();
  }

  @Override
  public Object readResolve() {
    final ImprovedArtillerySupportAdvance improvedArtillerySupportAdvance =
        new ImprovedArtillerySupportAdvance(gameData);
    attachments.forEach(improvedArtillerySupportAdvance::addAttachment);
    return improvedArtillerySupportAdvance;
  }
}
