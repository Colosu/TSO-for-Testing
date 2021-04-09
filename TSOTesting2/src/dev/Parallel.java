package dev;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

public class Parallel extends Thread {
	
	public Parallel(int[] groups, int j, int it) {
		this.groups = groups;
		this.pos = j;
		this.iter = it;
		rand = new Random();
	}
	
	@Override
	public void run() {
		DefaultMutableTreeNode tso;
		DefaultMutableTreeNode genetic;
    	double startTime;
    	double endTime;

		// Generate TSO test suite as input
    	time = 0;
		startTime = System.nanoTime() /(double)1000000000;
		tso = TSO();
		endTime = System.nanoTime() /(double)1000000000;
		time = endTime - startTime;
		
		// Generate genetic test suite as input
		timeG = 0;
		startTime = System.nanoTime() /(double)1000000000;
		genetic = GP();
		endTime = System.nanoTime() /(double)1000000000;
		timeG = endTime - startTime;
		
		// count mutants killed by best test suite
		killed = 0;
		killed = test(tso);
		
		// count mutants killed by genetic test suite
		killedG = 0;
		killedG = test(genetic);
	}
	
	private DefaultMutableTreeNode TSO() {
		ArrayList<DefaultMutableTreeNode> pob;
		ArrayList<DefaultMutableTreeNode> pobBest;
		DefaultMutableTreeNode gBest;
		ArrayList<Integer> fitnessList;
		ArrayList<Integer> fitnessBestList;
		Integer gBestFitness;
		int count = 0;
		int num = 0;

		// initialization
		pob = initPob(Main.POB);
		pobBest = new ArrayList<DefaultMutableTreeNode>();
		gBest = new DefaultMutableTreeNode("S");
		fitnessList = fitnessL(pob);
		fitnessBestList = new ArrayList<Integer>();
		gBestFitness = 0;
		initialization(pob, pobBest, gBest, fitnessList, fitnessBestList, gBestFitness);
		
		// iterate
		count = 0;
		num = 0;
		for (int i = 0; i < Main.TEST; i++) {
			if (i != pos) {
				num += groups[i];
			}
		}
		while (Collections.max(fitnessBestList) / num < 1 && count < iter) {
			iterate(pob, pobBest, gBest, fitnessList, fitnessBestList, gBestFitness);
			count++;
		}
		
		return gBest;
	}
	
	@SuppressWarnings("unchecked")
	private DefaultMutableTreeNode GP() {
		ArrayList<DefaultMutableTreeNode> pob;
		ArrayList<DefaultMutableTreeNode> pobD;
		DefaultMutableTreeNode ts;
		DefaultMutableTreeNode tsm;
		DefaultMutableTreeNode par;
		DefaultMutableTreeNode best;
		ArrayList<Integer> fitnessList;
		ArrayList<Integer> fitnessListD;
		Integer bestFitness;
		int epochs = 0;
		int count = 0;
		int prev = 0;
		int newer = 0;
		ArrayList<Integer> pos = new ArrayList<Integer>();
		int bestPos = 0;

		// initialization
		pob = initPob(Main.POB);
		pobD = new ArrayList<DefaultMutableTreeNode>();
		best = new DefaultMutableTreeNode("S");
		fitnessList = fitnessL(pob);
		fitnessListD = new ArrayList<Integer>();
		bestFitness = 0;
		
		//Main loop
		while (!StopCriterion(epochs, count)) {

			prev = newer;

			//Select next generation
			Selection(pob, pobD, fitnessList, fitnessListD, prev);
			
			pobD.clear();
			fitnessListD.clear();

			//Perform Crossover
			pos.clear();
			for (int i = 0; i < Main.POB; i++) {
				pos.add(i);
			}
			Collections.shuffle(pos);

			for (int i = 0; i < Main.POB-1; i+=2) {
				if (rand.nextDouble() < 0.9) {
					pobD.add(cloneNode(pob.get(pos.get(i))));
					fitnessListD.add(fitnessList.get(pos.get(i)).intValue());
					pobD.add(cloneNode(pob.get(pos.get(i+1))));
					fitnessListD.add(fitnessList.get(pos.get(i+1)).intValue());
					Crossover(pob.get(pos.get(i)), pob.get(pos.get(i+1)));
				}
			}

			//Perform Mutation
			for (int i = 0; i < Main.POB; i++) {
				ts = pob.get(i);

				for (Enumeration<DefaultMutableTreeNode> it = ts.breadthFirstEnumeration(); it.hasMoreElements(); ) {
					DefaultMutableTreeNode nod = it.nextElement();
					if (rand.nextDouble() < 0.05 && !nod.isRoot()) {
						tsm = Mutation(nod);
						par = (DefaultMutableTreeNode) nod.getParent();
						par.remove(nod);
						par.add(tsm);
						tsm = null;
						break;
					}
				}
//				pob.set(i, ts);
			}

			//Evaluate population
			fitnessList.clear();
			for (int i = 0; i < Main.POB; i++) {
				fitnessList.add(fitness(pob.get(i)));
			}

			newer = Collections.max(fitnessList);
			epochs++;
			if (prev == newer) {
				count++;
			} else {
				count = 0;
			}

			if (newer > bestFitness) {
				bestPos = 0;
				while(fitnessList.get(bestPos) < newer) {
					bestPos++;
				}
				best = cloneNode(pob.get(bestPos));
				bestFitness = fitnessList.get(bestPos);
			}
			
		}
		
		return best;
	}


	private ArrayList<DefaultMutableTreeNode> initPob(int tamPob) {
		ArrayList<DefaultMutableTreeNode> pob = new ArrayList<DefaultMutableTreeNode>();
		for(int i = 0; i < tamPob; i++) {
			pob.add(new DefaultMutableTreeNode("S"));
			derive(pob.get(i), Main.MAXDEPTH, 0, Main.MAXNODES);
		}
		return pob;
	}

	private ArrayList<Integer> fitnessL(ArrayList<DefaultMutableTreeNode> pob) {
		ArrayList<Integer> fitnessList = new ArrayList<Integer>();
		for(DefaultMutableTreeNode e : pob) {
			fitnessList.add(fitness(e));
		}
		return fitnessList;
	}
	
	private void initialization(ArrayList<DefaultMutableTreeNode> pob, ArrayList<DefaultMutableTreeNode> pobBest,
			DefaultMutableTreeNode gBest, ArrayList<Integer> fitnessList,
			ArrayList<Integer> fitnessBestList, Integer gBestFitness) {
		
		DefaultMutableTreeNode newNode;
		
		// deep copy of fitnessList
		for (Integer it : fitnessList) {
			fitnessBestList.add(it);
		}

		// deep copy of pob
		for (DefaultMutableTreeNode it : pob) {
			newNode = new DefaultMutableTreeNode("S");
			replicate(newNode, it);
			pobBest.add(newNode);
		}

		// get best element in pob and its fitness
		gBestFitness = Collections.max(fitnessList);
		int idx = fitnessList.indexOf(gBestFitness);
		replicate(gBest, pob.get(idx));
	}

	private boolean StopCriterion(int epochs, int count) {
		boolean stop = false;
	
		if ((count > epochs*0.2 && epochs > iter/5) || epochs > iter) {
			stop = true;
		}
	
		return stop;
	}

	@SuppressWarnings("unchecked")
	private void Selection(ArrayList<DefaultMutableTreeNode> TS, ArrayList<DefaultMutableTreeNode> TSD, ArrayList<Integer> FF, ArrayList<Integer> FFD, int prev) {
	
		ArrayList<DefaultMutableTreeNode> TSP = (ArrayList<DefaultMutableTreeNode>) TS.clone();
		int pos[] = new int[Main.POB];
		int posD[] = new int[Main.POB];
		double mean = FF.stream().mapToDouble(a -> a).average().orElse(Double.NaN);
		boolean valid = false;
		
		if(mean == Double.NaN) {
			System.err.println("Problems!!!");
		}
	
		for (int i = 0; i < Main.POB; i++) {
			pos[i] = FF.get(i) >= mean - rand.nextInt((int)(Math.abs(prev-mean)+1)) ? i : -1; //pos selection;
			if (FFD.size() > i) {
				posD[i] = FFD.get(i) >= mean - rand.nextInt((int)(Math.abs(prev-mean)+1)) ? i : -1; //pos selection;
			} else {
				posD[i] = -1;
			}
		}
		
		int it = 0;
		while (it < Main.POB && !valid) {
			if (posD[it] >= 0) {
				valid = true;
			}
			it++;
		}
		
		TS.clear();
		
		if (valid) {
			for (int i = 0; i < Main.POB; i++) {
				while (posD[i] < 0 || posD[i] > Main.POB) {
					posD[i] = posD[rand.nextInt(Main.POB)];
				}
			}
			for (int i = 0; i < Main.POB; i++) {
				if (pos[i] >= 0) {
					TS.add(cloneNode(TSP.get(pos[i])));
				} else {
					TS.add(cloneNode(TSD.get(posD[i])));
				}
			}
		} else {
			for (int i = 0; i < Main.POB; i++) {
				while (pos[i] < 0 || pos[i] > Main.POB) {
					pos[i] = pos[rand.nextInt(Main.POB)];
				}
			}
			for (int i = 0; i < Main.POB; i++) {
				TS.add(cloneNode(TSP.get(pos[i])));
			}
		}
	}
	
	private void iterate(ArrayList<DefaultMutableTreeNode> pob, ArrayList<DefaultMutableTreeNode> pobBest,
			DefaultMutableTreeNode gBest, ArrayList<Integer> fitnessList,
			ArrayList<Integer> fitnessBestList, Integer gBestFitness) {
		
		for(int i = 0; i < pob.size(); i++) {
			update(pob.get(i),pobBest.get(i),gBest);
			int fit = fitness(pob.get(i));
			if(fit > fitnessBestList.get(i)) {
				fitnessBestList.set(i, fit);
				DefaultMutableTreeNode aux = new DefaultMutableTreeNode("S");
				replicate(aux,pob.get(i));
				pobBest.set(i,aux);
			}
		}
		gBestFitness = Collections.max(fitnessBestList);
		int idx = fitnessBestList.indexOf(gBestFitness);
		gBest = new DefaultMutableTreeNode("S");
		replicate(gBest,pobBest.get(idx));
	}

	private void update(DefaultMutableTreeNode pob,
			DefaultMutableTreeNode pobBest, DefaultMutableTreeNode gBest) {
		
		DefaultMutableTreeNode pobBestCopy = new DefaultMutableTreeNode("S");
		replicate(pobBestCopy,pobBest);
		
		treeIntersect(pobBestCopy,gBest);
		treeIntersect(pob,pobBestCopy);
	}

	private void treeIntersect(TreeNode a, TreeNode b) {
		if(!a.isLeaf() && !b.isLeaf()) {
			boolean areEquals = true;
			int nChildren = a.getChildCount();
			if(b.getChildCount() == nChildren) {
				for(int i =0; i < nChildren && areEquals; i++) {
					areEquals = a.getChildAt(i).toString() == b.getChildAt(i).toString();
				}
			}
			else {
				areEquals = false;
			}
			if(a.toString() == b.toString() && areEquals) {
				for(int i = 0; i < nChildren; i++) {
					treeIntersect(a.getChildAt(i),b.getChildAt(i));
				}
			}
			else {
				((DefaultMutableTreeNode)a).removeAllChildren();
				derive(((DefaultMutableTreeNode)a), Integer.max(((DefaultMutableTreeNode)a).getDepth(),((DefaultMutableTreeNode)b).getDepth()), 0, Main.MAXNODES);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void Crossover(DefaultMutableTreeNode TS1, DefaultMutableTreeNode TS2) {
		
		Enumeration<DefaultMutableTreeNode> it1 = null;
		Enumeration<DefaultMutableTreeNode> it2 = null;
		DefaultMutableTreeNode c1 = null;
		DefaultMutableTreeNode c2 = null;
		DefaultMutableTreeNode p1 = null;
		DefaultMutableTreeNode p2 = null;

		int nNodes1 = 0;
		int nNodes2 = 0;
		int nNodesC1 = 0;
		int nNodesC2 = 0;
		int lim = 0;
		
		for(Enumeration<DefaultMutableTreeNode> e=TS1.preorderEnumeration();e.hasMoreElements();e.nextElement()) {
			nNodes1++;
		}
		for(Enumeration<DefaultMutableTreeNode> e=TS2.preorderEnumeration();e.hasMoreElements();e.nextElement()) {
			nNodes2++;
		}
		
		do {
			nNodesC1 = 0;
			nNodesC2 = 0;
			
			it1 = TS1.breadthFirstEnumeration();
			it2 = TS2.breadthFirstEnumeration();
			
			lim = rand.nextInt(nNodes1);
			for (int i = 0; i < lim; i++) {
				it1.nextElement();
			}
			lim = rand.nextInt(nNodes2);
			for (int i = 0; i < lim; i++) {
				it2.nextElement();
			}
			
			c1 = it1.nextElement();
			c2 = it2.nextElement();
			
			for(Enumeration<DefaultMutableTreeNode> e=c1.preorderEnumeration();e.hasMoreElements();e.nextElement()) {
				nNodesC1++;
			}
			for(Enumeration<DefaultMutableTreeNode> e=c2.preorderEnumeration();e.hasMoreElements();e.nextElement()) {
				nNodesC2++;
			}
		} while (c1.isRoot() || c2.isRoot() || nNodes1 - nNodesC1 + nNodesC2 > Main.MAXNODES || nNodes2 - nNodesC2 + nNodesC1 > Main.MAXNODES
				|| c1.getLevel() + c2.getDepth() > Main.MAXDEPTH || c2.getLevel() + c1.getDepth() > Main.MAXDEPTH);
		
		p1 = (DefaultMutableTreeNode) c1.getParent();
		p2 = (DefaultMutableTreeNode) c2.getParent();
		
		p1.add(c2);
		p2.add(c1);
		
		rename(p1);
		rename(p2);
	}
	
	@SuppressWarnings("unchecked")
	private DefaultMutableTreeNode Mutation(DefaultMutableTreeNode start) {
	
		DefaultMutableTreeNode T = null;
		int nNodes1 = 0;
		int nNodes2 = 0;
		T = new DefaultMutableTreeNode(start.getUserObject());
		for(Enumeration<DefaultMutableTreeNode> e=((DefaultMutableTreeNode) start.getRoot()).preorderEnumeration();e.hasMoreElements();e.nextElement()) {
			nNodes1++;
		}
		for(Enumeration<DefaultMutableTreeNode> e=start.preorderEnumeration();e.hasMoreElements();e.nextElement()) {
			nNodes2++;
		}
		derive(T, Main.MAXDEPTH - start.getLevel(), 0, Main.MAXNODES - nNodes1 + nNodes2);
		return T;
	}

	@SuppressWarnings("unchecked")
	private Integer fitness(DefaultMutableTreeNode next) {
		DefaultMutableTreeNode aux;
		ArrayList<String> inputs = new ArrayList<String>();
		Process p;
		ProcessBuilder pb;
		BufferedReader buf;
		String resultM = "0";
		String resultO = "0";
		boolean[] kills = new boolean[Main.MUT];
		int killed = 0;
		boolean blocked = false;
		String t;
		int ini = 0;
		int fin = 0;

		// get inputs
		for (Enumeration<DefaultMutableTreeNode> e=next.children();e.hasMoreElements();) {
			aux = e.nextElement();
			t = "";
			for(Enumeration<DefaultMutableTreeNode> e2=aux.preorderEnumeration();e2.hasMoreElements();) {
				t += e2.nextElement().toString();
				t += "|";
			}
			inputs.add(t);
		}

		// initialize counter
		for (int i = 0; i < Main.MUT; i++) {
			kills[i] = false;
		}

		// test test suite
		try {
			for (int j = 0; j < inputs.size(); j++) {
				fin = 0;
				for (int k = 0; k < pos; k++) {
					fin += groups[k];
				}
				ini = fin + groups[pos];
				for (int k = 0; k < fin; k++) {
					pb = new ProcessBuilder("java", "main.Main", inputs.get(j));
					pb.redirectError();
					pb.directory(new File("Tests/test" + (k + 1) + "/"));
					p = pb.start();
					buf = new BufferedReader(new InputStreamReader(p.getInputStream()));
					blocked = !p.waitFor(1, TimeUnit.SECONDS);
					if (!blocked) {
						resultM = buf.readLine();
					} else {
						p.destroyForcibly();
					}
					pb = new ProcessBuilder("java", "main.Main", inputs.get(j));
					pb.redirectError();
					pb.directory(new File("Tests/original/"));
					p = pb.start();
					buf = new BufferedReader(new InputStreamReader(p.getInputStream()));
					blocked = !p.waitFor(1, TimeUnit.SECONDS);
					if (!blocked) {
						resultO = buf.readLine();
					} else {
						p.destroyForcibly();
					}
					if (blocked || resultM == null || !Boolean.valueOf(resultM).equals(Boolean.valueOf(resultO))) {
						kills[k] = true;
					}
				}
				for (int k = ini; k < Main.MUT; k++) {
					pb = new ProcessBuilder("java", "main.Main", inputs.get(j));
					pb.redirectError();
					pb.directory(new File("Tests/test" + (k + 1) + "/"));
					p = pb.start();
					buf = new BufferedReader(new InputStreamReader(p.getInputStream()));
					blocked = !p.waitFor(1, TimeUnit.SECONDS);
					if (!blocked) {
						resultM = buf.readLine();
					} else {
						p.destroyForcibly();
					}
					pb = new ProcessBuilder("java", "main.Main", inputs.get(j));
					pb.redirectError();
					pb.directory(new File("Tests/original/"));
					p = pb.start();
					buf = new BufferedReader(new InputStreamReader(p.getInputStream()));
					blocked = !p.waitFor(1, TimeUnit.SECONDS);
					if (!blocked) {
						resultO = buf.readLine();
					} else {
						p.destroyForcibly();
					}
					if (blocked || resultM == null || !Boolean.valueOf(resultM).equals(Boolean.valueOf(resultO))) {
						kills[k] = true;
					}
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// count mutants killed
		killed = 0;
		for (int i = 0; i < fin; i++) {
			if (kills[i]) {
				killed++;
			}
		}
		for (int i = ini; i < Main.MUT; i++) {
			if (kills[i]) {
				killed++;
			}
		}
		return killed;
	}
	
	@SuppressWarnings("unchecked")
	private Integer test(DefaultMutableTreeNode root) {
		DefaultMutableTreeNode aux;
		ArrayList<String> inputs = new ArrayList<String>();
		Process p;
		ProcessBuilder pb;
		BufferedReader buf;
		String resultM = "0";
		String resultO = "0";
		boolean[] kills = new boolean[Main.MUT];
		int killed = 0;
		boolean blocked = false;
		String t;
		int ini = 0;
		int fin = 0;

		// get inputs
		for (Enumeration<DefaultMutableTreeNode> e = root.children(); e.hasMoreElements();) {
			aux = e.nextElement();
			t = "";
			for (Enumeration<DefaultMutableTreeNode> e2 = aux.preorderEnumeration(); e2.hasMoreElements();) {
				t += e2.nextElement().toString();
				t += "|";
			}
			inputs.add(t);
		}

		// initialize counter
		for (int i = 0; i < Main.MUT; i++) {
			kills[i] = false;
		}
		
		// test test suite
		try {
			for (int j = 0; j < inputs.size(); j++) {
				ini = 0;
				for (int k = 0; k < pos; k++) {
					ini += groups[k];
				}
				fin = ini + groups[pos];
				for (int k = ini; k < fin; k++) {
					pb = new ProcessBuilder("java", "main.Main", inputs.get(j));
					pb.redirectError();
					pb.directory(new File("Tests/test" + (k + 1) + "/"));
					p = pb.start();
					buf = new BufferedReader(new InputStreamReader(p.getInputStream()));
					blocked = !p.waitFor(1, TimeUnit.SECONDS);
					if (!blocked) {
						resultM = buf.readLine();
					} else {
						p.destroyForcibly();
					}
					pb = new ProcessBuilder("java", "main.Main", inputs.get(j));
					pb.redirectError();
					pb.directory(new File("Tests/original/"));
					p = pb.start();
					buf = new BufferedReader(new InputStreamReader(p.getInputStream()));
					blocked = !p.waitFor(1, TimeUnit.SECONDS);
					if (!blocked) {
						resultO = buf.readLine();
					} else {
						p.destroyForcibly();
					}
					if (blocked || resultM == null || !Boolean.valueOf(resultM).equals(Boolean.valueOf(resultO))) {
						kills[k] = true;
					}
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// count mutants killed
		killed = 0;
		for (int i = ini; i < fin; i++) {
			if (kills[i]) {
				killed++;
			}
		}
		return killed;
	}

	@SuppressWarnings("unchecked")
	public void derive(DefaultMutableTreeNode node, int maxDepth, int minNodes, int maxNodes) {
		applyGrammar(node, maxDepth, minNodes, maxNodes);
		for(Enumeration<DefaultMutableTreeNode> e = node.children();e.hasMoreElements();) {
			DefaultMutableTreeNode aux = e.nextElement();
			derive(aux, maxDepth - 1, minNodes, maxNodes);
		}
	}

	@SuppressWarnings("unchecked")
	private void applyGrammar(DefaultMutableTreeNode node, int maxDepth, int minNodes, int maxNodes) {
		DefaultMutableTreeNode aux = (DefaultMutableTreeNode) node.getRoot();
		int nNodes = 0;
		if (node.toString() == "S" && node.getChildCount() == 0) {
			node.add(new DefaultMutableTreeNode(node.toString() + Integer.toString(0)));
		}
		if (maxDepth > 0) {
			Random rand = new Random();
			int choose = rand.nextInt(2);
			int nChildren = node.getChildCount();
			for(Enumeration<DefaultMutableTreeNode> e2=aux.preorderEnumeration();e2.hasMoreElements();e2.nextElement()) {
				nNodes++;
			}
			if (nNodes < minNodes && nChildren == 0) {
				choose = 0;
			}
			while (choose != 1 && nNodes < maxNodes) {
				node.add(new DefaultMutableTreeNode(node.toString() + Integer.toString(nChildren)));
				nChildren++;
				choose = rand.nextInt(2);
				nNodes++;
			} 
		}
	}

	@SuppressWarnings("unchecked")
	private void replicate(DefaultMutableTreeNode newNode, DefaultMutableTreeNode next) {
		if(!next.isLeaf()) {
			for(Enumeration<DefaultMutableTreeNode> e = next.children(); e.hasMoreElements();) {
				DefaultMutableTreeNode aux = e.nextElement();
				DefaultMutableTreeNode toAdd =  new DefaultMutableTreeNode(aux.toString());
				newNode.add(toAdd);
				replicate(toAdd,aux);
			}
		}
	}

	private DefaultMutableTreeNode cloneNode(DefaultMutableTreeNode node){
		DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(node.getUserObject());
		for(int iChildren=node.getChildCount(), i=0;i<iChildren; i++){
			newNode.add((DefaultMutableTreeNode)cloneNode((DefaultMutableTreeNode)node.getChildAt(i) ) );
		}
		return newNode;
	}
	

	private void rename(DefaultMutableTreeNode node) {
		DefaultMutableTreeNode child;
		String label = node.toString();
		if (node.getChildCount() > 0) {
			for(int iChildren=node.getChildCount(), i=0;i<iChildren; i++){
				child = (DefaultMutableTreeNode) node.getChildAt(i);
				child.setUserObject(label + Integer.toString(i));
				rename(child);
			}
		}
	}
	
	public double getKilled() {
		return killed;
	}
	
	public double getKilledG() {
		return killedG;
	}
	
	public double getTime() {
		return time;
	}
	
	public double getTimeG() {
		return timeG;
	}

	private double killed;
	private double killedG;
	private double time;
	private double timeG;
	private int[] groups;
	private int pos;
	private int iter;
	private Random rand;
}
