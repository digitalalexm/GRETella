//===================================================================
// Preferences
//
// A general-puprose class
//
// Keeps persistent options of the application.
// Default values for options (used in case preferences file is
// not created yet) can be set in method setDef().
//===================================================================
package gretella.util;

import java.io.*;
import java.util.*;

public class GretellaPreferences {

    private String file;  //the full pathname of file

    private String header;  //header in file
    //program options that persist

    private Properties defPref = new Properties();      //default values

    private Properties pref = new Properties();  //user defined values


    public GretellaPreferences( String file, String header ) {
        this.file = ( new File( file ) ).getAbsolutePath();  //convert to absolute path

        this.header = header;
        setDef();
        pref.clear();
        load();
    }
    //set default values

    private void setDef() {
        defPref.clear();
        //default values of WebServer class variables
        defPref.put( "port", "36529" );
        defPref.put( "connections", "10" );
        defPref.put( "folder", "." + File.separator + "downloads" + File.separator );
        defPref.put( "dspeed", "200" );
        defPref.put( "uspeed", "10" );
        defPref.put( "id", new String ( GUIDGenerator.generate() ) );
    }

    public byte[] getId(){
        return pref.getProperty( "id" ).getBytes();
    }
    
    public String getSharingFolder() {
        return pref.getProperty( "folder" );
    }

    public void setSharingFolder( String path ){
        pref.put( "folder", path );
        //System.out.println( "pref path = " + pref.getProperty( "folder" ) );
    }
    
    public int getPort(){
        return Integer.parseInt( pref.getProperty( "port" ) );
    }
    
    public void setPort( int val ){
        pref.put( "port", "" + val );
    }
    
    public int getMaximumConnections() {
        return Integer.parseInt( pref.getProperty( "connections" ) );
    }

    public void setMaximumConnections( int val ){
        pref.put( "connections", "" + val );
    }
    
    public int getUpstream() {
        return Integer.parseInt( pref.getProperty( "uspeed" ) );
    }

    public void setUpstream( int val ){
        pref.put( "uspeed", "" + val );
    }
    
    public int getDownstream() {
        return Integer.parseInt( pref.getProperty( "dspeed" ) );
    }

    public void setDownstream( int val ){
        pref.put( "dspeed", "" + val );
    }
    
    //get and set
    public void setPref( String name, String val ) {
        pref.put( name, val );
    }

    public String getPref( String name ) {
        return pref.getProperty( name );  //null if not there

    }

    public String[] getProperties() {
        Enumeration e = pref.propertyNames();
        Vector elements = new Vector();
        while ( e.hasMoreElements() ) {
            elements.addElement( e.nextElement() );
        }
        return (String[]) elements.toArray( new String[ 0 ] );
    }

    public String[] getDefaultProperties() {
        Enumeration e = defPref.propertyNames();
        Vector elements = new Vector();
        while ( e.hasMoreElements() ) {
            elements.addElement( e.nextElement() );
        }
        return (String[]) elements.toArray( new String[ 0 ] );
    }
    //load and save

    public void load() {
        try {
            FileInputStream in = new FileInputStream( file );
            pref.load( in );
            String[] properties = getProperties();
            String[] defProperties = getDefaultProperties();
            if ( properties.length != defProperties.length ) {
                throw new Exception();
            }
            for ( int i = 0; i < properties.length; i++ ) {
                boolean found = false;
                for ( int j = 0; i < defProperties.length; j++ ) {
                    if ( properties[i].equals( defProperties[j] ) ) {
                        found = true;
                        break;
                    }
                }
                if ( found == false ) {
                    throw new Exception();
                }
            }
            System.out.println("File loaded!!");
        } catch ( Exception e ) {
            System.out.println("Configuration file had bad entries, a new one will be created");
            //the first time, preferences file will not exist
            pref = defPref;
            store();
        }
    }

    public void store() {
        try {
            FileOutputStream out = new FileOutputStream( file );
            pref.store( out, header );
            out.close();
           System.out.println("Preferenses saved");
        } catch ( IOException e ) {
            System.out.println( "Unable to store user preferences in " + file + ": " + e );
        }
    }
    //it does the same thing as the previous but it does not show a message dialog

    public boolean silentStore() {
        try {
            FileOutputStream out = new FileOutputStream( file );
            pref.store( out, header );
            return true;
        } catch ( IOException e ) {
            return false;
        }
    }
}

