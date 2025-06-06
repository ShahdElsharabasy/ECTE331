// For accurate measurements comparison, take the average of three executions; after the first run 


public class MyClass {
 
    public static void main(String args[]) {
        // range of random values
    	int min_rnd = 1;  
        int max_rnd = 100;  
        
        int scale=17;
        // size of the array
        int size=80010;
        // initial size of the array
        int num_threads0=10;
        // real size of the array if size is not a multiple of num_thread0
        int num_threads;
        
        int min_index=0;
        int max_index;
        
        
        Timer timer=new Timer();
        
        if(size % num_threads0!=0)
          num_threads=num_threads0+1;
          else
        num_threads=num_threads0;
          
        int data[]=new int[size];
        // output from single threads implementation
        int output_sth[]=new int[size];
        // output from multi-threads implementation
        int output_mth[]=new int[size];
        
        // initialisation of the data
        for (int i=0; i<size;i++){
            data[i]=(int)(Math.random()*(max_rnd-min_rnd+1)+min_rnd); 
            }
        
        // scaling of data 
        timer.resetTimer();
        Thread th=new Thread(new scale_thread(data,output_sth,scale,0,size-1));
        th.start();
           
         try{
             th.join();
         }
         catch(InterruptedException e){
             e.printStackTrace();
         }
        double sth_duration=timer.elapsedTime();
       
         
        int num_samples_per_thread=size/num_threads0;
        max_index=num_samples_per_thread-1;
        
        
        timer.resetTimer();
          Thread thread_array[]=new Thread[num_threads];
          
        for(int i=0;i< thread_array.length-1;i++){
           thread_array[i]=new Thread(new scale_thread(data,output_mth,scale,min_index,max_index));
           thread_array[i].start();
           min_index+=num_samples_per_thread;
           max_index+=num_samples_per_thread;
           
        }
       System.out.println("The last range [min_index,max_index]= "+min_index +" "+(size-1) ); 
       thread_array[thread_array.length-1]=new Thread(new scale_thread(data,output_mth,scale,min_index,size-1));
       thread_array[thread_array.length-1].start();
        
        for(int i=0; i<thread_array.length;i++){
            try{
                thread_array[i].join();
            }
            catch(InterruptedException e){
                e.printStackTrace();
            }
        }
        
         double mth_duration=timer.elapsedTime();  
        
        System.out.println(" Are the results of Single and multithread implementation equal: "+ isArraysEqual(output_sth, output_mth));
        
        System.out.println(" Duration of SingleThread Implementation "+ sth_duration);
        System.out.println(" Duration of MultiThread Implementation "+ mth_duration);
       
    }
    
    public static boolean isArraysEqual(int data1 [], int data2[]){
        if (data1.length!=data2.length) {System.out.println(" size of difference size"); return false;};
        for(int i=0; i<data1.length;i++)
           if(data1[i]!=data2[i]) {System.out.println(i);return false;}
         
         return true;  
    }
    
    public static void scaleArray(int input[], int output[], int scale){
        for(int i=0; i<input.length; i++){
            output[i]=input[i]*scale;
        }
    }
    
    
    
}


class scale_thread implements Runnable{
    private int array[];
    private int scale;
    private int minIndex, maxIndex;
    public int output [];
    public scale_thread(int array [], int output[], int scale, int minIndex, int maxIndex ){
        this.array=array;
        this.scale=scale;
        this.minIndex=minIndex;
        this.maxIndex=maxIndex;
        this.output=output;
    }
    public void run(){
        for (int i=minIndex;i<=maxIndex; i++)
        output[i]=array[i]*scale;
    }
}



class Timer{
private double savedTime;
public Timer(){
resetTimer();
}
public void resetTimer(){
savedTime=System.nanoTime();
}

public double elapsedTime(){
double eTime;
eTime=(System.nanoTime()-savedTime)/1000000;
return eTime;
}
}