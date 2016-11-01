package games.strategy.triplea.ui;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

import com.google.common.collect.Sets;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.AbstractUndoableMove;
import games.strategy.triplea.util.JFXUtils;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;
import javafx.animation.AnimationTimer;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

abstract public class AbstractUndoableMovesPanel extends BorderPane {
  protected List<AbstractUndoableMove> m_moves;
  protected final GameData m_data;
  protected final AbstractMovePanel m_movePanel;
  protected ScrollPane scroll;
  protected double scrollBarPreviousValue = -1;
  protected int previousVisibleIndex = -1;

  public AbstractUndoableMovesPanel(final GameData data, final AbstractMovePanel movePanel) {
    m_data = data;
    m_movePanel = movePanel;
    m_moves = Collections.emptyList();
  }

  public void setMoves(final List<AbstractUndoableMove> m_undoableMoves) {
    m_moves = m_undoableMoves;
    SwingUtilities.invokeLater(() -> initLayout());
  }

  public void undoMoves(final Map<Territory, List<Unit>> highlightUnitByTerritory) {
    final Set<Unit> units = Sets.newHashSet();
    for (final List<Unit> highlightedUnits : highlightUnitByTerritory.values()) {
      units.addAll(highlightedUnits);
    }
    m_movePanel.undoMoves(units);
  }


  private void initLayout() {
    getChildren().clear();
    final VBox items = new VBox();
    // we want the newest move at the top
    m_moves = new ArrayList<>(m_moves);
    Collections.reverse(m_moves);
    final Iterator<AbstractUndoableMove> iter = m_moves.iterator();
    if (m_moves.size() > 0) {
      setTop(new Label((this instanceof UndoablePlacementsPanel) ? "Placements:" : "Moves:"));
    }
    double scrollIncrement = 10;
    final int seperatorWidth = 150;
    final int seperatorHeight = 20;
    while (iter.hasNext()) {
      final AbstractUndoableMove item = iter.next();
      final VBox moveComponent = createComponentForMove(item);
      scrollIncrement = moveComponent.getPrefHeight();
      items.getChildren().add(moveComponent);
      if (iter.hasNext()) {
        final Separator seperator = new Separator(Orientation.HORIZONTAL);
        seperator.setPrefSize(seperatorWidth, seperatorHeight);
        seperator.setMaxSize(seperatorWidth, seperatorHeight);
        items.getChildren().add(seperator);
      }
    }
    if (m_movePanel.getUndoableMoves() != null && m_movePanel.getUndoableMoves().size() > 1) {
      final Button undoAllButton = new Button("Undo All");
      undoAllButton.setOnAction(new UndoAllMovesActionListener());
      items.getChildren().add(undoAllButton);
    }

    final double scrollIncrementFinal = scrollIncrement + seperatorHeight;
    // JScrollPane scroll = new JScrollPane(items);
    scroll = new ScrollPane(items);
    new AnimationTimer() {
      @Override
      public void handle(long now) {
        if (previousVisibleIndex != -1) {
          scroll.setVvalue(0);
          scroll.setHvalue(scrollIncrementFinal * (m_moves.size() - previousVisibleIndex));
          previousVisibleIndex = -1;
        }
      }
    }.start();
    scroll.setBorder(null);
    // scroll.getVerticalScrollBar().setUnitIncrement(scrollIncrementFinal);TODO CSS
    if (scrollBarPreviousValue != -1) {
      scroll.setVvalue(scrollBarPreviousValue);
      scrollBarPreviousValue = -1;
    }
    setCenter(scroll);
  }

  private VBox createComponentForMove(final AbstractUndoableMove move) {
    final HBox unitsBox = new HBox();
    unitsBox.getChildren().add(new Label((move.getIndex() + 1) + ") "));
    final Collection<UnitCategory> unitCategories = UnitSeperator.categorize(move.getUnits());
    final Iterator<UnitCategory> iter = unitCategories.iterator();
    final int buttonWidth = 80;
    final int buttonHeight = 22;
    while (iter.hasNext()) {
      final UnitCategory category = iter.next();
      final Optional<ImageIcon> icon =
          m_movePanel.getMap().getUIContext().getUnitImageFactory().getIcon(category.getType(),
              category.getOwner(), m_data, category.hasDamageOrBombingUnitDamage(), category.getDisabled());
      if (icon.isPresent()) {
        final Label label = new Label("x" + category.getUnits().size() + " ");
        label.setGraphic(new ImageView(JFXUtils.convertToFx((BufferedImage) icon.get().getImage())));
        unitsBox.getChildren().add(label);
      }
    }
    final Label text = new Label(move.getMoveLabel());
    final HBox textBox = new HBox();
    textBox.getChildren().add(text);
    final Button cancelButton = JFXUtils.getButtonWithAction(new UndoMoveActionListener(move.getIndex()));
    setSize(buttonWidth, buttonHeight, cancelButton);
    final Button viewbutton = JFXUtils.getButtonWithAction(new ViewAction(move));
    setSize(buttonWidth, buttonHeight, viewbutton);
    final HBox buttonsBox = new HBox();
    buttonsBox.getChildren().add(viewbutton);
    buttonsBox.getChildren().add(cancelButton);
    final VBox rVal = new VBox();
    rVal.getChildren().add(unitsBox);
    rVal.getChildren().add(textBox);
    rVal.getChildren().add(buttonsBox);
    rVal.getChildren().add(new Label(" "));
    return rVal;
  }

  public int getCountOfMovesMade() {
    return m_moves.size();
  }

  protected void setSize(double width, double height, final Button cancelButton) {
    cancelButton.setMinSize(width, height);
    cancelButton.setPrefSize(width, height);
    cancelButton.setMaxSize(width, height);
  }


  class UndoMoveActionListener implements EventHandler<ActionEvent> {
    private final int m_moveIndex;

    public UndoMoveActionListener(final int index) {
      m_moveIndex = index;
    }

    @Override
    public void handle(ActionEvent event) {
      // Record position of scroll bar as percentage.
      scrollBarPreviousValue = scroll.getVvalue();
      final String error = m_movePanel.undoMove(m_moveIndex);
      if (error == null) {
        previousVisibleIndex = Math.max(0, m_moveIndex - 1);
      } else {
        previousVisibleIndex = -1;
      }
    }
  }

  class UndoAllMovesActionListener implements EventHandler<ActionEvent> {

    @Override
    public void handle(ActionEvent event) {
      final int moveCount = m_movePanel.getUndoableMoves().size();
      final boolean suppressErrorMsgToUser = true;
      for (int i = moveCount - 1; i >= 0; i--) {
        m_movePanel.undoMove(i, suppressErrorMsgToUser);
      }
    }
  }


  class ViewAction implements EventHandler<ActionEvent> {
    private final AbstractUndoableMove m_move;

    public ViewAction(final AbstractUndoableMove move) {
      m_move = move;
    }

    @Override
    public void handle(ActionEvent event) {
      m_movePanel.cancelMove();
      if (!m_movePanel.getMap().isShowing(m_move.getEnd())) {
        m_movePanel.getMap().centerOn(m_move.getEnd());
      }
      specificViewAction(m_move);
    }
  }

  protected abstract void specificViewAction(final AbstractUndoableMove move);
}
