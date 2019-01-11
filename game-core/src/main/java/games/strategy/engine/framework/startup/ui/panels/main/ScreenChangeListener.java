package games.strategy.engine.framework.startup.ui.panels.main;

import games.strategy.engine.framework.startup.ui.SetupPanel;

/**
 * Callback interface to change the screen to render a given @{code SetupPanel} instance.
 */
public interface ScreenChangeListener {

  void screenChangeEvent(SetupPanel newSetupPanel);
}
