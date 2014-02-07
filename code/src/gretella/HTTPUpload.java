/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gretella;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author alexandros
 */
public class HTTPUpload implements Runnable {

    private Socket clientSocket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private long maxSpeed;
    private String request;
    private String fileName;
    private int indexId;
    GretellaView owner;

    public HTTPUpload( String request, Socket clientSocket, DataInputStream inputStream, DataOutputStream outputStream, long maxSpeed, GretellaView owner ) {
        this.clientSocket = clientSocket;
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.maxSpeed = maxSpeed * 1024;
        this.request = request;
        this.owner = owner;
        new Thread( this ).start();
    }

    public void run() {
        try {
            if ( getFileNameAndIndex() == false ) {
                try {
                    this.outputStream.write( HTTPProtocol.responce400().getBytes() );
                    this.outputStream.flush();
                } catch ( IOException ex ) {
                    Logger.getLogger( HTTPUpload.class.getName() ).log( Level.SEVERE, null, ex );
                }
                return;
            }

            File file = this.owner.getActualPath( indexId, fileName );

            if ( file == null ) {
                try {
                    this.outputStream.write( HTTPProtocol.responce404().getBytes() );
                    this.outputStream.flush();
                } catch ( IOException ex ) {
                    Logger.getLogger( HTTPUpload.class.getName() ).log( Level.SEVERE, null, ex );
                }
                return;
            }

            String resp = HTTPProtocol.okResponse( file.getName(), file.length() );
            System.out.println( resp );

            this.outputStream.write( resp.getBytes() );
            this.outputStream.flush();
            
            FileInputStream fin = new FileInputStream( file );            
            
            byte[] rdata = new byte[ 512 ];
            
            //System.out.println( "download " + downloaded );
            int numRead = 0;
            long currentTime = System.currentTimeMillis();
            long prevTime = currentTime;
            int sum = 0;
            long totalSize = 0;
            while ( true ) {
                numRead = fin.read( rdata );
                if( numRead == -1 )
                    break;
                System.out.println( "uloading " + sum );
                this.outputStream.write( rdata, 0,numRead );
                this.outputStream.flush();
                
                totalSize += numRead;
                sum += numRead;      
                
                currentTime = System.currentTimeMillis();                                               
                if( currentTime - prevTime >= 1000 ) {
                    prevTime = currentTime;
                    sum = 0;
                } else  if( sum >= this.maxSpeed ) {
                    try {
                        Thread.sleep( 1000 - currentTime + prevTime );
                    } catch ( InterruptedException ex ) {
                        //Logger.getLogger( HTTPUpload.class.getName() ).log( Level.SEVERE, null, ex );
                    }
                }
                
                if( totalSize == file.length() )
                    break;
            }
            
            fin.close();
            this.outputStream.close();
            System.out.println( "Uploading is finished" );
        } catch ( IOException ex ) {
            Logger.getLogger( HTTPUpload.class.getName() ).log( Level.SEVERE, null, ex );
        }

    }

    private boolean getFileNameAndIndex() {
        StringTokenizer tok = new StringTokenizer( request, "\n" );
        String line = tok.nextToken();
        if ( request.startsWith( "GET /get/" ) == false ) {
            return false;
        }
        StringTokenizer t1 = new StringTokenizer( line, " " );
        t1.nextToken();
        String surl = t1.nextToken();

        StringTokenizer t2 = new StringTokenizer( surl, "/" );

        if ( t2.hasMoreTokens() == false ) {
            return false;
        }
        t2.nextToken();

        if ( t2.hasMoreTokens() == false ) {
            return false;
        }
        indexId = Integer.parseInt( t2.nextToken() );


        if ( t2.hasMoreTokens() == false ) {
            return false;
        }
        fileName = URLDecoder.decode( t2.nextToken() );

        while ( t2.hasMoreElements() ) {
            System.out.println( "" + t2.nextToken() );
        }

        return true;
    }
}
