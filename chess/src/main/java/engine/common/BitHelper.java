package engine.common;

public class BitHelper {

    public static int lsb(long bb) {
        return Long.numberOfTrailingZeros(bb);
    }

    public static int msb(long bb) {
        return 63 - Long.numberOfLeadingZeros(bb);
    }


    public static int popcount(long bb) {
        return Long.bitCount(bb);
    }

    public static int[] indices(long bb){
        int[] out = new int[Long.bitCount(bb)];
        int i = 0;

        while (bb != 0){
            int sq = Long.numberOfTrailingZeros(bb);
            out[i++] = sq;
                    bb &= bb - 1;
        }

        return out;
    }

    public static long reverse(long bb) {
        return Long.reverse(bb);
    }

    public static long kingMoves(int sq) {
        long bb = Constants.SQUARE_MASKS[sq];

        long moves = 0L;


        moves |= (bb << 1)  & Constants.notAFile;
        moves |= (bb >>> 1) & Constants.notHFile;

        moves |= (bb << 8);
        moves |= (bb >>> 8);

        moves |= (bb << 9)  & Constants.notAFile;
        moves |= (bb << 9)  & Constants.notAFile;
        moves |= (bb << 7)  & Constants.notHFile;
        moves |= (bb >>> 9) & Constants.notHFile;
        moves |= (bb >>> 7) & Constants.notAFile;

        return moves;
    }


    public static long knightMoves(int sq) {
        long bb = Constants.SQUARE_MASKS[sq];
        long moves = 0L;

        moves |= (bb << 17) & Constants.notAFile;

        moves |= (bb << 15) & Constants.notHFile;

        moves |= (bb << 10) & Constants.notABFiles;

        moves |= (bb << 6) & Constants.notGHFiles;

        moves |= (bb >>> 15) & Constants.notAFile;

        moves |= (bb >>> 17) & Constants.notHFile;

        moves |= (bb >>> 6) & Constants.notABFiles;

        moves |= (bb >>> 10) & Constants.notGHFiles;

        return moves;
    }


    public static long hyperbolaQuintessence(long occ, long rayMask, int sq) {
        long from = 1L << sq;

        long occOnRay = (occ & ~from) & rayMask;

        long forward = occOnRay - (from << 1);
        long reverse = Long.reverse(Long.reverse(occOnRay) - (Long.reverse(from) << 1));

        return (forward ^ reverse) & rayMask;
    }



    public static long whiteSinglePush(long pawns, long empty) {
        return (pawns << 8) & empty;
    }

    public static long whiteDoublePush(long pawns, long empty) {
        long single = whiteSinglePush(pawns, empty);
        return (single << 8) & empty & Constants.RANK_4;
    }

    public static long whiteAttacksRight(long pawns) {
        return (pawns << 9) & Constants.notAFile;
    }

    public static long whiteAttacksLeft(long pawns) {
        return (pawns << 7) & Constants.notHFile;
    }

    public static long whiteAttacks(long pawns) {
        return whiteAttacksRight(pawns) | whiteAttacksLeft(pawns);
    }

    public static long whiteEpAttacks(int epSquare, long pawns) {
        if (epSquare == -1) return 0;
        long epMask = 1L << epSquare;
        long epLeft  = (pawns << 7) & Constants.notHFile;
        long epRight = (pawns << 9) & Constants.notAFile;
        long attacks = epLeft | epRight;
        return attacks & epMask;
    }


    public static long blackSinglePush(long pawns, long empty) {
        return (pawns >>> 8) & empty;
    }

    public static long blackDoublePush(long pawns, long empty) {
        long single = blackSinglePush(pawns, empty);
        return (single >>> 8) & empty & Constants.RANK_5;
    }

    public static long blackAttacksRight(long pawns) {
        return (pawns >>> 7) & Constants.notAFile;
    }

    public static long blackAttacksLeft(long pawns) {
        return (pawns >>> 9) & Constants.notHFile;
    }

    public static long blackAttacks(long pawns) {
        return blackAttacksLeft(pawns) | blackAttacksRight(pawns);
    }

    public static long blackEpAttacks(int epSquare, long pawns) {
        if (epSquare == -1) return 0;
        long epMask = 1L << epSquare;
        long epLeft  = (pawns >>> 9) & Constants.notHFile;
        long epRight = (pawns >>> 7) & Constants.notAFile;
        long attacks = epLeft | epRight;
        return attacks & epMask;
    }

    public static long attacks(int side, long pawns) {
        return side == Constants.WHITE ? whiteAttacks(pawns) : blackAttacks(pawns);
    }

    public static long epAttacks(int side, int epSquare, long pawns) {
        return side == Constants.WHITE
                ? whiteEpAttacks(epSquare, pawns)
                : blackEpAttacks(epSquare, pawns);
    }




}
