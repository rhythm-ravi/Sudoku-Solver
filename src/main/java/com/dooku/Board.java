package com.dooku;

import java.util.*;

public class Board {
    
    private final int n; // Grid dimension (3 for 9x9, 2 for 4x4, etc.)
    private final int size; // Total size (9 for 9x9, 4 for 4x4, etc.)
    private int[][] board; // Simple 2D representation
    
    // Bitwise constraint tracking - much faster than HashMaps
    private int[] rowConstraints;    // Bitmask of used numbers in each row
    private int[] colConstraints;    // Bitmask of used numbers in each column  
    private int[] boxConstraints;    // Bitmask of used numbers in each box
    
    // For UI animation - maintains compatibility with existing system
    public Deque<int[]> observableState = new LinkedList<>();
    
    private boolean isUnsolved = true;
    
    
    public Board(int n) {
        this.n = n;
        this.size = n * n;
        this.board = new int[size][size];
        this.rowConstraints = new int[size];
        this.colConstraints = new int[size];
        this.boxConstraints = new int[size];
    }
    
    /**
     * FIXED: Converts UI's 4D coordinates (i,j,k,l) to internal 2D (row,col)
     */
    private int[] convert4Dto2D(int i, int j, int k, int l) {
        int actualRow = i * n + k;  // i=box row, k=cell row within box
        int actualCol = j * n + l;  // j=box col, l=cell col within box
        return new int[]{actualRow, actualCol};
    }
    
    /**
     * FIXED: Converts internal 2D (row,col) back to UI's 4D (i,j,k,l)
     */
    private int[] convert2Dto4D(int row, int col) {
        int i = row / n;    // which box row
        int j = col / n;    // which box col
        int k = row % n;    // row within box
        int l = col % n;    // col within box
        return new int[]{i, j, k, l};
    }
    
    /**
     * Place digit - maintains UI compatibility
     */
    public boolean placeDigit(int i, int j, int k, int l, int value) {
        int[] coords = convert4Dto2D(i, j, k, l);
        return placeDigitInternal(coords[0], coords[1], value);
    }
    
    /**
     * Internal place digit with 2D coordinates
     */
    private boolean placeDigitInternal(int row, int col, int value) {
        if (value < 1 || value > size) return false;
        
        // Remove previous value if exists
        removeDigitInternal(row, col);

        int bitMask = 1 << value;
        int boxIndex = getBoxIndex(row, col);
        
        // Check if placement is valid
        if ((rowConstraints[row] & bitMask) != 0 ||
            (colConstraints[col] & bitMask) != 0 ||
            (boxConstraints[boxIndex] & bitMask) != 0) {
            return false;
        }
        
        // Place the digit
        board[row][col] = value;
        rowConstraints[row] |= bitMask;
        colConstraints[col] |= bitMask;
        boxConstraints[boxIndex] |= bitMask;
        
        return true;
    }
    
    /**
     * Remove digit - maintains UI compatibility  
     */
    public void removeDigit(int i, int j, int k, int l) {
        int[] coords = convert4Dto2D(i, j, k, l);
        removeDigitInternal(coords[0], coords[1]);
    }
    
    /**
     * Internal remove digit with 2D coordinates
     */
    private void removeDigitInternal(int row, int col) {
        int value = board[row][col];
        if (value == 0) return;
        
        int bitMask = 1 << value;
        int boxIndex = getBoxIndex(row, col);
        
        board[row][col] = 0;
        rowConstraints[row] &= ~bitMask;
        colConstraints[col] &= ~bitMask;
        boxConstraints[boxIndex] &= ~bitMask;
    }
    
    /**
     * Get box index for given row,col
     */
    private int getBoxIndex(int row, int col) {
        return (row / n) * n + (col / n);
    }
    
    /**
     * Get possible values for a cell using bitwise operations
     */
    private int getPossibilities(int row, int col) {
        if (board[row][col] != 0) return 0;
        
        int boxIndex = getBoxIndex(row, col);
        int usedBits = rowConstraints[row] | colConstraints[col] | boxConstraints[boxIndex];
        
        // All possible values except used ones
        int allPossible = (1 << (size + 1)) - 2; // Bits 1 to size
        return allPossible & ~usedBits;
    }
    
    /**
     * Apply naked singles - if a cell has only one possibility, fill it
     */
    private int applyNakedSingles(Stack<int[]> prevStates) {
        int changeCount = 0;
        
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                if (board[row][col] == 0) {
                    int possibilities = getPossibilities(row, col);
                    
                    if (possibilities == 0) return 0; // Invalid state
                    
                    if (Integer.bitCount(possibilities) == 1) {
                        int value = Integer.numberOfTrailingZeros(possibilities);
                        placeDigitInternal(row, col, value);
                        
                        // Add to observable state for UI animation
                        int[] fourDCoords = convert2Dto4D(row, col);
                        observableState.offerLast(new int[]{value, fourDCoords[0], fourDCoords[1], fourDCoords[2], fourDCoords[3]});
                        
                        prevStates.push(new int[]{row,col});
                        changeCount++;
                    }
                }
            }
        }
        
        return changeCount;
    }
    
    /**
     * Apply hidden singles - if a value can only go in one place in a unit, place it
     */
    private int applyHiddenSingles(Stack<int[]> prevStates) {
        int changeCount = 0;
        
        // Check rows
        for (int row = 0; row < size; row++) {
            for (int value = 1; value <= size; value++) {
                if ((rowConstraints[row] & (1 << value)) == 0) { // Value not used in row
                    int possibleCols = 0;
                    int lastCol = -1;
                    
                    for (int col = 0; col < size; col++) {
                        if ((getPossibilities(row, col) & (1 << value)) != 0) {
                            possibleCols++;
                            lastCol = col;
                        }
                    }
                    
                    if (possibleCols == 1) {
                        placeDigitInternal(row, lastCol, value);
                        int[] fourDCoords = convert2Dto4D(row, lastCol);
                        observableState.offerLast(new int[]{value, fourDCoords[0], fourDCoords[1], fourDCoords[2], fourDCoords[3]});
                        
                        prevStates.push(new int[]{row,lastCol});
                        changeCount++;
                    }
                }
            }
        }
        
        // Check columns
        for (int col = 0; col < size; col++) {
            for (int value = 1; value <= size; value++) {
                if ((colConstraints[col] & (1 << value)) == 0) {
                    int possibleRows = 0;
                    int lastRow = -1;
                    
                    for (int row = 0; row < size; row++) {
                        if ((getPossibilities(row, col) & (1 << value)) != 0) {
                            possibleRows++;
                            lastRow = row;
                        }
                    }
                    
                    if (possibleRows == 1) {
                        placeDigitInternal(lastRow, col, value);
                        int[] fourDCoords = convert2Dto4D(lastRow, col);
                        observableState.offerLast(new int[]{value, fourDCoords[0], fourDCoords[1], fourDCoords[2], fourDCoords[3]});
                        
                        prevStates.push(new int[]{lastRow,col});
                        changeCount++;
                    }
                }
            }
        }
        
        // Check boxes
        for (int boxIdx = 0; boxIdx < size; boxIdx++) {
            for (int value = 1; value <= size; value++) {
                if ((boxConstraints[boxIdx] & (1 << value)) == 0) {
                    int possibleCells = 0;
                    int lastRow = -1, lastCol = -1;
                    
                    int startRow = (boxIdx / n) * n;
                    int startCol = (boxIdx % n) * n;
                    
                    for (int row = startRow; row < startRow + n; row++) {
                        for (int col = startCol; col < startCol + n; col++) {
                            if ((getPossibilities(row, col) & (1 << value)) != 0) {
                                possibleCells++;
                                lastRow = row;
                                lastCol = col;
                            }
                        }
                    }
                    
                    if (possibleCells == 1) {
                        placeDigitInternal(lastRow, lastCol, value);
                        int[] fourDCoords = convert2Dto4D(lastRow, lastCol);
                        observableState.offerLast(new int[]{value, fourDCoords[0], fourDCoords[1], fourDCoords[2], fourDCoords[3]});
                        
                        prevStates.push(new int[]{lastRow,lastCol});
                        changeCount++;
                    }
                }
            }
        }
        
        return changeCount;
    }
    
    /**
     * Find the empty cell with fewest possibilities (MCV heuristic)
     */
    private int[] findMostConstrainedVariable() {
        int[] best = null;
        int minPossibilities = size + 1;
        
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                if (board[row][col] == 0) {
                    int possibilities = getPossibilities(row, col);
                    int count = Integer.bitCount(possibilities);
                    
                    if (count < minPossibilities) {
                        minPossibilities = count;
                        best = new int[]{row, col};
                        
                        if (count == 0) return best; // Impossible state, return immediately
                    }
                }
            }
        }
        
        return best;
    }
    
    /**
     * Main backtracking solver with constraint propagation
     */
    private boolean solveWithBacktracking() {

        // Apply constraint propagation first
        Stack<int[]> prevStates = new Stack<>();
        int key = 0;
        do {
            key = 0;
            key += applyNakedSingles(prevStates);
            key += applyHiddenSingles(prevStates);
        } while (key!=0);
        
        // Find most constrained variable
        int[] cell = findMostConstrainedVariable();
        if (cell == null) return true; // Solved!

        int bestRow = cell[0],      bestCol = cell[1];
        if (getPossibilities(bestRow, bestCol) == 0) {
            while (!prevStates.empty()) {
                int row = prevStates.peek()[0],     col = prevStates.peek()[1];
                prevStates.pop();
                int[] fourDCoords = convert2Dto4D(row, col);
                removeDigitInternal(row, col);
                observableState.offerLast(new int[]{0, fourDCoords[0], fourDCoords[1], fourDCoords[2], fourDCoords[3]});
            }
            return false; // No possibilities, backtrack
        }
        // Try each possible value
        for (int value = 1; value <= size; value++) {
            if ((getPossibilities(bestRow, bestCol) & (1 << value)) != 0) {
                // Try this value
                placeDigitInternal(bestRow, bestCol, value);
                
                // Add to observable state for UI
                int[] fourDCoords = convert2Dto4D(bestRow, bestCol);
                observableState.offerLast(new int[]{value, fourDCoords[0], fourDCoords[1], fourDCoords[2], fourDCoords[3]});
                
                if (solveWithBacktracking()) {
                    return true; // Solution found
                }
                
                // Backtrack
                removeDigitInternal(bestRow, bestCol);

                // Add backtrack to observable state
                observableState.offerLast(new int[]{0, fourDCoords[0], fourDCoords[1], fourDCoords[2], fourDCoords[3]});
            }
        }
        
        while (!prevStates.empty()) {
            int row = prevStates.peek()[0],     col = prevStates.peek()[1];
            prevStates.pop();
            int[] fourDCoords = convert2Dto4D(row, col);
            removeDigitInternal(row, col);
            observableState.offerLast(new int[]{0, fourDCoords[0], fourDCoords[1], fourDCoords[2], fourDCoords[3]});
        }
        return false; // No solution found
    }
    
    /**
     * Main solve method - maintains UI compatibility
     */
    public void solve() {
        observableState.clear();
        
        if (solveWithBacktracking()) {
            isUnsolved = false;
        }
    }
    
    /**
     * Check if puzzle is solved
     */
    public boolean isSolved() {
        return !isUnsolved;
    }
    
    /**
     * Debug method to print current board state
     */
    public void printBoard() {
        System.out.println("Current board state:");
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                System.out.print(board[i][j] + " ");
            }
            System.out.println();
        }
    }
}