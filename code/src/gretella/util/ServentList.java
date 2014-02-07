/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gretella.util;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Vector;
import java.util.StringTokenizer;

/**
 *
 * @author alexandros
 */
public class ServentList {

    private Vector<GnutellaServent> hosts;
    String file;
                
    public ServentList( String fileName ) throws IOException {
        hosts = new Vector( 20, 20 );
        this.file = fileName;
        try {
            FileInputStream fstream = new FileInputStream( fileName );
            DataInputStream in = new DataInputStream( fstream );
            BufferedReader br = new BufferedReader( new InputStreamReader( in ) );
            String strLine;            
            while ( ( strLine = br.readLine() ) != null ) {
                StringTokenizer tok = new StringTokenizer( strLine );
                GnutellaServent host = new GnutellaServent();
                host.setHostName( tok.nextToken( " " ) );
                host.setPort( Integer.parseInt( tok.nextToken() ) );
                hosts.add( host );
            }
            in.close();
        } catch ( Exception e ) {//Catch exception if any
            System.err.println( "Wraning:Unable to read the host file!!!" + e.getMessage() );
            System.err.println( "Loading deafault" + e.getMessage() );
            loadDefault();
            store();
        }
        /*System.out.println( "host num " + hosts.size() );
        for( INetHost host: hosts ){
            System.out.println( host.getHostName() + ":" + host.getPort() );
        }
        System.out.println( "end of host initial list" + hosts.size() );*/
    }

    public void setHosts( Vector<GnutellaServent> hosts ) {
        this.hosts = hosts;
    }
    
    private void loadDefault() {
        getHosts().add( new GnutellaServent( "gnutellahosts.com", 6346 ) );
        getHosts().add( new GnutellaServent( "nova.yi.org", 6346 ) );
    }

    public void store() throws IOException {
        File f = new File( file );
        if( f.exists() == false ){
            f.createNewFile();
        }
        FileOutputStream fstream = new FileOutputStream( f );
        PrintWriter br = new PrintWriter( fstream );
        for( GnutellaServent host: getHosts() ){
            br.write( host.getHostName() + " " + host.getPort() + "\r\n" );
        }
        br.close();
        fstream.close();        
    }

    public

    Vector<GnutellaServent> getHosts() {
        return hosts;
    }    
}
