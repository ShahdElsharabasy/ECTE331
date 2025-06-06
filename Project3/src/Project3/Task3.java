package Project3;


public class Task3 {

	public static void main(String[] args) {

		int iterations = 5;
		
		for(int i = 0; i < iterations; i++){
			System.out.println("\n Iteration " + (i + 1) + " \n");
			Data data = new Data();
			ThreadA threadA = new ThreadA(data);
			threadA.start();	
			ThreadB threadB = new ThreadB(data);
			threadB.start();
			ThreadC threadC = new ThreadC(data);
			threadC.start();
			
			try{
				threadA.join();
				threadB.join();
				threadC.join();
			}
			catch(InterruptedException e){
				e.printStackTrace();
			}
		}

	}

	static class Utility{
		public static int calculate(int n) {
			int sum=0;
			for(int i=1;i<=n;i++){
				sum+=i;
			}
			return sum;
		}

	}

	static class Data{
		int A1,B1,A2,B2,A3,B3;
		boolean gotoA2,gotoB2,gotoA3,gotoB3, gotoC;
		
		
	}

	static class ThreadA extends Thread{
		private Data data;

		public ThreadA(Data data){
			super();
			this.data = data;
		}

		public void run(){
			//function 1 block
			synchronized(data){
				data.A1 = Utility.calculate(500);
				System.out.println("A1 Completed " + data.A1);
				data.gotoB2 = true;
				data.notifyAll();
			}
			//function 2 block
			synchronized(data){
				try{	
					System.out.println("A2 waiting for B2's notification");
					while(!data.gotoA2){
						data.wait();
					}
					data.A2 = data.B2+ Utility.calculate(300);
					System.out.println("A2 Completed " + data.A2);
					data.gotoB3 = true;
					data.notifyAll();
				}
				catch(InterruptedException e){
					e.printStackTrace();
				}
			}
			//function 3 block
			synchronized(data){
				try{
					System.out.println("A3 waiting for B3's notification");
					while(!data.gotoA3){
						data.wait();
					}
					data.A3 = data.B3+ Utility.calculate(400);
					System.out.println("A3 Completed " + data.A3);
					//data.gotoA3 = true;
					//data.notify();
				}
				catch(InterruptedException e){
					e.printStackTrace();
				}
			}
		}
	}

	static class ThreadB extends Thread{
		private Data data;

		public ThreadB(Data data){
			super();
			this.data = data;
		}

		public void run(){
			synchronized(data){
				data.B1 = Utility.calculate(250);
				System.out.println("B1 Completed " + data.B1);
				
			}
			//function 2 block
			synchronized(data){
				try{
					System.out.println("B2 waiting for A1's notification");
					while(!data.gotoB2){
						data.wait();
					}
					data.B2 = data.A1+ Utility.calculate(200);
					System.out.println("B2 Completed " + data.B2);
					data.gotoA2 = true;
					data.notifyAll();
				}
				catch(InterruptedException e){
					e.printStackTrace();
				}
	
			}	
				//function 3 block
			synchronized(data){
				try{
					System.out.println("B3 waiting for A2's notification");
					while(!data.gotoB3){
						data.wait();
					}
					data.B3 = data.A2+ Utility.calculate(400);
					System.out.println("B3 Completed " + data.B3);
					data.gotoA3 = true;
					data.gotoC = true;
					data.notifyAll();
				}
				catch(InterruptedException e){
					e.printStackTrace();
				}
			}
		}
	}

	static class ThreadC extends Thread{
		private Data data;
		
		public ThreadC(Data data){
			super();
			this.data = data;
		}

		public void run(){
			synchronized(data){
				try{
				System.out.println("Waiting for B3'S completion");
				while(!data.gotoC){
					data.wait();
				}
				int result = data.A2 + data.B3;
				System.out.println("Thread C Completed " + result);

				}
				catch(InterruptedException e){
					e.printStackTrace();
				}
			}

		}
	}

}
