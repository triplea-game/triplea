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

/*
 * AxisAndAlliesDotOrgPBEMMessenger.java
 *
 *
 * Created on November 21, 2006, 8:56 PM
 */

package games.strategy.triplea.pbem;

import games.strategy.engine.pbem.IPBEMSaveGameMessenger;
import games.strategy.engine.pbem.IPBEMTurnSummaryMessenger;
import games.strategy.net.BrowserControl;
import games.strategy.net.MultiPartFormOutputStream;
import java.io.*;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Tony Clayton
 * @version 1.0
 */
public class AxisAndAlliesDotOrgPBEMMessenger
    implements IPBEMTurnSummaryMessenger, IPBEMSaveGameMessenger
{
    private static final int m_loginDelayInSeconds = 16;
    private static final String m_host = "www.axisandallies.org";

    private transient String m_username = null;
    private transient String m_password = null;
    private transient String m_cookies = null;
    private transient String m_referer = null;
    
    private transient String m_sesc = null;
    private transient String m_numReplies = null;
    private transient String m_msgNum = null;
    private transient String m_attachId = null;
    private transient String m_seqNum = null;

    private transient String m_screenshotRef = null;
    private transient String m_saveGameRef = null;
    private transient String m_turnSummaryRef = null;

    private transient String m_saveGameFileName = null;
    private transient InputStream m_saveGameFileIn = null;

    private String m_gameId = null;

    public AxisAndAlliesDotOrgPBEMMessenger()
    {
    }

    public String getName()
    {
        return "www.AxisAndAllies.org";
    }

    public boolean getNeedsUsername()
    {
        return true;
    }

    public boolean getNeedsPassword()
    {
        return true;
    }

    public boolean getCanViewPosted()
    {
        return true;
    }

    public void viewPosted()
    {
        String url = "http://"+m_host+"/forums/index.php?topic="+m_gameId+".new#new";
        BrowserControl.displayURL(url);
    }

    public void setGameId(String gameId)
    {
        m_gameId = gameId;
    }

    public String getGameId()
    {
        return m_gameId;
    }

    public void setUsername(String username)
    {
        m_username = username;
    }

    public String getUsername()
    {
        return m_username;
    }

    public void setPassword(String password)
    {
        m_password = password;
    }

    public String getPassword()
    {
        return m_password;
    }

    private void readCookies(URLConnection urlConn)
    {
        // read cookies from server
        int n=1; // n=0 has no key, and the HTTP return status in the value field
        for(boolean done = false; !done; n++)
        {
            String headerKey = urlConn.getHeaderFieldKey(n);
            String headerVal = urlConn.getHeaderField(n);
            if(headerKey != null || headerVal != null)
            {
                if(!headerKey.equals("Set-Cookie"))
                    continue;
                String cookie = headerVal.substring(0, headerVal.indexOf(";"));
                if(m_cookies == null)
                {
                    m_cookies = cookie;
                    continue;
                }
                if(m_cookies.indexOf(cookie) == -1)
                    m_cookies = m_cookies+"; "+cookie;
            } else
            {
                done = true;
            }
        }

    }

    private boolean postLogin()
    {
        URL url = null;
        URLConnection urlConn = null;
        OutputStream out = null;
        int code = 0;
        boolean gotPhpsessid = false;
        // set up connection
        try
        {
            url = new URL("http://"+m_host+"/forums/index.php?action=login2");
        }
        catch(MalformedURLException e)
        {
            e.printStackTrace();
            return false;
        }

        try
        {
            urlConn = url.openConnection();
            ((HttpURLConnection)urlConn).setRequestMethod("POST");
            ((HttpURLConnection)urlConn).setInstanceFollowRedirects(false);
            urlConn.setDoOutput(true);
            urlConn.setDoInput(true);
            urlConn.setRequestProperty("Host", m_host);
            urlConn.setRequestProperty("Accept", "*/*");
            urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            urlConn.setRequestProperty("Connection", "Keep-Alive");
            urlConn.setRequestProperty("Cache-Control", "no-cache");

            // send request to server
            out = urlConn.getOutputStream();
            out.write(new String("user="+m_username+"&passwrd="+m_password+"&cookielength=60").getBytes());
            out.flush();
            out.close();
            do 
            {
                readCookies(urlConn);
                m_referer = url.toString();
                code = ((HttpURLConnection)urlConn).getResponseCode();
                if(code == 301 || code == 302)
                {
                    // redirect
                    url = new URL(urlConn.getHeaderField("Location"));
                    urlConn = url.openConnection();
                    ((HttpURLConnection)urlConn).setRequestMethod("GET");
                    ((HttpURLConnection)urlConn).setInstanceFollowRedirects(false);
                    urlConn.setDoOutput(true);
                    urlConn.setDoInput(true);
                    urlConn.setRequestProperty("Host", m_host);
                    urlConn.setRequestProperty("Accept", "*/*");
                    urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    urlConn.setRequestProperty("Connection", "Keep-Alive");
                    urlConn.setRequestProperty("Cache-Control", "no-cache");
                    if(m_referer != null)
                        urlConn.setRequestProperty("Referer", m_referer);
                    if(m_cookies != null)
                        urlConn.setRequestProperty("Cookie", m_cookies);
                    continue;
                }
                if(code == 200)
                {
                    // fetch PHPSESSID
                    String refreshHdr = urlConn.getHeaderField("Refresh");
                    Pattern p_phpsessid = Pattern.compile(".*?\\?PHPSESSID=(\\w+).*");
                    Matcher match_phpsessid = p_phpsessid.matcher(refreshHdr);
                    if(match_phpsessid.matches())
                    {
                        //m_phpsessid = match_phpsessid.group(1);
                        gotPhpsessid= true;
                    }
                    urlConn = null;
                    continue;
                }
                // http error
                String msg = ((HttpURLConnection)urlConn).getResponseMessage();
                m_turnSummaryRef = String.valueOf(code) + ": " + msg;
                return false;
            } while (urlConn != null);

        } catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }

        if(!gotPhpsessid)
        {
            m_turnSummaryRef = "Error: PHPSESSID not found after login. ";
            return false;
        }
        if(m_cookies == null)
        {
            m_turnSummaryRef = "Error: cookies not found after login. ";
            return false;
        }
        try
        {
            Thread.sleep(m_loginDelayInSeconds * 1000);
        }
        catch(InterruptedException ie)
        {
            ie.printStackTrace();
        }
        return true;
    }

    private boolean goToForum()
    {
        URL url = null;
        URLConnection urlConn = null;
        
        int code = 0;
        boolean gotNumReplies = false;
        boolean gotMsgNum = false;
        // set up connection
        try
        {
            url = new URL("http://"+m_host+"/forums/index.php?topic="+m_gameId+".new");
        }
        catch(MalformedURLException e)
        {
            e.printStackTrace();
            return false;
        }

        try
        {
            urlConn = url.openConnection();
            ((HttpURLConnection)urlConn).setRequestMethod("GET");
            ((HttpURLConnection)urlConn).setInstanceFollowRedirects(false);
            urlConn.setDoOutput(false);
            urlConn.setDoInput(true);
            urlConn.setRequestProperty("Host", m_host);
            urlConn.setRequestProperty("Accept", "*/*");
            urlConn.setRequestProperty("Connection", "Keep-Alive");
            urlConn.setRequestProperty("Cache-Control", "no-cache");
            if(m_referer != null)
                urlConn.setRequestProperty("Referer", m_referer);
            if(m_cookies != null)
                urlConn.setRequestProperty("Cookie", m_cookies);

            m_referer = url.toString();
            readCookies(urlConn);
            code = ((HttpURLConnection)urlConn).getResponseCode();
            if(code != 200)
            {
                // http error
                String msg = ((HttpURLConnection)urlConn).getResponseMessage();
                m_turnSummaryRef = String.valueOf(code) + ": " + msg;
                return false;
            }

            // parse num_replies
            BufferedReader in = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
            String line = "";
            Pattern p_numReplies = Pattern.compile(".*?;num_replies=(\\d+)\".*");
            // parse msg num
            Pattern p_msgNum = Pattern.compile(".*?<a name=\"msg(\\d+)\"></a><a name=\"new\"></a>.*");
            // parse attachment id
            Pattern p_attachId = Pattern.compile(".*?action=dlattach;topic="+m_gameId+".0;attach=(\\d+)\">.*");
            while ((line = in.readLine()) != null)
            {
                if (!gotNumReplies)
                {
                    Matcher match_numReplies = p_numReplies.matcher(line);
                    if(match_numReplies.matches())
                    {
                        m_numReplies = match_numReplies.group(1);
                        gotNumReplies = true;
                        continue;
                    }
                }
                if (!gotMsgNum)
                {
                    Matcher match_msgNum = p_msgNum.matcher(line);
                    if(match_msgNum.matches())
                    {
                        m_msgNum = match_msgNum.group(1);
                        gotMsgNum = true;
                        continue;
                    } 
                }

                // must match every line... always take the last attachId
                Matcher match_attachId = p_attachId.matcher(line);
                if(match_attachId.matches())
                    m_attachId = match_attachId.group(1);
            }
            in.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return false;
        }
        if(!gotNumReplies || !gotMsgNum)
        {
            m_turnSummaryRef = "Error: ";
            if(!gotNumReplies)
                m_turnSummaryRef += "No num_replies found in A&A.org forum topic. ";

            if(!gotMsgNum)
                m_turnSummaryRef += "No msgXXXXXX found in A&A.org forum topic. ";
            return false;
        }
        return true;
    }

    private boolean preparePost()
    {
        URL url = null;
        URLConnection urlConn = null;
        
        int code = 0;
        boolean gotNumReplies = false;
        boolean gotSeqNum = false;
        boolean gotSesc = false;
        try
        {
            url = new URL("http://"+m_host+"/forums/index.php?action=post;topic="+m_gameId+".0;num_replies="+m_numReplies);
        }
        catch(MalformedURLException e)
        {
            e.printStackTrace();
        }

        try {
            urlConn = url.openConnection();
            ((HttpURLConnection)urlConn).setRequestMethod("GET");
            ((HttpURLConnection)urlConn).setInstanceFollowRedirects(false);
            urlConn.setDoOutput(false);
            urlConn.setDoInput(true);
            urlConn.setRequestProperty("Host", m_host);
            urlConn.setRequestProperty("Accept", "*/*");
            urlConn.setRequestProperty("Connection", "Keep-Alive");
            urlConn.setRequestProperty("Cache-Control", "no-cache");
            if(m_referer != null)
                urlConn.setRequestProperty("Referer", m_referer);
            if(m_cookies != null)
                urlConn.setRequestProperty("Cookie", m_cookies);

            m_referer = url.toString();
            readCookies(urlConn);
            code = ((HttpURLConnection)urlConn).getResponseCode();
            if(code != 200)
            {
                // http error
                String msg = ((HttpURLConnection)urlConn).getResponseMessage();
                m_turnSummaryRef = String.valueOf(code)+": "+msg;
                return false;
            }

            // parse num_replies again
            BufferedReader in = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
            String line = "";
            Pattern p_numReplies = Pattern.compile(".*?<input type=\"hidden\" name=\"num_replies\" value=\"(\\d+)\" />.*");
            // parse seqnum
            Pattern p_seqNum = Pattern.compile(".*?<input type=\"hidden\" name=\"seqnum\" value=\"(\\d+)\" />.*");
            // parse sesc/sc
            Pattern p_sesc = Pattern.compile(".*?<input type=\"hidden\" name=\"sc\" value=\"(\\w+)\" />.*");
            while ((line = in.readLine()) != null)
            {
                if (!gotNumReplies)
                {
                    Matcher match_numReplies = p_numReplies.matcher(line);
                    if(match_numReplies.matches())
                    {
                        m_numReplies = match_numReplies.group(1);
                        gotNumReplies = true;
                        continue;
                    } 
                }
                if (!gotSeqNum)
                {
                    Matcher match_seqNum = p_seqNum.matcher(line);
                    if(match_seqNum.matches())
                    {
                        m_seqNum = match_seqNum.group(1);
                        gotSeqNum = true;
                        continue;
                    }
                }
                if (!gotSesc)
                {
                    Matcher match_sesc = p_sesc.matcher(line);
                    if(match_sesc.matches())
                    {
                        m_sesc = match_sesc.group(1);
                        gotSesc = true;
                        continue;
                    }
                }
            }
            in.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return false;
        }

        if(!gotNumReplies || !gotSeqNum || !gotSesc)
        {
            m_turnSummaryRef = "Error: ";
            if(!gotNumReplies)
                m_turnSummaryRef += "No num_replies found in A&A.org post form. ";
            if(!gotSeqNum)
                m_turnSummaryRef += "No seqnum found in A&A.org post form. ";
            if(!gotSesc)
                m_turnSummaryRef += "No sc found in A&A.org post form. ";
            return false;
        }
        return true;
    }

    private String getNextNumReplies()
    {
        int postNum = Integer.parseInt(m_numReplies) + 1;
        return String.valueOf(postNum);
    }

    private String getNextReplyUrl()
    {
     
        return "http://"+m_host+"/forums/index.php?topic="+m_gameId+"."+getNextNumReplies()+"#msg"+m_msgNum;
    }

    private String getNewReplyUrl()
    {
        return "http://"+m_host+"/forums/index.php?topic="+m_gameId+"."+m_numReplies+"#msg"+m_msgNum;
    }

    private String getNewAttachUrl()
    {
        return "http://"+m_host+"/forums/index.php?action=dlattach;topic="+m_gameId+".0;attach="+m_attachId;
    }

    public boolean postTurnSummary(String summary, String screenshotRef, String saveGameRef)
    {
        URL url = null;
        URLConnection urlConn = null;
        MultiPartFormOutputStream out = null;
        String forumSummary = "";
        String screenshotSummary = "";
        String saveGameSummary = "";
        String finalSummary = "";

        m_turnSummaryRef = null;
        m_screenshotRef = screenshotRef;
        // if we aren't the save game poster then set it
        if(m_saveGameFileIn == null)
            m_saveGameRef = saveGameRef;
        // first, login if necessary
        if(m_cookies == null && !postLogin())
            return false;
        // now, go to forum
        if(!goToForum())
            return false;
        // now, prepare post
        if(!preparePost())
            return false;

        // set up connection
        try
        {
            url = new URL("http://"+m_host+"/forums/index.php?action=post2;start=0;board=40");
        }
        catch(MalformedURLException e)
        {
            e.printStackTrace();
            return false;
        }
        forumSummary = "\n\nA&A Forum : "+getNextReplyUrl();
        if(m_screenshotRef != null)
            screenshotSummary = "\nScreenshot: "+m_screenshotRef;
        if(m_saveGameRef != null)
            saveGameSummary = "\nSave Game : "+m_saveGameRef;
        finalSummary = summary+forumSummary+screenshotSummary+saveGameSummary;
        int code;
        BufferedReader in;
        String line;
        Pattern p1;
        Pattern p2;
        Pattern p3;

        // send request to server
        try {
            String boundary = MultiPartFormOutputStream.createBoundary();
            urlConn = MultiPartFormOutputStream.createConnection(url);
            ((HttpURLConnection)urlConn).setInstanceFollowRedirects(false);
            urlConn.setRequestProperty("Host", m_host);
            urlConn.setRequestProperty("Accept", "*/*");
            urlConn.setRequestProperty("Content-Type", MultiPartFormOutputStream.getContentType(boundary));
            urlConn.setRequestProperty("Connection", "keep-alive");
            urlConn.setRequestProperty("Cache-Control", "no-cache");
            if(m_referer != null)
                urlConn.setRequestProperty("Referer", m_referer);
            if(m_cookies != null)
                urlConn.setRequestProperty("Cookie", m_cookies);
            m_referer = url.toString();
            out = new MultiPartFormOutputStream(urlConn.getOutputStream(), boundary);
            // send request
            out.writeField("topic", m_gameId);
            out.writeField("subject", "TripleA Turn Summary Post "+m_gameId+"."+getNextNumReplies());
            out.writeField("icon", "xx");
            out.writeField("message", finalSummary);
            out.writeField("notify", "0");
            out.writeField("goback", "1");
            out.writeField("ns", "NS");
            // empty attachment
            if(m_saveGameFileIn != null)
            {
                out.writeFile("attachment[]", "application/octet-stream", m_saveGameFileName, m_saveGameFileIn);
                out.writeField("attachmentPreview", "");
            }
            out.writeField("post", "Post");
            out.writeField("num_replies", m_numReplies);
            out.writeField("sc", m_sesc);
            out.writeField("seqnum", m_seqNum);
            out.close();
            code = ((HttpURLConnection)urlConn).getResponseCode();
            if(code == 200)
            {
                // reporting an error
     
                in = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
                line = "";
                p1 = Pattern.compile(".*?<b>(The following error or errors occurred .*?)</b>.*");
                p2 = Pattern.compile(".*?<td>(An Error Has Occurred!)</td>.*");
                p3 = Pattern.compile(".*?session timed out.*"); // found after p1
                while ((line = in.readLine()) != null)
                {
                    Matcher m1 = p1.matcher(line);
                    if(m1.matches())
                    {
                        line = in.readLine(); // div start
                        line = in.readLine().trim(); // message
                        Matcher m3 = p3.matcher(line);
                        if(m3.matches())
                        {
                            // login again and repost
                            m_cookies = null;
                            postTurnSummary(summary, screenshotRef, saveGameRef);
                        }
                        m_turnSummaryRef = m1.group(1)+"\n"+line;
                        in.close();
                        return false;
                    }

                    Matcher m2 = p2.matcher(line);
                    if(m2.matches())
                    {
                        line = in.readLine(); // </tr>
                        line = in.readLine(); // <tr..>
                        line = in.readLine(); // <td..>
                        line = in.readLine(); // message
                        m_turnSummaryRef = m2.group(1)+"\n"+line.trim();
                        in.close();
                        return false;
                    }

                    m_turnSummaryRef = "An unknown error occurred while POSTing.";
                    in.close();
                    return false;
                }
            }

            if(code != 301 && code != 302)
            {
                // http error
                String msg = ((HttpURLConnection)urlConn).getResponseMessage();
                m_turnSummaryRef = String.valueOf(code)+": "+msg;
                return false;
            }

                // success!
                // now, go back to forum
            if(!goToForum())
                return false;

        } catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
    

        m_turnSummaryRef = getNewReplyUrl();
        if(m_saveGameFileIn != null)
        {
            if(m_attachId == null)
            {
                m_saveGameRef = "Error: attachment id not found in A&A.org post.";
                return false;
            }
            m_saveGameRef = getNewAttachUrl();
        }
        return true;
    }

    public String getTurnSummaryRef()
    {
        return m_turnSummaryRef;
    }

    public boolean postSaveGame(String filename, InputStream fileIn)
    {
        // not actually posting, just holding the reference
        m_saveGameRef = null;
        m_saveGameFileName = filename;
        m_saveGameFileIn = fileIn;
        return true;
    }

    public String getSaveGameRef()
    {
        return m_saveGameRef;
    }

    public String toString()
    {
        return getName();
    }

}
