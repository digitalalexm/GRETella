/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gretella.util;

import java.nio.ByteBuffer;

/**
 *
 * @author alexandros
 */
public class GnutellaMessage {

    protected byte[] header;
    public static final int DEFAULT_TTL = 3;
    public static final byte Ping = 0x00;
    public static final byte Pong = 0x01;
    public static final byte Bye = 0x02;
    public static final byte Push = 0x40;
    public static final byte Query = (byte) 0x80;
    public static final byte Query_Hit = (byte) 0x81;
    private byte TTL = DEFAULT_TTL;

    public GnutellaMessage( byte[] data ) {
        this.header = data;
    }
    
    /*
    public GnutellaMessage( byte[] header, byte[] content ) {
        byte[] data = new byte[ header.length + content.length ];
        for ( int i = 0; i < header.length; i++ ) {
            data[i] = header[i];
        }

        for ( int i = header.length; i < content.length; i++ ) {
            data[i] = content[i - header.length];
        }
    }*/

    public GnutellaMessage( byte payLoadType ) {
        header = new byte[ 23 ];
        putHeader();
        header[ 16] = payLoadType;
        header[ 17] = DEFAULT_TTL;
        header[ 18] = 1;
        header[ 19] = 0;
        header[ 20] = 0;
        header[ 21] = 0;
        header[ 22] = 0;
    }

    public GnutellaMessage( byte[] GUID, byte payLoadType ) {
        header = new byte[ 23 ];
        for ( int i = 0; i < 16; i++ ) {
            header[i] = GUID[i];
        }
        header[ 16] = payLoadType;
        header[ 17] = DEFAULT_TTL;
        header[ 18] = 1;
        header[ 19] = 0;
        header[ 20] = 0;
        header[ 21] = 0;
        header[ 22] = 0;
    }

    void setGUID( byte[] GUID ) {
        for( int i = 0; i < 16 ; i ++ ){
            this.header[ i ] = GUID[ i ];
        }
    }

    private void putHeader() {
        byte[] header = GUIDGenerator.generate();
        for ( int i = 0; i < header.length; i++ ) {
            this.header[i] = header[i];
        }
    }

    public byte[] getBytes() {
        return this.header;
    }

    public void updateMessage() {
        header[ 17]--;
        header[ 18]++;
    }

    public byte getType() {
        return header[ 16];
    }

    public byte getTTL() {
        return header[ 17];
    }

    public byte getHops() {
        return header[ 18];
    }

    public void setTTY( byte val ) {
        header[ 17] = val;
    }

    public void setHops( byte val ) {
        header[ 18] = val;
    }

    public int getPayloadLength() {
        ByteBuffer buffer = ByteBuffer.allocate( 4 );
        buffer.put( 0, this.header[22] );
        buffer.put( 1, this.header[21] );
        buffer.put( 2, this.header[20] );
        buffer.put( 3, this.header[19] );

        //buffer.rewind();
        return buffer.getInt();
    }
    
    public void setPayloadLegth( long length ){
        ByteBuffer buffer = ByteBuffer.allocate( 8 );
        buffer.putLong( length );
        this.header[19] = buffer.get( 7 );
        this.header[20] = buffer.get( 6 );
        this.header[21] = buffer.get( 5 );
        this.header[22] = buffer.get( 4 );
    }
    
    public byte[] getGUID() {
        byte[] GUID = new byte[ 16 ];
        for( int i = 0 ; i < 16 ; i++ ){
            GUID[ i ] = this.header[ i ];
        }
        return GUID;
    }
}
