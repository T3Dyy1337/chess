package util;

import engine.AttackGenerator;
import engine.Board;
import engine.Move;
import engine.MoveGenerator;

public class Perft {

    public static long perft(Board board, int depth) {
        if (depth == 0)
            return 1;

        long nodes = 0;

        int[] moves = new int[256];
        int moveCount = MoveGenerator.generateAllMoves(board,moves);

        for (int i = 0; i < moveCount; i++) {
            int move = moves[i];

            board.makeMove(move);

            int us = board.sideToMove ^ 1;
            int kingSq = (us == Constants.WHITE)
                    ? BitHelper.lsb(board.whiteKing)
                    : BitHelper.lsb(board.blackKing);
            long curNodes = 0;
            if (!AttackGenerator.isSquareAttacked(board, kingSq, board.sideToMove)) {
                curNodes = perft(board, depth - 1);
            }

            nodes += curNodes;

            if(depth == 5){
                System.out.println(Move.toUCI(move) + ": " + curNodes);
            }

            board.unmakeMove();
        }


        return nodes;
    }
}

