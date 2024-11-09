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
import com.ericsson.hosasdk.utility.log.ObjectWriter;
import com.ericsson.hosasdk.api.cc.TpCallNotificationInfo;
import com.ericsson.hosasdk.api.cc.TpReleaseCause;
import com.ericsson.hosasdk.api.cc.mpccs.IpAppMultiPartyCall;
import com.ericsson.hosasdk.api.cc.mpccs.IpAppMultiPartyCallControlManager;
import com.ericsson.hosasdk.api.cc.mpccs.IpAppMultiPartyCallControlManagerAdapter;
import com.ericsson.hosasdk.api.cc.mpccs.TpAppMultiPartyCallBack;
import com.ericsson.hosasdk.api.cc.mpccs.TpCallLegIdentifier;
import com.ericsson.hosasdk.api.cc.mpccs.TpMultiPartyCallIdentifier;
import com.ericsson.hosasdk.api.SDKCommunicationException;
import com.ericsson.hosasdk.utility.sync.ThreadPool;
import com.ericsson.hosasdk.api.TpAddress;
import com.ericsson.hosasdk.api.TpAddressPlan;
import com.ericsson.hosasdk.api.TpAddressPresentation;
import com.ericsson.hosasdk.api.TpAddressRange;
import com.ericsson.hosasdk.api.TpAddressScreening;
import com.ericsson.hosasdk.api.cc.*;
import com.ericsson.hosasdk.api.cc.mpccs.IpAppCallLeg;
import com.ericsson.hosasdk.api.cc.mpccs.IpAppCallLegMgr;
import com.ericsson.hosasdk.api.cc.mpccs.IpAppMultiPartyCall;
import com.ericsson.hosasdk.api.cc.mpccs.IpAppMultiPartyCallControlManager;
import com.ericsson.hosasdk.api.cc.mpccs.IpAppMultiPartyCallControlManagerMgr;
import com.ericsson.hosasdk.api.cc.mpccs.IpAppMultiPartyCallMgr;
import com.ericsson.hosasdk.api.cc.mpccs.IpMultiPartyCall;
import com.ericsson.hosasdk.api.cc.mpccs.IpMultiPartyCallControlManager;
import com.ericsson.hosasdk.api.cc.mpccs.TpCallLegIdentifier;
import com.ericsson.hosasdk.api.cc.mpccs.TpMultiPartyCallIdentifier;
import java.util.*;

public class MissedCallDetector extends IpAppMultiPartyCallControlManagerAdapter
    implements IpAppMultiPartyCallControlManager
{
    private Feature theMain;
    private ThreadPool itsThreadPool;
    private TpAppMultiPartyCallBack theCallback;
    private IpMultiPartyCallControlManager itsService;
    private FWproxy itsFramework;
    private Map theNotifications = new HashMap(); // user -> assignment Id
    private Map theTimerTasks = new HashMap(); // call Id -> TimerTask
    private Timer theTimer = new Timer();

    public MissedCallDetector(Feature aMain, FWproxy aFramework)
    {
        theMain = aMain;
        itsFramework = aFramework;
        theCallback = new TpAppMultiPartyCallBack();
        theCallback.AppMultiPartyCall(null);
        itsThreadPool = new ThreadPool();
    }

    public int getWaitPeriodInSeconds()
    {
        return 15;
    }

    public void start()
    {
        itsService = (IpMultiPartyCallControlManager) itsFramework.obtainSCF(IpMultiPartyCallControlManager.class,
            "P_MULTI_PARTY_CALL_CONTROL");
    }

    public void stop()
    {
        try
        {
            stopAllNotifications();
             itsFramework.releaseSCF(itsService);
        }
        finally  
        {	// even if disposing SCS resources fails, 
            // we still want to dispose of our resources
            IpAppMultiPartyCallControlManagerMgr.dispose(this);
        }
    }

    public void startNotifications(String aUser)
    {
        TpAddressRange origin = createE164Range("*");
        TpAddressRange destination = createE164Range(aUser);
        TpCallNotificationScope scope = new TpCallNotificationScope(destination,
            origin);

        TpCallEventType eventType1 = TpCallEventType.P_CALL_EVENT_ADDRESS_COLLECTED;
        TpAdditionalCallEventCriteria additionalCriteria1 = new TpAdditionalCallEventCriteria();
        additionalCriteria1.MinAddressLength(0);
        TpCallMonitorMode mode1 = TpCallMonitorMode.P_CALL_MONITOR_MODE_NOTIFY;
        TpCallEventRequest request1 = new TpCallEventRequest(eventType1,
            additionalCriteria1, mode1);

        TpCallEventType eventType2 = TpCallEventType.P_CALL_EVENT_ANSWER;
        TpAdditionalCallEventCriteria additionalCriteria2 = new TpAdditionalCallEventCriteria();
        additionalCriteria2.Dummy((short) 0);
        TpCallMonitorMode mode2 = TpCallMonitorMode.P_CALL_MONITOR_MODE_NOTIFY;
        TpCallEventRequest request2 = new TpCallEventRequest(eventType2,
            additionalCriteria2, mode2);

        TpCallEventRequest[] requests = {request1, request2};

        int assignmentId = itsService.createNotification(this,
            new TpCallNotificationRequest(scope, requests));

        theNotifications.put(aUser, new Integer(assignmentId));
    }

    public void stopNotifications(String aUser)
    {
        Integer assignmentId = (Integer) theNotifications.get(aUser);
        if (assignmentId != null) 
        {
            itsService.destroyNotification(assignmentId.intValue());
            theNotifications.remove(aUser);
        }
    }

    public void stopAllNotifications()
    {
        Set assignmentsIds = new HashSet(theNotifications.keySet());
        for (Iterator i = assignmentsIds.iterator(); i.hasNext();)
        {
            stopNotifications((String) i.next());
        }
    }

    public TpAppMultiPartyCallBack reportNotification(
        TpMultiPartyCallIdentifier call, 
        TpCallLegIdentifier[] legs, 
        TpCallNotificationInfo anInfo, 
        int assignmentID)
    {
        Integer callId = new Integer(call.CallSessionID);
        TpCallEventType type = anInfo.CallEventInfo.CallEventType;
        final TpCallNotificationReportScope scope = anInfo.CallNotificationReportScope;

        if (type == TpCallEventType.P_CALL_EVENT_ADDRESS_COLLECTED)
        {
            TimerTask t = new TimerTask()
            {
                public void run()
                {
                    theMain.missedCall(scope.OriginatingAddress.AddrString,
                        scope.DestinationAddress.AddrString);
                }
            };
            System.out.println("If the call is not answered in "
                + getWaitPeriodInSeconds()
                + " seconds, an SMS will be send to "
                + scope.DestinationAddress.AddrString);
            theTimer.schedule(t, getWaitPeriodInSeconds() * 1000);
            theTimerTasks.put(callId, t);
        }
        else if (type == TpCallEventType.P_CALL_EVENT_ANSWER)
        {
            TimerTask t = (TimerTask) theTimerTasks.remove(callId);
            if (t != null)
            {
                t.cancel();
            }
        }
        return theCallback;
    }

    public void notImplemented()
    {
        new UnsupportedOperationException("An unexpected callback method was invoked").printStackTrace();
    }

    /**
     * Return a default TpAddress, based on an address string.
     **/

    public static TpAddress createE164Address(String aNumber)
    {
        return new TpAddress(TpAddressPlan.P_ADDRESS_PLAN_E164,
            aNumber, "",
            TpAddressPresentation.P_ADDRESS_PRESENTATION_UNDEFINED,
            TpAddressScreening.P_ADDRESS_SCREENING_UNDEFINED, "");
    }

    /**
     * Return a default TpAddressRange, based on an address string range.
     **/

    public static TpAddressRange createE164Range(String aNumberRange)
    {
        return new TpAddressRange(TpAddressPlan.P_ADDRESS_PLAN_E164,
            aNumberRange, // address
            "",  // name
            ""); // subaddress
    }
}
