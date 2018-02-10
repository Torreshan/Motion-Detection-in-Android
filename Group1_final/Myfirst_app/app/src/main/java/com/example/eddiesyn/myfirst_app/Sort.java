package com.example.eddiesyn.myfirst_app;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by eddiesyn on 18-2-1.
 */

public class Sort {
    public static ArrayList<Double> sort(ArrayList<Double> X){
        ArrayList<Double> Temp = new ArrayList<>(X.size());
        for(int i=0; i<X.size(); i++){
            Temp.add(Double.valueOf(0));
        }
        for(int i=0; i<X.size(); i++){
            Temp.set(i,X.get(i));
        }
        Collections.sort(Temp);

        return Temp;
    }

    public static ArrayList<Double> kmax(ArrayList<Double> X, int k){
        ArrayList<Double> Outcome = new ArrayList<>(X.subList(X.size()-k, X.size()));

        return Outcome;
    }

    public static Double Max(ArrayList<Double> X){
        return Collections.max(X);
    }

    public static Double Min(ArrayList<Double> X){
        return Collections.min(X);
    }
}
