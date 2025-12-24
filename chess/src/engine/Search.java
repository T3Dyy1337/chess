package engine;

import util.Constants;
import static engine.ClassicalEvaluator.evaluate;

public final class Search {
    public static final int INF = 100_000;
    public static final int MATE = 30000;

    private static final int Q_MAX_PLY = 16;


    private static final int HISTORY_MAX = 200_000;

    private final SearchContext context = new SearchContext();

    private final TranspositionTable tt = new TranspositionTable(64); // MB, adjust later

    public int nodes;

    private int rootBestMove;
    private int previousBestMove;
    int previousScore;

    // Killer moves: [ply][0 or 1]
    private final int[][] killerMoves = new int[128][2];

    // History heuristic: [side][toSquare]
    private final int[][][] history = new int[2][64][64];




    public int search(Board board, int maxDepth) {
        nodes = 0;
        rootBestMove = 0;
        previousBestMove = 0;

        int previousScore = 0;


        for (int i = 0; i < killerMoves.length; i++) {
            killerMoves[i][0] = 0;
            killerMoves[i][1] = 0;
        }

        for (int s = 0; s < 2; s++) {
            for (int from = 0; from < 64; from++) {
                for (int to = 0; to < 64; to++) {
                    history[s][from][to] >>= 1;
                }
            }
        }



        for (int depth = 1; depth <= maxDepth; depth++) {
            rootBestMove = previousBestMove;
            int window = 50;
            int alpha = previousScore - window;
            int beta  = previousScore + window;

            int score = alphaBeta(board, depth, alpha, beta, 0,true);

            if (score <= alpha || score >= beta) {
                score = alphaBeta(board, depth, -INF, INF, 0,true);
            }

            // save PV root move for next iteration
            previousBestMove = rootBestMove;

            previousScore = score;

        }

        return rootBestMove;
    }


    private int alphaBeta(Board board, int depth, int alpha, int beta, int ply, boolean allowNull) {
        nodes++;

        if (ply > 0 && board.isRepetition()) {
            return 0;
        }

        if (board.halfmoveClock >= 100) return 0;

        if (depth == 0) {
            return quiescence(board, alpha, beta, ply);
        }

        int initAlpha = alpha;

        TranspositionTable.ProbeResult ttResult = tt.probe(board.zobristKey,depth,alpha,beta,ply);

        int ttMove = ttResult.bestMove;

        switch (ttResult.status) {
            case TranspositionTable.EXACT_HIT:
                return ttResult.score;

            case TranspositionTable.BETA_CUTOFF:
                return ttResult.score;

            case TranspositionTable.ALPHA_CUTOFF:
                return ttResult.score;

            case TranspositionTable.SHALLOW_HIT:
                ttMove = ttResult.bestMove;
                break;

            case TranspositionTable.MISS:
                break;
        }

        boolean endgame = board.nonPawnMaterial(board.sideToMove) <= 5;

        if (endgame) {
            allowNull = false;
        }

        if (allowNull && depth >= 3
            && !board.isInCheck()
            && board.nonPawnMaterial(board.sideToMove) >= 8){

            // make null move
            board.makeNullMove();

            int score = -alphaBeta(
                board,
                depth - 1 - 2,
                -beta,
                -beta + 1,
                ply + 1, false);

            board.unmakeNullMove();

            if (score >= beta) {
                return score;
            }
        }


        int[] moves = context.moves[ply];
        int[] scores = context.scores[ply];
        int count = MoveGenerator.generateAllMoves(board,moves);

        if (ttMove == 0 && ply == 0) {
            ttMove = previousBestMove;
        }


        //score moves using MVV-LVA
        for (int i = 0; i < count; i++) {
            scores[i] = scoreMove(board, moves[i], ttMove, ply);
        }

        int bestMove = 0;
        int bestEval = -INF;
        boolean hasLegalMove = false;

        boolean searchedOneLegal = false;

        for (int i = 0; i < count; i++) {
            // pick best remaining move
            int bestIdx = i;
            int bestScore = scores[i];
            for (int j = i + 1; j < count; j++) {
                if (scores[j] > bestScore) {
                    bestScore = scores[j];
                    bestIdx = j;
                }
            }

            // swap move + score
            int move = moves[bestIdx];
            moves[bestIdx] = moves[i];
            moves[i] = move;

            int tmp = scores[bestIdx];
            scores[bestIdx] = scores[i];
            scores[i] = tmp;

            int moverSide = board.sideToMove;
            boolean inCheck = board.isInCheck();

            board.makeMove(move);

            // legality check (pseudo-legal generator)
            int us = board.sideToMove ^ 1;
            int kingSq = (us == Constants.WHITE) ? board.whiteKingSq : board.blackKingSq;
            if (AttackGenerator.isSquareAttacked(board, kingSq, board.sideToMove)) {
                board.unmakeMove();
                continue;
            }

            hasLegalMove = true;


            int score;

            if (!searchedOneLegal) {
                // First legal move: full window, full depth
                searchedOneLegal = true;
                score = -alphaBeta(board, depth - 1, -beta, -alpha, ply + 1, true);
            } else {
                boolean quiet = !Move.isCapture(move);
                int reduction = 0;

                // Late Move Reductions (safe version)
                if (!inCheck && quiet && depth >= 3 && i >= 3) {
                    reduction = 1;
                    if (i >= 8 && depth >= 5) reduction = 2;
                }

                int newDepth = (depth - 1) - reduction;

                // Reduced-depth scout search
                score = -alphaBeta(board, newDepth, -alpha - 1, -alpha, ply + 1, true);

                // If it improves alpha, re-search full depth
                if (score > alpha) {
                    score = -alphaBeta(board, depth - 1, -alpha - 1, -alpha, ply + 1, true);

                    // If still not fail-high, do full window (standard PVS)
                    if (score > alpha && score < beta) {
                        score = -alphaBeta(board, depth - 1, -beta, -alpha, ply + 1, true);
                    }
                }
            }

            board.unmakeMove();

            if (score > bestEval) {
                bestEval = score;
                bestMove = move;
                if (ply == 0) rootBestMove = move;
            }

            if (score > alpha) {
                alpha = score;
            }

            if (alpha >= beta) {

                // Update killer & history for quiet moves only
                if (!Move.isCapture(move)) {
                    if (killerMoves[ply][0] != move) {
                        killerMoves[ply][1] = killerMoves[ply][0];
                        killerMoves[ply][0] = move;
                    }

                    if (ply > 0 && !Move.isCapture(move)) {
                        updateHistory(moverSide, move, depth);
                    }



                }

                break;
            }

        }

        if (!hasLegalMove)
        {
            if (board.isInCheck())
            {
                return -(MATE - ply); // checkmate
            }
            else
            {
                return 0; // stalemate
            }
        }



        int flag;
        if (bestEval <= initAlpha) {
            flag = TranspositionTable.UPPER;
        } else if (bestEval >= beta) {
            flag = TranspositionTable.LOWER;
        } else {
            flag = TranspositionTable.EXACT;
        }
        if (Math.abs(bestEval) < MATE - 1000)
            tt.store(
                board.zobristKey,
                depth,
                flag,
                bestEval,
                bestMove,
                ply
            );


        return bestEval;
    }

    private int quiescence(Board board, int alpha, int beta, int ply)
    {
        nodes++;
        if (board.halfmoveClock >= 100) return 0;

        if (ply >= Q_MAX_PLY) {
            return evaluate(board);
        }

        int standPat = evaluate(board);

        if (standPat >= beta) {
            return standPat;
        }
        if (standPat > alpha) {
            alpha = standPat;
        }

        int[] moves = context.moves[ply];
        int[] scores = context.scores[ply];
        int count = MoveGenerator.generateCaptures(board, moves);



        for (int i = 0; i < count; i++) {
            scores[i] = scoreMove(board, moves[i], 0, ply);
        }

        for (int i = 0; i < count; i++)
        {
            // pick best remaining move
            int bestIdx = i;
            int bestScore = scores[i];

            for (int j = i + 1; j < count; j++) {
                if (scores[j] > bestScore) {
                    bestScore = scores[j];
                    bestIdx = j;
                }
            }

            // swap move + score
            int move = moves[bestIdx];
            moves[bestIdx] = moves[i];
            moves[i] = move;

            int tmp = scores[bestIdx];
            scores[bestIdx] = scores[i];
            scores[i] = tmp;

            // === now process moves[i] ===

            board.makeMove(move);

            int us = board.sideToMove ^ 1;
            int kingSq = (us == Constants.WHITE)
                ? board.whiteKingSq
                : board.blackKingSq;

            if (AttackGenerator.isSquareAttacked(board, kingSq, board.sideToMove)) {
                board.unmakeMove();
                continue;
            }

            int score = -quiescence(board, -beta, -alpha, ply + 1);
            board.unmakeMove();

            if (score >= beta) return score;
            if (score > alpha) alpha = score;
        }

        return alpha;
    }

    private int scoreMove(Board board, int move, int ttMove, int ply) {
        if (move == ttMove) return 1_000_000;

        if (Move.isCapture(move)) {
            int to = Move.to(move);
            int victim = board.getPieceOn(to);

            if (victim == -1 && Move.flags(move) == Constants.EN_PASSANT) {
                victim = (board.sideToMove == Constants.WHITE)
                    ? Constants.B_PAWN
                    : Constants.W_PAWN;
            }

            int attacker = board.getPieceOn(Move.from(move));
            return 100_000 + pieceValue(victim) * 10 - pieceValue(attacker);
        }

        // Quiet moves
        if (move == killerMoves[ply][0]) return 90_000;
        if (move == killerMoves[ply][1]) return 80_000;

        return history[board.sideToMove][Move.from(move)][Move.to(move)];
    }

    private int pieceValue(int piece) {
        return switch (piece) {
            case Constants.W_PAWN,   Constants.B_PAWN   -> 1;
            case Constants.W_KNIGHT,Constants.B_KNIGHT -> 3;
            case Constants.W_BISHOP,Constants.B_BISHOP -> 3;
            case Constants.W_ROOK,  Constants.B_ROOK   -> 5;
            case Constants.W_QUEEN, Constants.B_QUEEN  -> 9;
            default -> 0;
        };
    }

    private void updateHistory(int moverSide, int move, int depth) {
        int from = Move.from(move);
        int to   = Move.to(move);
        int v = history[moverSide][from][to] + depth * depth;
        history[moverSide][from][to] = Math.min(v, HISTORY_MAX);
    }


}

