package engine.api;

public final class SearchLimits {

  public final long timeMs;
  public final int depth;
  public final int nodes;
  public final int threads;

  public SearchLimits(long timeMs, int depth, int nodes, int threads)
  {
    this.timeMs = timeMs;
    this.depth = depth;
    this.nodes = nodes;
    this.threads = threads;
  }

  public long getTimeMs()
  {
    return timeMs;
  }

  public int getDepth()
  {
    return depth;
  }

  public int getNodes()
  {
    return nodes;
  }

  public int getThreads()
  {
    return threads;
  }
}

