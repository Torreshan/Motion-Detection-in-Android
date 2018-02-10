package com.example.eddiesyn.myfirst_app;

import java.util.ArrayList;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;

/**
 * Created by eddiesyn on 18-2-2.
 */

public class Dist {
//    public static double Mydist(ArrayList<Double> A1, double[][] a2){
//        double[] a1 = new double[A1.size()];
//        double dist = 10000.0;
//        double temp = 0.0;
//        for(int i=0; i<A1.size(); i++){
//            a1[i] = A1.get(i);
//        }
//
//        ArrayRealVector A = new ArrayRealVector(a1);
//        RealMatrix B1 = new Array2DRowRealMatrix(a2);
////        RealMatrix B2 = new Array2DRowRealMatrix(a3);
////        RealMatrix B3 = new Array2DRowRealMatrix(a4);
//        for(int i=0; i<8; i++){
//            temp = B1.getRowVector(i).getDistance(A);
//            if(temp < dist){
//                dist = temp;
//            }
//        }
//        return dist;
//    }
    public static double Mydist(ArrayList<Double> A1, double[] a2){
        double[] a1 = new double[A1.size()-1];
        double dist;
//        double temp = 0.0;
        for(int i=0; i<A1.size()-1; i++){
            a1[i] = A1.get(i);
        }
//        for(int i=0; i<A1.size()-1; i++){
//            dist += Math.pow((a1[i]-a2[i]) / 4, 2);
//        }
        dist = Math.pow((a1[0]-a2[0])/4, 2) + Math.pow(a1[1]-a1[1], 2);
//        ArrayRealVector A = new ArrayRealVector(a1);
//        ArrayRealVector B = new ArrayRealVector(a2);
//        dist = A.getDistance(B);
        return Math.sqrt(dist);
    }
    public static boolean Eucd(ArrayList<Double> A1, ArrayList<Double> A2){
        double dist1 = 0.0;
        double dist2 = 0.0;
        for(int i=0; i<A1.size(); i++){
            dist1 += Math.pow(A1.get(i), 2);
        }
        for(int i=0; i<A2.size(); i++){
            dist2 += Math.pow(A2.get(i), 2);
        }
        return Math.sqrt(dist1) > Math.sqrt(dist2);
    }


}
