/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gretella.util;

/**
 *
 * @author alexandros
 */
public class SearchItem {
    
    private String fullFileName;
    private String fileName;
    private long fileIndex;
    private String host;
    private int port;
    private long fileSize;
    
    public SearchItem( String fullFilename, String fileName, long fileSize, long fileIndex, String host, int port ){
        this.fileName = fileName;
        this.fileIndex = fileIndex;
        this.host = host;
        this.port = port;
        this.fileSize = fileSize;     
        this.fullFileName = fullFilename;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName( String fileName ) {
        this.fileName = fileName;
    }

    public long getFileIndex() {
        return fileIndex;
    }

    public void setFileIndex( int fileIndex ) {
        this.fileIndex = fileIndex;
    }

    public String getHost() {
        return host;
    }

    public void setHost( String host ) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort( int port ) {
        this.port = port;
    }
    
    @Override
    public String toString(){
        return this.getFileName() + " size: " + this.fileSize + "KB host " + this.host + " port " + this.port;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize( long fileSize ) {
        this.fileSize = fileSize;
    }

    public String getFullFileName() {
        return fullFileName;
    }

    public void setFullFileName( String fullFileName ) {
        this.fullFileName = fullFileName;
    }
}
