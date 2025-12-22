package engine;

import util.Constants;

import java.util.Scanner;

public final class ConsoleGame {

  public static void play(boolean enginePlaysWhite, String fen) {
    Board board = new Board();
    if (fen == null) board.loadFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
    else board.loadFEN(fen);

    Search search = new Search(new TranspositionTable(256));

    Scanner sc = new Scanner(System.in);

    while (true) {
      board.print();

      if ((board.sideToMove == Constants.WHITE) == enginePlaysWhite) {
        int move = search.search(board, 12);
        System.out.println("Engine plays: " + Move.toUCI(move));
        board.makeMove(move);
      } else {
        System.out.print("Your move: ");
        String moveStr = sc.nextLine();
        int move = Move.fromUCI(board, moveStr);
        board.makeMove(move);
      }
    }
  }
}

