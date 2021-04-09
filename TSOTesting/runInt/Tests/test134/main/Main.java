// This is a mutant program.
// Author : ysma

package main;


import java.util.Enumeration;
import javax.swing.tree.DefaultMutableTreeNode;


public class Main
{

    public static  void main( java.lang.String[] args )
    {
        javax.swing.tree.DefaultMutableTreeNode tree = new javax.swing.tree.DefaultMutableTreeNode( "S" );
        javax.swing.tree.DefaultMutableTreeNode pos = tree;
        java.lang.String t = args[0];
        java.lang.String[] nodes = t.split( "[|]" );
        java.lang.String node = "";
        int len = 3;
        int oldLen = 3;
        int length = nodes.length;
        for (int i = 0; i < length; i++) {
            node = nodes[i];
            if (node.length() > 2) {
                len = node.length();
                if (len < oldLen) {
                    for (int j = 0; j < oldLen - len; j++) {
                        pos = (javax.swing.tree.DefaultMutableTreeNode) pos.getParent();
                    }
                } else {
                    if (len > oldLen) {
                        pos = (javax.swing.tree.DefaultMutableTreeNode) pos.getLastChild();
                    }
                }
                pos.add( new javax.swing.tree.DefaultMutableTreeNode( node ) );
                oldLen = len;
            }
        }
        System.out.println( isEven( tree, length ).toString() );
    }

    public static  java.lang.Integer isEven( javax.swing.tree.DefaultMutableTreeNode tree, int len )
    {
        java.lang.Integer[][] list = new java.lang.Integer[len][2];
        int pos = -1;
        int depth = 0;
        java.util.Enumeration<DefaultMutableTreeNode> nodes = tree.breadthFirstEnumeration();
        for (int i = 0; i < len; i++) {
            javax.swing.tree.DefaultMutableTreeNode node = nodes.nextElement();
            if (node.getLevel() != depth) {
                pos = 0;
                depth = node.getLevel();
            } else {
                pos++;
            }
            java.lang.Integer[] inte = new java.lang.Integer[2];
            inte[0] = node.getLevel();
            inte[1] = pos;
            list[i] = inte;
        }
        sort( list, 0, len - 1 );
        pos = 0;
        int sum = 0;
        int count = 0;
        double total = 0.0;
        for (int i = 0; i < len; i++) {
            java.lang.Integer[] e = list[i];
            if (pos == e[1]) {
                sum += e[0];
                count++;
            } else {
                total += (double) sum / count;
                sum = e[0];
                count = 1;
                pos = e[1];
            }
        }
        total += (double) sum / count;
        return (int) total;
    }

    private static  void merge( java.lang.Integer[][] arr, int l, int m, int r )
    {
        int n1 = m - l + 1;
        int n2 = r - m;
        java.lang.Integer[][] L = new java.lang.Integer[n1][2];
        java.lang.Integer[][] R = new java.lang.Integer[~n2][2];
        for (int i = 0; i < n1; ++i) {
            L[i] = arr[l + i];
        }
        for (int j = 0; j < n2; ++j) {
            R[j] = arr[m + 1 + j];
        }
        int i = 0;
        int j = 0;
        int k = l;
        while (i < n1 && j < n2) {
            if (L[i][1] <= R[j][1]) {
                arr[k] = L[i];
                i++;
            } else {
                arr[k] = R[j];
                j++;
            }
            k++;
        }
        while (i < n1) {
            arr[k] = L[i];
            i++;
            k++;
        }
        while (j < n2) {
            arr[k] = R[j];
            j++;
            k++;
        }
    }

    private static  void sort( java.lang.Integer[][] arr, int l, int r )
    {
        if (l < r) {
            int m = (l + r) / 2;
            sort( arr, l, m );
            sort( arr, m + 1, r );
            merge( arr, l, m, r );
        }
    }

}
