package games.strategy.engine.framework.mapDownload;

import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.ui.NewGameChooserModel;
import games.strategy.engine.framework.ui.background.BackgroundTaskRunner;
import games.strategy.ui.Util;
import games.strategy.util.EventThreadJOptionPane;
import games.strategy.util.Version;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class InstallMapDialog extends JDialog
{
	private static final long serialVersionUID = -1542210716764178580L;
	private static final String DOWNLOAD_URL_PREFIX = "Location:";
	private JButton m_installButton;
	private JButton m_cancelButton;
	private final List<DownloadFileDescription> m_games;
	private JList m_gamesList;
	private JEditorPane m_descriptionPane;
	private JLabel m_urlLabel;
	
	private InstallMapDialog(final Frame owner, final List<DownloadFileDescription> games)
	{
		super(owner, "Select game to install", true);
		m_games = games;
		createComponents();
		layoutCoponents();
		setupListeners();
		setWidgetActivation();
	}
	
	private void createComponents()
	{
		m_installButton = new JButton("Install Game");
		m_cancelButton = new JButton("Cancel");
		final Vector<String> gameNames = new Vector<String>();
		for (final DownloadFileDescription d : m_games)
		{
			gameNames.add(d.getMapName());
		}
		m_gamesList = new JList(gameNames);
		m_gamesList.setSelectedIndex(0);
		m_gamesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		// correctly handle empty names
		final ListCellRenderer oldRenderer = m_gamesList.getCellRenderer();
		m_gamesList.setCellRenderer(new ListCellRenderer()
		{
			public Component getListCellRendererComponent(final JList list, Object value, final int index, final boolean isSelected, final boolean cellHasFocus)
			{
				if (value.toString().trim().length() == 0)
				{
					value = " ";
				}
				return oldRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			}
		});
		m_descriptionPane = new JEditorPane();
		m_descriptionPane.setEditable(false);
		m_descriptionPane.setContentType("text/html");
		m_descriptionPane.setBackground(new JLabel().getBackground());
		m_urlLabel = new JLabel(DOWNLOAD_URL_PREFIX);
	}
	
	private void layoutCoponents()
	{
		setLayout(new BorderLayout());
		final JPanel buttonsPanel = new JPanel();
		add(buttonsPanel, BorderLayout.SOUTH);
		buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));
		buttonsPanel.add(Box.createGlue());
		buttonsPanel.add(m_cancelButton);
		buttonsPanel.add(m_installButton);
		buttonsPanel.add(Box.createGlue());
		buttonsPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
		final JScrollPane descriptionScroll = new JScrollPane();
		descriptionScroll.setViewportView(m_descriptionPane);
		descriptionScroll.setBorder(null);
		descriptionScroll.getViewport().setBorder(null);
		final JScrollPane gamesScroll = new JScrollPane();
		gamesScroll.setViewportView(m_gamesList);
		gamesScroll.setBorder(null);
		gamesScroll.getViewport().setBorder(null);
		final JPanel main = new JPanel();
		main.setBorder(new EmptyBorder(30, 30, 30, 30));
		main.setLayout(new BorderLayout());
		main.add(gamesScroll, BorderLayout.WEST);
		main.add(descriptionScroll, BorderLayout.CENTER);
		main.add(m_urlLabel, BorderLayout.SOUTH);
		add(main, BorderLayout.CENTER);
	}
	
	private void setupListeners()
	{
		m_cancelButton.addActionListener(new AbstractAction()
		{
			private static final long serialVersionUID = -2437255215905705911L;
			
			public void actionPerformed(final ActionEvent e)
			{
				setVisible(false);
			}
		});
		m_installButton.addActionListener(new AbstractAction()
		{
			private static final long serialVersionUID = -2202445889252381183L;
			
			public void actionPerformed(final ActionEvent event)
			{
				install(getSelected());
			}
		});
		m_gamesList.addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(final ListSelectionEvent e)
			{
				setWidgetActivation();
			}
		});
	}
	
	private boolean isDefaultMap(final DownloadFileDescription selected)
	{
		return NewGameChooserModel.getDefaultMapNames().contains(selected.getMapName());
	}
	
	private void install(final DownloadFileDescription selected)
	{
		if (isDefaultMap(selected))
		{
			Util.notifyError(this, "The map " + selected.getMapName() + " cannot be downloaded as it comes installed with TripleA");
			return;
		}
		// get the destination file
		final File destination = new File(GameRunner.getUserMapsFolder(), selected.getMapName() + ".zip");
		if (destination.exists())
		{
			final String msg = "You have version " + getVersionString(getVersion(destination)) + " installed, replace with version " + getVersionString(selected.getVersion()) + "?";
			final int rVal = EventThreadJOptionPane.showConfirmDialog(this, msg, "Exit", JOptionPane.YES_NO_OPTION);
			if (rVal != JOptionPane.OK_OPTION)
				return;
			if (!destination.delete())
			{
				// TODO
				// we can't delete the file on windows
				// something is leaking a file descriptor to it
				// the source seems to be some caching in the java url libraries
				// called from the constructor of NewGameChooserEntry
				// we will overwrite, rather than delete the file
			}
		}
		final File tempFile = new File(System.getProperty("java.io.tmpdir"), "tadownload:" + UUID.randomUUID().toString());
		tempFile.deleteOnExit();
		final DownloadRunnable download = new DownloadRunnable(selected.getUrl());
		BackgroundTaskRunner.runInBackground(getRootPane(), "Downloading", download);
		if (download.getError() != null)
		{
			Util.notifyError(this, download.getError());
			return;
		}
		FileOutputStream sink = null;
		try
		{
			validateZip(download);
			sink = new FileOutputStream(tempFile);
			sink.write(download.getContents());
			sink.getFD().sync();
			final DownloadFileProperties props = new DownloadFileProperties();
			props.setFrom(selected);
			DownloadFileProperties.saveForZip(destination, props);
		} catch (final IOException e)
		{
			Util.notifyError(this, "Could not create write to temp file:" + e.getMessage());
			return;
		} finally
		{
			if (sink != null)
			{
				try
				{
					sink.close();
				} catch (final IOException e)
				{
					// ignore
				}
			}
		}
		// try to make sure it is a valid zip file
		try
		{
			final ZipInputStream zis = new ZipInputStream(new FileInputStream(tempFile));
			try
			{
				while (zis.getNextEntry() != null)
				{
					zis.read(new byte[128]);
				}
			} finally
			{
				zis.close();
			}
		} catch (final IOException e)
		{
			Util.notifyError(this, "Invalid zip file:" + e.getMessage());
			return;
		}
		// move it to the games folder
		// try to rename first
		if (!tempFile.renameTo(destination))
		{
			try
			{
				final FileInputStream source = new FileInputStream(tempFile);
				try
				{
					final FileOutputStream destSink = new FileOutputStream(destination);
					try
					{
						copy(destSink, source);
						destSink.getFD().sync();
					} finally
					{
						destSink.close();
					}
				} finally
				{
					source.close();
					tempFile.delete();
				}
			} catch (final IOException e)
			{
				Util.notifyError(this, e.getMessage());
				return;
			}
		}
		// TODO - asking the user to restart isn't good, we should find the cause of the error, maybe a windows thing?
		// https://sourceforge.net/tracker/?func=detail&aid=2981890&group_id=44492&atid=439737
		EventThreadJOptionPane.showMessageDialog(getRootPane(), "Map successfully installed, please restart TripleA before playing");
		setVisible(false);
	}
	
	private void validateZip(final DownloadRunnable download) throws IOException
	{
		// try to unzip it to make sure it is valid
		try
		{
			final ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(download.getContents()));
			ZipEntry ze;
			while ((ze = zis.getNextEntry()) != null)
			{
				// make sure we can read something from each stream
				ze.getSize();
				zis.read(new byte[512]);
			}
		} catch (final Exception e)
		{
			throw new IOException("zip file could not be opened, it may have been corrupted during the download, please try again");
		}
	}
	
	public static void copy(final OutputStream sink, final InputStream is) throws IOException
	{
		final byte[] b = new byte[8192];
		int read;
		while ((read = is.read(b)) != -1)
		{
			sink.write(b, 0, read);
		}
	}
	
	private void setWidgetActivation()
	{
		if (m_gamesList.isSelectionEmpty())
		{
			m_installButton.setEnabled(false);
			m_descriptionPane.setText("");
			m_urlLabel.setText(DOWNLOAD_URL_PREFIX);
		}
		else
		{
			final DownloadFileDescription selected = getSelected();
			m_installButton.setEnabled(!selected.isDummyUrl());
			m_descriptionPane.setText(selected.getDescription());
			if (!selected.isDummyUrl())
			{
				m_urlLabel.setText(DOWNLOAD_URL_PREFIX + selected.getUrl());
			}
			// scroll to the top of the notes screen
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					m_descriptionPane.scrollRectToVisible(new Rectangle(0, 0, 0, 0));
				}
			});
		}
	}
	
	private DownloadFileDescription getSelected()
	{
		DownloadFileDescription selected = null;
		for (final DownloadFileDescription d : m_games)
		{
			if (d.getMapName().equals(m_gamesList.getSelectedValue()))
			{
				selected = d;
			}
		}
		return selected;
	}
	
	public static void installGames(final Component parent, final List<DownloadFileDescription> games)
	{
		final Frame parentFrame = JOptionPane.getFrameForComponent(parent);
		final InstallMapDialog dia = new InstallMapDialog(parentFrame, games);
		dia.setSize(800, 600);
		dia.setLocationRelativeTo(parentFrame);
		dia.setVisible(true);
	}
	
	private static String getVersionString(final Version v)
	{
		if (v == null)
		{
			return "Unknown";
		}
		return v.toString();
	}
	
	private static Version getVersion(final File zipFile)
	{
		final DownloadFileProperties props = DownloadFileProperties.loadForZip(zipFile);
		return props.getVersion();
	}
}
