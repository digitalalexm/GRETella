/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gretella.util;

import gretella.GretellaView;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.LinkedList;

/**
 *
 * @author alexandros
 */
public class FileDB implements Serializable, Runnable {

    private LinkedList<FileIndex> indeces;
    private int totalSize;
    private File dir;
    private int initFolderCount;
    private transient GretellaView owner;

    public FileDB( GretellaView o ) {
        indeces = new LinkedList();
        totalSize = 0;
        this.owner = o;
    }

    public File getActualPath( int indexId, String fileName ) {
        for( FileIndex idx : this.indeces ){
            if( idx.getId() == indexId && idx.getFileName().equals( fileName ) == true )
                return new File( idx.getFilePath() );
        }
        return null;
    }

    public LinkedList<FileIndex> getIndeces() {
        return this.indeces;
    }

    public int getFileCount() {
        return indeces.size();
    }

    public boolean constract( String folder ) {
        File f = new File( folder );
        this.indeces.clear();
        this.totalSize = 0;
        //System.out.println( "folder = " + folder );
        if ( f.exists() == false || f.isDirectory() == false ) {
            return false;
        }
        dir = f;
        new Thread( this ).start();
        return true;
    }

    public int getTotalSize() {
        return this.totalSize;
    }

    public void run() {
        initFolderCount = dir.listFiles().length;
        getFiles( dir );
        owner.setIndexStatus( "<html><b>" + indeces.size() + " files. Total size: " + this.totalSize + " KB</b<<html>" );
    }

    public void setOwner( GretellaView o ) {
        this.owner = o;
    }

    private void getFiles( File f ) {
        File[] dirFiles = f.listFiles();
        for ( int i = 0; i < dirFiles.length; i++ ) {
            if ( dirFiles[i].isDirectory() ) {
                getFiles( dirFiles[i] );
            } else {
                if ( indeces.size() < Integer.MAX_VALUE ) {
                    indeces.add( new FileIndex( indeces.size(), dirFiles[i].getAbsolutePath() ) );
                    this.totalSize += dirFiles[i].length() / 1024;
                    owner.addFile( dirFiles[i].getName() + " path " + dirFiles[i].getAbsolutePath() );
                    owner.setIndexStatus( "<html><b>scanner has found " + indeces.size() + " until now</b<<html>" );
                }
            }
        }        
    }

    public void printFiles() {
        owner.clearSharing();
        System.out.println( "size of index is " + this.indeces.size() );
        for ( FileIndex index : this.indeces ) {
            System.out.println( index.getFileName() + " path " + index.getFilePath() );
            owner.addFile( index.getFileName() + " path " + index.getFilePath() );
        }
        owner.setIndexStatus( "<html><b>" + indeces.size() + " files. Total size: " + this.totalSize + " KB</b<<html>" );
    }

    public int getInitFolderCount() {
        return initFolderCount;
    }

    public void store( String path ) throws IOException {
        File f = new File( path );
        if ( f.exists() == false ) {
            f.createNewFile();
        } else {
            f.delete();
            f.createNewFile();
        }
        ObjectOutputStream fstream = new ObjectOutputStream( new FileOutputStream( f ) );
        fstream.writeObject( this );
        fstream.close();
    }

    public static FileDB load( String path ) throws IOException, ClassNotFoundException {
        File f = new File( path );
        if ( f.exists() == false ) {
            return null;
        }
        ObjectInputStream fstream = new ObjectInputStream( new FileInputStream( f ) );

        FileDB obj = (FileDB) fstream.readObject();

        fstream.close();

        return obj;
    }
    
    public LinkedList<FileIndex> search( LinkedList<String> queries ) {
        LinkedList<FileIndex> results = new LinkedList();
        
        for( FileIndex index : this.indeces ){
            for( String q : queries ) {
                if( index.getFileName().contains( q )){
                    results.add( index );
                    break;
                }
            }
        }
        
        return results;
    }
}
