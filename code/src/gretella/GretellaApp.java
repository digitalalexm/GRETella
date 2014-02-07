/*
 * GretellaApp.java
 */

package gretella;

import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;

/**
 * The main class of the application.
 */
public class GretellaApp extends SingleFrameApplication {

    /**
     * At startup create and show the main frame of the application.
     */
    @Override 
    protected void startup() {
        GretellaView view = new GretellaView(this);
        show( view );
        view.initView();        
    }

    /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */
    @Override protected void configureWindow(java.awt.Window root) {
    }

    /**
     * A convenient static getter for the application instance.
     * @return the instance of GretellaApp
     */
    public static GretellaApp getApplication() {
        return Application.getInstance(GretellaApp.class);
    }

    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {
        launch(GretellaApp.class, args);
    }
}
