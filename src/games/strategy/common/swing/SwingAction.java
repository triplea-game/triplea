package games.strategy.common.swing;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;


public class SwingAction {

  @FunctionalInterface
  public static interface SwingActionFunction {
   public void doStuff(ActionEvent e);
  }


  public static AbstractAction of(final String name, final SwingActionFunction swingAction) {
    return new AbstractAction(name) {
      private static final long serialVersionUID = 1L;

      @Override
      public void actionPerformed(ActionEvent e) {
        swingAction.doStuff(e);
      }
    };
  }

  public static AbstractAction of(final SwingActionFunction swingAction) {
    return new AbstractAction() {
      private static final long serialVersionUID = 12331L;

      @Override
      public void actionPerformed(ActionEvent e) {
        swingAction.doStuff(e);
      }
    };
  }

}