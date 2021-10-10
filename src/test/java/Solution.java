import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/*
 * A basic example AI for the game.
 * Goes to the closest reachable eggs.
 */

public class Solution {
    private static List<Coord> eggsPositions = new ArrayList<>();
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int width = scanner.nextInt();
        int height = scanner.nextInt();
        for (int y=0; y < height; ++y) {
            for (int x=0; x < height; ++x) {
            	scanner.nextInt();
            }
        }
        for (int y=0; y < height; ++y) {
            for (int x=0; x < height; ++x) {
            	scanner.nextInt();
            }
        }

        while (true) {
            for (int y=0; y < height; ++y) {
                for (int x=0; x < height; ++x) {
                	scanner.nextInt();
                }
            }

            System.out.println("0 0");
        }
    }

    static class Coord {
        public int x, y;

        public Coord(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
