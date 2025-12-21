package engine;

import util.BitHelper;
import util.Constants;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class Search {

    private final NeuralEval eval; // kept (even if you mostly use ClassicalEvaluator now)

    public int nodesSearched;

    private final TranspositionTable transpositionTable;

    private static final int MAX_PLY = 128;
    private static final int MAX_CAPTURES = 128;

    private final int[][] captureMoves;
    private final int[][] captureScores;

    public final int[][] pvMoves;
    public final int[] pvLength;

    private final int[][] moveList;

    private final int[][] killerMoves;
    private final int[][] history;
    private final int[][] moveScores;


    public static final int MATE = 32000;
    private static final int DRAW = 0;
    private static final int INF = 1_000_000;

    public Search(NeuralEval eval, TranspositionTable transpositionTable) {
        this.eval = eval;
        this.transpositionTable = transpositionTable;

        this.pvMoves = new int[MAX_PLY][MAX_PLY];
        this.pvLength = new int[MAX_PLY];

        this.captureMoves = new int[MAX_PLY][MAX_CAPTURES];
        this.captureScores = new int[MAX_PLY][MAX_CAPTURES];

        this.moveList = new int[MAX_PLY][256];
        this.killerMoves = new int[MAX_PLY][2];
        this.history = new int[64][64];
        this.moveScores = new int[MAX_PLY][256];

    }

    public int search(Board board, int depth) {
        int score = 0;
        int bestMove = 0;

        for (int d = 1; d <= depth; d++) {
            transpositionTable.increaseGeneration();

            pvLength[0] = 0;
            nodesSearched = 0;

            int window = 50;
            int alpha = max(-INF, score - window);
            int beta  = min( INF, score + window);

            while (true) {
                int result = alphaBeta(board, d, alpha, beta, 0);

                if (result <= alpha) {
                    window <<= 1;
                    alpha = max(-INF, score - window);
                } else if (result >= beta) {
                    window <<= 1;
                    beta = min(INF, score + window);
                } else {
                    score = result;
                    break;
                }
            }

            if (pvLength[0] > 0) bestMove = pvMoves[0][0];

            for (int f = 0; f < 64; f++) {
                for (int t = 0; t < 64; t++) {
                    history[f][t] >>= 1;
                }
            }
        }

        return bestMove;
    }

    private int alphaBeta(Board board, int depth, int alpha, int beta, int ply) {
        nodesSearched++;

        if (ply >= MAX_PLY - 1) {
            return ClassicalEvaluator.evaluate(board);
        }

        int initAlpha = alpha;

        // TT probe
        TranspositionTable.ProbeResult tt =
                transpositionTable.probe(board.zobristKey, depth, alpha, beta, ply);

        if (tt.status == TranspositionTable.EXACT_HIT) {
            pvMoves[ply][0] = tt.bestMove;
            pvLength[ply] = 1;
            return tt.score;
        } else if (tt.status == TranspositionTable.BETA_CUTOFF
                || tt.status == TranspositionTable.ALPHA_CUTOFF) {
            return tt.score;
        }

        int ttMove = (tt.status == TranspositionTable.SHALLOW_HIT) ? tt.bestMove : 0;

        boolean inCheck = board.isInCheck();


        if (inCheck && depth <= 2) depth++;



        if (depth <= 0) {
            return quiescence(board, alpha, beta, ply);
        }

        if (depth >= 3 && !inCheck && hasNonPawnMaterial(board, board.sideToMove)) {
            int R = (depth >= 6) ? 3 : 2;

            board.makeNullMove();
            int score = -alphaBeta(board, depth - 1 - R, -beta, -beta + 1, ply + 1);
            board.unmakeNullMove();

            if (score >= beta) {
                return beta;
            }
        }

        int[] moves = moveList[ply];
        int[] scores = moveScores[ply];
        int moveCount = MoveGenerator.generateAllMoves(board, moves);

        if (moveCount == 0) return inCheck ? (-MATE + ply) : DRAW;

// TT move to front
        if (ttMove != 0) {
            for (int i = 0; i < moveCount; i++) {
                if (moves[i] == ttMove) { swap(moves, 0, i); break; }
            }
        }

        int insertPos = (ttMove != 0) ? 1 : 0;

// killers next
        for (int k = 0; k < 2; k++) {
            int killer = killerMoves[ply][k];
            if (killer == 0 || !isQuietForKiller(killer)) continue;
            for (int j = insertPos; j < moveCount; j++) {
                if (moves[j] == killer) { swap(moves, insertPos, j); insertPos++; break; }
            }
        }

// score remaining moves (captures + history)
        for (int i = insertPos; i < moveCount; i++) {
            int m = moves[i];
            if (!isQuietForKiller(m)) {
                int a = board.getPieceOn(Move.from(m));
                int v = board.getPieceOn(Move.to(m));
                int mvvlva = (v == -1) ? 0 : (Constants.PIECE_VALUE[v] * 1000 - Constants.PIECE_VALUE[a]);
                scores[i] = 1_000_000 + mvvlva;
            } else {
                scores[i] = history[Move.from(m)][Move.to(m)];
            }
        }

// make sure already-placed moves have very high scores so selection doesn’t move them back
        for (int i = 0; i < insertPos; i++) scores[i] = Integer.MAX_VALUE - i;

        int bestScore = -INF;
        int bestMove = 0;

        for (int idx = 0; idx < moveCount; idx++) {

            // pick best move among idx..end
            int best = idx;
            for (int j = idx + 1; j < moveCount; j++) {
                if (scores[j] > scores[best]) best = j;
            }
            if (best != idx) { swap(moves, idx, best); swap(scores, idx, best); }

            int move = moves[idx];

            if (wouldCaptureKing(board, move)) continue;

            board.makeMove(move);
            if (movedIntoCheck(board)) { board.unmakeMove(); continue; }

            int score;
            int newDepth = depth - 1;

            boolean doLMR =
                    depth >= 3 &&
                            idx >= 4 &&
                            isQuietForKiller(move) &&
                            !inCheck &&
                            move != ttMove &&
                            move != killerMoves[ply][0] &&
                            move != killerMoves[ply][1];

            if (doLMR) {
                int reduction = (depth >= 6 && idx >= 8) ? 2 : 1;
                newDepth = Math.max(0, newDepth - reduction);
            }

            if (idx == 0) {
                score = -alphaBeta(board, depth - 1, -beta, -alpha, ply + 1);
            } else {
                score = -alphaBeta(board, newDepth, -alpha - 1, -alpha, ply + 1);
                if (score > alpha) {
                    score = -alphaBeta(board, depth - 1, -beta, -alpha, ply + 1);
                }
            }

            board.unmakeMove();

            if (score > bestScore) { bestScore = score; bestMove = move; }

            if (score > alpha) {
                alpha = score;

                pvMoves[ply][0] = move;
                int nextLen = pvLength[ply + 1];
                for (int i = 0; i < nextLen; i++) pvMoves[ply][i + 1] = pvMoves[ply + 1][i];
                pvLength[ply] = nextLen + 1;

                if (isQuietForKiller(move)) history[Move.from(move)][Move.to(move)] += depth * depth;
            }

            if (alpha >= beta) {
                if (isQuietForKiller(move) && killerMoves[ply][0] != move) {
                    killerMoves[ply][1] = killerMoves[ply][0];
                    killerMoves[ply][0] = move;
                }
                break;
            }
        }


        if (bestMove == 0) {
            return inCheck ? (-MATE + ply) : DRAW;
        }

        int flag;
        if (bestScore <= initAlpha) {
            flag = TranspositionTable.UPPER;
        } else if (bestScore >= beta) {
            flag = TranspositionTable.LOWER;
        } else {
            flag = TranspositionTable.EXACT;
        }

        transpositionTable.store(board.zobristKey, depth, flag, bestScore, bestMove, ply);
        return bestScore;
    }

    private int quiescence(Board board, int alpha, int beta, int ply) {
        if (ply >= MAX_PLY - 1) return ClassicalEvaluator.evaluate(board);

        boolean inCheck = board.isInCheck();

        int standPat = ClassicalEvaluator.evaluate(board);

        if (!inCheck) {
            if (standPat >= beta) return beta;
            if (standPat > alpha) alpha = standPat;
        }

        int[] moves = captureMoves[ply];
        int[] scores = captureScores[ply];

        int moveCount = inCheck
                ? MoveGenerator.generateAllMoves(board, moves)    // evasions if in check
                : MoveGenerator.generateCaptures(board, moves);   // captures only

        if (!inCheck) {
            scoreCaptures(board, moves, moveCount, scores);
        }

        for (int i = 0; i < moveCount; i++) {
            int move = moves[i];

            if (wouldCaptureKing(board, move)) continue;

            board.makeMove(move);

            if (movedIntoCheck(board)) {
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


    private boolean movedIntoCheck(Board b) {
        int mover = b.sideToMove ^ 1;
        long kingBB = (mover == Constants.WHITE) ? b.whiteKing : b.blackKing;
        if (kingBB == 0) return true;
        int kingSq = BitHelper.lsb(kingBB);
        return AttackGenerator.isSquareAttacked(b, kingSq, b.sideToMove);
    }

    private boolean wouldCaptureKing(Board b, int move) {
        int flag = Move.flags(move);
        if (flag != Constants.CAPTURE && flag != Constants.EN_PASSANT && flag < Constants.PROMO_KNIGHT_CAPTURE) {
            return false;
        }

        if (flag == Constants.EN_PASSANT) return false;

        int victim = b.getPieceOn(Move.to(move));
        return victim == Constants.W_KING || victim == Constants.B_KING;
    }

    public void scoreCaptures(Board board, int[] captures, int captureCount, int[] scores) {
        for (int i = 0; i < captureCount; i++) {
            int move = captures[i];

            int attacker = board.getPieceOn(Move.from(move));

            int victimSq;
            if (Move.flags(move) == Constants.EN_PASSANT) {
                victimSq = (board.sideToMove == Constants.WHITE) ? Move.to(move) - 8 : Move.to(move) + 8;
            } else {
                victimSq = Move.to(move);
            }

            int victim = board.getPieceOn(victimSq);

            int attackerValue = Constants.PIECE_VALUE[attacker];
            int victimValue = Constants.PIECE_VALUE[victim];

            scores[i] = victimValue * 1000 - attackerValue; // MVV-LVA
        }

        for (int i = 0; i < captureCount; i++) {
            int best = i;
            for (int j = i + 1; j < captureCount; j++) {
                if (scores[j] > scores[best]) best = j;
            }
            if (best != i) {
                swap(captures, i, best);
                swap(scores, i, best);
            }
        }
    }

    public void swap(int[] array, int idx1, int idx2) {
        int temp = array[idx1];
        array[idx1] = array[idx2];
        array[idx2] = temp;
    }

    private boolean isQuietForKiller(int move) {
        int f = Move.flags(move);
        return f == Constants.QUIET
                || f == Constants.DOUBLE_PAWN_PUSH
                || f == Constants.KING_CASTLE
                || f == Constants.QUEEN_CASTLE;
    }

    private boolean hasNonPawnMaterial(Board b, int side) {
        if (side == Constants.WHITE) {
            return (b.whiteKnights | b.whiteBishops | b.whiteRooks | b.whiteQueens) != 0;
        } else {
            return (b.blackKnights | b.blackBishops | b.blackRooks | b.blackQueens) != 0;
        }
    }
}
