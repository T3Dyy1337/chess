package engine;

import util.Constants;

public final class Move {

    // Layout (32-bit int):
    // bits  0..5   from square (0..63)
    // bits  6..11  to square   (0..63)
    // bits 12..15  promo piece (0..15)  (you store full piece id, e.g. W_QUEEN)
    // bits 16..19  flags       (0..15)

    private static final int FROM_MASK  = 0x3F;
    private static final int TO_MASK    = 0x3F;
    private static final int PROMO_MASK = 0xF;
    private static final int FLAG_MASK  = 0xF;

    public static int encode(int from, int to, int promo, int flags) {
        return (from & FROM_MASK)
                | ((to & TO_MASK) << 6)
                | ((promo & PROMO_MASK) << 12)
                | ((flags & FLAG_MASK) << 16);
    }

    public static int from(int move) {
        return move & FROM_MASK;
    }

    public static int to(int move) {
        return (move >>> 6) & TO_MASK;
    }

    public static int promo(int move) {
        return (move >>> 12) & PROMO_MASK;
    }

    public static int flags(int move) {
        return (move >>> 16) & FLAG_MASK;
    }

    private static String squareToString(int sq) {
        char file = (char) ('a' + (sq & 7));
        char rank = (char) ('1' + (sq >>> 3));
        return "" + file + rank;
    }

    public static String toUCI(int move) {
        int from  = from(move);
        int to    = to(move);
        int flag  = flags(move);
        int promo = promo(move);

        StringBuilder sb = new StringBuilder(5);
        sb.append(squareToString(from));
        sb.append(squareToString(to));

        // Promotions are 8..15 in your scheme, so bit 3 is set.
        if ((flag & 8) != 0) {
            sb.append(promoChar(promo));
        }

        return sb.toString();
    }

    private static char promoChar(int promo) {
        return switch (promo) {
            case Constants.W_QUEEN,  Constants.B_QUEEN  -> 'q';
            case Constants.W_ROOK,   Constants.B_ROOK   -> 'r';
            case Constants.W_BISHOP, Constants.B_BISHOP -> 'b';
            case Constants.W_KNIGHT, Constants.B_KNIGHT -> 'n';
            default -> '?';
        };
    }
}
