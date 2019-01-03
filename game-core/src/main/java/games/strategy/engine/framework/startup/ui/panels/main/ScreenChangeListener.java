package games.strategy.engine.framework.startup.ui.panels.main;

import games.strategy.engine.framework.startup.ui.ISetupPanel;

/** Callback interface to change the screen to render a given @{code ISetupPanel} instance. */
public interface ScreenChangeListener {

  void screenChangeEvent(ISetupPanel newSetupPanel);
}
