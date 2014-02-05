package games.strategy.engine.framework.mapDownload;

import games.strategy.engine.EngineVersion;
import games.strategy.engine.framework.ui.background.BackgroundTaskRunner;
import games.strategy.net.DesktopUtilityBrowserLauncher;
import games.strategy.ui.Util;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Vector;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class DownloadMapDialog extends JDialog
{
	private static final long serialVersionUID = -4719699814187468325L;
	private JComboBox m_urlComboBox;
	private JButton m_listGamesButton;
	private JButton m_cancelButton;
	private JButton m_findMapsButton;
	private JLabel m_descriptionLabel;
	private static final String DOWNLOAD_SITES_PREF = "downloadSites";
	private final String FIRST_TIME_DOWNLOADING_PREF = "firstTimeDownloading." + EngineVersion.VERSION.toString();
	private final Frame owner;
	
	private DownloadMapDialog(final Frame owner)
	{
		super(owner, "Select Download Site", true);
		this.owner = owner;
		createComponents();
		layoutCoponents();
		setupListeners();
		setWidgetActivation();
	}
	
	private void createComponents()
	{
		m_listGamesButton = new JButton("List Games");
		m_cancelButton = new JButton("Cancel");
		m_findMapsButton = new JButton("Help...");
		m_urlComboBox = new JComboBox(getStoredDownloadSites());
		m_urlComboBox.setEditable(true);
		m_urlComboBox
					.setPrototypeDisplayValue("                                                                                                                                                                            ");
		m_descriptionLabel = new JLabel("<html>TripleA can download maps and games from any correctly configured website."
					+ "<br>Just type (or copy and paste) the website's map downloader xml link to the below box, then hit 'List Games'."
					+ "<br>You can download from multiple websites. For further instructions on how to use this feature, click 'Help...'</html>");
	}
	
	private void layoutCoponents()
	{
		setLayout(new BorderLayout());
		final JPanel buttonsPanel = new JPanel();
		add(buttonsPanel, BorderLayout.SOUTH);
		buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));
		buttonsPanel.add(Box.createGlue());
		buttonsPanel.add(m_cancelButton);
		buttonsPanel.add(m_findMapsButton);
		buttonsPanel.add(m_listGamesButton);
		buttonsPanel.add(Box.createGlue());
		buttonsPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
		final JPanel main = new JPanel();
		main.setBorder(new EmptyBorder(30, 30, 30, 30));
		main.setLayout(new BoxLayout(main, BoxLayout.X_AXIS));
		main.add(new JLabel("Select download site:"));
		main.add(m_urlComboBox);
		add(main, BorderLayout.CENTER);
		final JPanel intro = new JPanel();
		intro.add(m_descriptionLabel);
		add(intro, BorderLayout.NORTH);
	}
	
	private void setupListeners()
	{
		m_cancelButton.addActionListener(new AbstractAction()
		{
			private static final long serialVersionUID = 1410141388231981589L;
			
			public void actionPerformed(final ActionEvent e)
			{
				setVisible(false);
			}
		});
		m_listGamesButton.addActionListener(new AbstractAction()
		{
			private static final long serialVersionUID = -9051243018211311068L;
			
			public void actionPerformed(final ActionEvent event)
			{
				final String selectedUrl = (String) m_urlComboBox.getSelectedItem();
				if (selectedUrl == null || selectedUrl.trim().length() == 0)
				{
					Util.notifyError(m_cancelButton, "nothing selected");
					return;
				}
				final DownloadRunnable download = new DownloadRunnable(selectedUrl, true);
				BackgroundTaskRunner.runInBackground(getRootPane(), "Downloading....", download);
				if (download.getError() != null)
				{
					Util.notifyError(m_cancelButton, download.getError());
					return;
				}
				if (getPrefNode().getBoolean(FIRST_TIME_DOWNLOADING_PREF, true))
				{
					DesktopUtilityBrowserLauncher.openURL("http://tripleadev.1671093.n2.nabble.com/Download-Maps-Links-Hosting-Games-General-Information-tp4074312.html");
					getPrefNode().putBoolean(FIRST_TIME_DOWNLOADING_PREF, false);
					try
					{
						getPrefNode().flush();
					} catch (final BackingStoreException ex)
					{
						ex.printStackTrace();
					}
				}
				final List<DownloadFileDescription> downloads = download.getDownloads();
				if (downloads == null || downloads.isEmpty() || download.getError() != null)
				{
					Util.notifyError(m_cancelButton, download.getError());
					return;
				}
				addDownloadSites(selectedUrl.trim());
				setVisible(false);
				InstallMapDialog.installGames(owner, downloads);
			}
		});
		m_findMapsButton.addActionListener(new AbstractAction()
		{
			private static final long serialVersionUID = -4644282981460989512L;
			
			public void actionPerformed(final ActionEvent e)
			{
				try
				{
					DesktopUtilityBrowserLauncher.openURL("http://tripleadev.1671093.n2.nabble.com/Download-Maps-Links-Hosting-Games-General-Information-tp4074312.html");
				} catch (final Exception ex)
				{
					Util.notifyError(m_cancelButton, ex.getMessage());
					return;
				}
			}
		});
	}
	
	@SuppressWarnings("unchecked")
	public static Vector<String> getStoredDownloadSites()
	{
		final Preferences pref = getPrefNode();
		final byte[] stored = pref.getByteArray(DOWNLOAD_SITES_PREF, null);
		if (stored != null && stored.length > 0)
		{
			try
			{
				return (Vector<String>) new ObjectInputStream(new ByteArrayInputStream(stored)).readObject();
			} catch (final IOException e)
			{
				e.printStackTrace(System.out);
			} catch (final ClassNotFoundException e)
			{
				e.printStackTrace(System.out);
			}
		}
		// return new Vector();
		final Vector<String> mapVector = new Vector<String>();
		mapVector.add("http://downloads.sourceforge.net/project/tripleamaps/triplea_maps.xml"); // full map listing of all known maps
		return mapVector;
	}
	
	private static Preferences getPrefNode()
	{
		return Preferences.userNodeForPackage(DownloadMapDialog.class);
	}
	
	private static void addDownloadSites(final String url)
	{
		Vector<String> old = getStoredDownloadSites();
		old.remove(url);
		old.add(0, url);
		if (old.size() > 10)
		{
			old = new Vector<String>(old.subList(0, 10));
		}
		final ByteArrayOutputStream sink = new ByteArrayOutputStream();
		try
		{
			final ObjectOutputStream writer = new ObjectOutputStream(sink);
			writer.writeObject(old);
			writer.flush();
		} catch (final Exception e)
		{
			e.printStackTrace(System.out);
		}
		if (sink.toByteArray().length > 0)
		{
			getPrefNode().putByteArray(DOWNLOAD_SITES_PREF, sink.toByteArray());
		}
	}
	
	private void setWidgetActivation()
	{
	}
	
	public static void downloadGames(final JComponent parent)
	{
		final Frame parentFrame = JOptionPane.getFrameForComponent(parent);
		final DownloadMapDialog dia = new DownloadMapDialog(parentFrame);
		dia.pack();
		dia.setLocationRelativeTo(parentFrame);
		dia.setVisible(true);
	}
	
	public static void main(final String[] args)
	{
		downloadGames(null);
		// System.exit(-1);
	}
}
