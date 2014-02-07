/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gretella.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.LinkedList;

/**
 *
 * @author alexandros
 */
public class QueryHitMessage extends GnutellaMessage {

    byte[] content;

    public QueryHitMessage( byte[] header, byte[] content ) {
        super( header );
        this.content = content;
    }

    public QueryHitMessage( byte[] GUID, byte hits, int port, byte[] ip, long speed, LinkedList<ResultSet> resultSet, byte[] id ) {
        super( GnutellaMessage.Query_Hit );
        int rusultSetLength = 0;
        for ( int i = 0; i < resultSet.size(); i++ ) {
            rusultSetLength += resultSet.get( i ).getBytes().length;
        }
        content = new byte[ 10 + rusultSetLength + 17 ];
        this.setHits( hits );
        this.setPort( port );
        this.setIpAddress( ip );
        this.setSpeed( speed );
        this.addResultSet( resultSet );
        this.setId( id );
        
        //System.out.print( "data " );
        //for( int i = 0 ; i < content.length; i ++ ) {
          //  System.out.print( " " + content[ i ]  );
        //}
        //System.out.println( "" );
        
        super.setGUID( GUID );
        super.setPayloadLegth( content.length );
    }

    public void setHits( byte hits ) {
        content[ 0] = hits;
    }

    public byte getHits() {        
        return content[0];
    }

    public void setPort( int port ) {
        ByteBuffer buffer = ByteBuffer.allocate( 4 );
        buffer.putInt( port );
        content[2] = buffer.get( 2 );
        content[1] = buffer.get( 3 );
    }

    public int getPort() {
        ByteBuffer buffer = ByteBuffer.allocate( 4 );
        buffer.put( 0, (byte) 0 );
        buffer.put( 1, (byte) 0 );
        buffer.put( 2, content[2] );
        buffer.put( 3, content[1] );
        return buffer.getInt();
    }

    public void setIpAddress( byte[] data ) {
        content[3] = data[0];
        content[4] = data[1];
        content[5] = data[2];
        content[6] = data[3];
    }

    public byte[] getIpAddress() {
        byte[] data = new byte[ 4 ];
        data[0] = content[3];
        data[1] = content[4];
        data[2] = content[5];
        data[3] = content[6];
        return data;
    }

    public String getHost() throws UnknownHostException {
        InetAddress addr = InetAddress.getByAddress( this.getIpAddress() );
        return addr.getHostAddress();
    }

    public long getSpeed() {
        ByteBuffer buffer = ByteBuffer.allocate( 8 );
        buffer.put( 0, (byte) 0 );
        buffer.put( 1, (byte) 0 );
        buffer.put( 2, (byte) 0 );
        buffer.put( 3, (byte) 0 );
        buffer.put( 4, content[10] );
        buffer.put( 5, content[9] );
        buffer.put( 6, content[8] );
        buffer.put( 7, content[7] );
        return buffer.getLong();
    }

    public void setSpeed( long speed ) {
        ByteBuffer buffer = ByteBuffer.allocate( 8 );
        buffer.putLong( speed );
        content[ 7] = buffer.get( 7 );
        content[ 8] = buffer.get( 6 );
        content[ 9] = buffer.get( 5 );
        content[ 10] = buffer.get( 4 );
    }

    public LinkedList<ResultSet> getResultSets() {
        LinkedList<ResultSet> resultSet = new LinkedList();
   
        int hits = this.getHits();

        int pos = 11;
        int j = pos;
        int length = 0;
        int close = 0;
        for ( int i = 0; i < hits; i++ ) {
            length = 0;
            close = 0;

            while ( close < 2 ) {
                if ( content[j + 8 + length] == 0 ) {
                    close++;
                }
                length++;
            }


            byte[] data = new byte[ 8 + length ];

            for ( int k = 0; k < length + 8; k++ ) {
                data[k] = this.content[j + k];
            }
            
            /*System.out.println( "-------------------" );
            System.out.print( "result set "  );
            for( int f = 0 ; f < data.length; f ++ ){
                System.out.print( " " + data[ f ] );            
            }
            System.out.println( "" );
            System.out.println( "-------------------" );*/
            
            ResultSet result = new ResultSet( data );
            resultSet.add( result );

            pos += length + 8;

            j += length + 8;
        }

        //System.out.println( "bika" );

        return resultSet;
    }

    private void addResultSet( LinkedList<ResultSet> resultSet ) {
        int offset = 11;
        for( ResultSet set : resultSet ){
            System.out.println( "sending file " + set.getFileName() + " idx " + set.getIndex() );
            byte[] data = set.getBytes();
            for( int i = 0; i < data.length; i ++ ){
                this.content[ offset + i ] = data[ i ];
            }
            offset += data.length;
        }
    }

    private void setId( byte[] id ) {
        for( int i = 0 ; i < id.length; i ++ ){
            this.content[ content.length - 17 + i ] = id[ i ];
        }
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
