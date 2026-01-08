package engine.api;

import engine.move.Move;

public final class EngineResult {

  public final Move bestMove;
  public final int scoreCp;
  public final int depth;
  public final long nodes;
  public final long timeMs;

  public EngineResult(Move bestMove, int scoreCp, int depth, long nodes,
      long timeMs)
  {
    this.bestMove = bestMove;
    this.scoreCp = scoreCp;
    this.depth = depth;
    this.nodes = nodes;
    this.timeMs = timeMs;
  }


}

