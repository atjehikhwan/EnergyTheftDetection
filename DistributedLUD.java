/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package distributedlud;

/**
 *
 * @author aditya
 */
import java.util.*;

public class DistributedLUD {
    
    final static int sampling_time = 2000;
    
    public static void main(String[] args) {
        Scanner stdIn = new Scanner(System.in);
        int n = Integer.parseInt(stdIn.next());
        double[] aggregator_readings = new double[n];
        double[] honesty_coefficients = new double[n];
        SmartMeter[] smartMeters = new SmartMeter[n];
        for(int i=0;i<n;i++)
            smartMeters[i] = new SmartMeter(n, i);
        long startTime = System.currentTimeMillis();
        Random random = new Random();
        for(int i=0;i<n;i++){
            double sum = 0;
            for(int j=0;j<n;j++){
                int rand_num = random.nextInt(40)+1;
                smartMeters[j].meter_readings[i] = rand_num;
                sum+=(rand_num*smartMeters[j].getTheftRate());
            }
            aggregator_readings[i] = sum;
            System.out.println("T"+i);
            if(i<n-1)
                try{Thread.sleep(sampling_time);}catch(Exception exception){System.out.println(exception);};
        }
        
        /*for(int i=0;i<n;i++)
            aggregator_readings[i] = Double.parseDouble(stdIn.next());
        
        for(int i=0;i<n;i++){
            double[] readings = new double[n];
            for(int j=0;j<n;j++)
                readings[j] = Double.parseDouble(stdIn.next());
            smartMeters[i] = new SmartMeter(readings,n,i);
        }*/
//        for(int i=0;i<n;i++){
//            aggregator_readings[i] = 0.0;
//            for(int j=0;j<n;j++)
//                aggregator_readings[i]+=(smartMeters[j].meter_readings[i]*smartMeters[j].getTheftRate());
//        }
        
        distributedDecomposition(smartMeters, 0, n, null, null, null, aggregator_readings);
        backwardSubstitution(smartMeters, n-1, n, 0, null, honesty_coefficients);
        System.out.println("\n\n");
        for(int i=0;i<n;i++){
            if(honesty_coefficients[i]>0.9 && honesty_coefficients[i]<1.1)
                honesty_coefficients[i] = 1.0;
            System.out.println("SM: "+smartMeters[i].id+"\t\tHC: "+(double)Math.round(honesty_coefficients[i]*100)/100+"\t\tTR: "+(double)Math.round(smartMeters[i].getTheftRate()*100)/100);
        }
        long endTime   = System.currentTimeMillis();
        double totalTime = (endTime - startTime)/60000.0;
        System.out.println("\nRun-Time: "+(double)Math.round(totalTime*100)/100+"min.");
    }
    
    public static void distributedDecomposition(SmartMeter[] smartMeters, int id, int n, double[][] l, double[][] u, ArrayList<Double> y, double[] aggregator_readings){
        if(id==n)
            return;
        for(int i=0;i<=id;i++){
            smartMeters[id].L[i] = new double[n];
            smartMeters[id].U[i] = new double[n];
        }
        for(int i=0;i<id;i++){
            smartMeters[id].L[i] = l[i];
            smartMeters[id].U[i] = u[i];
        }
        smartMeters[id].U[id][0] = smartMeters[id].meter_readings[0];
        for(int i=1;i<n;i++){
            if(i>id)
                smartMeters[id].U[id][i] = 0;
            else{
                double sum = 0.0;
                for(int j=0;j<i;j++)
                    sum+=(smartMeters[id].L[j][i]*smartMeters[id].U[id][j]);
                smartMeters[id].U[id][i] = smartMeters[id].meter_readings[i]-sum;
            }
        }
        if(id==0){
            smartMeters[id].Y.add(aggregator_readings[id]);
            for(int i=0;i<n;i++)
                smartMeters[id].L[id][i] = smartMeters[id].meter_readings[i]/smartMeters[id].meter_readings[0];
        }
        else{
//            for(int i=0;i<id;i++){
//                for(int j=0;j<n;j++)
//                    System.out.print(l[i][j]+" ");
//            }
//            System.out.println();
//            for(int i=0;i<id;i++){
//                for(int j=0;j<n;j++)
//                    System.out.print(u[i][j]+" ");
//            }
//            System.out.println();
            smartMeters[id].Y = y;
            for(int i=0;i<n;i++){
                if(i<id)
                    smartMeters[id].L[id][i] = 0.0;
                else{
                    double sum = 0.0;
                    for(int j=0;j<id;j++)
                        sum+=(smartMeters[id].L[j][i]*smartMeters[id].U[id][j]);
                    smartMeters[id].L[id][i] = (smartMeters[id].meter_readings[i]-sum)/smartMeters[id].U[id][id];
                }
            }
            double sum = 0.0;
            for(int j=0;j<id;j++)
                sum+=(smartMeters[id].L[j][id]*smartMeters[id].Y.get(j));
            smartMeters[id].Y.add(aggregator_readings[id]-sum);
        }
        //System.out.println(smartMeters[id].Y.toString());
        distributedDecomposition(smartMeters, id+1, n, smartMeters[id].L, smartMeters[id].U, smartMeters[id].Y, aggregator_readings);
    }
    
    public static void backwardSubstitution(SmartMeter[] smartMeters, int id, int n, double sum, double[][] s, double[] honesty_coefficients){
        for(int i=n-1;i>=id;i--)
            smartMeters[id].S[i] = new double[n];
        for(int i=n-1;i>id;i--)
            smartMeters[id].S[i] = s[i];
            
        if(id==n-1)
            honesty_coefficients[id] = smartMeters[id].Y.get(id)/smartMeters[id].U[id][id];
        else
            honesty_coefficients[id] = (smartMeters[id].Y.get(id)-sum)/smartMeters[id].U[id][id];
        
        for(int i=0;i<n;i++)
            smartMeters[id].S[id][i] = smartMeters[id].U[id][i]*honesty_coefficients[id];
        
        sum=0;
        if(id>0){
            for(int i=id;i<n;i++)
                sum+=smartMeters[id].S[i][id-1];
            //System.out.println(honesty_coefficients[id]);

            backwardSubstitution(smartMeters, id-1, n, sum, smartMeters[id].S, honesty_coefficients);
        }
    }
}

class SmartMeter {
    int id;
    double[] meter_readings;
    double[][] L;
    double[][] U;
    double[][] S;
    ArrayList<Double> Y;
    final double theft_probability = 0.3;
    private double theft_rate;
    final double theft_rate_low = 1.1;
    final double theft_rate_high = 10;
    
    public SmartMeter(int n, int id){
        this.id = id;
        this.meter_readings = new double[n];
        this.L = new double[n][];
        this.U = new double[n][];
        this.Y = new ArrayList<>();
        this.S = new double[n][];
        Random r = new Random();
        double probability = r.nextDouble();
        if(probability <= this.theft_probability)
            theft_rate = this.theft_rate_low + (this.theft_rate_high-this.theft_rate_low)*r.nextDouble();
        else{
            theft_rate = 1.0;
        }
    }
    
    public double getTheftRate(){
        return this.theft_rate;
    }
}
