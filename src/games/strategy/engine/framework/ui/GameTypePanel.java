/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */


package games.strategy.engine.framework.ui;

import javax.swing.*;
import java.awt.*;
import javax.swing.border.*;
import java.awt.event.*;
import java.io.*;

import games.strategy.engine.data.*;
import games.strategy.engine.framework.*;
import org.xml.sax.*;
import games.strategy.engine.framework.ui.NewGameFileChooser;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author Sean Bridges
 * @version 1.0
 */

public class GameTypePanel extends JPanel
{
  private static final String SERVER = "Server";
  private static final String CLIENT = "Client";
  private static final String LOCAL = "Local Game";
  private static final String PBEM = "PBEM";

  private LauncherFrame m_launcherFrame;

  private JPanel m_gameFilePanel = new JPanel();
  private BorderLayout borderLayout1 = new BorderLayout();
  private Border border1;
  private TitledBorder titledBorder1;
  private Border border2;
  private JTextField m_fileNameTextField = new JTextField();
  private JPanel m_fileNamePanel = new JPanel();
  private JButton m_fileButton = new JButton();
  private BorderLayout borderLayout2 = new BorderLayout();
  private FlowLayout flowLayout2 = new FlowLayout();
  private JPanel m_gameTypePanel = new JPanel();
  private Border border3;
  private TitledBorder titledBorder2;
  private Border border4;
  private JComboBox m_gameTypeComboBox = new JComboBox(new String[] {LOCAL, SERVER, CLIENT, PBEM});

  private FlowLayout flowLayout3 = new FlowLayout();
  private JPanel m_gameVersionPanel = new JPanel();
  private JLabel mEngineVersionTitle = new JLabel();
  private Border border5;
  private TitledBorder titledBorder3;
  private JLabel m_gameName = new JLabel();
  private JLabel m_gameNameTitle = new JLabel();
  private JLabel m_engineVersion = new JLabel();
  private GridBagLayout gridBagLayout1 = new GridBagLayout();
  private JLabel m_gameVersionTitle = new JLabel();
  private JLabel m_gameVersion = new JLabel();
  private JComboBox mGameTypeComboBox = new JComboBox();
  private ComboBoxModel m_fileTypeModel = new DefaultComboBoxModel(new String[] {"New", "Saved"});
  private JPanel m_newSavedPanel = new JPanel();
  private JLabel jLabel1 = new JLabel();
  private FlowLayout flowLayout1 = new FlowLayout();
  private JButton m_helpButton = new JButton();

  public GameTypePanel()
  {
    try
    {
      jbInit();
      postJBInit();
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
  }

  /**
   * JBuilder initialization.
   */
  private void jbInit() throws Exception
  {
    border1 = BorderFactory.createEtchedBorder(Color.white,new Color(165, 163, 151));
    titledBorder1 = new TitledBorder(border1,"File");
    border2 = BorderFactory.createCompoundBorder(titledBorder1,BorderFactory.createEmptyBorder(5,5,5,5));
    border3 = BorderFactory.createEtchedBorder(Color.white,new Color(165, 163, 151));
    titledBorder2 = new TitledBorder(border3,"Game Type");
    border4 = BorderFactory.createCompoundBorder(titledBorder2,BorderFactory.createEmptyBorder(5,5,5,5));
    border5 = BorderFactory.createEtchedBorder(Color.white,new Color(165, 163, 151));
    titledBorder3 = new TitledBorder(border5,"Version");
    this.setLayout(borderLayout1);
    m_gameFilePanel.setBorder(border2);
    m_gameFilePanel.setLayout(borderLayout2);
    m_fileNameTextField.setEditable(false);
    m_fileNameTextField.setColumns(15);
    m_fileButton.setText("File...");
    m_fileButton.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        m_fileButton_actionPerformed(e);
      }
    });
    m_fileNamePanel.setLayout(flowLayout2);
    flowLayout2.setAlignment(FlowLayout.LEFT);
    m_gameTypePanel.setLayout(flowLayout3);
    m_gameTypePanel.setBorder(border4);
    flowLayout3.setAlignment(FlowLayout.LEFT);
    m_gameTypeComboBox.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        m_gameTypeComboBox_actionPerformed(e);
      }
    });
    mEngineVersionTitle.setFont(new java.awt.Font("Dialog", 1, 12));

    mEngineVersionTitle.setText("Engine Version:   ");
    m_gameVersionPanel.setBorder(titledBorder3);
    m_gameVersionPanel.setLayout(gridBagLayout1);
    m_gameNameTitle.setFont(new java.awt.Font("Dialog", 1, 12));

    m_gameNameTitle.setText("Game Name:   ");
    m_gameVersionTitle.setFont(new java.awt.Font("Dialog", 1, 12));

    m_gameVersionTitle.setText("Game Version:   ");
    m_gameName.setForeground(Color.black);
    m_gameVersion.setForeground(Color.black);
    m_engineVersion.setForeground(Color.black);
    mGameTypeComboBox.setModel(m_fileTypeModel);
    jLabel1.setText("Start a new game or load a saved?");
    m_newSavedPanel.setLayout(flowLayout1);
    flowLayout1.setAlignment(FlowLayout.LEFT);
    m_helpButton.setToolTipText("");
    m_helpButton.setText("Help...");
    m_helpButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        m_helpButton_actionPerformed(e);
      }
    });
    m_newSavedPanel.add(jLabel1, null);
    m_gameFilePanel.add(m_fileNamePanel, BorderLayout.CENTER);
    m_fileNamePanel.add(m_fileNameTextField, null);
    m_fileNamePanel.add(m_fileButton, null);
    m_gameFilePanel.add(m_newSavedPanel, BorderLayout.NORTH);
    m_newSavedPanel.add(mGameTypeComboBox, null);
    this.add(m_gameFilePanel,  BorderLayout.CENTER);
    this.add(m_gameTypePanel, BorderLayout.NORTH);
    m_gameTypePanel.add(m_gameTypeComboBox, null);
    m_gameTypePanel.add(m_helpButton, null);

    this.add(m_gameVersionPanel, BorderLayout.SOUTH);
    m_gameVersionPanel.add(m_engineVersion,     new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    m_gameVersionPanel.add(m_gameNameTitle,       new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    m_gameVersionPanel.add(m_gameName,     new GridBagConstraints(1, 1, 1, 2, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    m_gameVersionPanel.add(m_gameVersionTitle,    new GridBagConstraints(0, 2, 1, 2, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    m_gameVersionPanel.add(m_gameVersion,   new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    m_gameVersionPanel.add(mEngineVersionTitle,    new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
  }

  private void postJBInit()
  {
    m_engineVersion.setText(games.strategy.engine.EngineVersion.VERSION.toString());
  }

  public void initializeWithDefaultFile()
  {
    File defaultGame =  new File(NewGameFileChooser.DEFAULT_DIRECTORY, "classic_a&a.xml");
    try
    {
      loadFile(defaultGame);
    }
    catch (IOException ex)
    {
      System.err.println(NewGameFileChooser.DEFAULT_DIRECTORY);
      System.err.println(defaultGame.getAbsolutePath());
      ex.printStackTrace();

      //ignore, we're just loading the default file, no reason to cause a panic
    }
  }


  void m_gameTypeComboBox_actionPerformed(ActionEvent e)
  {
    boolean enable = !m_gameTypeComboBox.getSelectedItem().equals(CLIENT);

    m_fileButton.setEnabled(enable);

    if(!enable)
      m_fileNameTextField.setText("");

    m_launcherFrame.clearGameType();

    if(isServer() || isClient() || isPBEM())
      m_launcherFrame.chooseClientServerOptions();


    m_launcherFrame.setWidgetActivation();

  }

  public void setLauncherFrame(LauncherFrame launcher)
  {
    m_launcherFrame = launcher;
  }

  public LauncherFrame getLauncherFrame()
  {
    return m_launcherFrame;
  }

  void m_fileButton_actionPerformed(ActionEvent e)
  {
    JFileChooser fileChooser;
    if(gameTypeIsSaved())
      fileChooser = SaveGameFileChooser.getInstance();
    else
      fileChooser = NewGameFileChooser.getInstance();

    int rVal = fileChooser.showOpenDialog(this);


    if(rVal != JFileChooser.APPROVE_OPTION)
      return;

    try
    {
      File file = fileChooser.getSelectedFile();
      loadFile(file);
    }

    catch (IOException ex)
    {
      ex.printStackTrace();
      JOptionPane.showMessageDialog(getTopLevelAncestor(), "Error loading file", "Error", JOptionPane.WARNING_MESSAGE);
      return;
    }
    finally
    {
      m_launcherFrame.setWidgetActivation();
    }
  }

  private boolean gameTypeIsSaved()
  {
    boolean useSaved = mGameTypeComboBox.getSelectedIndex() == 1;
    return useSaved;
  }

  private void loadFile(File file) throws IOException
  {
    GameData data;
    if(gameTypeIsSaved())
    {
      data = (new GameDataManager()).loadGame(file);
    }
    else
    {
      try
      {
        data= (new GameParser()).parse(new FileInputStream(file));
      }
      catch (SAXException ex)
      {
        ex.printStackTrace(System.out);
        JOptionPane.showMessageDialog(getTopLevelAncestor(), "Error loading file", "Error", JOptionPane.WARNING_MESSAGE);
        return;
      }catch (GameParseException ex)
      {
        ex.printStackTrace(System.out);
        JOptionPane.showMessageDialog(getTopLevelAncestor(), "Error loading file", "Error", JOptionPane.WARNING_MESSAGE);
        return;
      }
    }
    m_launcherFrame.setGameData(data);

    m_gameName.setText(data.getGameName());
    m_gameVersion.setText(data.getGameVersion().toString());

    m_fileNameTextField.setText(file.getCanonicalPath());
  }

  public void setLocal()
  {
    m_gameTypeComboBox.setSelectedItem(LOCAL);
  }

  public boolean isPBEM()
  {
    return m_gameTypeComboBox.getSelectedItem().equals(PBEM);
  }

  public boolean isServer()
  {
    return m_gameTypeComboBox.getSelectedItem().equals(SERVER);
  }

  public boolean isClient()
  {
    return m_gameTypeComboBox.getSelectedItem().equals(CLIENT);
  }

  public boolean isLocal()
  {
    return m_gameTypeComboBox.getSelectedItem().equals(LOCAL);
  }

  public void setGameName(String name)
  {
    m_gameName.setText(name);
  }

  public void setGameVersion(String version)
  {
    m_gameVersion.setText(version);
  }

  void m_helpButton_actionPerformed(ActionEvent e)
  {
    String helpText =
        "<b>Local</b> Play a game locally on this machine.<br><br>" +
        "<b>Server</b> Host a game.  You will need to tell other players your ip  or host name and the port that the game is using.  You will be able to see this information from the server tab after you select the server options<br><br>" +
        "<b>Client</b> Join a hosted game.  You will need to know the servers ip or host name and the port the server is running on.";

    JEditorPane pane = new JEditorPane();
    pane.setEditable(false);
    pane.setEditorKit(new javax.swing.text.html.HTMLEditorKit());
    pane.setText(helpText);
    pane.setPreferredSize(new Dimension(250,250));
    final JScrollPane scroll = new JScrollPane(pane);


    JOptionPane.showMessageDialog(this, scroll, "Game types", JOptionPane.PLAIN_MESSAGE);



  }

}
