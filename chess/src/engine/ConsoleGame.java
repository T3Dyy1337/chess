package engine;

import util.Constants;

import java.util.Scanner;

public final class ConsoleGame {

  public static void play(boolean enginePlaysWhite, String fen) {
    Board board = new Board();
    if (fen == null) board.loadFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
    else board.loadFEN(fen);

    Search search = new Search();

    Scanner sc = new Scanner(System.in);

    while (true) {
      board.print();

      if (checkGameOver(board)) return;

      if ((board.sideToMove == Constants.WHITE) == enginePlaysWhite) {
        int move = search.search(board, 12);
        if (move == 0) {
          if (board.isRepetition()) {
            System.out.println("Draw by repetition");
          } else if (board.halfmoveClock >= 100) {
            System.out.println("Draw by 50-move rule");
          } else {
            System.out.println("Draw");
          }
          return;
        }


        System.out.println("Engine plays: " + Move.toUCI(move));
        board.makeMove(move);
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
      if (legal) return false; // has at least one legal move
    }

    // No legal moves
    if (board.isInCheck()) {
      System.out.println("Checkmate. " +
          (board.sideToMove == Constants.WHITE ? "Black wins." : "White wins."));
    } else {
      System.out.println("Stalemate.");
    }
    return true;
  }

}

