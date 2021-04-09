package dev;

import java.io.*;

public class Main {

	static final int EXP = 50;
	static final int POB = 5;
	static final int MAXDEPTH = 5;
	static final int MAXNODES = 10;
	static final int MUT = 593;
	static final int TEST = 10;
	static final int ITER = 10;
	
	public static void main(String[] args) {
		BufferedWriter writer;
		String str;
		Parallel[] th;

		double killed = 0;
		double killedTotal = 0;
		double killedR = 0;
		double killedTotalR = 0;
		
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
		
		// open output
		try {
			writer = new BufferedWriter(new FileWriter("results.txt"));
			str = "Iteration | framework | random\n";
			writer.append(str);
			writer.flush();
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}
		
		killedTotal = 0;
		killedTotalR = 0;

		for (int i = 0; i < EXP; i++) {
			th  = new Parallel[TEST];
			
			for (int j = 0; j < TEST; j++) {
				th[j] = new Parallel(groups, j);
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
			killedR = 0;
			
			// count mutants killed by best test suite
			for (int k = 0; k < TEST; k++) {
				killed += th[k].getKilled();
			}
			killedTotal += killed/MUT;
			
			// count mutants killed by random test suite
			for (int k = 0; k < TEST; k++) {
				killedR += th[k].getKilledR();
			}
			killedTotalR += killedR/MUT;
			
			// write results
			try {
				str = (i+1) + " & " + killed/MUT + " & " + killedR/MUT + "\\\\\n";
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
			str = "Total & " + killedTotal/EXP + " & " + killedTotalR/EXP + "\\\\\n";
			writer.append(str);
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
