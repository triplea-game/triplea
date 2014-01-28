package games.strategy.engine.framework.networkMaintenance;

import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.data.properties.PropertiesUI;
import games.strategy.engine.framework.startup.mc.IServerStartupRemote;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

/**
 * 
 * @author veqryn
 * 
 */
public class ChangeGameOptionsClientAction extends AbstractAction
{
	private static final long serialVersionUID = -6419002646689952824L;
	private final Component m_parent;
	private final IServerStartupRemote m_serverRemote;
	
	public ChangeGameOptionsClientAction(final Component parent, final IServerStartupRemote serverRemote)
	{
		super("Edit Game Options...");
		m_parent = JOptionPane.getFrameForComponent(parent);
		m_serverRemote = serverRemote;
	}
	
	public void actionPerformed(final ActionEvent e)
	{
		final byte[] oldBytes = m_serverRemote.getGameOptions();
		if (oldBytes == null || oldBytes.length == 0)
			return;
		try
		{
			final List<IEditableProperty> properties = GameProperties.streamToIEditablePropertiesList(oldBytes);
			final PropertiesUI pui = new PropertiesUI(properties, true);
			final JScrollPane scroll = new JScrollPane(pui);
			scroll.setBorder(null);
			scroll.getViewport().setBorder(null);
			final JOptionPane pane = new JOptionPane(scroll, JOptionPane.PLAIN_MESSAGE);
			final String ok = "OK";
			final String cancel = "Cancel";
			pane.setOptions(new Object[] { ok, cancel });
			final JDialog window = pane.createDialog(JOptionPane.getFrameForComponent(m_parent), "Game Options");
			window.setVisible(true);
			final Object buttonPressed = pane.getValue();
			if (buttonPressed == null || buttonPressed.equals(cancel))
			{
				return;
			}
			else
			{
				// ok was clicked. changing them in the ui changes the underlying properties, but it doesn't change the hosts, so we need to send it back to the host.
				final ByteArrayOutputStream sink = new ByteArrayOutputStream(1000);
				byte[] newBytes = null;
				try
				{
					GameProperties.toOutputStream(sink, properties);
					newBytes = sink.toByteArray();
				} catch (final IOException e2)
				{
					e2.printStackTrace();
				} finally
				{
					try
					{
						sink.close();
					} catch (final IOException e2)
					{
					}
				}
				if (newBytes != null)
				{
					m_serverRemote.changeToGameOptions(newBytes);
				}
			}
		} catch (final ClassNotFoundException e1)
		{
			e1.printStackTrace();
			return;
		} catch (final ClassCastException e1)
		{
			e1.printStackTrace();
			return;
		} catch (final IOException e1)
		{
			e1.printStackTrace();
			return;
		}
	}
}
