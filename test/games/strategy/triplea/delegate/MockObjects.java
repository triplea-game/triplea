package games.strategy.triplea.delegate;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.net.GUID;
import games.strategy.net.IConnectionChangeListener;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.net.Node;
import games.strategy.triplea.TripleAPlayer;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.delegate.dataObjects.CasualtyList;
import games.strategy.util.Match;

public class MockObjects {

  @SuppressWarnings("unchecked")
  public static TripleAPlayer getDummyPlayer() {
    TripleAPlayer dummyPlayer = mock(TripleAPlayer.class);
    when(dummyPlayer.confirmMoveHariKari()).thenReturn(false);
    when(dummyPlayer.confirmMoveInFaceOfAA(any(Collection.class))).thenReturn(false);
    when(dummyPlayer.confirmMoveKamikaze()).thenReturn(false);
    when(dummyPlayer.acceptAction(any(PlayerID.class), any(String.class), any(boolean.class))).thenReturn(true);
    when(dummyPlayer.selectAttackSubs(any(Territory.class))).thenReturn(false);
    when(dummyPlayer.selectAttackTransports(any(Territory.class))).thenReturn(false);
    when(dummyPlayer.selectAttackUnits(any(Territory.class))).thenReturn(false);
    when(dummyPlayer.selectShoreBombard(any(Territory.class))).thenReturn(false);
    when(dummyPlayer.shouldBomberBomb(any(Territory.class))).thenReturn(false);
    when(dummyPlayer.selectCasualties(any(Collection.class), any(Map.class), any(int.class), any(String.class),
        any(DiceRoll.class), any(PlayerID.class), any(Collection.class), any(PlayerID.class), any(Collection.class),
        any(boolean.class), any(Collection.class),
        any(CasualtyList.class), any(GUID.class), any(Territory.class), any(boolean.class)))
            .thenAnswer(new Answer<CasualtyDetails>() {

              @Override
              public CasualtyDetails answer(InvocationOnMock invocation) throws Throwable {
                CasualtyList defaultCasualties = (CasualtyList) invocation.getArguments()[11];
                if (defaultCasualties != null) {
                  return new CasualtyDetails(defaultCasualties.getKilled(), defaultCasualties.getDamaged(), true);
                }
                return null;
              }

            });
    when(dummyPlayer.whatShouldBomberBomb(any(Territory.class), any(Collection.class), any(Collection.class)))
        .thenAnswer(new Answer<Unit>() {

          @Override
          public Unit answer(InvocationOnMock invocation) throws Throwable {
            Collection<Unit> potentialTargets = invocation.getArgumentAt(1, Collection.class);
            if (potentialTargets == null || potentialTargets.isEmpty()) {
              // is null even allowed?
              return null;
            }
            final Collection<Unit> typicalFactories =
                Match.getMatches(potentialTargets, Matches.UnitCanProduceUnitsAndCanBeDamaged);
            if (typicalFactories.isEmpty()) {
              return potentialTargets.iterator().next();
            }
            return typicalFactories.iterator().next();
          }

        });
    return dummyPlayer;
  }

  public static IServerMessenger getDummyMessenger() {
    IServerMessenger messenger = mock(IServerMessenger.class);
    List<IConnectionChangeListener> connectionListeners = new CopyOnWriteArrayList<>();
    doAnswer(new Answer<Void>() {

      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        connectionListeners.add(invocation.getArgumentAt(0, IConnectionChangeListener.class));
        return null;
      }

    }).when(messenger).addConnectionChangeListener(any(IConnectionChangeListener.class));
    doAnswer(new Answer<Void>() {

      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        connectionListeners.remove(invocation.getArgumentAt(0, IConnectionChangeListener.class));
        return null;
      }

    }).when(messenger).removeConnectionChangeListener(any(IConnectionChangeListener.class));
    doAnswer(new Answer<Void>() {

      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        for (final IConnectionChangeListener listener : connectionListeners) {
          listener.connectionRemoved(invocation.getArgumentAt(0, INode.class));
        }
        return null;
      }

    }).when(messenger).removeConnection(any(INode.class));
    Node dummyNode;
    try {
      dummyNode = new Node("dummy", InetAddress.getLocalHost(), 0);
    } catch (final UnknownHostException e) {
      ClientLogger.logQuietly(e);
      throw new IllegalStateException(e);
    }
    when(messenger.getLocalNode()).thenReturn(dummyNode);
    when(messenger.getServerNode()).thenReturn(dummyNode);
    when(messenger.getNodes()).thenReturn(new HashSet<>());
    when(messenger.isConnected()).thenReturn(true);
    when(messenger.isServer()).thenReturn(true);
    when(messenger.isAcceptNewConnections()).thenReturn(false);
    when(messenger.getRemoteServerSocketAddress()).thenReturn(dummyNode.getSocketAddress());
    when(messenger.getPlayerMac(any(String.class))).thenReturn("DummyMacAdress");
    when(messenger.IsUsernameMiniBanned(any(String.class))).thenReturn(false);
    when(messenger.IsIpMiniBanned(any(String.class))).thenReturn(false);
    when(messenger.IsMacMiniBanned(any(String.class))).thenReturn(false);
    return messenger;
  }
}
