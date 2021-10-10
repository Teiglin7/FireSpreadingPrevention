package com.codingame.game;

import java.util.Arrays;

import com.codingame.gameengine.core.AbstractPlayer;
import com.codingame.gameengine.core.GameManager;
import com.codingame.gameengine.core.Module;
import com.codingame.gameengine.module.entities.Sprite;
import com.codingame.gameengine.module.entities.SpriteAnimation;
import com.google.inject.Inject;

public class GridModule implements Module {

    private GameManager<AbstractPlayer> gameManager;
    String[] layers;
    int height;
    int width;

    public class Cell {
    	int fireSpeed = 0;
    	int fireProgress = -1;
    	int value = 0;
    }
    Cell[][] grid = null;
    
    String getValueCode(int value) {
    	return new String(new char[] {(char)(65 + (value + 338) / 26), (char)(65 + (value + 338) % 26)});
    }
    
    public void propagate() {
        for (int y=0; y < height; ++y) {
            for (int x=0; x < width; ++x) {
            	if (grid[y][x].fireProgress >= 0) {
            		if (grid[y][x].fireProgress < 100) {
                		grid[y][x].fireProgress += grid[y][x].fireSpeed;
                		if (grid[y][x].fireProgress > 100)
                			grid[y][x].fireProgress = 100;
            		}
            	}
            }
        }
        for (int y=0; y < height; ++y) {
            for (int x=0; x < width; ++x) {
        		if (grid[y][x].fireSpeed > 0 && grid[y][x].fireProgress >= 100) {
            		if (y > 0 && grid[y - 1][x].fireProgress < 0)
            			grid[y - 1][x].fireProgress = 0;
            		if (y < height - 1 && grid[y + 1][x].fireProgress < 0)
            			grid[y + 1][x].fireProgress = 0;
            		if (x > 0 && grid[y][x - 1].fireProgress < 0)
            			grid[y][x - 1].fireProgress = 0;
            		if (x < width - 1 && grid[y][x + 1].fireProgress < 0)
            			grid[y][x + 1].fireProgress = 0;
        		}
            }
        }
    }

    @Inject
    GridModule(GameManager<AbstractPlayer> gameManager) {
        this.gameManager = gameManager;
        gameManager.registerModule(this);
    }
    
    void init(int height, int width) {
        this.height = height;
        this.width = width;
        grid = new Cell[width][height];
        for (int y=0; y < height; ++y) {
            for (int x=0; x < width; ++x)
            	grid[y][x] = new Cell();
        }
    }
    
    void sendFrameData() {
		String viewData = "";
        for (int y=0; y < height; ++y) {
            for (int x=0; x < width; ++x) {
            	viewData += getValueCode(grid[y][x].fireProgress);
            }
        }
		gameManager.setViewData("GridModule", viewData);
    }

	@Override
	public void onGameInit() {
		String viewData = getValueCode(height) + getValueCode(width);
        for (int y=0; y < height; ++y) {
            for (int x=0; x < width; ++x) {
            	viewData += getValueCode(grid[y][x].value) + getValueCode(grid[y][x].fireSpeed);
            }
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
