/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gretella;

import gretella.util.ByteUtil;
import gretella.util.FileIndex;
import gretella.util.GnutellaMessage;
import gretella.util.PingMessage;
import gretella.util.PongMessage;
import gretella.util.QueryMessage;
import gretella.util.GnutellaServent;
import gretella.util.QueryHitMessage;
import gretella.util.ResultSet;
import gretella.util.SearchItem;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author alexandros
 */
public class GnutellaClient implements Runnable {

    private Socket clientSocket = null;
    //private BufferedReader input = null;
    //private PrintWriter output = null;
    private boolean state;
    DataInputStream inputStream;
    DataOutputStream outputStream;
    private GnutellaServent servent;
    private LinkedList<GnutellaServent> extraHosts;
    private boolean online;
    private GretellaView owner;

    public GnutellaClient( String host, int port, GretellaView parrent ) throws IOException {
        owner = parrent;
        //System.out.println( "==============================" );
        //System.out.println( "Trying to connect " + host + " " + port );
        this.servent = new GnutellaServent( host, port );
        initSocket( this.servent );
        extraHosts = new LinkedList();
        state = outgoingHandShake( true );
        if ( state == true ) {
            new Thread( this ).start();
        }
    }

    public GnutellaClient( Socket socket, GretellaView parrent ) throws IOException {
        this.clientSocket = socket;
        this.inputStream = new DataInputStream( clientSocket.getInputStream() );
        this.outputStream = new DataOutputStream( clientSocket.getOutputStream() );
        this.owner = parrent;
        this.servent = new GnutellaServent( socket.getInetAddress().getHostAddress(), socket.getPort() );
        state = incommingHandShake();
        if ( state == true ) {
            online = true;
            new Thread( this ).start();
            System.out.println( "agent " + servent.getAgentName() );
            owner.addClient( this );
        }
    }

    public boolean isHost( GnutellaServent host ) {
        return this.servent.equals( host );
    }

    public void search( QueryMessage msg ) {
        try {
            if ( this.clientSocket.isConnected() == false ) {
                System.out.println( "i am disconnected " );
                return;
            }
            if ( outputStream != null ) {
                ///System.out.println( "i will send '" + msg.getQuery() + "'" );
                //System.out.println( "i will send " + msg.getBytes().length + " bytes" );
                this.outputStream.write( msg.getBytes() );
                this.outputStream.flush();
            }
        } catch ( IOException ex ) {
            System.out.println( "error in sending query" );
            Logger.getLogger( GnutellaClient.class.getName() ).log( Level.SEVERE, null, ex );
            this.owner.disconnected( this.servent );
        }
    }

    void send( GnutellaMessage msg ) {
        try {
            if ( this.clientSocket.isConnected() == false ) {
                return;
            }
            if ( outputStream != null ) {
                this.outputStream.write( msg.getBytes() );
                this.outputStream.flush();
            }
        } catch ( IOException ex ) {
            //System.out.println( "error in forwarding query" );
            //Logger.getLogger( GnutellaClient.class.getName() ).log( Level.SEVERE, null, ex );
        }
    }

    private void initSocket( GnutellaServent h ) throws IOException {
        clientSocket = new Socket( h.getHostName(), h.getPort() );
        clientSocket.setSoTimeout( 0 );
        //clientSocket.setSoTimeout( 50000 );
        clientSocket.setTcpNoDelay( true );
        inputStream = new DataInputStream( clientSocket.getInputStream() );
        outputStream = new DataOutputStream( clientSocket.getOutputStream() );
        setOnline( true );
    }

    public void closeSocket() throws IOException {
        inputStream.close();
        inputStream.close();
        clientSocket.close();
        setOnline( false );
    }

    private void expand() throws IOException {
        for ( GnutellaServent thost : getExtraHosts() ) {
            initSocket( thost );
            boolean ret = outgoingHandShake( false );
            if ( ret ) {
                this.servent.setHostName( thost.getHostName() );
                this.servent.setPort( thost.getPort() );
                new Thread( this ).start();
                break;
            } else {
                closeSocket();
            }
        }
    }

    private boolean incommingHandShake() {
        try {
            String ans = readUntilDourbleEndOfLine();

            System.out.println( ans );

            if ( ans.contains( GnutellaProtocol.CRITICAL_INIT_MSG ) ) {

                if ( owner.acceptMoreConnections() ) {
                    System.out.println( "ok" );

                    if ( ans.contains( "User-Agent:" ) ) {
                        System.out.println( "I have found user agent" );
                        int pos = ans.indexOf( "User-Agent:" );
                        pos += "User-Agent:".length();
                        StringTokenizer tok = new StringTokenizer( ans.substring( pos ).trim() );
                        servent.setAgentName( tok.nextToken( "\n" ) );
                    }

                    this.outputStream.write( GnutellaProtocol.GNUTELLA_OK.getBytes() );
                    this.outputStream.flush();

                    //inputStream.read( bytes );
                    //System.out.println( "temp=" + new String( bytes ) );
                    ans = readUntilDourbleEndOfLine();
                    System.out.println( ans );
                    return true;
                } else {
                    //System.out.println( "ok" );
                    this.outputStream.write( GnutellaProtocol.BUSY.getBytes() );
                    this.outputStream.flush();
                    return false;
                }
            } else if( ans.contains( "HTTP" ) ){                
                new HTTPUpload( ans, this.clientSocket, this.inputStream, this.outputStream, owner.getmaxUpload(), owner );
                return false;
            } else {
                return false;
            }


        } catch ( IOException ex ) {
            Logger.getLogger( GnutellaClient.class.getName() ).log( Level.SEVERE, null, ex );
            return false;
        }
    }

    private boolean outgoingHandShake( boolean extra ) {
        try {
            System.out.println( "Trying to connecto to " + this.servent.getHostName() + " : " + servent.getPort() );

            byte[] greetingData = GnutellaProtocol.INIT_MSG.getBytes();
            outputStream.write( greetingData, 0, greetingData.length );
            outputStream.flush();

            String ans = readUntilDourbleEndOfLine();

            System.out.println( ans );
            if ( ans.contains( "GNUTELLA/0.6 200 OK" ) ) {
                if ( ans.contains( "User-Agent:" ) ) {
                    System.out.println( "I have found user agent" );
                    int pos = ans.indexOf( "User-Agent:" );
                    pos += "User-Agent:".length();
                    StringTokenizer tok = new StringTokenizer( ans.substring( pos ).trim() );
                    servent.setAgentName( tok.nextToken( "\n" ) );
                }

                if ( ans.contains( "Remote-IP:" ) ) {
                    int pos = ans.indexOf( "Remote-IP:" );
                    pos += "Remote-IP:".length();
                    StringTokenizer tok = new StringTokenizer( ans.substring( pos ).trim() );
                    String ip = tok.nextToken( "\n" );
                    //System.out.println( "I have found my IP " + ip );
                    byte[] data = null;
                    data = ByteUtil.getBytesFromIp( ip );
                    owner.setIp( data );
                }
                greetingData = GnutellaProtocol.CLASIC_GNUTELLA_OK.getBytes();
                outputStream.write( greetingData, 0, greetingData.length );
                outputStream.flush();
                System.out.println( "Connected to " + servent.getHostName() );
                return true;
            } else {
                System.out.println( "refused connection by " + servent.getHostName() );
            //System.out.println( "==============================" );
            }

            if ( extra == true ) {
                if ( ans.contains( "X-Try-Ultrapeers:" ) ) {
                    //System.out.println( "I have found x-try" );
                    int pos = ans.indexOf( "X-Try-Ultrapeers:" );
                    StringTokenizer tok = new StringTokenizer( ans.substring( pos ).trim() );
                    tok.nextToken( ":" );
                    String token = tok.nextToken( "\n" );
                    tok = new StringTokenizer( token, "," );
                    while ( tok.hasMoreTokens() ) {
                        GnutellaServent thost = new GnutellaServent();
                        //System.out.println( "" );
                        StringTokenizer tokenizer = new StringTokenizer( tok.nextToken(), ":" );
                        if ( tokenizer.hasMoreElements() ) {
                            thost.setHostName( tokenizer.nextToken().trim() );
                            if ( tokenizer.hasMoreTokens() ) {
                                thost.setPort( Integer.parseInt( tokenizer.nextToken().trim() ) );
                            } else {
                                thost.setPort( 36529 );
                            }
                            //System.out.println( "new host '" + thost.getHostName() + "' port " + thost.getPort() );
                            this.getExtraHosts().add( thost );
                        }
                    }
                }
            }

            //System.out.println( input.toString() );            
            return false;
        } catch ( IOException ex ) {
            Logger.getLogger( GnutellaClient.class.getName() ).log( Level.SEVERE, null, ex );
            return false;
        }
    }

    public boolean isOk() {
        return state;
    }

    public GnutellaServent getHost() {
        return servent;
    }

    public void run() {
        try {
            PingMessage ping = new PingMessage();
            byte[] t = ping.getBytes();
            outputStream.write( ping.getBytes() );
            outputStream.flush();

            while ( this.online == true ) {

                byte[] header = this.readBytes( 23 );
                if ( header != null ) {
                    GnutellaMessage msg = new GnutellaMessage( header );
                    byte[] content = this.readBytes( msg.getPayloadLength() );
                    if ( content == null ) {
                        //System.out.println( "exception occured!!!!!!!!!!!" );
                        break;
                    }

                    int type = msg.getType();
                    //System.out.println( " answer = " + GnutellaMessage.Query_Hit + " msg = " + msg.getType() );
                    switch ( type ) {
                        case GnutellaMessage.Ping:
                            //System.out.println( "i have a ping" );
                            processPingMessage( new PingMessage( header, content ) );
                            break;
                        case GnutellaMessage.Pong:
                            //System.out.println( "i have a pong from " + this.servent.getHostName() );
                            processPongMessage( new PongMessage( header, content ) );
                            break;
                        case GnutellaMessage.Query:
                            //System.out.println( "i have a query from " + this.servent.getHostName() );
                            processQueryMessage( new QueryMessage( header, content ) );
                            break;
                        case GnutellaMessage.Query_Hit:
                            //System.out.println( "i have a query hit" );
                            processQueryHitMessage( new QueryHitMessage( header, content ) );
                            break;
                        case GnutellaMessage.Push:
                            //System.out.println( "i have a push message" );
                            break;
                        default:
                        //System.out.println( "Uknown message " + type );
                    }
                //System.out.println( "-----------------------------" );
                //break;                    
                } else {
                    //System.out.println( "EOF reached" );
                    break;
                }
                try {
                    Thread.sleep( 10 );
                } catch ( InterruptedException ex ) {
                }
            }
        } catch ( IOException ex ) {
            Logger.getLogger( GnutellaClient.class.getName() ).log( Level.SEVERE, null, ex + " \n " + this.servent.getHostName() + " type " + servent.getAgentName() );
        }        
        try {
            closeSocket();
        } catch ( IOException ex ) {
            Logger.getLogger( GnutellaClient.class.getName() ).log( Level.SEVERE, null, ex );
        }
        this.owner.disconnected( this.servent );
        System.gc();
    }

    private void processPingMessage( PingMessage msg ) {
        try {
            //System.out.println( "owner count " + owner.getFileCount() + " size " + owner.getFileSize() );
            PongMessage m = new PongMessage( owner.getPort(), owner.getIp(), owner.getFileCount(), owner.getFileSize() );
            //System.out.println( "i have made a pong with port " + m.getPort() + " count " + m.getFileCount() + " size " + m.getFileSize() );
            this.outputStream.write( m.getBytes() );

            owner.routeMessage( msg, servent.getHostName(), servent.getPort() );
        } catch ( IOException ex ) {
            Logger.getLogger( GnutellaClient.class.getName() ).log( Level.SEVERE, null, ex );
            this.owner.disconnected( this.servent );
        }
    }

    private void processPongMessage( PongMessage msg ) {
        owner.newPong( msg );
    }

    private void processQueryMessage( QueryMessage msg ) {        
        String query =  msg.getQuery();
        StringTokenizer tok = new StringTokenizer( query, " ");
        LinkedList< String > tokens = new LinkedList();
        while( tok.hasMoreTokens() ){
            tokens.add( tok.nextToken() );
        }
        LinkedList<FileIndex> indeces = owner.getIndeces( tokens );
        //System.out.println( "I have received the query '" + msg.getQuery() + "' length " + msg.getPayloadLength() + " min speed  " + msg.getMinimumSpeed() );
        if( indeces.size() > 0  ){
            System.out.println( "i have file to send " + indeces.size() );
            LinkedList<ResultSet> resultSet = new LinkedList();
            for( FileIndex index : indeces ){
                //System.out.println( "constructing result set with file " + index.getFileName() + " idx " + index.getId()  );                
                ResultSet set = new ResultSet( index.getId(), index.getFileSize(), ByteUtil.getNullTerminatedString( index.getFileName() ) );                
                //System.out.println( "I have xonstructed result" + set.getFileName() + " idx " + set.getIndex() );                
                resultSet.add( set );
            }
            System.out.println( "i have construct the resultsets " + resultSet.size() );
            QueryHitMessage omsg = new QueryHitMessage( msg.getGUID(), (byte)resultSet.size(), owner.getPort(), owner.getIp(),owner.getMaxSpeed(), resultSet, owner.getId() );
            try {
                this.outputStream.write( omsg.getBytes() );
            } catch ( IOException ex ) {
                System.out.println( "error in sending query hit" );
                //Logger.getLogger( GnutellaClient.class.getName() ).log( Level.SEVERE, null, ex );
                this.owner.disconnected( this.servent );
            }
        }
    }

    private void processQueryHitMessage( QueryHitMessage msg ) {
        try {
            System.out.println( "*************************" );
            System.out.println( "query hit from " + msg.getHost() + " hits  " + msg.getHits() );
            LinkedList<ResultSet> res = msg.getResultSets();

            //for ( int i = 0; i < res.size(); i++ ) {
            //    System.out.println( "res " + i + " " + res.get( i ).getFileName() + " index " + res.get( i ).getIndex() );
            //}

            byte[] activeSearchGuid = owner.getActiveSearchGUID();
            /*if( activeSearchGuid == null ) {
            owner.routeMessage( msg, servent.getHostName(), servent.getPort() );
            return;
            }*/

            /*System.out.print( "active guid = " );
            for( int i = 0 ; i < 16 ; i ++ ){
            System.out.print( activeSearchGuid[ i ] + " " );
            }*/
            //System.out.println( "" );
            byte[] temp = msg.getGUID();
            //System.out.print( "msg guid = " );
            //for( int i = 0 ; i < 16 ; i ++ ){
            //    System.out.print( temp[ i ] + " " );
            //}

            if ( ByteUtil.compareByteArrays( activeSearchGuid, temp ) ) {
                System.out.println( "ine to diko moy hit" );
                for ( ResultSet result : res ) {
                    System.out.println( " res " + result.getFileName() + " idx " + result.getIndex() );
                    owner.addSearchItem( new SearchItem( result.getFullFileName(), result.getFileName(), result.getFileSize(), result.getIndex(), msg.getHost(), msg.getPort() ) );
                }
            }// else {
        //   System.out.println( "den einai to diko moy hit" );
        //}
        //}

        } catch ( UnknownHostException ex ) {
            Logger.getLogger( GnutellaClient.class.getName() ).log( Level.SEVERE, null, ex );
        }
    //System.out.println( "bgeno!!!!" );
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline( boolean online ) {
        this.online = online;
    }

    byte[] readBytes( int num ) {
        int read = 0;
        int tmp = 0;
        //System.out.println( " num = " + num );
        byte[] data = new byte[ num ];
        do {
            try {
                tmp = inputStream.read( data, read, num - read );
                if ( read == -1 ) {
                    //System.out.println( "bika sto eof!!!!!!!!!!" );
                    tmp = 0;
                }
                read += tmp;
            } catch ( IOException ex ) {
                Logger.getLogger( GnutellaClient.class.getName() ).log( Level.SEVERE, null, ex );
                return null;
            }
        } while ( read < num );
        //System.out.println( "num = " + num + " read = " + read );
        return data;
    }

    public LinkedList<GnutellaServent> getExtraHosts() {
        return extraHosts;
    }

    String readUntilDourbleEndOfLine() throws IOException {
        StringBuffer buffer = new StringBuffer();
        String line = "-";

        while ( true ) {
            line = inputStream.readLine();
            //System.out.println( " line = '" + line +"'" );
            if( line == null ){
                return null;
            }
            if ( line.equals( "" ) ) {
                break;
            }
            buffer.append( line + "\n" );
        }
        return buffer.toString();
    }
}
