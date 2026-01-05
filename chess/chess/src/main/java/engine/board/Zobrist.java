package engine.board;

import java.util.Random;

public class Zobrist {
    public static final long[][] PIECE_KEYS = new long[12][64];
    public static final long[] CASTLING_KEYS = new long[16];
    public static final long[] ENPASSANT_KEYS = new long[64];
    public static final long SIDE_TO_MOVE_KEY;

    static {
        Random rng = new Random(248790174);

        for (int p = 0; p < 12; p++)
            for (int sq = 0; sq < 64; sq++)
                PIECE_KEYS[p][sq] = rng.nextLong();

        for (int i = 0; i < 16; i++)
            CASTLING_KEYS[i] = rng.nextLong();

        for (int sq = 0; sq < 64; sq++)
            ENPASSANT_KEYS[sq] = rng.nextLong();

        SIDE_TO_MOVE_KEY = rng.nextLong();
    }

    public static long hash(Board b) {
        long key = 0L;

        key ^= hashPieces(b.whitePawns,   0);
        key ^= hashPieces(b.whiteKnights, 1);
        key ^= hashPieces(b.whiteBishops, 2);
        key ^= hashPieces(b.whiteRooks,   3);
        key ^= hashPieces(b.whiteQueens,  4);
        key ^= hashPieces(b.whiteKing,    5);

        key ^= hashPieces(b.blackPawns,   6);
        key ^= hashPieces(b.blackKnights, 7);
        key ^= hashPieces(b.blackBishops, 8);
        key ^= hashPieces(b.blackRooks,   9);
        key ^= hashPieces(b.blackQueens,  10);
        key ^= hashPieces(b.blackKing,    11);

        if (b.sideToMove == 1)
            key ^= SIDE_TO_MOVE_KEY;

        key ^= CASTLING_KEYS[b.castlingRights];

        if (b.enPassantSquare != -1)
            key ^= ENPASSANT_KEYS[b.enPassantSquare];

        return key;
    }

    private static long hashPieces(long bb, int pieceIndex) {
        long key = 0L;

        while (bb != 0) {
            int sq = Long.numberOfTrailingZeros(bb);
            bb &= bb - 1;
            key ^= PIECE_KEYS[pieceIndex][sq];
        }

        return key;
    }


}
