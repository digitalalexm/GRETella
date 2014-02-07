/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gretella.util;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

/**
 *
 * @author alexandros
 */
public class IncompleteFile implements Serializable {
    private String fileName;
    private int totalSize;
    private int downlodedSize;
    private File file;
    
    public IncompleteFile(){
        this.fileName = null;
        file = null;
    }
    
    public IncompleteFile( String name ) throws IOException{
        this.fileName = name;
        this.file = new File( name );
        if ( file.exists() == false ) {
            file.mkdirs();
            file.createNewFile();
        }
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName( String fileName ) {
        this.fileName = fileName;
    }

    public int getTotalSize() {
        return totalSize;
    }

    public void setTotalSize( int totalSize ) {
        this.totalSize = totalSize;
    }

    public int getDownlodedSize() {
        return downlodedSize;
    }

    public void setDownlodedSize( int downloded ) {
        this.downlodedSize = downloded;
    }
}
