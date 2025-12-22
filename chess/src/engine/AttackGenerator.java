package engine;

import util.BitHelper;
import util.Constants;

public final class AttackGenerator {


    public static boolean isSquareAttacked(Board board, int sq, int bySide) {
        if (sq < 0 || sq >= 64) {
            return false;
        }

        long squareBB = 1L << sq;

        if (bySide == Constants.WHITE) {
            long pawns = board.whitePawns;
            if (((pawns << 7) & Constants.notHFile & squareBB) != 0) return true;
            if (((pawns << 9) & Constants.notAFile & squareBB) != 0) return true;
        } else {
            long pawns = board.blackPawns;
            if (((pawns >>> 7) & Constants.notAFile & squareBB) != 0) return true;
            if (((pawns >>> 9) & Constants.notHFile & squareBB) != 0) return true;
        }

        long knights = (bySide == Constants.WHITE) ? board.whiteKnights : board.blackKnights;
        if ((Constants.KNIGHT_MASKS[sq] & knights) != 0) return true;

        long king = (bySide == Constants.WHITE) ? board.whiteKing : board.blackKing;
        if (king != 0 && (Constants.KING_MASKS[sq] & king) != 0) return true;

        long occ = board.allPieces;

        long bishopsQueens = (bySide == Constants.WHITE)
            ? (board.whiteBishops | board.whiteQueens)
            : (board.blackBishops | board.blackQueens);

        long diagAtk =
            BitHelper.hyperbolaQuintessence(occ, Constants.DIAG_MASKS[sq], sq) |
                BitHelper.hyperbolaQuintessence(occ, Constants.ANTIDIAG_MASKS[sq], sq);

        if ((diagAtk & bishopsQueens) != 0) return true;

        long rooksQueens = (bySide == Constants.WHITE)
            ? (board.whiteRooks | board.whiteQueens)
            : (board.blackRooks | board.blackQueens);

        long orthoAtk =
            BitHelper.hyperbolaQuintessence(occ, Constants.RANK_MASKS[sq], sq) |
                BitHelper.hyperbolaQuintessence(occ, Constants.FILE_MASKS[sq], sq);

        return (orthoAtk & rooksQueens) != 0;
    }
}
