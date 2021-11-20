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
import com.codingame.gameengine.module.entities.TextBasedEntity.TextAlign;
import com.codingame.gameengine.module.entities.Rectangle;
import com.google.inject.Inject;

public class Referee extends AbstractReferee {
    @Inject private SoloGameManager<Player> gameManager;
    @Inject private GraphicEntityModule graphicEntityModule;
    @Inject GridModule gridModule;

    Rectangle infoBackground = null;
    Text infoText = null;
    
    final int firstTurnMaxTimeMs = 5000;
    final int turnMaxTimeMs = 100;
    
    int width = 0;
    int height = 0;
    
    int cuttingCooldown = 0;

    @Override
    public void init() {
        gameManager.setFrameDuration(500);
        gameManager.setFirstTurnMaxTime(firstTurnMaxTimeMs);
        gameManager.setTurnMaxTime(turnMaxTimeMs);
        gameManager.setMaxTurns(201);
        
        List<String> testCaseLines = gameManager.getTestCaseInput();

        gridModule.init(testCaseLines);
        
        width = gridModule.getWidth();
        height = gridModule.getHeight();
        
        System.out.println(width + "x" + height);

        for (String line : testCaseLines)
        	gameManager.getPlayer().sendInputLine(line);

        infoBackground = graphicEntityModule.createRectangle()
        		.setZIndex(1000)
        		.setX(0).setY(0)
        		.setWidth(250).setHeight(150)
        		.setFillColor(0x0)
        		.setFillAlpha(0.5);
        infoText = graphicEntityModule.createText()
        		.setX(10).setY(10)
        		.setZIndex(1001)
        		.setFillColor(0xFFFFFF);
        
        /*String[] cutSprites = graphicEntityModule.createSpriteSheetSplitter()
        	    .setSourceImage("cut.png")
        	    .setImageCount(5)
        	    .setWidth(512).setHeight(512)
        	    .setOrigRow(0).setOrigCol(0)
        	    .setImagesPerRow(5)
        	    .setName("cut")
        	    .split();

    	SpriteAnimation cutAnim = graphicEntityModule.createSpriteAnimation()
    	    .setImages(cutSprites)
    	    .setX(50).setY(50)
    	    .setScale(1.0)
    		.setZIndex(999)
    	    .setLoop(true);*/
        
        cuttingCooldown = 0;
        
        updateView();
    }

    @Override
    public void gameTurn(int turn) {

        if (cuttingCooldown > 0)
        	--cuttingCooldown;

        gameManager.getPlayer().sendInputLine(Integer.toString(cuttingCooldown));
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
        
        List<String> outputs = null;
        try {
        	outputs = gameManager.getPlayer().getOutputs();
            Coord pos = parseOutput(outputs);
            if (pos != null) {
            	if (cuttingCooldown == 0) {
            		if (pos.x < 0 || pos.x >= width || pos.y < 0 || pos.y >= height)
                        endGame("Invalid cutting cell {x=" + pos.x + ", y=" + pos.y + "}");
            		if (gridModule.getCell(pos).fireProgress == -2)
                        endGame("You tried to cut a cell which is already safe");
            		else if (gridModule.getCell(pos).fireProgress == gridModule.getCell(pos).fireDuration)
                        endGame("You tried to cut a cell which is already burnt");
            		else if (gridModule.getCell(pos).fireProgress >= 0)
                        endGame("You tried to cut a cell which is in fire");
            		else {
                    	gridModule.cutCell(pos);
                    	cuttingCooldown = gridModule.getCell(pos).cuttingDuration;
            		}
            	}
            	else {
                    endGame("Unauthorized action, you have to WAIT while your cuttingCooldown is > 0");
            	}
            }
            if (!gameManager.isGameEnd()) {
                if (gridModule.getNActiveFire() <= 0) {
                	endGame("Fire extinguished");
                }
                else if (turn >= 200) {
                	endGame("Max turns reached");
                }
            }
        } catch (TimeoutException e) {
        	endGame("Timeout (" + (turn == 1 ?
        			("first turn max time = " + firstTurnMaxTimeMs) :
        			("turn max time = " + turnMaxTimeMs)
        	) + " ms)");
            System.err.println("Timeout!");
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

    	gridModule.propagate();

        updateView();
    }
    
    void endGame(String cause) {
		while (gridModule.getNActiveFire() > 0)
	    	gridModule.propagate();
        gameManager.winGame("Game over: " + cause + ".\nScore (remaining value): " + gridModule.getRemainingValue());
    }

    @Override
    public void onEnd() {
        gameManager.putMetadata("remainingValue", String.valueOf(gridModule.getRemainingValue()));
    }

    private void updateView() {
    	infoText.setText(
    			"Remaining value: " + gridModule.getRemainingValue() + " / " + (gridModule.getTotalValue()) +
    			"\nBurnt value: " + gridModule.getBurntValue() +
    			"\nCut value: " + gridModule.getCutValue() +
    			"\nActive fires: " + gridModule.getNActiveFire() +
    			"\nCutting cooldown: " + cuttingCooldown);
    }

    private Coord parseOutput(List<String> outputs) {
    	String error = "";
        if (outputs.size() != 1) {
        	error = "You sent too many lines in your turn.";
        } else {
            String output = outputs.get(0);
        	if (output.equalsIgnoreCase("WAIT"))
        		return null;
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
            error = String.format("Expected output: WAIT or [x] [y] but received %s.\n", output) + error;
        }
        gameManager.loseGame(error);
        return null;
    }
}
