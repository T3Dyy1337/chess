package engine;

import util.BitHelper;
import util.Constants;

import static java.lang.Math.max;
import static java.lang.Math.min;

public final class Search {

    public int nodesSearched;

    private final TranspositionTable tt;

    private static final int MAX_PLY = 128;
    private static final int MAX_CAPTURES = 128;

    private final int[][] moveList;
    private final int[][] moveScores;

    private final int[][] captureMoves;
    private final int[][] captureScores;

    private final int[][] killerMoves;
    private final int[][] history;

    public final int[][] pvMoves;
    public final int[] pvLength;

    public static final int MATE = 32000;
    private static final int DRAW = 0;
    private static final int INF = 1_000_000;

    public Search(TranspositionTable tt) {
        this.tt = tt;

        moveList = new int[MAX_PLY][256];
        moveScores = new int[MAX_PLY][256];

        captureMoves = new int[MAX_PLY][MAX_CAPTURES];
        captureScores = new int[MAX_PLY][MAX_CAPTURES];

        killerMoves = new int[MAX_PLY][2];
        history = new int[64][64];

        pvMoves = new int[MAX_PLY][MAX_PLY];
        pvLength = new int[MAX_PLY];
    }

    public int searchTimed(Board board, int maxDepth, long timeMs) {
        long endTime = System.currentTimeMillis() + timeMs;
        int bestMove = 0;

        for (int depth = 1; depth <= (maxDepth > 0 ? maxDepth : 64); depth++) {
            if (System.currentTimeMillis() >= endTime) break;

            int move = search(board, depth);
            bestMove = move;
        }
        return bestMove;
    }


    public int search(Board board, int maxDepth){
        nodesSearched = 0;

        int bestMove = 0;
        int bestScore = 0;

        int score = 0;

        for(int depth = 1; depth <= maxDepth; depth++){
            tt.increaseGeneration();
            pvLength[0] = 0;

            int window = 50;
            int alpha = Math.max(-INF, score - window);
            int beta  = Math.min( INF, score + window);

            while (true) {
                int r = alphaBeta(board, depth, alpha, beta, 0);
                if (r <= alpha) { window <<= 1; alpha = Math.max(-INF, score - window); }
                else if (r >= beta) { window <<= 1; beta = Math.min(INF, score + window); }
                else { score = r; break; }
                if (Math.abs(r) > MATE - 1000) {
                    score = r;
                    break;
                }

            }

            if(pvLength[0] > 0){
                bestMove = pvMoves[0][0];
            }
        }
        bestScore = score;
        System.out.println(bestScore);
        return bestMove;
    }


    private int alphaBeta(Board board, int depth, int alpha, int beta, int ply){
        nodesSearched++;

        int maxMate = MATE - ply;
        if (alpha < -maxMate) alpha = -maxMate;
        if (beta  >  maxMate) beta  =  maxMate;
        if (alpha >= beta) return alpha;


        pvLength[ply] = 0;
        boolean inCheck = board.isInCheck();
        if (inCheck && ply < MAX_PLY - 1) {
            depth++;
        }




        int ttMove = 0;
        TranspositionTable.ProbeResult probe = tt.probe(board.zobristKey,depth,alpha,beta,ply);


        if (probe.status == TranspositionTable.EXACT_HIT) {
            if (probe.bestMove != 0) {
                pvMoves[ply][0] = probe.bestMove;
                pvLength[ply] = 1;
            } else {
                pvLength[ply] = 0;
            }
            return probe.score;
        }
        if (probe.status == TranspositionTable.BETA_CUTOFF ||
            probe.status == TranspositionTable.ALPHA_CUTOFF) {
            return probe.score;
        }
        if(probe.status == TranspositionTable.SHALLOW_HIT) ttMove = probe.bestMove;

        int initAlpha = alpha;

        if (depth == 0)
            return quiescence(board, alpha, beta, ply);

        if (depth <= 3 && !inCheck) {
            int staticEval = ClassicalEvaluator.evaluate(board);
            int margin = 100 * depth;

            if (staticEval - margin >= beta) {
                return staticEval;
            }
        }


        int[] moves = moveList[ply];
        int moveCount = MoveGenerator.generateAllMoves(board,moves);

        if (moveCount == 0) {
            if (inCheck) {
                return -MATE + ply;
            } else {
                return DRAW;
            }
        }

        if (ttMove != 0) {
            for (int i = 0; i < moveCount; i++) {
                if (moves[i] == ttMove) {
                    int temp = moves[0];
                    moves[0] = moves[i];
                    moves[i] = temp;
                    break;
                }
            }
        }


        int[] scores = moveScores[ply];

        for (int i = 0; i < moveCount; i++) {
            moveScores[ply][i] = scoreMove(board, moves[i], ply);
        }


        int bestScore = -INF;
        int bestMove = 0;

        for (int i = 0; i < moveCount; i++)
        {

            int best = i;
            int pickScore = moveScores[ply][i];

            for (int j = i + 1; j < moveCount; j++) {
                int s = moveScores[ply][j];
                if (s > pickScore) {
                    pickScore = s;
                    best = j;
                }
            }

            if (best != i) {
                int tmp = moves[i]; moves[i] = moves[best]; moves[best] = tmp;
                tmp = moveScores[ply][i]; moveScores[ply][i] = moveScores[ply][best]; moveScores[ply][best] = tmp;
            }


            int move = moves[i];

            board.makeMove(move);

            int mover = board.sideToMove ^ 1;
            int kingSq = (mover == Constants.WHITE)
                ? board.whiteKingSq
                : board.blackKingSq;

            if (AttackGenerator.isSquareAttacked(board, kingSq, board.sideToMove)) {
                board.unmakeMove();
                continue;
            }




            boolean isQuiet = isQuiet(move);
            int score;

            if (i == 0) {
                score = -alphaBeta(board, depth - 1, -beta, -alpha, ply + 1);
            } else {
                int newDepth = depth - 1;

                if (depth >= 3
                    && i >= 4
                    && isQuiet
                    && !inCheck) {

                    newDepth = Math.max(0, newDepth - 1);
                }

                score = -alphaBeta(board, newDepth, -alpha - 1, -alpha, ply + 1);

                if (score > alpha) {
                    score = -alphaBeta(board, depth - 1, -beta, -alpha, ply + 1);
                }
            }

            board.unmakeMove();

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }

            if (score > alpha) {
                alpha = score;

                pvMoves[ply][0] = move;

                int nextLen = pvLength[ply + 1];
                for (int k = 0; k < nextLen; k++) {
                    pvMoves[ply][k + 1] = pvMoves[ply + 1][k];
                }
                pvLength[ply] = nextLen + 1;
            }

            if(alpha >= beta){
                int f = Move.flags(move);
                boolean quiet = (f == Constants.QUIET || f == Constants.DOUBLE_PAWN_PUSH ||
                    f == Constants.KING_CASTLE || f == Constants.QUEEN_CASTLE);

                if (quiet) {
                    if (killerMoves[ply][0] != move) {
                        killerMoves[ply][1] = killerMoves[ply][0];
                        killerMoves[ply][0] = move;
                    }
                    history[Move.from(move)][Move.to(move)] += depth * depth;
                }

                break;
            }

        }

        int flag;
        if (bestScore <= initAlpha) {
            flag = TranspositionTable.UPPER;
        } else if (bestScore >= beta) {
            flag = TranspositionTable.LOWER;
        } else {
            flag = TranspositionTable.EXACT;
        }

        tt.store(board.zobristKey,depth,flag,bestScore,bestMove,ply);


        return bestScore;
    }

    private int quiescence(Board board, int alpha, int beta, int ply) {
        nodesSearched++;
        boolean inCheck = board.isInCheck();
        if (inCheck) {
            int[] moves = captureMoves[ply];
            int moveCount = MoveGenerator.generateAllMoves(board, moves);

            for (int i = 0; i < moveCount; i++) {
                int move = moves[i];

                board.makeMove(move);

                int mover = board.sideToMove ^ 1;
                int kingSq = (mover == Constants.WHITE)
                    ? board.whiteKingSq
                    : board.blackKingSq;

                // Only keep moves that actually evade check
                if (AttackGenerator.isSquareAttacked(board, kingSq, board.sideToMove)) {
                    board.unmakeMove();
                    continue;
                }

                int score = -quiescence(board, -beta, -alpha, ply + 1);
                board.unmakeMove();

                if (score >= beta) return beta;
                if (score > alpha) alpha = score;
            }
            return alpha;
        }
        int standPat = ClassicalEvaluator.evaluate(board);
        if (standPat >= beta) return beta;
        if (standPat > alpha) alpha = standPat;

        int[] moves = captureMoves[ply];
        int moveCount = MoveGenerator.generateCaptures(board, moves);

        int[] scores = captureScores[ply];

        for (int i = 0; i < moveCount; i++) {
            scores[i] = mvvLva(board, moves[i]);
        }

        for (int i = 0; i < moveCount; i++) {
            int best = i;
            int bestS = scores[i];
            for (int j = i + 1; j < moveCount; j++) {
                if (scores[j] > bestS) { bestS = scores[j]; best = j; }
            }
            if (best != i) {
                int tmp = moves[i]; moves[i] = moves[best]; moves[best] = tmp;
                tmp = scores[i]; scores[i] = scores[best]; scores[best] = tmp;
            }

            int move = moves[i];

            if (isBadCapture(board, move)) {
                continue;
            }


            int to = Move.to(move);
            int victim = board.getPieceOn(to);
            int gain = (victim == -1) ? 0 : Constants.PIECE_VALUE[victim];

            if (standPat + gain + 100 < alpha) continue;


            board.makeMove(move);

            int mover = board.sideToMove ^ 1;
            int kingSq = (mover == Constants.WHITE)
                ? board.whiteKingSq
                : board.blackKingSq;

            if (AttackGenerator.isSquareAttacked(board, kingSq, board.sideToMove)) {
                board.unmakeMove();
                continue;
            }

            int score = -quiescence(board, -beta, -alpha, ply + 1);

            board.unmakeMove();

            if (score >= beta) return beta;
            if (score > alpha) alpha = score;
        }

        return alpha;
    }


    private int mvvLva(Board b, int move) {
        int from = Move.from(move);
        int to   = Move.to(move);

        int attacker = b.getPieceOn(from);
        int victim   = b.getPieceOn(to);

        if (victim == -1) return 0;

        return Constants.PIECE_VALUE[victim] * 1000
            - Constants.PIECE_VALUE[attacker];
    }


    boolean givesCheck(Board board, int move) {
        board.makeMove(move);

        int enemyKingSq = (board.sideToMove == Constants.WHITE)
            ? board.whiteKingSq
            : board.blackKingSq;

        boolean check = AttackGenerator.isSquareAttacked(
            board,
            enemyKingSq,
            board.sideToMove ^ 1
        );

        board.unmakeMove();
        return check;
    }


    private int scoreMove(Board b, int move, int ply) {
        int from = Move.from(move);
        int to   = Move.to(move);
        int flag = Move.flags(move);

        // Captures first (MVV-LVA)
        if (flag == Constants.CAPTURE || flag == Constants.EN_PASSANT ||
            (flag >= Constants.PROMO_KNIGHT_CAPTURE && flag <= Constants.PROMO_QUEEN_CAPTURE)) {

            int attacker = b.getPieceOn(from);
            int victimSq = (flag == Constants.EN_PASSANT)
                ? (b.sideToMove == Constants.WHITE ? to - 8 : to + 8)
                : to;
            int victim = b.getPieceOn(victimSq);

            if (victim == -1) return 0; // defensive
            return 1_000_000 + Constants.PIECE_VALUE[victim] * 1000 - Constants.PIECE_VALUE[attacker];
        }

        // Killer moves
        if (move == killerMoves[ply][0]) return 900_000;
        if (move == killerMoves[ply][1]) return 800_000;

        // History
        return history[from][to];
    }

    private boolean isQuiet(int move) {
        int flag = Move.flags(move);

        return flag == Constants.QUIET
            || flag == Constants.DOUBLE_PAWN_PUSH
            || flag == Constants.KING_CASTLE
            || flag == Constants.QUEEN_CASTLE;
    }

    private boolean isBadCapture(Board b, int move) {
        int from = Move.from(move);
        int to   = Move.to(move);

        int attacker = b.getPieceOn(from);
        if (attacker == -1) {
            // Illegal / stale move (TT or ordering artifact)
            return true; // safest: skip it
        }

        int victimSq = to;
        if (Move.flags(move) == Constants.EN_PASSANT) {
            victimSq = (b.sideToMove == Constants.WHITE) ? to - 8 : to + 8;
        }

        int victim = b.getPieceOn(victimSq);
        if (victim == -1) {
            // No victim → not a real capture
            return false;
        }

        // Losing capture?
        if (Constants.PIECE_VALUE[victim] < Constants.PIECE_VALUE[attacker]) {
            // Is the capture square defended?
            if (AttackGenerator.isSquareAttacked(b, to, b.sideToMove ^ 1)) {
                return true;
            }
        }

        return false;
    }


    public void clear() {
        tt.clear();
        nodesSearched = 0;
        for (int i = 0; i < killerMoves.length; i++) {
            killerMoves[i][0] = 0;
            killerMoves[i][1] = 0;
        }
        for (int i = 0; i < history.length; i++) {
            for (int j = 0; j < history[i].length; j++) {
                history[i][j] = 0;
            }
        }
    }



}

