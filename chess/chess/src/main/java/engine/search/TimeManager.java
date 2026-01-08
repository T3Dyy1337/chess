package engine.search;

import engine.board.Board;
import engine.move.MoveGenerator;

public final class TimeManager {

  private TimeManager() {}

  // Safety / tuning
  private static final long MIN_TIME_MS = 5;         // never less than this
  private static final long PANIC_TIME_MS = 5_000;   // below this, go fast
  private static final long PANIC_MOVE_MS = 80;      // cap in panic
  private static final double MAX_RATIO = 0.20;      // never use >20% of remaining time
  private static final double INC_USAGE = 0.80;      // how much of increment we "count"

  /**
   * Computes per-move search time in ms.
   *
   * @param board current position
   * @param remainingMs clock time left for side to move (ms)
   * @param incrementMs increment per move (ms), 0 if none
   * @param fullmoveNumber (1..), used for phase-ish estimation
   * @param lastRootEvalCp evaluation (centipawns) from previous move (optional; pass 0 if unknown)
   * @param last2RootEvalCp evaluation from 2 moves ago (optional; pass 0 if unknown)
   */
  public static long computeTimeForMoveMs(
      Board board,
      long remainingMs,
      long incrementMs,
      int fullmoveNumber,
      int lastRootEvalCp,
      int last2RootEvalCp
  ) {
    if (remainingMs <= 0) return MIN_TIME_MS;

    // Panic mode
    if (remainingMs < PANIC_TIME_MS) {
      return Math.max(MIN_TIME_MS, Math.min(PANIC_MOVE_MS, remainingMs / 10));
    }

    // --- 1) Estimate moves remaining (simple but effective) ---
    // Opening: spend less; Middlegame: normal; Endgame: often less (or TB), but still allow tactics.
    boolean endgame = board.nonPawnMaterial(board.sideToMove) <= 5;

    int movesRemaining;
    if (endgame) movesRemaining = 15;
    else if (fullmoveNumber < 15) movesRemaining = 40;
    else if (fullmoveNumber < 40) movesRemaining = 25;
    else movesRemaining = 20;

    // --- 2) Baseline budget ---
    double base = (double) remainingMs / movesRemaining;
    base += incrementMs * INC_USAGE;

    // --- 3) Complexity factor from position ---
    double complexity = 1.0;

    boolean inCheck = board.isInCheck();
    if (inCheck) complexity *= 2.0;

    // quick legal move count (we can generate once here)
    int[] tmpMoves = new int[256];
    int legalCount = MoveGenerator.generateAllMoves(board, tmpMoves);

    if (legalCount > 35) complexity *= 1.20;
    else if (legalCount < 8) complexity *= 0.75;

    // Phase nudges: opening faster, endgame slightly faster
    if (fullmoveNumber < 10) complexity *= 0.75;
    if (endgame) complexity *= 0.85;

    // --- 4) Volatility factor (optional, but nice) ---
    // If you pass lastRootEvalCp/last2RootEvalCp, we can detect "swingy" games.
    int delta = Math.abs(lastRootEvalCp - last2RootEvalCp);
    double volatility = 1.0 + Math.min(delta / 120.0, 1.0); // in [1..2]

    // --- 5) Combine ---
    long allocated = (long) (base * complexity * volatility);

    // --- 6) Safety caps ---
    long maxAllowed = (long) (remainingMs * MAX_RATIO);
    if (incrementMs > 0) maxAllowed += (long)(incrementMs * 0.5); // allow a bit more with increment

    if (allocated > maxAllowed) allocated = maxAllowed;
    if (allocated < MIN_TIME_MS) allocated = MIN_TIME_MS;

    // Don't ever allocate basically all remaining time
    if (allocated > remainingMs - 50) allocated = Math.max(MIN_TIME_MS, remainingMs - 50);

    return allocated;
  }
}
