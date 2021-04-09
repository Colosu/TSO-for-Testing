package dev;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

public class Parallel extends Thread {
	
	public Parallel(int[] groups, int j) {
		this.groups = groups;
		this.pos = j;
	}
	
	@Override
	public void run() {
		ArrayList<DefaultMutableTreeNode> pob;
		ArrayList<DefaultMutableTreeNode> pobBest;
		DefaultMutableTreeNode gBest;
		ArrayList<Integer> fitnessList;
		ArrayList<Integer> fitnessBestList;
		Integer gBestFitness;
		int count = 0;
		int num = 0;
		DefaultMutableTreeNode rand;

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
		while (Collections.max(fitnessBestList) / num < 1 && count < Main.ITER) {
			iterate(pob, pobBest, gBest, fitnessList, fitnessBestList, gBestFitness);
			count++;
		}
		
		// Generate random test suite as input
		rand = new DefaultMutableTreeNode("S");
		derive(rand, Main.MAXDEPTH, Main.MAXNODES, Main.MAXNODES);
		
		// count mutants killed by best test suite
		killed = 0;
		killed = test(gBest);
		
		// count mutants killed by random test suite
		killedR = 0;
		killedR = test(rand);
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
		for(Iterator<DefaultMutableTreeNode> e = pob.iterator(); e.hasNext();) {
			fitnessList.add(fitness(e.next()));
		}
		return fitnessList;
	}
	
	private void initialization(ArrayList<DefaultMutableTreeNode> pob, ArrayList<DefaultMutableTreeNode> pobBest,
			DefaultMutableTreeNode gBest, ArrayList<Integer> fitnessList,
			ArrayList<Integer> fitnessBestList, Integer gBestFitness) {
		
		DefaultMutableTreeNode newNode;
		
		// deep copy of fitnessList
		for (Iterator<Integer> it = fitnessList.iterator(); it.hasNext();) {
			fitnessBestList.add(it.next());
		}

		// deep copy of pob
		for (Iterator<DefaultMutableTreeNode> it = pob.iterator(); it.hasNext();) {
			newNode = new DefaultMutableTreeNode("S");
			replicate(newNode, it.next());
			pobBest.add(newNode);
		}

		// get best element in pob and its fitness
		gBestFitness = Collections.max(fitnessList);
		int idx = fitnessList.indexOf(gBestFitness);
		replicate(gBest, pob.get(idx));
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
	
	public double getKilled() {
		return killed;
	}
	
	public double getKilledR() {
		return killedR;
	}

	private double killed;
	private double killedR;
	private int[] groups;
	private int pos;
}
