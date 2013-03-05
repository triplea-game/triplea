package games.strategy.engine.framework.ui;

import games.strategy.engine.data.GameData;
import games.strategy.util.LocalizeHTML;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class NewGameChooser extends JDialog
{
	private static final long serialVersionUID = -3223711652118741132L;
	private static NewGameChooserModel s_cachedGameModel = null; // any methods touching s_cachedGameModel should be both static and synchronized
	private JButton m_okButton;
	private JButton m_cancelButton;
	private JButton m_refreshGamesButton;
	private JList m_gameList;
	private JPanel m_infoPanel;
	private JEditorPane m_notesPanel;
	private NewGameChooserModel m_gameListModel;
	private NewGameChooserEntry m_choosen;
	
	private NewGameChooser(final Frame owner)
	{
		super(owner, "Select a Game", true);
		createComponents();
		layoutCoponents();
		setupListeners();
		setWidgetActivation();
		updateInfoPanel();
	}
	
	private void createComponents()
	{
		m_okButton = new JButton("OK");
		m_cancelButton = new JButton("Cancel");
		m_refreshGamesButton = new JButton("Refresh Game List");
		m_gameListModel = getNewGameChooserModel();
		m_gameList = new JList(m_gameListModel);
		m_infoPanel = new JPanel();
		m_infoPanel.setLayout(new BorderLayout());
		m_notesPanel = new JEditorPane();
		m_notesPanel.setEditable(false);
		m_notesPanel.setContentType("text/html");
		m_notesPanel.setBackground(new JLabel().getBackground());
	}
	
	private void layoutCoponents()
	{
		setLayout(new BorderLayout());
		final JSplitPane mainSplit = new JSplitPane();
		add(mainSplit, BorderLayout.CENTER);
		final JScrollPane listScroll = new JScrollPane();
		listScroll.setBorder(null);
		listScroll.getViewport().setBorder(null);
		listScroll.setViewportView(m_gameList);
		final JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new GridBagLayout());
		final JLabel gamesLabel = new JLabel("Games");
		gamesLabel.setFont(gamesLabel.getFont().deriveFont(Font.BOLD, gamesLabel.getFont().getSize() + 2));
		leftPanel.add(gamesLabel, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
		leftPanel.add(listScroll, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(0, 10, 0, 0), 0, 0));
		mainSplit.setLeftComponent(leftPanel);
		mainSplit.setRightComponent(m_infoPanel);
		mainSplit.setBorder(null);
		listScroll.setMinimumSize(new Dimension(200, 0));
		final JPanel buttonsPanel = new JPanel();
		add(buttonsPanel, BorderLayout.SOUTH);
		buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));
		buttonsPanel.add(Box.createHorizontalStrut(30));
		buttonsPanel.add(m_refreshGamesButton);
		buttonsPanel.add(Box.createGlue());
		buttonsPanel.add(m_okButton);
		buttonsPanel.add(m_cancelButton);
		buttonsPanel.add(Box.createGlue());
		final JScrollPane notesScroll = new JScrollPane();
		notesScroll.setViewportView(m_notesPanel);
		notesScroll.setBorder(null);
		notesScroll.getViewport().setBorder(null);
		m_infoPanel.add(Box.createVerticalStrut(10), BorderLayout.NORTH);
		m_infoPanel.add(Box.createHorizontalStrut(10), BorderLayout.WEST);
		m_infoPanel.add(notesScroll, BorderLayout.CENTER);
	}
	
	public static NewGameChooserEntry chooseGame(final Frame parent, final String defaultGameName)
	{
		final NewGameChooser chooser = new NewGameChooser(parent);
		chooser.setSize(800, 600);
		chooser.setLocationRelativeTo(parent);
		if (defaultGameName != null)
		{
			chooser.selectGame(defaultGameName);
		}
		chooser.setVisible(true);
		// chooser is now visible and waits for user action
		final NewGameChooserEntry choosen = chooser.m_choosen;
		chooser.setVisible(false);
		chooser.dispose();
		return choosen;
	}
	
	private void selectGame(final String gameName)
	{
		if (gameName == null)
		{
			return;
		}
		final NewGameChooserEntry entry = m_gameListModel.findByName(gameName);
		if (entry != null)
		{
			m_gameList.setSelectedValue(entry, true);
		}
	}
	
	private void updateInfoPanel()
	{
		if (getSelected() != null)
		{
			final GameData data = getSelected().getGameData();
			final StringBuilder notes = new StringBuilder();
			notes.append("<h1>").append(data.getGameName()).append("</h1>");
			final String mapNameDir = data.getProperties().get("mapName", "");
			appendListItem("Map Name", mapNameDir, notes);
			appendListItem("Number Of Players", data.getPlayerList().size() + "", notes);
			appendListItem("Location", getSelected().getLocation() + "", notes);
			appendListItem("Version", data.getGameVersion() + "", notes);
			notes.append("<p></p>");
			final String notesProperty = data.getProperties().get("notes", "");
			if (notesProperty != null && notesProperty.trim().length() != 0)
			{
				// UIContext resource loader should be null (or potentially is still the last game we played's loader),
				// so we send the map dir name so that our localizing of image links can get a new resource loader if needed
				notes.append(LocalizeHTML.localizeImgLinksInHTML(notesProperty.trim(), null, mapNameDir));
			}
			m_notesPanel.setText(notes.toString());
		}
		else
		{
			m_notesPanel.setText("");
		}
		// scroll to the top of the notes screen
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				m_notesPanel.scrollRectToVisible(new Rectangle(0, 0, 0, 0));
			}
		});
	}
	
	private void appendListItem(final String title, final String value, final StringBuilder builder)
	{
		builder.append("<b>").append(title).append("</b>").append(": ").append(value).append("<br>");
	}
	
	private NewGameChooserEntry getSelected()
	{
		final int selected = m_gameList.getSelectedIndex();
		if (selected == -1)
		{
			return null;
		}
		return m_gameListModel.get(selected);
	}
	
	private void setWidgetActivation()
	{
	}
	
	private void setupListeners()
	{
		m_refreshGamesButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				refreshGameList();
			}
		});
		m_okButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				selectAndReturn();
			}
		});
		m_cancelButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				cancelAndReturn();
			}
		});
		m_gameList.addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(final ListSelectionEvent e)
			{
				updateInfoPanel();
			}
		});
		m_gameList.addMouseListener(new MouseListener()
		{
			public void mouseClicked(final MouseEvent event)
			{
				if (event.getClickCount() == 2)
				{
					selectAndReturn();
				}
			}
			
			public void mousePressed(final MouseEvent e)
			{
			} // ignore
			
			public void mouseReleased(final MouseEvent e)
			{
			} // ignore
			
			public void mouseEntered(final MouseEvent e)
			{
			} // ignore
			
			public void mouseExited(final MouseEvent e)
			{
			} // ignore
		});
	}
	
	// any methods touching s_cachedGameModel should be both static and synchronized
	public synchronized static NewGameChooserModel getNewGameChooserModel()
	{
		if (s_cachedGameModel == null)
		{
			refreshNewGameChooserModel();
		}
		return s_cachedGameModel;
	}
	
	// any methods touching s_cachedGameModel should be both static and synchronized
	public synchronized static void refreshNewGameChooserModel()
	{
		clearNewGameChooserModel();
		s_cachedGameModel = new NewGameChooserModel();
	}
	
	// any methods touching s_cachedGameModel should be both static and synchronized
	public synchronized static void clearNewGameChooserModel()
	{
		if (s_cachedGameModel != null)
		{
			s_cachedGameModel.clear();
			s_cachedGameModel = null;
		}
	}
	
	/**
	 * Refreshes the game list (from disk) then caches the new list
	 */
	private void refreshGameList()
	{
		m_gameList.setEnabled(false);
		final NewGameChooserEntry selected = getSelected();
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				try
				{
					refreshNewGameChooserModel();
					m_gameListModel = getNewGameChooserModel();
					m_gameList.setModel(m_gameListModel);
					if (selected != null)
					{
						final String name = selected.getGameData().getGameName();
						final NewGameChooserEntry found = m_gameListModel.findByName(name);
						if (name != null)
						{
							m_gameList.setSelectedValue(found, true);
						}
					}
				} finally
				{
					m_gameList.setEnabled(true);
				}
			}
		});
	}
	
	private void selectAndReturn()
	{
		m_choosen = getSelected();
		setVisible(false);
	}
	
	private void cancelAndReturn()
	{
		m_choosen = null;
		setVisible(false);
	}
	
	public static void main(final String[] args)
	{
		chooseGame(null, "Revised");
		System.exit(0);
	}
}
