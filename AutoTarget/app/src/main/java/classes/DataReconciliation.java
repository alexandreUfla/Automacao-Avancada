package classes;

import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

public class DataReconciliation {
    
    /**
     * Aplica a reconciliação de dados na medição de distâncias.
     * Formula: y_hat = y - V * A^T * (A * V * A^T)^-1 * A * y
     */
    public static double[] reconcile(double[] y_arr, double[][] V_arr, double[][] A_arr) {
        try {
            RealVector y = MatrixUtils.createRealVector(y_arr);
            RealMatrix V = MatrixUtils.createRealMatrix(V_arr);
            RealMatrix A = MatrixUtils.createRealMatrix(A_arr);

            RealMatrix AT = A.transpose();
            RealMatrix AVAT = A.multiply(V).multiply(AT);

            // Inversão da matriz usando decomposição LU
            RealMatrix invAVAT = new LUDecomposition(AVAT).getSolver().getInverse();

            // Calculando a correção: V * A^T * (A*V*A^T)^-1 * A * y
            RealVector correction = V.multiply(AT).multiply(invAVAT).multiply(A).operate(y);

            // y_hat = y - correction
            RealVector y_hat = y.subtract(correction);
            
            return y_hat.toArray();
        } catch (Exception e) {
            // Em caso de matrizes singulares ou erros dimensionais (ruído zero, etc)
            e.printStackTrace();
            return y_arr; // fallback para as medidas originais
        }
    }
}
