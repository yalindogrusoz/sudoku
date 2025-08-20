import java.util.*;
public class Cage {
    //instance vars
    private int targetSum; // what the cells of a cage should sum to 
    private ArrayList<Cell> cells;

    // constructor 
    public Cage(int[] rows, int[] cols, int targetSum) {
        if (rows.length != cols.length) // if mismatch between size of parallel arrays
        { 
            throw new IllegalArgumentException("Row and column arrays must have same length.");
        }
        this.targetSum = targetSum;
        this.cells = new ArrayList<>();
        for (int i = 0; i < rows.length; i++) {
            this.cells.add(new Cell(rows[i], cols[i])); // fill the ArrayList with cells with corresponding row and col
        }
    }

    //getters
    public int getTargetSum()
    {
        return targetSum;
    }

    public ArrayList<Cell> getCells()
    {
        return cells;
    }

    public int getSize()
    {
        return cells.size();
    }

    public boolean containsCell(Cell c)
    {
        for (int i = 0; i < cells.size(); i++)
        {
            if (c.equals(cells.get(i)))
            {
                return true;
            }
        }
        return false;
    }
}
