import { WIDTH, HEIGHT } from '../core/constants.js'
import { fitAspectRatio } from '../core/utils.js'
import { api as entityModule } from '../entity-module/GraphicEntityModule.js'
import { EntityFactory } from '../entity-module/EntityFactory.js'


export class GridModule {
	/**
	 * Corresponds to the moduleName variable used in the Java module.
	 */
	static get moduleName() {
		return 'GridModule'
	}

	constructor(assets) {
		this.startId = 1000000
		this.runtimeId = this.startId
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
		this.globalData = {
			players: players,
			playerCount: players.length
		}
		this.height = this.getValue(globalData, 0);
		this.width = this.getValue(globalData, 2);
		this.tiles = new Array(this.height);
		for (var i = 0; i < this.height; i++) {
			this.tiles[i] = new Array(this.width);
			for (var j = 0; j < this.width; j++) {
				this.tiles[i][j] = new Array(3);
				this.tiles[i][j][0] = EntityFactory.create("S");
				this.tiles[i][j][0].id = this.runtimeId++;
				this.tiles[i][j][1] = EntityFactory.create("S");
				this.tiles[i][j][1].id = this.runtimeId++;
				this.tiles[i][j][2] = EntityFactory.create("A");
				this.tiles[i][j][2].id = this.runtimeId++;
			}
		}
		this.tilesIds = Array.from({length: this.runtimeId - this.startId}, (_, i) => this.startId + i);
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
					for (var k = 0; k < 3; k++)
						entityModule.entities.set(this.tiles[i][j][k].id, this.tiles[i][j][k]);
				}
			}
			entityModule.entities.set(this.group.id, this.group);
		}
		
		var k = 0;
		for (var i = 0; i < this.height; i++) {
			for (var j = 0; j < this.width; j++) {
				var fireProgress = this.getValue(frameData, k);
				if (i==1 && j == 1) console.log(fireProgress);
				k += 2;
				if (frameInfo.number % 2 == 0) {
					this.tiles[i][j][0].addState(1, {
						values: {
							...this.tiles[i][j][0].defaultState,
							image: "tileDirt.png",
							x: 128 * j,
							y: 128 * i,
							zIndex: 0,
							alpha: 1,
							visible: true
						},
						curve: {}
					}, frameInfo.number, frameInfo);
					var opacity = (100 - Math.max(0, fireProgress)) / 100.0;
					this.tiles[i][j][1].addState(1, {
						values: {
							...this.tiles[i][j][1].defaultState,
							image: "tileGrass.png",
							x: 128 * j,
							y: 128 * i,
							zIndex: 1,
							alpha: opacity,
							visible: (opacity != 0.0)
						},
						curve: {}
					}, frameInfo.number, frameInfo);
					this.tiles[i][j][2].addState(1, {
						values: {
							...this.tiles[i][j][2].defaultState,
							images: "onfire_0001.png,onfire_0002.png,onfire_0003.png,onfire_0004.png",
							x: 128 * j,
							y: 128 * i,
							zIndex: 2,
							alpha: 1,
							visible: (fireProgress >= 0 && fireProgress < 100)
						},
						curve: {}
					}, frameInfo.number, frameInfo);
				}
			}
		}
		var scale = fitAspectRatio(128 * this.width, 128 * this.height, WIDTH, HEIGHT);
		this.group.addState(1, {
			values: {
				...this.group.defaultState,
				children: this.tilesIds,
				scaleX : scale,
				scaleY : scale,
				visible: true
			},
			curve: {}
		}, frameInfo.number, frameInfo);

		/*var sprite = EntityFactory.create("S");
		sprite.id = this.runtimeId++;
		entityModule.entities.set(sprite.id, sprite);
		sprite.addState(0, {
			values: {
				...sprite.defaultState,
				image: "tree.png",
				visible: true,
				zIndex: 10
			},
			curve: {}
		}, frameInfo.number, frameInfo)*/
		
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

	}

	/**
	 * Called every delta milliseconds.
	 * @param delta time between current and last call. Aproximately 16ms by default.
	 */
	animateScene(delta) {

	}
}