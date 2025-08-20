// This is for a 9x9 killer sudoku, modify as needed

import java.util.*;

/*
 * Main.java
 *
 * Driver for Killer Sudoku:
 *  - lets you enter cages interactively (e.g., "A1 A2 B1" then sum)
 *  - supports undo via "delete <cells>" (e.g., "delete A1A2A3")
 *  - renders your puzzle after each change (cages + legend)
 *  - builds DLX exact cover and solves
 *  - prints the solved grid
 *
 * Notes:
 *  - Letters are columns A..I, numbers are rows 1..9 (your Helpers already use that).
 *  - Make sure EVERY cell is in exactly one cage, otherwise it's unsatisfiable.
 *  - To enter a "given" (prefilled digit), just make a 1-cell cage: e.g., cells "A1" with sum "5".
 */

public class Main {


    // turn (row=1..9, col=1..9) into "A1".."I9"
    private static String label(int r, int c) 
    {
        char col = (char) ('A' + (c - 1));
        return "" + col + r;
    }

    // one-line coordinate + usage intro (printed once at start)
    // one-line coordinate + usage intro (printed once at start)
private static boolean printIntro(Scanner sc) {
    System.out.println("Coordinates guide:");
    System.out.println("  LETTER = column (A..I), NUMBER = row (1..9). Example: B7  => row 7, column 2");
    System.out.println("Undo tip:");
    System.out.println("  Type 'delete <cells>' anytime to undo a cage. Example: delete A1A2A3 (spaces/commas also fine)");
    System.out.println("Givens tip:");
    System.out.println("  Use a 1-cell cage to fix a digit. Example: cells 'A1' with sum '5' means A1=5.");
    System.out.println("Do you want all solutions, or one? Caution: if a very sparsely filled puzzle without a lot of clues is given, requesting all solutions might be problematic.");
    System.out.println("Type either 'all' or 'one'");

    String ans;
    do {
        ans = sc.nextLine().trim().toLowerCase();
    } while (!ans.equals("all") && !ans.equals("one"));

    System.out.println();
    return ans.equals("all"); // true ⇒ want all solutions
}

    // add one cage to the puzzle using a coordinate string like "A1 A2 B1"
    private static void addCage(Puzzle p, String coordLine, int sum) {
        ArrayList<Cell> cells = Helpers.parseCellsLine(coordLine);
        if (cells.isEmpty()) throw new IllegalArgumentException("No cells parsed for that cage.");
        int[] rows = Helpers.toRows(cells);
        int[] cols = Helpers.toCols(cells);
        p.addCage(new Cage(rows, cols, sum));
    }

    // internal lightweight representation while editing
    private static class CageDef {
        final ArrayList<Cell> cells;
        final int sum;
        CageDef(ArrayList<Cell> cells, int sum) { this.cells = cells; this.sum = sum; }
    }

    // build a temporary Puzzle from current cage defs (used to render after each change)
    private static Puzzle buildPuzzleFromDefs(List<CageDef> defs) {
        Puzzle temp = new Puzzle();
        for (CageDef d : defs) {
            int[] rows = Helpers.toRows(d.cells);
            int[] cols = Helpers.toCols(d.cells);
            temp.addCage(new Cage(rows, cols, d.sum));
        }
        return temp;
    }

    // canonical key for a set of cells (order-insensitive) to match deletes
    private static String keyForCells(ArrayList<Cell> cells) {
        int[] keys = new int[cells.size()];
        for (int i = 0; i < cells.size(); i++) {
            Cell ce = cells.get(i);
            keys[i] = (ce.getRow() - 1) * 9 + ce.getCol();
        }
        Arrays.sort(keys);
        return Arrays.toString(keys);
    }

    // read cages interactively until user types "done"
    private static void inputCages(Scanner sc, Puzzle p) {
        System.out.println("Enter cages. Example:");
        System.out.println("  Cells:  A1 A2 B1");
        System.out.println("  Sum:    12");
        System.out.println("Type 'done' when finished.\n");
        System.out.println("Remember, letters correspond to columns and numbers to rows. B7, for instance, is the 7th row and 2nd column.");

        // keep editable list locally; we only commit to 'p' when you're done
        ArrayList<CageDef> defs = new ArrayList<>();

        int idx = 1;
        while (true) {
            System.out.print("\nCage #" + idx + " cells (e.g., \"A1 A2 B1\" or 'done' or 'delete A1A2A3'): ");
            String cellsLine = sc.nextLine().trim();
            if (cellsLine.equalsIgnoreCase("done")) break;
            if (cellsLine.isEmpty()) { System.out.println("  (empty; try again)"); continue; }

            // handle deletion: "delete <cells>"
            if (cellsLine.toLowerCase().startsWith("delete")) {
                String rest = cellsLine.substring(6).trim(); // after "delete"
                try {
                    ArrayList<Cell> targetCells = Helpers.parseCellsLine(rest);
                    if (targetCells.isEmpty()) {
                        System.out.println("  Couldn't parse which cage to delete. Example: delete A1A2A3");
                    } else {
                        String targetKey = keyForCells(targetCells);
                        int removeAt = -1;
                        for (int i = 0; i < defs.size(); i++) {
                            if (keyForCells(defs.get(i).cells).equals(targetKey)) { removeAt = i; break; }
                        }
                        if (removeAt >= 0) {
                            defs.remove(removeAt);
                            System.out.println("  Deleted cage: " + rest);
                        } else {
                            System.out.println("  No cage matches those cells.");
                        }
                    }
                } catch (IllegalArgumentException e) {
                    System.out.println("  Couldn't parse delete request: " + e.getMessage());
                }

                // render current state after deletion
                try {
                    Puzzle preview = buildPuzzleFromDefs(defs);
                    Helpers.render(preview);
                } catch (IllegalArgumentException e) {
                    // shouldn't happen on deletion
                }
                continue;
            }

            // normal add path
            System.out.print("Cage #" + idx + " sum: ");
            String sumLine = sc.nextLine().trim();
            int sum;
            try {
                sum = Integer.parseInt(sumLine);
                if (sum <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                System.out.println("  Bad sum; please enter a positive integer.");
                continue;
            }

            // try adding (via preview) to catch overlaps immediately
            try {
                ArrayList<Cell> cells = Helpers.parseCellsLine(cellsLine);
                CageDef newDef = new CageDef(cells, sum);

                ArrayList<CageDef> test = new ArrayList<>(defs);
                test.add(newDef);

                Puzzle preview = buildPuzzleFromDefs(test); // will throw if overlap
                defs.add(newDef);                           // commit
                Helpers.render(preview);                   // visualize after each add
                idx++;
            } catch (IllegalArgumentException ex) {
                System.out.println("  Couldn't add cage: " + ex.getMessage());
                System.out.println("  (Tip: cells must be unique across cages.)");
            }
        }

        // commit all defs into the real puzzle
        for (CageDef d : defs) {
            int[] rows = Helpers.toRows(d.cells);
            int[] cols = Helpers.toCols(d.cells);
            p.addCage(new Cage(rows, cols, d.sum));
        }
        System.out.println();
    }

    // (kept from your original file; now unused—givens are 1-cell cages)
    private static boolean parseAndSetGiven(Puzzle p, String line) { /* unchanged; unused */ 
        line = line.trim();
        if (line.isEmpty()) return false;

        String cellStr = null;
        String digitStr = null;

        if (line.contains("=") || line.contains(":")) {
            String[] parts = line.split("[:=]");
            if (parts.length != 2) throw new IllegalArgumentException("Use forms like A1=5 or A1 5.");
            cellStr = parts[0].trim();
            digitStr = parts[1].trim();
        } else {
            String[] parts = line.split("\\s+");
            if (parts.length != 2) throw new IllegalArgumentException("Use forms like A1=5 or A1 5.");
            cellStr = parts[0].trim();
            digitStr = parts[1].trim();
        }

        if (cellStr.length() != 2)
            throw new IllegalArgumentException("Cell should look like A1..I9.");

        char colCh = Character.toUpperCase(cellStr.charAt(0));
        char rowCh = cellStr.charAt(1);

        int col = Helpers.rowFromChar(colCh);   // letter -> column index (1..9)
        int row = Helpers.colFromChar(rowCh);   // number -> row index (1..9)

        int d;
        try { d = Integer.parseInt(digitStr); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Digit must be 0..9."); }
        if (d < 0 || d > 9) throw new IllegalArgumentException("Digit must be 0..9.");

        p.setGiven(row, col, d);
        return true;
    }

    // sanity check that every cell is in exactly one cage
    private static boolean checkFullCoverage(Puzzle p) {
        boolean[][] mark = p.getInCage();
        ArrayList<String> missing = new ArrayList<>();
        for (int r = 1; r <= 9; r++) {
            for (int c = 1; c <= 9; c++) {
                if (!mark[r - 1][c - 1]) missing.add(label(r, c));
            }
        }
        if (!missing.isEmpty()) {
            System.out.println("⚠ Some cells are not in any cage: " + missing);
            return false;
        }
        return true;
    }

    // print a solved grid (simple ASCII, with A..I header)
    private static void printGrid(int[][] grid) {
        System.out.println("\nSolved grid:");
        System.out.print("   ");
        for (int c = 0; c < 9; c++) System.out.print(" " + (char)('A' + c) + " ");
        System.out.println();
        for (int r = 1; r <= 9; r++) {
            System.out.printf("%d ", r);
            if (r < 10) System.out.print(" ");
            for (int c = 1; c <= 9; c++) {
                int d = grid[r - 1][c - 1];
                System.out.print(d == 0 ? " . " : (" " + d + " "));
            }
            System.out.println();
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        Puzzle p = new Puzzle();
    
        // one-time intro and ask whether to stop after first
        boolean wantAll = printIntro(sc);
    
        // cages
        inputCages(sc, p);
    
        if (p.getCages().isEmpty()) {
            System.out.println("No cages were added. Exiting so you can try again.");
            return;
        }
    
        //  every cell covered by exactly one cage
        if (!checkFullCoverage(p)) {
            System.out.println("Please add cages so all 81 cells are covered, then run again.");
            return;
        }
    
        //  show puzzle (cage colors)
        Helpers.render(p);
    
        //  build DLX and solve
        DLX.KillerDLXBuilder builder = new DLX.KillerDLXBuilder(p);
        DLX dlx = builder.build();

        int[] chosenRowIds;
        ArrayList<DLX.RowDecode> decodes = builder.getRowDecodes();

        if (wantAll) {
            dlx.setMaxSolutions(10); // In this version, when you type "all" we cap at 10 solutions to avoid excessive memory use (not a memory leak).
            ArrayList<int[]> all = dlx.solveAll();
            if (all.isEmpty()) {
                System.out.println("\nNo solution found. (Check sums, distinctness, and single-cell givens.)");
                return;
            }

            System.out.println("Found " + all.size() + " solution(s). Printing all:");
            for (int s = 0; s < all.size(); s++) {
                int[][] grid = new int[9][9];
                for (int rowId : all.get(s)) {
                    DLX.RowDecode rd = decodes.get(rowId);
                    for (int i = 0; i < rd.rows.length; i++) {
                        int r = rd.rows[i], c = rd.cols[i], d = rd.digits[i];
                        grid[r - 1][c - 1] = d;
                    }
                }
                System.out.println("\nSolution " + (s + 1) + " of " + all.size() + ":");
                printGrid(grid);
            }
            return; // done printing all
        } else {
            chosenRowIds = dlx.solveFirst();
            if (chosenRowIds == null) {
                System.out.println("\nNo solution found. (Check sums, distinctness, and single-cell givens.)");
                return;
            }

            // reconstruct single solution and print
            int[][] grid = new int[9][9];
            for (int rowId : chosenRowIds) {
                DLX.RowDecode rd = decodes.get(rowId);
                for (int i = 0; i < rd.rows.length; i++) {
                    int r = rd.rows[i], c = rd.cols[i], d = rd.digits[i];
                    grid[r - 1][c - 1] = d;
                }
            }
            printGrid(grid);
        }
    }
}