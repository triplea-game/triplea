package games.strategy.engine.framework.mapDownload;

import games.strategy.engine.framework.ui.background.BackgroundTaskRunner;
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

public class DownloadMapDialog extends JDialog {

	private JComboBox m_urlComboBox;
    private JButton m_listGamesButton;
	private JButton m_cancelButton;
	private final String DOWNLOAD_SITES_PREF = "downloadSites";
	private final Frame owner;
	
	private DownloadMapDialog(Frame owner) {
		super(owner, "Select Download Site",  true);
		this.owner = owner;
		createComponents();
		layoutCoponents();
		setupListeners();
		setWidgetActivation();
		
	}

	private void createComponents() {
		m_listGamesButton = new JButton("List Games");
		m_cancelButton = new JButton("Cancel");

		m_urlComboBox = new JComboBox(getStoredDownloadSites());
		m_urlComboBox.setEditable(true);
		m_urlComboBox.setPrototypeDisplayValue(
				"                                                                                                                                                                            ");
	}
	


	private void layoutCoponents() {

		setLayout(new BorderLayout());
		
		JPanel buttonsPanel = new JPanel();		
		add(buttonsPanel, BorderLayout.SOUTH);		
		
		buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));
		
		buttonsPanel.add(Box.createGlue());		
		buttonsPanel.add(m_cancelButton);
		buttonsPanel.add(m_listGamesButton);
		buttonsPanel.add(Box.createGlue());
		buttonsPanel.setBorder(new EmptyBorder(20,20,20,20));

		JPanel main = new JPanel();
		main.setBorder(new EmptyBorder(30,30,30,30));
		main.setLayout(new BoxLayout(main, BoxLayout.X_AXIS));
		main.add(new JLabel("Select download site:"));
		main.add(m_urlComboBox);
		add(main, BorderLayout.CENTER);
	}
	
	

	private void setupListeners() {
		m_cancelButton.addActionListener(new AbstractAction() {
			
			public void actionPerformed(ActionEvent e) {
				setVisible(false);				
			}
		});

		m_listGamesButton.addActionListener(new AbstractAction() {
			
			public void actionPerformed(ActionEvent event) {
				final String selected = (String) m_urlComboBox.getSelectedItem();
				if(selected == null || selected.trim().length() == 0) {
					Util.notifyError(m_cancelButton, "nothing selected");
					return;
				}
				
				DownloadRunnable download = new DownloadRunnable(selected);
				
				BackgroundTaskRunner.runInBackground(getRootPane(), "Downloading....", download);
				
				if(download.getError() != null) {
					Util.notifyError(m_cancelButton, download.getError());
					return;
				}
								
				List<DownloadFileDescription> downloads;
				try
				{					
					downloads = new DownloadFileParser().parse(new ByteArrayInputStream(download.getContents()));					
					if(downloads.isEmpty()) {
						throw new IllegalStateException("No games listed.");
					}
				} catch(Exception e) {
					Util.notifyError(m_cancelButton, e.getMessage());
					return;
				}
				
				addDownloadSites(selected.trim());
				setVisible(false);
				InstallMapDialog.installGames(owner, downloads);
				
			}
		});
		
		
	}

	private Vector getStoredDownloadSites() {		
		Preferences pref = getPrefNode();
		byte[] stored = pref.getByteArray(DOWNLOAD_SITES_PREF, null);
		if(stored == null) {
			return new Vector();
		}
		try {
			return (Vector) new ObjectInputStream(new ByteArrayInputStream(stored)).readObject();
		} catch (IOException e) {
			e.printStackTrace(System.out);
		} catch (ClassNotFoundException e) {
			e.printStackTrace(System.out);
		}
		return new Vector();
	}

	private Preferences getPrefNode() {
		return Preferences.userNodeForPackage(DownloadMapDialog.class);
	}
	
	private void addDownloadSites(String url) {
		Vector old = getStoredDownloadSites();
		old.remove(url);
		old.add(0, url);
		
		if(old.size() > 10) {
			old = new Vector(old.subList(0, 10));
		}
		ByteArrayOutputStream sink = new ByteArrayOutputStream();
		try
		{
			ObjectOutputStream writer = new ObjectOutputStream(sink);
			writer.writeObject(old);
			writer.flush();
		} catch(Exception e) {
			e.printStackTrace(System.out);
		}
		if(sink.toByteArray().length > 0) {
			getPrefNode().putByteArray(DOWNLOAD_SITES_PREF, sink.toByteArray());	
		}
	}
	


	private void setWidgetActivation() {

	}


	public static void downloadGames(JComponent parent) {
		Frame parentFrame = JOptionPane.getFrameForComponent(parent);
		DownloadMapDialog dia = new DownloadMapDialog(parentFrame);
		dia.pack();
		dia.setLocationRelativeTo(parentFrame);
		dia.setVisible(true);
		
	}



	public static void main(String[] args) {
		downloadGames(null);
		//System.exit(-1);
	}
	
}


