/*
 * File: LinRegression 
 * Created: 10:04:10 05.12.2014
 * Encoding: UTF-8
 */

package de.uros.citlab.module.util;

import com.achteck.misc.log.Logger;
import de.planet.math.util.MatrixUtil;

/**
 * Describtion of LinRegression: 
 *
 * @author Tobias Grüning <tobias.gruening@uni-rostock.de>
 */
public class LinRegression {

    public static double[] calcLine(int[] xPoints, int[] yPoints) {
        int dimA = xPoints.length;
        double minX = 10000;
        double maxX = 0;
        double sumX = 0.0;
        double[][] A = new double[dimA][2];
        double[] Y = new double[dimA];
        boolean notReached = true;
        int actInd = 0;
        int runInd = 0;
        for (int i = 0; i < dimA; i++) {
            double[] rowI = A[i];
            int actPx = xPoints[i];
            int actPy = yPoints[i];
            rowI[0] = 1.0;
            rowI[1] = actPx;
            minX = Math.min(minX, actPx);
            maxX = Math.max(maxX, actPx);
            sumX += actPx;
            Y[i] = actPy;
        }
        if (maxX - minX < 2) {
            return new double[]{sumX / dimA, Double.POSITIVE_INFINITY};
        }

        return solveLin(A, Y);
    }

    public static double[] solveLin(double[][] mat1, double[] Y) {
        double[][] mat1T = MatrixUtil.transpose(mat1);
        double[][] multLS = MatrixUtil.multiply(mat1T, mat1);
        double[] multRS = MatrixUtil.multiply(mat1T, Y);
        double[][] inv = null;
        if (multLS.length != 2) {
            Logger.getLogger(LinRegression.class.getName()).log(Logger.ERROR, "Matrix not 2x2");
        } else {
            inv = new double[2][2];
            double n = (multLS[0][0] * multLS[1][1] - multLS[0][1] * multLS[1][0]);
//            if (n < 1E-7) {
//                return new double[]{mat1[0][1], Double.POSITIVE_INFINITY};
//            }
            double fac = 1.0 / n;
            inv[0][0] = fac * multLS[1][1];
            inv[1][1] = fac * multLS[0][0];
            inv[1][0] = -fac * multLS[1][0];
            inv[0][1] = -fac * multLS[0][1];
        }
        double[] res = MatrixUtil.multiply(inv, multRS);
        return res;
    }
    
    
} 
