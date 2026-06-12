package games.strategy.triplea.ui;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.player.PlayerBridge;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.ui.panels.map.MapPanel;
import games.strategy.triplea.ui.panels.map.MapSelectionListener;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.util.List;
import javax.swing.JPanel;
import org.junit.jupiter.api.Test;

final class PlacePanelTest {
  @Test
  void activeStalePlacePanelCanCallRemotePlaceDelegateWithCurrentPlayerUnits() throws Exception {
    // Models the client/UI half of the race: the placement map listener is
    // still callable with an active panel, current player, and PlayerBridge.
    // The remote delegate is mocked to fail like an uninitialized server-side
    // PlaceDelegate; server lifecycle is covered by PlaceDelegateTest.
    final GameData data = mock(GameData.class);
    when(data.acquireReadLock()).thenReturn(() -> {});
    final MapPanel mapPanel = mock(MapPanel.class);
    when(mapPanel.getData()).thenReturn(data);
    when(mapPanel.getUiContext()).thenReturn(mock(UiContext.class));
    final PlacePanel panel = new PlacePanel(data, mapPanel);

    final GamePlayer currentPlayer = mock(GamePlayer.class);
    final Unit unit = mock(Unit.class);
    final UnitAttachment unitAttachment = mock(UnitAttachment.class);
    when(unit.getUnitAttachment()).thenReturn(unitAttachment);
    when(currentPlayer.getUnits()).thenReturn(List.of(unit));

    final Territory selectedTerritory = mock(Territory.class);
    when(selectedTerritory.isWater()).thenReturn(false);
    when(selectedTerritory.getOwner()).thenReturn(currentPlayer);

    final PlayerBridge playerBridge = mock(PlayerBridge.class);
    final IAbstractPlaceDelegate placeDelegate = mock(IAbstractPlaceDelegate.class);
    when(playerBridge.getRemoteDelegate()).thenReturn(placeDelegate);
    when(placeDelegate.getPlaceableUnits(anyCollection(), same(selectedTerritory)))
        .thenThrow(
            new NullPointerException(
                "Cannot invoke \"games.strategy.engine.data.GamePlayer.getRulesAttachment()\" "
                    + "because \"player\" is null"));

    setField(ActionPanel.class, panel, "currentPlayer", currentPlayer);
    setField(ActionPanel.class, panel, "active", true);
    setField(AbstractMovePanel.class, panel, "playerBridge", playerBridge);

    final MapSelectionListener listener =
        getField(PlacePanel.class, panel, "placeMapSelectionListener", MapSelectionListener.class);

    assertThrows(
        NullPointerException.class,
        () -> listener.territorySelected(selectedTerritory, leftClickMouseDetails()));
  }

  private static MouseDetails leftClickMouseDetails() {
    return new MouseDetails(
        new MouseEvent(
            new JPanel(), MouseEvent.MOUSE_RELEASED, 0, 0, 0, 0, 1, false, MouseEvent.BUTTON1),
        0,
        0);
  }

  private static void setField(
      final Class<?> declaringClass, final Object target, final String name, final Object value)
      throws ReflectiveOperationException {
    final Field field = declaringClass.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static <T> T getField(
      final Class<?> declaringClass,
      final Object target,
      final String name,
      final Class<T> fieldType)
      throws ReflectiveOperationException {
    final Field field = declaringClass.getDeclaredField(name);
    field.setAccessible(true);
    return fieldType.cast(field.get(target));
  }
}
