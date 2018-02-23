package games.strategy.triplea.oddsCalculator.ta;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.triplea.ui.UiContext;
import games.strategy.ui.SwingComponents;

public class OddsCalculatorDialog extends JDialog {
  private static final long serialVersionUID = -7625420355087851930L;
  private static final int MAX_HEIGHT = 640;
  private static Point lastPosition;
  private static Dimension lastShape;
  private final OddsCalculatorPanel panel;

  public static void show(final TripleAFrame taFrame, final Territory t) {
    final OddsCalculatorDialog dialog =
        new OddsCalculatorDialog(taFrame.getGame().getData(), taFrame.getUiContext(), taFrame, t);
    dialog.pack();
    dialog.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosed(final WindowEvent e) {
        if ((taFrame.getUiContext() != null) && !taFrame.getUiContext().isShutDown()) {
          taFrame.getUiContext().removeShutdownWindow(dialog);
        }
      }
    });
    // close when hitting the escape key
    SwingComponents.addEscapeKeyListener(dialog, () -> {
      dialog.setVisible(false);
      dialog.dispose();
    });

    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    if (lastPosition == null) {
      dialog.setLocationRelativeTo(taFrame);
      if (dialog.getHeight() > MAX_HEIGHT) {
        dialog.setSize(new Dimension(dialog.getWidth(), MAX_HEIGHT));
      }
    } else {
      dialog.setLocation(lastPosition);
      dialog.setSize(lastShape);
    }
    dialog.setVisible(true);
    taFrame.getUiContext().addShutdownWindow(dialog);
  }

  OddsCalculatorDialog(final GameData data, final UiContext uiContext, final JFrame parent, final Territory location) {
    super(parent, "Odds Calculator");
    panel = new OddsCalculatorPanel(data, uiContext, location, this);
    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(panel, BorderLayout.CENTER);
    pack();
  }

  @Override
  public void dispose() {
    lastPosition = new Point(getLocation());
    lastShape = new Dimension(getSize());
    panel.shutdown();
    super.dispose();
  }

  @Override
  public void setVisible(final boolean vis) {
    super.setVisible(vis);
    panel.selectCalculateButton();
  }
}
