/*
 * MathUtils.java
 *
 * Created on February 19, 2006, 1:13 AM
 *
 */

package ika.utils;

/**
 *
 * @author jenny
 */
public class MathUtils {
    
    public static final int binomialCoeff(int n, int k) {
        return MathUtils.fact(n) / (MathUtils.fact(k) * MathUtils.fact(n-k));
    }
    
    public static final int fact(int m) {
        int fact = 1;
        for (int i = 2; i <= m; i++)
            fact *= i;
        return fact;
    }

    /**
     * Since 1.5 there is Math.log10()
     * @param x
     * @return
     */
    public static final double log10 (double x) {
        return Math.log(x)/Math.log(10.);
    }

    public static final double log2 (double x) {
        return Math.log(x)/Math.log(2.);
    }
    
    public static boolean numbersAreClose (double x, double y) {
        final double TOL = 0.000000001;
        return MathUtils.numbersAreClose (x, y, TOL);
    }
    public static boolean numbersAreClose(double x, double y, double tolerance) {
        return (Math.abs(x-y) < tolerance);
    }
    
    /** Numbers are signed in Java. This converts an unsigned byte to an int.*/
    public static int unsignedByteToInt(byte b) {
        return (int) b & 0xFF;
    }
}
