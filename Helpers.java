import java.util.*;

public class Helpers {

    // row corresponding to the letter when the board is like
    /*
     *   A B C D E F G H I
     * 1 
     * 2 
     * 3
     * 4
     * 5
     * 6
     * 7
     * 8
     * 9
     */
    public static int rowFromChar(char ch) {
        ch = Character.toUpperCase(ch);
        if (ch < 'A' || ch > 'I') throw new IllegalArgumentException("Row must be A..I");
        return (ch - 'A') + 1; // A=1,...I=9 etc.
    }

    // cols: '1'..'9' -> 1..9
    public static int colFromChar(char ch) {
        if (ch < '1' || ch > '9') throw new IllegalArgumentException("Col must be 1..9");
        return (ch - '1') + 1; // char operation, '9' - '1' +1 = 9 for example, to turn '9' to 9
    }

    // Parse inputs like "A2A3A4stop" or "A2 A3 A4" or "a2,b5,c9"
    // Stops on "stop"
    public static ArrayList<Cell> parseCellsLine(String s) {
        ArrayList<Cell> out = new ArrayList<>();
        if (s == null) return out;
        s = s.trim(); // gets rid of everything but alphanumeric values

        int i = 0;
        int n = s.length();
        while (i < n) {
            char ch = s.charAt(i);
            // skip separators
            if (Character.isWhitespace(ch) || ch == ',' || ch == '-') {
                i++;
                continue;
            }
            // stop or done
            String tail = s.substring(i).toLowerCase(); // get the rest, starting from current index
            if (tail.startsWith("stop") || tail.startsWith("done")) break; // if "stop", get out of while

            if (i + 1 >= n) throw new IllegalArgumentException("Bad cell token near end."); // if the last char is invalid, incomplete

            int col = rowFromChar(s.charAt(i)); // eg. A->1
            int row = colFromChar(s.charAt(i + 1)); // eg. 1->1
            out.add(new Cell(row, col));
            i += 2; // skip by 2 when normally added, parse over 2 chars like A3
        }
        return out;
    }

    // turn a list of cells into parallel row array
    public static int[] toRows(ArrayList<Cell> cells) {
        int[] rows = new int[cells.size()];
        for (int i = 0; i < cells.size(); i++) rows[i] = cells.get(i).getRow();
        return rows;
    }

    // turn a list of cells into parallel col array
    public static int[] toCols(ArrayList<Cell> cells) {
        int[] cols = new int[cells.size()];
        for (int i = 0; i < cells.size(); i++) cols[i] = cells.get(i).getCol();
        return cols;
    }

    // index helpers for exact cover (0 based)
    public static int cellIndex0(int r1to9, int c1to9) {
        return (r1to9 - 1) * 9 + (c1to9 - 1); // 0..80
    }

    public static int blockIndex0(int r1to9, int c1to9) {
        int r0 = r1to9 - 1;
        int c0 = c1to9 - 1;
        return (r0 / 3) * 3 + (c0 / 3); // 0..8
    }

    // make all k length set of permutations of distinct digits 1-9 that sum to targetSum
    // each int[] has length k
    public static void genPermsSumDistinct(int k, int target, ArrayList<int[]> out) { 
        boolean[] used = new boolean[10]; // 1-9
        int[] curr = new int[k];
        backtrackPerm(0, k, target, used, curr, out); // pos defaulted to 0
    }

    private static void backtrackPerm(int pos, int k, int remain,
                                      boolean[] used, int[] curr, ArrayList<int[]> out) {
        if (pos == k) {
            if (remain == 0) out.add(Arrays.copyOf(curr, k));
            return;
        }
        for (int d = 1; d <= 9; d++) {
            if (used[d]) continue; // already used, continue
            if (d > remain) continue; // too large, continue
            used[d] = true; // update used[]
            curr[pos] = d; // include d in partial solution in curr[] in index pos
            backtrackPerm(pos + 1, k, remain - d, used, curr, out); // recursively call the function
            used[d] = false; // after recursing with d, make it false again and continue recursing
        }
    }

    // helper to render one 3-char tile with bg color and optional number on top
    private static String tile(int num, int id, String[] colors, String whitetext, String reset) {
        String content = (num == 0) ? " . " : (" " + num + " ");
        if (id >= 0) {
            String msg = colors[id % colors.length];
            return msg + whitetext + content + reset;
        } else {
            return content;
        }
    }

    // Prints a Killer Sudoku board with A..I across the top and 1..9 down the side.
    // Cages are shown as colored tiles; the number clues are printed on top of the color.
    // A legend at the bottom shows one sample colored tile per cage and its target sum.
    public static void render(Puzzle p) {
        // Build cage-id grid (-1 = no cage)
        int[][] cageId = new int[Puzzle.N][Puzzle.N];
        for (int r = 0; r < Puzzle.N; r++) Arrays.fill(cageId[r], -1);

        ArrayList<Cage> cages = p.getCages();
        for (int k = 0; k < cages.size(); k++) {
            for (Cell ce : cages.get(k).getCells()) {
                cageId[ce.getRow() - 1][ce.getCol() - 1] = k;
            }
        }

        // ANSI colors
        final String reset = "\u001B[0m";
        final String whitetext = "\u001B[97m"; // white text
        final String[] colors = {
            "\u001B[41m",  // red
            "\u001B[43m",  // yellow
            "\u001B[42m",  // green
            "\u001B[44m",  // blue
            "\u001B[45m",  // magenta
            "\u001B[46m",  // cyan
            "\u001B[100m", // gray
            "\u001B[101m", // bright red
            "\u001B[102m", // bright green
            "\u001B[104m", // bright blue
            "\u001B[105m", // bright magenta
            "\u001B[106m", // bright cyan
            "\u001B[103m"  // bright yellow
        };

        

        // Column header: A..I
        System.out.print("   "); // left margin for row labels
        for (int c = 0; c < 9; c++) {
            char ch = (char) ('A' + c);
            System.out.print(" " + ch + " ");
        }
        System.out.println();

        // Rows: 1..9 with colored tiles
        for (int r = 1; r <= 9; r++) {
            System.out.printf("%d ", r); // row label (1..9)
            if (r < 10) System.out.print(" "); // keep alignment
            for (int c = 1; c <= 9; c++) {
                int g = p.getGiven(r, c);           // 0 if empty
                int id = cageId[r - 1][c - 1];      // -1 if not in a cage
                System.out.print(tile(g, id, colors, whitetext, reset));
            }
            System.out.println();
        }

        // Legend
    if (!cages.isEmpty()) {
        System.out.println();
        System.out.println("Key (tile color = sample cell: sum):");
        int perLine = 4; // a little wider since labels are longer
        int count = 0;
        for (int k = 0; k < cages.size(); k++) {
            Cage cg = cages.get(k);
            int sum = cg.getTargetSum();

            // pick a sample coordinate from this cage (first cell) to remove ambiguity on which colored cage (there can be two red cages if there are a lot, for instance)
            Cell sampleCell = cg.getCells().get(0);

            String sample = colors[k % colors.length] + whitetext + "   " + reset;
            String label = Helpers.label(sampleCell.getRow(), sampleCell.getCol());
            System.out.print(sample + " " + label + ": " + sum + "    ");
            count++;
            if (count % perLine == 0) System.out.println();
        }
        if (count % perLine != 0) System.out.println();
    }
    }

    // Convert (row=1..9, col=1..9) -> "A1".."I9"
    public static String label(int row1to9, int col1to9) {
        char colChar = (char)('A' + (col1to9 - 1));
        return "" + colChar + row1to9;
    }
}

