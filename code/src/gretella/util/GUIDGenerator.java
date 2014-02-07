/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gretella.util;

import java.util.Random;

/**
 *
 * @author alexandros
 */
public class GUIDGenerator {

    static int[] values;
    static Random rand = new Random();

    public static void init() {
        rand.setSeed( System.currentTimeMillis() );
        values = new int[ 14 ];
        for ( int i = 0; i < values.length; i++ ) {
            values[i] = (byte) rand.nextInt();
        }
    }

    public static byte[] generate() {
        byte[] data = new byte[ 16 ];
        data[ 15] = 0;
        data[ 14] = (byte) rand.nextInt();
        data[ 13] = (byte) rand.nextInt();
        data[ 12] = (byte) rand.nextInt();
        data[ 11] = (byte) rand.nextInt();
        data[ 10] = (byte) rand.nextInt();
        data[ 9] = (byte) rand.nextInt();
        data[ 8] = (byte) 0xff;
        data[ 7] = (byte) rand.nextInt();
        data[ 6] = (byte) rand.nextInt();
        data[ 5] = (byte) rand.nextInt();
        data[ 4] = (byte) rand.nextInt();
        data[ 3] = (byte) rand.nextInt();
        data[ 2] = (byte) rand.nextInt();
        data[ 1] = (byte) rand.nextInt();
        data[ 0] = (byte) rand.nextInt();
        return data;
    }
}
