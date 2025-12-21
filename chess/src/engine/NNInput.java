package engine;


import util.Constants;

public class NNInput {

    private static final int PLANES = 19;
    private static final int SIZE = 8;

    public static float[] encode(Board b) {

        float[] out = new float[PLANES * SIZE * SIZE];

        long[] pieceBB = {
                b.whitePawns, b.whiteKnights, b.whiteBishops,
                b.whiteRooks, b.whiteQueens, b.whiteKing,
                b.blackPawns, b.blackKnights, b.blackBishops,
                b.blackRooks, b.blackQueens, b.blackKing
        };

        for (int p = 0; p < 12; p++) {
            long bb = pieceBB[p];
            int base = p * 64;

            while (bb != 0) {
                int sq = Long.numberOfTrailingZeros(bb);
                bb &= bb - 1;

                int file = sq & 7;
                int rank = sq >>> 3;
                int r = 7 - rank;

                out[base + r * 8 + file] = 1f;
            }
        }

        fillPlane(out, 12, b.sideToMove == 0 ? 1f : 0f);

        fillPlane(out, 13, (b.castlingRights & Constants.WHITE_KINGSIDE) != 0 ? 1f : 0f);
        fillPlane(out, 14, (b.castlingRights & Constants.WHITE_QUEENSIDE) != 0 ? 1f : 0f);
        fillPlane(out, 15, (b.castlingRights & Constants.BLACK_KINGSIDE) != 0 ? 1f : 0f);
        fillPlane(out, 16, (b.castlingRights & Constants.BLACK_QUEENSIDE) != 0 ? 1f : 0f);

        if (b.enPassantSquare != -1) {
            int sq = b.enPassantSquare;
            int file = sq & 7;
            int rank = sq >>> 3;
            int r = 7 - rank;

            out[17 * 64 + r * 8 + file] = 1f;
        }

        float hm = Math.min(b.halfmoveClock, 100) / 100f;
        fillPlane(out, 18, hm);

        return out;
    }

    private static void fillPlane(float[] out, int plane, float v) {
        int base = plane * 64;
        for (int i = 0; i < 64; i++)
            out[base + i] = v;
    }
}

