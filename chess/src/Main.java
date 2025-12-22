import engine.*;
import util.BitHelper;
import util.Constants;
import util.Perft;
import util.WeightLoader;


public class Main {
    public static void main(String[] args) {
        //Board b = new Board();
        //String fen = "8/3p4/p2p3p/3N4/2P3rP/1P6/kBK5/4R3 w - - 0 37";
        //b.loadFEN(fen);

        //TranspositionTable tt = new TranspositionTable(256);

        //Search search = new Search(tt);

        //testSearch(b,search,12);

        //System.out.println(Perft.perft(b,6));
        ConsoleGame.play(false,"8/6p1/3b2B1/p6P/2pk2P1/2p5/P1K5/8 w - - 1 47");
    }


    public static void testSearch(Board board, Search search, int depth) {
        long start = System.currentTimeMillis();
        int bestMove = search.search(board, depth);
        long end = System.currentTimeMillis();

        System.out.println("Best move: " + Move.toUCI(bestMove));
        System.out.println("Nodes searched: " + search.nodesSearched);
        System.out.println("Time (ms): " + (end - start));
        double sec = (end - start) / 1000.0;
        System.out.println("NPS: " + (long)(search.nodesSearched / Math.max(0.001, sec)));

        System.out.print("PV: ");
        for (int i = 0; i < search.pvLength[0]; i++) {
            int move = search.pvMoves[0][i];
            System.out.print(Move.toUCI(move) + " ");
        }
        System.out.println();

        }

    }



