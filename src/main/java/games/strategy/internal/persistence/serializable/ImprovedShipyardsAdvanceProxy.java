package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.IAttachment;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.triplea.delegate.ImprovedShipyardsAdvance;

/**
 * A serializable proxy for the {@link ImprovedShipyardsAdvance} class.
 */
@Immutable
public final class ImprovedShipyardsAdvanceProxy implements Proxy {
  private static final long serialVersionUID = -46130524050751414L;

  public static final ProxyFactory FACTORY =
      ProxyFactory.newInstance(ImprovedShipyardsAdvance.class, ImprovedShipyardsAdvanceProxy::new);

  private final Map<String, IAttachment> attachments;
  private final GameData gameData;

  public ImprovedShipyardsAdvanceProxy(final ImprovedShipyardsAdvance improvedShipyardsAdvance) {
    checkNotNull(improvedShipyardsAdvance);

    attachments = improvedShipyardsAdvance.getAttachments();
    gameData = improvedShipyardsAdvance.getData();
  }

  @Override
  public Object readResolve() {
    final ImprovedShipyardsAdvance improvedShipyardsAdvance = new ImprovedShipyardsAdvance(gameData);
    attachments.forEach(improvedShipyardsAdvance::addAttachment);
    return improvedShipyardsAdvance;
  }
}
