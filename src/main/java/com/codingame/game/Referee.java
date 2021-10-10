package com.codingame.game;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.codingame.gameengine.core.AbstractPlayer.TimeoutException;
import com.codingame.gameengine.core.AbstractReferee;
import com.codingame.gameengine.core.SoloGameManager;
import com.codingame.gameengine.module.entities.Curve;
import com.codingame.gameengine.module.entities.GraphicEntityModule;
import com.codingame.gameengine.module.entities.Group;
import com.codingame.gameengine.module.entities.Sprite;
import com.codingame.gameengine.module.entities.SpriteAnimation;
import com.codingame.gameengine.module.entities.Text;
import com.google.inject.Inject;

public class Referee extends AbstractReferee {
    @Inject private SoloGameManager<Player> gameManager;
    @Inject private GraphicEntityModule graphicEntityModule;
    @Inject GridModule gridModule;
    
    int width = 0;
    int height = 0;
    
    private int eggsCollected = 0;

    @Override
    public void init() {
        gameManager.setFrameDuration(500);
        
        // Draw background
        //graphicEntityModule.createSprite().setImage(Constants.BACKGROUND_SPRITE);
        
        List<String> testCaseLines = gameManager.getTestCaseInput();
        
        Integer[] dim = Arrays.stream(testCaseLines.get(0).split(" "))
            .map(s -> Integer.valueOf(s))
            .toArray(size -> new Integer[size]);
        width = dim[0];
        height = dim[1];
        System.out.println(width + "x" + height);

        gridModule.init(height, width);
        
        for (int y=0; y < height; ++y) {
            String[] lineValues = testCaseLines.get(1 + y).split(" ");
            for (int x=0; x < width; ++x) {
            	gridModule.grid[y][x].fireSpeed = Integer.valueOf(lineValues[x]);
            	gridModule.grid[y][x].fireProgress = gridModule.grid[y][x].fireSpeed == 0 ? 100 : -1;
            }
        }
        for (int y=0; y < height; ++y) {
            String[] lineValues = testCaseLines.get(1 + y).split(" ");
            for (int x=0; x < width; ++x)
            	gridModule.grid[y][x].value = Integer.valueOf(lineValues[x]);
        }

        gridModule.grid[5][5].fireProgress = 0;
        

        /*for (int y=0; y < height; ++y) {
            for (int x=0; x < width; ++x) {
            	graphicEntityModule.createSprite().setImage(Constants.DIRT_SPRITE)
            			.setX(x * Constants.CELL_SIZE)
            			.setY(y * Constants.CELL_SIZE)
                        .setZIndex(0);
            }
        }

        String[] fireSpriteSheet = graphicEntityModule.createSpriteSheetSplitter()
    	    .setSourceImage("onfire.png")
    	    .setImageCount(9)
    	    .setWidth(128)
    	    .setHeight(128)
    	    .setOrigRow(0)
    	    .setOrigCol(0)
    	    .setImagesPerRow(3)
    	    .setName("onfire")
    	    .split();
        
        for (int y=0; y < height; ++y) {
            for (int x=0; x < width; ++x) {
            	grid[y][x].tileSprite = graphicEntityModule.createSprite().setImage(Constants.GRASS_SPRITE)
            			.setX(x * Constants.CELL_SIZE)
            			.setY(y * Constants.CELL_SIZE)
                        .setZIndex(1);
            	grid[y][x].fireSprite = graphicEntityModule.createSpriteAnimation()
                	    .setImages(fireSpriteSheet)
            			.setX(x * Constants.CELL_SIZE + Constants.CELL_OFFSET)
            			.setY(y * Constants.CELL_SIZE + Constants.CELL_OFFSET)
            			.setAnchor(0.5)
                	    .setLoop(true)
                        .setZIndex(2);
            }
        }*/
        
        
        /*for (int i = 0; i < eggsCount; i++) {
            Coord position = new Coord(testInputs[i * 3 + 2], testInputs[i * 3 + 3]);
            int eggsQuantity = testInputs[i * 3 + 4];
            eggs.put(position, eggsQuantity);
            
            String[] seaweedSprites = graphicEntityModule.createSpriteSheetSplitter()
        	    .setSourceImage("onfire.png")
        	    .setImageCount(9)
        	    .setWidth(128)
        	    .setHeight(128)
        	    .setOrigRow(0)
        	    .setOrigCol(0)
        	    .setImagesPerRow(3)
        	    .setName("onfire")
        	    .split();

        	//Creating an animation from the splitted spritesheet
            SpriteAnimation eggSprite = graphicEntityModule.createSpriteAnimation()
        	    .setImages(seaweedSprites)
        	    .setX(50)
        	    .setY(50)
        	    .setLoop(true)
        	    .setAnchor(.5)
                .setZIndex(1);

            Sprite eggSprite = graphicEntityModule.createSprite().setImage(Constants.EGGS_SPRITE)
                .setAnchor(.5)
                .setZIndex(1);
            
            Text eggText = graphicEntityModule.createText(String.valueOf(eggsQuantity))
                .setFontSize(60)
                .setFillColor(0x000000)
                .setAnchor(.5)
                .setZIndex(2);
            
            Group eggGroup = graphicEntityModule.createGroup(eggSprite, eggText)
                .setX(position.x * Constants.CELL_SIZE + Constants.CELL_OFFSET)
                .setY(position.y * Constants.CELL_SIZE + Constants.CELL_OFFSET);
            
            eggGroups.put(position, eggGroup);
            
            gameManager.getPlayer().sendInputLine(position.toString() + " " + eggsQuantity);
        }

        fishSprite = graphicEntityModule.createSprite().setImage(Constants.FISH_SPRITE)
            .setX(fishPosition.x * Constants.CELL_SIZE + Constants.CELL_OFFSET)
            .setY(fishPosition.y * Constants.CELL_SIZE + Constants.CELL_OFFSET)
            .setAnchor(.5)
            .setZIndex(2);*/

        for (String line : testCaseLines)
        	gameManager.getPlayer().sendInputLine(line);
        
        updateView();
    }

    @Override
    public void gameTurn(int turn) {
    	
    	gridModule.propagate();
        
        for (int y=0; y < height; ++y) {
        	String line = "";
            for (int x=0; x < width; ++x) {
            	if (x != 0)
            		line += " ";
            	line += gridModule.grid[y][x].fireProgress;
            }
            gameManager.getPlayer().sendInputLine(line);
        }

        gameManager.getPlayer().execute();

        try {
            List<String> outputs = gameManager.getPlayer().getOutputs();

            Coord pos = checkOutput(outputs);

        } catch (TimeoutException e) {
            gameManager.loseGame("Timeout!");
        }
        
        if (turn >= 30) {
            gameManager.winGame("Game over. Max turns reached.");
        }

        /*// Check if an egg is picked up
        if (eggs.containsKey(fishPosition)) {
            eggsCollected += eggs.get(fishPosition);
            eggs.remove(fishPosition);
            Group g = eggGroups.get(fishPosition);
            g.setScale(0);
        }
        
        // Check win condition
        if (fishPosition.x >= Constants.COLUMNS - 1 && eggsCollected > 0) {
            gameManager.winGame(String.format("Congrats! You collected %d eggs!", eggsCollected));
        }*/

        updateView();
    }

    @Override
    public void onEnd() {
        gameManager.putMetadata("eggs", String.valueOf(eggsCollected));
    }

    private void updateView() {
        /*fishSprite.setX(fishPosition.x * Constants.CELL_SIZE + Constants.CELL_OFFSET, Curve.LINEAR)
            .setY(fishPosition.y * Constants.CELL_SIZE + Constants.CELL_OFFSET, Curve.LINEAR);*/
        /*for (Cell[] cellLine : grid) {
            for (Cell cell : cellLine) {
            	cell.tileSprite.setAlpha(cell.fireSpeed == 0 ? 0.0 : ((100 - Math.max(0,cell.fireProgress)) / 100.0));
            	cell.fireSprite.setScale(cell.fireProgress >= 0 && cell.fireProgress < 100 ? Math.pow(cell.fireSpeed / 100.0, 0.5) : 0.0);
                tooltips.setTooltipText(cell.tileSprite, String.format(
                		"Value: %s\nFire speed: %s\nFire progress: %s",
                		cell.value, cell.fireSpeed, cell.fireProgress));
            }
        }*/
    }

    private Coord checkOutput(List<String> outputs) {
    	String error = "Test";
        if (outputs.size() != 1) {
        	error = "You did not send 1 output in your turn.";
        } else {
            String output = outputs.get(0);
            String[] words = output.split(" ");
            if (words.length == 2) {
            	Coord pos = new Coord(0, 0);
	            try{
	            	pos.x = Integer.valueOf(words[0]);
	            } catch(NumberFormatException e) {
	            	error = String.format("Cannot parse 1nd number \"%s\".", words[0]);
	            }
	            try{
	            	pos.y = Integer.valueOf(words[1]);
	            } catch(NumberFormatException e) {
	            	error = String.format("Cannot parse 2nd number \"%s\".", words[1]);
	            }
	            return pos;
            }
            else {
            	error = String.format("%s words instead of 2", words.length);
            }
            error = String.format("Expected output: [x] [y] but received %s. ", output) + error;
        }
        gameManager.loseGame(error);
        return null;
    }
}
