package games.strategy.engine.framework.headlessGameServer;

import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.startup.mc.ServerModel;
import games.strategy.engine.framework.startup.mc.SetupPanelModel;
import games.strategy.engine.framework.startup.ui.ServerSetupPanel;

import java.awt.Component;

/**
 * Setup panel model for headless server.
 * 
 * @author veqryn (Mark Christopher Duncan)
 * 
 */
public class HeadlessServerSetupPanelModel extends SetupPanelModel
{
	protected final Component m_ui;
	
	public HeadlessServerSetupPanelModel(final GameSelectorModel gameSelectorModel, final Component ui)
	{
		super(gameSelectorModel);
		m_ui = ui;
	}
	
	@Override
	public void showSelectType()
	{
		final ServerModel model = new ServerModel(m_gameSelectorModel, this, true);
		if (!model.createServerMessenger(m_ui))
		{
			model.cancel();
			return;
		}
		if (m_ui == null)
		{
			final HeadlessServerSetup serverSetup = new HeadlessServerSetup(model, m_gameSelectorModel);
			setGameTypePanel(serverSetup);
		}
		else
		{
			final ServerSetupPanel serverSetupPanel = new ServerSetupPanel(model, m_gameSelectorModel);
			setGameTypePanel(serverSetupPanel);
		}
	}
}
