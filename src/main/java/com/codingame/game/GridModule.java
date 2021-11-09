package com.codingame.game;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import com.codingame.gameengine.core.AbstractPlayer;
import com.codingame.gameengine.core.GameManager;
import com.codingame.gameengine.core.Module;
import com.codingame.gameengine.module.entities.Sprite;
import com.codingame.gameengine.module.entities.SpriteAnimation;
import com.google.inject.Inject;

public class GridModule implements Module {

    private GameManager<AbstractPlayer> gameManager;
    
    public int height;
    public int width;
    public int remainingValue = 0;
    public int burntValue = 0;
    public int cutValue = 0;
    public int nActiveFire = 0;
    
    Coord cuttingPos = new Coord(-1, -1);
    int cuttingCooldown = 0;
    

    int treeCuttingDuration = 0;
    int treeFireDuration = 0;
    int treeValue = 0;
    int houseCuttingDuration = 0;
    int houseFireDuration = 0;
    int houseValue = 0;
    
    int maxLostValue = 0;
    
    enum CellType {
    	SAFE,
    	TREE,
    	HOUSE,
    }

    public class Cell {
    	CellType type;
    	int fireDuration = 0;
    	int fireProgress = -1;
    	int cuttingDuration = 1;
    	int value = 0;
    }
    Cell[][] grid = null;
    
    static String getValueCode(int value) {
    	return new String(new char[] {(char)(65 + (value + 338) / 26), (char)(65 + (value + 338) % 26)});
    }

    @Inject
    GridModule(GameManager<AbstractPlayer> gameManager) {
        this.gameManager = gameManager;
        gameManager.registerModule(this);
    }
    
    void init(List<String> data) {

        int k = 0;
        String[] words;

        maxLostValue = Integer.parseInt(data.get(k++));
        
        words= data.get(k++).split(" ");
        treeCuttingDuration = Integer.parseInt(words[0]);
        treeFireDuration = Integer.parseInt(words[1]);
        treeValue = Integer.parseInt(words[2]);
        
        words= data.get(k++).split(" ");
        houseCuttingDuration = Integer.parseInt(words[0]);
        houseFireDuration = Integer.parseInt(words[1]);
        houseValue = Integer.parseInt(words[2]);
        
        words= data.get(k++).split(" ");
        width = Integer.parseInt(words[0]);
        height = Integer.parseInt(words[1]);
        grid = new Cell[height][width];
        
        Coord fireStart = new Coord(0, 0);
        words = data.get(k++).split(" ");
        fireStart.x = Integer.parseInt(words[0]);
        fireStart.y = Integer.parseInt(words[1]);
        
        for (int y=0; y < height; ++y) {
            String line = data.get(k++);
            for (int x=0; x < width; ++x) {
            	Cell cell = grid[y][x] = new Cell();
            	switch (line.charAt(x)) {
            	case '#':
            		cell.type = CellType.SAFE;
            		cell.fireDuration = 0;
            		cell.cuttingDuration = 0;
            		cell.value = 0;
            		cell.fireProgress = -2;
            		break;
            	case '.':
            		cell.type = CellType.TREE;
            		cell.fireDuration = treeFireDuration;
            		cell.cuttingDuration = treeCuttingDuration;
            		cell.value = treeValue;
            		cell.fireProgress = -1;
            		break;
            	case 'X':
            		cell.type = CellType.HOUSE;
            		cell.fireDuration = houseFireDuration;
            		cell.cuttingDuration = houseCuttingDuration;
            		cell.value = houseValue;
            		cell.fireProgress = -1;
            		break;
            	default:
            		System.err.println("Invalid input: unknown char '" + line.charAt(x) + "' for cell.");
            		break;
            	}
                remainingValue += cell.value;
            }
        }
        
        setFire(fireStart);
    }

    
    boolean isInGrid(Coord pos) {
    	return pos.x >= 0 && pos.x < width && pos.y >= 0 && pos.y < height;
    }
    Cell getCell(Coord pos) {
    	return grid[pos.y][pos.x];
    }
    
    public int getLostValue() {
    	return burntValue + cutValue;
    }
    
    public void setFire(Coord pos) {
    	if (getCell(pos).fireProgress < 0) {
    		getCell(pos).fireProgress = 0;
    		remainingValue -= getCell(pos).value;
    		burntValue += getCell(pos).value;
    		nActiveFire += 1;
    	}
    }
    public void cutCell(Coord pos) {
    	Cell cell = getCell(pos);
    	if (cell.fireProgress == -1) {
    		cell.fireProgress = -2;
    		remainingValue -= cell.value;
    		cutValue += cell.value;
    		cuttingPos = pos;
    		cuttingCooldown = cell.cuttingDuration;
    		
    	}
    }
    
    public void propagate() {
        for (int y=0; y < height; ++y) {
            for (int x=0; x < width; ++x) {
        		if (grid[y][x].fireProgress >= 0 && grid[y][x].fireProgress < grid[y][x].fireDuration) {
            		if ((++grid[y][x].fireProgress) >= grid[y][x].fireDuration)
            			nActiveFire -= 1;
        		}
            }
        }
        for (int y=0; y < height; ++y) {
            for (int x=0; x < width; ++x) {
        		if (grid[y][x].fireProgress == grid[y][x].fireDuration) {
        			for (Coord dir : Coord.DIRECTIONS) {
        				Coord adjPos = dir.add(x, y);
        				if (isInGrid(adjPos) && getCell(adjPos).fireProgress == -1)
                			setFire(adjPos);
        			}
        		}
            }
        }
    }
    
    void sendFrameData() {
		String viewData = "";
    	viewData += getValueCode(cuttingPos.x) + getValueCode(cuttingPos.y);
        for (int y=0; y < height; ++y) {
            for (int x=0; x < width; ++x) {
            	viewData += getValueCode(grid[y][x].fireProgress);
            }
        }
		gameManager.setViewData("GridModule", viewData);
		if (0 == --cuttingCooldown)
			cuttingPos.x = -1;
    }

	@Override
	public void onGameInit() {
		String viewData = "";
		viewData += maxLostValue + "\n";
		viewData += treeFireDuration + " " + treeCuttingDuration + " " + treeValue + "\n";
		viewData += houseFireDuration + " " + houseCuttingDuration + " " + houseValue + "\n";
		viewData += width + " " + height + "\n";
        for (int y=0; y < height; ++y) {
            for (int x=0; x < width; ++x) {
            	viewData += grid[y][x].type == CellType.SAFE ? '#' : grid[y][x].type == CellType.TREE ? '.' : 'X';
            }
            viewData += "\n";
        }
		gameManager.setViewGlobalData("GridModule", viewData);
		sendFrameData();
	}

	@Override
	public void onAfterGameTurn() {
		sendFrameData();
	}

	@Override
	public void onAfterOnEnd() {
		// TODO Auto-generated method stub
		
	}

}
