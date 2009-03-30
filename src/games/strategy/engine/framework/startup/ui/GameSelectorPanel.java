package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.data.properties.PropertiesUI;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.ui.*;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FilenameFilter;
import java.net.URLDecoder;
import java.util.*;

import javax.swing.*;

public class GameSelectorPanel extends JPanel implements Observer
{
    private JLabel m_nameText;
    private JLabel m_versionText;
    private JLabel m_fileNameLabel;
    private JLabel m_fileNameText;
    
    private JLabel m_nameLabel;
    private JLabel m_versionLabel;
    
    private JLabel m_roundLabel;
    private JLabel m_roundText;


    private JButton m_loadSavedGame;
    private JButton m_loadNewGame;
    private JButton m_gameOptions;
    
    private final GameSelectorModel m_model;
    
    public GameSelectorPanel(GameSelectorModel model)
    {
        m_model = model;
        m_model.addObserver(this);
        
        createComponents();
        layoutComponents();
        setupListeners();
        setWidgetActivation();
        updateGameData();
    }

    @SuppressWarnings("deprecation")
    private void updateGameData()
    {

        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    updateGameData();
                }
            });
            return;
        }

        
        m_nameText.setText(m_model.getGameName());
        m_versionText.setText(m_model.getGameVersion());
        m_roundText.setText(m_model.getGameRound());
        
        String fileName = m_model.getFileName();
        try
        {
            fileName = URLDecoder.decode(fileName);
        } catch(IllegalArgumentException e) 
        {
            //ignore
        }
               
        m_fileNameText.setText(getLimitedFileNameText(fileName));
        m_fileNameText.setToolTipText(fileName);        
    }

    private String getLimitedFileNameText(String fileName) 
    {
        final int maxLength = 25;
        
        if(fileName.length() <= maxLength) 
        {
            return fileName;
        }
        
        final int defaultCuttoff = 18;
        int cuttoff = defaultCuttoff;
                           
        ///games will be in most paths, 
        //try to ignore it
        if(fileName.indexOf("games") > 0) 
        { 
            cuttoff = Math.min(18, fileName.indexOf("games"));
        }                
        
        int length = fileName.length();
        return fileName.substring(0,cuttoff) + "..."  + fileName.substring(length - (maxLength - cuttoff) - 2, length);
        
    }
    
    private void createComponents()
    {
        m_nameLabel = new JLabel("Game Name:");
        m_versionLabel = new JLabel("Game Version:");
        m_roundLabel = new JLabel("Game Round:");
        m_fileNameLabel = new JLabel("File Name:");
        
        
        
        m_nameText = new JLabel();
        m_versionText = new JLabel();
        m_roundText = new JLabel();
        m_fileNameText = new JLabel();
        
        m_loadNewGame = new JButton("Choose Game...");
        m_loadSavedGame = new JButton("Load Saved Game...");
        m_gameOptions = new JButton("Game Options...");
    }

    private void layoutComponents()
    {
        setLayout(new GridBagLayout());
        
        add(m_nameLabel, new GridBagConstraints(0,0,1,1,0,0,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10,10,3,5), 0,0));
        add(m_nameText, new GridBagConstraints(1,0,1,1,0,0,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10,0,3,0), 0,0));
        
        add(m_versionLabel, new GridBagConstraints(0,1,1,1,0,0,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,10,3,5), 0,0));
        add(m_versionText, new GridBagConstraints(1,1,1,1,0,0,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,0,3,0), 0,0));
        
        add(m_roundLabel, new GridBagConstraints(0,2,1,1,0,0,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,10,3,5), 0,0));
        add(m_roundText, new GridBagConstraints(1,2,1,1,0,0,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,0,3,0), 0,0));

        
        
        add(m_fileNameLabel, new GridBagConstraints(0,3,1,1,0,0,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(20,10,3,5), 0,0));
        add(m_fileNameText, new GridBagConstraints(0,4,2,1,0,0,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,10,3,5), 0,0));

        
        
        add(m_loadNewGame, new GridBagConstraints(0,5,2,1,0,0,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(25,10,10,10), 0,0));
        add(m_loadSavedGame, new GridBagConstraints(0,6,2,1,0,0,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,10,10,10), 0,0));
        add(m_gameOptions, new GridBagConstraints(0,7,2,1,0,0,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(25,10,10,10), 0,0));        
        
        //spacer
        add(new JPanel(), new GridBagConstraints(0,8,2,1,1,1,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0,0));
        
        
        
    }

    private void setupListeners()
    {
        m_loadNewGame.addActionListener(new ActionListener()
        {
        
            public void actionPerformed(ActionEvent e)
            {
                selectGameFile(false);
            }        
        });
        
        m_loadSavedGame.addActionListener(new ActionListener()
        {
        
            public void actionPerformed(ActionEvent e)
            {
                selectGameFile(true);
            }
        
        });
        
        m_gameOptions.addActionListener(new ActionListener()
        {
        
            public void actionPerformed(ActionEvent e)
            {
                selectGameOptions();
            }
        
        });
    }

    private void selectGameOptions()
    {
        PropertiesUI panel = new PropertiesUI(m_model.getGameData().getProperties(), true);
        JScrollPane scroll = new JScrollPane(panel);
        scroll.setBorder(null);
        scroll.getViewport().setBorder(null);
        JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(this), scroll, "Game Options", JOptionPane.PLAIN_MESSAGE, null);
    }

    private void setWidgetActivation()
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    setWidgetActivation();
                }
            });
            return;
        }
        boolean canSelectGameData = m_model != null && m_model.canSelect();
       
        m_loadSavedGame.setEnabled(canSelectGameData);
        m_loadNewGame.setEnabled(canSelectGameData);

        // Disable game options if there are none.
        if (canSelectGameData && m_model.getGameData() != null && m_model.getGameData().getProperties().getEditableProperties().size() > 0)
            m_gameOptions.setEnabled(true);
        else
            m_gameOptions.setEnabled(false);
    }

    public void update(Observable o, Object arg)
    {
        updateGameData();
        setWidgetActivation();
    }
    
    private void selectGameFile(boolean saved)
    {
        // For some strange reason, 
        //    the only way to get a Mac OS X native-style file dialog
        //    is to use an AWT FileDialog instead of a Swing JDialog
        if(saved)
        {
            if(GameRunner.isMac())
            {
                FileDialog fileDialog = new FileDialog(MainFrame.getInstance());
                fileDialog.setMode(FileDialog.LOAD);
                
                SaveGameFileChooser.ensureDefaultDirExists();
                fileDialog.setDirectory(SaveGameFileChooser.DEFAULT_DIRECTORY.getPath());
                fileDialog.setFilenameFilter(new FilenameFilter(){
                   public boolean accept(File dir, String name)
                   {   
                       // the extension should be .tsvg, but find svg extensions as well
                       //also, macs download the file as tsvg.gz, so accept that as well
                       return name.endsWith(".tsvg") || name.endsWith(".svg") || name.endsWith("tsvg.gz");
                   }
                });
                            
                fileDialog.setVisible(true);
                
                String fileName = fileDialog.getFile();
                String dirName = fileDialog.getDirectory();
                
                if (fileName==null)
                    return;
                else
                {
                    File f = new File(dirName, fileName);
                    m_model.load(f, this);
                }
                
            }            
            // Non-Mac platforms should use the normal Swing JFileChooser
            else
            {
                JFileChooser fileChooser = SaveGameFileChooser.getInstance();
    
                int rVal = fileChooser.showOpenDialog(JOptionPane.getFrameForComponent(this));
    
    
                if(rVal != JFileChooser.APPROVE_OPTION)
                    return;
                
                m_model.load(fileChooser.getSelectedFile(), this);
            }
        }
        else
        {
            NewGameChooserEntry entry = NewGameChooser.chooseGame(JOptionPane.getFrameForComponent(this), m_model.getGameName());
            if(entry != null) {
                m_model.load(entry);
            }
        }
            
        
    }
    
    
    public static void main(String[] args)
    {
        JFrame f = new JFrame();
        GameSelectorModel model = new GameSelectorModel();
        f.getContentPane().add(new GameSelectorPanel(model));
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        model.loadDefaultGame(f);
        
        f.pack();
        f.setVisible(true);
    }
    

}
