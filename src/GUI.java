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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.util.*;

public class GUI extends JPanel
{
    private JButton theStartButton, theStopButton;
    private Main theMain;
    private JLabel theText;

    public GUI(Main aMain, String aDescription)
    {
        theMain = aMain;
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        theText = new JLabel(aDescription);
        textPanel.add(theText);
        textPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(theStartButton = new JButton(new AbstractAction("Start")
        {
            public void actionPerformed(ActionEvent e)
            {
                theMain.start();
            }
        }));
        buttonPanel.add(theStopButton = new JButton(new AbstractAction("Stop")
        {
            public void actionPerformed(ActionEvent e)
            {
                theMain.stop();
            }
        }));
        buttonPanel.setBorder(new EmptyBorder(0, 8, 8, 8));

        setLayout(new BorderLayout());
        JComponent l;
        add(l = new JLabel(new ImageIcon("images/banner.png")),
            BorderLayout.NORTH);
        l.setBorder(new EmptyBorder(8, 8, 0, 8));
        add(textPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        updateState();
    }

    void updateState()
    {
        theStartButton.setEnabled(theMain.isStopped());
        theStopButton.setEnabled(theMain.isStarted());
    }
}

