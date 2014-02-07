/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gretella.util;

/**
 *
 * @author alexandros
 */
public class PingMessage extends GnutellaMessage {

    private byte[] content;

    public PingMessage() {
        super( GnutellaMessage.Ping );
        content = null;
    }

    public PingMessage( byte[] content ) {
        super( GnutellaMessage.Ping );
        this.content = content;
    }

    public PingMessage( byte[] header, byte[] content ) {
        super( header );
        this.content = content;
    }

    @Override
    public byte[] getBytes() {
        if ( content != null ) {
            byte[] ret = new byte[ header.length + content.length ];
            for ( int i = 0; i < header.length; i++ ) {
                ret[i] = header[i];
            }

            for ( int i = header.length; i < content.length + header.length; i++ ) {
                ret[i] = content[i - header.length];
            }
            return ret;
        } else {
            return header;
        }
    }
}
