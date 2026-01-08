package engine.api;

import engine.board.Board;

public interface ChessEngine {

  /** Reset engine state (hash, heuristics, history) */
  void reset();

  /** Set position to analyze / play from */
  void setPosition(Board board);

  /** Configure search parameters */
  void setSearchLimits(SearchLimits limits);

  /** Start computing best move (blocking call) */
  EngineResult computeMove();

}

