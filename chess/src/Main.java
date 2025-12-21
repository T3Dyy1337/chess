import engine.*;
import util.BitHelper;
import util.Constants;
import util.Perft;
import util.WeightLoader;


public class Main {
    public static void main(String[] args) {
        Board b = new Board();
        b.loadFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1 ");
        float[] input = NNInput.encode(b);
        float[] weights = {};
        try{
            weights = WeightLoader.loadWeights("C:\\Users\\juric\\OneDrive\\Dokumenty\\chess\\chess\\files\\weights.bin");
        }catch (Exception e){
            e.printStackTrace();
        }
        NeuralEval nn = new NeuralEval(weights);
        TranspositionTable tt = new TranspositionTable(256);




//        Search search = new Search(nn,tt);
//
//
//        testSearch(b,search,10);
//
//        for (int i = 0; i < search.pvLength[0]; i++) {
//            System.out.print(Move.toUCI(search.pvMoves[0][i]) + " ");
//        }
//        System.out.println();

        System.out.println(Perft.perft(b,5));

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

    }


}
