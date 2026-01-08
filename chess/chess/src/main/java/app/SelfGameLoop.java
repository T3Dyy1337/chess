package app;

import engine.board.Board;
import engine.common.Constants;
import engine.move.AttackGenerator;
import engine.move.MoveGenerator;
import engine.search.Search;
import engine.search.TimeManager;

public final class SelfGameLoop {

  private final Search search;

  // Clock in ms
  private long whiteTimeMs;
  private long blackTimeMs;
  private final long whiteIncMs;
  private final long blackIncMs;

  // Eval history for volatility (centipawns)
  private int lastEvalCp = 0;
  private int last2EvalCp = 0;

  // Scratch to avoid allocations
  private final int[] tmpMoves = new int[256];

  public SelfGameLoop(Search search,
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

  /** Plays until game end by "no legal moves" or 50-move rule. */
  public void play(Board board) {

    while (true) {
      // 1) Game end checks (your rules)
      if (board.halfmoveClock >= 100) break; // draw by 50-move rule (100 plies)

      int legalMoves = countLegalMoves(board);
      if (legalMoves == 0) {
        boolean inCheck = isSideToMoveInCheck(board);
        break;
      }

      // 2) Pick clock for side to move
      boolean whiteToMove = (board.sideToMove == Constants.WHITE);
      long remainingMs = whiteToMove ? whiteTimeMs : blackTimeMs;
      long incMs = whiteToMove ? whiteIncMs : blackIncMs;

      if (remainingMs <= 0) {
        // flag
        break;
      }

      // 3) Compute time FOR THIS MOVE (once)
      int fullmoveNumber = getFullmoveNumber(board); // if you don't have it, see helper below
      long timeForMoveMs = TimeManager.computeTimeForMoveMs(
          board,
          remainingMs,
          incMs,
          fullmoveNumber,
          lastEvalCp,
          last2EvalCp
      );
      board.print();
      // 4) Search + measure real time used
      long start = System.currentTimeMillis();
      int bestMove = search.search(board, timeForMoveMs);
      long spent = System.currentTimeMillis() - start;

      // 5) Update clock (spent then increment)
      if (whiteToMove) {
        whiteTimeMs = Math.max(0, whiteTimeMs - spent) + whiteIncMs;
      } else {
        blackTimeMs = Math.max(0, blackTimeMs - spent) + blackIncMs;
      }

      // 6) Update eval history for NEXT move (requires the tiny Search change)
      last2EvalCp = lastEvalCp;
      lastEvalCp = search.lastCompletedRootScore;

      // 7) Play move
      board.makeMove(bestMove);
    }
  }

  /** Counts legal moves by generating pseudo-legal and filtering king-in-check moves. */
  private int countLegalMoves(Board board) {
    int count = MoveGenerator.generateAllMoves(board, tmpMoves);
    int legal = 0;

    int us = board.sideToMove;
    int ourKingSq = (us == Constants.WHITE) ? board.whiteKingSq : board.blackKingSq;

    for (int i = 0; i < count; i++) {
      int move = tmpMoves[i];
      board.makeMove(move);

      // after makeMove, sideToMove flipped; attacker side is new sideToMove
      boolean illegal = AttackGenerator.isSquareAttacked(board, ourKingSq, board.sideToMove);

      board.unmakeMove();
      if (!illegal) legal++;
    }

    return legal;
  }

  /** True if side to move is currently in check (reliable). */
  private boolean isSideToMoveInCheck(Board board) {
    int us = board.sideToMove;
    int kingSq = (us == Constants.WHITE) ? board.whiteKingSq : board.blackKingSq;
    int them = us ^ 1;
    return AttackGenerator.isSquareAttacked(board, kingSq, them);
  }

  /**
   * If your Board already has fullmoveNumber, use that.
   * If not, this fallback returns 1 so TimeManager uses the "opening" estimate.
   * Best: store fullmoveNumber in Board and return it.
   */
  private int getFullmoveNumber(Board board) {
    // Replace with: return board.fullmoveNumber;
    return 1;
  }

  // Expose clock if you want
  public long getWhiteTimeMs() { return whiteTimeMs; }
  public long getBlackTimeMs() { return blackTimeMs; }
}
