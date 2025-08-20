import java.util.*;

public class Puzzle {
    public static final int N = 9;

    // these are the given clues of 9x9 2d array. if 0, no clue given
    private final int[][] givens = new int[N][N]; 

    // marks whether a cell belongs to any cage
    private final boolean[][] inCage = new boolean[N][N];

    // all cages in this puzzle
    private final ArrayList<Cage> cages = new ArrayList<>();

    /* Add a cage and mark its cells. Throws if a cell is already in another cage. */
    public void addCage(Cage c) {
        // mark cells; cell coords are 1..9 in Cell class
        for (Cell cell : c.getCells()) {
            int r0 = cell.getRow() - 1; // conversion from 1-9 to 0-8.
            int c0 = cell.getCol() - 1;
            if (inCage[r0][c0]) { // if the inCage is already marked true for a cell we are trying to mark, give exception, this is illegal.
                throw new IllegalArgumentException(
                    "Cell (" + cell.getRow() + "," + cell.getCol() + ") is already in a cage."
                );
            }
            inCage[r0][c0] = true;
        }
        cages.add(c);
    }

    /* Set a clue (prefilled digit). row/col are 1..9, d is 1..9, 0 clears. */
    public void setGiven(int rownum, int colnum, int d) {
        if (rownum < 1 || rownum > 9 || colnum < 1 || colnum > 9)
            throw new IllegalArgumentException("Row/col must be 1..9.");
        if (d < 0 || d > 9)
            throw new IllegalArgumentException("Digit must be 0..9 (0 clears).");
        givens[rownum - 1][colnum - 1] = d;
    }

    /* Get the given at (row,col). Returns 0 if empty. row/col are 1..9. */
    public int getGiven(int row1to9, int col1to9) {
        if (row1to9 < 1 || row1to9 > 9 || col1to9 < 1 || col1to9 > 9)
            throw new IllegalArgumentException("Row/col must be 1..9.");
        return givens[row1to9 - 1][col1to9 - 1];
    }

    /* True if this cell belongs to any cage. row/col are 1..9. */
    public boolean isInCage(int row1to9, int col1to9) {
        if (row1to9 < 1 || row1to9 > 9 || col1to9 < 1 || col1to9 > 9)
            throw new IllegalArgumentException("Row/col must be 1..9.");
        return inCage[row1to9 - 1][col1to9 - 1];
    }

    /* Access to cages list */
    public ArrayList<Cage> getCages() {
        return new ArrayList<>(cages); // create a new one while returning so it doesn't accidentally get modified
    }

    /* Direct accessors if you need the raw arrays elsewhere. */
    public int[][] getGivens() 
    {
        return givens; 
    }
    public boolean[][] getInCage()
    {
        return inCage;
    }
}