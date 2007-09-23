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
 * AllYouCanUploadDotComPBEMMessenger.java
 *
 *
 * Created on November 21, 2006, 8:56 PM
 */

package games.strategy.engine.pbem;

import games.strategy.net.MultiPartFormOutputStream;
import java.io.*;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Tony Clayton
 * @version 1.0
 */
public class AllYouCanUploadDotComPBEMMessenger
    implements IPBEMScreenshotMessenger
{
    private transient String m_screenshotRef = null;

    public AllYouCanUploadDotComPBEMMessenger()
    {
    }

    public String getName()
    {
        return "www.AllYouCanUpload.com";
    }

    public boolean getNeedsUsername()
    {
        return false;
    }

    public boolean getNeedsPassword()
    {
        return false;
    }

    public boolean getCanViewPosted()
    {
        return false;
    }

    public void viewPosted()
    {
    }

    public void setGameId(String gameId)
    {
    }

    public String getGameId()
    {
        return null;
    }

    public void setUsername(String username)
    {
    }

    public String getUsername()
    {
        return null;
    }

    public void setPassword(String password)
    {
    }

    public String getPassword()
    {
        return null;
    }

    public boolean postScreenshot(String fileName, InputStream fileIn)
        throws IOException
    {
        URL url = null;
        URLConnection urlConn = null;
        MultiPartFormOutputStream out = null;

        m_screenshotRef = null;

        // set up connection
        try
        {
            url = new URL("http://allyoucanupload.webshots.com/uploadcomplete");
        }
        catch(MalformedURLException e)
        {
            e.printStackTrace();
            return false;
        }
        try
        {
            String boundary = MultiPartFormOutputStream.createBoundary();
            urlConn = MultiPartFormOutputStream.createConnection(url);
            urlConn.setRequestProperty("Accept", "*/*");
            urlConn.setRequestProperty("Content-Type", MultiPartFormOutputStream.getContentType(boundary));
            urlConn.setRequestProperty("Connection", "Keep-Alive");
            urlConn.setRequestProperty("Cache-Control", "no-cache");
            ((HttpURLConnection)urlConn).setInstanceFollowRedirects(true);
            out = new MultiPartFormOutputStream(urlConn.getOutputStream(), boundary);
            out.writeField("imagesCount", "1");
            out.writeField("images[0].submittedPhotoSize", "100%");
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return false;
        }
        // this one throws an exception
        out.writeFile("images[0].fileName", "image/png", fileName, fileIn);

        // send request to server
        out.close();

        int code = ((HttpURLConnection)urlConn).getResponseCode();
        if(code != 200)
	{
            // http error
            String msg = ((HttpURLConnection)urlConn).getResponseMessage();
            m_screenshotRef = String.valueOf(code)+": "+msg;
            return false;
	}
        // read response from server
        try
        {
            BufferedReader in = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
            String line = "";
            // server will always serve images in .jpg format
            Pattern p = Pattern.compile(".*?<input type=\"text\" .*?value=\"(http://.*?jpg)\">.*");
            while ((line = in.readLine()) != null)
            {
                Matcher m = p.matcher(line);
                if(m.matches())
                    m_screenshotRef = m.group(1);
            }
            in.close();
        }
        catch(IOException ioe)
        {
            ioe.printStackTrace();
            return false;
        }
        if(m_screenshotRef == null)
        {
            m_screenshotRef = "Error: screenshot URL could not be found after posting.";
            return false;
        }

        return true;
    }

    public String getScreenshotRef()
    {
        return m_screenshotRef;
    }

    public String toString()
    {
        return getName();
    }
}
