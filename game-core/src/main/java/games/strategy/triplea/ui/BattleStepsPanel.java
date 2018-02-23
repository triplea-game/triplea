package games.strategy.triplea.ui;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import games.strategy.util.Interruptibles;

/**
 * A panel for showing the battle steps in a display.
 * Contains code for walking from the current step, to a given step
 * there is a delay while we walk so that the user can see the steps progression.
 * Users of this class should deactive it after they are done.
 */
class BattleStepsPanel extends JPanel implements Active {
  private static final long serialVersionUID = 911638924664810435L;
  private static final Logger logger = Logger.getLogger(BattleStepsPanel.class.getName());
  // if this is the target step, we want to walk to the last step
  private static final String LAST_STEP = "NULL MARKER FOR LAST STEP";
  private final DefaultListModel<String> listModel = new DefaultListModel<>();
  private final JList<String> list = new JList<>(listModel);
  private final MyListSelectionModel listSelectionModel = new MyListSelectionModel();
  // the step we want to reach
  private String targetStep = null;
  // all changes to state should be done while locked on this object.
  // when we reach the target step, or when we want to walk the step
  // notifyAll on this object
  private final Object mutex = new Object();
  private final List<CountDownLatch> waiters = new ArrayList<>();
  private boolean hasWalkThread = false;

  BattleStepsPanel() {
    setLayout(new BorderLayout());
    add(list, BorderLayout.CENTER);
    list.setBackground(this.getBackground());
    list.setSelectionModel(listSelectionModel);
  }

  @Override
  public void deactivate() {
    wakeAll();
  }

  private void wakeAll() {
    synchronized (mutex) {
      for (final CountDownLatch l : waiters) {
        l.countDown();
      }
      waiters.clear();
    }
  }

  /**
   * Set the steps given, setting the selected step to the first step.
   */
  public void listBattle(final List<String> steps) {
    if (!SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("Not in dispatch thread");
    }
    synchronized (mutex) {
      listModel.removeAllElements();
      steps.forEach(listModel::addElement);
      listSelectionModel.hiddenSetSelectionInterval(0);
      if (!steps.contains(targetStep)) {
        targetStep = null;
      }
    }
    validate();
  }

  private void clearTargetStep() {
    synchronized (mutex) {
      targetStep = null;
    }
    wakeAll();
  }

  private boolean doneWalkingSteps() {
    synchronized (mutex) {
      // not looking for anything
      if (targetStep == null) {
        return true;
      }
      // we cant find it, something is wrong
      if (!targetStep.equals(LAST_STEP) && (listModel.lastIndexOf(targetStep) == -1)) {
        new IllegalStateException("Step not found:" + targetStep + " in:" + listModel).printStackTrace();
        clearTargetStep();
        return true;
      }
      // at end, we are done
      if (targetStep.equals(LAST_STEP) && (list.getSelectedIndex() == (listModel.getSize() - 1))) {
        return true;
      }
      // we found it, we are done
      if (targetStep.equals(list.getSelectedValue())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Walks through and pause at each list item until we find our target.
   */
  private void walkStep() {
    if (!SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("Wrong thread");
    }
    if (doneWalkingSteps()) {
      wakeAll();
      return;
    }
    int index = list.getSelectedIndex() + 1;
    if (index >= list.getModel().getSize()) {
      index = 0;
    }
    listSelectionModel.hiddenSetSelectionInterval(index);
    waitThenWalk();
  }

  private void waitThenWalk() {
    new Thread(() -> {
      synchronized (mutex) {
        if (hasWalkThread) {
          return;
        }
        hasWalkThread = true;
      }
      try {
        if (Interruptibles.sleep(330)) {
          SwingUtilities.invokeLater(this::walkStep);
        }
      } finally {
        synchronized (mutex) {
          hasWalkThread = false;
        }
      }
    }).start();
  }

  /**
   * This method blocks until the last step is reached, unless
   * this method is called from the swing event thread.
   */
  public void walkToLastStep() {
    synchronized (mutex) {
      targetStep = LAST_STEP;
    }
    goToTarget();
  }

  /**
   * Set the target step for this panel
   * This method returns immediatly, and must be called from the swing event thread.
   */
  public void setStep(final String step) {
    synchronized (mutex) {
      if (listModel.indexOf(step) != -1) {
        targetStep = step;
      } else {
        logger.warning("Could not find step name:" + step);
      }
    }
    goToTarget();
  }

  private void goToTarget() {
    if (!SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("Not swing event thread");
    }
    waitThenWalk();
  }

  /**
   * Doesnt allow the user to change the selection, must be done through
   * hiddenSetSelectionInterval.
   */
  private static final class MyListSelectionModel extends DefaultListSelectionModel {
    private static final long serialVersionUID = -4359950441657840015L;

    @Override
    public void setSelectionInterval(final int index0, final int index1) {}

    void hiddenSetSelectionInterval(final int index) {
      super.setSelectionInterval(index, index);
    }
  }
}
