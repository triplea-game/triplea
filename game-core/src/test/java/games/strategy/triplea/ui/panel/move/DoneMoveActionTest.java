package games.strategy.triplea.ui.panel.move;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.triplea.ui.AbstractUndoableMovesPanel;
import java.awt.Component;
import java.util.function.Function;
import javax.swing.JComponent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DoneMoveActionTest {
  @Mock private JComponent parentComponent;
  @Mock private AbstractUndoableMovesPanel undoableMovesPanel;
  @Mock private Component unitScrollerPanel;
  @Mock private Function<JComponent, Boolean> confirmNoMovement;

  @InjectMocks private DoneMoveAction doneMoveAction;

  @Test
  @DisplayName("With no moves made, if user declines confirmation, return false")
  void noMovesMadeAndUserSaysNoToConfirmation() {
    when(undoableMovesPanel.noMovesMade()).thenReturn(true);
    when(confirmNoMovement.apply(parentComponent)).thenReturn(false);

    assertThat(doneMoveAction.doneMoveAction(), is(false));
    verify(unitScrollerPanel, never()).setVisible(any());
  }

  @Test
  @DisplayName("With no moves made, if user accepts confirmation, return true")
  void noMovesMadeAndUserSaysYesToConfirmation() {
    when(undoableMovesPanel.noMovesMade()).thenReturn(true);
    when(confirmNoMovement.apply(parentComponent)).thenReturn(true);

    assertThat(doneMoveAction.doneMoveAction(), is(true));
    verify(unitScrollerPanel).setVisible(false);
  }

  @Test
  @DisplayName("If moves are made, return true, show no confirmation, hide the unit scroller")
  void movesMadeDoesNotPrompt() {
    when(undoableMovesPanel.noMovesMade()).thenReturn(false);

    assertThat(doneMoveAction.doneMoveAction(), is(true));

    verify(confirmNoMovement).apply(any());
    verify(unitScrollerPanel).setVisible(false);
  }
}
