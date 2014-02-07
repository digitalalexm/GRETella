/*
 * GretellaView.java
 */
package gretella;

import gretella.util.ByteUtil;
import gretella.util.CashedMesage;
import gretella.HTTPDownload;
import gretella.util.FileDB;
import gretella.util.FileIndex;
import gretella.util.GUIDGenerator;
import gretella.util.GnutellaMessage;
import gretella.util.GretellaPreferences;
import gretella.util.PongMessage;
import gretella.util.SearchItem;
import gretella.util.ServentList;
import gretella.util.GnutellaServent;
import gretella.util.QueryMessage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.Queue;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.swing.DefaultListModel;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * The application's main frame.
 */
public class GretellaView extends FrameView implements Runnable {

    public final static String DOWNLOAD_FOLDER = "downloads";
    private static final String INDEX_FILE = "index.db";
    private boolean connected;
    private boolean disconnect;
    private ServentList hostList;
    private LinkedList<GnutellaClient> clients;
    private DefaultListModel downloadsModel = new DefaultListModel();
    private DefaultListModel networkModel = new DefaultListModel();
    private GretellaPreferences prefs;
    private Vector<GnutellaServent> network;
    private FileDB index;
    private int downSpeed;
    private int upSpeed;
    private int connections;
    private String path;
    private byte[] id;
    private GnutellaServer server;
    private byte[] ip;
    private int port;
    private Searcher searcher;
    private Queue<CashedMesage> cashe;
    private LinkedList<SearchItem> searchItems;
    private byte[] activeSearchGUID;
    private LinkedList<HTTPDownload> downloads;
    private int downloadsId = 0;
    
    public GretellaView( SingleFrameApplication app ) {
        super( app );
        searchItems = new LinkedList();
        downloads = new LinkedList();
        activeSearchGUID = null;
        InetAddress addr;
        try {
            addr = InetAddress.getLocalHost();
            ip = addr.getAddress();
        } catch ( UnknownHostException ex ) {
            Logger.getLogger( GretellaView.class.getName() ).log( Level.SEVERE, null, ex );
            ip = new byte[ 4 ];
            ip[ 0] = 127;
            ip[ 0] = 0;
            ip[ 0] = 0;
            ip[ 0] = 1;
        }


        initComponents();

        this.cashe = new LinkedList();
        // status bar initialization - message timeout, idle icon and busy animation, etc
        ResourceMap resourceMap = getResourceMap();
        int messageTimeout = resourceMap.getInteger( "StatusBar.messageTimeout" );
        messageTimer = new Timer( messageTimeout, new ActionListener() {

            public void actionPerformed( ActionEvent e ) {
                statusMessageLabel.setText( "" );
            }
        } );
        messageTimer.setRepeats( false );
        int busyAnimationRate = resourceMap.getInteger( "StatusBar.busyAnimationRate" );
        for ( int i = 0; i < busyIcons.length; i++ ) {
            busyIcons[i] = resourceMap.getIcon( "StatusBar.busyIcons[" + i + "]" );
        }
        busyIconTimer = new Timer( busyAnimationRate, new ActionListener() {

            public void actionPerformed( ActionEvent e ) {
                busyIconIndex = ( busyIconIndex + 1 ) % busyIcons.length;
                statusAnimationLabel.setIcon( busyIcons[busyIconIndex] );
            }
        } );
        idleIcon = resourceMap.getIcon( "StatusBar.idleIcon" );
        statusAnimationLabel.setIcon( idleIcon );
        progressBar.setVisible( false );

        // connecting action tasks to status bar via TaskMonitor
        TaskMonitor taskMonitor = new TaskMonitor( getApplication().getContext() );
        taskMonitor.addPropertyChangeListener( new java.beans.PropertyChangeListener() {

            public void propertyChange( java.beans.PropertyChangeEvent evt ) {
                String propertyName = evt.getPropertyName();
                if ( "started".equals( propertyName ) ) {
                    if ( !busyIconTimer.isRunning() ) {
                        statusAnimationLabel.setIcon( busyIcons[0] );
                        busyIconIndex = 0;
                        busyIconTimer.start();
                    }
                    progressBar.setVisible( true );
                    progressBar.setIndeterminate( true );
                } else if ( "done".equals( propertyName ) ) {
                    busyIconTimer.stop();
                    statusAnimationLabel.setIcon( idleIcon );
                    progressBar.setVisible( false );
                    progressBar.setValue( 0 );
                } else if ( "message".equals( propertyName ) ) {
                    String text = (String) ( evt.getNewValue() );
                    statusMessageLabel.setText( ( text == null ) ? "" : text );
                    messageTimer.restart();
                } else if ( "progress".equals( propertyName ) ) {
                    int value = (Integer) ( evt.getNewValue() );
                    progressBar.setVisible( true );
                    progressBar.setIndeterminate( false );
                    progressBar.setValue( value );
                }
            }
        } );
        jtpMain.setSelectedIndex( 0 );
        connected = false;
        clients = new LinkedList();
        this.jlstDoanloads.setModel( this.downloadsModel );
        this.jlstNetwork.setModel( this.networkModel );
        GUIDGenerator.init();
        this.prefs = new GretellaPreferences( "gretella.ini", "Gretella 0.10" );
        network = new Vector();
        loadPreferences();

        index = new FileDB( this );
        index.constract( prefs.getSharingFolder() );

        try {
            hostList = new ServentList( "HostList.txt" );
        } catch ( IOException ex ) {
            System.out.println( "Unable to save the Gnutella host list" );
            Logger.getLogger( GretellaView.class.getName() ).log( Level.SEVERE, null, ex );
        }
        initServer();
        jspDownloads.setResizeWeight( 1.0 );
        searcher = new Searcher( this );
        this.jmiDisconnect.setEnabled( false );

        checkFolders();
    }

    void addSearchItem( SearchItem searchItem ) {
        this.searchItems.add( searchItem );
        lstSearchItems.add( this.searchItems.size() - 1 + " " + searchItem.toString() );
    }

    File getActualPath( int indexId, String fileName ) {
        return this.index.getActualPath( indexId, fileName );
    }

    LinkedList<FileIndex> getIndeces( LinkedList<String> tokens ) {
        return this.index.search( tokens );
    }

    public long getMaxSpeed() {
        return this.upSpeed;
    }

    public long getmaxUpload() {
        return this.upSpeed;
    }

    void newPong( PongMessage msg ) {
        if ( this.clients.size() < this.connections ) {
            try {
                System.out.println( " newPOng " + msg.getHost() );
                this.directConnect( new GnutellaServent( msg.getHost(), msg.getPort() ) );
            } catch ( IOException ex ) {
                Logger.getLogger( GretellaView.class.getName() ).log( Level.SEVERE, "Pong responce", ex );
            }
        }
    }

    void remove( HTTPDownload aThis ) {
        this.downloads.remove( aThis );
        
        for ( int i = 0; i < this.downloadsModel.size(); i++ ) {
            String temp = (String) downloadsModel.get( i );
            if ( temp.startsWith( aThis.getId() + " " ) ) {
                this.downloadsModel.remove( i );
                break;
            }            
        }
    }

    void setIp( byte[] data ) {
        this.ip = data;
    }

    public void addClient( GnutellaClient client ) {
        clients.add( client );
        networkModel.addElement( client.getHost().getHostName() + ":" + client.getHost().getPort() + " agent: " + client.getHost().getAgentName() );
    }

    private void checkFolders() {
        File downloadFolder = new File( DOWNLOAD_FOLDER );
        if ( downloadFolder.isDirectory() == false ) {
            if ( downloadFolder.mkdirs() == false ) {
                JOptionPane.showMessageDialog( null, "It is impossible to create the download folder and the application will be terminated" );
                System.exit( -1 );
            }
        }
    }

    private void clearSearch() {
        this.activeSearchGUID = null;
        this.searchItems.clear();
        this.lstSearchItems.removeAll();
    }

    private void closeServerSockets() {
        this.server.setConnect( false );
    }

    private void initDirectoConnect() {
        try {
            String tmp = this.jtxtConnectIp.getText();
            StringTokenizer tok = new StringTokenizer( tmp, ":" );
            if ( tok.countTokens() != 2 ) {
                return;
            }
            GnutellaServent host = new GnutellaServent( tok.nextToken(), Integer.parseInt( tok.nextToken() ) );
            this.directConnect( host );
        } catch ( IOException ex ) {
            Logger.getLogger( GretellaView.class.getName() ).log( Level.SEVERE, null, ex );
        }
    }

    private void initServer() {
        for ( int i = 0; i < 100; i++ ) {
            try {
                this.server = new GnutellaServer( this.getPort() + i, this );
                if ( i > 0 ) {
                    JOptionPane.showMessageDialog( null, "Port " + getPort() + " was unavailiable. Now  the port " + ( getPort() + i ) + " is used" );
                    this.port = getPort() + i;
                    this.jtxtPort.setText( getPort() + "" );
                    prefs.setPort( getPort() );
                    prefs.store();
                }
                break;
            } catch ( IOException ex ) {
            }
        }
    }

    public void addFile( String string ) {
        //this.sharingModel. string );
        this.lstSharing.add( string );
    }

    public void clearSharing() {
        this.lstSharing.removeAll();
    }

    public void initView() {
        jtpMain.setSelectedIndex( 0 );
    }

    @Action
    public void showAboutBox() {
        if ( aboutBox == null ) {
            JFrame mainFrame = GretellaApp.getApplication().getMainFrame();
            aboutBox = new GretellaAboutBox( mainFrame );
            aboutBox.setLocationRelativeTo( mainFrame );
        }
        GretellaApp.getApplication().show( aboutBox );
    }

    private void clearLists() {
        this.networkModel.clear();
        this.jlstNetwork.removeAll();
    }

    private void connect() {
        //try {            
        //if ( this.connected == true ) {
        //return;
        //}
        disconnect = false;
        connected = true;
        clients.clear();
        clearLists();
        this.jlblStatus.setText( "<html><b>Status: node searching <b></html>" );
        this.jmiConnect.setEnabled( false );
        new Thread( this ).start();
    }

    public void run() {
        Vector<GnutellaServent> hosts = hostList.getHosts();
        if ( hosts.size() == 0 ) {
            JOptionPane.showMessageDialog( null, "There is no known Gnuttela servent" );
            return;
        }
        for ( int i = 0; i < hosts.size(); i++ ) {
            //for ( INetHost host : hosts ) {
            GnutellaServent host = hosts.get( i );
            if ( disconnect == true ) {
                disconnect = false;
                break;
            }
            try {
                GnutellaClient client = new GnutellaClient( host.getHostName(), host.getPort(), this );
                if ( client.isOk() ) {
                    addClient( client );
                    network.add( client.getHost() );
                    this.connected = true;
                    this.jlblStatus.setText( "<html><b>Status: Connected</b></hml>" );
                    jmiDisconnect.setEnabled( true );
                    //break;
                    if ( clients.size() >= this.connections ) {
                        break;
                    }
                }
                LinkedList<GnutellaServent> tmp = client.getExtraHosts();
                for ( GnutellaServent tmpHost : tmp ) {
                    hosts.add( tmpHost );
                }
            } catch ( IOException ex ) {
                System.out.println( ex );
                //Logger.getLogger( GretellaView.class.getName() ).log( Level.SEVERE, null, ex );
            }
        }
        if ( clients.size() == 0 ) {
            this.jmiConnect.setEnabled( true );
            this.jmiDisconnect.setEnabled( false );
            this.connected = false;
            this.jlblStatus.setText( "<html><b>Status: Disconnected</b></hml>" );
        } else {
            this.jmiConnect.setEnabled( false );
            this.jmiDisconnect.setEnabled( true );
        }

    }

    private boolean directConnect( GnutellaServent host ) throws IOException {
        //System.out.println( "direct connect" + host.getHostName() );
        GnutellaClient client = new GnutellaClient( host.getHostName(), host.getPort(), this );
        if ( client.isOk() ) {
            addClient( client );
            network.add( client.getHost() );
            this.connected = true;
            this.jlblStatus.setText( "<html><b>Status: Connected</b></hml>" );
            jmiDisconnect.setEnabled( true );
            return true;
        } else {
            return false;
        }

    }

    private void disconnect() {
        for ( GnutellaClient client : clients ) {
            try {
                client.closeSocket();
            } catch ( IOException ex ) {
                Logger.getLogger( GretellaView.class.getName() ).log( Level.SEVERE, null, ex );
            }
            clearLists();
            this.disconnect = true;
            jmiConnect.setEnabled( true );
            jmiDisconnect.setEnabled( false );
            this.connected = false;
            this.jlblStatus.setText( "<html><b>Status: Disconnected</b></hml>" );
        }
        this.jmiDisconnect.setEnabled( false );
        this.jmiConnect.setEnabled( true );
        this.connected = false;
        this.jlblStatus.setText( "<html><b>Status: Disconnected<b></html>" );
    }

    public void disconnected( GnutellaServent host ) {
        for ( int i = 0; i < network.size(); i++ ) {
            if ( network.get( i ).equals( host ) == true ) {
                //System.out.println( "i have found my host" );
                network.remove( i );
                break;
            }
        }

        for ( int i = 0; i < networkModel.size(); i++ ) {
            if ( ( (String) networkModel.get( i ) ).contains( host.getHostName() ) && ( (String) networkModel.get( i ) ).contains( host.getPort() + "" ) ) {
                //System.out.println( "i have found my host in list" );
                networkModel.remove( i );
                break;
            }
        }

        for ( int i = 0; i < this.clients.size(); i++ ) {
            if ( clients.get( i ).isHost( host ) == true ) {
                //System.out.println( "i have found my host in clients list" );
                clients.remove( i );
                break;
            }
        }
        
        if ( this.clients.size() == 0 )
            disconnect();
    }

    private void loadPreferences() {
        this.jtxtOptionsConnectionsNumber.setText( "" + prefs.getMaximumConnections() );
        this.connections = prefs.getMaximumConnections();
        this.jtxtPort.setText( "" + prefs.getPort() );
        this.port = prefs.getPort();
        this.jtxtUpSpeed.setText( "" + prefs.getUpstream() );
        this.upSpeed = prefs.getUpstream();
        this.jtxtDownSpeed.setText( "" + prefs.getDownstream() );
        this.downSpeed = prefs.getDownstream();
        this.jtxtFilePath.setText( prefs.getSharingFolder() );
        this.path = prefs.getSharingFolder();
        this.id = prefs.getId();
    }

    private void openServerSocket() {
        //throw new UnsupportedOperationException( "Not yet implemented" );
    }

    private void saveOptions() {
        int tmpPort = Integer.parseInt( this.jtxtPort.getText() );
        downSpeed = Integer.parseInt( this.jtxtDownSpeed.getText() );
        upSpeed = Integer.parseInt( this.jtxtUpSpeed.getText() );
        connections = Integer.parseInt( this.jtxtOptionsConnectionsNumber.getText() );
        String tmpPath = this.jtxtFilePath.getText();

        if ( tmpPort != getPort() ) {
            closeServerSockets();
            port = tmpPort;
            openServerSocket();
        }

        if ( tmpPath.equals( this.path ) == false ) {
            this.path = tmpPath;
            reloadIdex();
        }

        prefs.setPort( tmpPort );
        prefs.setDownstream( downSpeed );
        prefs.setUpstream( upSpeed );
        prefs.setMaximumConnections( connections );
        prefs.setSharingFolder( tmpPath );
        prefs.store();
        JOptionPane.showMessageDialog( null, "Changes saved" );
    }

    private void reloadIdex() {
        this.lstSharing.removeAll();
        //System.out.println( index );
        this.index.constract( this.path );
        try {
            this.index.store( GretellaView.INDEX_FILE );
            System.out.println( "index savad" );
        } catch ( IOException ex ) {
            System.out.println( "It was impossible to store the index file" );
            Logger.getLogger( GretellaView.class.getName() ).log( Level.SEVERE, null, ex );
        }
    }

    public byte[] getId() {
        return this.id;
    }

    public void setIndexStatus( String str ) {
        this.jlblIndexStatus.setText( str );
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings ( "unchecked" )
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        jtpMain = new javax.swing.JTabbedPane();
        jpSearch = new javax.swing.JPanel();
        jSplitPane1 = new javax.swing.JSplitPane();
        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jtxtSearchContent = new javax.swing.JTextField();
        jbtnSearch = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        lstSearchItems = new java.awt.List();
        jbntDownload = new javax.swing.JButton();
        jpDownloads = new javax.swing.JPanel();
        jspDownloads = new javax.swing.JSplitPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        jlstDoanloads = new javax.swing.JList();
        jPanel3 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jtxtDFileSDownloaded = new javax.swing.JTextField();
        jtxtDFileSize = new javax.swing.JTextField();
        jtxtDCurrentSpeed = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        jtxtDAverageSPeed = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jbtnRemoveDownload = new javax.swing.JButton();
        jpSharing = new javax.swing.JPanel();
        jLabel13 = new javax.swing.JLabel();
        jbtnRefresh = new javax.swing.JButton();
        jlblIndexStatus = new javax.swing.JLabel();
        lstSharing = new java.awt.List();
        jpOptions = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jbtnOptionsCancel = new javax.swing.JButton();
        jbtnOptionsOk = new javax.swing.JButton();
        jtxtOptionsConnectionsNumber = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        jtxtFilePath = new javax.swing.JTextField();
        jbtnFileFolderBrowze = new javax.swing.JButton();
        jLabel22 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        jtxtDownSpeed = new javax.swing.JTextField();
        jtxtUpSpeed = new javax.swing.JTextField();
        jLabel24 = new javax.swing.JLabel();
        jLabel25 = new javax.swing.JLabel();
        jLabel26 = new javax.swing.JLabel();
        jtxtPort = new javax.swing.JTextField();
        jpNetwork = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane5 = new javax.swing.JScrollPane();
        jlstNetwork = new javax.swing.JList();
        jLabel14 = new javax.swing.JLabel();
        jtxtConnectIp = new javax.swing.JTextField();
        jbtnDirectConnect = new javax.swing.JButton();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        jmiConnect = new javax.swing.JMenuItem();
        jmiDisconnect = new javax.swing.JMenuItem();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        jlblStatus = new javax.swing.JLabel();
        jlblSpeed = new javax.swing.JLabel();

        mainPanel.setName("mainPanel"); // NOI18N

        jtpMain.setName("jtpMain"); // NOI18N

        jpSearch.setName("jpSearch"); // NOI18N

        jSplitPane1.setName("jSplitPane1"); // NOI18N

        jPanel1.setName("jPanel1"); // NOI18N

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(gretella.GretellaApp.class).getContext().getResourceMap(GretellaView.class);
        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N

        jtxtSearchContent.setText(resourceMap.getString("jtxtSearchContent.text")); // NOI18N
        jtxtSearchContent.setName("jtxtSearchContent"); // NOI18N
        jtxtSearchContent.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jtxtSearchContentActionPerformed(evt);
            }
        });

        jbtnSearch.setText(resourceMap.getString("jbtnSearch.text")); // NOI18N
        jbtnSearch.setName("jbtnSearch"); // NOI18N
        jbtnSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbtnSearchActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jtxtSearchContent, javax.swing.GroupLayout.DEFAULT_SIZE, 65, Short.MAX_VALUE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, 65, Short.MAX_VALUE)
                    .addComponent(jbtnSearch, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jtxtSearchContent, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jbtnSearch)
                .addContainerGap(457, Short.MAX_VALUE))
        );

        jSplitPane1.setLeftComponent(jPanel1);

        jPanel2.setName("jPanel2"); // NOI18N

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        lstSearchItems.setName("lstSearchItems"); // NOI18N
        jScrollPane1.setViewportView(lstSearchItems);

        jbntDownload.setText(resourceMap.getString("jbntDownload.text")); // NOI18N
        jbntDownload.setName("jbntDownload"); // NOI18N
        jbntDownload.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbntDownloadActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jbntDownload, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 1038, Short.MAX_VALUE)))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 508, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jbntDownload))
        );

        jSplitPane1.setRightComponent(jPanel2);

        javax.swing.GroupLayout jpSearchLayout = new javax.swing.GroupLayout(jpSearch);
        jpSearch.setLayout(jpSearchLayout);
        jpSearchLayout.setHorizontalGroup(
            jpSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpSearchLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 1140, Short.MAX_VALUE)
                .addContainerGap())
        );
        jpSearchLayout.setVerticalGroup(
            jpSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpSearchLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 539, Short.MAX_VALUE)
                .addContainerGap())
        );

        jtpMain.addTab(resourceMap.getString("jpSearch.TabConstraints.tabTitle"), jpSearch); // NOI18N

        jpDownloads.setName("jpDownloads"); // NOI18N

        jspDownloads.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jspDownloads.setName("jspDownloads"); // NOI18N

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        jlstDoanloads.setName("jlstDoanloads"); // NOI18N
        jlstDoanloads.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jlstDoanloadsMouseClicked(evt);
            }
        });
        jScrollPane2.setViewportView(jlstDoanloads);

        jspDownloads.setTopComponent(jScrollPane2);

        jPanel3.setName("jPanel3"); // NOI18N

        jLabel3.setText(resourceMap.getString("jLabel3.text")); // NOI18N
        jLabel3.setName("jLabel3"); // NOI18N

        jLabel4.setText(resourceMap.getString("jLabel4.text")); // NOI18N
        jLabel4.setName("jLabel4"); // NOI18N

        jLabel5.setText(resourceMap.getString("jLabel5.text")); // NOI18N
        jLabel5.setName("jLabel5"); // NOI18N

        jtxtDFileSDownloaded.setEditable(false);
        jtxtDFileSDownloaded.setText(resourceMap.getString("jtxtDFileSDownloaded.text")); // NOI18N
        jtxtDFileSDownloaded.setName("jtxtDFileSDownloaded"); // NOI18N

        jtxtDFileSize.setEditable(false);
        jtxtDFileSize.setText(resourceMap.getString("jtxtDFileSize.text")); // NOI18N
        jtxtDFileSize.setName("jtxtDFileSize"); // NOI18N

        jtxtDCurrentSpeed.setEditable(false);
        jtxtDCurrentSpeed.setText(resourceMap.getString("jtxtDCurrentSpeed.text")); // NOI18N
        jtxtDCurrentSpeed.setName("jtxtDCurrentSpeed"); // NOI18N

        jLabel8.setText(resourceMap.getString("jLabel8.text")); // NOI18N
        jLabel8.setName("jLabel8"); // NOI18N

        jtxtDAverageSPeed.setEditable(false);
        jtxtDAverageSPeed.setText(resourceMap.getString("jtxtDAverageSPeed.text")); // NOI18N
        jtxtDAverageSPeed.setName("jtxtDAverageSPeed"); // NOI18N

        jLabel9.setText(resourceMap.getString("jLabel9.text")); // NOI18N
        jLabel9.setName("jLabel9"); // NOI18N

        jLabel10.setText(resourceMap.getString("jLabel10.text")); // NOI18N
        jLabel10.setName("jLabel10"); // NOI18N

        jLabel11.setText(resourceMap.getString("jLabel11.text")); // NOI18N
        jLabel11.setName("jLabel11"); // NOI18N

        jLabel12.setText(resourceMap.getString("jLabel12.text")); // NOI18N
        jLabel12.setName("jLabel12"); // NOI18N

        jbtnRemoveDownload.setText(resourceMap.getString("jbtnRemoveDownload.text")); // NOI18N
        jbtnRemoveDownload.setName("jbtnRemoveDownload"); // NOI18N
        jbtnRemoveDownload.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbtnRemoveDownloadActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jtxtDFileSDownloaded)
                            .addComponent(jtxtDFileSize, javax.swing.GroupLayout.DEFAULT_SIZE, 61, Short.MAX_VALUE)))
                    .addComponent(jtxtDCurrentSpeed)
                    .addComponent(jtxtDAverageSPeed))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel9)
                    .addComponent(jLabel10)
                    .addComponent(jLabel11)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel12)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 855, Short.MAX_VALUE)
                        .addComponent(jbtnRemoveDownload)))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jtxtDFileSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel12)
                    .addComponent(jLabel3)
                    .addComponent(jbtnRemoveDownload))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jtxtDFileSDownloaded, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel11)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jtxtDCurrentSpeed, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel10)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jtxtDAverageSPeed, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9)
                    .addComponent(jLabel8))
                .addContainerGap(397, Short.MAX_VALUE))
        );

        jspDownloads.setRightComponent(jPanel3);

        javax.swing.GroupLayout jpDownloadsLayout = new javax.swing.GroupLayout(jpDownloads);
        jpDownloads.setLayout(jpDownloadsLayout);
        jpDownloadsLayout.setHorizontalGroup(
            jpDownloadsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpDownloadsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jspDownloads, javax.swing.GroupLayout.DEFAULT_SIZE, 1140, Short.MAX_VALUE)
                .addContainerGap())
        );
        jpDownloadsLayout.setVerticalGroup(
            jpDownloadsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpDownloadsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jspDownloads, javax.swing.GroupLayout.DEFAULT_SIZE, 539, Short.MAX_VALUE)
                .addContainerGap())
        );

        jtpMain.addTab(resourceMap.getString("jpDownloads.TabConstraints.tabTitle"), jpDownloads); // NOI18N

        jpSharing.setName("jpSharing"); // NOI18N

        jLabel13.setText(resourceMap.getString("jLabel13.text")); // NOI18N
        jLabel13.setName("jLabel13"); // NOI18N

        jbtnRefresh.setText(resourceMap.getString("jbtnRefresh.text")); // NOI18N
        jbtnRefresh.setName("jbtnRefresh"); // NOI18N
        jbtnRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbtnRefreshActionPerformed(evt);
            }
        });

        jlblIndexStatus.setText(resourceMap.getString("jlblIndexStatus.text")); // NOI18N
        jlblIndexStatus.setName("jlblIndexStatus"); // NOI18N

        lstSharing.setName("lstSharing"); // NOI18N

        javax.swing.GroupLayout jpSharingLayout = new javax.swing.GroupLayout(jpSharing);
        jpSharing.setLayout(jpSharingLayout);
        jpSharingLayout.setHorizontalGroup(
            jpSharingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpSharingLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpSharingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lstSharing, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 1140, Short.MAX_VALUE)
                    .addComponent(jLabel13)
                    .addGroup(jpSharingLayout.createSequentialGroup()
                        .addComponent(jbtnRefresh)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 844, Short.MAX_VALUE)
                        .addComponent(jlblIndexStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 225, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jpSharingLayout.setVerticalGroup(
            jpSharingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpSharingLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel13)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lstSharing, javax.swing.GroupLayout.DEFAULT_SIZE, 480, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpSharingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jbtnRefresh)
                    .addComponent(jlblIndexStatus, javax.swing.GroupLayout.DEFAULT_SIZE, 25, Short.MAX_VALUE))
                .addContainerGap())
        );

        jtpMain.addTab(resourceMap.getString("jpSharing.TabConstraints.tabTitle"), jpSharing); // NOI18N

        jpOptions.setName("jpOptions"); // NOI18N

        jLabel6.setText(resourceMap.getString("jLabel6.text")); // NOI18N
        jLabel6.setName("jLabel6"); // NOI18N

        jbtnOptionsCancel.setText(resourceMap.getString("jbtnOptionsCancel.text")); // NOI18N
        jbtnOptionsCancel.setName("jbtnOptionsCancel"); // NOI18N

        jbtnOptionsOk.setText(resourceMap.getString("jbtnOptionsOk.text")); // NOI18N
        jbtnOptionsOk.setName("jbtnOptionsOk"); // NOI18N
        jbtnOptionsOk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbtnOptionsOkActionPerformed(evt);
            }
        });

        jtxtOptionsConnectionsNumber.setText(resourceMap.getString("jtxtOptionsConnectionsNumber.text")); // NOI18N
        jtxtOptionsConnectionsNumber.setName("jtxtOptionsConnectionsNumber"); // NOI18N

        jLabel7.setText(resourceMap.getString("jLabel7.text")); // NOI18N
        jLabel7.setName("jLabel7"); // NOI18N

        jtxtFilePath.setText(resourceMap.getString("jtxtFilePath.text")); // NOI18N
        jtxtFilePath.setName("jtxtFilePath"); // NOI18N

        jbtnFileFolderBrowze.setText(resourceMap.getString("jbtnFileFolderBrowze.text")); // NOI18N
        jbtnFileFolderBrowze.setName("jbtnFileFolderBrowze"); // NOI18N
        jbtnFileFolderBrowze.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbtnFileFolderBrowzeActionPerformed(evt);
            }
        });

        jLabel22.setText(resourceMap.getString("jLabel22.text")); // NOI18N
        jLabel22.setName("jLabel22"); // NOI18N

        jLabel23.setText(resourceMap.getString("jLabel23.text")); // NOI18N
        jLabel23.setName("jLabel23"); // NOI18N

        jtxtDownSpeed.setText(resourceMap.getString("jtxtDownSpeed.text")); // NOI18N
        jtxtDownSpeed.setName("jtxtDownSpeed"); // NOI18N

        jtxtUpSpeed.setName("jtxtUpSpeed"); // NOI18N

        jLabel24.setText(resourceMap.getString("jLabel24.text")); // NOI18N
        jLabel24.setName("jLabel24"); // NOI18N

        jLabel25.setText(resourceMap.getString("jLabel25.text")); // NOI18N
        jLabel25.setName("jLabel25"); // NOI18N

        jLabel26.setText(resourceMap.getString("jLabel26.text")); // NOI18N
        jLabel26.setName("jLabel26"); // NOI18N

        jtxtPort.setName("jtxtPort"); // NOI18N

        javax.swing.GroupLayout jpOptionsLayout = new javax.swing.GroupLayout(jpOptions);
        jpOptions.setLayout(jpOptionsLayout);
        jpOptionsLayout.setHorizontalGroup(
            jpOptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpOptionsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jtxtFilePath, javax.swing.GroupLayout.PREFERRED_SIZE, 296, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jbtnFileFolderBrowze)
                .addGap(781, 781, 781))
            .addGroup(jpOptionsLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(jbtnOptionsOk, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jbtnOptionsCancel)
                .addContainerGap(1012, Short.MAX_VALUE))
            .addGroup(jpOptionsLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, 734, Short.MAX_VALUE)
                .addContainerGap(414, Short.MAX_VALUE))
            .addGroup(jpOptionsLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(jtxtPort, javax.swing.GroupLayout.PREFERRED_SIZE, 68, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(1080, Short.MAX_VALUE))
            .addGroup(jpOptionsLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(1069, 1069, 1069))
            .addGroup(jpOptionsLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(jtxtOptionsConnectionsNumber, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(1081, Short.MAX_VALUE))
            .addGroup(jpOptionsLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(jLabel22)
                .addGap(998, 998, 998))
            .addGroup(jpOptionsLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(jtxtDownSpeed, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel25, javax.swing.GroupLayout.DEFAULT_SIZE, 644, Short.MAX_VALUE)
                .addContainerGap(425, Short.MAX_VALUE))
            .addGroup(jpOptionsLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(jLabel23, javax.swing.GroupLayout.DEFAULT_SIZE, 727, Short.MAX_VALUE)
                .addContainerGap(421, Short.MAX_VALUE))
            .addGroup(jpOptionsLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(jtxtUpSpeed, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel24, javax.swing.GroupLayout.DEFAULT_SIZE, 647, Short.MAX_VALUE)
                .addContainerGap(425, Short.MAX_VALUE))
            .addGroup(jpOptionsLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(jLabel26, javax.swing.GroupLayout.PREFERRED_SIZE, 179, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(969, Short.MAX_VALUE))
        );
        jpOptionsLayout.setVerticalGroup(
            jpOptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpOptionsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jtxtOptionsConnectionsNumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel22, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpOptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel25)
                    .addComponent(jtxtDownSpeed, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel23, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpOptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel24)
                    .addComponent(jtxtUpSpeed, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel26, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jtxtPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpOptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jtxtFilePath, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jbtnFileFolderBrowze))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpOptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jbtnOptionsOk)
                    .addComponent(jbtnOptionsCancel))
                .addGap(291, 291, 291))
        );

        jtpMain.addTab(resourceMap.getString("jpOptions.TabConstraints.tabTitle"), jpOptions); // NOI18N

        jpNetwork.setName("jpNetwork"); // NOI18N

        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N

        jScrollPane5.setName("jScrollPane5"); // NOI18N

        jlstNetwork.setName("jlstNetwork"); // NOI18N
        jScrollPane5.setViewportView(jlstNetwork);

        jLabel14.setText(resourceMap.getString("jLabel14.text")); // NOI18N
        jLabel14.setName("jLabel14"); // NOI18N

        jtxtConnectIp.setText(resourceMap.getString("jtxtConnectIp.text")); // NOI18N
        jtxtConnectIp.setName("jtxtConnectIp"); // NOI18N

        jbtnDirectConnect.setText(resourceMap.getString("jbtnDirectConnect.text")); // NOI18N
        jbtnDirectConnect.setName("jbtnDirectConnect"); // NOI18N
        jbtnDirectConnect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbtnDirectConnectActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jpNetworkLayout = new javax.swing.GroupLayout(jpNetwork);
        jpNetwork.setLayout(jpNetworkLayout);
        jpNetworkLayout.setHorizontalGroup(
            jpNetworkLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpNetworkLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpNetworkLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 1140, Short.MAX_VALUE)
                    .addComponent(jLabel1)
                    .addGroup(jpNetworkLayout.createSequentialGroup()
                        .addComponent(jLabel14)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jtxtConnectIp, javax.swing.GroupLayout.PREFERRED_SIZE, 166, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jbtnDirectConnect)))
                .addContainerGap())
        );
        jpNetworkLayout.setVerticalGroup(
            jpNetworkLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpNetworkLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 498, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpNetworkLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel14)
                    .addComponent(jtxtConnectIp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jbtnDirectConnect))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jtpMain.addTab(resourceMap.getString("jpNetwork.TabConstraints.tabTitle"), jpNetwork); // NOI18N

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jtpMain, javax.swing.GroupLayout.DEFAULT_SIZE, 1165, Short.MAX_VALUE)
                .addContainerGap())
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addComponent(jtpMain, javax.swing.GroupLayout.DEFAULT_SIZE, 589, Short.MAX_VALUE)
                .addContainerGap())
        );

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        jmiConnect.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_MASK));
        jmiConnect.setText(resourceMap.getString("jmiConnect.text")); // NOI18N
        jmiConnect.setName("jmiConnect"); // NOI18N
        jmiConnect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmiConnectActionPerformed(evt);
            }
        });
        fileMenu.add(jmiConnect);

        jmiDisconnect.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, java.awt.event.InputEvent.CTRL_MASK));
        jmiDisconnect.setText(resourceMap.getString("jmiDisconnect.text")); // NOI18N
        jmiDisconnect.setName("jmiDisconnect"); // NOI18N
        jmiDisconnect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmiDisconnectActionPerformed(evt);
            }
        });
        fileMenu.add(jmiDisconnect);

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(gretella.GretellaApp.class).getContext().getActionMap(GretellaView.class, this);
        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        statusPanel.setName("statusPanel"); // NOI18N

        statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N

        statusMessageLabel.setName("statusMessageLabel"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

        progressBar.setName("progressBar"); // NOI18N

        jlblStatus.setText(resourceMap.getString("jlblStatus.text")); // NOI18N
        jlblStatus.setName("jlblStatus"); // NOI18N

        jlblSpeed.setText(resourceMap.getString("jlblSpeed.text")); // NOI18N
        jlblSpeed.setName("jlblSpeed"); // NOI18N

        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusMessageLabel)
                .addComponent(jlblStatus)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jlblSpeed)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 633, Short.MAX_VALUE)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusAnimationLabel)
                .addContainerGap())
            .addComponent(statusPanelSeparator, javax.swing.GroupLayout.DEFAULT_SIZE, 1185, Short.MAX_VALUE)
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addComponent(statusPanelSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(statusPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 16, Short.MAX_VALUE)
                        .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(statusMessageLabel)
                            .addComponent(statusAnimationLabel)
                            .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(3, 3, 3))
                    .addGroup(statusPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jlblSpeed)
                            .addComponent(jlblStatus))
                        .addContainerGap())))
        );

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
    }// </editor-fold>//GEN-END:initComponents

private void jmiConnectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmiConnectActionPerformed
    connect();
}//GEN-LAST:event_jmiConnectActionPerformed

private void jmiDisconnectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmiDisconnectActionPerformed
    disconnect();
}//GEN-LAST:event_jmiDisconnectActionPerformed

private void jbtnOptionsOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbtnOptionsOkActionPerformed
    saveOptions();
}//GEN-LAST:event_jbtnOptionsOkActionPerformed

private void jbtnRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbtnRefreshActionPerformed
    reloadIdex();
}//GEN-LAST:event_jbtnRefreshActionPerformed

private void jbtnFileFolderBrowzeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbtnFileFolderBrowzeActionPerformed
    javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
    fc.setFileSelectionMode( javax.swing.JFileChooser.DIRECTORIES_ONLY );
    int choice = fc.showDialog( this.jpOptions, "Choose Directory" );
    if ( choice == JFileChooser.APPROVE_OPTION ) {
        this.jtxtFilePath.setText( fc.getSelectedFile().getAbsolutePath() );
    }
}//GEN-LAST:event_jbtnFileFolderBrowzeActionPerformed

private void jbtnSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbtnSearchActionPerformed
    initSearch();
}//GEN-LAST:event_jbtnSearchActionPerformed

private void jtxtSearchContentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jtxtSearchContentActionPerformed
    initSearch();
}//GEN-LAST:event_jtxtSearchContentActionPerformed

private void jbntDownloadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbntDownloadActionPerformed
    startDownload();
}//GEN-LAST:event_jbntDownloadActionPerformed

private void jlstDoanloadsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jlstDoanloadsMouseClicked
    selectDownload();
}//GEN-LAST:event_jlstDoanloadsMouseClicked

private void jbtnDirectConnectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbtnDirectConnectActionPerformed
    initDirectoConnect();
}//GEN-LAST:event_jbtnDirectConnectActionPerformed

private void jbtnRemoveDownloadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbtnRemoveDownloadActionPerformed
    stopDownload();
}//GEN-LAST:event_jbtnRemoveDownloadActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JButton jbntDownload;
    private javax.swing.JButton jbtnDirectConnect;
    private javax.swing.JButton jbtnFileFolderBrowze;
    private javax.swing.JButton jbtnOptionsCancel;
    private javax.swing.JButton jbtnOptionsOk;
    private javax.swing.JButton jbtnRefresh;
    private javax.swing.JButton jbtnRemoveDownload;
    private javax.swing.JButton jbtnSearch;
    private javax.swing.JLabel jlblIndexStatus;
    private javax.swing.JLabel jlblSpeed;
    private javax.swing.JLabel jlblStatus;
    private javax.swing.JList jlstDoanloads;
    private javax.swing.JList jlstNetwork;
    private javax.swing.JMenuItem jmiConnect;
    private javax.swing.JMenuItem jmiDisconnect;
    private javax.swing.JPanel jpDownloads;
    private javax.swing.JPanel jpNetwork;
    private javax.swing.JPanel jpOptions;
    private javax.swing.JPanel jpSearch;
    private javax.swing.JPanel jpSharing;
    private javax.swing.JSplitPane jspDownloads;
    private javax.swing.JTabbedPane jtpMain;
    private javax.swing.JTextField jtxtConnectIp;
    private javax.swing.JTextField jtxtDAverageSPeed;
    private javax.swing.JTextField jtxtDCurrentSpeed;
    private javax.swing.JTextField jtxtDFileSDownloaded;
    private javax.swing.JTextField jtxtDFileSize;
    private javax.swing.JTextField jtxtDownSpeed;
    private javax.swing.JTextField jtxtFilePath;
    private javax.swing.JTextField jtxtOptionsConnectionsNumber;
    private javax.swing.JTextField jtxtPort;
    private javax.swing.JTextField jtxtSearchContent;
    private javax.swing.JTextField jtxtUpSpeed;
    private java.awt.List lstSearchItems;
    private java.awt.List lstSharing;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    // End of variables declaration//GEN-END:variables
    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[ 15 ];
    private int busyIconIndex = 0;
    private JDialog aboutBox;

    public byte[] getIp() {
        return ip;
    }

    public int getFileSize() {
        return index.getTotalSize();
    }

    public int getFileCount() {
        return index.getFileCount();
    }

    public int getPort() {
        return port;
    }

    public void search() {
        String query = jtxtSearchContent.getText();
        QueryMessage msg = new QueryMessage( 1, ByteUtil.getNullTerminatedString( query ) );
        setActiveSearchGUID( msg.getGUID() );
        for ( GnutellaClient client : this.clients ) {
            //System.out.println( "sending search to " + client.getHost().getHostName() );            

            client.search( msg );
        }
    }

    private void initSearch() {
        searcher.search();
        clearSearch();
    }

    public boolean acceptMoreConnections() {
        if ( this.clients.size() < this.connections ) {
            return true;
        } else {
            return false;
        }
    }

    public void casheMessage( CashedMesage msg ) {
        this.cashe.add( msg );
        if ( cashe.size() > 100 ) {
            this.cashe.poll();
        }
    }

    public boolean isCashed( CashedMesage msg ) {
        return this.cashe.contains( msg );
    }

    public void routeMessage( GnutellaMessage msg, String hostName, int port ) {
        msg.updateMessage();
        //System.out.println( "routing msg with ttl " + msg.getTTL()  );
        if ( msg.getTTL() <= 0 ) {
            //System.out.println( "i will not route this" );
            return;
        }
        for ( GnutellaClient client : this.clients ) {
            if ( client.getHost().getHostName().equals( hostName ) && client.getHost().getPort() == port ) {
                //System.out.println( "i will NOT send to " + client.getHost().getHostName() + " : " + client.getHost().getPort() + " becouse msg is from " + hostName + ":" + port );
                continue;
            }
            //System.out.println( "i will send to " + client.getHost().getHostName() + " : " + client.getHost().getPort() + " and  msg is from " + hostName + ":" + port );
            client.send( msg );
        }
    }

    public byte[] getActiveSearchGUID() {
        return activeSearchGUID;
    }

    public void setActiveSearchGUID( byte[] activeSearchGUID ) {
        this.activeSearchGUID = activeSearchGUID;
    }

    private void selectDownload() {        
        this.jtxtDAverageSPeed.setText( "" );
        this.jtxtDCurrentSpeed.setText( "" );
        this.jtxtDFileSDownloaded.setText( "" );
        this.jtxtDFileSize.setText( "" );
        int udx = this.jlstDoanloads.getSelectedIndex();
        if ( udx == -1 ) {
            return;
        }
        for ( HTTPDownload d : downloads ) {
            if ( d.isSelected() == true ) {
                d.setSelected( false );
                break;
            }
        }
        this.downloads.get( udx ).setSelected( true );
    }

    private void startDownload() {
        try {
            int idx = this.lstSearchItems.getSelectedIndex();
            if ( idx == -1 ) {
                return;
            }
            String item = this.lstSearchItems.getSelectedItem();
            StringTokenizer tok = new StringTokenizer( item, " " );
            int pos = Integer.parseInt( tok.nextToken() );

            SearchItem sitem = this.searchItems.get( pos );

            HTTPDownload download = new HTTPDownload( downloadsId, sitem, this.downSpeed, this );
            downloadsId++;
            this.downloads.add( download );
            this.downloadsModel.addElement( download.toString() );
        } catch ( MalformedURLException ex ) {
            Logger.getLogger( GretellaView.class.getName() ).log( Level.SEVERE, null, ex );
        } catch ( IOException ex ) {
            Logger.getLogger( GretellaView.class.getName() ).log( Level.SEVERE, null, ex );
        }
    }

    public void setDownloadStatus( long fileSize, long downloaded, float averageSpeed, float currentSpeed ) {
        this.jtxtDFileSize.setText( fileSize + "" );
        this.jtxtDFileSDownloaded.setText( downloaded + "" );
        this.jtxtDAverageSPeed.setText( averageSpeed + "" );
        this.jtxtDCurrentSpeed.setText( currentSpeed + "" );
    }

    private void stopDownload() {
        int idx = this.jlstDoanloads.getSelectedIndex();
        if ( idx == -1 ) {
            return;
        }
        this.downloadsModel.remove( idx );
        this.downloads.get( idx ).setCancel( true );
        this.downloads.remove( idx );
    }
}
