package games.strategy.engine.pbem;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.framework.GameDataManager;
import games.strategy.engine.history.IDelegateHistoryWriter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;


public class PBEMMessagePoster implements java.io.Serializable
{
    private static final String TURNSUMMARY_MSGR_PROP_NAME = "games.strategy.engine.framework.PBEMMessagePoster.TURNSUMMARY_MSGR";
    private static final String SCREENSHOT_MSGR_PROP_NAME = "games.strategy.engine.framework.PBEMMessagePoster.SCREENSHOT_MSGR";
    private static final String SAVEGAME_MSGR_PROP_NAME = "games.strategy.engine.framework.PBEMMessagePoster.SAVEGAME_MSGR";

    private GameData m_gameData = null;
    private IDelegateHistoryWriter m_historyWriter = null;

    private IPBEMMessenger m_turnSummaryMessenger = null;
    private IPBEMMessenger m_screenshotMessenger = null;
    private IPBEMMessenger m_saveGameMessenger = null;

    private File m_screenshotFile = null;
    private String m_screenshotFilename = null;
    private InputStream m_screenshotFileIn = null;

    private File m_saveGameFile = null;
    private String m_saveGameFilename = null;
    private InputStream m_saveGameFileIn = null;

    private String m_turnSummary = null;

    private String m_screenshotRef = null;
    private String m_saveGameRef = null;
    private String m_turnSummaryRef = null;

    public PBEMMessagePoster()
    {
    }

    public PBEMMessagePoster(GameData gameData, IDelegateHistoryWriter historyWriter)
    {
        m_gameData = gameData;
        m_historyWriter = historyWriter;
        setTurnSummaryMessenger((IPBEMMessenger)gameData.getProperties().get(TURNSUMMARY_MSGR_PROP_NAME));
        setScreenshotMessenger((IPBEMMessenger)gameData.getProperties().get(SCREENSHOT_MSGR_PROP_NAME));
        setSaveGameMessenger((IPBEMMessenger)gameData.getProperties().get(SAVEGAME_MSGR_PROP_NAME));
    }

    public boolean hasMessengers()
    {
        return (m_turnSummaryMessenger != null 
                || m_screenshotMessenger != null
                || m_saveGameMessenger != null);
    }

    public void setTurnSummaryMessenger(IPBEMMessenger msgr)
    {
        m_turnSummaryMessenger = msgr;
    }

    public IPBEMTurnSummaryMessenger getTurnSummaryMessenger()
    {
        IPBEMMessenger msgr = m_turnSummaryMessenger;
        if(msgr instanceof IPBEMTurnSummaryMessenger)
            return (IPBEMTurnSummaryMessenger)msgr;
        else
            return null;
    }

    public void setScreenshotMessenger(IPBEMMessenger msgr)
    {
        m_screenshotMessenger = msgr;
    }

    public IPBEMScreenshotMessenger getScreenshotMessenger()
    {
        IPBEMMessenger msgr = m_screenshotMessenger;
        if(msgr instanceof IPBEMScreenshotMessenger)
            return (IPBEMScreenshotMessenger)msgr;
        else
            return null;
    }

    public void setSaveGameMessenger(IPBEMMessenger msgr)
    {
        m_saveGameMessenger = msgr;
    }

    public IPBEMSaveGameMessenger getSaveGameMessenger()
    {
        IPBEMMessenger msgr = m_saveGameMessenger;
        if(msgr instanceof IPBEMSaveGameMessenger)
            return (IPBEMSaveGameMessenger)msgr;
        else
            return null;
    }

    public void setTurnSummary(String turnSummary)
    {
        m_turnSummary = turnSummary;
    }

    public void setScreenshot(File screenshotFile)
        throws FileNotFoundException
    {
        if(screenshotFile != null)
            setScreenshot(screenshotFile.getName(), ((InputStream) (new FileInputStream(screenshotFile))));
        else
            setScreenshot(null, null);
    }

    public void setScreenshot(String filename, InputStream fileIn)
    {
        m_screenshotFilename = filename;
        m_screenshotFileIn = fileIn;
    }

    public void setSaveGame(File saveGameFile)
        throws FileNotFoundException
    {
        if(saveGameFile != null)
            setSaveGame(saveGameFile.getName(), ((InputStream) (new FileInputStream(saveGameFile))));
        else
            setSaveGame(null, null);
    }

    public void setSaveGame(String filename, InputStream fileIn)
    {
        m_saveGameFilename = filename;
        m_saveGameFileIn = fileIn;
    }

    public String getTurnSummaryRef()
    {
        return m_turnSummaryRef;
    }

    public String getScreenshotRef()
    {
        return m_screenshotRef;
    }

    public String getSaveGameRef()
    {
        return m_saveGameRef;
    }

    private void setTestData()
    {
        IPBEMScreenshotMessenger screenshotMsgr = getScreenshotMessenger();
        IPBEMSaveGameMessenger saveGameMsgr = getSaveGameMessenger();
        IPBEMTurnSummaryMessenger turnSummaryMsgr = getTurnSummaryMessenger();
        // set screenshot
        if(screenshotMsgr != null)
        {
            BufferedImage testImage = new BufferedImage(100, 50, BufferedImage.TYPE_INT_RGB);
            Graphics g = testImage.createGraphics();
            g.setFont(new Font("Ariel", Font.BOLD, 15));
            g.setColor(Color.WHITE);
            g.drawString("Test Post", 15, 15);

            try
            {
                ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                if(ImageIO.write(testImage, "jpg", byteOut))
                {
                    ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
                    setScreenshot("test.jpg", byteIn);
                }
            }
            catch(IOException ioe)
            {
                ioe.printStackTrace();
            }
        }

        // set save game
        if(saveGameMsgr != null)
        {
            File saveGameFile = null;
            try
            {
                saveGameFile = File.createTempFile("triplea", ".tsvg");
                (new GameDataManager()).saveGame(saveGameFile, m_gameData);
                setSaveGame(saveGameFile);
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
        // set turn summary
        if(turnSummaryMsgr != null)
            setTurnSummary("TripleA test for turn summary auto-posting.");
    }

    public boolean postTestData()
    {
        setTestData();
        return post();
    }

    public boolean post()
    {
        boolean retval = true;
        IPBEMScreenshotMessenger screenshotMsgr = getScreenshotMessenger();
        IPBEMSaveGameMessenger saveGameMsgr = getSaveGameMessenger();
        IPBEMTurnSummaryMessenger turnSummaryMsgr = getTurnSummaryMessenger();
        try
        {
            if(screenshotMsgr != null)
                if(screenshotMsgr.postScreenshot(m_screenshotFilename, m_screenshotFileIn))
                    m_screenshotRef = screenshotMsgr.getScreenshotRef();
                else
                    retval = false;
        }
        catch(Exception e) { }
        try
        {
            if(saveGameMsgr != null)
                if(saveGameMsgr.postSaveGame(m_saveGameFilename, m_saveGameFileIn))
                    m_saveGameRef = saveGameMsgr.getSaveGameRef();
                else
                    retval = false;
        }
        catch(Exception e) { }
        // screenshot/save game refs might be null here, if same IPBEMMessenger object is used for turnSummaryMsgr
        // pass them as-is, but re-fetch them below
        try
        {
            if(turnSummaryMsgr != null && !turnSummaryMsgr.postTurnSummary(m_turnSummary, m_screenshotRef, m_saveGameRef))
                retval = false;
        }
        catch(Exception e) { }
        // Re-fetch all refs and write history last. 
        // refs might not be set until all posts are done, 
        // and won't be set yet if there were posting errors
        if(m_historyWriter != null)
        {
            m_historyWriter.startEvent("Post Turn Summary");
            if(screenshotMsgr != null)
            {
                m_screenshotRef = screenshotMsgr.getScreenshotRef();
                if(m_screenshotRef != null)
                    m_historyWriter.addChildToEvent("Screenshot: "+m_screenshotRef, null);
            }
            if(saveGameMsgr != null)
            {
                m_saveGameRef = saveGameMsgr.getSaveGameRef();
                if(m_saveGameRef != null)
                    m_historyWriter.addChildToEvent("Save Game: "+m_saveGameRef, null);
            }
            if(turnSummaryMsgr != null)
            {
                m_turnSummaryRef = turnSummaryMsgr.getTurnSummaryRef();
                if(m_turnSummaryRef != null)
                    m_historyWriter.addChildToEvent("Turn Summary: "+m_turnSummaryRef, null);
            }
        }

        // finally, close input streams
        try
        {
            if(m_screenshotFileIn != null)
                m_screenshotFileIn.close();
        }
        catch(Exception e) { }
        try
        {
            if(m_saveGameFileIn != null)
                m_saveGameFileIn.close();
        }
        catch(Exception e) { }
        return retval;
    }

    public void storeMessengers(GameData gameData)
    {
        // If set to the NullPBEMMessenger object, just save null
        // to increase portability.
        Object saveObj;
        if(m_turnSummaryMessenger instanceof NullPBEMMessenger)
            saveObj = null;
        else
            saveObj = m_turnSummaryMessenger;
        gameData.getProperties().set(TURNSUMMARY_MSGR_PROP_NAME, saveObj);
        if(m_screenshotMessenger instanceof NullPBEMMessenger)
            saveObj = null;
        else
            saveObj = m_screenshotMessenger;
        gameData.getProperties().set(SCREENSHOT_MSGR_PROP_NAME, saveObj);
        if(m_saveGameMessenger instanceof NullPBEMMessenger)
            saveObj = null;
        else
            saveObj = m_saveGameMessenger;
        gameData.getProperties().set(SAVEGAME_MSGR_PROP_NAME, saveObj);
    }

}
