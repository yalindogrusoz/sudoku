import java.util.*;
/*
* DLX.java
*
* - Dancing Links (Algorithm X) core
* - KillerDLXBuilder: builds the exact-cover matrix for Killer Sudoku
*
* Note: column names are only for debugging/clarity; solver doesn't depend on them.
*/

public class DLX {

    // building blocks for the 2d "toroidal" (circular) doubly linked list; circular as in each node is contained in a row and column node where you can traverse up/down and left/right to complete the column and row rings respectively
    private static class Node {
        Node L, R, U, D; 
        public Column C;          // owning column header
        int rowId;         // ID we give this row so we can decode a solution
        Node() { L = R = U = D = this; }
    }

    // Column header is also a Node, but tracks "size" (# of 1s)
    private static class Column extends Node {
        int size = 0;
        final String name;
        Column(String name) 
        {
            this.name = name; 
            this.C = this; 
        }
    }

    private final Column head = new Column("head"); // sentinel value
    private final ArrayList<Column> cols = new ArrayList<>();
    private final ArrayList<Node> partial = new ArrayList<>(); // current partial solution (stack)
    private final ArrayList<int[]> solutions = new ArrayList<>(); // potentially an array of solutions if we want all; otherwise only 1 

    private static boolean stopAfterFirst = true; // 1 or all

    public static void setStopAfterFirst(boolean b) // inputted when printing intro
    {
        stopAfterFirst = b;
    }

    // --- ADD: cap on number of solutions we will collect ---
    private int maxSolutions = Integer.MAX_VALUE;
    public void setMaxSolutions(int k) { maxSolutions = (k <= 0) ? 1 : k; }
    // -------------------------------------------------------

    // Build header row with the given column names (left-to-right circular list).
    public DLX(String[] colNames) {
        head.L = head.R = head; // empty ring initially
        for (String nm : colNames) {
            Column c = new Column(nm);
            cols.add(c);
            // insert c just before head (append)
            c.R = head;
            c.L = head.L;
            head.L.R = c;
            head.L = c;
            // empty column (vertical ring to itself)
            c.U = c.D = c;
        }
    }

    // Add one exact-cover row: rowId helps us reconstruct the solution later.
    public void addRow(int rowId, int[] columnIndices) {
        if (columnIndices == null || columnIndices.length == 0) return;

        // Keep a horizontal ring of nodes for this row
        Node first = null;
        Node prev = null;

        // Sorting is not required but fine for determinism
        Arrays.sort(columnIndices);

        for (int idx : columnIndices) {
            Column c = cols.get(idx);
            Node n = new Node();
            n.C = c;
            n.rowId = rowId;

            // insert into bottom of column c (just above c itself)
            n.D = c;
            n.U = c.U;
            c.U.D = n;
            c.U = n;
            c.size++;

            // link horizontally into the row ring
            if (first == null) {
                first = n;
                n.L = n.R = n;
            } else {
                n.L = prev;
                n.R = prev.R;
                prev.R.L = n;
                prev.R = n;
            }
            prev = n;
        }
    }

    // for heuristics choose the column with the smallest size (fewest 1s). otherwise, randomly could be chosen but with a deterministic machine this is best practice
    private Column chooseColumn() {
        int best = Integer.MAX_VALUE;
        Column bestC = null;
        for (Node x = head.R; x != head; x = x.R) {
            Column c = (Column) x;
            if (c.size < best) { best = c.size; bestC = c; }
        }
        return bestC;
    }

    // Cover column c (remove it from the header row) and
    // remove rows that have a 1 in this column from all other columns they touch.
    private void cover(Column c) {
        c.R.L = c.L;
        c.L.R = c.R;
        for (Node i = c.D; i != c; i = i.D) { // traverse the column ring, stopping when loop back
            for (Node j = i.R; j != i; j = j.R) {
                // The following two lines are "unlinking" j to temporarily remove it
                j.D.U = j.U;
                j.U.D = j.D;
                j.C.size--;
            }
        }
    }

    // Undo "cover"
    private void uncover(Column c) {
        for (Node i = c.U; i != c; i = i.U) {
            for (Node j = i.L; j != i; j = j.L) {
                j.C.size++;
                //reverse the unlinking by pointing neighbors back at j
                j.D.U = j;
                j.U.D = j;
            }
        }
        c.R.L = c;
        c.L.R = c;
    }

    // Core recursive search
    private void search() {
        // --- ADD: stop early if we've already collected enough solutions ---
        if (solutions.size() >= maxSolutions) return;
        // -------------------------------------------------------------------

        // If header row is empty (meaning that all columns are covered and hence removed) solution found.
        if (head.R == head) {
            int[] rows = new int[partial.size()];
            for (int i = 0; i < partial.size(); i++) rows[i] = partial.get(i).rowId;
            solutions.add(rows);
            return;
        }

        Column c = chooseColumn();
        if (c == null || c.size == 0) return; // dead end (no nodes in the chosen column, meaning no rows to satisfy the column)

        cover(c);
        for (Node r = c.D; r != c; r = r.D) { // try each row with a node the chosen column
            partial.add(r);
            // cover all columns in this row
            for (Node j = r.R; j != r; j = j.R) cover(j.C);

            search(); // recurse
            if (stopAfterFirst && !solutions.isEmpty()) { // if only one solution desired and it is found, leave after undoing the last part
                // quick exit; unwind minimal necessary to be safe
                for (Node j = r.L; j != r; j = j.L) uncover(j.C);
                partial.remove(partial.size() - 1);
                uncover(c);
                return;
            }

            // backtrack
            for (Node j = r.L; j != r; j = j.L) uncover(j.C);
            partial.remove(partial.size() - 1);

            // --- ADD: also stop iterating if we hit the cap ---
            if (solutions.size() >= maxSolutions) { uncover(c); return; }
            // ---------------------------------------------------
        }
        uncover(c);
    }

    /* Solve and return the first solution as an array of rowIds (or null if none). */
    public int[] solveFirst() {
        stopAfterFirst = true;
        search();
        return solutions.isEmpty() ? null : solutions.get(0);
    }

    /* Solve and return all solutions (careful: can be many). */
    // Here, implementation can be changed to find a number like 5 solutions instead of all. No one really cares about the exact number of solutions if too big, and it will take too long.  
    public ArrayList<int[]> solveAll() {
        stopAfterFirst = false;
        search();
        return solutions;
    }

    // Small dynamic int array (so we can avoid boxing)
    private static class IntArrayBuilder {
        int[] a;
        int n = 0;
        IntArrayBuilder(int cap) { a = new int[Math.max(4, cap)]; }
        void add(int x) {
            if (n == a.length) a = Arrays.copyOf(a, a.length * 2); // double size whenever needed
            a[n++] = x;
        }
        int[] toArray() { return Arrays.copyOf(a, n); }
    }

    /** Packs enough info to rebuild digits in the grid after DLX picks this row. */
    static class RowDecode {
        final int cageIndex;
        final int[] rows;   // r (1..9)
        final int[] cols;   // c (1..9)
        final int[] digits; // d (1..9)
        RowDecode(int cageIndex, int[] rows, int[] cols, int[] digits) {
            this.cageIndex = cageIndex;
            this.rows = rows; this.cols = cols; this.digits = digits;
        }
    }

    /**
     * KillerDLXBuilder
     *
     * Columns =
     *   81 cell constraints          (each (r,c) once)
     *   81 row-digit constraints     (each (r,d) once)
     *   81 col-digit constraints     (each (c,d) once)
     *   81 box-digit constraints     (each (box,d) once)
     *   #cages cage-choice columns   (pick exactly one assignment per cage)
     *
     * Rows =
     *   For each cage of size k, for each k-length permutation of distinct digits that sum to target:
     *     map those digits to the k cells of the cage (in stored order),
     *     then set 1s in the 4 Sudoku constraints + the cage column.
     *
     * Givens are respected by filtering: if a cell has a given g, only rows with d=g survive.
     */
    static class KillerDLXBuilder {
        private final Puzzle puzzle;
        private final ArrayList<RowDecode> decodes = new ArrayList<>();

        KillerDLXBuilder(Puzzle puzzle) { this.puzzle = puzzle; }

        private static int cellColIndex(int r, int c) {               // 0..80
            return (r - 1) * 9 + (c - 1);
        }
        private static int rowDigitColIndex(int r, int d) {           // 81..161
            return 81 + (r - 1) * 9 + (d - 1);
        }
        private static int colDigitColIndex(int c, int d) {           // 162..242
            return 162 + (c - 1) * 9 + (d - 1);
        }
        private static int boxDigitColIndex(int b, int d) {           // 243..323
            return 243 + b * 9 + (d - 1);
        }

        public DLX build() {
            ArrayList<Cage> cages = puzzle.getCages();

            // Column count (fixed 324 + #cages)
            final int NUM_COLS = 324 + cages.size();

            // Make human-friendly names (purely for debugging)
            String[] names = new String[NUM_COLS];
            // 81 cells
            for (int r = 1; r <= 9; r++)
                for (int c = 1; c <= 9; c++)
                    names[cellColIndex(r, c)] = "Cell(r=" + r + ",c=" + c + ")";
            // 81 row-digit
            for (int r = 1; r <= 9; r++)
                for (int d = 1; d <= 9; d++)
                    names[rowDigitColIndex(r, d)] = "Row(r=" + r + ")#" + d;
            // 81 col-digit
            for (int c = 1; c <= 9; c++)
                for (int d = 1; d <= 9; d++)
                    names[colDigitColIndex(c, d)] = "Col(c=" + c + ")#" + d;
            // 81 box-digit (box index 0..8)
            for (int b = 0; b < 9; b++)
                for (int d = 1; d <= 9; d++)
                    names[boxDigitColIndex(b, d)] = "Box(b=" + b + ")#" + d;
            // cage columns at the end
            for (int k = 0; k < cages.size(); k++)
                names[324 + k] = "Cage#" + k;

            DLX dlx = new DLX(names);

            // Build rows
            int nextRowId = 0;
            for (int k = 0; k < cages.size(); k++) {
                Cage cg = cages.get(k);
                ArrayList<Cell> cells = cg.getCells();
                int K = cells.size();

                // Generate all K-length permutations of distinct digits summing to target
                ArrayList<int[]> perms = new ArrayList<>();
                Helpers.genPermsSumDistinct(K, cg.getTargetSum(), perms);

                // For each digit permutation, map digits -> cage's cells in stored order
                for (int[] perm : perms) {
                    // Filter by givens: if a cell has a given g, require perm[i] == g
                    boolean ok = true;
                    for (int i = 0; i < K && ok; i++) {
                        Cell ce = cells.get(i);
                        int given = puzzle.getGiven(ce.getRow(), ce.getCol());
                        if (given != 0 && given != perm[i]) ok = false;
                    }
                    if (!ok) continue;

                    // Build the set of column indices this row will cover
                    IntArrayBuilder colsToHit = new IntArrayBuilder(5 * K + 1);

                    // cage-choice column
                    colsToHit.add(324 + k);

                    // record for reconstruction
                    int[] rArr = new int[K];
                    int[] cArr = new int[K];
                    int[] dArr = new int[K];

                    for (int i = 0; i < K; i++) {
                        Cell ce = cells.get(i);
                        int r = ce.getRow(), c = ce.getCol(), d = perm[i];
                        int b = Helpers.blockIndex0(r, c); // 0..8

                        colsToHit.add(cellColIndex(r, c));
                        colsToHit.add(rowDigitColIndex(r, d));
                        colsToHit.add(colDigitColIndex(c, d));
                        colsToHit.add(boxDigitColIndex(b, d));

                        rArr[i] = r; cArr[i] = c; dArr[i] = d;
                    }

                    dlx.addRow(nextRowId, colsToHit.toArray());
                    decodes.add(new RowDecode(k, rArr, cArr, dArr));
                    nextRowId++;
                }
            }
            return dlx;
        }

        public ArrayList<RowDecode> getRowDecodes() { return decodes; }
    }
}
