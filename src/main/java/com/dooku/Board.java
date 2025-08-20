package com.dooku;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Stack;
import java.util.function.*;

import java.util.Arrays;
public class Board {
    

    /*private*/ int[][][][] board;          // Used to represent an n*n matrix of n*n matrices (of digits)
    private int n;
    boolean isUnsolved = true;

    HashMap<Integer, boolean[]> SubgridMap = new HashMap<>();        // Maps each individual subgrid with the list of its possible values
    HashMap<Integer,  boolean[]> RowMap = new HashMap<>();
    HashMap<Integer,  boolean[]> ColumnMap = new HashMap<>();

    Deque<int[]> queue = new LinkedList<>();
    Stack<int[]> stack = new Stack<>();

    private void generateList(BiConsumer<Deque<int[]>,Integer> rule) {
        rule.accept(queue, n);
    }

    public Board(int n) {       // Creates a Board object with empty matrice of n2 by n2
        this.n = n;
        board = new int[n][n][n][n];       
        setPossibilities();
    }

    private void setPossibilities() {
        for (int i=0; i<n; i++) {
            for (int j=0; j<n; j++) {
                
                boolean[] allTrue = new boolean[n*n+1];
                for (int k=0; k<=n*n; k++) allTrue[k]=true;

                int key = n*i + j;
                SubgridMap.put( key, allTrue.clone() );
                RowMap.put(  key,  allTrue.clone() );
                ColumnMap.put(  key, allTrue.clone() );
            }
        }
    }

    private boolean placeDigit(int row, int col, int srow, int scol) {      // Place the next possible number on given tile

        // Keys representing coordinates that get affected through this placement
        int gkey = row*n + col;
        int rkey = row*n + srow;
        int ckey = col*n + scol;

        int currentValue = board[row][col][srow][scol];

        currentValue++;

        while (currentValue <= n*n) {   // All tries for extension here
            observableState.offerLast(new int[]{currentValue, row, col, srow, scol});
            if ( placeDigit(row, col, srow, scol, currentValue) )
                return true;
            currentValue++;
        }

        observableState.offerLast(new int[]{0, row, col, srow, scol});
        removeDigit(row, col, srow, scol);  // Alll backtracks here, so adding extension tries and backtracks should give us entire solution step set

        board[row][col][srow][scol] = 0;          // backtracking to the previous configuration, hence will need to be checked for all possible values extending from any new configuration
        return false;                             // No valid placements for said tile under given k-1 config
    }
    
    public boolean placeDigit(int row, int col, int srow, int scol, int value) {
        
        // Keys representing coordinates that get affected through this placement
        int gkey = row*n + col;
        int rkey = row*n + srow;
        int ckey = col*n + scol;

        if (    (value>=1 && value<=n*n) && SubgridMap.get(gkey)[value]   &&      RowMap.get(rkey)[value]     &&      ColumnMap.get(ckey)[value]        ) {         // If placeable based on possibilities for the three coordinates
            removeDigit(row, col, srow, scol);  // Remove current digit if valid placement
            
            SubgridMap.get(gkey)[value] = false;      // For each coord, its possibilities are updated (for value)
            RowMap.get(rkey)[value] = false;
            ColumnMap.get(ckey)[value] = false;
            board[row][col][srow][scol] = value;

            return true;                          // Tile had valid placements and was placed with the next valid value
        }

        return false;                             // No valid placements for said tile under given k-1 config
    }

    public void removeDigit(int row, int col, int srow, int scol) {
        int gkey = row*n + col;
        int rkey = row*n + srow;
        int ckey = col*n + scol;

        int currentValue = board[row][col][srow][scol];
        SubgridMap.get(gkey)[currentValue] = true;
        RowMap.get(rkey)[currentValue] = true;
        ColumnMap.get(ckey)[currentValue] = true;   // Whatever was before now becomes placable
    }

    public Deque<int[]> observableState = new LinkedList<>();

    public void solve() {
        

        // To be placed queue only generated after the fixed nums have been added, since 
        // those numbs mustnt be changed
        // Either store those nums and check whether current tile belongs to the fixed nums collection or not (for all tiles)
        // Or use given approach
        generateList((p,q) -> {
            // here row and column corresponding to if the board were a n^2 * n^2 matrix
            for (int row=0; row<q*q; row++) for (int col=0; col<q*q; col++)  {
                int i, j, k, l;
                i=row/n;    j=col/n;    k=row%n;    l=col%n;
                if (board[i][j][k][l]==0) 
                    p.offerLast(new int[]{i,j,k,l});    
            }
        });


        // if dequed from ques,then vailid k config( ie for tiles dequed)
        while(!queue.isEmpty()) {       // While n config not attained
            int[] current = queue.peekFirst();
            // System.out.println( SubgridMap.get(n*current[0]+current[1])[4] );
            // // int row = current[0], col = current[1], srow = current[2], scol = current[3];
            // System.out.println(current[0]*n + current[1] + ""  + (current[2]*n  + current[3] ));
            if (placeDigit(current[0], current[1], current[2], current[3])) {          // If placement successful
                // System.out.println("SUCCESS!");
                stack.push(queue.pollFirst());
            }
            else {
                queue.offerFirst(stack.pop());
            }        
        }
        isUnsolved=false;
    }
}

// each row has a set of tiles, and so on
// each column has a set of tiles and the tiles a
// each tile exists independently (outside of their scopes) and draws from a row, column and subgrid

// Its possibilities is the intersection b/w them all, and since if any changes, then all would need to change

// each tile refers to the total al collection of them all, 

// if we backtrack on a tile,


