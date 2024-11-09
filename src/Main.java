/*
 * **************************************************************************
 * *                                                                        *
 * * Ericsson hereby grants to the user a royalty-free, irrevocable,        *
 * * worldwide, nonexclusive, paid-up license to copy, display, perform,    *
 * * prepare and have prepared derivative works based upon the source code  *
 * * in this sample application, and distribute the sample source code and  *
 * * derivative works thereof and to grant others the foregoing rights.     *
 * *                                                                        *
 * * ERICSSON DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE,        *
 * * INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS.       *
 * * IN NO EVENT SHALL ERICSSON BE LIABLE FOR ANY SPECIAL, INDIRECT OR      *
 * * CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS    *
 * * OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE  *
 * * OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE *
 * * OR PERFORMANCE OF THIS SOFTWARE.                                       *
 * *                                                                        *
 * **************************************************************************
 */

import com.ericsson.hosasdk.api.*;
import com.ericsson.hosasdk.utility.framework.FWproxy;
import com.ericsson.hosasdk.utility.log.*;
import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class Main
{
    private Feature theFeature;
    private GUI theGUI;
    private boolean theIsStarted, theIsStopped;
    private FWproxy itsFramework;

    public static void main(String[] args)
        throws Exception
    {
        new Main("config/config.ini");
    }

    private Main(String aConfigFileName)
        throws IOException
    {
        SimpleTracer.SINGLETON.PRINT_STACKTRACES = false;
        HOSAMonitor.addListener(SimpleTracer.SINGLETON);

        Properties p = new Properties();
        p.load(new FileInputStream(aConfigFileName));
        itsFramework = new FWproxy(p);
        theFeature = new Feature(itsFramework, p);

        String s = "<HTML>" 
            + "<font size=\"4\"><p align=center>The Missed Calls Messenger detects when a user doesn't answer a call within "
            + theFeature.getWaitPeriodInSeconds() + " seconds.</p>"
            + "<p align=center>It then sends an SMS to this user.</p></font>"
            + "<font size=\"3\"><p align=center>The set of users is hardcoded to 1 and 2.</p></font>"
            + "</HTML>";

        initGUI(s);
        theIsStopped = true;
        theGUI.updateState();
    }

    public boolean isStarted()
    {
        return theIsStarted;
    }

    public boolean isStopped()
    {
        return theIsStopped;
    }

    public void start()
    {
        theIsStarted = theIsStopped = false;
        theGUI.updateState();
        try
        {
            theFeature.start();
            theFeature.addUser("1");
            theFeature.addUser("2");
            theIsStarted = true;
        }
        catch (RuntimeException e)
        {
            theIsStopped = true;
            System.err.println(ObjectWriter.print(e));
            // throw e;
        }
        finally 
        {
            theGUI.updateState();
        }
    }

    public void stop()
    {
        theIsStarted = theIsStopped = false;
        theGUI.updateState();
        try
        {
            theFeature.stop();
        }
        finally 
        {
            theIsStopped = true;
            theGUI.updateState();
        }
    }

    void initGUI(String aDescription)
    {
        JFrame f = new JFrame();
        f.setTitle("MissedCalls");
        f.getContentPane().setLayout(new BorderLayout());
        f.getContentPane().add(theGUI = new GUI(this, aDescription),
            BorderLayout.CENTER);
        f.pack();
        f.setLocation(100, 100);
        f.setVisible(true);
        f.addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                try
                {
                    if (itsFramework != null)
                    {
                        itsFramework.endAccess(null);
                        itsFramework.dispose();
                    }
                }
                finally
                {
                    System.exit(0);
                }
            }
        });
    }
}

