package com.example.eddiesyn.myfirst_app;

import java.util.ArrayList;
import java.util.Arrays;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.moment.GeometricMean;
import org.apache.commons.math3.stat.descriptive.moment.Kurtosis;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.Skewness;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.descriptive.moment.Variance;
import org.apache.commons.math3.stat.descriptive.rank.Max;
import org.apache.commons.math3.stat.descriptive.rank.Min;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.apache.commons.math3.stat.descriptive.summary.Product;
import org.apache.commons.math3.stat.descriptive.summary.Sum;

import static java.lang.Math.abs;

/**
 * Created by eddiesyn on 18-2-1.
 */

public class FeatureExtraction {
    public static ArrayList<Double> magnitude(ArrayList<Double> AX, ArrayList<Double> AY, ArrayList<Double> AZ){
        int n = AX.size();
        ArrayList<Double> A = new ArrayList<Double>(n);
        for(int i=0; i<n; i++){
            A.add(Double.valueOf(0));
        }

        for(int i=0; i<n; i++){
            A.set(i, Math.sqrt(Math.pow(AX.get(i), 2) + Math.pow(AY.get(i), 2) + Math.pow(AZ.get(i), 2)) );
        }

        return A;
    }

    public static ArrayList<Double> Smooth(ArrayList<Double> Dataseries){
        ArrayList<Double> SmoothData = new ArrayList<>();

        int size=Dataseries.size();
        Double[] in = (Double[])Dataseries.toArray(new Double[size]);
        Double[] out = new Double[size];
        out[0] = ( 3.0 * in[0] + 2.0 * in[1] + in[2] - in[4] ) / 5.0;
        out[1] = ( 4.0 * in[0] + 3.0 * in[1] + 2 * in[2] + in[3] ) / 10.0;
        for (int i = 2; i <= size - 3; i++ )
        {
            out[i] = ( in[i - 2] + in[i - 1] + in[i] + in[i + 1] + in[i + 2] ) / 5.0;
        }
        out[size - 2] = ( 4.0 * in[size - 1] + 3.0 * in[size - 2] + 2 * in[size - 3] + in[size - 4] ) / 10.0;
        out[size - 1] = ( 3.0 * in[size - 1] + 2.0 * in[size - 2] + in[size - 3] - in[size - 5] ) / 5.0;
        SmoothData.addAll(Arrays.asList(out));

        return SmoothData;

    }




    public static ArrayList<Double> Feature(ArrayList<Double> X){
//        Complex[] S = new Complex[X.size()];
        ArrayList<Double> Outcome = new ArrayList<>(2);
        for(int i=0; i<3; i++){
            Outcome.add(Double.valueOf(0));
        }
        double[] x = new double[X.size()];
        for(int i=0; i<X.size(); i++){
            x[i] = abs( X.get(i));
        }

//        for(int i=0; i<X.size(); i++){
//            S[i] = new Complex(i,0);
//            S[i] = new Complex(X.get(i), 0);
//        }

//        Complex[] Out = FFT.fft(S);
//        ArrayList<Double> ABS = new ArrayList<>(X.size());
//        for(int i=0; i<X.size(); i++){
//            ABS.add(Double.valueOf(0));
//        }
//        for(int i=0; i<X.size(); i++){
//            ABS.set(i, Out[i].abs());
//        }
//
//        ArrayList<Double> TEMP = new ArrayList<>(ABS.size());
//        //for(int i=0; i<ABS.size(); i++){
//        //    TEMP.add(Double.valueOf(0));
//        //}
//        TEMP.addAll(Sort.sort(ABS));
//        Double F = 0.0;
//        for(int i=0; i<3; i++){
//            F += (double)(ABS.indexOf(Sort.kmax(TEMP, 3).get(i))-1) / ABS.size() * 150;
//        }
//
//        F /= 3;
//
//        Outcome.set(1,F);
        Outcome.set(0,StatUtils.max( x ));
        StandardDeviation std = new StandardDeviation();
        Skewness skw = new Skewness();
        Outcome.set(1,std.evaluate(x));
        Outcome.set(2, skw.evaluate(x));
//        double[] MyTemp = new double[X.size()];
//        for(int i=0; i<6; i++){
//            MyTemp[i] = (double) X.get(i);
//        }
//
//
//        Double sum = 0.0;
//        for(int i=0; i<X.size(); i++){
//            sum += X.get(i);
//        }
//
//        Skewness skewness = new Skewness();
//        Kurtosis kurtosis = new Kurtosis();
//        Outcome.set(1, kurtosis.evaluate(MyTemp));
//        Outcome.set(2, skewness.evaluate(MyTemp));
//        Outcome.set(3, sum / X.size());
//        Outcome.set(4,StatUtils.max( MyTemp ));
//        Outcome.set(5,StatUtils.min( MyTemp ));

        return Outcome;
    }

    public static double Mean(ArrayList<Double> X){
        double Mean = 0.0;
        for(int i=0; i<X.size(); i++){
            Mean += X.get(i);
        }
        return Mean/X.size();
    }
}
