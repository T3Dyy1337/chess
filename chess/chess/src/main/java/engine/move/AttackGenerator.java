package engine.move;

import engine.board.Board;
import engine.common.BitHelper;
import engine.common.Constants;

public final class AttackGenerator {

    public static boolean isSquareAttacked(Board board, int sq, int bySide) {
        if (sq < 0 || sq >= 64) return false;

        final boolean white = bySide == Constants.WHITE;
        final long squareBB = 1L << sq;
        final long occ = board.allPieces;

        long pawns = white ? board.whitePawns : board.blackPawns;
        if (white) {
            if (((pawns << 7) & Constants.notHFile & squareBB) != 0) return true;
            if (((pawns << 9) & Constants.notAFile & squareBB) != 0) return true;
        } else {
            if (((pawns >>> 7) & Constants.notAFile & squareBB) != 0) return true;
            if (((pawns >>> 9) & Constants.notHFile & squareBB) != 0) return true;
        }

        long knights = white ? board.whiteKnights : board.blackKnights;
        if ((Constants.KNIGHT_MASKS[sq] & knights) != 0) return true;

        long king = white ? board.whiteKing : board.blackKing;
        if ((Constants.KING_MASKS[sq] & king) != 0) return true;

        long queens = white ? board.whiteQueens : board.blackQueens;
        long bishopsQueens = (white ? board.whiteBishops : board.blackBishops) | queens;

        long diagAtk =
            BitHelper.hyperbolaQuintessence(occ, Constants.DIAG_MASKS[sq], sq) |
                BitHelper.hyperbolaQuintessence(occ, Constants.ANTIDIAG_MASKS[sq], sq);

        if ((diagAtk & bishopsQueens) != 0) return true;

        long rooksQueens = (white ? board.whiteRooks : board.blackRooks) | queens;

        long orthoAtk =
            BitHelper.hyperbolaQuintessence(occ, Constants.RANK_MASKS[sq], sq) |
                BitHelper.hyperbolaQuintessence(occ, Constants.FILE_MASKS[sq], sq);

        return (orthoAtk & rooksQueens) != 0;
    }
}
