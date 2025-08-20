

public class Cell {
    //instance vars
    private int row; // 1 to 9
    private int col; // 1 to 9

    //constructor
    public Cell(int row, int col)
    {
        if (row < 1 || row > 9 || col < 1 || col > 9)
            throw new IllegalArgumentException("Row/col must be 1..9");
        this.row = row;
        this.col = col;
    }

    //getters
    public int getRow()
    {
        return row;
    }
    public int getCol()
    {
        return col;
    }

    @Override 
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cell)) return false;
        Cell other = (Cell) o;
        return row == other.row && col == other.col; // if row and column are the same, true
    }

    @Override 
    public String toString() 
    { 
        return "(" + row + "," + col + ")"; 
    }

    public int cellNum()
    {
        return ((row-1)*10 + col);
    }
}