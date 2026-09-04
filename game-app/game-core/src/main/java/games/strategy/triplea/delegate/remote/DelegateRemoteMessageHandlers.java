package games.strategy.triplea.delegate.remote;

import games.strategy.engine.data.GameData;
import games.strategy.net.Messengers;
import lombok.experimental.UtilityClass;

/**
 * Central registration point for the typed delegate message handlers. Called from {@link
 * games.strategy.engine.framework.ServerGame#addDelegateMessenger} as each delegate endpoint is
 * registered; each interface's {@code registerHandlers} registers unconditionally and the registry
 * overwrites, so re-running this across delegates and across games is harmless and refreshes each
 * handler's captured per-game game data.
 *
 * <p>Handlers are keyed by message type on the session-scoped registry and dispatch to whichever
 * delegate the addressed per-name endpoint currently holds, so a single registration per type
 * serves every delegate of that interface.
 */
@UtilityClass
public class DelegateRemoteMessageHandlers {
  public static void registerAll(final Messengers messengers, final GameData gameData) {
    IPoliticsDelegate.registerHandlers(messengers);
    IUserActionDelegate.registerHandlers(messengers);
    IBattleDelegate.registerHandlers(messengers, gameData);
    ITechDelegate.registerHandlers(messengers);
    IAbstractForumPosterDelegate.registerHandlers(messengers);
    IEditDelegate.registerHandlers(messengers);
    IAbstractMoveDelegate.registerHandlers(messengers);
    IMoveDelegate.registerHandlers(messengers, gameData);
    IAbstractPlaceDelegate.registerHandlers(messengers, gameData);
    IPurchaseDelegate.registerHandlers(messengers);
  }
}
