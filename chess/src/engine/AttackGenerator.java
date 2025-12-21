package engine;

import util.BitHelper;
import util.Constants;

public final class AttackGenerator {

    public static long attacksToSquare(Board board, int square, int side) {
        //TODO
        return 0;
    }

    public static boolean isSquareAttacked(Board board, int sq, int bySide) {
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

        if ((Constants.KNIGHT_MASKS[sq] &
                (bySide == Constants.WHITE ? board.whiteKnights : board.blackKnights)) != 0)
            return true;

        if ((Constants.KING_MASKS[sq] &
                (bySide == Constants.WHITE ? board.whiteKing : board.blackKing)) != 0)
            return true;

        long occ = board.allPieces;

        long bishopsQueens = (bySide == Constants.WHITE ?
                (board.whiteBishops | board.whiteQueens) :
                (board.blackBishops | board.blackQueens));

        long diagAtk =
                BitHelper.hyperbolaQuintessence(occ, Constants.DIAG_MASKS[sq], sq) |
                        BitHelper.hyperbolaQuintessence(occ, Constants.ANTIDIAG_MASKS[sq], sq);

        if ((diagAtk & bishopsQueens) != 0)
            return true;

        long rooksQueens = (bySide == Constants.WHITE ?
                (board.whiteRooks | board.whiteQueens) :
                (board.blackRooks | board.blackQueens));

        long orthoAtk =
                BitHelper.hyperbolaQuintessence(occ, Constants.RANK_MASKS[sq], sq) |
                        BitHelper.hyperbolaQuintessence(occ, Constants.FILE_MASKS[sq], sq);

        if ((orthoAtk & rooksQueens) != 0)
            return true;

        return false;
    }

    public static long allAttacks(Board board, int bySide) {
        long attacks = 0;

        attacks |= generatePawnAttacks(board, bySide);
        attacks |= knightAttacks(board, bySide);
        attacks |= bishopAttacks(board, bySide);
        attacks |= rookAttacks(board, bySide);
        attacks |= queenAttacks(board, bySide);
        attacks |= kingAttacks(board, bySide);

        return attacks;
    }


    static long generatePawnAttacks(Board board, int bySide){
        if(bySide == Constants.WHITE){
            return BitHelper.whiteAttacks(board.whitePawns);
        }else{
            return BitHelper.blackAttacks(board.blackPawns);
        }
    }

    static long knightAttacks(Board board, int bySide) {
        long bb = (bySide == Constants.WHITE) ? board.whiteKnights : board.blackKnights;
        long attacks = 0;
        while (bb != 0) {
            int sq = Long.numberOfTrailingZeros(bb);
            attacks |= Constants.KNIGHT_MASKS[sq];
            bb &= bb - 1;
        }
        return attacks;
    }


    static long kingAttacks(Board board, int bySide) {
        long king = (bySide == Constants.WHITE) ? board.whiteKing : board.blackKing;
        int kingSquare = BitHelper.lsb(king);
        return Constants.KING_MASKS[kingSquare];
    }

    static long bishopAttacks(Board board, int bySide) {
        long bb = (bySide == Constants.WHITE ? board.whiteBishops : board.blackBishops);
        long occ = board.allPieces;
        long attacks = 0;
        while (bb != 0) {
            int sq = Long.numberOfTrailingZeros(bb);
            attacks |= BitHelper.hyperbolaQuintessence(occ, Constants.DIAG_MASKS[sq], sq);
            attacks |= BitHelper.hyperbolaQuintessence(occ, Constants.ANTIDIAG_MASKS[sq], sq);
            bb &= bb - 1;
        }
        return attacks;
    }

    static long rookAttacks(Board board, int bySide) {
        long bb = (bySide == Constants.WHITE ? board.whiteRooks : board.blackRooks);
        long occ = board.allPieces;
        long attacks = 0;
        while (bb != 0) {
            int sq = Long.numberOfTrailingZeros(bb);
            attacks |= BitHelper.hyperbolaQuintessence(occ, Constants.RANK_MASKS[sq], sq);
            attacks |= BitHelper.hyperbolaQuintessence(occ, Constants.FILE_MASKS[sq], sq);
            bb &= bb - 1;
        }
        return attacks;
    }

    static long queenAttacks(Board board, int bySide) {
        long bb = bySide == Constants.WHITE ? board.whiteQueens : board.blackQueens;
        long occ = board.allPieces;
        long attacks = 0;
        while (bb != 0) {
            int sq = Long.numberOfTrailingZeros(bb);
            attacks |= BitHelper.hyperbolaQuintessence(occ, Constants.RANK_MASKS[sq], sq);
            attacks |= BitHelper.hyperbolaQuintessence(occ, Constants.FILE_MASKS[sq], sq);
            attacks |= BitHelper.hyperbolaQuintessence(occ, Constants.DIAG_MASKS[sq], sq);
            attacks |= BitHelper.hyperbolaQuintessence(occ, Constants.ANTIDIAG_MASKS[sq], sq);
            bb &= bb - 1;
        }
        return attacks;

    }





}
