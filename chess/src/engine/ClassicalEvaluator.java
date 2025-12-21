package engine;


import util.Constants;
import util.BitHelper;

public final class ClassicalEvaluator {

    // =========================
    // Material values (cp)
    // =========================
    private static final int PAWN   = 100;
    private static final int KNIGHT = 320;
    private static final int BISHOP = 330;
    private static final int ROOK   = 500;
    private static final int QUEEN  = 900;

    // =========================
    // Piece-Square Tables
    // White perspective
    // =========================

    private static final int[] PAWN_PST = {
            0,  0,  0,  0,  0,  0,  0,  0,
            5, 10, 10,-20,-20, 10, 10,  5,
            5, -5,-10,  0,  0,-10, -5,  5,
            0,  0,  0, 20, 20,  0,  0,  0,
            5,  5, 10, 25, 25, 10,  5,  5,
            10, 10, 20, 30, 30, 20, 10, 10,
            50, 50, 50, 50, 50, 50, 50, 50,
            0,  0,  0,  0,  0,  0,  0,  0
    };

    private static final int[] KNIGHT_PST = {
            -50,-40,-30,-30,-30,-30,-40,-50,
            -40,-20,  0,  5,  5,  0,-20,-40,
            -30,  5, 10, 15, 15, 10,  5,-30,
            -30,  0, 15, 20, 20, 15,  0,-30,
            -30,  5, 15, 20, 20, 15,  5,-30,
            -30,  0, 10, 15, 15, 10,  0,-30,
            -40,-20,  0,  0,  0,  0,-20,-40,
            -50,-40,-30,-30,-30,-30,-40,-50
    };

    private static final int[] BISHOP_PST = {
            -20,-10,-10,-10,-10,-10,-10,-20,
            -10,  5,  0,  0,  0,  0,  5,-10,
            -10, 10, 10, 10, 10, 10, 10,-10,
            -10,  0, 10, 10, 10, 10,  0,-10,
            -10,  5,  5, 10, 10,  5,  5,-10,
            -10,  0,  5, 10, 10,  5,  0,-10,
            -10,  0,  0,  0,  0,  0,  0,-10,
            -20,-10,-10,-10,-10,-10,-10,-20
    };

    private static final int[] ROOK_PST = {
            0,  0,  5, 10, 10,  5,  0,  0,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            5, 10, 10, 10, 10, 10, 10,  5,
            0,  0,  0,  0,  0,  0,  0,  0
    };

    private static final int[] QUEEN_PST = {
            -20,-10,-10, -5, -5,-10,-10,-20,
            -10,  0,  0,  0,  0,  0,  0,-10,
            -10,  0,  5,  5,  5,  5,  0,-10,
            -5,  0,  5,  5,  5,  5,  0, -5,
            0,  0,  5,  5,  5,  5,  0, -5,
            -10,  5,  5,  5,  5,  5,  0,-10,
            -10,  0,  5,  0,  0,  0,  0,-10,
            -20,-10,-10, -5, -5,-10,-10,-20
    };

    private static final int[] KING_PST = {
            -30,-40,-40,-50,-50,-40,-40,-30,
            -30,-40,-40,-50,-50,-40,-40,-30,
            -30,-40,-40,-50,-50,-40,-40,-30,
            -30,-40,-40,-50,-50,-40,-40,-30,
            -20,-30,-30,-40,-40,-30,-30,-20,
            -10,-20,-20,-20,-20,-20,-20,-10,
            20, 20,  0,  0,  0,  0, 20, 20,
            20, 30, 10,  0,  0, 10, 30, 20
    };

    // =========================
    // Entry point
    // =========================
    public static int evaluate(Board b) {
        int score = 0;

        score += material(b);
        score += pieceSquare(b);
        score += pawnStructure(b);
        score += mobility(b);

        return (b.sideToMove == Constants.WHITE) ? score : -score;
    }

    // =========================
    // Material
    // =========================
    private static int material(Board b) {
        return
                PAWN   * (Long.bitCount(b.whitePawns)   - Long.bitCount(b.blackPawns)) +
                        KNIGHT * (Long.bitCount(b.whiteKnights) - Long.bitCount(b.blackKnights)) +
                        BISHOP * (Long.bitCount(b.whiteBishops) - Long.bitCount(b.blackBishops)) +
                        ROOK   * (Long.bitCount(b.whiteRooks)   - Long.bitCount(b.blackRooks)) +
                        QUEEN  * (Long.bitCount(b.whiteQueens)  - Long.bitCount(b.blackQueens));
    }

    // =========================
    // Piece-square tables
    // =========================
    private static int pieceSquare(Board b) {
        int score = 0;

        score += pst(b.whitePawns,   PAWN_PST,   false);
        score += pst(b.whiteKnights, KNIGHT_PST, false);
        score += pst(b.whiteBishops, BISHOP_PST, false);
        score += pst(b.whiteRooks,   ROOK_PST,   false);
        score += pst(b.whiteQueens,  QUEEN_PST,  false);
        score += pst(b.whiteKing,    KING_PST,   false);

        score -= pst(b.blackPawns,   PAWN_PST,   true);
        score -= pst(b.blackKnights, KNIGHT_PST, true);
        score -= pst(b.blackBishops, BISHOP_PST, true);
        score -= pst(b.blackRooks,   ROOK_PST,   true);
        score -= pst(b.blackQueens,  QUEEN_PST,  true);
        score -= pst(b.blackKing,    KING_PST,   true);

        return score;
    }

    private static int pst(long bb, int[] table, boolean mirror) {
        int score = 0;
        while (bb != 0) {
            int sq = BitHelper.lsb(bb);
            bb &= bb - 1;
            score += table[mirror ? (sq ^ 56) : sq];
        }
        return score;
    }

    // =========================
    // Pawn structure (simple)
    // =========================
    private static int pawnStructure(Board b) {
        int score = 0;

        score -= doubledPawns(b.whitePawns) * 10;
        score += doubledPawns(b.blackPawns) * 10;

        long whiteCenter = b.whitePawns & ((1L<<27)|(1L<<28)|(1L<<35)|(1L<<36));
        long blackCenter = b.blackPawns & ((1L<<27)|(1L<<28)|(1L<<35)|(1L<<36));

        score += Long.bitCount(whiteCenter) * 15;
        score -= Long.bitCount(blackCenter) * 15;

        return score;
    }


    private static int doubledPawns(long pawns) {
        int count = 0;
        for (int file = 0; file < 8; file++) {
            long fileMask = Constants.FILE_MASKS[file];
            if (Long.bitCount(pawns & fileMask) > 1)
                count++;
        }
        return count;
    }

    // =========================
    // Mobility (cheap)
    // =========================
    private static int mobility(Board b) {
        int whiteMoves = MoveGenerator.generateAllMoves(b, new int[256]);
        b.sideToMove ^= 1;
        int blackMoves = MoveGenerator.generateAllMoves(b, new int[256]);
        b.sideToMove ^= 1;
        return (whiteMoves - blackMoves) * 2;
    }


}

