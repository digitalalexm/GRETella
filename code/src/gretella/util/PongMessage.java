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
public class PongMessage extends GnutellaMessage {

    private byte[] content;

    public PongMessage() {
        super( GnutellaMessage.Pong );
        this.content = new byte[ 14 ];
    }
    
    public PongMessage( int port, byte[] ip, long count, long size  ) {
        super( GnutellaMessage.Ping );
        this.content = new byte[ 14 ];
        this.setPort( port );
        this.setIpAddress( ip );
        this.setFileCount( count );
        this.setFileSize( size );
        this.setPayloadLegth( content.length );
    }

    public PongMessage( byte[] header, byte[] content ) {
        super( header );
        this.content = content;
    }

    public String getHost() throws UnknownHostException {
        InetAddress addr = InetAddress.getByAddress( this.getIpAddress() );
        return addr.getHostAddress();
    }

    //public PongMessage( byte[] GUID, byte[] ip, short port ) {
//        super( GUID, GnutellaMessage.Pong );
  //  }

    public int getPort() {
        ByteBuffer buffer = ByteBuffer.allocate( 4 );
        buffer.put( 0, (byte) 0 );
        buffer.put( 1, (byte) 0 );
        buffer.put( 2, content[1] );
        buffer.put( 3, content[0] );
        return buffer.getInt();
    }

    public void setPort( int port ) {
        ByteBuffer buffer = ByteBuffer.allocate( 4 );
        buffer.putInt( port );
        content[1] = buffer.get( 2 );
        content[0] = buffer.get( 3 );
    }

    public void setIpAddress( byte[] data ) {
        content[2] = data[0];
        content[3] = data[1];
        content[4] = data[2];
        content[5] = data[3];
    }

    public byte[] getIpAddress() {
        byte[] data = new byte[4];
        data[0] = content[2];
        data[1] = content[3];
        data[2] = content[4];
        data[3] = content[5];
        return data;
    }
    
    public void setFileCount( long count ) {
        ByteBuffer buffer = ByteBuffer.allocate( 8 );
        buffer.putLong( count );
        content[6] = buffer.get( 7 );
        content[7] = buffer.get( 6 );
        content[8] = buffer.get( 5 );
        content[9] = buffer.get( 4 );
    }
    
    public long getFileCount(){
        ByteBuffer buffer = ByteBuffer.allocate( 8 );        
        buffer.put( 4, content[9] );
        buffer.put( 5, content[8] );
        buffer.put( 6, content[7] );
        buffer.put( 7, content[6] );
        return buffer.getLong();
    }
    
    public void setFileSize( long size ) {
        ByteBuffer buffer = ByteBuffer.allocate( 8 );
        buffer.putLong( size );
        content[10] = buffer.get( 7 );
        content[11] = buffer.get( 6 );
        content[12] = buffer.get( 5 );
        content[13] = buffer.get( 4 );
    }
    
    public long getFileSize(){
        ByteBuffer buffer = ByteBuffer.allocate( 8 );        
        buffer.put( 4, content[13] );
        buffer.put( 5, content[12] );
        buffer.put( 6, content[11] );
        buffer.put( 7, content[10] );
        return buffer.getLong();
    }
    
    @Override
    public byte[] getBytes(){
        byte[] ret = new byte[ header.length + content.length ];
        for ( int i = 0; i < header.length; i++ ) {
            ret[i] = header[i];
        }

        for ( int i = header.length; i < content.length + header.length; i++ ) {
            ret[i] = content[i - header.length];
        }
        return ret;
    }

}
