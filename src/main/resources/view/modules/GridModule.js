import { WIDTH, HEIGHT } from '../core/constants.js'
import { fitAspectRatio, lerpColor } from '../core/utils.js'
import { api as entityModule } from '../entity-module/GraphicEntityModule.js'
import { EntityFactory } from '../entity-module/EntityFactory.js'



function getMouseMoveFunc(container, grid) {
    return function (ev) {
        var pos = ev.data.getLocalPosition(container)
		grid.mouseX = pos.x;
		grid.mouseY = pos.y;
		grid.updateTooltip();
    }
}


const CellType = Object.freeze({"SAFE":1, "TREE":2, "HOUSE":3});


export class GridModule {
	
	/**
	 * Corresponds to the moduleName variable used in the Java module.
	 */
	static get moduleName() {
		return 'GridModule'
	}
	
    generateText(text, size, color, align) {
        var textEl = new PIXI.Text(text, {
            fontSize: Math.round(size / 1.2) + 'px',
            fontFamily: 'Lato',
            fontWeight: 'bold',
            fill: color
        })

        textEl.lineHeight = Math.round(size / 1.2)
        if (align === 'right') {
            textEl.anchor.x = 1
        } else if (align === 'center') {
            textEl.anchor.x = 0.5
        }

        return textEl
    };


	constructor(assets) {
		this.startId = 1000000
		this.runtimeId = this.startId
		this.tileSize = 128;
		this.tileOffset = this.tileSize / 2;
		this.fireImages = Array.from({length: 9}, (_, i) => "fire_" + String(i+1).padStart(4, '0') + ".png").join()
		this.cutImages = Array.from({length: 5}, (_, i) => "cut_" + String(i+1).padStart(4, '0') + ".png").join()
	}
	
	updateTooltip() {
		var tooltip = this.tooltip;
		var selectionRect = this.selectionRect;
        if (tooltip && selectionRect) {
            tooltip.x = this.mouseX
            tooltip.y = this.mouseY
			var step = this.scale * this.tileSize;
			var x = Math.floor((this.mouseX - this.xOffset) / step);
			var y = Math.floor((this.mouseY - this.yOffset) / step);
			if (x >= 0 && x < this.width && y >= 0 && y < this.height) {
				tooltip.visible = true;
				selectionRect.visible = true;
				selectionRect.x = this.xOffset + step * x;
				selectionRect.y = this.yOffset + step * y;
				var tile = this.tiles[y][x];
				var text = "x=" + x + " y=" + y + "\n";
				var type = tile.type;
				var fireProgress = Math.round(tile.tileSprite.currentState.fireProgress);
				if (fireProgress == -2)
					type = CellType.SAFE;
				text += "TYPE=" + (type == CellType.SAFE ? "SAFE" : type == CellType.TREE ? "TREE" : "HOUSE");
				if (type != CellType.SAFE) {
					var fireDuration = tile.type == CellType.TREE ? this.treeFireDuration : this.houseFireDuration;
					var cuttingDuration = tile.type == CellType.TREE ? this.treeCuttingDuration : this.houseCuttingDuration;
					var value = tile.type == CellType.TREE ? this.treeValue : this.houseValue;
					text += "\n" + (fireProgress == -1 ? "No fire" : fireProgress == fireDuration ?
									"Burnt tile" : ("Fire progress: " + fireProgress + " / " + fireDuration));
					text += "\n" + "V=" + value + " F=" + fireDuration + " C=" + cuttingDuration;
				}
	            tooltip.label.text = text;
	            tooltip.background.width = tooltip.label.width + 20
	            tooltip.background.height = tooltip.label.height + 20
	
	            tooltip.pivot.x = -30
	            tooltip.pivot.y = -50
	            if (tooltip.y - tooltip.pivot.y + tooltip.height > HEIGHT) {
	                tooltip.pivot.y = 10 + tooltip.height
	                tooltip.y = HEIGHT + tooltip.pivot.y - tooltip.height
	            }
	            if (tooltip.x - tooltip.pivot.x + tooltip.width > WIDTH) {
	                tooltip.pivot.x = 10 + tooltip.width
	                tooltip.x = WIDTH + tooltip.pivot.x - tooltip.width
	            }
			}
			else {
				tooltip.visible = false;
				selectionRect.visible = false;
			}
        }
	}
	
	getValue(sequence, i) {
		return (sequence.charCodeAt(i) - 65) * 26 + (sequence.charCodeAt(i + 1) - 65) - 338;
	}

	/**
	 * Called when global data is received. Should only be called on init.
	 * @param players information about players, such as avatar, name, color..
	 * @param globalData data that has been sent from the Java module.
	 */
	handleGlobalData(players, globalData) {
		console.log("Loading map...");
		var dataLines = globalData.split('\n');
		console.log(dataLines);
		this.maxBurntValue = parseInt(dataLines[0]);
		var treeWords = dataLines[1].split(' ');
		this.treeFireDuration = parseInt(treeWords[0]);
		this.treeCuttingDuration = parseInt(treeWords[1]);
		this.treeValue = parseInt(treeWords[2]);
		var houseWords = dataLines[2].split(' ');
		this.houseFireDuration = parseInt(houseWords[0]);
		this.houseCuttingDuration = parseInt(houseWords[1]);
		this.houseValue = parseInt(houseWords[2]);
		var sizeWords = dataLines[3].split(' ');
		this.width = parseInt(sizeWords[0]);
		this.height = parseInt(sizeWords[1]);
		this.scale = fitAspectRatio(128 * this.width, 128 * this.height, WIDTH, HEIGHT);
		this.offsetX = fitAspectRatio(128 * this.width, 128 * this.height, WIDTH, HEIGHT);
		this.xOffset = (WIDTH - this.width * this.tileSize * this.scale) / 2;
		this.yOffset = (HEIGHT - this.height * this.tileSize * this.scale) / 2;
		this.tiles = new Array(this.height);
		for (var i = 0; i < this.height; i++) {
			this.tiles[i] = new Array(this.width);
			console.log(dataLines[4 + i]);
			for (var j = 0; j < this.width; j++) {
				var tile = this.tiles[i][j] = {};
				tile.tileSprite = EntityFactory.create("S");
				tile.tileSprite.defaultState.fireProgress = 0;
				tile.tileSprite.id = this.runtimeId++;
				tile.objectSprite = EntityFactory.create("S");
				tile.objectSprite.id = this.runtimeId++;
				tile.fireAnim = EntityFactory.create("A");
				tile.fireAnim.id = this.runtimeId++;
				tile.fireProgress = -3;
				var typeChar = dataLines[4 + i].charAt(j);
				tile.type = typeChar == "#" ? CellType.SAFE : typeChar == "." ? CellType.TREE : CellType.HOUSE;
			}
		}
		this.cutAnim = EntityFactory.create("A");
		this.cutAnim.id = this.runtimeId++;
		this.worldSpriteIds = Array.from({length: this.runtimeId - this.startId}, (_, i) => this.startId + i);
		this.group = EntityFactory.create("G");
		this.group.id = this.runtimeId++;
		console.log("map loaded.");
	}
	

	/**
	 * Called when data is received.
	 * Handles data for the given frame. Returns data that will be sent as parameter to updateScene.
	 * @param frameInfo information about the current frame.
	 * @param frameData data that has been sent from the Java module.
	 */
	handleFrameData(frameInfo, frameData) {
		// Handle your data here
		console.log("Loading turn " + frameInfo.number + "...");
		if (frameInfo.number == 0) {
			for (var i = 0; i < this.height; i++) {
				for (var j = 0; j < this.width; j++) {
					entityModule.entities.set(this.tiles[i][j].tileSprite.id, this.tiles[i][j].tileSprite);
					entityModule.entities.set(this.tiles[i][j].objectSprite.id, this.tiles[i][j].objectSprite);
					entityModule.entities.set(this.tiles[i][j].fireAnim.id, this.tiles[i][j].fireAnim);
				}
			}
			entityModule.entities.set(this.cutAnim.id, this.cutAnim);
			entityModule.entities.set(this.group.id, this.group);
			this.group.addState(1, {
				values: {
					...this.group.defaultState,
					children: this.worldSpriteIds,
					x: this.xOffset,
					y: this.yOffset,
					scaleX : this.scale,
					scaleY : this.scale,
					visible: true
				},
				curve: {}
			}, frameInfo.number, frameInfo);
		}
		
		var k = 0;
		var cutX = this.getValue(frameData, k);
		k += 2;
		var cutY = this.getValue(frameData, k);
		k += 2;
		for (var i = 0; i < this.height; i++) {
			for (var j = 0; j < this.width; j++) {
				var fireProgress = this.getValue(frameData, k);
				k += 2;
				var tile = this.tiles[i][j];
				if (tile.fireProgress != fireProgress) {
					var fireDuration = tile.type == CellType.TREE ? this.treeFireDuration : this.houseFireDuration;
					tile.tileSprite.addState(1, {
						values: {
							...tile.tileSprite.defaultState,
							image: "tile.png",
							tint: fireProgress == -2 ? 0xE0C870 : lerpColor(0x00A000, 0xA04000,
								Math.max(0, fireProgress / fireDuration)),
							x: this.tileSize * j,
							y: this.tileSize * i,
							zIndex: 1,
							alpha: 1.0,
							visible: true,
							fireProgress: fireProgress // Registering for runtime tooltips
						},
						curve: {}
					}, frameInfo.number, frameInfo);
					if (tile.type != CellType.SAFE && (tile.fireProgress == -3 || fireProgress == fireDuration || fireProgress == -2)) {
						tile.objectSprite.addState(1, {
							values: {
								...tile.objectSprite.defaultState,
								image: tile.type == CellType.TREE ? "tree.png" : "house.png",
								x: this.tileSize * j + this.tileSize / 2,
								y: this.tileSize * i + this.tileSize,
								anchorX: 0.4 + 0.2 * Math.random(),
								anchorY: 0.95 + 0.1 * Math.random(),
								zIndex: 2 + i,
								alpha: 1.0,
								scaleX: tile.fireProgress == -3 ? 1.0 : 0.0,
								scaleY: tile.fireProgress == -3 ? 1.0 : 0.0,
								visible: tile.fireProgress == -3
							},
							curve: {}
						}, frameInfo.number, frameInfo);
					}
					if (fireProgress == 0 || fireProgress == fireDuration) {
						var scale = fireProgress == 0 ? 1.0 : 0.0;
						tile.fireAnim.addState(0, {
							values: {
								...tile.fireAnim.defaultState,
								images: this.fireImages,
								x: this.tileSize * j + this.tileOffset,
								y: this.tileSize * i + this.tileOffset,
								scaleX: (1.0 - scale),
								scaleY: (1.0 - scale),
								anchorX: 0.5,
								anchorY: 0.5,
								zIndex: 1000,
								alpha: 1,
								visible: true,
								loop: true,
								animationProgress: 0
							},
							curve: {}
						}, frameInfo.number, frameInfo);
						tile.fireAnim.addState(1, {
							values: {
								...tile.fireAnim.defaultState,
								images: this.fireImages,
								x: this.tileSize * j + this.tileOffset,
								y: this.tileSize * i + this.tileOffset,
								scaleX: scale,
								scaleY: scale,
								anchorX: 0.5,
								anchorY: 0.5,
								zIndex: 1000,
								alpha: 1,
								visible: (fireProgress == 0),
								loop: true,
								animationProgress: 1
							},
							curve: {}
						}, frameInfo.number, frameInfo);
					}
					tile.fireProgress = fireProgress;
				}
			}
		}
		{
			var scale = fireProgress == 0 ? 1.0 : 0.0;
			this.cutAnim.addState(0, {
				values: {
					...this.cutAnim.defaultState,
					images: this.cutImages,
					x: this.tileSize * cutX + this.tileOffset,
					y: this.tileSize * cutY + this.tileOffset,
					scaleX: 0.2,
					scaleY: 0.2,
					anchorX: 0.5,
					anchorY: 0.5,
					zIndex: 1000,
					alpha: 1,
					visible: cutX >= 0,
					loop: true,
					animationProgress: 0
				},
				curve: {}
			}, frameInfo.number, frameInfo);
			this.cutAnim.addState(0, {
				values: {
					...this.cutAnim.defaultState,
					images: this.cutImages,
					x: this.tileSize * cutX + this.tileOffset,
					y: this.tileSize * cutY + this.tileOffset,
					scaleX: 0.2,
					scaleY: 0.2,
					anchorX: 0.5,
					anchorY: 0.5,
					zIndex: 1000,
					alpha: 1,
					visible: cutX >= 0,
					loop: true,
					animationProgress: 0
				},
				curve: {}
			}, frameInfo.number, frameInfo);
		}
		
		console.log("Turn " + frameInfo.number + " loaded.");


		// Return what is necessary to your module
		return { frameInfo, frameData }
	}

	/**
	 * Called when the scene needs an update.
	 * @param previousData data from the previous frame.
	 * @param currentData data of the current frame.
	 * @param progress progress of the frame. 0 <= progress <= 1
	 * @param speed the speed of the viewer, setted up by the user.
	 */
	updateScene(previousData, currentData, progress, speed) {

	}
	

	/**
	 * Called when the viewer needs to be rerendered (init phase, resized viewer).
	 * @param container a PIXI Container. Add your elements to this object.
	 * @param canvasData canvas data containing width and height.
	 */
	reinitScene(container, canvasData) {
		var tooltip = this.tooltip = new PIXI.Container()
        var tooltipBackground = tooltip.background = new PIXI.Graphics()
        tooltipBackground.beginFill(0x0, 0.7)
        tooltipBackground.drawRect(0, 0, 200, 100)
        tooltipBackground.endFill()
        tooltipBackground.x = -10
        tooltipBackground.y = -10
        tooltip.visible = true
        tooltip.addChild(tooltipBackground)
        var label = tooltip.label = this.generateText('...', 36, 0xFFFFFF, 'left')
        tooltip.addChild(label)
        tooltip.interactiveChildren = false

		var selectionRect = this.selectionRect = new PIXI.Graphics();
		var rectSize = this.scale * this.tileSize;
      	selectionRect.lineStyle(2, 0x000000, 1);
        selectionRect.drawRect(0, 0, rectSize, rectSize);
        selectionRect.visible = false;

        container.mousemove = getMouseMoveFunc(container, this);
        container.interactive = true;
        container.addChild(tooltip);
        container.addChild(selectionRect);
	}

	/**
	 * Called every delta milliseconds.
	 * @param delta time between current and last call. Aproximately 16ms by default.
	 */
	animateScene(delta) {
		this.updateTooltip();
	}
}