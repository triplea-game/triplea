package games.strategy.engine.framework.ui;

import games.strategy.engine.data.GameData;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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

public class NewGameChooser extends JDialog {

	
    private JButton m_okButton;
	private JButton m_cancelButton;
	private JList m_gameList;
	private JPanel m_infoPanel;
	private JEditorPane m_notesPanel;
	private NewGameChooserModel m_gameListModel;
	
	private JLabel m_gameLocationLabel;
	private JLabel m_mapNameLabel;
	private JLabel m_numberOfPlayersLabel;
	private JLabel m_gameNameLabel;
	private JLabel m_gameVersionLabel;
	
	private NewGameChooserEntry m_choosen;
	
	public NewGameChooser(Frame owner)
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
		m_gameListModel = new NewGameChooserModel();
		m_gameList = new JList(m_gameListModel);
		
		m_infoPanel = new JPanel();
		m_infoPanel.setLayout(new BorderLayout());
				
		m_notesPanel = new JEditorPane();
        m_notesPanel.setEditable(false);
        m_notesPanel.setContentType("text/html");   
        
        m_gameLocationLabel = new JLabel();
        m_mapNameLabel = new JLabel();
        m_numberOfPlayersLabel = new JLabel();
        m_gameNameLabel = new JLabel();
        m_gameVersionLabel = new JLabel();
	}
	
	private void layoutCoponents()
	{
		setLayout(new BorderLayout());
		
		
		JSplitPane mainSplit = new JSplitPane();
		add(mainSplit, BorderLayout.CENTER);
		
		JScrollPane listScroll = new JScrollPane();
		listScroll.setBorder(null);
		listScroll.getViewport().setBorder(null);
		listScroll.setViewportView(m_gameList);
		mainSplit.setLeftComponent(listScroll);
		mainSplit.setRightComponent(m_infoPanel);
		
				
		JPanel buttonsPanel = new JPanel();		
		add(buttonsPanel, BorderLayout.SOUTH);
		
		buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));
		
		buttonsPanel.add(Box.createGlue());
		buttonsPanel.add(m_okButton);
		buttonsPanel.add(m_cancelButton);
		buttonsPanel.add(Box.createHorizontalStrut(20));

		
		JScrollPane notesScroll = new JScrollPane();
		notesScroll.setViewportView(m_notesPanel);
		
        m_infoPanel.add(notesScroll, BorderLayout.CENTER);		
        
        JPanel infoSummaryPanel = new JPanel();
        infoSummaryPanel.setLayout(new GridBagLayout());
        int index = 0;
        addInfo(infoSummaryPanel, "Name:", m_gameNameLabel, index++);        
        addInfo(infoSummaryPanel, "Map:", m_mapNameLabel, index++);
        addInfo(infoSummaryPanel, "Number Of Players:", m_numberOfPlayersLabel, index++);
        addInfo(infoSummaryPanel, "File:", m_gameLocationLabel, index++);
        addInfo(infoSummaryPanel, "Version:", m_gameVersionLabel, index++);
        
        
        m_infoPanel.add(infoSummaryPanel, BorderLayout.NORTH);
	}
	
	private void addInfo(JPanel infoSummaryPanel, String name, JLabel component, int row)
	{
	    JLabel nameLabel = new JLabel(name);
	    nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
        infoSummaryPanel.add(nameLabel, 
	            new GridBagConstraints(0,row,1,1,0,0,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,0,0,10), 0,0));
	    infoSummaryPanel.add(component, 
	            new GridBagConstraints(1,row,1,1,1,1,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,0,0,0), 0,0));
	}
	
	public static NewGameChooserEntry chooseGame(Frame parent, String defaultGameName)
	{
	    NewGameChooser chooser = new NewGameChooser(parent);
	    chooser.setSize(700,500);
	    chooser.setLocationRelativeTo(parent);
	 
	    if(defaultGameName != null) { 
	        chooser.selectGame(defaultGameName);
	    }
	        
	    
	    chooser.setVisible(true);
	    
	    	    
	    return chooser.m_choosen;
	}
	
	private void selectGame(String gameName)
    {
	    if(gameName == null) {
	        return;
	    }
	    
	    NewGameChooserEntry entry = m_gameListModel.findByName(gameName);
        if(entry != null) {
            m_gameList.setSelectedValue(entry, true);
        }
    }

    private void updateInfoPanel()
	{
		if(getSelected() != null) 
		{
		    GameData data = getSelected().getGameData();
		    m_notesPanel.setText((String) data.getProperties().get("notes", "No Game Notes"));
		    
		    m_gameLocationLabel.setText(getSelected().getLocation());
		    m_numberOfPlayersLabel.setText(data.getPlayerList().size() + "");
		    m_mapNameLabel.setText((String) data.getProperties().get("mapName", ""));
		    m_gameNameLabel.setText(data.getGameName());
		    m_gameVersionLabel.setText(data.getGameVersion().toString());
		    
		} else 
		{
		    m_notesPanel.setText("");
		    
		    m_gameLocationLabel.setText("");
		    m_numberOfPlayersLabel.setText("");
		    m_mapNameLabel.setText("");
		    m_gameNameLabel.setText("");
		    m_gameVersionLabel.setText("");
		    
		}
		
		//scroll to the top of the notes screen
		SwingUtilities.invokeLater(new Runnable()
        {        
            public void run()
            {         
                m_notesPanel.scrollRectToVisible(new Rectangle(0,0,0,0));
            }
        
        });
	}
	
	private NewGameChooserEntry getSelected()
	{
	    int selected = m_gameList.getSelectedIndex();
	    if(selected == -1) 
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
	     m_okButton.addActionListener(new ActionListener()
        {
        
            public void actionPerformed(ActionEvent e)
            {
                selectAndReturn();
            }
        
        });
	     
	    m_cancelButton.addActionListener(new ActionListener()
        {
        
            public void actionPerformed(ActionEvent e)
            {
                cancelAndReturn();        
            }            
        });		 
	    
	    m_gameList.addListSelectionListener(new ListSelectionListener()
        {
        
            public void valueChanged(ListSelectionEvent e)
            {
                updateInfoPanel();        
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
	
	public static void main(String[] args)
	{
	    chooseGame(null,null);
	    System.exit(0);		
	}
}
