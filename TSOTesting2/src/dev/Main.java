package dev;

import java.io.*;

public class Main {

	static final int EXP = 50;
	static final int POB = 10;
	static final int MAXDEPTH = 5;
	static final int MAXNODES = 10;
	static final int MUT = 575; //593 / 575
	static final int TEST = 10;
	static final int ITER = 10;
	
	public static void main(String[] args) {
		BufferedWriter writer;
		String str;
		Parallel[] th;

		double killed = 0;
		double killedTotal = 0;
		double killedG = 0;
		double killedTotalG = 0;

		double time = 0;
		double timeTotal = 0;
		double timeG = 0;
		double timeTotalG = 0;
		
		int size = MUT/TEST;
		int diff = MUT - size*TEST;
		int[] groups = new int[TEST];
		
		for(int i = 0; i < TEST; i++) {
			if (diff > 0) {
				groups[i] = size + 1;
				diff--;
			} else {
				groups[i] = size;
			}
		}
		
		// Activate Garbage Collector
		System.gc();

		for (int I = 1; I <= ITER; I++) {
			
			// open output
			try {
				writer = new BufferedWriter(new FileWriter("results"+I+".txt"));
				str = "Iteration | framework score | genetic score | framework time | genetic time\n";
				writer.append(str);
				writer.flush();
			} catch (IOException e1) {
				e1.printStackTrace();
				return;
			}
			
			killedTotal = 0;
			killedTotalG = 0;
			
			for (int i = 0; i < EXP; i++) {
				th  = new Parallel[TEST];
				
				for (int j = 0; j < TEST; j++) {
					th[j] = new Parallel(groups, j, I);
				}
				
				//Call paralelism
				for (int j = 0; j < TEST; j++) {
					th[j].start();
				}

				//Join paralelism
				try {
					for (int j = 0; j < TEST; j++) {
						th[j].join();
					} 
				} catch (Exception e) {
					e.printStackTrace();
				}

				killed = 0;
				killedG = 0;
				time = 0;
				timeG = 0;
				
				// count mutants killed by best test suite
				for (int k = 0; k < TEST; k++) {
					killed += th[k].getKilled();
					time += th[k].getTime();
				}
				killedTotal += killed/MUT;
				timeTotal += time/TEST;
				
				// count mutants killed by genetic test suite
				for (int k = 0; k < TEST; k++) {
					killedG += th[k].getKilledG();
					timeG += th[k].getTimeG();
				}
				killedTotalG += killedG/MUT;
				timeTotalG += timeG/TEST;
				
				// write results
				try {
					str = (i+1) + " & " + killed/MUT + " & " + killedG/MUT + " & " + time/TEST + " & " + timeG/TEST + "\\\\\n";
					writer.append(str);
					writer.flush();
					str = "\\hline\n";
					writer.append(str);
					writer.flush();
				} catch (IOException e) {
					e.printStackTrace();
				} 
			}
			
			// close output
			try {
				str = "Total & " + killedTotal/EXP + " & " + killedTotalG/EXP + " & " + timeTotal/EXP + " & " + timeTotalG/EXP + "\\\\\n";
				writer.append(str);
				writer.flush();
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
