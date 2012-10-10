/*
Copyright (C) 2009 Rachel Engel

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package com.isecpartners.gizmo;

import cybervillains.ca.KeyStoreManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ProxySelector;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JDialog;
import javax.swing.JFrame;

import javax.swing.UIManager;
import org.jdesktop.application.Action;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.SingleFrameApplication;

/**
 * The application's main frame.
 */
public class GizmoView extends FrameView {

    public static String HOME_DIR = "";
    public static String CONV = "";
    private static GizmoView view;
    private static Logger log;
    private String regex = "";
    private static UIEventHandling keys = null;
    static String instanceCreateTimeString;
    private int port = 8080;
    private Configuration config = new Configuration();
    private java.net.InetAddress ip = null;

    public int getPort() {
        return port;
    }

    public Configuration config() {
        return config;
    }

    public boolean isFocusCycleRoot() {
        return true;
    }

    public static UIEventHandling getUIEventHandler() {
        synchronized (GizmoView.class) {
            if (keys == null) {
                keys = UIEventHandling.getHandler();
            }
        }
        return keys;
    }

    public static void log(String str) {
        log.log(Level.INFO, str);
    }

    public static void log(Level lvl, String str) {
        log.log(lvl, str);
    }

    public static void setupLogging() {
        try {
            log = Logger.getLogger("log");
            Handler ch = new ConsoleHandler();
            Handler fh = new FileHandler("log");
            ch.setFormatter(new MySimplerFormatter());
            fh.setFormatter(new MySimplerFormatter());
            log.addHandler(ch);
            log.addHandler(fh);
        } catch (IOException ex) {
            Logger.getLogger(GizmoView.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(GizmoView.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static GizmoView create(Gizmo aThis, int port) {
        if (view == null) {
            view = new GizmoView(aThis, port);
        }
        return view;
    }

    public static GizmoView create(Gizmo aThis) {
        if (view == null) {
            view = new GizmoView(aThis);
        }

        return view;
    }

    public static GizmoView getView() {
        return view;
    }

    static BlobScroller getSummaryScrollers() {
        return summaryScroller;
    }

    static BlobScroller getInterceptScroller() {
        return interceptScroller;
    }
    Thread fetcherThread;
    private static BlobScroller summaryScroller;
    private static BlobScroller interceptScroller;
    private ProtocolEntryPoint httpFetcher;

    public TextBlob getCurrent() {
        return summaryScroller.getCurrent();
    }

    void append(HTTPMessage next, String str, String prefix) {
//        summaryScroller.print(prefix);
        summaryScroller.addMessage(prefix, next, str);
    }

    void append(HTTPMessage next, String str) {
        summaryScroller.addMessage(next, str);
    }

    void appendAfter(HTTPMessage next, String str, String prefix, TextBlob blob) {
        summaryScroller.addMessageAfter(next, str, blob);
    }

    public boolean matchRequest(String contents) {
        boolean ret = false;

        if (regex == null || regex.trim().equals("")) {
            return true;
        }

        ret = contents.matches(regex);

        return ret;
    }

    void setPort(int i) {
        this.port = i;

        fetcherThread.interrupt();
        httpFetcher = new ProtocolEntryPoint(this);
        fetcherThread = new Thread(httpFetcher);
        fetcherThread.setName("fetcher");
        fetcherThread.start();
        setTitle();

    }

    public void grabRoot() {
        jTabbedPane1.setSelectedIndex(0);
        jTextPane1.grabFocus();
    }

    void toggleInterceptRadioButton() {
        boolean new_position = this.jRadioButton1.isSelected() ? false : true;
        this.jRadioButton1.setSelected(new_position);
    }

    private void getHomeDir() {
        try {
            String BaseDir = System.getProperty("user.home") + File.separator + "gizmo";
            System.out.println(BaseDir);
            if (!new File(BaseDir).exists()) {
                (new File(BaseDir)).mkdir();
            }

            HOME_DIR = BaseDir + File.separator + instanceCreateTimeString;
            System.out.println(HOME_DIR);
            if (!new File(HOME_DIR).exists()) {
                (new File(HOME_DIR)).mkdir();
            }
            int num = 0;
            if (!new File(HOME_DIR + File.separator + "index").exists()) {
                PrintWriter fw = new PrintWriter(new FileOutputStream(HOME_DIR + File.separator + "index"));
                fw.println("" + num);
                fw.close();
            } else {
                num = Integer.parseInt(new BufferedReader(new FileReader(new File(HOME_DIR + File.separator + "index"))).readLine());
                num++;
                PrintWriter fw = new PrintWriter(new FileOutputStream(HOME_DIR + File.separator + "index"));
            }

            CONV = HOME_DIR + File.separator + num;
            System.out.println(CONV);
            File f = new File(CONV);
            boolean success = f.mkdir();
            System.out.println(success);


        } catch (Exception e) {
            System.out.println(e);
        }

    }

    private GizmoView(SingleFrameApplication app) {
        super(app);

        local_init(config.getPort(), config.getLocallyBoundAddress());
    }

    private GizmoView(SingleFrameApplication app, int port) {
        super(app);
        local_init(port, config.getLocallyBoundAddress());
    }

    private void local_init(int port, java.net.InetAddress ip) {
        this.port = port;
        this.ip = ip;
        instanceCreateTimeString = new SimpleDateFormat("yyyy_MM_dd_kk_mm_ss").format(new Date());
        getHomeDir();
        initComponents();
        setupLogging();
        ProxySelector.setDefault(new MySelector());
        summaryScroller = new BlobScroller(this, jTextPane1);
        interceptScroller = new BlobScroller(this, jTextPane4, true);
        searchBox.addActionListener(new SearchListener());
        httpFetcher = new ProtocolEntryPoint(this);
        fetcherThread = new Thread(httpFetcher);
        fetcherThread.setName("fetcher");
        Thread.currentThread().setName("gui thread");
        KeyStoreManager.initKeystore();
        this.setTitle();
        //DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keys);
        jPanel2.requestFocus();
        getUIEventHandler().setupMainInput(this.getComponent());
        getUIEventHandler().setSummaryInput(jTextPane1);
        getUIEventHandler().setInterceptInput(jTextPane4);
        GizmoView.log(UIManager.getLookAndFeel().getName());
        fetcherThread.start();


    }

    public void setStatus(String text) {
        this.statusBar.setText(text);
    }

    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = Gizmo.getApplication().getMainFrame();
            aboutBox = new GizmoAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        Gizmo.getApplication().show(aboutBox);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.*/
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTextPane1 = new javax.swing.JTextPane();
        searchBox = new javax.swing.JTextField();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        jTextPane4 = new javax.swing.JTextPane();
        intercept_match_input = new javax.swing.JTextField();
        statusBar = new javax.swing.JLabel();
        jRadioButton1 = new javax.swing.JRadioButton();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        jMenuItem2 = new javax.swing.JMenuItem();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextPane2 = new javax.swing.JTextPane();

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(com.isecpartners.gizmo.Gizmo.class).getContext().getResourceMap(GizmoView.class);
        mainPanel.setBackground(resourceMap.getColor("mainPanel.background")); // NOI18N
        mainPanel.setName("mainPanel"); // NOI18N

        jPanel4.setBackground(resourceMap.getColor("jPanel4.background")); // NOI18N
        jPanel4.setName("jPanel4"); // NOI18N

        jTabbedPane1.setBackground(resourceMap.getColor("jTabbedPane1.background")); // NOI18N
        jTabbedPane1.setForeground(resourceMap.getColor("jTabbedPane1.foreground")); // NOI18N
        jTabbedPane1.setName("jTabbedPane1"); // NOI18N

        jPanel1.setBackground(resourceMap.getColor("jPanel1.background")); // NOI18N
        jPanel1.setName("jPanel1"); // NOI18N

        jScrollPane3.setName("jScrollPane3"); // NOI18N

        jTextPane1.setBackground(resourceMap.getColor("jTextPane1.background")); // NOI18N
        jTextPane1.setBorder(null);
        jTextPane1.setName("jTextPane1"); // NOI18N
        jTextPane1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextPane1KeyPressed(evt);
            }
        });
        jScrollPane3.setViewportView(jTextPane1);

        searchBox.setName("searchBox"); // NOI18N

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(searchBox, javax.swing.GroupLayout.DEFAULT_SIZE, 721, Short.MAX_VALUE)
            .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 721, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(searchBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 650, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab(resourceMap.getString("jPanel1.TabConstraints.tabTitle"), jPanel1); // NOI18N

        jPanel2.setBackground(resourceMap.getColor("jPanel2.background")); // NOI18N
        jPanel2.setName("jPanel2"); // NOI18N

        jScrollPane4.setName("jScrollPane4"); // NOI18N

        jTextPane4.setName("jTextPane4"); // NOI18N
        jScrollPane4.setViewportView(jTextPane4);

        intercept_match_input.setBackground(resourceMap.getColor("intercept_match_input.background")); // NOI18N
        intercept_match_input.setText(resourceMap.getString("intercept_match_input.text")); // NOI18N
        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(com.isecpartners.gizmo.Gizmo.class).getContext().getActionMap(GizmoView.class, this);
        intercept_match_input.setAction(actionMap.get("intercept_match")); // NOI18N
        intercept_match_input.setName("intercept_match_input"); // NOI18N

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 721, Short.MAX_VALUE)
            .addComponent(intercept_match_input, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 721, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(intercept_match_input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 650, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab(resourceMap.getString("jPanel2.TabConstraints.tabTitle"), jPanel2); // NOI18N

        statusBar.setText(resourceMap.getString("statusBar.text")); // NOI18N
        statusBar.setName("statusBar"); // NOI18N

        jRadioButton1.setAction(actionMap.get("toggleIntercepting")); // NOI18N
        jRadioButton1.setBackground(resourceMap.getColor("jRadioButton1.background")); // NOI18N
        jRadioButton1.setText(resourceMap.getString("jRadioButton1.text")); // NOI18N
        jRadioButton1.setName("jRadioButton1"); // NOI18N

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jTabbedPane1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 726, Short.MAX_VALUE)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(statusBar, javax.swing.GroupLayout.DEFAULT_SIZE, 641, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jRadioButton1)))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 704, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(statusBar, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jRadioButton1)))
        );

        jTabbedPane1.getAccessibleContext().setAccessibleName(resourceMap.getString("jTabbedPane1.AccessibleContext.accessibleName")); // NOI18N

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        jMenuItem3.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem3.setText(resourceMap.getString("jMenuItem3.text")); // NOI18N
        jMenuItem3.setName("jMenuItem3"); // NOI18N
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem3ActionPerformed(evt);
            }
        });
        fileMenu.add(jMenuItem3);

        menuBar.add(fileMenu);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        jMenuItem2.setAction(actionMap.get("openHelpPane")); // NOI18N
        jMenuItem2.setText(resourceMap.getString("jMenuItem2.text")); // NOI18N
        jMenuItem2.setName("jMenuItem2"); // NOI18N
        helpMenu.add(jMenuItem2);

        menuBar.add(helpMenu);

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        jTextPane2.setBorder(null);
        jTextPane2.setName("jTextPane2"); // NOI18N
        jScrollPane2.setViewportView(jTextPane2);

        setComponent(mainPanel);
        setMenuBar(menuBar);
    }// </editor-fold>//GEN-END:initComponents

    private void jTextPane1KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextPane1KeyPressed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextPane1KeyPressed

    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem3ActionPerformed
        ConfigFrame frame = new ConfigFrame(config);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setVisible(true);
    }//GEN-LAST:event_jMenuItem3ActionPerformed

    public boolean intercepting() {
        return this.httpFetcher.intercepting();
    }

    @Action
    public void intercept() {
        this.httpFetcher.intercept();
        setTitle();
    }

    public void addIntercept(HTTPMessage msg, String prefix) {
        GizmoView.interceptScroller.addMessage(prefix, msg, "\n\n");
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField intercept_match_input;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JRadioButton jRadioButton1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextPane jTextPane1;
    private javax.swing.JTextPane jTextPane2;
    private javax.swing.JTextPane jTextPane4;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JTextField searchBox;
    private javax.swing.JLabel statusBar;
    // End of variables declaration//GEN-END:variables
    private JDialog aboutBox;

    public String getSelectedText() {
        if (jTabbedPane1.getSelectedIndex() == 0) {
            return jTextPane1.getSelectedText();
        } else {
            return jTextPane2.getSelectedText();
        }
    }

    @Action
    public void openHelpPane() {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new Help().setVisible(true);
            }
        });
    }

    public javax.swing.JTextField getSearchBox() {
        return searchBox;
    }

    @Action
    public void intercept_match() {
        this.regex = intercept_match_input.getText();
        jPanel2.requestFocusInWindow();
    }

    private void setTitle() {
        String intercepting = intercepting() ? "intercepting" : "not intercepting";
        this.getFrame().setTitle("Gizmo. Proxying on [" + port + "], " + intercepting);
    }

    @Action
    public void stuff() {
        int ii = 2;
        ii++;
    }

    @Action
    public void toggleIntercepting() {
        this.intercept();
    }

    public void setIP(java.net.InetAddress ip) {
        this.ip = ip;

        fetcherThread.interrupt();
        httpFetcher = new ProtocolEntryPoint(this);
        fetcherThread = new Thread(httpFetcher);
        fetcherThread.setName("fetcher");
        fetcherThread.start();
        setTitle();
    }

    public java.net.InetAddress getIP() {
        return ip;
    }
}
