package engine.eval;

import engine.board.Board;
import engine.common.BitHelper;
import engine.common.Constants;

/**
 * Evaluation is "white minus black" (positive = good for white).
 * Final score is returned from side-to-move perspective (tempo baked in too).
 */
public final class ClassicalEvaluator {

    private static final class AttackCache {
        long whiteAtt;
        long blackAtt;
    }


    // Weights
    private static final int TEMPO_BONUS = 12;

    // Pawn structure
    private static final int DOUBLED_PAWN_PENALTY = 20;
    private static final int ISOLATED_PAWN_PENALTY = 18;
    private static final int BACKWARD_PAWN_PENALTY = 14;
    private static final int PASSED_PAWN_BONUS_BASE = 14;
    private static final int PASSED_PAWN_BONUS_PER_RANK = 10;

    // Piece evaluation / patterns
    private static final int BISHOP_PAIR_BONUS = 25;
    private static final int ROOK_OPEN_FILE_BONUS = 18;
    private static final int ROOK_SEMI_OPEN_FILE_BONUS = 10;
    private static final int KNIGHT_OUTPOST_BONUS = 14;

    // Mobility
    private static final int MOBILITY_N = 4;
    private static final int MOBILITY_B = 4;
    private static final int MOBILITY_R = 2;
    private static final int MOBILITY_Q = 1;

    // Center & space
    private static final long CENTER_4 = sqBB(Constants.D4) | sqBB(Constants.E4) | sqBB(Constants.D5) | sqBB(Constants.E5);
    private static final long EXT_CENTER = CENTER_4
        | sqBB(Constants.C3) | sqBB(Constants.D3) | sqBB(Constants.E3) | sqBB(Constants.F3)
        | sqBB(Constants.C4) | sqBB(Constants.F4)
        | sqBB(Constants.C5) | sqBB(Constants.F5)
        | sqBB(Constants.C6) | sqBB(Constants.D6) | sqBB(Constants.E6) | sqBB(Constants.F6);

    private static final int CENTER_CONTROL_BONUS = 6;
    private static final int EXT_CENTER_CONTROL_BONUS = 2;
    private static final int SPACE_BONUS_PER_SQ = 1;

    // Connectivity / trapped / king safety
    private static final int CONNECTIVITY_BONUS_PER_DEFENDED_PIECE = 2;
    private static final int TRAPPED_PIECE_PENALTY = 25;

    private static final int KING_SAFETY_PAWN_SHIELD = 10;

    private static final int[] KING_ATTACK_SCORES =
        {0, 4, 10, 18, 28, 40, 60, 80, 100};

    // Tapered eval (midgame/endgame blend)
    private static final int PHASE_N = 1, PHASE_B = 1, PHASE_R = 2, PHASE_Q = 4;
    private static final int PHASE_MAX = 2 * (2*PHASE_N + 2*PHASE_B + 2*PHASE_R + PHASE_Q);


    public static int evaluate(Board b) {

        //calculate attacks and cache
        AttackCache cache = new AttackCache();
        cache.whiteAtt = allAttacks(b, Constants.WHITE);
        cache.blackAtt = allAttacks(b, Constants.BLACK);

        // --- Material ---
        int material = material(b);

        // --- Piece-square tables (tapered mg/eg) ---
        int phase = gamePhase(b);
        int pstMg = pst(b, true);
        int pstEg = pst(b, false);
        int pst = taper(pstMg, pstEg, phase);

        // --- Pawn structure ---
        int pawns = pawnStructure(b);

        // --- Evaluation of pieces (bishop pair, rooks on files, outposts, etc.) ---
        int pieces = pieceFeatures(b);

        // --- Evaluation patterns (simple patterns you can expand) ---
        int patterns = evalPatterns(b);

        // --- Mobility ---
        int mobility = mobility(b);

        // --- Center control ---
        int center = centerControl(cache);

        // --- Connectivity (pieces defended by own pieces) ---
        int conn = connectivity(b, cache);

        // --- Trapped pieces (very rough heuristic) ---
        int trapped = trappedPieces(b, cache);

        // --- King safety ---
        int kingSafetyG = kingSafety(b, cache);
        int kingSafety = taper(kingSafetyG, kingSafetyG, phase);

        // --- Space ---
        int space = space(cache);

        // --- Tempo ---
        int tempo = (b.sideToMove == Constants.WHITE) ? TEMPO_BONUS : -TEMPO_BONUS;

        int scoreWhiteMinusBlack =
            material + pst + pawns + pieces + patterns + mobility + center + conn + trapped + kingSafety + space + tempo;

        // Return from side-to-move perspective (common engine convention)
        return (b.sideToMove == Constants.WHITE) ? scoreWhiteMinusBlack : -scoreWhiteMinusBlack;
    }


    private static int material(Board b) {
        int w =
            Constants.PAWN_VALUE   * BitHelper.popcount(b.whitePawns) +
                Constants.KNIGHT_VALUE * BitHelper.popcount(b.whiteKnights) +
                Constants.BISHOP_VALUE * BitHelper.popcount(b.whiteBishops) +
                Constants.ROOK_VALUE   * BitHelper.popcount(b.whiteRooks) +
                Constants.QUEEN_VALUE  * BitHelper.popcount(b.whiteQueens);

        int bl =
            Constants.PAWN_VALUE   * BitHelper.popcount(b.blackPawns) +
                Constants.KNIGHT_VALUE * BitHelper.popcount(b.blackKnights) +
                Constants.BISHOP_VALUE * BitHelper.popcount(b.blackBishops) +
                Constants.ROOK_VALUE   * BitHelper.popcount(b.blackRooks) +
                Constants.QUEEN_VALUE  * BitHelper.popcount(b.blackQueens);

        return w - bl;
    }



    private static int pst(Board b, boolean midgame) {
        int score = 0;

        score += pstPieces(b.whitePawns,   midgame ? PST_P_MG : PST_P_EG, true);
        score += pstPieces(b.whiteKnights, midgame ? PST_N_MG : PST_N_EG, true);
        score += pstPieces(b.whiteBishops, midgame ? PST_B_MG : PST_B_EG, true);
        score += pstPieces(b.whiteRooks,   midgame ? PST_R_MG : PST_R_EG, true);
        score += pstPieces(b.whiteQueens,  midgame ? PST_Q_MG : PST_Q_EG, true);
        score += pstPieces(b.whiteKing,    midgame ? PST_K_MG : PST_K_EG, true);

        score -= pstPieces(b.blackPawns,   midgame ? PST_P_MG : PST_P_EG, false);
        score -= pstPieces(b.blackKnights, midgame ? PST_N_MG : PST_N_EG, false);
        score -= pstPieces(b.blackBishops, midgame ? PST_B_MG : PST_B_EG, false);
        score -= pstPieces(b.blackRooks,   midgame ? PST_R_MG : PST_R_EG, false);
        score -= pstPieces(b.blackQueens,  midgame ? PST_Q_MG : PST_Q_EG, false);
        score -= pstPieces(b.blackKing,    midgame ? PST_K_MG : PST_K_EG, false);

        return score;
    }

    private static int pstPieces(long bb, int[] table, boolean white) {
        int s = 0;
        while (bb != 0) {
            int sq = Long.numberOfTrailingZeros(bb);
            bb &= bb - 1;
            int idx = white ? sq : mirrorVertical(sq);
            s += table[idx];
        }
        return s;
    }


    private static int pawnStructure(Board b) {
        int score = 0;

        score += evalPawnStructureSide(b.whitePawns, b.blackPawns, true);
        score -= evalPawnStructureSide(b.blackPawns, b.whitePawns, false);

        return score;
    }

    private static int evalPawnStructureSide(long myPawns, long oppPawns, boolean white) {
        int s = 0;

        long p = myPawns;

        while (p != 0) {
            int sq = Long.numberOfTrailingZeros(p);
            p &= p - 1;

            int file = sq & 7;
            long fileMask = Constants.FILE_MASKS[file];

            // doubled
            int count = BitHelper.popcount(myPawns & fileMask);
            if (count > 1) s -= DOUBLED_PAWN_PENALTY * (count - 1);

            // isolated
            long adj = 0;
            if (file > 0) adj |= Constants.FILE_MASKS[file - 1];
            if (file < 7) adj |= Constants.FILE_MASKS[file + 1];
            if ((myPawns & adj) == 0) s -= ISOLATED_PAWN_PENALTY;

            long maskFiles = Constants.FILE_MASKS[file];
            if (file > 0) maskFiles |= Constants.FILE_MASKS[file - 1];
            if (file < 7) maskFiles |= Constants.FILE_MASKS[file + 1];

            long inFront = white ? northFill(sqBB(sq)) : southFill(sqBB(sq));
            long oppBlockers = oppPawns & maskFiles & inFront;

            long ahead1 = white ? (sqBB(sq) << 8) : (sqBB(sq) >>> 8);
            boolean blocked = (ahead1 & (myPawns | oppPawns)) != 0;

            if (oppBlockers == 0) {
                int rank = sq >>> 3; // 0..7
                int adv = white ? rank : (7 - rank);
                int bonus = PASSED_PAWN_BONUS_BASE + PASSED_PAWN_BONUS_PER_RANK;

                if (blocked) bonus /= 2;

                s += bonus * adv;

            } else {
                if (blocked) {
                    adj = 0;
                    if (file > 0) adj |= Constants.FILE_MASKS[file - 1];
                    if (file < 7) adj |= Constants.FILE_MASKS[file + 1];

                    long supporters = myPawns & adj;
                    long supportZone = (inFront | sqBB(sq));
                    if ((supporters & supportZone) == 0) s -= BACKWARD_PAWN_PENALTY;
                }
            }
        }

        return s;
    }


    private static int pieceFeatures(Board b) {
        int s = 0;

        if (BitHelper.popcount(b.whiteBishops) >= 2) s += BISHOP_PAIR_BONUS;
        if (BitHelper.popcount(b.blackBishops) >= 2) s -= BISHOP_PAIR_BONUS;

        s += rooksOnFiles(b.whiteRooks, b.whitePawns, b.blackPawns);
        s -= rooksOnFiles(b.blackRooks, b.blackPawns, b.whitePawns);

        s += knightOutposts(b.whiteKnights, b.whitePawns, b.blackPawns, true);
        s -= knightOutposts(b.blackKnights, b.blackPawns, b.whitePawns, false);

        return s;
    }

    private static int rooksOnFiles(long rooks, long myPawns, long oppPawns) {
        int s = 0;
        long bb = rooks;
        while (bb != 0) {
            int sq = Long.numberOfTrailingZeros(bb);
            bb &= bb - 1;
            int f = sq & 7;

            long file = Constants.FILE_MASKS[f];
            boolean myPawnOnFile = (myPawns & file) != 0;
            boolean oppPawnOnFile = (oppPawns & file) != 0;

            if (!myPawnOnFile && !oppPawnOnFile) s += ROOK_OPEN_FILE_BONUS;
            else if (!myPawnOnFile) s += ROOK_SEMI_OPEN_FILE_BONUS;
        }
        return s;
    }

    private static int knightOutposts(long knights, long myPawns, long oppPawns, boolean white) {
        int s = 0;
        long bb = knights;
        while (bb != 0) {
            int sq = Long.numberOfTrailingZeros(bb);
            bb &= bb - 1;

            int rank = sq >>> 3;
            if (white) {
                if (rank < 3) continue;
            } else {
                if (rank > 4) continue;
            }

            long sqMask = sqBB(sq);

            long pawnProtect = white ? (BitHelper.blackAttacks(sqMask))
                : (BitHelper.whiteAttacks(sqMask));
            boolean prot = (myPawns & pawnProtect) != 0;

            long enemyAttacks = white ? BitHelper.attacks(Constants.BLACK, oppPawns)
                : BitHelper.attacks(Constants.WHITE, oppPawns);
            boolean chased = (enemyAttacks & sqMask) != 0;

            if (prot && !chased) s += KNIGHT_OUTPOST_BONUS;
        }
        return s;
    }


    private static int evalPatterns(Board b) {
        int s = 0;

        s += connectedRooksBonus(b, true);
        s -= connectedRooksBonus(b, false);

        return s;
    }

    private static int connectedRooksBonus(Board b, boolean white) {
        long rooks = white ? b.whiteRooks : b.blackRooks;
        if (BitHelper.popcount(rooks) < 2) return 0;

        int[] sqs = BitHelper.indices(rooks);
        int a = sqs[0], c = sqs[1];
        if ((a >>> 3) != (c >>> 3)) return 0;

        long between = squaresBetweenOnRank(a, c);
        long occ = b.allPieces & ~rooks;
        if ((between & occ) == 0) return 10;
        return 0;
    }


    private static int mobility(Board b) {
        int s = 0;

        long wOcc = b.whitePawns | b.whiteKnights | b.whiteBishops | b.whiteRooks | b.whiteQueens | b.whiteKing;
        long bOcc = b.blackPawns | b.blackKnights | b.blackBishops | b.blackRooks | b.blackQueens | b.blackKing;
        long occ = b.allPieces;

        s += MOBILITY_N * mobilityKnights(b.whiteKnights, wOcc);
        s += MOBILITY_B * mobilityBishops(b.whiteBishops, occ, wOcc);
        s += MOBILITY_R * mobilityRooks(b.whiteRooks, occ, wOcc);
        s += MOBILITY_Q * mobilityQueens(b.whiteQueens, occ, wOcc);

        s -= MOBILITY_N * mobilityKnights(b.blackKnights, bOcc);
        s -= MOBILITY_B * mobilityBishops(b.blackBishops, occ, bOcc);
        s -= MOBILITY_R * mobilityRooks(b.blackRooks, occ, bOcc);
        s -= MOBILITY_Q * mobilityQueens(b.blackQueens, occ, bOcc);

        return s;
    }

    private static int mobilityKnights(long knights, long myOcc) {
        int m = 0;
        long bb = knights;
        while (bb != 0) {
            int sq = Long.numberOfTrailingZeros(bb);
            bb &= bb - 1;
            long moves = Constants.KNIGHT_MASKS[sq] & ~myOcc;
            m += BitHelper.popcount(moves);
        }
        return m;
    }

    private static int mobilityBishops(long bishops, long occ, long myOcc) {
        int m = 0;
        long bb = bishops;
        while (bb != 0) {
            int sq = Long.numberOfTrailingZeros(bb);
            bb &= bb - 1;

            long diag =
                BitHelper.hyperbolaQuintessence(occ, Constants.DIAG_MASKS[sq], sq) |
                    BitHelper.hyperbolaQuintessence(occ, Constants.ANTIDIAG_MASKS[sq], sq);

            m += BitHelper.popcount(diag & ~myOcc);
        }
        return m;
    }

    private static int mobilityRooks(long rooks, long occ, long myOcc) {
        int m = 0;
        long bb = rooks;
        while (bb != 0) {
            int sq = Long.numberOfTrailingZeros(bb);
            bb &= bb - 1;

            long ortho =
                BitHelper.hyperbolaQuintessence(occ, Constants.RANK_MASKS[sq], sq) |
                    BitHelper.hyperbolaQuintessence(occ, Constants.FILE_MASKS[sq], sq);

            m += BitHelper.popcount(ortho & ~myOcc);
        }
        return m;
    }

    private static int mobilityQueens(long queens, long occ, long myOcc) {
        int m = 0;
        long bb = queens;
        while (bb != 0) {
            int sq = Long.numberOfTrailingZeros(bb);
            bb &= bb - 1;

            long diag =
                BitHelper.hyperbolaQuintessence(occ, Constants.DIAG_MASKS[sq], sq) |
                    BitHelper.hyperbolaQuintessence(occ, Constants.ANTIDIAG_MASKS[sq], sq);

            long ortho =
                BitHelper.hyperbolaQuintessence(occ, Constants.RANK_MASKS[sq], sq) |
                    BitHelper.hyperbolaQuintessence(occ, Constants.FILE_MASKS[sq], sq);

            m += BitHelper.popcount((diag | ortho) & ~myOcc);
        }
        return m;
    }


    private static int centerControl(AttackCache cache) {
        long wAtt = cache.whiteAtt;
        long blAtt = cache.blackAtt;

        int s = 0;
        s += CENTER_CONTROL_BONUS * BitHelper.popcount(wAtt & CENTER_4);
        s -= CENTER_CONTROL_BONUS * BitHelper.popcount(blAtt & CENTER_4);

        s += EXT_CENTER_CONTROL_BONUS * BitHelper.popcount(wAtt & EXT_CENTER);
        s -= EXT_CENTER_CONTROL_BONUS * BitHelper.popcount(blAtt & EXT_CENTER);

        return s;
    }


    private static int connectivity(Board b, AttackCache cache) {
        long wAtt = cache.whiteAtt;
        long blAtt = cache.blackAtt;

        long wPieces = b.whitePawns | b.whiteKnights | b.whiteBishops | b.whiteRooks | b.whiteQueens;
        long blPieces = b.blackPawns | b.blackKnights | b.blackBishops | b.blackRooks | b.blackQueens;

        int s = 0;
        s += CONNECTIVITY_BONUS_PER_DEFENDED_PIECE * BitHelper.popcount(wPieces & wAtt);
        s -= CONNECTIVITY_BONUS_PER_DEFENDED_PIECE * BitHelper.popcount(blPieces & blAtt);
        return s;
    }


    private static int trappedPieces(Board b, AttackCache cache) {
        int s = 0;

        long wOcc = b.whitePawns | b.whiteKnights | b.whiteBishops | b.whiteRooks | b.whiteQueens | b.whiteKing;
        long bOcc = b.blackPawns | b.blackKnights | b.blackBishops | b.blackRooks | b.blackQueens | b.blackKing;
        long occ = b.allPieces;

        long wEnemyAtt = cache.blackAtt;
        long bEnemyAtt = cache.whiteAtt;

        // White trapped minors
        s -= trappedMinorPenalty(b.whiteKnights, wOcc, occ, wEnemyAtt, true);
        s -= trappedMinorPenalty(b.whiteBishops, wOcc, occ, wEnemyAtt, false);

        // Black trapped minors
        s += trappedMinorPenalty(b.blackKnights, bOcc, occ, bEnemyAtt, true);
        s += trappedMinorPenalty(b.blackBishops, bOcc, occ, bEnemyAtt, false);

        return s;
    }

    private static int trappedMinorPenalty(long minors, long myOcc, long occ, long enemyAttacks, boolean knight) {
        int p = 0;
        long bb = minors;
        while (bb != 0) {
            int sq = Long.numberOfTrailingZeros(bb);
            bb &= bb - 1;

            long moves;
            if (knight) {
                moves = Constants.KNIGHT_MASKS[sq] & ~myOcc;
            } else {
                moves =
                    (BitHelper.hyperbolaQuintessence(occ, Constants.DIAG_MASKS[sq], sq) |
                        BitHelper.hyperbolaQuintessence(occ, Constants.ANTIDIAG_MASKS[sq], sq))
                        & ~myOcc;
            }

            int mob = BitHelper.popcount(moves);
            boolean attacked = (enemyAttacks & sqBB(sq)) != 0;

            if (attacked && mob <= 1) p += TRAPPED_PIECE_PENALTY;
        }
        return p;
    }


    private static int kingSafety(Board b, AttackCache cache) {
        int s = 0;

        int wKingSq = BitHelper.lsb(b.whiteKing);
        int bKingSq = BitHelper.lsb(b.blackKing);

        long wZone = kingZone(wKingSq);
        long bZone = kingZone(bKingSq);

        s += pawnShieldBonus(b.whitePawns, wKingSq, true);
        s -= pawnShieldBonus(b.blackPawns, bKingSq, false);

        long wEnemyAtt = cache.blackAtt;
        long bEnemyAtt = cache.whiteAtt;

        int attacksOnWhite = BitHelper.popcount(wEnemyAtt & wZone);
        int attacksOnBlack = BitHelper.popcount(bEnemyAtt & bZone);

        s -= KING_ATTACK_SCORES[Math.min(8, attacksOnWhite)];
        s += KING_ATTACK_SCORES[Math.min(8, attacksOnBlack)];

        return s;
    }

    private static int pawnShieldBonus(long pawns, int kingSq, boolean white) {
        int file = kingSq & 7;
        long files = Constants.FILE_MASKS[file];
        if (file > 0) files |= Constants.FILE_MASKS[file - 1];
        if (file < 7) files |= Constants.FILE_MASKS[file + 1];

        long front1 = white ? (sqBB(kingSq) << 8) : (sqBB(kingSq) >>> 8);
        long front2 = white ? (sqBB(kingSq) << 16) : (sqBB(kingSq) >>> 16);

        long shieldSquares = (front1 | front2) & files;
        int cnt = BitHelper.popcount(pawns & shieldSquares);
        return cnt * KING_SAFETY_PAWN_SHIELD;
    }


    private static int space(AttackCache cache) {
        long wAtt = cache.whiteAtt;
        long blAtt = cache.blackAtt;

        long whiteHalfEnemy = Constants.RANK_5 | Constants.RANK_6 | Constants.RANK_7 | Constants.RANK_8;
        long blackHalfEnemy = Constants.RANK_1 | Constants.RANK_2 | Constants.RANK_3 | Constants.RANK_4;

        int s = 0;
        s += SPACE_BONUS_PER_SQ * BitHelper.popcount(wAtt & whiteHalfEnemy);
        s -= SPACE_BONUS_PER_SQ * BitHelper.popcount(blAtt & blackHalfEnemy);

        return s;
    }


    private static long allAttacks(Board b, int side) {
        long occ = b.allPieces;

        long pawns   = (side == Constants.WHITE) ? b.whitePawns   : b.blackPawns;
        long knights = (side == Constants.WHITE) ? b.whiteKnights : b.blackKnights;
        long bishops = (side == Constants.WHITE) ? b.whiteBishops : b.blackBishops;
        long rooks   = (side == Constants.WHITE) ? b.whiteRooks   : b.blackRooks;
        long queens  = (side == Constants.WHITE) ? b.whiteQueens  : b.blackQueens;
        long king    = (side == Constants.WHITE) ? b.whiteKing    : b.blackKing;

        long att = 0;

        // pawns
        att |= BitHelper.attacks(side, pawns);

        // knights
        long bb = knights;
        while (bb != 0) {
            int sq = Long.numberOfTrailingZeros(bb);
            bb &= bb - 1;
            att |= Constants.KNIGHT_MASKS[sq];
        }

        // bishops
        bb = bishops;
        while (bb != 0) {
            int sq = Long.numberOfTrailingZeros(bb);
            bb &= bb - 1;
            att |= BitHelper.hyperbolaQuintessence(occ, Constants.DIAG_MASKS[sq], sq);
            att |= BitHelper.hyperbolaQuintessence(occ, Constants.ANTIDIAG_MASKS[sq], sq);
        }

        // rooks
        bb = rooks;
        while (bb != 0) {
            int sq = Long.numberOfTrailingZeros(bb);
            bb &= bb - 1;
            att |= BitHelper.hyperbolaQuintessence(occ, Constants.RANK_MASKS[sq], sq);
            att |= BitHelper.hyperbolaQuintessence(occ, Constants.FILE_MASKS[sq], sq);
        }

        // queens
        bb = queens;
        while (bb != 0) {
            int sq = Long.numberOfTrailingZeros(bb);
            bb &= bb - 1;

            att |= BitHelper.hyperbolaQuintessence(occ, Constants.RANK_MASKS[sq], sq);
            att |= BitHelper.hyperbolaQuintessence(occ, Constants.FILE_MASKS[sq], sq);
            att |= BitHelper.hyperbolaQuintessence(occ, Constants.DIAG_MASKS[sq], sq);
            att |= BitHelper.hyperbolaQuintessence(occ, Constants.ANTIDIAG_MASKS[sq], sq);
        }

        // king
        if (king != 0) {
            int sq = Long.numberOfTrailingZeros(king);
            att |= Constants.KING_MASKS[sq];
        }

        return att;
    }


    private static int gamePhase(Board b) {
        int phase = 0;

        phase += PHASE_N * (BitHelper.popcount(b.whiteKnights) + BitHelper.popcount(b.blackKnights));
        phase += PHASE_B * (BitHelper.popcount(b.whiteBishops) + BitHelper.popcount(b.blackBishops));
        phase += PHASE_R * (BitHelper.popcount(b.whiteRooks)   + BitHelper.popcount(b.blackRooks));
        phase += PHASE_Q * (BitHelper.popcount(b.whiteQueens)  + BitHelper.popcount(b.blackQueens));

        if (phase > PHASE_MAX) phase = PHASE_MAX;
        return phase;
    }

    private static int taper(int mg, int eg, int phase) {
        return (mg * phase + eg * (PHASE_MAX - phase)) / PHASE_MAX;
    }


    private static long sqBB(int sq) { return 1L << sq; }

    private static int mirrorVertical(int sq) {
        return sq ^ 56;
    }

    private static long northFill(long bb) {
        bb |= bb << 8;
        bb |= bb << 16;
        bb |= bb << 32;
        return bb;
    }

    private static long southFill(long bb) {
        bb |= bb >>> 8;
        bb |= bb >>> 16;
        bb |= bb >>> 32;
        return bb;
    }

    private static long kingZone(int kingSq) {
        long zone = Constants.KING_MASKS[kingSq] | sqBB(kingSq);

        long bb = zone;
        long expanded = 0;
        while (bb != 0) {
            int sq = Long.numberOfTrailingZeros(bb);
            bb &= bb - 1;
            expanded |= Constants.KING_MASKS[sq];
        }
        return zone | expanded;
    }

    private static long squaresBetweenOnRank(int a, int b) {
        if ((a >>> 3) != (b >>> 3)) return 0;
        int min = Math.min(a, b);
        int max = Math.max(a, b);
        long mask = 0;
        for (int sq = min + 1; sq < max; sq++) mask |= sqBB(sq);
        return mask;
    }


    private static final int[] PST_P_MG = {
        0,  0,  0,  0,  0,  0,  0,  0,
        10, 12, 12, -2, -2, 12, 12, 10,
        6,  8, 10, 14, 14, 10,  8,  6,
        4,  6,  8, 16, 16,  8,  6,  4,
        2,  4,  6, 12, 12,  6,  4,  2,
        0,  2,  2,  6,  6,  2,  2,  0,
        0,  0,  0, -8, -8,  0,  0,  0,
        0,  0,  0,  0,  0,  0,  0,  0
    };

    private static final int[] PST_P_EG = {
        0,  0,  0,  0,  0,  0,  0,  0,
        12, 14, 14,  6,  6, 14, 14, 12,
        10, 12, 12, 10, 10, 12, 12, 10,
        8, 10, 10, 12, 12, 10, 10,  8,
        6,  8,  8, 10, 10,  8,  8,  6,
        4,  6,  6,  8,  8,  6,  6,  4,
        2,  4,  4,  6,  6,  4,  4,  2,
        0,  0,  0,  0,  0,  0,  0,  0
    };

    private static final int[] PST_N_MG = {
        -40,-30,-20,-20,-20,-20,-30,-40,
        -30,-10,  0,  4,  4,  0,-10,-30,
        -20,  4, 10, 12, 12, 10,  4,-20,
        -20,  6, 12, 16, 16, 12,  6,-20,
        -20,  6, 12, 16, 16, 12,  6,-20,
        -20,  4, 10, 12, 12, 10,  4,-20,
        -30,-10,  0,  2,  2,  0,-10,-30,
        -40,-30,-20,-20,-20,-20,-30,-40
    };

    private static final int[] PST_N_EG = {
        -30,-20,-10,-10,-10,-10,-20,-30,
        -20, -5,  0,  2,  2,  0, -5,-20,
        -10,  2,  8, 10, 10,  8,  2,-10,
        -10,  4, 10, 12, 12, 10,  4,-10,
        -10,  4, 10, 12, 12, 10,  4,-10,
        -10,  2,  8, 10, 10,  8,  2,-10,
        -20, -5,  0,  2,  2,  0, -5,-20,
        -30,-20,-10,-10,-10,-10,-20,-30
    };

    private static final int[] PST_B_MG = {
        -20,-10,-10,-10,-10,-10,-10,-20,
        -10,  2,  0,  2,  2,  0,  2,-10,
        -10,  6,  8, 10, 10,  8,  6,-10,
        -10,  8, 10, 12, 12, 10,  8,-10,
        -10,  8, 10, 12, 12, 10,  8,-10,
        -10,  6,  8, 10, 10,  8,  6,-10,
        -10,  2,  0,  2,  2,  0,  2,-10,
        -20,-10,-10,-10,-10,-10,-10,-20
    };

    private static final int[] PST_B_EG = {
        -10, -5, -5, -5, -5, -5, -5,-10,
        -5,  2,  2,  2,  2,  2,  2, -5,
        -5,  4,  6,  6,  6,  6,  4, -5,
        -5,  4,  6,  8,  8,  6,  4, -5,
        -5,  4,  6,  8,  8,  6,  4, -5,
        -5,  4,  6,  6,  6,  6,  4, -5,
        -5,  2,  2,  2,  2,  2,  2, -5,
        -10, -5, -5, -5, -5, -5, -5,-10
    };

    private static final int[] PST_R_MG = {
        0,  0,  2,  4,  4,  2,  0,  0,
        -2,  0,  0,  2,  2,  0,  0, -2,
        -2,  0,  0,  2,  2,  0,  0, -2,
        -2,  0,  0,  2,  2,  0,  0, -2,
        -2,  0,  0,  2,  2,  0,  0, -2,
        -2,  0,  0,  2,  2,  0,  0, -2,
        4,  6,  6,  8,  8,  6,  6,  4,
        0,  0,  2,  4,  4,  2,  0,  0
    };

    private static final int[] PST_R_EG = {
        0,  0,  2,  4,  4,  2,  0,  0,
        0,  2,  2,  4,  4,  2,  2,  0,
        0,  2,  2,  4,  4,  2,  2,  0,
        0,  2,  2,  4,  4,  2,  2,  0,
        0,  2,  2,  4,  4,  2,  2,  0,
        0,  2,  2,  4,  4,  2,  2,  0,
        2,  4,  4,  6,  6,  4,  4,  2,
        0,  0,  2,  4,  4,  2,  0,  0
    };

    private static final int[] PST_Q_MG = {
        -10, -5, -5, -2, -2, -5, -5,-10,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  2,  2,  2,  2,  0, -5,
        -2,  0,  2,  4,  4,  2,  0, -2,
        -2,  0,  2,  4,  4,  2,  0, -2,
        -5,  0,  2,  2,  2,  2,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -10, -5, -5, -2, -2, -5, -5,-10
    };

    private static final int[] PST_Q_EG = {
        -5, -2, -2, -2, -2, -2, -2, -5,
        -2,  0,  0,  0,  0,  0,  0, -2,
        -2,  0,  2,  2,  2,  2,  0, -2,
        -2,  0,  2,  4,  4,  2,  0, -2,
        -2,  0,  2,  4,  4,  2,  0, -2,
        -2,  0,  2,  2,  2,  2,  0, -2,
        -2,  0,  0,  0,  0,  0,  0, -2,
        -5, -2, -2, -2, -2, -2, -2, -5
    };

    private static final int[] PST_K_MG = {
        20, 30, 10,  0,  0, 10, 30, 20,
        20, 20,  0,  0,  0,  0, 20, 20,
        -10,-20,-20,-20,-20,-20,-20,-10,
        -20,-30,-30,-40,-40,-30,-30,-20,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30
    };

    private static final int[] PST_K_EG = {
        -30,-20,-10, -5, -5,-10,-20,-30,
        -20,-10,  0,  0,  0,  0,-10,-20,
        -10,  0, 10, 10, 10, 10,  0,-10,
        -5,  0, 10, 15, 15, 10,  0, -5,
        -5,  0, 10, 15, 15, 10,  0, -5,
        -10,  0, 10, 10, 10, 10,  0,-10,
        -20,-10,  0,  0,  0,  0,-10,-20,
        -30,-20,-10, -5, -5,-10,-20,-30
    };
}
