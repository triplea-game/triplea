package games.strategy.engine.framework.mapDownload;

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
	private final String DOWNLOAD_SITES_PREF = "downloadSites";
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
				final DownloadRunnable download = new DownloadRunnable(selectedUrl);
				BackgroundTaskRunner.runInBackground(getRootPane(), "Downloading....", download);
				if (download.getError() != null)
				{
					Util.notifyError(m_cancelButton, download.getError());
					return;
				}
				List<DownloadFileDescription> downloads;
				try
				{
					downloads = new DownloadFileParser().parse(new ByteArrayInputStream(download.getContents()), selectedUrl);
					if (downloads.isEmpty())
					{
						throw new IllegalStateException("No games listed.");
					}
				} catch (final Exception e)
				{
					Util.notifyError(m_cancelButton, e.getMessage());
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
					DesktopUtilityBrowserLauncher.openURL("http://tripleadev.1671093.n2.nabble.com/Download-Maps-Links-Hosting-Games-General-Information-tp4074312p4074312.html");
				} catch (final Exception ex)
				{
					Util.notifyError(m_cancelButton, ex.getMessage());
					return;
				}
			}
		});
	}
	
	private Vector getStoredDownloadSites()
	{
		final Preferences pref = getPrefNode();
		final byte[] stored = pref.getByteArray(DOWNLOAD_SITES_PREF, null);
		if (stored == null)
		{
			/* Code to have a default map listing:  (choose only one)
			
			Vector mapVector = new Vector();
			mapVector.add(0, "http://sourceforge.net/projects/tripleamaps/files/basic_map_list.xml");  // basic map listing with no possibly copywritten maps
			mapVector.add(0, "http://downloads.sourceforge.net/project/tripleamaps/triplea_maps.xml");  // full map listing of all known maps
			return mapVector;
			 */
			return new Vector();
		}
		try
		{
			return (Vector) new ObjectInputStream(new ByteArrayInputStream(stored)).readObject();
		} catch (final IOException e)
		{
			e.printStackTrace(System.out);
		} catch (final ClassNotFoundException e)
		{
			e.printStackTrace(System.out);
		}
		return new Vector();
	}
	
	private Preferences getPrefNode()
	{
		return Preferences.userNodeForPackage(DownloadMapDialog.class);
	}
	
	private void addDownloadSites(final String url)
	{
		Vector old = getStoredDownloadSites();
		old.remove(url);
		old.add(0, url);
		if (old.size() > 10)
		{
			old = new Vector(old.subList(0, 10));
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
