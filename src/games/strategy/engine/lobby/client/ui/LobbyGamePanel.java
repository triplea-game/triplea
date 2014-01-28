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

import games.strategy.engine.framework.TripleAProcessRunner;
import games.strategy.engine.framework.startup.ui.InGameLobbyWatcher;
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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;

public class LobbyGamePanel extends JPanel
{
	private static final long serialVersionUID = -7829506356288575574L;
	private JButton m_hostGame;
	private JButton m_joinGame;
	private JButton m_bootGame;
	private LobbyGameTableModel m_gameTableModel;
	private final Messengers m_messengers;
	private JTable m_gameTable;
	private TableSorter m_tableSorter;
	
	public LobbyGamePanel(final Messengers messengers)
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
		final int dateColumn = m_gameTableModel.getColumnIndex(LobbyGameTableModel.Column.Started);
		m_tableSorter.setSortingStatus(dateColumn, TableSorter.DESCENDING);
		// these should add up to 700 at most
		m_gameTable.getColumnModel().getColumn(m_gameTableModel.getColumnIndex(LobbyGameTableModel.Column.Players)).setPreferredWidth(42);
		m_gameTable.getColumnModel().getColumn(m_gameTableModel.getColumnIndex(LobbyGameTableModel.Column.Round)).setPreferredWidth(40);
		m_gameTable.getColumnModel().getColumn(m_gameTableModel.getColumnIndex(LobbyGameTableModel.Column.P)).setPreferredWidth(12);
		m_gameTable.getColumnModel().getColumn(m_gameTableModel.getColumnIndex(LobbyGameTableModel.Column.B)).setPreferredWidth(12);
		m_gameTable.getColumnModel().getColumn(m_gameTableModel.getColumnIndex(LobbyGameTableModel.Column.GV)).setPreferredWidth(32);
		m_gameTable.getColumnModel().getColumn(m_gameTableModel.getColumnIndex(LobbyGameTableModel.Column.EV)).setPreferredWidth(42);
		m_gameTable.getColumnModel().getColumn(m_gameTableModel.getColumnIndex(LobbyGameTableModel.Column.Started)).setPreferredWidth(54);
		m_gameTable.getColumnModel().getColumn(m_gameTableModel.getColumnIndex(LobbyGameTableModel.Column.Status)).setPreferredWidth(110);
		m_gameTable.getColumnModel().getColumn(m_gameTableModel.getColumnIndex(LobbyGameTableModel.Column.Name)).setPreferredWidth(148);
		m_gameTable.getColumnModel().getColumn(m_gameTableModel.getColumnIndex(LobbyGameTableModel.Column.Comments)).setPreferredWidth(148);
		m_gameTable.getColumnModel().getColumn(m_gameTableModel.getColumnIndex(LobbyGameTableModel.Column.Host)).setPreferredWidth(60);
		m_gameTable.setDefaultRenderer(Date.class, new DefaultTableCellRenderer()
		{
			private static final long serialVersionUID = -2807387751127250972L;
			private final SimpleDateFormat format = new SimpleDateFormat("hh:mm a");
			
			@Override
			public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column)
			{
				super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				setText(format.format((Date) value));
				return this;
			}
		});
	}
	
	private void layoutComponents()
	{
		final JScrollPane scroll = new JScrollPane(m_gameTable);
		setLayout(new BorderLayout());
		add(scroll, BorderLayout.CENTER);
		final JToolBar toolBar = new JToolBar();
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
			public void actionPerformed(final ActionEvent e)
			{
				hostGame();
			}
		});
		m_joinGame.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				joinGame();
			}
		});
		m_bootGame.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				bootGame();
			}
		});
		m_gameTable.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(final ListSelectionEvent e)
			{
				setWidgetActivation();
			}
		});
		
		m_gameTable.addMouseListener(new MouseListener()
		{
			public void mouseClicked(final MouseEvent e)
			{
				if (e.getClickCount() == 2)
				{
					joinGame();
				}
				mouseOnGamesList(e);
			}
			
			public void mousePressed(final MouseEvent e)
			{
				// right clicks do not 'select' a row by default. so force a row selection at the mouse point.
				final int r = m_gameTable.rowAtPoint(e.getPoint());
				if (r >= 0 && r < m_gameTable.getRowCount())
					m_gameTable.setRowSelectionInterval(r, r);
				else
					m_gameTable.clearSelection();
				mouseOnGamesList(e);
			}
			
			public void mouseReleased(final MouseEvent e)
			{
				mouseOnGamesList(e);
			}
			
			public void mouseEntered(final MouseEvent e)
			{
			} // ignore
			
			public void mouseExited(final MouseEvent e)
			{
			} // ignore
		});
	}
	
	private void mouseOnGamesList(final MouseEvent e)
	{
		if (!e.isPopupTrigger())
			return;
		if (!SwingUtilities.isRightMouseButton(e))
			return;
		final int selectedIndex = m_gameTable.getSelectedRow();
		if (selectedIndex == -1)
			return;
		// we sort the table, so get the correct index
		final int modelIndex = m_tableSorter.modelIndex(selectedIndex);
		final GameDescription description = m_gameTableModel.get(modelIndex);
		final JPopupMenu menu = new JPopupMenu();
		boolean hasActions = false;
		for (final Action a : getGamesListRightClickActions(description))
		{
			if (a == null)
				continue;
			// if (hasActions)
			// menu.addSeparator();
			hasActions = true;
			menu.add(a);
		}
		if (hasActions)
			menu.show(m_gameTable, e.getX(), e.getY());
	}
	
	private List<Action> getGamesListRightClickActions(final GameDescription description)
	{
		final List<Action> rVal = new ArrayList<Action>();
		rVal.add(getJoinGameAction());
		rVal.add(getHostGameAction());
		if (isAdmin())
		{
			rVal.add(getHostSupportInfoAction(description));
			rVal.add(getBootGameAction());
		}
		return rVal;
	}
	
	private Action getHostSupportInfoAction(final GameDescription description)
	{
		final String supportEmail = description == null ? "" : description.getBotSupportEmail() == null ? "" : description.getBotSupportEmail();
		if (supportEmail.length() == 0)
			return null;
		final String text = "Support Email for this Host is as follows. "
					+ "\n(Please copy the email address below and manually email them ONLY if something is seriously "
					+ "\nwrong with the host, like it needs to be restarted because it is down and not working at all.) "
					+ "\n\nEmail: \n" + supportEmail;
		return new AbstractAction("Show Host Support Information/Email")
		{
			private static final long serialVersionUID = -8291828709227364205L;
			
			public void actionPerformed(final ActionEvent e)
			{
				final JTextPane textPane = new JTextPane();
				textPane.setEditable(false);
				textPane.setText(text);
				JOptionPane.showMessageDialog(null, textPane, "Host Support Info", JOptionPane.INFORMATION_MESSAGE);
			}
		};
	}
	
	private Action getJoinGameAction()
	{
		return new AbstractAction("Join Game")
		{
			private static final long serialVersionUID = 1238500960279152158L;
			
			public void actionPerformed(final ActionEvent e)
			{
				joinGame();
			}
		};
	}
	
	private Action getHostGameAction()
	{
		return new AbstractAction("Host Game")
		{
			private static final long serialVersionUID = 2256758711590833222L;
			
			public void actionPerformed(final ActionEvent e)
			{
				hostGame();
			}
		};
	}
	
	private Action getBootGameAction()
	{
		return new AbstractAction("Boot Game")
		{
			private static final long serialVersionUID = 2256758711590833222L;
			
			public void actionPerformed(final ActionEvent e)
			{
				bootGame();
			}
		};
	}
	
	private void joinGame()
	{
		final int selectedIndex = m_gameTable.getSelectedRow();
		if (selectedIndex == -1)
			return;
		// we sort the table, so get the correct index
		final int modelIndex = m_tableSorter.modelIndex(selectedIndex);
		final GameDescription description = m_gameTableModel.get(modelIndex);
		TripleAProcessRunner.joinGame(description, m_messengers, getParent());
	}
	
	protected void hostGame()
	{
		final ServerOptions options = new ServerOptions(JOptionPane.getFrameForComponent(this), m_messengers.getMessenger().getLocalNode().getName(), 3300, true);
		options.setLocationRelativeTo(JOptionPane.getFrameForComponent(this));
		options.setNameEditable(false);
		options.setVisible(true);
		if (!options.getOKPressed())
		{
			return;
		}
		TripleAProcessRunner.hostGame(options.getPort(), options.getName(), options.getComments(), options.getPassword(), m_messengers);
	}
	
	private void bootGame()
	{
		final int selectedIndex = m_gameTable.getSelectedRow();
		if (selectedIndex == -1)
			return;
		final int result = JOptionPane.showConfirmDialog(null, "Are you sure you want to disconnect the selected game?", "Remove Game From Lobby", JOptionPane.OK_CANCEL_OPTION);
		if (result != JOptionPane.OK_OPTION)
			return;
		// we sort the table, so get the correct index
		final int modelIndex = m_tableSorter.modelIndex(selectedIndex);
		final GameDescription description = m_gameTableModel.get(modelIndex);
		final INode lobbyWatcherNode = new Node(description.getHostedBy().getName() + "_" + InGameLobbyWatcher.LOBBY_WATCHER_NAME, description.getHostedBy().getAddress(),
					description.getHostedBy().getPort());
		final IModeratorController controller = (IModeratorController) m_messengers.getRemoteMessenger().getRemote(ModeratorController.getModeratorControllerName());
		controller.boot(lobbyWatcherNode);
		JOptionPane.showMessageDialog(null, "The game you selected has been disconnected from the lobby.");
	}
	
	private void setWidgetActivation()
	{
		final boolean selected = m_gameTable.getSelectedRow() >= 0;
		m_joinGame.setEnabled(selected);
	}
}
