package app;

import engine.board.Board;
import engine.move.AttackGenerator;
import engine.move.Move;
import engine.move.MoveGenerator;
import engine.search.Search;
import engine.search.TranspositionTable;
import engine.common.Constants;

import java.util.Scanner;

public final class ConsoleGame {

  public static void play(boolean enginePlaysWhite, String fen) {
    Board board = new Board();
    TranspositionTable tt = new TranspositionTable(1024);
    if (fen == null) board.loadFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
    else board.loadFEN(fen);

    Search search = new Search(tt);

    Scanner sc = new Scanner(System.in);

    while (true) {
      board.print();

      if (checkGameOver(board)) return;

      if ((board.sideToMove == Constants.WHITE) == enginePlaysWhite) {
        long start = System.currentTimeMillis();

        long timeForMove = 3000;
        int move = search.search(board, timeForMove);


        long elapsed = System.currentTimeMillis() - start;
        double seconds = elapsed / 1000.0;

        double nps = seconds > 0 ? (search.nodes / seconds) : 0;



        if (move == 0) {
          if (board.isThreefoldRepetition()) {
            System.out.println("Draw by repetition");
          } else if (board.halfmoveClock >= 100) {
            System.out.println("Draw by 50-move rule");
          } else {
            System.out.println("Draw");
          }
          return;
        }


        System.out.printf(
            "Engine plays: %s   (nodes=%d, time=%.2fs, NPS=%.0f)\n",
            Move.toUCI(move), search.nodes, seconds, nps
        );        board.makeMove(move);
      } else {
        System.out.print("Your move: ");
        String moveStr = sc.nextLine();
        int move = Move.fromUCI(board, moveStr);
        board.makeMove(move);
        if (checkGameOver(board)) return;
      }

      if (board.isThreefoldRepetition()) {
        System.out.println("Draw by repetition");
        return;
      }
    }
  }

  private static boolean checkGameOver(Board board) {
    int[] moves = new int[256];
    int moveCount = MoveGenerator.generateAllMoves(board, moves);

    for (int i = 0; i < moveCount; i++) {
      board.makeMove(moves[i]);

      int mover = board.sideToMove ^ 1;
      int kingSq = mover == Constants.WHITE
          ? board.whiteKingSq
          : board.blackKingSq;

      boolean legal = !AttackGenerator.isSquareAttacked(
          board, kingSq, board.sideToMove
      );

      board.unmakeMove();
      if (legal) return false;
    }

    if (board.isInCheck()) {
      System.out.println("Checkmate. " +
          (board.sideToMove == Constants.WHITE ? "Black wins." : "White wins."));
    } else {
      System.out.println("Stalemate.");
    }
    return true;
  }

}

