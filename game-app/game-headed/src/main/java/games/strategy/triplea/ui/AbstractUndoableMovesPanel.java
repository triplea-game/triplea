package games.strategy.triplea.ui;

import static games.strategy.triplea.image.UnitImageFactory.ImageKey;
import static games.strategy.triplea.util.UnitSeparator.getComparatorUnitCategories;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.AbstractUndoableMove;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeparator;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public abstract class AbstractUndoableMovesPanel extends JPanel {
  private static final long serialVersionUID = 1910945925958952416L;
  protected List<AbstractUndoableMove> moves;

  protected final AbstractMovePanel movePanel;
  protected JScrollPane scroll;
  // TODO replace this Integer with a int primitive... Using null as toggle switch is bad code
  protected Integer scrollBarPreviousValue = null;
  protected Integer previousVisibleIndex = null;

  protected AbstractUndoableMovesPanel(final AbstractMovePanel movePanel) {
    this.movePanel = movePanel;
    moves = List.of();
  }

  void setMoves(final List<AbstractUndoableMove> undoableMoves) {
    // we want the newest move at the top
    moves = new ArrayList<>(undoableMoves);
    Collections.reverse(moves);
    SwingUtilities.invokeLater(this::initLayout);
  }

  public void undoMoves(final Collection<Collection<Unit>> highlightUnitByTerritory) {
    final var units =
        highlightUnitByTerritory.stream().flatMap(Collection::stream).collect(Collectors.toSet());
    movePanel.undoMoves(units);
  }

  private void initLayout() {
    removeAll();
    setLayout(new BorderLayout());
    if (movesMade()) {
      Box box = Box.createVerticalBox();
      JLabel titleLabel = ActionPanel.createIndentedLabel();
      titleLabel.setText(getLabelText());
      box.add(titleLabel);
      box.add(new JSeparator());
      add(box, BorderLayout.NORTH);
    }
    add(createScrollPane(), BorderLayout.CENTER);
    // The two lines below are needed to ensure the view redraws at the correct size
    // when first loaded.
    revalidate();
    doLayout();
  }

  private JScrollPane createScrollPane() {
    final JPanel items = new JPanel();
    items.setLayout(new BoxLayout(items, BoxLayout.Y_AXIS));

    int scrollIncrement = 10;
    for (final AbstractUndoableMove item : moves) {
      final JComponent moveComponent = newComponentForMove(item);
      scrollIncrement = moveComponent.getPreferredSize().height;
      items.add(moveComponent);
    }

    final int scrollIncrementFinal = scrollIncrement;
    scroll =
        new JScrollPane(items) {
          private static final long serialVersionUID = -1064967105431785533L;

          @Override
          public void paint(final Graphics g) {
            if (previousVisibleIndex != null) {
              items.scrollRectToVisible(
                  new Rectangle(
                      0,
                      scrollIncrementFinal * (moves.size() - previousVisibleIndex),
                      1,
                      scrollIncrementFinal));
              previousVisibleIndex = null;
            }
            super.paint(g);
          }
        };
    scroll.setBorder(null);
    scroll.getVerticalScrollBar().setUnitIncrement(scrollIncrementFinal);
    if (scrollBarPreviousValue != null) {
      scroll.getVerticalScrollBar().setValue(scrollBarPreviousValue);
      scrollBarPreviousValue = null;
    }
    return scroll;
  }

  private JComponent newComponentForMove(final AbstractUndoableMove move) {
    final Box unitsBox = new Box(BoxLayout.X_AXIS);
    unitsBox.add(new JLabel((move.getIndex() + 1) + ") "));
    final List<UnitCategory> unitCategories =
        new ArrayList<>(UnitSeparator.categorize(move.getUnits()));
    final Dimension buttonSize = new Dimension(80, 22);
    unitCategories.sort(getComparatorUnitCategories(movePanel.getData()));
    for (final UnitCategory category : unitCategories) {
      final ImageIcon icon =
          movePanel.getMap().getUiContext().getUnitImageFactory().getIcon(ImageKey.of(category));
      final JLabel label =
          new JLabel("x" + category.getUnits().size() + " ", icon, SwingConstants.LEFT);
      unitsBox.add(label);
      MapUnitTooltipManager.setUnitTooltip(
          label,
          category.getType(),
          category.getOwner(),
          category.getUnits().size(),
          movePanel.getMap().getUiContext());
    }
    unitsBox.add(Box.createHorizontalGlue());
    final JLabel text = new JLabel(move.getMoveLabel());
    final Box textBox = new Box(BoxLayout.X_AXIS);
    textBox.add(text);
    textBox.add(Box.createHorizontalGlue());
    final JButton undoButton = new JButton("Undo");
    final int moveIndex = move.getIndex();
    undoButton.addActionListener(
        (e) -> {
          // Record position of scroll bar as percentage.
          scrollBarPreviousValue = scroll.getVerticalScrollBar().getValue();
          final String error = movePanel.undoMove(moveIndex);
          if (error == null) {
            // Disable the button so it can't be clicked again. Note: Undoing will cause a later
            // setMoves() call on this object, which will re-build the UI for all the moves.
            undoButton.setEnabled(false);
            previousVisibleIndex = Math.max(0, moveIndex - 1);
          } else {
            previousVisibleIndex = null;
          }
        });
    setSize(buttonSize, undoButton);
    final JButton viewButton = new JButton(new ViewAction(move));
    setSize(buttonSize, viewButton);
    final Box buttonsBox = new Box(BoxLayout.X_AXIS);
    buttonsBox.add(viewButton);
    buttonsBox.add(undoButton);
    buttonsBox.add(Box.createHorizontalGlue());
    final Box containerBox = new Box(BoxLayout.Y_AXIS);
    containerBox.add(unitsBox);
    containerBox.add(textBox);
    containerBox.add(buttonsBox);
    containerBox.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
    return containerBox;
  }

  public boolean movesMade() {
    return !moves.isEmpty();
  }

  protected void setSize(final Dimension buttonSize, final JButton cancelButton) {
    cancelButton.setMinimumSize(buttonSize);
    cancelButton.setPreferredSize(buttonSize);
    cancelButton.setMaximumSize(buttonSize);
  }

  class ViewAction extends AbstractAction {
    private static final long serialVersionUID = -6999284663802575467L;
    private final AbstractUndoableMove move;

    ViewAction(final AbstractUndoableMove move) {
      super("Show");
      this.move = move;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      movePanel.cancelMove();
      if (!movePanel.getMap().isShowing(move.getEnd())) {
        movePanel.getMap().centerOn(move.getEnd());
      }
      specificViewAction(move);
    }
  }

  protected String getLabelText() {
    return "Moves:";
  }

  protected abstract void specificViewAction(AbstractUndoableMove move);
}
