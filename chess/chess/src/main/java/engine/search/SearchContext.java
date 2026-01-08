package engine.search;


public final class SearchContext {
  public static final int MAX_PLY = 128;
  public static final int MAX_MOVES = 256;

  public final int[][] moves = new int[MAX_PLY][MAX_MOVES];
  public final int[] moveCounts = new int[MAX_PLY];

  public final int[][] scores = new int[MAX_PLY][MAX_MOVES];

  public final int[][] killerMoves = new int[128][2];

  public final int[][][] history = new int[2][64][64];

}

