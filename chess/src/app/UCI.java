package app;


import engine.board.Board;
import engine.move.Move;
import engine.search.Search;
import engine.common.Constants;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

public final class UCI {

  private final Board board = new Board();
  private final Search search;

  public UCI(Search search) {
    this.search = search;
  }

  public void loop() throws Exception {
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    String line;

    while ((line = br.readLine()) != null) {
      if (line.equals("uci")) {
        System.out.println("id name YourEngine");
        System.out.println("id author You");
        System.out.println("uciok");
      }

      else if (line.equals("isready")) {
        System.out.println("readyok");
      }

      else if (line.startsWith("position")) {
        parsePosition(line);
      }

      else if (line.startsWith("go")) {
        parseGo(line);
      }

      else if (line.equals("ucinewgame")) {
        board.clear();
        board.loadFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        //search.clear();
      }


      else if (line.equals("quit")) {
        break;
      }
    }
  }

  // ----------------------------

  private void parsePosition(String line) {
    StringTokenizer st = new StringTokenizer(line);
    st.nextToken();

    if (!st.hasMoreTokens()) return;

    String token = st.nextToken();

    if (token.equals("startpos")) {
      board.loadFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
    }
    else if (token.equals("fen")) {
      StringBuilder fen = new StringBuilder();
      int fenParts = 0;
      while (st.hasMoreTokens() && fenParts < 6) {
        fen.append(st.nextToken()).append(" ");
        fenParts++;
      }
      board.loadFEN(fen.toString().trim());
    }


    // moves
    if (st.hasMoreTokens()) {
      token = st.nextToken();
      if (token.equals("moves")) {
        while (st.hasMoreTokens()) {
          String moveStr = st.nextToken();
          int move = Move.fromUCI(board, moveStr);
          board.makeMove(move);
        }
      }
    }
  }

  private void parseGo(String line) {
    int depth = -1;
    long wtime = -1, btime = -1;
    long winc = 0, binc = 0;

    StringTokenizer st = new StringTokenizer(line);
    st.nextToken(); // "go"

    while (st.hasMoreTokens()) {
      String tok = st.nextToken();
      switch (tok) {
        case "depth" -> depth = Integer.parseInt(st.nextToken());
        case "wtime" -> wtime = Long.parseLong(st.nextToken());
        case "btime" -> btime = Long.parseLong(st.nextToken());
        case "winc"  -> winc  = Long.parseLong(st.nextToken());
        case "binc"  -> binc  = Long.parseLong(st.nextToken());
      }
    }

    long timeForMove = computeTimeForMove(wtime, btime, winc, binc);
    //int bestMove = search.searchTimed(board, depth, timeForMove);

    //System.out.println("bestmove " + Move.toUCI(bestMove));
  }

  private long computeTimeForMove(long wtime, long btime, long winc, long binc) {
    boolean white = board.sideToMove == Constants.WHITE;

    long time = white ? wtime : btime;
    long inc  = white ? winc  : binc;

    if (time <= 0) return 100; // panic

    // Typical: use ~1/30 of remaining time
    return Math.max(50, time / 30 + inc / 2);
  }

}
