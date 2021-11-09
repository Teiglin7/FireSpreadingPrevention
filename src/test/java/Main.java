import com.codingame.gameengine.runner.SoloGameRunner;
//import com.codingame.gameengine.runner.simulate.GameResult;

public class Main {
    public static void main(String[] args) {
        SoloGameRunner gameRunner = new SoloGameRunner();

        // Sets the player
        gameRunner.setAgent(Solution.class);

        // Sets a test case
        gameRunner.setTestCase("test3.yaml");

        gameRunner.start();
        //GameResult result = gameRunner.simulate();
    }
}
