package com.codingame.game;

import java.util.HashMap;
import java.util.Map;

import com.codingame.gameengine.module.entities.World;

public class Constants {
    public static final int VIEWER_WIDTH = World.DEFAULT_WIDTH;
    public static final int VIEWER_HEIGHT = World.DEFAULT_HEIGHT;
    
    public static final int CELL_SIZE = 128;
    public static final int CELL_OFFSET = CELL_SIZE / 2;
    
    public static final String DIRT_SPRITE = "tileDirt.png";
    public static final String GRASS_SPRITE = "tileGrass.png";
    public static final String BACKGROUND_SPRITE = "background.png";
}
