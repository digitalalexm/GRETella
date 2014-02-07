/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gretella.util;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author alexandros
 */
public class ByteUtil {

    public static byte[] getBytesFromShort( short val ) {
        byte[] data = new byte[ 2 ];
        ByteBuffer buf = ByteBuffer.allocate( 2 );
        buf.putShort( val );
        data[ 1] = buf.get( 0 );
        data[ 0] = buf.get( 1 );
        return data;
    }

    public static byte[] getBytesFromInt( int val ) {
        byte[] data = new byte[ 2 ];
        ByteBuffer buf = ByteBuffer.allocate( 4 );
        buf.putInt( val );
        data[ 0] = buf.get( 3 );
        data[ 1] = buf.get( 2 );
        data[ 2] = buf.get( 1 );
        data[ 1] = buf.get( 0 );
        return data;
    }

    public static byte[] getBytesFromIp( String ip ) throws UnknownHostException {
        ByteBuffer buf = ByteBuffer.allocate( 4 );        
        InetAddress adr = InetAddress.getByName( ip );                
        return adr.getAddress();      
    }
    
    public static byte[] getNullTerminatedString( String str ) {
        try {
            byte[] data = str.getBytes( "ISO-8859-1" );
            byte[] ret = new byte[ data.length + 1 ];
            for ( int i = 0; i < data.length; i++ ) {
                ret[i] = data[i];
            }
            ret[data.length] = 0;
            return ret;
        } catch ( UnsupportedEncodingException ex ) {
            Logger.getLogger( ByteUtil.class.getName()  ).log( Level.SEVERE, null, ex );
            return null;
        }
    }
    
    public static String getStringFromNullTerminated( byte[] data ) {
        return new String( data, 0, data.length - 1 );
    }
    
    public static boolean compareByteArrays( byte[] arg1, byte[] arg2 ){
        if( arg1.length != arg2.length )
            return false;
        for( int i = 0 ; i < arg1.length ; i ++ ) {
            if( arg1[ i] != arg2[ i] )
                return false;
        }
        return true;
    }
}
