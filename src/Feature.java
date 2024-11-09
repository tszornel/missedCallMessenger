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

import com.ericsson.hosasdk.utility.framework.FWproxy;
import com.ericsson.hosasdk.api.*;
import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class Feature
{
    private FWproxy itsFramework;
    private MissedCallDetector itsMissedCallDetector;
    private Messenger itsMessenger;

    public Feature(FWproxy aFramework, Properties p)
    {
        itsFramework = aFramework;
        itsMissedCallDetector = new MissedCallDetector(this,
            itsFramework);
        itsMessenger = new Messenger(this, itsFramework);
    }

    public int getWaitPeriodInSeconds()
    {
        return itsMissedCallDetector.getWaitPeriodInSeconds();
    }

    public String getDescription()
    {
        return "";
    }

    public void start()
    {
        itsMissedCallDetector.start();
        itsMessenger.start();
    }

    public void addUser(String aUser)
    {
        itsMissedCallDetector.startNotifications(aUser);
    }

    public void missedCall(String anOrigin, String aDestination)
    {
        itsMessenger.sendSMS(anOrigin, aDestination,
            "Call from " + anOrigin + " missed");
    }

    public void stop()
    {
        itsMissedCallDetector.stop();
        itsMessenger.stop();
    }
}
