package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.startup.launcher.*;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.random.IronyGamesDiceRollerRandomSource;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

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
    private JButton m_testButton = new JButton();
    private  JTextArea m_instructionsText = new JTextArea();
    
    
    private final GameSelectorModel m_gameSelectorModel;
    
    

    public PBEMSetupPanel(GameSelectorModel model)
    {
        m_gameSelectorModel = model;
        createComponents();
        layoutComponents();
        setupListeners();
        setWidgetActivation();
    }

    private void createComponents()
    {
        m_email1Label.setText("To:");
        m_email2Label.setText("Cc:");
        m_gameIDLabel.setText("ID:");
        m_testButton.setText("Test Email");

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
        
        
        m_instructionsText.setText("\nPBEM differs from single player in that dice rolls are done by a dice server, and the results "
                + "are mailed to the email addresses below.\n\n" + "Dice are rolled using the dice server at http://www.irony.com/mailroll.html"
                + "\n\nYou can enter up to 5 addresses in the To: or CC: fields, seperating each address by a space." 
                + "\n\nYou must enter an address in the To: field."
                
        );

        m_instructionsText.setBackground(this.getBackground());
    }

    private void layoutComponents()
    {
        this.setLayout(m_gridBagLayout1);
    
        this.add(m_instructionsText, new GridBagConstraints(0, 0, 5, 1, 0.0, 0.2, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(5, 5,
                5, 5), 0, 0));
        
        
        this.add(m_email1TextField, new GridBagConstraints(1, 2, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0,
                0, 0), 0, 0));
        this.add(m_email1Label, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 20,
                0, 5), 0, 0));
        
        this.add(m_email2TextField, new GridBagConstraints(1, 3, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0,
                0, 0), 0, 0));
        this.add(m_email2Label, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 20, 0,
                5), 0, 0));

        this.add(m_gameIDTextField, new GridBagConstraints(1, 4, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0,
                0, 0), 0, 0));
        this.add(m_gameIDLabel, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 20, 0,
                5), 0, 0));

        
        
        this.add(m_testButton, new GridBagConstraints(0, 5, 3, 1, 0.2, 0.2, GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        
    }
    
    void test()
    {
        IronyGamesDiceRollerRandomSource random = new IronyGamesDiceRollerRandomSource(m_email1TextField.getText(), m_email2TextField.getText(), getGameID());
        random.test();
    }

    private void setupListeners()
    {
        m_gameSelectorModel.addObserver(this);
        
        m_testButton.addActionListener(new ActionListener()
        {
        
            public void actionPerformed(ActionEvent e)
            {
                test();
            }
        
        });
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
    
    
    private void loadEmails(GameData data)
    {
        if(data.getProperties().get(EMAIL_1_PROP_NAME) != null)
        {
            if(m_email1TextField.getText().trim().length() == 0)
                m_email1TextField.setText(data.getProperties().get(EMAIL_1_PROP_NAME).toString());            
        }
        if(data.getProperties().get(EMAIL_2_PROP_NAME) != null)
        {
            if(m_email2TextField.getText().trim().length() == 0)
                m_email2TextField.setText(data.getProperties().get(EMAIL_2_PROP_NAME).toString());            
        }
        if(data.getProperties().get(EMAIL_ID_PROP_NAME) != null)
        {
            if(m_gameIDTextField.getText().trim().length() == 0)
                m_gameIDTextField.setText(data.getProperties().get(EMAIL_ID_PROP_NAME).toString());            
        }        

        
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
        //TODO - we should verify the emails before
        return m_gameSelectorModel.getGameData() != null;
    }
    
    private void storeEmails(GameData data)
    {
        data.getProperties().set(EMAIL_1_PROP_NAME, getEmail1());
        data.getProperties().set(EMAIL_2_PROP_NAME, getEmail2());
        data.getProperties().set(EMAIL_ID_PROP_NAME, getGameID());        
    }
    
    @Override
    public void postStartGame()
    {
        storeEmails(m_gameSelectorModel.getGameData());
    }

    public void update(Observable o, Object arg)
    {
        loadEmails(m_gameSelectorModel.getGameData());
    }
    
    @Override
    public ILauncher getLauncher()
    {
        IronyGamesDiceRollerRandomSource randomSource = new IronyGamesDiceRollerRandomSource(getEmail1(), getEmail2(), getGameID());
        
        
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


