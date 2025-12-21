package engine;

import util.BitHelper;
import util.Constants;

import java.util.List;

public class MoveGenerator {


    private static boolean isKing(int piece) {
        return piece == Constants.W_KING || piece == Constants.B_KING;
    }

    private static boolean isOppKingOn(Board board, int sq) {
        int victim = board.getPieceOn(sq);
        return isKing(victim);
    }


    public static int generateAllMoves(Board board, int[] moves){
        int moveCount = 0;
        moveCount = generatePawnMoves(board, moves, moveCount);
        moveCount = generateKingMoves(board,moves, moveCount);
        moveCount = generateKnightMoves(board,moves,moveCount);
        moveCount = generateBishopMoves(board,moves,moveCount);
        moveCount = generateRookMoves(board,moves,moveCount);
        moveCount = generateQueenMoves(board,moves,moveCount);
        return moveCount;
    }


    public static int generatePawnMoves(Board board, int[] moves, int moveCount){
        int ourColor = board.sideToMove;

        long pawns = (ourColor == Constants.WHITE) ? board.whitePawns : board.blackPawns;
        long own   = (ourColor == Constants.WHITE ? board.whitePieces : board.blackPieces);
        long opp   = (ourColor == Constants.WHITE ? board.blackPieces : board.whitePieces);
        long empty = ~board.allPieces;

        long singlePush = (ourColor == Constants.WHITE)
                ? BitHelper.whiteSinglePush(pawns, empty)
                : BitHelper.blackSinglePush(pawns, empty);

        long promotionRank = (ourColor == Constants.WHITE) ? Constants.RANK_8 : Constants.RANK_1;

        long promotions      = singlePush & promotionRank;
        long quietSinglePush = singlePush & ~promotionRank;

        // ---- Promotions (quiet) ----
        while (promotions != 0) {
            int to = BitHelper.lsb(promotions);
            long toMask = 1L << to;
            int from = (ourColor == Constants.WHITE) ? to - 8 : to + 8;

            moves[moveCount++] = Move.encode(from, to,
                    (ourColor == Constants.WHITE) ? Constants.W_QUEEN : Constants.B_QUEEN,
                    Constants.PROMO_QUEEN);
            moves[moveCount++] = Move.encode(from, to,
                    (ourColor == Constants.WHITE) ? Constants.W_ROOK : Constants.B_ROOK,
                    Constants.PROMO_ROOK);
            moves[moveCount++] = Move.encode(from, to,
                    (ourColor == Constants.WHITE) ? Constants.W_BISHOP : Constants.B_BISHOP,
                    Constants.PROMO_BISHOP);
            moves[moveCount++] = Move.encode(from, to,
                    (ourColor == Constants.WHITE) ? Constants.W_KNIGHT : Constants.B_KNIGHT,
                    Constants.PROMO_KNIGHT);

            promotions ^= toMask;
        }

        // ---- Quiet single pushes ----
        while (quietSinglePush != 0) {
            int to = BitHelper.lsb(quietSinglePush);
            long toMask = 1L << to;
            int from = (ourColor == Constants.WHITE) ? to - 8 : to + 8;

            moves[moveCount++] = Move.encode(from, to, 0, Constants.QUIET);
            quietSinglePush ^= toMask;
        }

        // ---- Double pushes ----
        long doublePushes = (ourColor == Constants.WHITE)
                ? BitHelper.whiteDoublePush(pawns, empty)
                : BitHelper.blackDoublePush(pawns, empty);

        while (doublePushes != 0) {
            int to = BitHelper.lsb(doublePushes);
            long toMask = 1L << to;
            int from = (ourColor == Constants.WHITE) ? to - 16 : to + 16;

            moves[moveCount++] = Move.encode(from, to, 0, Constants.DOUBLE_PAWN_PUSH);
            doublePushes ^= toMask;
        }

        // ---- Pawn captures ----
        long attacksLeft = (ourColor == Constants.WHITE)
                ? BitHelper.whiteAttacksLeft(pawns)
                : BitHelper.blackAttacksLeft(pawns);

        long attacksRight = (ourColor == Constants.WHITE)
                ? BitHelper.whiteAttacksRight(pawns)
                : BitHelper.blackAttacksRight(pawns);

        long leftCaps  = attacksLeft  & opp;
        long rightCaps = attacksRight & opp;

        long leftPromoCaps  = leftCaps  & promotionRank;
        long rightPromoCaps = rightCaps & promotionRank;

        // Promotion captures (skip if victim is king)
        while (leftPromoCaps != 0) {
            int to = BitHelper.lsb(leftPromoCaps);
            long toMask = 1L << to;

            if (!isOppKingOn(board, to)) {
                int from = (ourColor == Constants.WHITE) ? (to - 7) : (to + 9);

                moves[moveCount++] = Move.encode(from, to,
                        (ourColor == Constants.WHITE) ? Constants.W_QUEEN : Constants.B_QUEEN,
                        Constants.PROMO_QUEEN_CAPTURE);
                moves[moveCount++] = Move.encode(from, to,
                        (ourColor == Constants.WHITE) ? Constants.W_ROOK : Constants.B_ROOK,
                        Constants.PROMO_ROOK_CAPTURE);
                moves[moveCount++] = Move.encode(from, to,
                        (ourColor == Constants.WHITE) ? Constants.W_BISHOP : Constants.B_BISHOP,
                        Constants.PROMO_BISHOP_CAPTURE);
                moves[moveCount++] = Move.encode(from, to,
                        (ourColor == Constants.WHITE) ? Constants.W_KNIGHT : Constants.B_KNIGHT,
                        Constants.PROMO_KNIGHT_CAPTURE);
            }

            leftPromoCaps ^= toMask;
        }

        while (rightPromoCaps != 0) {
            int to = BitHelper.lsb(rightPromoCaps);
            long toMask = 1L << to;

            if (!isOppKingOn(board, to)) {
                int from = (ourColor == Constants.WHITE) ? (to - 9) : (to + 7);

                moves[moveCount++] = Move.encode(from, to,
                        (ourColor == Constants.WHITE) ? Constants.W_QUEEN : Constants.B_QUEEN,
                        Constants.PROMO_QUEEN_CAPTURE);
                moves[moveCount++] = Move.encode(from, to,
                        (ourColor == Constants.WHITE) ? Constants.W_ROOK : Constants.B_ROOK,
                        Constants.PROMO_ROOK_CAPTURE);
                moves[moveCount++] = Move.encode(from, to,
                        (ourColor == Constants.WHITE) ? Constants.W_BISHOP : Constants.B_BISHOP,
                        Constants.PROMO_BISHOP_CAPTURE);
                moves[moveCount++] = Move.encode(from, to,
                        (ourColor == Constants.WHITE) ? Constants.W_KNIGHT : Constants.B_KNIGHT,
                        Constants.PROMO_KNIGHT_CAPTURE);
            }

            rightPromoCaps ^= toMask;
        }

        // Normal captures (skip if victim is king)
        long leftQuietCaps  = leftCaps  & ~promotionRank;
        long rightQuietCaps = rightCaps & ~promotionRank;

        while (leftQuietCaps != 0) {
            int to = BitHelper.lsb(leftQuietCaps);
            long toMask = 1L << to;

            if (!isOppKingOn(board, to)) {
                int from = (ourColor == Constants.WHITE) ? (to - 7) : (to + 9);
                moves[moveCount++] = Move.encode(from, to, 0, Constants.CAPTURE);
            }

            leftQuietCaps ^= toMask;
        }

        while (rightQuietCaps != 0) {
            int to = BitHelper.lsb(rightQuietCaps);
            long toMask = 1L << to;

            if (!isOppKingOn(board, to)) {
                int from = (ourColor == Constants.WHITE) ? (to - 9) : (to + 7);
                moves[moveCount++] = Move.encode(from, to, 0, Constants.CAPTURE);
            }

            rightQuietCaps ^= toMask;
        }

        // ---- En passant ---- (can never capture a king)
        if (board.enPassantSquare != -1) {
            long epMask = 1L << board.enPassantSquare;

            if (ourColor == Constants.WHITE) {
                long leftEP  = (pawns << 7) & Constants.notHFile & epMask;
                long rightEP = (pawns << 9) & Constants.notAFile & epMask;

                if (leftEP != 0) {
                    int to = board.enPassantSquare;
                    int from = to - 7;
                    moves[moveCount++] = Move.encode(from, to, 0, Constants.EN_PASSANT);
                }
                if (rightEP != 0) {
                    int to = board.enPassantSquare;
                    int from = to - 9;
                    moves[moveCount++] = Move.encode(from, to, 0, Constants.EN_PASSANT);
                }

            } else {
                long leftEP  = (pawns >>> 9) & Constants.notHFile & epMask;
                long rightEP = (pawns >>> 7) & Constants.notAFile & epMask;

                if (leftEP != 0) {
                    int to = board.enPassantSquare;
                    int from = to + 9;
                    moves[moveCount++] = Move.encode(from, to, 0, Constants.EN_PASSANT);
                }
                if (rightEP != 0) {
                    int to = board.enPassantSquare;
                    int from = to + 7;
                    moves[moveCount++] = Move.encode(from, to, 0, Constants.EN_PASSANT);
                }
            }
        }

        return moveCount;
    }

    public static int generateKnightMoves(Board board, int[] moves, int moveCount) {
        long knights = board.sideToMove == Constants.WHITE ? board.whiteKnights : board.blackKnights;
        long own     = board.sideToMove == Constants.WHITE ? board.whitePieces : board.blackPieces;
        long oppBB   = board.sideToMove == Constants.WHITE ? board.blackPieces : board.whitePieces;

        while (knights != 0) {
            int from = BitHelper.lsb(knights);
            long fromMask = 1L << from;
            knights ^= fromMask;

            long targets = Constants.KNIGHT_MASKS[from] & ~own;

            while (targets != 0) {
                int to = BitHelper.lsb(targets);
                long toMask = 1L << to;
                targets ^= toMask;

                boolean isCap = (toMask & oppBB) != 0;
                if (isCap && isOppKingOn(board, to)) continue; // <<< forbid king capture

                int flag = isCap ? Constants.CAPTURE : Constants.QUIET;
                moves[moveCount++] = Move.encode(from, to, 0, flag);
            }
        }
        return moveCount;
    }


    public static int generateBishopMoves(Board board, int[] moves, int moveCount){
        long bishops = board.sideToMove == Constants.WHITE ? board.whiteBishops : board.blackBishops;
        long own     = board.sideToMove == Constants.WHITE ? board.whitePieces : board.blackPieces;
        long oppBB   = board.sideToMove == Constants.WHITE ? board.blackPieces : board.whitePieces;

        while (bishops != 0) {
            int from = BitHelper.lsb(bishops);
            long fromMask = 1L << from;
            bishops ^= fromMask;

            long targets = generateBishopRays(board.allPieces, from) & ~own;

            while (targets != 0) {
                int to = BitHelper.lsb(targets);
                long toMask = 1L << to;
                targets ^= toMask;

                boolean isCap = (toMask & oppBB) != 0;
                if (isCap && isOppKingOn(board, to)) continue;

                int flag = isCap ? Constants.CAPTURE : Constants.QUIET;
                moves[moveCount++] = Move.encode(from, to, 0, flag);
            }
        }
        return moveCount;
    }

    public static int generateRookMoves(Board board, int[] moves, int moveCount){
        long rooks = board.sideToMove == Constants.WHITE ? board.whiteRooks : board.blackRooks;
        long own   = board.sideToMove == Constants.WHITE ? board.whitePieces : board.blackPieces;
        long oppBB = board.sideToMove == Constants.WHITE ? board.blackPieces : board.whitePieces;

        while (rooks != 0) {
            int from = BitHelper.lsb(rooks);
            long fromMask = 1L << from;
            rooks ^= fromMask;

            long targets = generateRookRays(board.allPieces, from) & ~own;

            while (targets != 0) {
                int to = BitHelper.lsb(targets);
                long toMask = 1L << to;
                targets ^= toMask;

                boolean isCap = (toMask & oppBB) != 0;
                if (isCap && isOppKingOn(board, to)) continue;

                int flag = isCap ? Constants.CAPTURE : Constants.QUIET;
                moves[moveCount++] = Move.encode(from, to, 0, flag);
            }
        }
        return moveCount;
    }

    public static int generateQueenMoves(Board board, int[] moves, int moveCount){
        long queens = board.sideToMove == Constants.WHITE ? board.whiteQueens : board.blackQueens;
        long own    = board.sideToMove == Constants.WHITE ? board.whitePieces : board.blackPieces;
        long oppBB  = board.sideToMove == Constants.WHITE ? board.blackPieces : board.whitePieces;

        while (queens != 0) {
            int from = BitHelper.lsb(queens);
            long fromMask = 1L << from;
            queens ^= fromMask;

            long targets = generateQueenRays(board.allPieces, from) & ~own;

            while (targets != 0) {
                int to = BitHelper.lsb(targets);
                long toMask = 1L << to;
                targets ^= toMask;

                boolean isCap = (toMask & oppBB) != 0;
                if (isCap && isOppKingOn(board, to)) continue;

                int flag = isCap ? Constants.CAPTURE : Constants.QUIET;
                moves[moveCount++] = Move.encode(from, to, 0, flag);
            }
        }
        return moveCount;
    }

    public static int generateKingMoves(Board board, int[] moves, int moveCount){
        long king  = board.sideToMove == Constants.WHITE ? board.whiteKing : board.blackKing;
        long own   = board.sideToMove == Constants.WHITE ? board.whitePieces : board.blackPieces;
        long oppBB = board.sideToMove == Constants.WHITE ? board.blackPieces : board.whitePieces;

        int from = BitHelper.lsb(king);
        long targets = Constants.KING_MASKS[from] & ~own;

        while (targets != 0) {
            int to = BitHelper.lsb(targets);
            long toMask = 1L << to;
            targets ^= toMask;

            // Can't move king into check
            if (AttackGenerator.isSquareAttacked(board, to, board.sideToMove ^ 1)) continue;

            boolean isCap = (toMask & oppBB) != 0;
            if (isCap && isOppKingOn(board, to)) continue; // forbid king capture (should never happen anyway)

            int flag = isCap ? Constants.CAPTURE : Constants.QUIET;
            moves[moveCount++] = Move.encode(from, to, 0, flag);
        }

        // Castling (unchanged)
        if (board.sideToMove == Constants.WHITE) {
            if ((board.castlingRights & Constants.WHITE_KINGSIDE) != 0) {
                long emptyMask = (1L << Constants.F1) | (1L << Constants.G1);
                if ((board.allPieces & emptyMask) == 0) {
                    if (!AttackGenerator.isSquareAttacked(board, Constants.E1, Constants.BLACK) &&
                            !AttackGenerator.isSquareAttacked(board, Constants.F1, Constants.BLACK) &&
                            !AttackGenerator.isSquareAttacked(board, Constants.G1, Constants.BLACK)) {
                        moves[moveCount++] = Move.encode(Constants.E1, Constants.G1, 0, Constants.KING_CASTLE);
                    }
                }
            }
            if ((board.castlingRights & Constants.WHITE_QUEENSIDE) != 0) {
                long emptyMask = (1L << Constants.B1) | (1L << Constants.C1) | (1L << Constants.D1);
                if ((board.allPieces & emptyMask) == 0) {
                    if (!AttackGenerator.isSquareAttacked(board, Constants.E1, Constants.BLACK) &&
                            !AttackGenerator.isSquareAttacked(board, Constants.D1, Constants.BLACK) &&
                            !AttackGenerator.isSquareAttacked(board, Constants.C1, Constants.BLACK)) {
                        moves[moveCount++] = Move.encode(Constants.E1, Constants.C1, 0, Constants.QUEEN_CASTLE);
                    }
                }
            }
        } else {
            if ((board.castlingRights & Constants.BLACK_KINGSIDE) != 0) {
                long emptyMask = (1L << Constants.F8) | (1L << Constants.G8);
                if ((board.allPieces & emptyMask) == 0) {
                    if (!AttackGenerator.isSquareAttacked(board, Constants.E8, Constants.WHITE) &&
                            !AttackGenerator.isSquareAttacked(board, Constants.F8, Constants.WHITE) &&
                            !AttackGenerator.isSquareAttacked(board, Constants.G8, Constants.WHITE)) {
                        moves[moveCount++] = Move.encode(Constants.E8, Constants.G8, 0, Constants.KING_CASTLE);
                    }
                }
            }
            if ((board.castlingRights & Constants.BLACK_QUEENSIDE) != 0) {
                long emptyMask = (1L << Constants.B8) | (1L << Constants.C8) | (1L << Constants.D8);
                if ((board.allPieces & emptyMask) == 0) {
                    if (!AttackGenerator.isSquareAttacked(board, Constants.E8, Constants.WHITE) &&
                            !AttackGenerator.isSquareAttacked(board, Constants.D8, Constants.WHITE) &&
                            !AttackGenerator.isSquareAttacked(board, Constants.C8, Constants.WHITE)) {
                        moves[moveCount++] = Move.encode(Constants.E8, Constants.C8, 0, Constants.QUEEN_CASTLE);
                    }
                }
            }
        }

        return moveCount;
    }
    public static long generateBishopRays(long occupancy, int square){

        return BitHelper.hyperbolaQuintessence(occupancy,Constants.DIAG_MASKS[square], square)
                | BitHelper.hyperbolaQuintessence(occupancy,Constants.ANTIDIAG_MASKS[square],square);
    }


    public static long generateRookRays(long occupancy, int square){
        return BitHelper.hyperbolaQuintessence(occupancy,Constants.FILE_MASKS[square], square)
                | BitHelper.hyperbolaQuintessence(occupancy,Constants.RANK_MASKS[square],square);
    }

    public static long generateQueenRays(long occupancy, int square){
        return generateBishopRays(occupancy,square) | generateRookRays(occupancy,square);
    }


    public static int generateCaptures(Board board, int[] moves) {
        int moveCount = 0;
        moveCount = generatePawnCaptures(board, moves, moveCount);
        moveCount = generateKnightCaptures(board, moves, moveCount);
        moveCount = generateBishopCaptures(board, moves, moveCount);
        moveCount = generateRookCaptures(board, moves, moveCount);
        moveCount = generateQueenCaptures(board, moves, moveCount);
        moveCount = generateKingCaptures(board, moves, moveCount);
        return moveCount;
    }

    private static int generatePawnCaptures(Board board, int[] moves, int moveCount) {
        int us = board.sideToMove;

        long pawns = (us == Constants.WHITE) ? board.whitePawns : board.blackPawns;
        long oppBB = (us == Constants.WHITE) ? board.blackPieces : board.whitePieces;

        long promoRank = (us == Constants.WHITE) ? Constants.RANK_8 : Constants.RANK_1;

        long leftAttacks  = (us == Constants.WHITE) ? BitHelper.whiteAttacksLeft(pawns)
                : BitHelper.blackAttacksLeft(pawns);
        long rightAttacks = (us == Constants.WHITE) ? BitHelper.whiteAttacksRight(pawns)
                : BitHelper.blackAttacksRight(pawns);

        long leftCaps  = leftAttacks  & oppBB;
        long rightCaps = rightAttacks & oppBB;

        long promoLeft  = leftCaps  & promoRank;
        long promoRight = rightCaps & promoRank;

        while (promoLeft != 0) {
            int to = BitHelper.lsb(promoLeft);
            promoLeft &= promoLeft - 1;

            if (isOppKingOn(board, to)) continue;

            int from = (us == Constants.WHITE) ? to - 7 : to + 9;
            moveCount = addPromoCaps(moves, moveCount, from, to, us);
        }

        while (promoRight != 0) {
            int to = BitHelper.lsb(promoRight);
            promoRight &= promoRight - 1;

            if (isOppKingOn(board, to)) continue;

            int from = (us == Constants.WHITE) ? to - 9 : to + 7;
            moveCount = addPromoCaps(moves, moveCount, from, to, us);
        }

        long quietLeft  = leftCaps  & ~promoRank;
        long quietRight = rightCaps & ~promoRank;

        while (quietLeft != 0) {
            int to = BitHelper.lsb(quietLeft);
            quietLeft &= quietLeft - 1;

            if (isOppKingOn(board, to)) continue;

            int from = (us == Constants.WHITE) ? to - 7 : to + 9;
            moves[moveCount++] = Move.encode(from, to, 0, Constants.CAPTURE);
        }

        while (quietRight != 0) {
            int to = BitHelper.lsb(quietRight);
            quietRight &= quietRight - 1;

            if (isOppKingOn(board, to)) continue;

            int from = (us == Constants.WHITE) ? to - 9 : to + 7;
            moves[moveCount++] = Move.encode(from, to, 0, Constants.CAPTURE);
        }

        // En passant (cannot capture king)
        if (board.enPassantSquare != -1) {
            int ep = board.enPassantSquare;
            long epMask = 1L << ep;

            if (us == Constants.WHITE) {
                if (((pawns << 7) & Constants.notHFile & epMask) != 0)
                    moves[moveCount++] = Move.encode(ep - 7, ep, 0, Constants.EN_PASSANT);
                if (((pawns << 9) & Constants.notAFile & epMask) != 0)
                    moves[moveCount++] = Move.encode(ep - 9, ep, 0, Constants.EN_PASSANT);
            } else {
                if (((pawns >>> 9) & Constants.notHFile & epMask) != 0)
                    moves[moveCount++] = Move.encode(ep + 9, ep, 0, Constants.EN_PASSANT);
                if (((pawns >>> 7) & Constants.notAFile & epMask) != 0)
                    moves[moveCount++] = Move.encode(ep + 7, ep, 0, Constants.EN_PASSANT);
            }
        }

        return moveCount;
    }

    private static int addPromoCaps(int[] moves, int mc, int from, int to, int us) {
        moves[mc++] = Move.encode(from, to, us == Constants.WHITE ? Constants.W_QUEEN  : Constants.B_QUEEN,  Constants.PROMO_QUEEN_CAPTURE);
        moves[mc++] = Move.encode(from, to, us == Constants.WHITE ? Constants.W_ROOK   : Constants.B_ROOK,   Constants.PROMO_ROOK_CAPTURE);
        moves[mc++] = Move.encode(from, to, us == Constants.WHITE ? Constants.W_BISHOP : Constants.B_BISHOP, Constants.PROMO_BISHOP_CAPTURE);
        moves[mc++] = Move.encode(from, to, us == Constants.WHITE ? Constants.W_KNIGHT : Constants.B_KNIGHT, Constants.PROMO_KNIGHT_CAPTURE);
        return mc;
    }


    private static int generateKnightCaptures(Board board, int[] moves, int moveCount) {
        long knights = board.sideToMove == Constants.WHITE ? board.whiteKnights : board.blackKnights;
        long oppBB   = board.sideToMove == Constants.WHITE ? board.blackPieces : board.whitePieces;

        while (knights != 0) {
            int from = BitHelper.lsb(knights);
            knights &= knights - 1;

            long targets = Constants.KNIGHT_MASKS[from] & oppBB;
            while (targets != 0) {
                int to = BitHelper.lsb(targets);
                targets &= targets - 1;

                if (isOppKingOn(board, to)) continue;

                moves[moveCount++] = Move.encode(from, to, 0, Constants.CAPTURE);
            }
        }
        return moveCount;
    }


    private static int generateBishopCaptures(Board board, int[] moves, int moveCount) {
        return generateSlidingCaptures(board, moves, moveCount,
                board.sideToMove == Constants.WHITE ? board.whiteBishops : board.blackBishops,
                board.sideToMove == Constants.WHITE ? board.blackPieces : board.whitePieces,
                true, false);
    }

    private static int generateRookCaptures(Board board, int[] moves, int moveCount) {
        return generateSlidingCaptures(board, moves, moveCount,
                board.sideToMove == Constants.WHITE ? board.whiteRooks : board.blackRooks,
                board.sideToMove == Constants.WHITE ? board.blackPieces : board.whitePieces,
                false, true);
    }

    private static int generateQueenCaptures(Board board, int[] moves, int moveCount) {
        return generateSlidingCaptures(board, moves, moveCount,
                board.sideToMove == Constants.WHITE ? board.whiteQueens : board.blackQueens,
                board.sideToMove == Constants.WHITE ? board.blackPieces : board.whitePieces,
                true, true);
    }

    private static int generateSlidingCaptures(Board board, int[] moves, int moveCount,
                                               long pieces, long oppBB, boolean diag, boolean ortho) {

        while (pieces != 0) {
            int from = BitHelper.lsb(pieces);
            pieces &= pieces - 1;

            long attacks = 0;
            if (diag)  attacks |= generateBishopRays(board.allPieces, from);
            if (ortho) attacks |= generateRookRays(board.allPieces, from);

            long caps = attacks & oppBB;
            while (caps != 0) {
                int to = BitHelper.lsb(caps);
                caps &= caps - 1;

                if (isOppKingOn(board, to)) continue;

                moves[moveCount++] = Move.encode(from, to, 0, Constants.CAPTURE);
            }
        }
        return moveCount;
    }

    private static int generateKingCaptures(Board board, int[] moves, int moveCount) {
        long king  = board.sideToMove == Constants.WHITE ? board.whiteKing : board.blackKing;
        long oppBB = board.sideToMove == Constants.WHITE ? board.blackPieces : board.whitePieces;

        int from = BitHelper.lsb(king);
        long targets = Constants.KING_MASKS[from] & oppBB;

        while (targets != 0) {
            int to = BitHelper.lsb(targets);
            targets &= targets - 1;

            if (isOppKingOn(board, to)) continue;

            moves[moveCount++] = Move.encode(from, to, 0, Constants.CAPTURE);
        }
        return moveCount;
    }



}
