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
 * @author alexm
 */
public class ResultSet {

    private byte[] content;
    
    public ResultSet( byte[] data ) {
        this.content = data;
    }

    public ResultSet( long index, long fileSize, byte[] fileName ) {
        content = new byte[ 8 + fileName.length + 1 ];
        this.setIndex( index );
        this.setFileSize( fileSize );
        System.out.println( "i have the name " + ByteUtil.getStringFromNullTerminated( fileName ));
        this.setFileName( fileName );
        content[ content.length - 1 ] = 0;
        System.out.print( "result set "  );
        for( int i = 0 ; i < content.length; i ++ ){
            System.out.print( " " + content[ i ] );            
        }
        System.out.println( "" );
    }

    public long getIndex() {
        ByteBuffer buffer = ByteBuffer.allocate( 8 );
        buffer.put( 0, (byte) 0 );
        buffer.put( 1, (byte) 0 );
        buffer.put( 2, (byte) 0 );
        buffer.put( 3, (byte) 0 );
        buffer.put( 4, (byte) content[3]  );
        buffer.put( 5, (byte) content[2]  );
        buffer.put( 6, (byte) content[1]  );
        buffer.put( 7, (byte) content[0]  );

        return buffer.getLong();
    }

    public void setIndex( long index ) {
        ByteBuffer buffer = ByteBuffer.allocate( 8 );
        buffer.putLong( index );
        content[0] = buffer.get( 7 );
        content[1] = buffer.get( 6 );
        content[2] = buffer.get( 5 );
        content[3] = buffer.get( 4 );
    }

    public long getFileSize() {
        ByteBuffer buffer = ByteBuffer.allocate( 8 );
        buffer.put( 0, (byte) 0 );
        buffer.put( 1, (byte) 0 );
        buffer.put( 2, (byte) 0 );
        buffer.put( 3, (byte) 0 );
        buffer.put( 4, (byte) content[7]  );
        buffer.put( 5, (byte) content[6]  );
        buffer.put( 6, (byte) content[5]  );
        buffer.put( 7, (byte) content[4]  );

        return buffer.getLong();
    }

    public void setFileSize( long size ) {
        ByteBuffer buffer = ByteBuffer.allocate( 8 );
        buffer.putLong( size );
        content[4] = buffer.get( 7 );
        content[5] = buffer.get( 6 );
        content[6] = buffer.get( 5 );
        content[7] = buffer.get( 4 );
    }

    public String getFileName() {
        String temp = getFullFileName();
        if( temp.endsWith( "urn:sha" ) == false || temp.length() < 9 )
            return temp;
        return temp.substring( 0, temp.length() - 8 );
    }

    public String getFullFileName() {        
        int i = 8;
        while ( true ) {            
            if ( this.content[ i ] == 0 ) {                
                break;
            }            
            i++;
        }
        
        try {
            return new String( content, 8, i - 8, "UTF-8"  );
        } catch ( UnsupportedEncodingException ex ) {
            Logger.getLogger( ResultSet.class.getName() ).log( Level.SEVERE, null, ex );
            return new String( content, 8, i - 8 );
        }
    }
    
    public void setFileName( byte[] fileName ) {
        for ( int i = 0; i < fileName.length; i++ ) {
            content[ 8 + i] = fileName[i];
        }
    }

    public byte[] getBytes() {
        return content;
    }    

    public void setContent( byte[] content ) {
        this.content = content;
    }
}
