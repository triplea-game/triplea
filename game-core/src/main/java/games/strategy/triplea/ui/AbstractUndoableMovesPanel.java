package games.strategy.triplea.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.swing.AbstractAction;
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

import com.google.common.collect.Sets;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.AbstractUndoableMove;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;

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
    moves = Collections.emptyList();
  }

  public void setMoves(final List<AbstractUndoableMove> undoableMoves) {
    moves = undoableMoves;
    SwingUtilities.invokeLater(() -> initLayout());
  }

  void undoMoves(final Map<Territory, List<Unit>> highlightUnitByTerritory) {
    final Set<Unit> units = Sets.newHashSet();
    for (final List<Unit> highlightedUnits : highlightUnitByTerritory.values()) {
      units.addAll(highlightedUnits);
    }
    movePanel.undoMoves(units);
  }


  private void initLayout() {
    removeAll();
    setLayout(new BorderLayout());
    final JPanel items = new JPanel();
    items.setLayout(new BoxLayout(items, BoxLayout.Y_AXIS));
    // we want the newest move at the top
    moves = new ArrayList<>(moves);
    Collections.reverse(moves);
    final Iterator<AbstractUndoableMove> iter = moves.iterator();
    if (iter.hasNext()) {
      add(new JLabel((this instanceof UndoablePlacementsPanel) ? "Placements:" : "Moves:"), BorderLayout.NORTH);
    }
    int scrollIncrement = 10;
    final Dimension seperatorSize = new Dimension(150, 20);
    while (iter.hasNext()) {
      final AbstractUndoableMove item = iter.next();
      final JComponent moveComponent = createComponentForMove(item);
      scrollIncrement = moveComponent.getPreferredSize().height;
      items.add(moveComponent);
      if (iter.hasNext()) {
        final JSeparator seperator = new JSeparator(SwingConstants.HORIZONTAL);
        seperator.setPreferredSize(seperatorSize);
        seperator.setMaximumSize(seperatorSize);
        items.add(seperator);
      }
    }
    if ((movePanel.getUndoableMoves() != null) && (movePanel.getUndoableMoves().size() > 1)) {
      final JButton undoAllButton = new JButton("Undo All");
      undoAllButton.addActionListener(new UndoAllMovesActionListener());
      items.add(undoAllButton);
    }

    final int scrollIncrementFinal = scrollIncrement + seperatorSize.height;
    // JScrollPane scroll = new JScrollPane(items);
    scroll = new JScrollPane(items) {
      private static final long serialVersionUID = -1064967105431785533L;

      @Override
      public void paint(final Graphics g) {
        if (previousVisibleIndex != null) {
          items.scrollRectToVisible(new Rectangle(0, scrollIncrementFinal * ((moves.size()) - previousVisibleIndex),
              1, scrollIncrementFinal));
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
    add(scroll, BorderLayout.CENTER);
    SwingUtilities.invokeLater(() -> validate());
  }

  private JComponent createComponentForMove(final AbstractUndoableMove move) {
    final Box unitsBox = new Box(BoxLayout.X_AXIS);
    unitsBox.add(new JLabel((move.getIndex() + 1) + ") "));
    final Collection<UnitCategory> unitCategories = UnitSeperator.categorize(move.getUnits());
    final Dimension buttonSize = new Dimension(80, 22);
    for (final UnitCategory category : unitCategories) {
      final Optional<ImageIcon> icon =
          movePanel.getMap().getUiContext().getUnitImageFactory().getIcon(category.getType(),
              category.getOwner(), category.hasDamageOrBombingUnitDamage(), category.getDisabled());
      if (icon.isPresent()) {
        final JLabel label = new JLabel("x" + category.getUnits().size() + " ", icon.get(), SwingConstants.LEFT);
        unitsBox.add(label);
      }
    }
    unitsBox.add(Box.createHorizontalGlue());
    final JLabel text = new JLabel(move.getMoveLabel());
    final Box textBox = new Box(BoxLayout.X_AXIS);
    textBox.add(text);
    textBox.add(Box.createHorizontalGlue());
    final JButton cancelButton = new JButton(new UndoMoveActionListener(move.getIndex()));
    setSize(buttonSize, cancelButton);
    final JButton viewbutton = new JButton(new ViewAction(move));
    setSize(buttonSize, viewbutton);
    final Box buttonsBox = new Box(BoxLayout.X_AXIS);
    buttonsBox.add(viewbutton);
    buttonsBox.add(cancelButton);
    buttonsBox.add(Box.createHorizontalGlue());
    final Box containerBox = new Box(BoxLayout.Y_AXIS);
    containerBox.add(unitsBox);
    containerBox.add(textBox);
    containerBox.add(buttonsBox);
    containerBox.add(new JLabel(" "));
    return containerBox;
  }

  public int getCountOfMovesMade() {
    return moves.size();
  }

  protected void setSize(final Dimension buttonSize, final JButton cancelButton) {
    cancelButton.setMinimumSize(buttonSize);
    cancelButton.setPreferredSize(buttonSize);
    cancelButton.setMaximumSize(buttonSize);
  }


  class UndoMoveActionListener extends AbstractAction {
    private static final long serialVersionUID = -397312652244693138L;
    private final int moveIndex;

    public UndoMoveActionListener(final int index) {
      super("Undo");
      moveIndex = index;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      // Record position of scroll bar as percentage.
      scrollBarPreviousValue = scroll.getVerticalScrollBar().getValue();
      final String error = movePanel.undoMove(moveIndex);
      if (error == null) {
        previousVisibleIndex = Math.max(0, moveIndex - 1);
      } else {
        previousVisibleIndex = null;
      }
    }
  }

  class UndoAllMovesActionListener extends AbstractAction {
    private static final long serialVersionUID = 7908136093303143896L;

    public UndoAllMovesActionListener() {
      super("UndoAllMoves");
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      final int moveCount = movePanel.getUndoableMoves().size();
      final boolean suppressErrorMsgToUser = true;
      for (int i = moveCount - 1; i >= 0; i--) {
        movePanel.undoMove(i, suppressErrorMsgToUser);
      }
    }
  }


  class ViewAction extends AbstractAction {
    private static final long serialVersionUID = -6999284663802575467L;
    private final AbstractUndoableMove move;

    public ViewAction(final AbstractUndoableMove move) {
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

  protected abstract void specificViewAction(final AbstractUndoableMove move);
}
