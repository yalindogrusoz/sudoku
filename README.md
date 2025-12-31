# Killer Sudoku Solver with DLX

This repository is for a project during [Pioneer Academics](https://pioneeracademics.com/), mentored by [Prof. Carl Yerger](https://www.davidson.edu/people/carl-yerger), within the field of Combinatorics and Graph Theory. [Knuth's Algorithm X](https://arxiv.org/abs/cs/0011047), which efficiently solves the Exact Cover Problem using his data structure DLX (a 2D-doubly-linked-list), was adapted to Killer Sudoku for its efficient soltution.
Details of the work can be accessed [here](SudokuDLX.pdf).

## Execution Instructions
This implementation is done using Java.
After running, you will be asked to choose if you want one or all solutions
Typing "one" will print only the first solution found, if any
Typing "all" will print at most 10 solutions--no more is printed as excessive memory usage is prevented.

Then, cages will be inputted one by one.
A printed grid with coordinates of cells will be shown after each cage is entered for ease of input.
An example input is as follows:

Cage #23 cells (e.g., "A1 A2 B1" or 'done' or 'delete A1A2A3'): f3f4
Cage #23 sum: 5

And to undo a step (delete a created cage), you can do:
Cage #23 cells (e.g., "A1 A2 B1" or 'done' or 'delete A1A2A3'): delete f3f4
and the cage will be removed

After each cage addition, the updated grid is shown with color-coded grids and a legend to show each color grid (along with a cell's coordinates in case there are multiple of the same color) and its target sum.

### Sample Run
![alt text](<CageAddition.png>)

After all cages are added, type "done" and the algorithm will run. A result is shown below for a Killer Sudoku with 6 solutions.

![alt text](<SampleResult.png>)
