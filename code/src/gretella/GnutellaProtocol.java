/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gretella;

import gretella.util.GnutellaMessage;

/**
 *
 * @author alexandros
 */
public class GnutellaProtocol {

    public static final String INIT_MSG = "GNUTELLA CONNECT/0.6\n" +
            "User-Agent: Gretella/0.10\n" +
            //"User-Agent: LimeWire/4.16.6\n" +
            //"X-Locale-Pref: en\n" +
            "X-Requeries: false\n" +
            //"X-Version: 4.9\n" +
            //"Listen-IP: 217.254.98.153:6346\n" +
            //"Remote-IP: 68.37.233.44\n" +
            //"Accept-Encoding: deflate\n" +
            "X-Ultrapeer: true\n" +
            "X-Degree: 32\n" +
            "X-Query-Routing: 0.1\n" +
            "X-Ultrapeer-Query-Routing: 0.1\n" +
            "X-Dynamic-Querying: 0.1\n" +
            "X-Ext-Probes: 0.1\n" +
            "Vendor-Message: false\n" +
            //"GGEP: false\n" +
            //"Pong-Caching: 0.1\n" +
            "X-Max-TTL: " + GnutellaMessage.DEFAULT_TTL + "\n" +
            //"X-Guess: 0.1\n" +
            "\n";
    public static final String CLASIC_GNUTELLA_OK = "GNUTELLA/0.6 200 OK\n" +
            "\n";
    public static final String GNUTELLA_OK = "GNUTELLA/0.6 200 OK\n" +
            "User-Agent: Gretella/0.10\n" +
            //"User-Agent: LimeWire/4.16.6\n" +
            //"X-Locale-Pref: en\n" +
            "X-Requeries: false\n" +
            //"X-Version: 4.9\n" +
            //"Listen-IP: 217.254.98.153:6346\n" +
            //"Remote-IP: 68.37.233.44\n" +
            //"Accept-Encoding: deflate\n" +
            "X-Ultrapeer: true\n" +
            "X-Degree: 32\n" +
            "X-Query-Routing: 0.1\n" +
            "X-Ultrapeer-Query-Routing: 0.1\n" +
            "X-Dynamic-Querying: 0.1\n" +
            "X-Ext-Probes: 0.1\n" +
            "Vendor-Message: false\n" +
            //"GGEP: false\n" +
            //"Pong-Caching: 0.1\n" +
            "X-Max-TTL: " + GnutellaMessage.DEFAULT_TTL + "\n" +
            //"X-Guess: 0.1\n" +
            "\n";
    public static final String BUSY = "GNUTELLA/0.6 503 Busy\n\n";
    public static final String CRITICAL_INIT_MSG = "GNUTELLA CONNECT/0.6";
    //public static final String INIT_MSG = "GNUTELLA CONNECT/0.6\n\n";    
    //public static final String USER_AGENT = "User-Agent: LimeWire/4.14.0/1.0\n\n\n";
    //public static final String Pong_Caching = "Pong-Caching: 0.1\n";    
}
