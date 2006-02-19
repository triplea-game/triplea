package games.strategy.engine.framework.startup.ui;

/**
* 18:12 17/02/2005
* Romain Guy <romain.guy@jext.org>
* Subject to the BSD license.
*/

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import java.awt.*;
import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
* An infinite progress panel displays a rotating figure and a message to notice the user of a long, duration unknown
* task. The shape and the text are drawn upon a white veil which alpha level (or shield value) lets the underlying
* component shine through. This panel is meant to be used asa <i>glass pane</i> in the window performing the long
* operation. <br /><br /> On the contrary to regular glass panes, you don't need to set it visible or not by yourself.
* Once you've started the animation all the mouse events are intercepted by this panel, preventing them from being
* forwared to the underlying components. <br /><br /> The panel can be controlled by the <code>start()</code>,
* <code>stop()</code> and <code>interrupt()</code> methods. <br /><br /> Example: <br /><br />
* <pre>InfiniteProgressPanel pane = new InfiniteProgressPanel();
* frame.setGlassPane(pane);
* pane.start()</pre>
* <br /><br /> Several properties can be configured at creation time. The message and its font can be changed at
* runtime. Changing the font can be done using <code>setFont()</code> and <code>setForeground()</code>.
*
* @author Romain Guy
* @version 1.0
*/

public class InfiniteProgressPanel extends JComponent implements ActionListener {

   private static final int NUMBER_OF_BARS = 14;
   private double m_dScale = 1.5d;
   private MouseAdapter m_oMouseAdapter = new MouseAdapter() {
   };
   private MouseMotionAdapter m_oMouseMotionAdapter = new MouseMotionAdapter() {
   };
   private KeyAdapter m_oKeyAdapter = new KeyAdapter() {
   };
   
   
   private Font font = new Font("Serif", Font.PLAIN, 32);
   /**
    * Disable back buffering if the window is resized
    */
   private ComponentAdapter m_oComponentAdapter = new ComponentAdapter() {
       public void componentResized(ComponentEvent e) {
           if (m_bUseBackBuffer == true){
               m_bUseBackBuffer = false;
               setOpaque(false);
               m_oImageBuf = null;
           }
       }
   };
   private BufferedImage m_oImageBuf = null;
   private Area[] m_oBars;
   private Rectangle m_oBarsBounds = null;
   private Rectangle m_oBarsScreenBounds = null;
   private AffineTransform m_oCenterAndScaleTransform = null;
   private Timer m_oTimer = new Timer(1000 / 15, this);
   private Color[] m_oColors = new Color[NUMBER_OF_BARS * 2];
   private int m_iColorOffset = 0;
   private boolean m_bUseBackBuffer;
   private boolean m_bTempHide = false;

   public InfiniteProgressPanel(boolean i_bUseBackBuffer) {
       m_bUseBackBuffer = i_bUseBackBuffer;
       // build bars
       m_oBars = buildTicker(NUMBER_OF_BARS);
       // calculate bars bounding rectangle
       m_oBarsBounds = new Rectangle();
       for (int i = 0; i < m_oBars.length; i++) {
           m_oBarsBounds = m_oBarsBounds.union(m_oBars[i].getBounds());
       }
       // create colors
       for (int i = 0; i < m_oBars.length; i++) {
           int channel = 224 - 128 / (i + 1);
           m_oColors[i] = new Color(channel, channel, channel);
           m_oColors[NUMBER_OF_BARS + i] = m_oColors[i];
       }
       // set cursor
       setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
       // set opaque
       setOpaque(m_bUseBackBuffer);
   }

   /**
    * Called to animate the rotation of the bar's colors
    */
   public void actionPerformed(ActionEvent e) {
       // rotate colors
       if (m_iColorOffset == NUMBER_OF_BARS) {
           m_iColorOffset = 0;
       } else {
           m_iColorOffset++;
       }
       // repaint
       if (m_oBarsScreenBounds != null) {
           repaint(m_oBarsScreenBounds);
       } else {
           repaint();
       }
   }

   /**
    * Show/Hide the pane, starting and stopping the animation as you go
    */
   public void setVisible(boolean i_bIsVisible) {
       // capture
       if (i_bIsVisible) {
           if (m_bUseBackBuffer) {
               try {
                   // get window contents rect
                   Window oWindow = SwingUtilities.getWindowAncestor(this);
                   Insets oInsets = oWindow.getInsets();
                   Rectangle oRectangle = new Rectangle(oWindow.getBounds());
                   oRectangle.x += oInsets.left;
                   oRectangle.y += oInsets.top;
                   oRectangle.width -= oInsets.left + oInsets.right;
                   oRectangle.height -= oInsets.top + oInsets.bottom;
                   // capture window contents
                   m_oImageBuf = new Robot().createScreenCapture(oRectangle);
                   // fade
                   Graphics2D oGraphics = m_oImageBuf.createGraphics();
                   oGraphics.setColor(new Color(255, 255, 255, 160));
                   oGraphics.fillRect(0, 0, m_oImageBuf.getWidth(), m_oImageBuf.getHeight());
                   oGraphics.dispose();
                   // add window resize listener
                   oWindow.addComponentListener(m_oComponentAdapter);
               } catch (AWTException e) {
                   e.printStackTrace();
               }
           }
           // capture events
           addMouseListener(m_oMouseAdapter);
           addMouseMotionListener(m_oMouseMotionAdapter);
           addKeyListener(m_oKeyAdapter);
           // start anim
           m_oTimer.start();
       } else {
           // stop anim
           m_oTimer.stop();
           /// free back buffer
           m_oImageBuf = null;
           // stop capturing events
           removeMouseListener(m_oMouseAdapter);
           removeMouseMotionListener(m_oMouseMotionAdapter);
           removeKeyListener(m_oKeyAdapter);
           // remove window resize listener
           Window oWindow = SwingUtilities.getWindowAncestor(this);
           if (oWindow != null) oWindow.removeComponentListener(m_oComponentAdapter);
       }
       super.setVisible(i_bIsVisible);
   }

   /**
    * Recalc bars based on changes in size
    */
   public void setBounds(int x, int y, int width, int height) {
       super.setBounds(x, y, width, height);
       // update centering transform
       m_oCenterAndScaleTransform = new AffineTransform();
       m_oCenterAndScaleTransform.translate((double) getWidth() / 2d, (double) getHeight() / 2d);
       m_oCenterAndScaleTransform.scale(m_dScale, m_dScale);
       // calc new bars bounds
       if (m_oBarsBounds != null) {
           Area oBounds = new Area(m_oBarsBounds);
           oBounds.transform(m_oCenterAndScaleTransform);
           m_oBarsScreenBounds = oBounds.getBounds();
       }
   }

   /**
    * paint background dimed and bars over top
    */
   protected void paintComponent(Graphics g) {
       if (!m_bTempHide) {
           Rectangle oClip = g.getClipBounds();
           if (m_oImageBuf != null) {
               // draw background image
               g.drawImage(m_oImageBuf,
                       oClip.x, oClip.y, oClip.x + oClip.width, oClip.y + oClip.height,
                       oClip.x, oClip.y, oClip.x + oClip.width, oClip.y + oClip.height,
                       null);
           } else {
               g.setColor(new Color(255, 255, 255, 160));
               g.fillRect(oClip.x, oClip.y, oClip.width, oClip.height);
           }
           // move to center
           Graphics2D g2 = (Graphics2D) g.create();
           g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
           g2.transform(m_oCenterAndScaleTransform);
           // draw ticker
           for (int i = 0; i < m_oBars.length; i++) {
               g2.setColor(m_oColors[i + m_iColorOffset]);
               g2.fill(m_oBars[i]);
           }
       }
       
       g.setColor(Color.BLACK);
       //g.fillRect(10,10,100,100);
       g.setFont(font);
       g.drawString("Loading, Please Wait", 50,220);
   }

   /**
    * Builds the circular shape and returns the result as an array of <code>Area</code>. Each <code>Area</code> is one
    * of the bars composing the shape.
    */
   private static Area[] buildTicker(int i_iBarCount) {
       Area[] ticker = new Area[i_iBarCount];
//        Point2D.Double center = new Point2D.Double((double) i_iWidth / 2, (double) i_iHeight / 2);
       Point2D.Double center = new Point2D.Double(0, 0);
       double fixedAngle = 2.0 * Math.PI / ((double) i_iBarCount);

       for (double i = 0.0; i < (double) i_iBarCount; i++) {
           Area primitive = buildPrimitive();

           AffineTransform toCenter = AffineTransform.getTranslateInstance(center.getX(), center.getY());
           AffineTransform toBorder = AffineTransform.getTranslateInstance(45.0, -6.0);
           AffineTransform toCircle = AffineTransform.getRotateInstance(-i * fixedAngle, center.getX(), center.getY());

           AffineTransform toWheel = new AffineTransform();
           toWheel.concatenate(toCenter);
           toWheel.concatenate(toBorder);

           primitive.transform(toWheel);
           primitive.transform(toCircle);

           ticker[(int) i] = primitive;
       }

       return ticker;
   }

   /**
    * Builds a bar.
    */
   private static Area buildPrimitive() {
       Rectangle2D.Double body = new Rectangle2D.Double(6, 0, 30, 12);
       Ellipse2D.Double head = new Ellipse2D.Double(0, 0, 12, 12);
       Ellipse2D.Double tail = new Ellipse2D.Double(30, 0, 12, 12);

       Area tick = new Area(body);
       tick.add(new Area(head));
       tick.add(new Area(tail));

       return tick;
   }

   /**
    * Test Main Method
    */
   public static void main(String[] args) {
       final JFrame oTestFrame = new JFrame("Test");
       oTestFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
       oTestFrame.setSize(400, 400);

       JButton oButton = new JButton("Start!");
       oButton.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent e) {
               final InfiniteProgressPanel oWaitingPane = new InfiniteProgressPanel(false);
               oTestFrame.setGlassPane(oWaitingPane);
               oWaitingPane.setVisible(true);
               Thread oThread = new Thread() {
                   public void run() {
                       synchronized (this) {
                           try {
                               wait(15000);
                           } catch (InterruptedException e1) {
                               e1.printStackTrace();
                           }
                       }
                       oWaitingPane.setVisible(false);
                   }
               };
               oThread.start();
           }
       });
       oTestFrame.getContentPane().add(oButton);

       JButton oBufferedButton = new JButton("Start Back Buffered!");
       oBufferedButton.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent e) {
               final InfiniteProgressPanel oWaitingPane = new InfiniteProgressPanel(true);
               oTestFrame.setGlassPane(oWaitingPane);
               oWaitingPane.setVisible(true);
               Thread oThread = new Thread() {
                   public void run() {
                       synchronized (this) {
                           try {
                               wait(15000);
                           } catch (InterruptedException e1) {
                               e1.printStackTrace();
                           }
                       }
                       oWaitingPane.setVisible(false);
                   }
               };
               oThread.start();
           }
       });
       oTestFrame.getContentPane().add(oBufferedButton, BorderLayout.SOUTH);

       oTestFrame.setVisible(true);
   }
} 