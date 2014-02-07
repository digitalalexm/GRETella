/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gretella.util;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author alexandros
 */
public class QueryMessage extends GnutellaMessage {

    byte[] content;

    public QueryMessage() {
        super( GnutellaMessage.Query );
        content = null;
    }

    public QueryMessage( int minSpeed, byte[] query ) {
        super( GnutellaMessage.Query );
        content = new byte[ 2 + query.length ];
        this.setMinimumSpeed( minSpeed );
        this.setQuery( query );
        this.setPayloadLegth( content.length );
    }

    public QueryMessage( byte[] header, byte[] content ) {
        super( header );
        this.content = content;
    }

    public int getMinimumSpeed() {
        ByteBuffer buffer = ByteBuffer.allocate( 4 );
        buffer.put( 0, (byte) 0 );
        buffer.put( 1, (byte) 0 );
        buffer.put( 2, content[1] );
        buffer.put( 3, content[0] );
        return buffer.getInt();
    }

    public void setMinimumSpeed( int speed ) {
        ByteBuffer buffer = ByteBuffer.allocate( 4 );
        buffer.putInt( speed );
        content[ 0] = buffer.get( 3 );
        content[ 1] = buffer.get( 2 );
    }

    public void setQuery( byte[] data ) {
        for ( int i = 0; i < data.length; i++ ) {
            content[ 2 + i] = data[i];
        }
    }

    public String getQuery() {
        byte[] b = new byte[ super.getPayloadLength() - 2 ];
        int pos = b.length;
        for ( int i = 0; i < b.length; i++ ) {
            if ( content[2 + i] != 0 ) {
                b[i] = content[2 + i];
            } else {
                pos = i;
                break;
            }
        }
        try {
            return new String( b, 0, pos, "UTF-8" );
        } catch ( UnsupportedEncodingException ex ) {
            Logger.getLogger( QueryMessage.class.getName() ).log( Level.SEVERE, null, ex );
            return new String( b, 0, pos );
        }
    }

    public String getTrueQuery() {

        byte[] b = new byte[ super.getPayloadLength() - 2 ];
        int pos = b.length;
        for ( int i = 0; i < b.length; i++ ) {
            if ( content[ 2 + i] != 0 ) {
                b[i] = content[ 2 + i];
            } else {
                pos = i;
                break;
            }
        }

        return new String( b, 0, pos );
    }

    @Override
    public byte[] getBytes() {
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
