package games.strategy.engine.framework.networkMaintenance;

import games.strategy.common.ui.BasicGameMenuBar;
import games.strategy.engine.framework.startup.mc.IServerStartupRemote;

import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

/**
 * 
 * @author veqryn
 * 
 */
public class GetGameSaveClientAction extends AbstractAction
{
	private static final long serialVersionUID = 1118264715230932068L;
	private final Component m_parent;
	private final IServerStartupRemote m_serverRemote;
	
	public GetGameSaveClientAction(final Component parent, final IServerStartupRemote serverRemote)
	{
		super("Download Gamesave (Save Game)...");
		m_parent = JOptionPane.getFrameForComponent(parent);
		m_serverRemote = serverRemote;
	}
	
	public void actionPerformed(final ActionEvent e)
	{
		final Frame frame = JOptionPane.getFrameForComponent(m_parent);
		final File f = BasicGameMenuBar.getSaveGameLocationDialog(frame);
		if (f != null)
		{
			final byte[] bytes = m_serverRemote.getSaveGame();
			FileOutputStream fout = null;
			try
			{
				fout = new FileOutputStream(f);
				fout.write(bytes);
			} catch (final IOException exception)
			{
				exception.printStackTrace();
			} finally
			{
				if (fout != null)
					try
					{
						fout.close();
					} catch (final IOException exception)
					{
						exception.printStackTrace();
					}
			}
			JOptionPane.showMessageDialog(frame, "Game Saved", "Game Saved", JOptionPane.INFORMATION_MESSAGE);
		}
	}
}
