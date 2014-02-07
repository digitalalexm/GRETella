/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gretella;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author alexandros
 */
public class GnutellaServer implements Runnable {

    private ServerSocket serverSocket = null;
    private Socket socket;
    private int port;
    private boolean stop;
    private GretellaView owner;
    private boolean connect;
    
    public GnutellaServer( int port, GretellaView o ) throws IOException {
        this.port = port;
        stop = false;
        serverSocket = new ServerSocket( port );
        owner = o;
        connect = true;
        new Thread( this ).start();
    }

    public void run() {
        //int i = 0;
        System.out.println( "waiting incommng sockets" );
        while ( stop == false ) {
            try {
                socket = serverSocket.accept();
                //i++;
                //System.out.println( "i have an application" );
                //System.out.println( "incomming = " + i );
                new GnutellaClient( socket, this.owner );                         
            } catch ( IOException ex ) {
                Logger.getLogger( GnutellaServer.class.getName() ).log( Level.SEVERE, null, ex );
                break;
            }
        }
        try {
            if( socket != null )
                socket.close();
        } catch ( IOException ex ) {
            Logger.getLogger( GnutellaServer.class.getName() ).log( Level.SEVERE, null, ex );
        }
    }

    public boolean isConnect() {
        return connect;
    }

    public void setConnect( boolean connect ) {
        this.connect = connect;
    }
}
