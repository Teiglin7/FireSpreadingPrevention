import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.Stack;
import java.util.stream.Collectors;

import com.codingame.game.Coord;
import com.codingame.game.GridModule.Cell;

/*
 * An example AI for the game.
 */

public class Solution {
	
	static class Cell implements Comparable<Cell> {
		int fireDuration = 0;
		int value = 0;
		int cuttingDuration = 1;
		Coord pos;
		boolean safe = false;

		boolean mark = false;
		int baseFireTime = 0;
		int fireTime = 0;
		int valueFlow = 0;
		Cell fireSource;
		int adjValue = 0;
		
		int state = 0;
		
		@Override
		public int compareTo(Cell otherCell) {
			return Integer.compare(baseFireTime, otherCell.baseFireTime);
		}
		
		@Override
		public String toString() {
			return String.format("{(%s) %d %d}", pos, baseFireTime, cuttingDuration);
		}
		
		public static class FireTimeComparator implements Comparator<Cell> {
		    @Override
		    public int compare(Cell cell1, Cell cell2) {
		        return Integer.compare(cell1.fireTime, cell2.fireTime);
		    }
		}
	}
	
	static int width = 0;
	static int height = 0;
	static Cell[][] grid = null;
	static int currentMark = 0;
	
	
	static int computeSpreading(Coord start, boolean computeFlow, List<Cell> cells) {
		int burntValue = 0;
        for (Cell[] line : grid) {
            for (Cell cell : line) {
            	cell.mark = false;
            	cell.fireSource = null;
            	cell.fireTime = Integer.MAX_VALUE;
            	cell.valueFlow = 0;
            }
        }
        Stack<Cell> cellStack = new Stack<Cell>();
		PriorityQueue<Cell> pendingCells = new PriorityQueue<Cell>(new Cell.FireTimeComparator());
		pendingCells.add(getCell(start));
		getCell(start).fireTime = 0;
		while (!pendingCells.isEmpty()) {
			Cell cell = pendingCells.poll();
			Coord pos = cell.pos;
			if (!cell.mark) {
				cell.mark = true;
				burntValue += cell.value;
				if (computeFlow)
					cellStack.add(cell);
				for (Coord dir : Coord.DIRECTIONS) {
    				Cell adjCell = getCell(pos.add(dir.x, dir.y));
    				if (!adjCell.mark && !adjCell.safe) {
    					int fireTime = cell.fireTime + cell.fireDuration;
    					if (fireTime < adjCell.fireTime) {
        					adjCell.fireTime = fireTime;
        					adjCell.fireSource = cell;
        					pendingCells.add(adjCell);
    					}
    				}
				}
			}
		}
		
		if (computeFlow) {
			while (!cellStack.empty()) {
				Cell cell = cellStack.pop();
				cell.valueFlow += cell.value;
				if (cell.fireSource != null)
					cell.fireSource.valueFlow += cell.valueFlow;
			}
		}
		
		return burntValue;
	}
	
	static boolean areCellsTreatable(List<Cell> cells) {
		Collections.sort(cells);
		int freeTime = 0;
		for (Cell cell : cells) {
			if (freeTime >= cell.baseFireTime)
				return false;
			freeTime += cell.cuttingDuration;
		}
		return true;
	}
	

	static boolean areCellsConnected(List<Cell> cells) {
	    // Initialisation
	    for (Cell cell : cells)
	    	cell.mark = false;
	    int count = 0;
	    Stack<Cell> pendingCells = new Stack<Cell>();
	    pendingCells.add(cells.get(0));
	    cells.get(0).mark = true;
	    // Parcours en profondeur
	    while (pendingCells.size() >= 1) {
	        Cell cell = pendingCells.pop();
	        ++count;
            for (Coord dir : Coord.DIRECTIONS_ALL) {
            	Cell adjCell = getCell(cell.pos.add(dir));
	            if (adjCell.state == 1 && !adjCell.mark) {
	            	pendingCells.push(adjCell);
	            	adjCell.mark = true;
	            }
            }
	    }
        // Connexe si toutes les cases ont été parcourues
	    return count == cells.size();
	}
	
	static List<Cell> searchSolution(Coord startPos) {
		// Pondération de la recherche des meilleurs reculs
	    double coefFlux = 1;
	    double coefInstantAC = -10;
	    double coefInstantNC = 0;
	    double coefValeurVs = 1;
	    double coefCout = -1;
	    double coefValeur = -10;
	    // Initialisation des cases
	    int totalValue = computeSpreading(startPos, false, new ArrayList<Cell>());
	    for (Cell[] line : grid)
	    	for (Cell cell : line)
	    		cell.baseFireTime = cell.fireTime;

	    Cell startCell = getCell(startPos);
	    List<Cell> cells = new ArrayList<Cell>();
	    cells.add(startCell);
	    startCell.safe = true;
	    int cuttingDuration = getCell(startPos).cuttingDuration;
	    int deletedValue = startCell.value;
	    startCell.state = 1;
	    // Initialisation du meilleur résultat
	    int bestValue = totalValue;
	    List<Cell> bestCells = null;
	    int bestTreatmentDuration = 0;
	    // Génération des parties de la forêt
	    while(true) {
	        int burntValue = computeSpreading(startPos, true, cells);
	        // Actualisation du meilleur résultat
	        System.err.println(String.format("Value=%d Best=%d, T=%d", burntValue + deletedValue, bestValue, areCellsTreatable(cells) ? 1 : 0));
	        
	        if (areCellsTreatable(cells)) {
	        	int newValue = burntValue + deletedValue;
	        	if (newValue > bestValue)
	        		break;
	        	if (newValue < bestValue) {
		        	bestValue = newValue;
		        	bestCells = new ArrayList<>(cells);
		        	bestTreatmentDuration = cuttingDuration;
		        }
	        }
	        // Recherche de la meilleure case à ajouter
	        Cell newCell = null;
	        for (Cell cell : cells) {
	            Coord adjPos = new Coord(0, 0);
	            for (adjPos.y = cell.pos.y - 1; adjPos.y <= cell.pos.y + 1; ++adjPos.y) {
	            	for (adjPos.x = cell.pos.x - 1; adjPos.x <= cell.pos.x + 1; ++adjPos.x) {
	            		Cell adjCell = getCell(adjPos);
	            		if (adjCell.state == 0 && adjCell.baseFireTime != Integer.MAX_VALUE && (newCell == null || adjCell.valueFlow > newCell.valueFlow))
	            			newCell = adjCell;
	            	}
	            }
	        }
	        // Finit si aucun ajout n'est possible
	        if (newCell == null)
	            break;
	        // Si l'ajout de la nouvelle case est possible, ...
	        List<Cell> cellsWithNewCell = new ArrayList<>(cells);
	        cellsWithNewCell.add(newCell);
	        if (areCellsTreatable(cellsWithNewCell)) {
	            // ...la case peut directement être rajoutée
	            cuttingDuration += newCell.cuttingDuration;
	            deletedValue += newCell.value;
	            newCell.state = 1;
	            newCell.safe = true;
	            cells.add(newCell);
	            System.err.println(String.format("-> Ajout: (%s)", newCell.pos));
	        }
	        else {
	            // ...sinon, il faut reculer une des cases actuelles
	            newCell = null;
	            double bestMoveValue = -Double.NEGATIVE_INFINITY;
	            int iOldCell = -1;
	            // Recherche de la case à déplacer
	            for (int i=0; i < cells.size(); ++i) {
	            	Cell oldCell2 = cells.get(i);
	            	oldCell2.state = 2;
	            	oldCell2.safe = false;
	                // Recherche de l'endroit où la déplacer
		            for (Cell cell : cells) {
	                    if (cells.size() <= 1 || oldCell2 != cell) {
	                        for (Coord dir : Coord.DIRECTIONS_ALL) {
	                        	Cell newCell2 = getCell(cell.pos.add(dir));
	                            double moveValue =
	                            		coefFlux * newCell2.valueFlow +
	                            		coefInstantAC * oldCell2.baseFireTime +
	                            		coefInstantNC * newCell2.baseFireTime +
	                            		coefCout * newCell2.cuttingDuration +
	                            		coefValeur * newCell2.value +
	                            		coefValeurVs * (newCell2.adjValue - oldCell2.adjValue);
	                            if (newCell2.state == 0 && newCell2.baseFireTime != Integer.MAX_VALUE &&
	                            	(newCell == null || moveValue > bestMoveValue))
                            	{
	                                cells.set(i, newCell2);
	                                newCell2.state = 1;
	                                if (areCellsConnected(cells)) {
	                                	newCell = newCell2;
	                                	iOldCell = i;
	                                	bestMoveValue = moveValue;
	                                }
	                                newCell2.state = 0;
	                                cells.set(i, oldCell2);
                            	}
	                        }
	                    }
		            }
		            oldCell2.state = 1;
	            	oldCell2.safe = true;
	            }
	            // Finit si aucun recul n'est possible
	            if (newCell == null)
	                break;
	            Cell oldCell = cells.get(iOldCell);
	            // Recul de la case et actualisation des données
	            oldCell.state = 2;
	            oldCell.safe = false;
	            cuttingDuration += newCell.cuttingDuration - oldCell.cuttingDuration;
	            deletedValue += newCell.value - oldCell.value;
	            newCell.state = 1;
	            newCell.safe = true;
	            System.err.println(String.format("-> Recul (%s) -> (%s)", oldCell.pos, newCell.pos));
	            cells.set(iOldCell, newCell);
	        }
	    }
	    return bestCells;
	}
	

	static boolean isInGrid(Coord pos) {
    	return pos.x >= 0 && pos.x < width && pos.y >= 0 && pos.y < height;
	}
	static Cell getCell(Coord pos) {
		return grid[pos.y][pos.x];
	}
	
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        int treeTreatmentDuration = scanner.nextInt();
        int treeFireDuration = scanner.nextInt();
        int treeValue = scanner.nextInt();
        int houseTreatmentDuration = scanner.nextInt();
        int houseFireDuration = scanner.nextInt();
        int houseValue = scanner.nextInt();
        
        width = scanner.nextInt();
        height = scanner.nextInt();
        grid = new Cell[height][width];

        Coord fireStart = new Coord(0, 0);
        fireStart.x = scanner.nextInt();
        fireStart.y = scanner.nextInt();

        scanner.nextLine();
        
        for (int y=0; y < height; ++y) {
            for (int x=0; x < width; ++x) {
            	grid[y][x] = new Cell();
            	grid[y][x].pos = new Coord(x, y);
            }
        }

        for (int y=0; y < height; ++y) {
            String line = scanner.nextLine();
            for (int x=0; x < width; ++x) {
            	Cell cell = grid[y][x] = new Cell();
            	cell.pos = new Coord(x, y);
            	switch (line.charAt(x)) {
            	case '#':
            		cell.safe = true;
            		cell.fireDuration = 0;
            		cell.cuttingDuration = 0;
            		cell.value = 0;
            		break;
            	case '.':
            		cell.safe = false;
            		cell.fireDuration = treeFireDuration;
            		cell.cuttingDuration = treeTreatmentDuration;
            		cell.value = treeValue;
            		break;
            	case 'X':
            		cell.safe = false;
            		cell.fireDuration = houseFireDuration;
            		cell.cuttingDuration = houseTreatmentDuration;
            		cell.value = houseValue;
            		break;
            	default:
            		System.err.println("Invalid input: unknown char '" + line.charAt(x) + "' for cell.");
            		break;
            	}
            }
        }
        
        for (Cell[] line : grid) {
            for (Cell cell : line) {
            	for (Coord dir : Coord.DIRECTIONS) {
            		Coord adjPos = cell.pos.add(dir);
            		if (isInGrid(adjPos)) {
                		Cell adjCell = getCell(adjPos);
                		if (adjCell.value > cell.adjValue)
                			cell.adjValue = adjCell.value;
            		}
            	}
            }
        }
        
        List<Cell> cells = searchSolution(fireStart);
		Collections.sort(cells);
        for (Cell cell : cells) {
        	System.err.println(cell.pos + " - " + cell.baseFireTime);
        }
        
        int i = 0;

        while (true) {
        	int cooldown = scanner.nextInt();
            for (int y=0; y < height; ++y) {
                for (int x=0; x < width; ++x) {
                	scanner.nextInt();
                }
            }
            
            if (cooldown == 0 && i < cells.size()) {
                System.out.println(cells.get(i).pos.toString());
                i += 1;
            }
            else
            	System.out.println("WAIT");
        }
    }
}
