package com.example.eddiesyn.myfirst_app;

/**
 * Created by eddiesyn on 18-1-31.
 */

public class FFT {
    /** Be aware of the length of x is the power of 2; */
    public static Complex[] fft(Complex[] x) {
        int n = x.length;

        if (n == 1){
            return x;
        }

        if (n % 2 != 0){
            return dft(x);
        }

        Complex[] even = new Complex[n / 2];
        for (int k=0; k<n/2; k++){
            even[k] = x[2*k];
        }
        Complex[] evenValue = fft(even);

        Complex[] odd = even;
        for (int k=0; k<n/2; k++){
            odd[k] = x[2*k + 1];
        }
        Complex[] oddValue = fft(odd);

        Complex[] result = new Complex[n];
        for (int k=0; k<n/2; k++){
            double p = -2 * k * Math.PI / n;
            Complex m = new Complex(Math.cos(p), Math.sin(p));
            result[k] = evenValue[k].plus(m.times(oddValue[k]));
            result[k + n / 2] = evenValue[k].minus(m.times(oddValue[k]));
        }
        return result;
    }

    public static Complex[] dft(Complex[] x){
        int n = x.length;

        if(n == 1){
            return x;
        }

        Complex[] result = new Complex[n];
        for (int i=0; i<n; i++){
            result[i] = new Complex(0,0);
            for (int k=0; k<n; k++){
                double p = -2 * k * Math.PI / n;
                Complex m = new Complex(Math.cos(p), Math.sin(p));
                result[i].plus( x[k].times(m) );
            }
        }
        return result;
    }
}
