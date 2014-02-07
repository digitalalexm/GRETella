/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gretella.util;

import java.io.File;
import java.io.Serializable;

/**
 *
 * @author alexandros
 */
public class FileIndex implements Serializable{
    private int id;
    private String fileName;
    private String filePath;
    private long fileSize;

    public FileIndex() {
        id = 0;
        fileName = filePath = null;
        fileSize = 0;
    }
    
    public FileIndex( int id, String filePath ) {
        this.id = id;
        this.filePath = filePath;
        
        File f = new File( filePath );
        fileName = f.getName();
        this.fileSize = f.length() / 1024;
    }
    
    public int getId() {
        return id;
    }

    public void setId( int id ) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName( String fileName ) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath( String filePath ) {
        this.filePath = filePath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize( long fileSize ) {
        this.fileSize = fileSize;
    }
    
    
}
