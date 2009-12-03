package games.strategy.engine.framework.mapDownload;

import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.ui.NewGameChooserModel;
import games.strategy.engine.framework.ui.background.BackgroundTaskRunner;
import games.strategy.ui.Util;
import games.strategy.util.EventThreadJOptionPane;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;
import java.util.Vector;
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

public class InstallMapDialog extends JDialog {

	
    private static final String DOWNLOAD_URL_PREFIX = "Location:";
	private JButton m_installButton;
	private JButton m_cancelButton;
	private List<DownloadFileDescription> m_games;
	private JList m_gamesList;
	private JEditorPane m_descriptionPane;
	private JLabel m_urlLabel;
	
	private InstallMapDialog(Frame owner, List<DownloadFileDescription> games) {
		super(owner, "Select game to install",  true);
		m_games = games;
		
		createComponents();
		layoutCoponents();
		setupListeners();
		setWidgetActivation();
		
	}

	private void createComponents() {
		m_installButton = new JButton("Install Game");
		m_cancelButton = new JButton("Cancel");

		Vector<String> gameNames = new Vector<String>();
		for(DownloadFileDescription d : m_games) {			
			gameNames.add(d.getMapName());			
		}
		
		m_gamesList = new JList(gameNames);
		m_gamesList.setSelectedIndex(0);
		m_gamesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		//correctly handle empty names
		final ListCellRenderer oldRenderer = m_gamesList.getCellRenderer(); 
		m_gamesList.setCellRenderer(new ListCellRenderer() {
			
			public Component getListCellRendererComponent(JList list, Object value,
					int index, boolean isSelected, boolean cellHasFocus) {
				if(value.toString().trim().isEmpty())
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
	


	private void layoutCoponents() {

		setLayout(new BorderLayout());
		
		JPanel buttonsPanel = new JPanel();		
		add(buttonsPanel, BorderLayout.SOUTH);		
		
		buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));
		
		buttonsPanel.add(Box.createGlue());
		
		buttonsPanel.add(m_cancelButton);
		buttonsPanel.add(m_installButton);
		buttonsPanel.add(Box.createGlue());
		buttonsPanel.setBorder(new EmptyBorder(20,20,20,20));

		JScrollPane descriptionScroll = new JScrollPane();		
		descriptionScroll.setViewportView(m_descriptionPane);
		descriptionScroll.setBorder(null);
        descriptionScroll.getViewport().setBorder(null);
		
        JScrollPane gamesScroll = new JScrollPane();		
		gamesScroll.setViewportView(m_gamesList);
		gamesScroll.setBorder(null);
        gamesScroll.getViewport().setBorder(null);
		
        
        
		JPanel main = new JPanel();
		main.setBorder(new EmptyBorder(30,30,30,30));
		main.setLayout(new BorderLayout());
		main.add(gamesScroll, BorderLayout.WEST);
		main.add(descriptionScroll, BorderLayout.CENTER);
		main.add(m_urlLabel, BorderLayout.SOUTH);
		
		add(main, BorderLayout.CENTER);
		
		
	}
	


	private void setupListeners() {
		m_cancelButton.addActionListener(new AbstractAction() {
			
			public void actionPerformed(ActionEvent e) {
				setVisible(false);				
			}
		});
		
		m_installButton.addActionListener(new AbstractAction() {
			
			public void actionPerformed(ActionEvent event) {
				install(getSelected());
			}
				
		});
		
		m_gamesList.addListSelectionListener(new ListSelectionListener() {
			
			public void valueChanged(ListSelectionEvent e) {
				setWidgetActivation();				
			}
		});
	}

	private boolean isDefaultMap(DownloadFileDescription selected) {
		return NewGameChooserModel.getDefaultMapNames().contains(selected.getMapName());
	}
	
	private void install(DownloadFileDescription selected) {
		
		
		if(isDefaultMap(selected)) {
			Util.notifyError(this, "The map " + selected.getMapName() + " cannot be downloaded as it comes installed with TripleA");
			return;
		}
		
		//get the destination file
		File destination = new File(GameRunner.getUserMapsFolder(), selected.getMapName() + ".zip");
		if(destination.exists()) {
			int rVal = EventThreadJOptionPane.showConfirmDialog(this, "A map with this name already exists, delete it?", "Exit" , JOptionPane.YES_NO_OPTION);
	        if(rVal != JOptionPane.OK_OPTION)
	            return;
			
			if(!destination.delete()) {
				//TODO
				//we can't delete the file on windows
				//something is leaking a file descriptor to it
				//the source seems to be some caching in the java url libraries
				//called from the constructor of NewGameChooserEntry
				//we will overwrite, rather than delete the file
			}
		}
		
		File tempFile = new 
			File(System.getProperty("java.io.tmpdir"), "tadownload:" +UUID.randomUUID().toString());
		tempFile.deleteOnExit();
		
		
		
		
		DownloadRunnable download = new DownloadRunnable(selected.getUrl());
		BackgroundTaskRunner.runInBackground(getRootPane(), "Downloading", download);
		if(download.getError() != null) {
			Util.notifyError(this, download.getError());
			return;
		}
		
		FileOutputStream sink = null;
		try {
			
			sink = new FileOutputStream(tempFile);
			sink.write(download.getContents());
			sink.getFD().sync();
		} catch (IOException e) {
			Util.notifyError(this, "Could not create write to temp file:" + e.getMessage());
			return;
		} finally { 
			if(sink != null) {
				try {
					sink.close();
				} catch (IOException e) {
					//ignore
				}
			}
		}
				
		//try to make sure it is a valid zip file
		try
		{			
			 ZipInputStream zis = new ZipInputStream(new FileInputStream(tempFile));			 			 
			 try
			 {
				 while(zis.getNextEntry() != null) {
					 zis.read(new byte[128]);
				 }	 
			 } finally { 
				 zis.close();
			 }
		} catch(IOException e) {
			Util.notifyError(this, "Invalid zip file:" + e.getMessage());
			return;
		}
		
		
		//move it to the games folder
		//try to rename first
		if(!tempFile.renameTo(destination)) {
			try
			{
				FileInputStream source = new FileInputStream(tempFile);
				try
				{
					FileOutputStream destSink = new FileOutputStream(destination);			
					try
					{
						copy(destSink, source);
						destSink.getFD().sync();
					} finally {
						destSink.close();
					}
				} finally {
					source.close();
					tempFile.delete();
				}
			} catch(IOException e) {
				Util.notifyError(this, e.getMessage());
				return;
			}
			
		}
		
		EventThreadJOptionPane.showMessageDialog(getRootPane(), "Map successfully installed");
		setVisible(false);
	}

	public static void copy(OutputStream sink, InputStream is) throws IOException {
		byte[] b = new byte[8192];  
		int read;  
		while ((read = is.read(b)) != -1) {  
			sink.write(b, 0, read);  
		}
	}

	private void setWidgetActivation() {
		if(m_gamesList.isSelectionEmpty()) {
			m_installButton.setEnabled(false);
			m_descriptionPane.setText("");
			m_urlLabel.setText(DOWNLOAD_URL_PREFIX);
			
		} else {
			
			DownloadFileDescription selected = getSelected();
						
			m_installButton.setEnabled(!selected.isDummyUrl());
							
			m_descriptionPane.setText(selected.getDescription());
			
			if(!selected.isDummyUrl()) {
				m_urlLabel.setText(DOWNLOAD_URL_PREFIX + selected.getUrl());
			}
			
			//scroll to the top of the notes screen
			SwingUtilities.invokeLater(new Runnable()
	        {        
	            public void run()
	            {         
	            	m_descriptionPane.scrollRectToVisible(new Rectangle(0,0,0,0));
	            }
	        
	        });
		}
	}

	private DownloadFileDescription getSelected() {
		DownloadFileDescription selected = null;
		for(DownloadFileDescription d : m_games) { 
			if(d.getMapName().equals(m_gamesList.getSelectedValue())) {
				selected = d;
			}
		}
		return selected;
	}


	public static void installGames(Component parent, List<DownloadFileDescription> games) {
		Frame parentFrame = JOptionPane.getFrameForComponent(parent);
		InstallMapDialog dia = new InstallMapDialog(parentFrame, games);
		dia.setSize(800,600);
		dia.setLocationRelativeTo(parentFrame);
		dia.setVisible(true);
		
	}

	
	
}


