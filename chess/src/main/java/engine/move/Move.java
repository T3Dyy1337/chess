package engine.move;

import engine.board.Board;
import engine.common.Constants;

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

    public static boolean isCapture(int move) {
        int f = flags(move);
        return f == Constants.CAPTURE
            || f == Constants.EN_PASSANT
            || (f >= Constants.PROMO_KNIGHT_CAPTURE && f <= Constants.PROMO_QUEEN_CAPTURE);
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

    private static int squareFromUCI(char file, char rank) {
        return (rank - '1') * 8 + (file - 'a');
    }

    public static int fromUCI(Board board, String uci) {
        if (uci.length() < 4) return 0;

        int from = (uci.charAt(1) - '1') * 8 + (uci.charAt(0) - 'a');
        int to   = (uci.charAt(3) - '1') * 8 + (uci.charAt(2) - 'a');

        int piece = board.getPieceOn(from);
        if (piece == -1) {
            throw new IllegalStateException("No piece on from-square: " + uci);
        }

        int flags = Constants.QUIET;
        int promo = 0;

        boolean isCapture = board.getPieceOn(to) != -1;


        if (uci.length() == 5) {
            char pc = uci.charAt(4);
            boolean white = (piece & 1) == 0;

            switch (pc) {
                case 'q' -> promo = white ? Constants.W_QUEEN  : Constants.B_QUEEN;
                case 'r' -> promo = white ? Constants.W_ROOK   : Constants.B_ROOK;
                case 'b' -> promo = white ? Constants.W_BISHOP : Constants.B_BISHOP;
                case 'n' -> promo = white ? Constants.W_KNIGHT : Constants.B_KNIGHT;
                default  -> throw new IllegalArgumentException("Bad promotion: " + uci);
            }

            if (isCapture) {
                flags = switch (promo) {
                    case Constants.W_QUEEN,  Constants.B_QUEEN  -> Constants.PROMO_QUEEN_CAPTURE;
                    case Constants.W_ROOK,   Constants.B_ROOK   -> Constants.PROMO_ROOK_CAPTURE;
                    case Constants.W_BISHOP, Constants.B_BISHOP -> Constants.PROMO_BISHOP_CAPTURE;
                    case Constants.W_KNIGHT, Constants.B_KNIGHT -> Constants.PROMO_KNIGHT_CAPTURE;
                    default -> throw new IllegalStateException();
                };
            } else {
                flags = switch (promo) {
                    case Constants.W_QUEEN,  Constants.B_QUEEN  -> Constants.PROMO_QUEEN;
                    case Constants.W_ROOK,   Constants.B_ROOK   -> Constants.PROMO_ROOK;
                    case Constants.W_BISHOP, Constants.B_BISHOP -> Constants.PROMO_BISHOP;
                    case Constants.W_KNIGHT, Constants.B_KNIGHT -> Constants.PROMO_KNIGHT;
                    default -> throw new IllegalStateException();
                };
            }

            return Move.encode(from, to, promo, flags);
        }


        if (piece == Constants.W_KING) {
            if (from == Constants.E1 && to == Constants.G1)
                return Move.encode(from, to, 0, Constants.KING_CASTLE);
            if (from == Constants.E1 && to == Constants.C1)
                return Move.encode(from, to, 0, Constants.QUEEN_CASTLE);
        }

        if (piece == Constants.B_KING) {
            if (from == Constants.E8 && to == Constants.G8)
                return Move.encode(from, to, 0, Constants.KING_CASTLE);
            if (from == Constants.E8 && to == Constants.C8)
                return Move.encode(from, to, 0, Constants.QUEEN_CASTLE);
        }

        if ((piece == Constants.W_PAWN || piece == Constants.B_PAWN)
            && to == board.enPassantSquare) {
            return Move.encode(from, to, 0, Constants.EN_PASSANT);
        }

        if (isCapture) {
            flags = Constants.CAPTURE;
        }

        if (piece == Constants.W_PAWN && from / 8 == 1 && to / 8 == 3)
            flags = Constants.DOUBLE_PAWN_PUSH;

        if (piece == Constants.B_PAWN && from / 8 == 6 && to / 8 == 4)
            flags = Constants.DOUBLE_PAWN_PUSH;

        return Move.encode(from, to, 0, flags);
    }


}
