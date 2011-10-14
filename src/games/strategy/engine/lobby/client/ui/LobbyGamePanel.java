/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package games.strategy.engine.lobby.client.ui;

import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.GameRunner2;
import games.strategy.engine.framework.startup.ui.ServerOptions;
import games.strategy.engine.lobby.server.GameDescription;
import games.strategy.engine.lobby.server.IModeratorController;
import games.strategy.engine.lobby.server.ModeratorController;
import games.strategy.net.INode;
import games.strategy.net.Messengers;
import games.strategy.net.Node;
import games.strategy.ui.TableSorter;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;

public class LobbyGamePanel extends JPanel
{
	private JButton m_hostGame;
	private JButton m_joinGame;
	private JButton m_bootGame;
	private LobbyGameTableModel m_gameTableModel;
	private Messengers m_messengers;
	private JTable m_gameTable;
	private TableSorter m_tableSorter;
	
	public LobbyGamePanel(Messengers messengers)
	{
		m_messengers = messengers;
		createComponents();
		layoutComponents();
		setupListeners();
		setWidgetActivation();
	}
	
	private void createComponents()
	{
		m_hostGame = new JButton("Host Game");
		m_joinGame = new JButton("Join Game");
		m_bootGame = new JButton("Boot Game");
		m_gameTableModel = new LobbyGameTableModel(m_messengers.getMessenger(), m_messengers.getChannelMessenger(), m_messengers.getRemoteMessenger());
		
		m_tableSorter = new TableSorter(m_gameTableModel);
		m_gameTable = new LobbyGameTable(m_tableSorter);
		m_tableSorter.setTableHeader(m_gameTable.getTableHeader());
		
		// only allow one row to be selected
		m_gameTable.setColumnSelectionAllowed(false);
		m_gameTable.setRowSelectionAllowed(true);
		m_gameTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		// by default, sort newest first
		int dateColumn = m_gameTableModel.getColumnIndex(LobbyGameTableModel.Column.Started);
		m_tableSorter.setSortingStatus(dateColumn, TableSorter.DESCENDING);
		
		m_gameTable.getColumnModel().getColumn(m_gameTableModel.getColumnIndex(LobbyGameTableModel.Column.Players)).setPreferredWidth(65);
		m_gameTable.getColumnModel().getColumn(m_gameTableModel.getColumnIndex(LobbyGameTableModel.Column.Status)).setPreferredWidth(150);
		m_gameTable.getColumnModel().getColumn(m_gameTableModel.getColumnIndex(LobbyGameTableModel.Column.Name)).setPreferredWidth(150);
		m_gameTable.getColumnModel().getColumn(m_gameTableModel.getColumnIndex(LobbyGameTableModel.Column.Comments)).setPreferredWidth(150);
		
		m_gameTable.setDefaultRenderer(Date.class, new DefaultTableCellRenderer()
			{
				
				private SimpleDateFormat format = new SimpleDateFormat("hh:mm a");
				
				@Override
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
			{
				super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				setText(format.format((Date) value));
				return this;
			}
				
			});
		
	}
	
	private void layoutComponents()
	{
		JScrollPane scroll = new JScrollPane(m_gameTable);
		setLayout(new BorderLayout());
		add(scroll, BorderLayout.CENTER);
		
		JToolBar toolBar = new JToolBar();
		
		toolBar.add(m_hostGame);
		toolBar.add(m_joinGame);
		if (isAdmin())
			toolBar.add(m_bootGame);
		toolBar.setFloatable(false);
		
		add(toolBar, BorderLayout.SOUTH);
	}
	
	public boolean isAdmin()
	{
		return ((IModeratorController) m_messengers.getRemoteMessenger().getRemote(ModeratorController.getModeratorControllerName())).isAdmin();
	}
	
	private void setupListeners()
	{
		m_hostGame.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				hostGame();
			}
			
		});
		
		m_joinGame.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				joinGame();
			}
			
		});
		
		m_bootGame.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				bootGame();
			}
		});
		
		m_gameTable.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			
			@Override
			public void valueChanged(ListSelectionEvent e)
			{
				setWidgetActivation();
			}
			
		});
		
	}
	
	private void joinGame()
	{
		int selectedIndex = m_gameTable.getSelectedRow();
		if (selectedIndex == -1)
			return;
		
		// we sort the table, so get the correct index
		int modelIndex = m_tableSorter.modelIndex(selectedIndex);
		GameDescription description = m_gameTableModel.get(modelIndex);
		
		List<String> commands = new ArrayList<String>();
		populateBasicJavaArgs(commands);
		
		commands.add("-D" + GameRunner2.TRIPLEA_CLIENT_PROPERTY + "=true");
		commands.add("-D" + GameRunner2.TRIPLEA_PORT_PROPERTY + "=" + description.getPort());
		commands.add("-D" + GameRunner2.TRIPLEA_HOST_PROPERTY + "=" + description.getHostedBy().getAddress().getHostAddress());
		commands.add("-D" + GameRunner2.TRIPLEA_NAME_PROPERTY + "=" + m_messengers.getMessenger().getLocalNode().getName());
		
		String javaClass = "games.strategy.engine.framework.GameRunner";
		commands.add(javaClass);
		
		exec(commands);
		
	}
	
	protected void hostGame()
	{
		ServerOptions options = new ServerOptions(JOptionPane.getFrameForComponent(this), m_messengers.getMessenger().getLocalNode().getName(), 3300, true);
		options.setLocationRelativeTo(JOptionPane.getFrameForComponent(this));
		options.setNameEditable(false);
		options.setVisible(true);
		if (!options.getOKPressed())
		{
			return;
		}
		
		List<String> commands = new ArrayList<String>();
		populateBasicJavaArgs(commands);
		
		commands.add("-D" + GameRunner2.TRIPLEA_SERVER_PROPERTY + "=true");
		commands.add("-D" + GameRunner2.TRIPLEA_PORT_PROPERTY + "=" + options.getPort());
		commands.add("-D" + GameRunner2.TRIPLEA_NAME_PROPERTY + "=" + options.getName());
		
		commands.add("-D" + GameRunner2.LOBBY_HOST + "=" + m_messengers.getMessenger().getRemoteServerSocketAddress().getAddress().getHostAddress());
		commands.add("-D" + GameRunner2.LOBBY_PORT + "=" + m_messengers.getMessenger().getRemoteServerSocketAddress().getPort());
		commands.add("-D" + GameRunner2.LOBBY_GAME_COMMENTS + "=" + options.getComments());
		commands.add("-D" + GameRunner2.LOBBY_GAME_HOSTED_BY + "=" + m_messengers.getMessenger().getLocalNode().getName());
		
		if (options.getPassword() != null && options.getPassword().length() > 0)
			commands.add("-D" + GameRunner2.TRIPLEA_SERVER_PASSWORD_PROPERTY + "=" + options.getPassword());
		
		String javaClass = "games.strategy.engine.framework.GameRunner";
		commands.add(javaClass);
		
		exec(commands);
	}
	
	private void bootGame()
	{
		int result = JOptionPane.showConfirmDialog(null, "Are you sure you want to disconnect the selected game?", "Remove Game From Lobby", JOptionPane.OK_CANCEL_OPTION);
		
		if (result != JOptionPane.OK_OPTION)
			return;
		
		int selectedIndex = m_gameTable.getSelectedRow();
		if (selectedIndex == -1)
			return;
		
		// we sort the table, so get the correct index
		int modelIndex = m_tableSorter.modelIndex(selectedIndex);
		GameDescription description = m_gameTableModel.get(modelIndex);
		
		INode lobbyWatcherNode = new Node(description.getHostedBy().getName() + "_lobby_watcher", description.getHostedBy().getAddress(), description.getHostedBy().getPort());
		final IModeratorController controller = (IModeratorController) m_messengers.getRemoteMessenger().getRemote(ModeratorController.getModeratorControllerName());
		controller.boot(lobbyWatcherNode);
		JOptionPane.showMessageDialog(null, "The game you selected has been disconnected from the lobby.");
	}
	
	private void exec(List<String> commands)
	{
		ProcessBuilder builder = new ProcessBuilder(commands);
		// merge the streams, so we only have to start one reader thread
		builder.redirectErrorStream(true);
		try
		{
			Process p = builder.start();
			final InputStream s = p.getInputStream();
			
			// we need to read the input stream to prevent possible
			// deadlocks
			Thread t = new Thread(new Runnable()
			{
				
				@Override
				public void run()
				{
					try
					{
						while (s.read() >= 0)
						{
							// just read
						}
					} catch (IOException e)
					{
						e.printStackTrace();
					}
				}
				
			}, "Process ouput gobbler");
			
			t.setDaemon(true);
			t.start();
			
		} catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
	}
	
	private static void populateBasicJavaArgs(List<String> commands)
	{
		String javaCommand = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
		commands.add(javaCommand);
		
		commands.add("-classpath");
		commands.add(System.getProperty("java.class.path"));
		commands.add("-Xmx128m");
		
		// preserve noddraw to fix 1742775
		String[] preservedSystemProperties = { "sun.java2d.noddraw" };
		for (String key : preservedSystemProperties)
		{
			if (System.getProperties().getProperty(key) != null)
			{
				String value = System.getProperties().getProperty(key);
				if (value.matches("[a-zA-Z0-9.]+"))
				{
					commands.add("-D" + key + "=" + value);
				}
				
			}
		}
		
		if (GameRunner.isMac())
		{
			commands.add("-Dapple.laf.useScreenMenuBar=true");
			
			commands.add("-Xdock:name=\"TripleA\"");
			
			File icons = new File(GameRunner.getRootFolder(), "icons/triplea_icon.png");
			if (!icons.exists())
				throw new IllegalStateException("Icon file not found");
			
			commands.add("-Xdock:icon=" + icons.getAbsolutePath() + "");
		}
	}
	
	private void setWidgetActivation()
	{
		boolean selected = m_gameTable.getSelectedRow() >= 0;
		m_joinGame.setEnabled(selected);
	}
	
}
