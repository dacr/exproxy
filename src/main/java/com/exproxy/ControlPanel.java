/*
 * ControlPanel.java
 *
 * Created on 7 juillet 2005, 16:20
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.exproxy;

import com.exproxy.tools.JButtonAction;
import com.exproxy.tools.Logging;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import com.exproxy.tools.ApplicationProperties;
import java.awt.Dimension;
import java.awt.Toolkit;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import com.exproxy.listeners.EXProxyListener;
import com.exproxy.tools.KeystoreManager;
import java.io.File;


interface AliveListener {
    public void alive(boolean status);
}

/**
 * This class provides a simple Swing Graphical User Interface
 * contained inside a JPanel. The main method creates a JFrame
 * to display this panel.
 * @author David Crosson
 */
public class ControlPanel extends JPanel implements EXProxyListener {
    private final Logger log = Logger.getLogger(getClass().getName());
    private final DefaultComboBoxModel networkInterfacesModel;
    private final DefaultComboBoxModel ipAddressesModel;
    private List<AliveListener> aliveListeners = new ArrayList<AliveListener>();

    private JTextField statusText;
    private JTextField activeCountText;

    private int activeCount=0;
    
    private EXProxy exproxy;
    private String networkInterfaceName;
    private String networkAddress;
    private int port;
    private int backlog;
    
    private static ApplicationProperties props;

    private static JFrame frame;

    /**
     *
     */
    static JFrame getFrame() {
        return frame;
    }

    /**
     * Make the EXProxy ControlPanel visible to the display.
     * Default EXProxy parameters will be use, check 
     * {@link com.exproxy.EXProxy#EXProxy()}
     * constructor javadoc.
     * @param args not used
     * @throws java.io.IOException raised if the log system can't be initialized or
     * if a problem occurs during NetworkInterface operations.
     */
    public static void main(String[] args) throws IOException {
        Logging.logSystemInit(); 
        
        KeystoreManager km = new KeystoreManager(
                new File("keystore"),
                "default",
                "CN=EXProxy, OU=EXProxy, O=EXProxy, L=EXProxy, S=EXProxy, C=EXProxy",
                "changeit",
                "changeit",
                3650);
        String niName = "eth0";
	if (args.length > 0) niName = args[0];
        props = ApplicationProperties.getInstance();
        JPanel panel = new ControlPanel(niName);
        String title = String.format("%s %s", 
                props.getProductLongName(),
                props.getProductVersion());
        frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel, BorderLayout.CENTER);
        frame.pack();
        frame.setResizable(false);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int x= 0 +(screen.width-frame.getWidth())/2;
        int y= 0 +(screen.height-frame.getHeight())/2;
        frame.setLocation(x, y);
        
        frame.setVisible(true);
    }
    
    /**
     * Creates a new instance of ControlPanel.
     * @throws java.io.IOException raised if a problem occurs during NetworkInterface operations.
     */
    public ControlPanel(String niName) throws IOException {
        super(new BorderLayout());
        
        Logger log = Logger.getLogger(EXProxy.class.getName());
        
        InetAddress nif=null;
        NetworkInterface ni = null;
        ni = NetworkInterface.getByName(niName);
        if (ni==null) {
            niName = "lo";
            ni = NetworkInterface.getByName(niName);
        }

        Enumeration<InetAddress> addrs = ni.getInetAddresses();
        while(addrs.hasMoreElements()) {
            InetAddress cur = addrs.nextElement();
            if (cur instanceof Inet4Address) {
                nif=cur;
                break;
            }
        }
        setNetworkInterfaceName(niName);
        setNetworkAddress(nif.getHostAddress());
        setPort(props.getDefaultPort());
        setBacklog(props.getDefaultBacklog());


        String nname = getNetworkInterfaceName();
        String ip = getNetworkAddress();

        JPanel configPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        networkInterfacesModel = new NetworkInterfaceComboBoxModel();
        ipAddressesModel = new IpAddressesComboBoxModel();

        JLabel networkInterfacesLabel = new JLabel("Network Interface :");
        JComboBox networkInterfaces = new JComboBox(getNetworkInterfacesModel());
        networkInterfaces.setAction(new NetworkInterfacesAction(this));
        networkInterfaces.setRenderer(new NetworkInterfaceRenderer());
        try {
            networkInterfaces.setSelectedItem(NetworkInterface.getByName(nname));
        } catch(SocketException e) {
            log.log(Level.SEVERE, "Couldn't initialize with setup network interface", e);
        }

        JLabel ipAddressesLabel = new JLabel("IP :");
        JComboBox ipAddresses = new JComboBox(getIpAddressesModel());
        ipAddresses.setAction(new IpAddressesAction(this));
        ipAddresses.setRenderer(new IpAddressesRenderer());
        for(int index=0; index<ipAddressesModel.getSize(); index++) {
            InetAddress addr = (InetAddress)ipAddressesModel.getElementAt(index);
            if (addr.getHostAddress().equals(ip)) {
                ipAddresses.setSelectedItem(addr);
                break;
            }
        }


        JLabel portLabel = new JLabel("Port :");
        JFormattedTextField port = new JFormattedTextField(NumberFormat.getInstance());
        port.setText(Integer.toString(getPort()));

        configPanel.add(networkInterfacesLabel);
        configPanel.add(networkInterfaces);
        configPanel.add(ipAddressesLabel);
        configPanel.add(ipAddresses);
        configPanel.add(portLabel);
        configPanel.add(port);

        GridLayout gridLayout = new GridLayout(2,1);
        gridLayout.setVgap(6);
        JPanel infoPanel = new JPanel(gridLayout);
        
        JPanel statusPanel = new JPanel(new BorderLayout());
        JLabel statusLabel = new JLabel("Status :");
        statusText = new JTextField();
        statusText.setEditable(false);
        statusPanel.add(statusLabel, BorderLayout.WEST);
        statusPanel.add(statusText, BorderLayout.CENTER);

        JPanel activeCountPanel = new JPanel(new BorderLayout());
        JLabel activeCountLabel = new JLabel("Active handlers count :");
        activeCountText = new JTextField(Integer.toString(activeCount));
        activeCountText.setEditable(false);
        activeCountPanel.add(activeCountLabel, BorderLayout.WEST);
        activeCountPanel.add(activeCountText, BorderLayout.CENTER);


        infoPanel.setBorder(new EmptyBorder(6,6,6,6));
        infoPanel.add(statusPanel);
        infoPanel.add(activeCountPanel);

        JPanel buttons = new JPanel(new FlowLayout());
        buttons.add(new JButton(new ControlPanelStartAction(this)));
        buttons.add(new JButton(new ControlPanelRestartAction(this)));
        buttons.add(new JButton(new ControlPanelStopAction(this)));

        add(buttons, BorderLayout.SOUTH);
        add(infoPanel, BorderLayout.CENTER);
        add(configPanel, BorderLayout.NORTH);
        start();
    }

    /**
     *
     */
    DefaultComboBoxModel getNetworkInterfacesModel() {
        return networkInterfacesModel;
    }

    /**
     *
     */
    DefaultComboBoxModel getIpAddressesModel() {
        return ipAddressesModel;
    }

    /**
     * Get the currently selected network interface name.
     * @return Network interface name such as "eth0" or "lo"
     */
    public String getNetworkInterfaceName() {
        return networkInterfaceName;
    }

    /**
     *
     */
    void setNetworkInterfaceName(String networkInterfaceName) {
        this.networkInterfaceName = networkInterfaceName;
    }

    /**
     * Get the currently selected network address (may differ from
     * the one used by EXProxy server).
     * @return IPV4 or IPV6 address string like "127.0.0.1", ...
     */
    public String getNetworkAddress() {
        return networkAddress;
    }

    /**
     *
     */
    void setNetworkAddress(String networkAddress) {
        this.networkAddress = networkAddress;
    }

    /**
     * Get the currently chosen port (may differ from
     * the one used by EXProxy server)
     * @return port number
     */
    public int getPort() {
        return port;
    }

    /**
     *
     */
    void setPort(int port) {
        this.port = port;
    }
    
    /**
     * get running EXProxy proxy server instance
     * @return exproxy or null if not started
     */
    public EXProxy getExproxy() {
        return exproxy;
    }

    /**
     *
     */
    void setExproxy(EXProxy exproxy) {
        this.exproxy = exproxy;
    }

    /**
     * get backlog configuration for EXProxy
     * @return backlog number
     */
    public int getBacklog() {
        return backlog;
    }

    /**
     *
     */
    void setBacklog(int backlog) {
        this.backlog = backlog;
    }
    
    /**
     *
     */
    void start() {
        if (exproxy != null && exproxy.isAlive()) return;
        
        InetAddress ia=null;
        try {
            NetworkInterface ni = NetworkInterface.getByName(getNetworkInterfaceName());
            Enumeration<InetAddress> addrs = ni.getInetAddresses();
            while(addrs.hasMoreElements()) {
                InetAddress addr = addrs.nextElement();
                if (addr.getHostAddress().equals(getNetworkAddress())) {
                    ia = addr;
                    break;
                }
            }
        } catch(SocketException e) {
            log.log(Level.SEVERE, "Can't process network parameters", e);
        }
        if (ia != null) {
            activeCount=0;
            refreshActiveCount(activeCount);
            exproxy = new EXProxy(ia, getPort(), getBacklog());
            exproxy.addListener(this);
            exproxy.start();
            notifyAliveListener(true);
            statusText.setText(String.format("EXProxy started on %s %s %d", getNetworkInterfaceName(), getNetworkAddress(), getPort()));
        }
    }
    /**
     *
     */
    void restart() {
        stop();
        start();
    }
    /**
     *
     */
    void stop() {
        if (exproxy!=null && exproxy.isAlive()) {
            exproxy.interrupt();
            exproxy.removeListener(this);
            activeCount=0;
            refreshActiveCount(activeCount);
            exproxy=null;
            notifyAliveListener(false);
            statusText.setText(String.format("EXProxy stopped", getNetworkInterfaceName(), getNetworkAddress(), getPort()));
        }
    }
    
    /**
     *
     */
    void addAliveListener(AliveListener listener) {
        aliveListeners.add(listener);
    }
    /**
     *
     */
    void removeAliveListenerr(AliveListener listener) {
        aliveListeners.remove(listener);
    }
    /**
     *
     */
    void notifyAliveListener(boolean isAlive) {
        for(AliveListener listener : aliveListeners) {
            listener.alive(isAlive);
        }
    }

    /**
     *
     */
    private void refreshActiveCount(final int activeCount) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                activeCountText.setText(Integer.toString(activeCount));
            }
        });
    }
    /**
     * Called by EXProxy when a new connection has been
     * requested by a client.
     * @param host Client host address
     * @param port Client port
     */
    public void connectionEnd(String host, int port) {
        activeCount--;
        refreshActiveCount(activeCount);
    }

    /**
     *  Called by EXProxy when a connection has been
     * has been closed.
     * @param host Client host address
     * @param port Client port
     */
    public void connectionStart(String host, int port) {
        activeCount++;
        refreshActiveCount(activeCount);
    }
}


abstract class ControlPanelAction extends JButtonAction implements AliveListener {
    private final ControlPanel controlPanel;
    ControlPanelAction(ControlPanel controlPanel, String text) {
        super(text);
        this.controlPanel = controlPanel;
        controlPanel.addAliveListener(this);
    }
    ControlPanel getControlPanel() {
        return controlPanel;
    }
    
}

class ControlPanelStartAction extends ControlPanelAction {
    ControlPanelStartAction(ControlPanel controlPanel) {
        super(controlPanel, "S_tart");
        putValue(Action.SHORT_DESCRIPTION, "Starts EXProxy");
        //putValue(Action.SMALL_ICON, Resources.createIconRotateRight());
    }
    public void actionPerformed(ActionEvent e) {
        getControlPanel().start();
    }
    public void alive(boolean status) {
        setEnabled(!status);
    }
}



class ControlPanelRestartAction extends ControlPanelAction {
    ControlPanelRestartAction(ControlPanel controlPanel) {
        super(controlPanel, "R_estart");
        putValue(Action.SHORT_DESCRIPTION, "Restarts EXProxy");
        //putValue(Action.SMALL_ICON, Resources.createIconRotateRight());
    }
    public void actionPerformed(ActionEvent e) {
        getControlPanel().restart();
    }
    public void alive(boolean status) {
        setEnabled(status);
    }
}


class ControlPanelStopAction extends ControlPanelAction {
    ControlPanelStopAction(ControlPanel controlPanel) {
        super(controlPanel, "St_op");
        putValue(Action.SHORT_DESCRIPTION, "Stops EXProxy");
        //putValue(Action.SMALL_ICON, Resources.createIconRotateRight());
    }
    public void actionPerformed(ActionEvent e) {
        getControlPanel().stop();
    }
    public void alive(boolean status) {
        setEnabled(status);
    }
}
















class NetworkInterfaceComboBoxModel extends DefaultComboBoxModel {
    NetworkInterfaceComboBoxModel() {
        super();
        try {
            Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
            while(e.hasMoreElements()) {
                NetworkInterface ni = e.nextElement();
                addElement(ni);
            }
        } catch(SocketException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Can't get network interfaces", e);
        }
    }
}


 class NetworkInterfaceRenderer extends JLabel implements ListCellRenderer {
     NetworkInterfaceRenderer() {
         setOpaque(true);
     }
     public Component getListCellRendererComponent(
         JList list,
         Object value,
         int index,
         boolean isSelected,
         boolean cellHasFocus) {
         NetworkInterface ni = (NetworkInterface) value;
         setText(ni.getName());
         setBackground(isSelected ? Color.red : Color.white);
         setForeground(isSelected ? Color.white : Color.black);
         return this;
     }
 }
 


class IpAddressesComboBoxModel extends DefaultComboBoxModel {
    IpAddressesComboBoxModel() {
    }
}

class IpAddressesRenderer extends JLabel implements ListCellRenderer {
     IpAddressesRenderer() {
         setOpaque(true);
     }
     public Component getListCellRendererComponent(
         JList list,
         Object value,
         int index,
         boolean isSelected,
         boolean cellHasFocus) {
         InetAddress addr = (InetAddress) value;
         if(value==null) {
             setText(null);
             return this;
         }
         setText(addr.getHostAddress());
         setBackground(isSelected ? Color.red : Color.white);
         setForeground(isSelected ? Color.white : Color.black);
         return this;
     }
 }


class NetworkInterfacesAction extends AbstractAction {
    private ControlPanel controlPanel;
    NetworkInterfacesAction(ControlPanel controlPanel) {
        this.controlPanel = controlPanel;
    }
    public void actionPerformed(ActionEvent actionEvent) {
        JComboBox combo = (JComboBox)actionEvent.getSource();
        NetworkInterface ni = (NetworkInterface)combo.getModel().getSelectedItem();
        controlPanel.setNetworkInterfaceName(ni.getName());

        controlPanel.getIpAddressesModel().removeAllElements();
        Enumeration<InetAddress> addrs = ni.getInetAddresses();
        InetAddress toSelect=null;
        while(addrs.hasMoreElements()) {
            InetAddress addr = addrs.nextElement();
            if (toSelect == null && addr instanceof Inet4Address) toSelect = addr;
            controlPanel.getIpAddressesModel().addElement(addr);
        }
        if (toSelect != null) {
            controlPanel.getIpAddressesModel().setSelectedItem(toSelect);
            controlPanel.setNetworkAddress(toSelect.getHostAddress());
        }
        if (controlPanel.getFrame() != null) controlPanel.getFrame().pack();
    }
}


class IpAddressesAction extends AbstractAction {
    private ControlPanel controlPanel;
    IpAddressesAction(ControlPanel controlPanel) {
        this.controlPanel = controlPanel;
    }
    public void actionPerformed(ActionEvent actionEvent) {
        JComboBox combo = (JComboBox)actionEvent.getSource();
        InetAddress addr = (InetAddress)combo.getModel().getSelectedItem();
        if (addr != null) {
            controlPanel.setNetworkAddress(addr.getHostAddress());
        }
    }
}
