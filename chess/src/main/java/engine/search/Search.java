package engine.search;

import engine.board.Board;
import engine.eval.ClassicalEvaluator;
import engine.move.AttackGenerator;
import engine.move.Move;
import engine.move.MoveGenerator;
import engine.common.Constants;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import static engine.eval.ClassicalEvaluator.evaluate;

public final class Search {

    public static final int INF = 100_000;
    public static final int MATE = 30000;

    private static final int Q_MAX_PLY = 16;


    private static final int HISTORY_MAX = 200_000;

    public static final AtomicLong GLOBAL_NODES = new AtomicLong();


    private final SearchContext context = new SearchContext();

    private final TranspositionTable tt;

    public long nodes;

    private int rootBestMove;
    private int previousBestMove;

    private long startTimeMs;
    private long timeLimitMs;
    private volatile boolean stop;

    public int lastCompletedRootScore;

    public Search(TranspositionTable tt) {
        this.tt = tt;
    }

    public int search(Board board, long timeForMoveMs) {
        this.startTimeMs = System.currentTimeMillis();
        this.timeLimitMs = timeForMoveMs;
        this.stop = false;

        return search(board, 64);
    }


    public int search(Board board, int maxDepth) {

        final int THREADS = Runtime.getRuntime().availableProcessors();
        System.out.println(THREADS);

        long startNodes = GLOBAL_NODES.get();


        if (THREADS == 1) {
            return searchSingle(board, maxDepth);
        }

        tt.increaseGeneration();

        ExecutorService pool =
            java.util.concurrent.Executors.newFixedThreadPool(THREADS);

        java.util.List<java.util.concurrent.Future<Integer>> futures =
            new java.util.ArrayList<>();

        for (int t = 0; t < THREADS; t++) {
            Board copy = board.copy();
            Search worker = new Search(tt);
            worker.startTimeMs = this.startTimeMs;
            worker.timeLimitMs = this.timeLimitMs;
            worker.stop        = false;
            futures.add(pool.submit(() -> worker.searchSingle(copy, maxDepth)));
        }

        int bestMove = 0;

        for (var f : futures) {
            try {
                int move = f.get();
                if (move != 0 && bestMove == 0) {
                    bestMove = move;
                }
            } catch (Exception ignored) {}
        }

        long endNodes = GLOBAL_NODES.get();
        this.nodes = endNodes - startNodes;

        pool.shutdown();
        return bestMove;
    }



    public int searchSingle(Board board, int maxDepth) {
        nodes = 0;
        rootBestMove = 0;
        previousBestMove = 0;

        int previousScore = 0;


        for (int i = 0; i < context.killerMoves.length; i++) {
            context.killerMoves[i][0] = 0;
            context.killerMoves[i][1] = 0;
        }

        for (int s = 0; s < 2; s++) {
            for (int from = 0; from < 64; from++) {
                for (int to = 0; to < 64; to++) {
                    context.history[s][from][to] >>= 1;
                }
            }
        }

        for (int depth = 1; depth <= maxDepth; depth++) {
            if (System.currentTimeMillis() - startTimeMs > timeLimitMs) {
                stop = true;
                break;
            }

            rootBestMove = previousBestMove;

            int window = 50;
            int alpha = previousScore - window;
            int beta  = previousScore + window;

            int score = alphaBeta(board, depth, alpha, beta, 0,true);

            if (stop) break;

            if (score <= alpha || score >= beta) {
                score = alphaBeta(board, depth, -INF, INF, 0,true);
            }

            if (stop) break;

            previousBestMove = rootBestMove;
            previousScore = score;
            lastCompletedRootScore = previousScore;

        }

        return rootBestMove;
    }


    private int alphaBeta(Board board, int depth, int alpha, int beta, int ply, boolean allowNull) {
        GLOBAL_NODES.incrementAndGet();

        if (ply > 0 && board.isRepetition()) {
            return 0;
        }

        if (board.halfmoveClock >= 100) return 0;

        if (stop) return 0;

        if (!stop && System.currentTimeMillis() - startTimeMs > timeLimitMs) {
            stop = true;
            return 0;
        }


        if (depth == 0) {
            return quiescence(board, alpha, beta, ply);
        }

        int initAlpha = alpha;

        TranspositionTable.ProbeResult ttResult = tt.probe(board.zobristKey,depth,alpha,beta,ply);

        int ttMove = ttResult.bestMove;

        switch (ttResult.status) {
            case TranspositionTable.EXACT_HIT, TranspositionTable.BETA_CUTOFF, TranspositionTable.ALPHA_CUTOFF:
                if (ply == 0 && ttMove != 0){
                    rootBestMove = ttMove;
                }
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


        boolean inCheck = board.isInCheck();

        if (!inCheck && depth <= 2) {
            int staticEval = evaluate(board);

            int margin = 200 * depth;

            if (staticEval + margin <= alpha) {
                return staticEval;
            }
        }



        int[] moves = context.moves[ply];
        int[] scores = context.scores[ply];
        int count = MoveGenerator.generateAllMoves(board,moves);

        if (ttMove == 0 && ply == 0) {
            ttMove = previousBestMove;
        }


        for (int i = 0; i < count; i++) {
            scores[i] = scoreMove(board, moves[i], ttMove, ply);
        }

        int bestMove = 0;
        int bestEval = -INF;
        boolean hasLegalMove = false;

        boolean searchedOneLegal = false;

        for (int i = 0; i < count; i++) {
            int bestIdx = i;
            int bestScore = scores[i];
            for (int j = i + 1; j < count; j++) {
                if (scores[j] > bestScore) {
                    bestScore = scores[j];
                    bestIdx = j;
                }
            }

            int move = moves[bestIdx];
            moves[bestIdx] = moves[i];
            moves[i] = move;

            int tmp = scores[bestIdx];
            scores[bestIdx] = scores[i];
            scores[i] = tmp;

            int moverSide = board.sideToMove;

            board.makeMove(move);

            int us = board.sideToMove ^ 1;
            int kingSq = (us == Constants.WHITE) ? board.whiteKingSq : board.blackKingSq;
            if (AttackGenerator.isSquareAttacked(board, kingSq, board.sideToMove)) {
                board.unmakeMove();
                continue;
            }

            hasLegalMove = true;
            inCheck = board.isInCheck();

            boolean quiet = !Move.isCapture(move);

            if (!inCheck &&
                quiet &&
                depth <= 3 &&
                i > 8 &&
                !board.isRepetition())
            {
                board.unmakeMove();
                continue;
            }

            int score;

            if (!searchedOneLegal) {
                searchedOneLegal = true;
                score = -alphaBeta(board, depth - 1, -beta, -alpha, ply + 1, true);
            } else {
                quiet = !Move.isCapture(move);
                int reduction = 0;

                if (!inCheck && quiet && depth >= 3 && i >= 3) {
                    reduction = 1;
                    if (i >= 8 && depth >= 5) reduction = 2;
                }

                int newDepth = (depth - 1) - reduction;
                score = -alphaBeta(board, newDepth, -alpha - 1, -alpha, ply + 1, true);

                if (score > alpha) {
                    score = -alphaBeta(board, depth - 1, -beta, -alpha, ply + 1, true);
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
                if (!Move.isCapture(move)) {
                    if (context.killerMoves[ply][0] != move) {
                        context.killerMoves[ply][1] = context.killerMoves[ply][0];
                        context.killerMoves[ply][0] = move;
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
                return -(MATE - ply);
            }
            else
            {
                return 0;
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
        GLOBAL_NODES.incrementAndGet();
        if (board.halfmoveClock >= 100) return 0;

        if (ply >= Q_MAX_PLY) {
            return ClassicalEvaluator.evaluate(board);
        }

        if (stop) return alpha;
        if (!stop && System.currentTimeMillis() - startTimeMs > timeLimitMs) {
            stop = true;
            return alpha;
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
            int bestIdx = i;
            int bestScore = scores[i];

            for (int j = i + 1; j < count; j++) {
                if (scores[j] > bestScore) {
                    bestScore = scores[j];
                    bestIdx = j;
                }
            }

            int move = moves[bestIdx];
            moves[bestIdx] = moves[i];
            moves[i] = move;

            int tmp = scores[bestIdx];
            scores[bestIdx] = scores[i];
            scores[i] = tmp;

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

        if (move == context.killerMoves[ply][0]) return 90_000;
        if (move == context.killerMoves[ply][1]) return 80_000;

        return context.history[board.sideToMove][Move.from(move)][Move.to(move)];
    }

    private int pieceValue(int piece) {
        return switch (piece) {
            case Constants.W_PAWN, Constants.B_PAWN   -> 1;
            case Constants.W_KNIGHT, Constants.B_KNIGHT -> 3;
            case Constants.W_BISHOP, Constants.B_BISHOP -> 3;
            case Constants.W_ROOK, Constants.B_ROOK   -> 5;
            case Constants.W_QUEEN, Constants.B_QUEEN  -> 9;
            default -> 0;
        };
    }

    private void updateHistory(int moverSide, int move, int depth) {
        int from = Move.from(move);
        int to   = Move.to(move);
        int v = context.history[moverSide][from][to] + depth * depth;
        context.history[moverSide][from][to] = Math.min(v, HISTORY_MAX);
    }


}

