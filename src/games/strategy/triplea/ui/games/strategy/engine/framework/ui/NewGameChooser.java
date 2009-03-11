package games.strategy.engine.framework.ui;

import games.strategy.engine.data.GameData;

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
        m_notesPanel.setBackground(new JLabel().getBackground());
        
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
		
		JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new GridBagLayout());
		
		JLabel gamesLabel = new JLabel("Games");
		gamesLabel.setFont(gamesLabel.getFont().deriveFont(Font.BOLD, gamesLabel.getFont().getSize() + 2) );
		leftPanel.add(gamesLabel, new GridBagConstraints(0,0,1,1,0,0,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10,10,10,10), 0,0));
		leftPanel.add(listScroll, new GridBagConstraints(0,1,1,1,1.0,1.0,GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(0,10,0,0), 0,0));
		
		
		mainSplit.setLeftComponent(leftPanel);		
		mainSplit.setRightComponent(m_infoPanel);

		mainSplit.setBorder(null);
		
		listScroll.setMinimumSize(new Dimension(150,0));
		
		JPanel buttonsPanel = new JPanel();		
		add(buttonsPanel, BorderLayout.SOUTH);		
		
		buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));
		
		buttonsPanel.add(Box.createGlue());
		buttonsPanel.add(m_okButton);
		buttonsPanel.add(m_cancelButton);
		buttonsPanel.add(Box.createGlue());

		
		JScrollPane notesScroll = new JScrollPane();		
		notesScroll.setViewportView(m_notesPanel);
		notesScroll.setBorder(null);
        notesScroll.getViewport().setBorder(null);
        
        m_infoPanel.add(Box.createVerticalStrut(10), BorderLayout.NORTH);
        m_infoPanel.add(Box.createHorizontalStrut(10), BorderLayout.WEST);
        m_infoPanel.add(notesScroll, BorderLayout.CENTER);		
        
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
		    
		    StringBuilder notes = new StringBuilder();
		    
		    notes.append("<h1>").append(data.getGameName()).append("</h1>");
		    appendListItem("Map Name", (String) data.getProperties().get("mapName", ""), notes);
		    appendListItem("Number Of Players", data.getPlayerList().size() + "", notes);
		    appendListItem("Location", getSelected().getLocation() + "", notes);
		    appendListItem("Version", data.getGameVersion() + "", notes);
		    notes.append("<p></p>");
		    notes.append((String) data.getProperties().get("notes", ""));
		    
		    m_notesPanel.setText(notes.toString());
		    
		   
		    
		} else 
		{
		    m_notesPanel.setText("");
		    
		   
		    
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
    
    private void appendListItem(String title, String value, StringBuilder builder)
    {
        builder.append("<b>").append(title).append("</b>").append(": ").append(value).append("<br>");
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
	    chooseGame(null,"Revised");
	    System.exit(0);		
	}
}
