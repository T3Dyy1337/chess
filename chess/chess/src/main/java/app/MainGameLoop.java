package app;

import engine.board.Board;
import engine.common.Constants;
import engine.move.AttackGenerator;
import engine.move.Move;
import engine.move.MoveGenerator;
import engine.search.Search;
import engine.search.TimeManager;

import java.util.Scanner;

public final class MainGameLoop {

  private final Search search;
  private final Scanner in = new Scanner(System.in);

  // clocks (ms)
  private long whiteTimeMs;
  private long blackTimeMs;
  private final long whiteIncMs;
  private final long blackIncMs;

  // eval history for engine time management
  private int lastEvalCp = 0;
  private int last2EvalCp = 0;

  // scratch
  private final int[] moves = new int[256];

  public MainGameLoop(Search search,
      long whiteTimeMs,
      long blackTimeMs,
      long whiteIncMs,
      long blackIncMs) {
    this.search = search;
    this.whiteTimeMs = whiteTimeMs;
    this.blackTimeMs = blackTimeMs;
    this.whiteIncMs = whiteIncMs;
    this.blackIncMs = blackIncMs;
  }

  public void play(Board board, boolean humanIsWhite) {

    while (true) {

      // --- game end checks ---
      if (board.halfmoveClock >= 100) {
        System.out.println("Draw by 50-move rule");
        break;
      }

      int legalCount = countLegalMoves(board);
      if (legalCount == 0) {
        if (isSideToMoveInCheck(board)) {
          System.out.println("Checkmate!");
        } else {
          System.out.println("Stalemate!");
        }
        break;
      }

      boolean whiteToMove = board.sideToMove == Constants.WHITE;
      boolean humanToMove = (whiteToMove == humanIsWhite);

      if (humanToMove) {
        // ======================
        // HUMAN MOVE
        // ======================
        System.out.print("Your move (uci): ");
        String uci = in.nextLine().trim();

        int move = parseAndValidateMove(board, uci);
        if (move == 0) {
          System.out.println("Illegal move, try again.");
          continue;
        }

        board.makeMove(move);
        continue;
      }

      // ======================
      // ENGINE MOVE
      // ======================
      long remainingMs = whiteToMove ? whiteTimeMs : blackTimeMs;
      long incMs = whiteToMove ? whiteIncMs : blackIncMs;

      if (remainingMs <= 0) {
        System.out.println("Engine flagged!");
        break;
      }

      long timeForMoveMs = TimeManager.computeTimeForMoveMs(
          board,
          remainingMs,
          incMs,
          /* fullmoveNumber */ 1,
          lastEvalCp,
          last2EvalCp
      );

      long start = System.currentTimeMillis();
      int bestMove = search.search(board, timeForMoveMs);
      long spent = System.currentTimeMillis() - start;

      if (whiteToMove) {
        whiteTimeMs = Math.max(0, whiteTimeMs - spent) + whiteIncMs;
      } else {
        blackTimeMs = Math.max(0, blackTimeMs - spent) + blackIncMs;
      }

      last2EvalCp = lastEvalCp;
      lastEvalCp = search.lastCompletedRootScore;

      System.out.println("Engine plays: " + Move.toUCI(bestMove));
      board.makeMove(bestMove);
    }
  }

  // ============================================================
  // Helpers
  // ============================================================

  private int parseAndValidateMove(Board board, String uci) {
    if (uci.length() < 4) return 0;

    int from = squareFromUci(uci.substring(0, 2));
    int to   = squareFromUci(uci.substring(2, 4));
    int promo = 0;

    if (uci.length() == 5) {
      promo = promoFromChar(board,uci.charAt(4));
    }

    int count = MoveGenerator.generateAllMoves(board, moves);
    int us = board.sideToMove;
    int kingSq = (us == Constants.WHITE) ? board.whiteKingSq : board.blackKingSq;

    for (int i = 0; i < count; i++) {
      int m = moves[i];
      if (Move.from(m) == from && Move.to(m) == to) {
        if (promo != 0 && Move.promo(m) != promo) continue;

        board.makeMove(m);
        boolean illegal = AttackGenerator.isSquareAttacked(
            board,
            kingSq,
            board.sideToMove
        );
        board.unmakeMove();

        if (!illegal) return m;
      }
    }
    return 0;
  }

  private int countLegalMoves(Board board) {
    int count = MoveGenerator.generateAllMoves(board, moves);
    int legal = 0;

    int us = board.sideToMove;
    int kingSq = (us == Constants.WHITE) ? board.whiteKingSq : board.blackKingSq;

    for (int i = 0; i < count; i++) {
      board.makeMove(moves[i]);
      boolean illegal = AttackGenerator.isSquareAttacked(
          board,
          kingSq,
          board.sideToMove
      );
      board.unmakeMove();
      if (!illegal) legal++;
    }
    return legal;
  }

  private boolean isSideToMoveInCheck(Board board) {
    int us = board.sideToMove;
    int kingSq = (us == Constants.WHITE) ? board.whiteKingSq : board.blackKingSq;
    return AttackGenerator.isSquareAttacked(board, kingSq, us ^ 1);
  }

  private int squareFromUci(String s) {
    int file = s.charAt(0) - 'a';
    int rank = s.charAt(1) - '1';
    return rank * 8 + file;
  }

  private int promoFromChar(Board board, char c) {
    boolean white = board.sideToMove == Constants.WHITE;

    return switch (c) {
      case 'q' -> white ? Constants.W_QUEEN  : Constants.B_QUEEN;
      case 'r' -> white ? Constants.W_ROOK   : Constants.B_ROOK;
      case 'b' -> white ? Constants.W_BISHOP : Constants.B_BISHOP;
      case 'n' -> white ? Constants.W_KNIGHT : Constants.B_KNIGHT;
      default  -> 0;
    };
  }

}
