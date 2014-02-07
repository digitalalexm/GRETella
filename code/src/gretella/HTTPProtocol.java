/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gretella;

/**
 *
 * @author alexandros
 */
public class HTTPProtocol {

    public static String makeGet( String file ) {
        StringBuffer data = new StringBuffer();

        data.append( "GET /get/" + file + " HTTP/1.1\n" );
        data.append( "User-Agent: Gnutella\n" );
        //data.append( "Connection: Keep-Alive\n" );
        data.append( "Connection: Close\n" );
        data.append( "Range: bytes=0-\n" );
        data.append( "\n" );

        return data.toString();
    }

    public static String okResponse( String fileName, long fileSize ) {
        StringBuffer data = new StringBuffer();
        
        data.append( "HTTP/1.1 200 OK\n" );
        data.append( "Content-Disposition: attachment; filename=\"" + fileName + "\"\n" );
        data.append( "Content-Range: bytes=0-\n" );                               
        data.append( "Content-Length: "+ fileSize + "\n" );
        data.append( "Content-Type: application/binary\n" );
        data.append( "Connection: Close\n" );
        data.append( "\n" );
        
        return data.toString();
    }
    
    public static String responce404() {
        StringBuffer data = new StringBuffer();
        
        data.append( "HTTP/1.1 404 File Not Found\n" );        
        data.append( "\n" );
        
        return data.toString();
    }
    
    public static String responce400() {
        StringBuffer data = new StringBuffer();
        
        data.append( "HTTP/1.1 400 Bad URL format\n" );        
        data.append( "\n" );
        
        return data.toString();
    }
}
