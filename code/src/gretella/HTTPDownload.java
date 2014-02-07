/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gretella;

import gretella.util.*;
import gretella.GretellaView;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author alexandros
 */
public class HTTPDownload implements Runnable {

    private int id;
    private long totalSize;
    private long downloaded;
    private float averageSpeed;
    private float currentSpeed;
    private HttpURLConnection con;
    private boolean cancel;
    private int maxSpeed;
    private String fileName;
    private GretellaView owner;
    private boolean selected;
    private SearchItem item;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private Socket clientSocket;

    public HTTPDownload( int id, SearchItem item, int maxSpeed, GretellaView owner ) throws MalformedURLException, IOException {
        this.id = id;
        this.item = item;
        this.maxSpeed = maxSpeed * 1024;
        this.owner = owner;
        this.totalSize = item.getFileSize();
        new Thread( this ).start();
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize( long totalSize ) {
        this.totalSize = totalSize;
    }

    public long getDownloaded() {
        return downloaded;
    }

    public void setDownloaded( long downloaded ) {
        this.downloaded = downloaded;
    }

    public float getAverageSpeed() {
        return averageSpeed;
    }

    public void setAverageSpeed( float averageSpeed ) {
        this.averageSpeed = averageSpeed;
    }

    public float getCurrentSpeed() {
        return currentSpeed;
    }

    public void setCurrentSpeed( float currentSpeed ) {
        this.currentSpeed = currentSpeed;
    }

    public boolean isCancel() {
        return cancel;
    }

    public void setCancel( boolean cansel ) {
        this.cancel = cansel;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected( boolean selected ) {
        this.selected = selected;
    }

    public void run() {
        try {
            File f = createFile();
            try {
                initSocket();

                String handshake = HTTPProtocol.makeGet( item.getFileIndex() + "/" + URLEncoder.encode( item.getFileName() ) );
                System.out.println( "handshake = " + handshake );

                byte[] data = handshake.getBytes();
                outputStream.write( data, 0, data.length );

                String ans = readUntilDourbleEndOfLine();

                System.out.println( "ans " + ans );

                long startTime = System.currentTimeMillis();
                long currentTime = startTime;
                long prevTime = startTime;

                FileOutputStream fout = new FileOutputStream( f );

                byte[] rdata = new byte[ 512 ];
                
                long tempDownloaded = 0;
                while ( cancel == false ) {
                    //System.out.println( "download " + downloaded );
                    int numRead;
                    numRead = inputStream.read( rdata );
                    if ( numRead == -1 ) {
                        break;
                    }
                    tempDownloaded += numRead;
                    
                    fout.write( rdata, 0, numRead );
                    downloaded += numRead;

                    currentTime = System.currentTimeMillis();
                    long time = Math.max( (currentTime - startTime ) / 1000, 1 );
                    this.averageSpeed = ( this.downloaded / 1024 )  / (float)time;
                    
                    if( currentTime - prevTime > 1000 ) {
                        prevTime = currentTime ;
                        currentSpeed = tempDownloaded / (float)1024;
                        //currentSpeed /= 1000;
                        tempDownloaded = 0;                        
                    }
                    
                    if ( this.selected == true ) {
                        owner.setDownloadStatus( totalSize / 1024, downloaded / 1024, averageSpeed, currentSpeed );
                    }
                    
                    if( tempDownloaded >= this.maxSpeed ) {
                        //try {
                        //    Thread.sleep( 1000 - ( currentTime - prevTime ) );
                        //} catch ( InterruptedException ex ) {
                        //}
                    }
                }

                fout.close();
            } catch ( UnknownHostException ex ) {
                Logger.getLogger( HTTPDownload.class.getName() ).log( Level.SEVERE, null, ex );
                cancel = true;
            } catch ( IOException ex ) {
                Logger.getLogger( HTTPDownload.class.getName() ).log( Level.SEVERE, null, ex );
                cancel = true;
            }



            disconnect();
            if ( cancel == true ) {
                System.out.println( "Download canceled or terminated by connection errors" );
            }
        } catch ( IOException ex ) {
            Logger.getLogger( HTTPDownload.class.getName() ).log( Level.SEVERE, null, ex );            
        }
        owner.remove( this );
    }

    public int getId() {
        return this.id;
    }

    private File createFile() throws IOException {
        File file = new File( GretellaView.DOWNLOAD_FOLDER + File.separator + item.getFileName() );
        if ( file.exists() == true ) {
            file.delete();
        }
        System.out.println( "storing file to " + file.getAbsolutePath() );
        file.createNewFile();
        return file;
    }

    private void disconnect() {
        try {
            this.outputStream.close();
        } catch ( IOException ex ) {
            Logger.getLogger( HTTPDownload.class.getName() ).log( Level.SEVERE, null, ex );
        }
        try {
            this.inputStream.close();
        } catch ( IOException ex ) {
            Logger.getLogger( HTTPDownload.class.getName() ).log( Level.SEVERE, null, ex );
        }
        try {
            this.clientSocket.close();
        } catch ( IOException ex ) {
            Logger.getLogger( HTTPDownload.class.getName() ).log( Level.SEVERE, null, ex );
        }
    }

    private void initSocket() throws UnknownHostException, IOException {
        System.out.println( "http trying to connecto to " + item.getHost() + ":" + item.getPort() );
        clientSocket = new Socket( item.getHost(), item.getPort() );
        clientSocket.setSoTimeout( 0 );
        System.out.println( "http connected and initiating handshake" );
        this.inputStream = new DataInputStream( clientSocket.getInputStream() );
        this.outputStream = new DataOutputStream( clientSocket.getOutputStream() );
    }

    String readUntilDourbleEndOfLine() throws IOException {
        StringBuffer buffer = new StringBuffer();
        String line = "-";

        while ( true ) {
            line = inputStream.readLine();
            //System.out.println( " line = '" + line +"'" );
            if ( line == null ) {
                return null;
            }
            if ( line.equals( "" ) ) {
                break;
            }
            buffer.append( line + "\n" );
        }
        return buffer.toString();
    }

    public String toString() {
        return this.id + " " + item.getFileName() + " from " + item.getHost() + ":" + item.getPort();
    }
}
    
    
                    
                
