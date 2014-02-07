/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gretella.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 *
 * @author alexandros
 */
public class GnutellaServent {

    private String hostName;
    private int port;
    private String agentName;

    public GnutellaServent() {
        hostName = "127.0.0.1";
        port = 0;
        agentName = "";
    }

    public GnutellaServent( String host, int port ) {
        this.hostName = host;
        this.port = port;
        agentName = "";
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName( String hostName ) {
        this.hostName = hostName;
    }

    public int getPort() {
        return port;
    }

    public void setPort( int port ) {
        this.port = port;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName( String agentName ) {
        this.agentName = agentName;
    }

    public byte[] getPortBytes() {
        byte[] data = new byte[ 2 ];
        ByteBuffer buf = ByteBuffer.allocate( 4 );
        buf.putInt( port );
        data[ 1 ] = buf.get( 2 );
        data[ 0 ] = buf.get( 3 );
        return data;
    }

    public byte[] getIpBytes() throws UnknownHostException {        
        ByteBuffer buf = ByteBuffer.allocate( 4 );        
        InetAddress adr = InetAddress.getByName( this.hostName );        
        //System.out.println( " 0 " + (int) data[ 0 ] );
        //System.out.println( " 1 " + (int) data[ 1 ] );
        //System.out.println( " 2 " + (int) data[ 2 ] );
        //System.out.println( " 3 " + (int) data[ 3 ] );
        return adr.getAddress();      
    }
    
    public boolean equals( GnutellaServent other    ){
        if( other.getHostName().equals( this.getHostName() ) && other.getPort() == this.getPort() )
            return true;
        else 
            return false;        
    }
}
