package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.startup.launcher.*;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.pbem.IPBEMMessenger;
import games.strategy.engine.pbem.IPBEMSaveGameMessenger;
import games.strategy.engine.pbem.IPBEMScreenshotMessenger;
import games.strategy.engine.pbem.IPBEMTurnSummaryMessenger;
import games.strategy.engine.pbem.NullPBEMMessenger;
import games.strategy.engine.pbem.PBEMMessagePoster;
import games.strategy.engine.random.*;
import games.strategy.ui.ProgressWindow;
import games.strategy.util.Util;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

public class PBEMSetupPanel extends SetupPanel implements Observer
{
    private static final String EMAIL_1_PROP_NAME = "games.strategy.engine.framework.ui.PBEMStartup.EMAIL2";
    private static final String EMAIL_2_PROP_NAME = "games.strategy.engine.framework.ui.PBEMStartup.EMAIL1";
    private static final String EMAIL_ID_PROP_NAME = "games.strategy.engine.framework.ui.PBEMStartup.ID";    

    private GridBagLayout m_gridBagLayout1 = new GridBagLayout();
    private JTextField m_email1TextField = new JTextField();
    private JTextField m_email2TextField = new JTextField();
    private JTextField m_gameIDTextField = new JTextField();
    private JLabel m_email1Label = new JLabel();
    private JLabel m_email2Label = new JLabel();
    private JLabel m_gameIDLabel = new JLabel();
    private JButton m_testDiceyButton = new JButton();
    private  JTextArea m_instructionsText = new JTextArea();
    private JComboBox m_diceServers = new JComboBox();
    private JButton m_testPostButton = new JButton();
    private JButton m_viewPostButton = new JButton();
    private JComboBox m_turnSummaryMsgrs = new JComboBox();
    private JComboBox m_screenshotMsgrs = new JComboBox();
    private JComboBox m_saveGameMsgrs = new JComboBox();
    private JTextField m_turnSummaryMsgrLogin = new JTextField(20);
    private JPasswordField m_turnSummaryMsgrPassword = new JPasswordField(20);
    private JTextField m_screenshotMsgrLogin = new JTextField(20);
    private JPasswordField m_screenshotMsgrPassword = new JPasswordField(20);
    private JTextField m_saveGameMsgrLogin = new JTextField(20);
    private JPasswordField m_saveGameMsgrPassword = new JPasswordField(20);
    private JLabel m_turnSummaryMsgrLabel = new JLabel("Post Turn Summary:");
    private JLabel m_turnSummaryMsgrLoginLabel = new JLabel("Login:");
    private JLabel m_turnSummaryMsgrPasswordLabel = new JLabel("Password:");
    private JLabel m_screenshotMsgrLabel = new JLabel("Post Screenshot File:");
    private JLabel m_screenshotMsgrLoginLabel = new JLabel("Login:");
    private JLabel m_screenshotMsgrPasswordLabel = new JLabel("Password:");
    private JLabel m_saveGameMsgrLabel = new JLabel("Post Save Game File:");
    private JLabel m_saveGameMsgrLoginLabel = new JLabel("Login:");
    private JLabel m_saveGameMsgrPasswordLabel = new JLabel("Password:");
    
    private Map<Object,Document> usernameMap = new HashMap<Object,Document>();
    private Map<Object,Document> passwordMap = new HashMap<Object,Document>();

    private final GameSelectorModel m_gameSelectorModel;
    
    

    public PBEMSetupPanel(GameSelectorModel model)
    {
        m_gameSelectorModel = model;
        createComponents();
        layoutComponents();
        setupListeners();
        setWidgetActivation();
        if(model.getGameData() != null)
        {
            loadEmails(model.getGameData());
            loadPBEMMessengers(model.getGameData());
        }
    }

    private void createComponents()
    {
        m_email1Label.setText("To:");
        m_email2Label.setText("Cc:");
        m_gameIDLabel.setText("ID:");
        m_testDiceyButton.setText("Test Email");
        m_testPostButton.setText("Test Post");
        m_viewPostButton.setText("View Posts");

        m_email2TextField.setText("");
        m_email2TextField.setColumns(50);

        m_email1TextField.setText("");
        m_email1TextField.setColumns(50);
        
        m_gameIDTextField.setText("");
        m_gameIDTextField.setColumns(10);
        
        
        m_instructionsText.setEditable(false);
        m_instructionsText.setText("PBEM Properties");
        m_instructionsText.setLineWrap(true);
        m_instructionsText.setWrapStyleWord(true);
        
        
        DefaultComboBoxModel diceServerModel = new DefaultComboBoxModel();
        
        populateDiceRollModel(diceServerModel);
        
        m_diceServers.setModel(diceServerModel);
        
        DefaultComboBoxModel turnSummaryMsgrModel = new DefaultComboBoxModel();
        DefaultComboBoxModel screenshotMsgrModel = new DefaultComboBoxModel();
        DefaultComboBoxModel saveGameMsgrModel = new DefaultComboBoxModel();
        
        IPBEMMessenger[] messengers = m_gameSelectorModel.getGameData().getGameLoader().getPBEMMessengers();

        NullPBEMMessenger nullMsgr = new NullPBEMMessenger();
        turnSummaryMsgrModel.addElement(nullMsgr);
        screenshotMsgrModel.addElement(nullMsgr);
        saveGameMsgrModel.addElement(nullMsgr);
        for (IPBEMMessenger m : messengers)
        {
            if(m instanceof IPBEMTurnSummaryMessenger)
                turnSummaryMsgrModel.addElement(m);
            if(m instanceof IPBEMScreenshotMessenger)
                screenshotMsgrModel.addElement(m);
            if(m instanceof IPBEMSaveGameMessenger)
                saveGameMsgrModel.addElement(m);
        }

        m_turnSummaryMsgrs.setModel(turnSummaryMsgrModel);
        m_screenshotMsgrs.setModel(screenshotMsgrModel);
        m_saveGameMsgrs.setModel(saveGameMsgrModel);
        
        // initialize document maps
        Document d = new PlainDocument();
        usernameMap.put(nullMsgr, d);
        m_turnSummaryMsgrLogin.setDocument(d);
        m_screenshotMsgrLogin.setDocument(d);
        m_saveGameMsgrLogin.setDocument(d);
        d = new PlainDocument();
        passwordMap.put(nullMsgr, d);
        m_turnSummaryMsgrPassword.setDocument(d);
        m_screenshotMsgrPassword.setDocument(d);
        m_saveGameMsgrPassword.setDocument(d);

        m_instructionsText.setText("You can enter up to 5 addresses in the To: or CC: fields, seperating each address by a space." 
                + "\n\nYou must enter an address in the To: field."
                
        );

        m_instructionsText.setBackground(this.getBackground());
    }
    
    private void populateDiceRollModel(DefaultComboBoxModel model) {
        File f = new File(GameRunner.getRootFolder(), "dice_servers");
        if(!f.exists()) {
            throw new IllegalStateException("No dice server folder:" + f);
        }
        
        java.util.List<Properties> propFiles = new ArrayList<Properties>();
        
        File[] files =  f.listFiles();
        for(File file : files) 
        {
            if(!file.isDirectory() && file.getName().endsWith(".properties")) 
            {
                try
                {
                    Properties props = new Properties();
                    FileInputStream fin = new FileInputStream(file);
                    try
                    {
                        props.load(fin);
                        propFiles.add(props);
                        
                    }
                    finally
                    {
                        fin.close();
                    }
                }
                catch(IOException e) 
                {
                    System.out.println("error reading file:" + file);
                    e.printStackTrace(System.out);
                }
            }
        }
        
        Collections.sort(propFiles, new Comparator<Properties>() {

            public int compare(Properties o1, Properties o2)
            {
                int n1 = Integer.parseInt(o1.getProperty("order"));
                int n2 = Integer.parseInt(o2.getProperty("order"));
                return n1 - n2;
            }
        });
        
        for(Properties prop : propFiles) {
            model.addElement(new PropertiesDiceRoller(prop));
        }
    }

    private void layoutComponents()
    {
        this.setLayout(m_gridBagLayout1);
    
        this.add(m_instructionsText, new GridBagConstraints(0, 0, 5, 1, 0.0, 0.2, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(5, 5,
                5, 5), 0, 0));
        
        
        this.add(m_email1TextField, new GridBagConstraints(1, 2, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0,
                0, 5), 0, 0));
        //this.add(m_email1Label, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 20,
        this.add(m_email1Label, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 20,
                0, 5), 0, 0));
        
        this.add(m_email2TextField, new GridBagConstraints(1, 3, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0,
                0, 5), 0, 0));
        this.add(m_email2Label, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 20, 0,
                5), 0, 0));

        this.add(m_gameIDTextField, new GridBagConstraints(1, 4, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0,
                0, 5), 0, 0));
        this.add(m_gameIDLabel, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 20, 0,
                5), 0, 0));
        GridBagConstraints labelConstraint = new GridBagConstraints();
        labelConstraint.anchor=GridBagConstraints.EAST;
        labelConstraint.insets = new Insets(5, 0, 0, 0);
        GridBagConstraints widgetConstraint = new GridBagConstraints();
        widgetConstraint.anchor=GridBagConstraints.WEST;
        widgetConstraint.gridwidth=GridBagConstraints.REMAINDER;
        widgetConstraint.fill=GridBagConstraints.HORIZONTAL;
        widgetConstraint.weightx = 0.20000000000000001D;
        widgetConstraint.insets = new Insets(5, 0, 0, 0);
        GridBagConstraints diceServerConstraint = new GridBagConstraints();
        diceServerConstraint.anchor=GridBagConstraints.NORTHWEST; 
        diceServerConstraint.gridwidth=GridBagConstraints.REMAINDER; 
        diceServerConstraint.fill=GridBagConstraints.HORIZONTAL; 
        diceServerConstraint.insets = new Insets(5, 0, 0, 0);
        JPanel diceServer = new JPanel(new GridBagLayout());
        diceServer.add(new JLabel("Dice Server:"), labelConstraint);
        diceServer.add(m_diceServers, widgetConstraint);
        
        //hide until we are ready
        this.add(diceServer, new GridBagConstraints(0, 5, 2, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(10, 15, 0, 0), 0, 0));        

        this.add(m_testDiceyButton, new GridBagConstraints(2, 5, 1, 1, 0.2, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(15, 5, 0, 5), 0, 0));        

        JPanel pbemMsgrPanel = new JPanel(new GridBagLayout());

        // turn summary
        pbemMsgrPanel.add(m_turnSummaryMsgrLabel, labelConstraint);
        pbemMsgrPanel.add(m_turnSummaryMsgrs, widgetConstraint);
        pbemMsgrPanel.add(m_turnSummaryMsgrLoginLabel, labelConstraint);
        pbemMsgrPanel.add(m_turnSummaryMsgrLogin, widgetConstraint);
        pbemMsgrPanel.add(m_turnSummaryMsgrPasswordLabel, labelConstraint);
        pbemMsgrPanel.add(m_turnSummaryMsgrPassword, widgetConstraint);

        // screenshot
        pbemMsgrPanel.add(m_screenshotMsgrLabel, labelConstraint);
        pbemMsgrPanel.add(m_screenshotMsgrs, widgetConstraint);
        pbemMsgrPanel.add(m_screenshotMsgrLoginLabel, labelConstraint);
        pbemMsgrPanel.add(m_screenshotMsgrLogin, widgetConstraint);
        pbemMsgrPanel.add(m_screenshotMsgrPasswordLabel, labelConstraint);
        pbemMsgrPanel.add(m_screenshotMsgrPassword, widgetConstraint);

        // savegame
        pbemMsgrPanel.add(m_saveGameMsgrLabel, labelConstraint);
        pbemMsgrPanel.add(m_saveGameMsgrs, widgetConstraint);
        pbemMsgrPanel.add(m_saveGameMsgrLoginLabel, labelConstraint);
        pbemMsgrPanel.add(m_saveGameMsgrLogin, widgetConstraint);
        pbemMsgrPanel.add(m_saveGameMsgrPasswordLabel, labelConstraint);
        pbemMsgrPanel.add(m_saveGameMsgrPassword, widgetConstraint);

        this.add(pbemMsgrPanel, new GridBagConstraints(0, 6, 2, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(10, 15, 0, 0), 0, 0));        

        this.add(m_viewPostButton, new GridBagConstraints(2, 6, 1, 1, 0.2, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                new Insets(15, 5, 0, 5), 0, 0));        

        this.add(m_testPostButton, new GridBagConstraints(2, 6, 1, 1, 0.2, 0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE,
                new Insets(0, 5, 0, 5), 0, 0));        
    }
    
    private IRemoteDiceServer getDiceServer()
    {
        return (IRemoteDiceServer) m_diceServers.getSelectedItem();
    }
    
    void testEmail()
    {
        PBEMDiceRoller random = new PBEMDiceRoller(m_email1TextField.getText(), m_email2TextField.getText(), getGameID(), getDiceServer());
        random.test();
    }

    void viewPostedItems()
    {
        IPBEMTurnSummaryMessenger msgr = getTurnSummaryMessenger();
        msgr.setGameId(m_gameIDTextField.getText().trim());
        msgr.viewPosted();
    }

    void testPost()
    {
        final ProgressWindow progressWindow = new ProgressWindow(MainFrame.getInstance(), "Testing Post...");
        progressWindow.setVisible(true);

        GameData gameData = new GameData();
        // populate PBEM Messengers from UI fields and store
        // the messengers in gameData
        storePBEMMessengers(gameData);

        // create a new message poster with no IDelegateHistoryWriter
        PBEMMessagePoster poster = new PBEMMessagePoster(gameData);

        

        String screenshotRef = poster.getScreenshotRef();
        String saveGameRef = poster.getSaveGameRef();
        String turnSummaryRef = poster.getTurnSummaryRef();
        String message = "";
        if(screenshotRef != null)
            message += "Test Screenshot: "+screenshotRef+"\n";
        if(saveGameRef != null)
            message += "Test Save Game: "+saveGameRef+"\n";
        if(turnSummaryRef != null)
            message += "Test turn summary: "+turnSummaryRef+"\n";

        SwingUtilities.invokeLater(new Runnable() {

            public void run()
            {
                progressWindow.setVisible(false);
                progressWindow.removeAll();
                progressWindow.dispose();
            }

        });

        JOptionPane.showMessageDialog(MainFrame.getInstance(), message, "Test Turn Summary Post", JOptionPane.INFORMATION_MESSAGE);

    }

    private void setupListeners()
    {
        m_gameSelectorModel.addObserver(this);
        
        m_testDiceyButton.addActionListener(new ActionListener()
        {

            public void actionPerformed(ActionEvent e)
            {
                testEmail();
            }

        });

        m_testPostButton.addActionListener(new ActionListener()
        {
        
            public void actionPerformed(ActionEvent e)
            {
                Runnable t = new Runnable() {

                    public void run()
                    {
                        testPost();
                    }
                };
                (new Thread(t)).start();
            }
        
        });

        m_viewPostButton.addActionListener(new ActionListener() {

        
            public void actionPerformed(ActionEvent e)
            {
                Runnable t = new Runnable() {

                    public void run()
                    {
                        viewPostedItems();
                    }
                };
                (new Thread(t)).start();
            }
        });
        // add document listeners for text-changed events
        final DocumentListener docListener = new DocumentListener() {

            public void changedUpdate(DocumentEvent e)
            {
                notifyObservers();
            }

            public void insertUpdate(DocumentEvent e)
            {
                notifyObservers();
            }

            public void removeUpdate(DocumentEvent e)
            {
                notifyObservers();
            }
        };

        m_turnSummaryMsgrs.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                IPBEMMessenger msgr = (IPBEMMessenger)((JComboBox)e.getSource()).getSelectedItem();
                m_turnSummaryMsgrLogin.setEnabled(msgr.getNeedsUsername());
                m_turnSummaryMsgrLoginLabel.setEnabled(msgr.getNeedsUsername());
                m_turnSummaryMsgrPassword.setEnabled(msgr.getNeedsPassword());
                m_turnSummaryMsgrPasswordLabel.setEnabled(msgr.getNeedsPassword());
                // update maps
                Object key = m_turnSummaryMsgrs.getSelectedItem();
                Document d = (Document)usernameMap.get(key);
                if(d == null)
                {
                    d = new PlainDocument();
                    d.addDocumentListener(docListener);
                }
                usernameMap.put(key, d);
                m_turnSummaryMsgrLogin.setDocument(d);
                d = (Document)passwordMap.get(key);
                if(d == null)
                {
                    d = new PlainDocument();
                    d.addDocumentListener(docListener);
                }
                passwordMap.put(key, d);
                m_turnSummaryMsgrPassword.setDocument(d);
                notifyObservers();
            }
        });
        m_turnSummaryMsgrs.setSelectedIndex(m_turnSummaryMsgrs.getSelectedIndex());

        m_screenshotMsgrs.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                IPBEMMessenger msgr = (IPBEMMessenger)((JComboBox)e.getSource()).getSelectedItem();
                m_screenshotMsgrLogin.setEnabled(msgr.getNeedsUsername());
                m_screenshotMsgrLoginLabel.setEnabled(msgr.getNeedsUsername());
                m_screenshotMsgrPassword.setEnabled(msgr.getNeedsPassword());
                m_screenshotMsgrPasswordLabel.setEnabled(msgr.getNeedsPassword());
                Object key = m_screenshotMsgrs.getSelectedItem();
                Document d = (Document)usernameMap.get(key);
                if(d == null)
                    d = new PlainDocument();
                usernameMap.put(key, d);
                m_screenshotMsgrLogin.setDocument(d);
                d = (Document)passwordMap.get(key);
                if(d == null)
                    d = new PlainDocument();
                passwordMap.put(key, d);
                m_screenshotMsgrPassword.setDocument(d);
                notifyObservers();
            }
        });
        m_screenshotMsgrs.setSelectedIndex(m_screenshotMsgrs.getSelectedIndex());

        m_saveGameMsgrs.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                IPBEMMessenger msgr = (IPBEMMessenger)((JComboBox)e.getSource()).getSelectedItem();
                m_saveGameMsgrLogin.setEnabled(msgr.getNeedsUsername());
                m_saveGameMsgrLoginLabel.setEnabled(msgr.getNeedsUsername());
                m_saveGameMsgrPassword.setEnabled(msgr.getNeedsPassword());
                m_saveGameMsgrPasswordLabel.setEnabled(msgr.getNeedsPassword());
                // update maps
                Object key = m_saveGameMsgrs.getSelectedItem();
                Document d = (Document)usernameMap.get(key);
                if(d == null)
                    d = new PlainDocument();
                usernameMap.put(key, d);
                m_saveGameMsgrLogin.setDocument(d);
                d = (Document)passwordMap.get(key);
                if(d == null)
                    d = new PlainDocument();
                passwordMap.put(key, d);
                m_saveGameMsgrPassword.setDocument(d);
                notifyObservers();
            }
        });
        m_saveGameMsgrs.setSelectedIndex(m_saveGameMsgrs.getSelectedIndex());

        m_email1TextField.getDocument().addDocumentListener(docListener);
        m_email2TextField.getDocument().addDocumentListener(docListener);
        m_gameIDTextField.getDocument().addDocumentListener(docListener);

        m_turnSummaryMsgrLogin.getDocument().addDocumentListener(docListener);
        m_turnSummaryMsgrPassword.getDocument().addDocumentListener(docListener);
        m_screenshotMsgrLogin.getDocument().addDocumentListener(docListener);
        m_screenshotMsgrPassword.getDocument().addDocumentListener(docListener);
        m_saveGameMsgrLogin.getDocument().addDocumentListener(docListener);
        m_saveGameMsgrPassword.getDocument().addDocumentListener(docListener);
    }

    private String getGameID()
    {
        return m_gameIDTextField.getText();
    }
    
    private String getEmail1()
    {
        return m_email1TextField.getText();
    }

    private String getEmail2()
    {
        return m_email2TextField.getText();
    }
    
    private IPBEMTurnSummaryMessenger getTurnSummaryMessenger()
    {
        IPBEMMessenger msgr = (IPBEMMessenger)m_turnSummaryMsgrs.getSelectedItem();
        if(msgr instanceof IPBEMTurnSummaryMessenger)
            return (IPBEMTurnSummaryMessenger)msgr;
        else
            return null;
    }
    
    private IPBEMScreenshotMessenger getScreenshotMessenger()
    {
        IPBEMMessenger msgr = (IPBEMMessenger)m_screenshotMsgrs.getSelectedItem();
        if(msgr instanceof IPBEMScreenshotMessenger)
            return (IPBEMScreenshotMessenger)msgr;
        else
            return null;
    }

    private IPBEMSaveGameMessenger getSaveGameMessenger()
    {
        IPBEMMessenger msgr = (IPBEMMessenger)m_saveGameMsgrs.getSelectedItem();
        if(msgr instanceof IPBEMSaveGameMessenger)
            return (IPBEMSaveGameMessenger)msgr;
        else
            return null;
    }

    private void loadEmails(GameData data)
    {
        if (m_gameSelectorModel.isSavedGame())
        {
            if( data.getProperties().get( EMAIL_1_PROP_NAME) != null )
            {
                m_email1TextField.setText(data.getProperties().get(
                        EMAIL_1_PROP_NAME).toString());
            }

            if( data.getProperties().get( EMAIL_2_PROP_NAME) != null )
            {
                m_email2TextField.setText(data.getProperties().get(
                        EMAIL_2_PROP_NAME).toString());
            }

            if( data.getProperties().get( EMAIL_ID_PROP_NAME) != null )
            {
                m_gameIDTextField.setText(data.getProperties().get(
                    EMAIL_ID_PROP_NAME).toString());
            }
        }     
    }    
    
    private void replaceOrAddComboBoxItem(JComboBox comboBox, Object customObj)
    {
        if(customObj != null)
        {
            Object defaultObj;
            int i;
            for(i = 0; (defaultObj = comboBox.getItemAt(i)) != null && !defaultObj.toString().equals(customObj.toString()); i++)
                defaultObj = null;

            if(defaultObj != null)
            {
                ((DefaultComboBoxModel)comboBox.getModel()).getIndexOf(defaultObj);
                comboBox.removeItemAt(i);
                comboBox.insertItemAt(customObj, i);
            } else
            {
                comboBox.addItem(customObj);
            }
            comboBox.setSelectedItem(customObj);
        }
    }

    private void loadPBEMMessengers(GameData data)
    {
        // TODO: store username, and password fields in user data

        // Let a temporary poster do the loading 
        PBEMMessagePoster poster = new PBEMMessagePoster(data);

        // We now have two instances of these messengers - deserialized instance and GameLoader instance.
        // Need to merge the two.
        if(!(m_turnSummaryMsgrs.getSelectedItem() instanceof IPBEMTurnSummaryMessenger))
            replaceOrAddComboBoxItem(m_turnSummaryMsgrs, poster.getTurnSummaryMessenger());
        if(!(m_screenshotMsgrs.getSelectedItem() instanceof IPBEMScreenshotMessenger))
            replaceOrAddComboBoxItem(m_screenshotMsgrs, poster.getScreenshotMessenger());
        if(!(m_saveGameMsgrs.getSelectedItem() instanceof IPBEMSaveGameMessenger))
            replaceOrAddComboBoxItem(m_saveGameMsgrs, poster.getSaveGameMessenger());
    }

    private void setWidgetActivation()
    {

    }
    
    
    @Override
    public void cancel()
    {
        m_gameSelectorModel.deleteObserver(this);

    }

    @Override
    public boolean canGameStart()
    {
        boolean canStart = true;
        boolean canTestEmail = true;
        boolean canTestPost = true;
        boolean canViewPost = true;
        IPBEMMessenger turnSummaryMsgr = null;
        IPBEMMessenger screenshotMsgr = null;
        IPBEMMessenger saveGameMsgr = null;

        // verify the emails 

        if (m_email1TextField.getText().trim().equals("") ||
            !Util.isMailValid(m_email1TextField.getText()))
        {
            m_email1Label.setForeground(Color.RED);
            canTestEmail = false;
            canStart = false;
        } else
        {
            m_email1Label.setForeground(Color.BLACK);
        }
        if(!Util.isMailValid(m_email2TextField.getText()))
        {
            m_email2Label.setForeground(Color.RED);
            canTestEmail = false;
            canStart = false;
        } else
        {
            m_email2Label.setForeground(Color.BLACK);
        }

        //verify PBEM messenger fields

        turnSummaryMsgr = (IPBEMMessenger)m_turnSummaryMsgrs.getSelectedItem();
        if(turnSummaryMsgr.getNeedsUsername() && m_turnSummaryMsgrLogin.getText().trim().equals(""))
        {
            m_turnSummaryMsgrLoginLabel.setForeground(Color.RED);
            canTestPost = false;
            canStart = false;
        } else
        {
            m_turnSummaryMsgrLoginLabel.setForeground(Color.BLACK);
        }
        if(turnSummaryMsgr.getNeedsPassword() && (new String(m_turnSummaryMsgrPassword.getPassword())).trim().equals(""))
        {
            m_turnSummaryMsgrPasswordLabel.setForeground(Color.RED);
            canTestPost = false;
            canStart = false;
        } else
        {
            m_turnSummaryMsgrPasswordLabel.setForeground(Color.BLACK);
        }
        screenshotMsgr = (IPBEMMessenger)m_screenshotMsgrs.getSelectedItem();
        if(screenshotMsgr.getNeedsUsername() && m_screenshotMsgrLogin.getText().trim().equals(""))
        {
            m_screenshotMsgrLoginLabel.setForeground(Color.RED);
            canTestPost = false;
            canStart = false;
        } else
        {
            m_screenshotMsgrLoginLabel.setForeground(Color.BLACK);
        }
        if(screenshotMsgr.getNeedsPassword() && (new String(m_screenshotMsgrPassword.getPassword())).trim().equals(""))
        {
            m_screenshotMsgrPasswordLabel.setForeground(Color.RED);
            canTestPost = false;
            canViewPost = false;
            canStart = false;
        } else
        {
            m_screenshotMsgrPasswordLabel.setForeground(Color.BLACK);
        }
        saveGameMsgr = (IPBEMMessenger)m_saveGameMsgrs.getSelectedItem();
        if(saveGameMsgr.getNeedsUsername() && m_saveGameMsgrLogin.getText().trim().equals(""))
        {
            m_saveGameMsgrLoginLabel.setForeground(Color.RED);
            canTestPost = false;
            canStart = false;
        } else
        {
            m_saveGameMsgrLoginLabel.setForeground(Color.BLACK);
        }
        if(saveGameMsgr.getNeedsPassword() && (new String(m_saveGameMsgrPassword.getPassword())).trim().equals(""))
        {
            m_saveGameMsgrPasswordLabel.setForeground(Color.RED);
            canTestPost = false;
            canStart = false;
        } else
        {
            m_saveGameMsgrPasswordLabel.setForeground(Color.BLACK);
        }
        if (turnSummaryMsgr instanceof NullPBEMMessenger
            && screenshotMsgr instanceof NullPBEMMessenger
            && saveGameMsgr instanceof NullPBEMMessenger)
        {
            canTestPost = false;
            m_gameIDLabel.setForeground(Color.BLACK);
        }
        else
        {
            // if posting, require game id
            if(m_gameIDTextField.getText().trim().equals(""))
            {
                m_gameIDLabel.setForeground(Color.RED);
                canTestPost = false;
                canViewPost = false;
                canStart = false;
            } else
            {
                m_gameIDLabel.setForeground(Color.BLACK);
            }
        }
        if(!turnSummaryMsgr.getCanViewPosted())
            canViewPost = false;
        m_testDiceyButton.setEnabled(canTestEmail);
        m_testPostButton.setEnabled(canTestPost);
        m_viewPostButton.setEnabled(canViewPost);
        return canStart && (m_gameSelectorModel.getGameData() != null);
    }
    
    private void storeEmails(GameData data)
    {
        data.getProperties().set(EMAIL_1_PROP_NAME, getEmail1());
        data.getProperties().set(EMAIL_2_PROP_NAME, getEmail2());
        data.getProperties().set(EMAIL_ID_PROP_NAME, getGameID());        
    }
    
    private void storePBEMMessengers(GameData gameData)
    {
        // let a temporary poster do the storing
        PBEMMessagePoster poster = new PBEMMessagePoster();
        IPBEMMessenger msgr;
        if((msgr = getTurnSummaryMessenger()) != null)
        {
            msgr.setUsername(m_turnSummaryMsgrLogin.getText().trim());
            msgr.setPassword((new String(m_turnSummaryMsgrPassword.getPassword())).trim());
            msgr.setGameId(m_gameIDTextField.getText().trim());
        }
        poster.setTurnSummaryMessenger(msgr);

        if((msgr = getScreenshotMessenger()) != null)
        {
            msgr.setUsername(m_screenshotMsgrLogin.getText().trim());
            msgr.setPassword((new String(m_screenshotMsgrPassword.getPassword())).trim());
            msgr.setGameId(m_gameIDTextField.getText().trim());
        }
        poster.setScreenshotMessenger(msgr);

        if((msgr = getSaveGameMessenger()) != null)
        {
            msgr.setUsername(m_saveGameMsgrLogin.getText().trim());
            msgr.setPassword((new String(m_saveGameMsgrPassword.getPassword())).trim());
            msgr.setGameId(m_gameIDTextField.getText().trim());
        }
        poster.setSaveGameMessenger(msgr);
        poster.storeMessengers(gameData);
    }
    @Override
    public void postStartGame()
    {
        storeEmails(m_gameSelectorModel.getGameData());
        storePBEMMessengers(m_gameSelectorModel.getGameData());
    }

    public void update(Observable o, Object arg)
    {
        loadEmails(m_gameSelectorModel.getGameData());
        loadPBEMMessengers(m_gameSelectorModel.getGameData());
    }
    
    @Override
    public ILauncher getLauncher()
    {
        PBEMDiceRoller randomSource = new PBEMDiceRoller(getEmail1(), getEmail2(), getGameID(), getDiceServer());
        
        
        Map<String,String> playerTypes = new HashMap<String,String>();
        
        String playerType = m_gameSelectorModel.getGameData().getGameLoader().getServerPlayerTypes()[0];
        
        for(String playerName : m_gameSelectorModel.getGameData().getPlayerList().getNames())
        {
            playerTypes.put(playerName, playerType);
        }
                
        LocalLauncher launcher = new LocalLauncher(m_gameSelectorModel, randomSource, playerTypes);
        return launcher;
        
    }
    
}


