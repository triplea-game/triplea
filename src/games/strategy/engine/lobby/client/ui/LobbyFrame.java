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

import games.strategy.engine.chat.Chat;
import games.strategy.engine.chat.ChatMessagePanel;
import games.strategy.engine.chat.ChatPlayerPanel;
import games.strategy.engine.chat.IPlayerActionFactory;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.lobby.client.LobbyClient;
import games.strategy.engine.lobby.client.login.LobbyServerProperties;
import games.strategy.engine.lobby.server.IModeratorController;
import games.strategy.engine.lobby.server.LobbyServer;
import games.strategy.engine.lobby.server.ModeratorController;
import games.strategy.net.IMessenger;
import games.strategy.net.IMessengerErrorListener;
import games.strategy.net.INode;
import games.strategy.util.EventThreadJOptionPane;
import games.strategy.util.MD5Crypt;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.SpinnerNumberModel;

public class LobbyFrame extends JFrame
{
	private final LobbyClient m_client;
	private final ChatMessagePanel m_chatMessagePanel;
	
	public LobbyFrame(final LobbyClient client, final LobbyServerProperties props)
	{
		super("TripleA Lobby");
		setIconImage(GameRunner.getGameIcon(this));
		m_client = client;
		setJMenuBar(new LobbyMenu(this));
		final Chat chat = new Chat(m_client.getMessenger(), LobbyServer.LOBBY_CHAT, m_client.getChannelMessenger(), m_client.getRemoteMessenger());
		m_chatMessagePanel = new ChatMessagePanel(chat);
		showServerMessage(props);
		m_chatMessagePanel.setShowTime(true);
		final ChatPlayerPanel chatPlayers = new ChatPlayerPanel(null);
		chatPlayers.addHiddenPlayerName(LobbyServer.ADMIN_USERNAME);
		chatPlayers.setChat(chat);
		chatPlayers.setPreferredSize(new Dimension(200, 600));
		chatPlayers.addActionFactory(new IPlayerActionFactory()
		{
			public List<Action> mouseOnPlayer(final INode clickedOn)
			{
				return createAdminActions(clickedOn);
			}
		});
		final LobbyGamePanel gamePanel = new LobbyGamePanel(m_client.getMessengers());
		final JSplitPane leftSplit = new JSplitPane();
		leftSplit.setOrientation(JSplitPane.VERTICAL_SPLIT);
		leftSplit.setTopComponent(gamePanel);
		leftSplit.setBottomComponent(m_chatMessagePanel);
		leftSplit.setResizeWeight(0.8);
		gamePanel.setPreferredSize(new Dimension(700, 200));
		m_chatMessagePanel.setPreferredSize(new Dimension(700, 400));
		final JSplitPane mainSplit = new JSplitPane();
		mainSplit.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
		mainSplit.setLeftComponent(leftSplit);
		mainSplit.setRightComponent(chatPlayers);
		add(mainSplit, BorderLayout.CENTER);
		pack();
		m_chatMessagePanel.requestFocusInWindow();
		setLocationRelativeTo(null);
		m_client.getMessenger().addErrorListener(new IMessengerErrorListener()
		{
			public void messengerInvalid(final IMessenger messenger, final Exception reason)
			{
				connectionToServerLost();
			}
		});
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(final WindowEvent e)
			{
				shutdown();
			}
		});
	}
	
	public ChatMessagePanel GetChatMessagePanel()
	{
		return m_chatMessagePanel;
	}
	
	private void showServerMessage(final LobbyServerProperties props)
	{
		if (props.getServerMessage() != null && props.getServerMessage().length() > 0)
		{
			m_chatMessagePanel.addServerMessage(props.getServerMessage());
		}
	}
	
	private List<Action> createAdminActions(final INode clickedOn)
	{
		if (!m_client.isAdmin())
			return Collections.emptyList();
		if (clickedOn.equals(m_client.getMessenger().getLocalNode()))
			return Collections.emptyList();
		final IModeratorController controller = (IModeratorController) m_client.getRemoteMessenger().getRemote(ModeratorController.getModeratorControllerName());
		final List<Action> rVal = new ArrayList<Action>();
		rVal.add(new AbstractAction("Boot " + clickedOn.getName())
		{
			public void actionPerformed(final ActionEvent e)
			{
				if (!confirm("Boot " + clickedOn.getName()))
				{
					return;
				}
				controller.boot(clickedOn);
			}
		});
		rVal.add(new AbstractAction("Ban Player")
		{
			public void actionPerformed(final ActionEvent e)
			{
				final List<String> banTypes = new ArrayList<String>();
				banTypes.add("IP Address");
				banTypes.add("Mac Address");
				banTypes.add("Username");
				banTypes.add("IP, Mac");
				banTypes.add("Name, IP");
				banTypes.add("Name, Mac");
				banTypes.add("Name, IP, Mac");
				banTypes.add("Cancel");
				final int resultBT = JOptionPane.showOptionDialog(LobbyFrame.this, "Select the type of ban: ", "Select Ban Type", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
							banTypes.toArray(), banTypes.toArray()[7]);
				if (resultBT < 0)
					return;
				final String selectedBanType = (String) banTypes.toArray()[resultBT];
				if (selectedBanType.equals("Cancel"))
					return;
				final List<String> timeUnits = new ArrayList<String>();
				timeUnits.add("Minute");
				timeUnits.add("Hour");
				timeUnits.add("Day");
				timeUnits.add("Week");
				timeUnits.add("Month");
				timeUnits.add("Year");
				timeUnits.add("Forever");
				timeUnits.add("Cancel");
				final int resultTU = JOptionPane.showOptionDialog(LobbyFrame.this, "Select the unit of measurement: ", "Select Timespan Unit", JOptionPane.OK_CANCEL_OPTION,
							JOptionPane.QUESTION_MESSAGE, null, timeUnits.toArray(), timeUnits.toArray()[7]);
				if (resultTU < 0)
					return;
				final String selectedTimeUnit = (String) timeUnits.toArray()[resultTU];
				if (selectedTimeUnit.equals("Cancel"))
					return;
				if (selectedTimeUnit.equals("Forever"))
				{
					if (selectedBanType.toLowerCase().contains("name"))
						controller.banUsername(clickedOn, null);
					if (selectedBanType.toLowerCase().contains("ip"))
						controller.banIp(clickedOn, null);
					if (selectedBanType.toLowerCase().contains("mac"))
						controller.banMac(clickedOn, null);
					controller.boot(clickedOn); // Should we keep this auto?
					return;
				}
				final String resultLOT = JOptionPane.showInputDialog(LobbyFrame.this, "Now please enter the length of time to ban the player: (In " + selectedTimeUnit + "s) ", 1);
				if (resultLOT == null)
					return;
				final long result2 = Long.parseLong(resultLOT);
				if (result2 < 0)
					return;
				long ticks = 0;
				if (selectedTimeUnit.equals("Minute"))
					ticks = result2 * 1000 * 60;
				else if (selectedTimeUnit.equals("Hour"))
					ticks = result2 * 1000 * 60 * 60;
				else if (selectedTimeUnit.equals("Day"))
					ticks = result2 * 1000 * 60 * 60 * 24;
				else if (selectedTimeUnit.equals("Week"))
					ticks = result2 * 1000 * 60 * 60 * 24 * 7;
				else if (selectedTimeUnit.equals("Month"))
					ticks = result2 * 1000 * 60 * 60 * 24 * 30;
				else if (selectedTimeUnit.equals("Year"))
					ticks = result2 * 1000 * 60 * 60 * 24 * 365;
				final long expire = System.currentTimeMillis() + ticks;
				if (selectedBanType.toLowerCase().contains("name"))
					controller.banUsername(clickedOn, new Date(expire));
				if (selectedBanType.toLowerCase().contains("ip"))
					controller.banIp(clickedOn, new Date(expire));
				if (selectedBanType.toLowerCase().contains("mac"))
					controller.banMac(clickedOn, new Date(expire));
				controller.boot(clickedOn); // Should we keep this auto?
			}
		});
		rVal.add(new AbstractAction("Mute Player")
		{
			public void actionPerformed(final ActionEvent e)
			{
				final List<String> muteTypes = new ArrayList<String>();
				muteTypes.add("IP Address");
				muteTypes.add("Mac Address");
				muteTypes.add("Username");
				muteTypes.add("IP, Mac");
				muteTypes.add("Name, IP");
				muteTypes.add("Name, Mac");
				muteTypes.add("Name, IP, Mac");
				muteTypes.add("Cancel");
				final int resultMT = JOptionPane.showOptionDialog(LobbyFrame.this, "Select the type of mute: ", "Select Mute Type", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
							muteTypes.toArray(), muteTypes.toArray()[7]);
				if (resultMT < 0)
					return;
				final String selectedMuteType = (String) muteTypes.toArray()[resultMT];
				if (selectedMuteType.equals("Cancel"))
					return;
				final List<String> timeUnits = new ArrayList<String>();
				timeUnits.add("Minute");
				timeUnits.add("Hour");
				timeUnits.add("Day");
				timeUnits.add("Week");
				timeUnits.add("Month");
				timeUnits.add("Year");
				timeUnits.add("Forever");
				timeUnits.add("Cancel");
				final int resultTU = JOptionPane.showOptionDialog(LobbyFrame.this, "Select the unit of measurement: ", "Select Timespan Unit", JOptionPane.OK_CANCEL_OPTION,
							JOptionPane.QUESTION_MESSAGE, null, timeUnits.toArray(), timeUnits.toArray()[7]);
				if (resultTU < 0)
					return;
				final String selectedTimeUnit = (String) timeUnits.toArray()[resultTU];
				if (selectedTimeUnit.equals("Cancel"))
					return;
				if (selectedTimeUnit.equals("Forever"))
				{
					if (selectedMuteType.toLowerCase().contains("name"))
						controller.muteUsername(clickedOn, null);
					if (selectedMuteType.toLowerCase().contains("ip"))
						controller.muteIp(clickedOn, null);
					if (selectedMuteType.toLowerCase().contains("mac"))
						controller.muteMac(clickedOn, null);
					return;
				}
				final String resultLOT = JOptionPane.showInputDialog(LobbyFrame.this, "Now please enter the length of time to mute the player: (In " + selectedTimeUnit + "s) ", 1);
				if (resultLOT == null)
					return;
				final long result2 = Long.parseLong(resultLOT);
				if (result2 < 0)
					return;
				long ticks = 0;
				if (selectedTimeUnit.equals("Minute"))
					ticks = result2 * 1000 * 60;
				else if (selectedTimeUnit.equals("Hour"))
					ticks = result2 * 1000 * 60 * 60;
				else if (selectedTimeUnit.equals("Day"))
					ticks = result2 * 1000 * 60 * 60 * 24;
				else if (selectedTimeUnit.equals("Week"))
					ticks = result2 * 1000 * 60 * 60 * 24 * 7;
				else if (selectedTimeUnit.equals("Month"))
					ticks = result2 * 1000 * 60 * 60 * 24 * 30;
				else if (selectedTimeUnit.equals("Year"))
					ticks = result2 * 1000 * 60 * 60 * 24 * 365;
				final long expire = System.currentTimeMillis() + ticks;
				if (selectedMuteType.toLowerCase().contains("name"))
					controller.muteUsername(clickedOn, new Date(expire));
				if (selectedMuteType.toLowerCase().contains("ip"))
					controller.muteIp(clickedOn, new Date(expire));
				if (selectedMuteType.toLowerCase().contains("mac"))
					controller.muteMac(clickedOn, new Date(expire));
			}
		});
		rVal.add(new AbstractAction("Quick Mute")
		{
			public void actionPerformed(final ActionEvent e)
			{
				final JLabel label = new JLabel("How many minutes should this player be muted?");
				final JSpinner spinner = new JSpinner(new SpinnerNumberModel(10, 0, 60 * 24 * 7, 1));
				final JPanel panel = new JPanel();
				panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
				panel.add(label);
				panel.add(spinner);
				if (JOptionPane.showConfirmDialog(LobbyFrame.this, panel, "Mute Player", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
				{
					final Object value = spinner.getValue();
					if (value == null)
						return;
					final long resultML = Long.parseLong(value.toString());
					if (resultML < 0)
						return;
					final long ticks = resultML * 1000 * 60;
					final long expire = System.currentTimeMillis() + ticks;
					controller.muteUsername(clickedOn, new Date(expire));
					controller.muteIp(clickedOn, new Date(expire));
					controller.muteMac(clickedOn, new Date(expire));
				}
			}
		});
		rVal.add(new AbstractAction("Show player information")
		{
			public void actionPerformed(final ActionEvent e)
			{
				final String text = controller.getInformationOn(clickedOn);
				final JTextPane textPane = new JTextPane();
				textPane.setEditable(false);
				textPane.setText(text);
				JOptionPane.showMessageDialog(null, textPane, "Player Info", JOptionPane.INFORMATION_MESSAGE);
			}
		});
		rVal.add(new AbstractAction("Reset password")
		{
			public void actionPerformed(final ActionEvent e)
			{
				final String newPassword = JOptionPane.showInputDialog(JOptionPane.getFrameForComponent(LobbyFrame.this), "Enter new password");
				if (newPassword == null || newPassword.length() < 2)
					return;
				final String error = controller.setPassword(clickedOn, MD5Crypt.crypt(newPassword));
				final String msg = error == null ? "Password set" : error;
				JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(LobbyFrame.this), msg);
			}
		});
		return rVal;
	}
	
	private boolean confirm(final String question)
	{
		final int rVal = JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(this), question, "Question", JOptionPane.OK_CANCEL_OPTION);
		return rVal == JOptionPane.OK_OPTION;
	}
	
	public LobbyClient getLobbyClient()
	{
		return m_client;
	}
	
	void setShowChatTime(final boolean showTime)
	{
		if (m_chatMessagePanel != null)
			m_chatMessagePanel.setShowTime(showTime);
	}
	
	void shutdown()
	{
		System.exit(0);
	}
	
	private void connectionToServerLost()
	{
		EventThreadJOptionPane.showMessageDialog(LobbyFrame.this, "Connection to Server Lost", "Connection Lost", JOptionPane.ERROR_MESSAGE, null);
	}
}
