/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gretella;

/**
 *
 * @author alexandros
 */
public class Searcher implements Runnable {

    private GretellaView owner;
    
    public Searcher( GretellaView owner ){
        this.owner = owner;
    }
    
    public void search(){        
        new Thread( this ).start();
    }
    
    public void run() {
        owner.search();
    }
    

}
