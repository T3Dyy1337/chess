package engine;

import util.BitHelper;
import util.Constants;

public final class ClassicalEvaluator {

    private static final int PAWN   = 100;
    private static final int KNIGHT = 320;
    private static final int BISHOP = 330;
    private static final int ROOK   = 500;
    private static final int QUEEN  = 900;

    private static final int PH_N = 1, PH_B = 1, PH_R = 2, PH_Q = 4; // per piece
    private static final int PHASE_MAX = 24;


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

    private static final int[] KING_MG_PST = {
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -20,-30,-30,-40,-40,-30,-30,-20,
        -10,-20,-20,-20,-20,-20,-20,-10,
        20, 20,  0,  0,  0,  0, 20, 20,
        20, 30, 10,  0,  0, 10, 30, 20
    };

    private static final int[] KING_EG_PST = {
        -50,-30,-30,-30,-30,-30,-30,-50,
        -30,-10,  0,  0,  0,  0,-10,-30,
        -30,  0, 10, 15, 15, 10,  0,-30,
        -30,  5, 15, 20, 20, 15,  5,-30,
        -30,  5, 15, 20, 20, 15,  5,-30,
        -30,  0, 10, 15, 15, 10,  0,-30,
        -30,-10,  0,  0,  0,  0,-10,-30,
        -50,-30,-30,-30,-30,-30,-30,-50
    };

    private static final int[] PASSED_BONUS = { 0,  5, 10, 20, 35, 55, 85, 0 };


    public static int evaluate(Board b) {
        int score = 0; // white POV

        final int phase = gamePhase(b);
        final int mgW = phase;
        final int egW = PHASE_MAX - phase;

        score += material(b);

        score += pieceSquare(b, mgW, egW);

        score += pawnStructure(b);
        score += passedPawns(b, phase);

        score += bishopPair(b, phase);
        score += rookFiles(b);

        score += mobilityAll(b);
        score += knightOutposts(b);

        score += kingSafety(b, phase);

        score += hangingPieces(b, phase);

        score += 8;

        return (b.sideToMove == Constants.WHITE) ? score : -score;
    }


    private static int material(Board b) {
        return
            PAWN   * (Long.bitCount(b.whitePawns)   - Long.bitCount(b.blackPawns)) +
                KNIGHT * (Long.bitCount(b.whiteKnights) - Long.bitCount(b.blackKnights)) +
                BISHOP * (Long.bitCount(b.whiteBishops) - Long.bitCount(b.blackBishops)) +
                ROOK   * (Long.bitCount(b.whiteRooks)   - Long.bitCount(b.blackRooks)) +
                QUEEN  * (Long.bitCount(b.whiteQueens)  - Long.bitCount(b.blackQueens));
    }


    private static int gamePhase(Board b) {
        int phase =
            PH_N * (Long.bitCount(b.whiteKnights) + Long.bitCount(b.blackKnights)) +
                PH_B * (Long.bitCount(b.whiteBishops) + Long.bitCount(b.blackBishops)) +
                PH_R * (Long.bitCount(b.whiteRooks)   + Long.bitCount(b.blackRooks)) +
                PH_Q * (Long.bitCount(b.whiteQueens)  + Long.bitCount(b.blackQueens));

        if (phase > PHASE_MAX) phase = PHASE_MAX;
        if (phase < 0) phase = 0;
        return phase;
    }

    private static int pieceSquare(Board b, int mgW, int egW) {
        int score = 0;

        score += pst(b.whitePawns,   PAWN_PST,   false);
        score += pst(b.whiteKnights, KNIGHT_PST, false);
        score += pst(b.whiteBishops, BISHOP_PST, false);
        score += pst(b.whiteRooks,   ROOK_PST,   false);
        score += pst(b.whiteQueens,  QUEEN_PST,  false);

        score -= pst(b.blackPawns,   PAWN_PST,   true);
        score -= pst(b.blackKnights, KNIGHT_PST, true);
        score -= pst(b.blackBishops, BISHOP_PST, true);
        score -= pst(b.blackRooks,   ROOK_PST,   true);
        score -= pst(b.blackQueens,  QUEEN_PST,  true);

        score += kingPst(b.whiteKing, false, mgW, egW);
        score -= kingPst(b.blackKing, true,  mgW, egW);

        return score;
    }

    private static int pst(long bb, int[] table, boolean mirror) {
        int score = 0;
        while (bb != 0) {
            int sq = Long.numberOfTrailingZeros(bb);
            bb &= bb - 1;
            score += table[mirror ? (sq ^ 56) : sq];
        }
        return score;
    }

    private static int kingPst(long kingBB, boolean mirror, int mgW, int egW) {
        if (kingBB == 0) return 0;
        int sq = Long.numberOfTrailingZeros(kingBB);
        int idx = mirror ? (sq ^ 56) : sq;
        return (KING_MG_PST[idx] * mgW + KING_EG_PST[idx] * egW) / PHASE_MAX;
    }


    private static int pawnStructure(Board b) {
        int score = 0;

        score -= doubledPawns(b.whitePawns) * 10;
        score += doubledPawns(b.blackPawns) * 10;

        score -= isolatedPawns(b.whitePawns) * 12;
        score += isolatedPawns(b.blackPawns) * 12;

        score -= backwardPawnsWhite(b) * 10;
        score += backwardPawnsBlack(b) * 10;

        long centerMask = (1L<<27)|(1L<<28)|(1L<<35)|(1L<<36);
        score += Long.bitCount(b.whitePawns & centerMask) * 10;
        score -= Long.bitCount(b.blackPawns & centerMask) * 10;

        return score;
    }

    private static int doubledPawns(long pawns) {
        int count = 0;
        for (int file = 0; file < 8; file++) {
            long m = Constants.FILE_MASKS[file];
            if (Long.bitCount(pawns & m) > 1) count++;
        }
        return count;
    }

    private static int isolatedPawns(long pawns) {
        int count = 0;
        for (int file = 0; file < 8; file++) {
            long onFile = pawns & Constants.FILE_MASKS[file];
            if (onFile == 0) continue;

            long adj = 0;
            if (file > 0) adj |= pawns & Constants.FILE_MASKS[file - 1];
            if (file < 7) adj |= pawns & Constants.FILE_MASKS[file + 1];

            if (adj == 0) count += Long.bitCount(onFile);
        }
        return count;
    }

    private static int backwardPawnsWhite(Board b) {
        int count = 0;
        long wp = b.whitePawns;
        long bp = b.blackPawns;

        long blackPawnAttacks = blackPawnAttacks(bp);

        while (wp != 0) {
            int sq = Long.numberOfTrailingZeros(wp);
            wp &= wp - 1;

            int rank = sq >>> 3;
            if (rank >= 6) continue;

            int file = sq & 7;
            int front = sq + 8;
            if (front >= 64) continue;

            if (((1L << front) & blackPawnAttacks) == 0) continue;

            boolean hasSupport = false;
            if (file > 0) {
                long mask = Constants.FILE_MASKS[file - 1] & (~0L << (sq + 1));
                if ((b.whitePawns & mask) != 0) hasSupport = true;
            }
            if (!hasSupport && file < 7) {
                long mask = Constants.FILE_MASKS[file + 1] & (~0L << (sq + 1));
                if ((b.whitePawns & mask) != 0) hasSupport = true;
            }

            if (!hasSupport) count++;
        }

        return count;
    }

    private static int backwardPawnsBlack(Board b) {
        int count = 0;
        long bp = b.blackPawns;
        long wp = b.whitePawns;

        long whitePawnAttacks = whitePawnAttacks(wp);

        while (bp != 0) {
            int sq = Long.numberOfTrailingZeros(bp);
            bp &= bp - 1;

            int rank = sq >>> 3;
            if (rank <= 1) continue;

            int file = sq & 7;
            int front = sq - 8;
            if (front < 0) continue;

            if (((1L << front) & whitePawnAttacks) == 0) continue;

            boolean hasSupport = false;
            if (file > 0) {
                long mask = Constants.FILE_MASKS[file - 1] & ((1L << sq) - 1);
                if ((b.blackPawns & mask) != 0) hasSupport = true;
            }
            if (!hasSupport && file < 7) {
                long mask = Constants.FILE_MASKS[file + 1] & ((1L << sq) - 1);
                if ((b.blackPawns & mask) != 0) hasSupport = true;
            }

            if (!hasSupport) count++;
        }

        return count;
    }


    private static int passedPawns(Board b, int phase) {
        int score = 0;

        int egBoost = (PHASE_MAX - phase); // 0..24

        long wp = b.whitePawns;
        while (wp != 0) {
            int sq = Long.numberOfTrailingZeros(wp);
            wp &= wp - 1;

            if (isPassedPawnWhite(b, sq)) {
                int rank = sq >>> 3;
                int base = PASSED_BONUS[rank];
                score += base + (base * egBoost) / 24;
            }
        }

        long bp = b.blackPawns;
        while (bp != 0) {
            int sq = Long.numberOfTrailingZeros(bp);
            bp &= bp - 1;

            if (isPassedPawnBlack(b, sq)) {
                int rank = sq >>> 3;
                int mirrored = 7 - rank;
                int base = PASSED_BONUS[mirrored];
                score -= base + (base * egBoost) / 24;
            }
        }

        return score;
    }

    private static boolean isPassedPawnWhite(Board b, int sq) {
        int file = sq & 7;
        long mask = Constants.FILE_MASKS[file];
        if (file > 0) mask |= Constants.FILE_MASKS[file - 1];
        if (file < 7) mask |= Constants.FILE_MASKS[file + 1];

        long inFront = mask & (~0L << (sq + 8)); // all squares ahead
        return (b.blackPawns & inFront) == 0;
    }

    private static boolean isPassedPawnBlack(Board b, int sq) {
        int file = sq & 7;
        long mask = Constants.FILE_MASKS[file];
        if (file > 0) mask |= Constants.FILE_MASKS[file - 1];
        if (file < 7) mask |= Constants.FILE_MASKS[file + 1];

        long inFront = mask & ((1L << sq) - 1); // all squares below
        return (b.whitePawns & inFront) == 0;
    }


    private static int bishopPair(Board b, int phase) {
        int score = 0;
        int wb = Long.bitCount(b.whiteBishops);
        int bb = Long.bitCount(b.blackBishops);

        // bishop pair grows slightly toward endgame/open
        int bonus = 30 + (PHASE_MAX - phase) / 2; // 30..42

        if (wb >= 2) score += bonus;
        if (bb >= 2) score -= bonus;
        return score;
    }

    private static int rookFiles(Board b) {
        int score = 0;

        long allPawns = b.whitePawns | b.blackPawns;

        long wr = b.whiteRooks;
        while (wr != 0) {
            int sq = Long.numberOfTrailingZeros(wr);
            wr &= wr - 1;

            int file = sq & 7;
            long fileMask = Constants.FILE_MASKS[file];

            boolean open = (fileMask & allPawns) == 0;
            boolean semiOpen = !open && ((fileMask & b.whitePawns) == 0);

            if (open) score += 20;
            else if (semiOpen) score += 10;
        }

        long br = b.blackRooks;
        while (br != 0) {
            int sq = Long.numberOfTrailingZeros(br);
            br &= br - 1;

            int file = sq & 7;
            long fileMask = Constants.FILE_MASKS[file];

            boolean open = (fileMask & allPawns) == 0;
            boolean semiOpen = !open && ((fileMask & b.blackPawns) == 0);

            if (open) score -= 20;
            else if (semiOpen) score -= 10;
        }

        return score;
    }


    private static int mobilityAll(Board b) {
        int score = 0;

        final int N_W = 4;
        final int B_W = 3;
        final int R_W = 2;
        final int Q_W = 1;

        score += N_W * knightMobility(b, b.whiteKnights, b.whitePieces);
        score -= N_W * knightMobility(b, b.blackKnights, b.blackPieces);

        score += B_W * bishopMobility(b, b.whiteBishops, b.whitePieces);
        score -= B_W * bishopMobility(b, b.blackBishops, b.blackPieces);

        score += R_W * rookMobility(b, b.whiteRooks, b.whitePieces);
        score -= R_W * rookMobility(b, b.blackRooks, b.blackPieces);

        score += Q_W * queenMobility(b, b.whiteQueens, b.whitePieces);
        score -= Q_W * queenMobility(b, b.blackQueens, b.blackPieces);

        return score;
    }

    private static int knightMobility(Board b, long knights, long own) {
        int mob = 0;
        while (knights != 0) {
            int sq = Long.numberOfTrailingZeros(knights);
            knights &= knights - 1;
            mob += Long.bitCount(Constants.KNIGHT_MASKS[sq] & ~own);
        }
        return mob;
    }

    private static int bishopMobility(Board b, long bishops, long own) {
        int mob = 0;
        long occ = b.allPieces;
        while (bishops != 0) {
            int sq = Long.numberOfTrailingZeros(bishops);
            bishops &= bishops - 1;
            long attacks =
                BitHelper.hyperbolaQuintessence(occ, Constants.DIAG_MASKS[sq], sq) |
                    BitHelper.hyperbolaQuintessence(occ, Constants.ANTIDIAG_MASKS[sq], sq);
            mob += Long.bitCount(attacks & ~own);
        }
        return mob;
    }

    private static int rookMobility(Board b, long rooks, long own) {
        int mob = 0;
        long occ = b.allPieces;
        while (rooks != 0) {
            int sq = Long.numberOfTrailingZeros(rooks);
            rooks &= rooks - 1;
            long attacks =
                BitHelper.hyperbolaQuintessence(occ, Constants.RANK_MASKS[sq], sq) |
                    BitHelper.hyperbolaQuintessence(occ, Constants.FILE_MASKS[sq], sq);
            mob += Long.bitCount(attacks & ~own);
        }
        return mob;
    }

    private static int queenMobility(Board b, long queens, long own) {
        int mob = 0;
        long occ = b.allPieces;
        while (queens != 0) {
            int sq = Long.numberOfTrailingZeros(queens);
            queens &= queens - 1;

            long diag =
                BitHelper.hyperbolaQuintessence(occ, Constants.DIAG_MASKS[sq], sq) |
                    BitHelper.hyperbolaQuintessence(occ, Constants.ANTIDIAG_MASKS[sq], sq);

            long ortho =
                BitHelper.hyperbolaQuintessence(occ, Constants.RANK_MASKS[sq], sq) |
                    BitHelper.hyperbolaQuintessence(occ, Constants.FILE_MASKS[sq], sq);

            mob += Long.bitCount((diag | ortho) & ~own);
        }
        return mob;
    }


    private static int knightOutposts(Board b) {
        int score = 0;

        long wPawnAtt = whitePawnAttacks(b.whitePawns);
        long bPawnAtt = blackPawnAttacks(b.blackPawns);

        long wOutRanks = Constants.RANK_MASKS[4] | Constants.RANK_MASKS[5];
        long wKn = b.whiteKnights & wOutRanks;
        while (wKn != 0) {
            int sq = Long.numberOfTrailingZeros(wKn);
            wKn &= wKn - 1;

            long sqBB = 1L << sq;
            if ((sqBB & bPawnAtt) != 0) continue;
            int bonus = 20;
            if ((sqBB & wPawnAtt) != 0) bonus += 10;
            score += bonus;
        }

        long bOutRanks = Constants.RANK_MASKS[2] | Constants.RANK_MASKS[3];
        long bKn = b.blackKnights & bOutRanks;
        while (bKn != 0) {
            int sq = Long.numberOfTrailingZeros(bKn);
            bKn &= bKn - 1;

            long sqBB = 1L << sq;
            if ((sqBB & wPawnAtt) != 0) continue;
            int bonus = 20;
            if ((sqBB & bPawnAtt) != 0) bonus += 10;
            score -= bonus;
        }

        return score;
    }


    private static int kingSafety(Board b, int phase) {
        int score = 0;

        int mgW = phase; // 0..24

        score += pawnShieldScore(b, Constants.WHITE) * mgW / PHASE_MAX;
        score -= pawnShieldScore(b, Constants.BLACK) * mgW / PHASE_MAX;

        int wKingSq = b.whiteKingSq;
        int bKingSq = b.blackKingSq;

        if (wKingSq >= 0 && wKingSq < 64) {
            long ring = Constants.KING_MASKS[wKingSq];
            long blackAtt = attacksAll(b, Constants.BLACK);
            int hits = Long.bitCount(blackAtt & ring);
            score -= (hits * 18) * mgW / PHASE_MAX;
        }
        if (bKingSq >= 0 && bKingSq < 64) {
            long ring = Constants.KING_MASKS[bKingSq];
            long whiteAtt = attacksAll(b, Constants.WHITE);
            int hits = Long.bitCount(whiteAtt & ring);
            score += (hits * 18) * mgW / PHASE_MAX;
        }

        return score;
    }

    private static int pawnShieldScore(Board b, int side) {
        int kingSq = (side == Constants.WHITE) ? b.whiteKingSq : b.blackKingSq;
        if (kingSq < 0 || kingSq >= 64) return 0;

        long pawns = (side == Constants.WHITE) ? b.whitePawns : b.blackPawns;

        int rank = kingSq >>> 3;
        int file = kingSq & 7;

        int holes = 0;
        int frontRank = (side == Constants.WHITE) ? (rank + 1) : (rank - 1);

        if (frontRank < 0 || frontRank > 7) return 0;

        int base = frontRank << 3;
        for (int df = -1; df <= 1; df++) {
            int f = file + df;
            if (f < 0 || f > 7) continue;
            int sq = base + f;
            if (((pawns >>> sq) & 1L) == 0) holes++;
        }

        return 30 - holes * 20;
    }


    private static int hangingPieces(Board b, int phase) {
        int score = 0;
        int mgW = phase;

        long whiteAtt = attacksAll(b, Constants.WHITE);
        long blackAtt = attacksAll(b, Constants.BLACK);

        long whiteDef = whiteAtt;
        long blackDef = blackAtt;

        long wPiecesNoKing = b.whitePieces & ~b.whiteKing;
        long bPiecesNoKing = b.blackPieces & ~b.blackKing;

        long wHanging = wPiecesNoKing & blackAtt & ~whiteDef;
        long bHanging = bPiecesNoKing & whiteAtt & ~blackDef;

        int wCount = Long.bitCount(wHanging);
        int bCount = Long.bitCount(bHanging);

        score -= (wCount * 22) * mgW / PHASE_MAX;
        score += (bCount * 22) * mgW / PHASE_MAX;

        return score;
    }


    private static long attacksAll(Board b, int side) {
        long occ = b.allPieces;
        long attacks = 0L;

        if (side == Constants.WHITE) {
            attacks |= whitePawnAttacks(b.whitePawns);
            attacks |= knightAttackSet(b.whiteKnights);
            attacks |= kingAttackSet(b.whiteKing);
            attacks |= bishopAttackSet(b.whiteBishops, occ);
            attacks |= rookAttackSet(b.whiteRooks, occ);
            attacks |= queenAttackSet(b.whiteQueens, occ);
        } else {
            attacks |= blackPawnAttacks(b.blackPawns);
            attacks |= knightAttackSet(b.blackKnights);
            attacks |= kingAttackSet(b.blackKing);
            attacks |= bishopAttackSet(b.blackBishops, occ);
            attacks |= rookAttackSet(b.blackRooks, occ);
            attacks |= queenAttackSet(b.blackQueens, occ);
        }

        return attacks;
    }

    private static long whitePawnAttacks(long pawns) {
        long a = 0L;
        a |= (pawns << 7) & Constants.notHFile;
        a |= (pawns << 9) & Constants.notAFile;
        return a;
    }

    private static long blackPawnAttacks(long pawns) {
        long a = 0L;
        a |= (pawns >>> 7) & Constants.notAFile;
        a |= (pawns >>> 9) & Constants.notHFile;
        return a;
    }

    private static long knightAttackSet(long knights) {
        long att = 0L;
        while (knights != 0) {
            int sq = Long.numberOfTrailingZeros(knights);
            knights &= knights - 1;
            att |= Constants.KNIGHT_MASKS[sq];
        }
        return att;
    }

    private static long kingAttackSet(long king) {
        if (king == 0) return 0L;
        int sq = Long.numberOfTrailingZeros(king);
        return Constants.KING_MASKS[sq];
    }

    private static long bishopAttackSet(long bishops, long occ) {
        long att = 0L;
        while (bishops != 0) {
            int sq = Long.numberOfTrailingZeros(bishops);
            bishops &= bishops - 1;
            att |= BitHelper.hyperbolaQuintessence(occ, Constants.DIAG_MASKS[sq], sq);
            att |= BitHelper.hyperbolaQuintessence(occ, Constants.ANTIDIAG_MASKS[sq], sq);
        }
        return att;
    }

    private static long rookAttackSet(long rooks, long occ) {
        long att = 0L;
        while (rooks != 0) {
            int sq = Long.numberOfTrailingZeros(rooks);
            rooks &= rooks - 1;
            att |= BitHelper.hyperbolaQuintessence(occ, Constants.RANK_MASKS[sq], sq);
            att |= BitHelper.hyperbolaQuintessence(occ, Constants.FILE_MASKS[sq], sq);
        }
        return att;
    }

    private static long queenAttackSet(long queens, long occ) {
        long att = 0L;
        while (queens != 0) {
            int sq = Long.numberOfTrailingZeros(queens);
            queens &= queens - 1;
            att |= BitHelper.hyperbolaQuintessence(occ, Constants.DIAG_MASKS[sq], sq);
            att |= BitHelper.hyperbolaQuintessence(occ, Constants.ANTIDIAG_MASKS[sq], sq);
            att |= BitHelper.hyperbolaQuintessence(occ, Constants.RANK_MASKS[sq], sq);
            att |= BitHelper.hyperbolaQuintessence(occ, Constants.FILE_MASKS[sq], sq);
        }
        return att;
    }
}
