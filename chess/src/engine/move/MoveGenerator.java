package engine.move;

import engine.board.Board;
import engine.common.BitHelper;
import engine.common.Constants;

public class MoveGenerator {



    public static int generateAllMoves(Board board, int[] moves){
        int moveCount = 0;
        moveCount = generatePawnMoves(board, moves, moveCount);
        moveCount = generateKnightMoves(board,moves,moveCount);
        moveCount = generateBishopMoves(board,moves,moveCount);
        moveCount = generateRookMoves(board,moves,moveCount);
        moveCount = generateQueenMoves(board,moves,moveCount);
        moveCount = generateKingMoves(board,moves, moveCount);
        return moveCount;
    }


    public static int generatePawnMoves(Board board, int[] moves, int mc) {
        int us = board.sideToMove;
        long pawns = us == Constants.WHITE ? board.whitePawns : board.blackPawns;
        long opp   = us == Constants.WHITE ? board.blackPieces : board.whitePieces;
        long empty = ~board.allPieces;

        long promoRank = us == Constants.WHITE ? Constants.RANK_8 : Constants.RANK_1;

        long singlePush = us == Constants.WHITE
            ? BitHelper.whiteSinglePush(pawns, empty)
            : BitHelper.blackSinglePush(pawns, empty);

        long promos = singlePush & promoRank;
        long quiet  = singlePush & ~promoRank;

        // Promotions
        while (promos != 0) {
            int to = BitHelper.lsb(promos);
            promos &= promos - 1;
            int from = us == Constants.WHITE ? to - 8 : to + 8;

            moves[mc++] = Move.encode(from, to, us == Constants.WHITE ? Constants.W_QUEEN  : Constants.B_QUEEN,  Constants.PROMO_QUEEN);
            moves[mc++] = Move.encode(from, to, us == Constants.WHITE ? Constants.W_ROOK   : Constants.B_ROOK,   Constants.PROMO_ROOK);
            moves[mc++] = Move.encode(from, to, us == Constants.WHITE ? Constants.W_BISHOP : Constants.B_BISHOP, Constants.PROMO_BISHOP);
            moves[mc++] = Move.encode(from, to, us == Constants.WHITE ? Constants.W_KNIGHT : Constants.B_KNIGHT, Constants.PROMO_KNIGHT);
        }

        // Quiet single pushes
        while (quiet != 0) {
            int to = BitHelper.lsb(quiet);
            quiet &= quiet - 1;
            int from = us == Constants.WHITE ? to - 8 : to + 8;
            moves[mc++] = Move.encode(from, to, 0, Constants.QUIET);
        }

        // Double pushes
        long dbl = us == Constants.WHITE
            ? BitHelper.whiteDoublePush(pawns, empty)
            : BitHelper.blackDoublePush(pawns, empty);

        while (dbl != 0) {
            int to = BitHelper.lsb(dbl);
            dbl &= dbl - 1;
            int from = us == Constants.WHITE ? to - 16 : to + 16;
            moves[mc++] = Move.encode(from, to, 0, Constants.DOUBLE_PAWN_PUSH);
        }

        // Captures
        long left  = us == Constants.WHITE ? BitHelper.whiteAttacksLeft(pawns)  : BitHelper.blackAttacksLeft(pawns);
        long right = us == Constants.WHITE ? BitHelper.whiteAttacksRight(pawns) : BitHelper.blackAttacksRight(pawns);

        long leftCaps  = left  & opp;
        long rightCaps = right & opp;

        long promoLeft  = leftCaps  & promoRank;
        long promoRight = rightCaps & promoRank;

        while (promoLeft != 0) {
            int to = BitHelper.lsb(promoLeft);
            promoLeft &= promoLeft - 1;
            int from = us == Constants.WHITE ? to - 7 : to + 9;
            mc = addPromoCaps(moves, mc, from, to, us);
        }

        while (promoRight != 0) {
            int to = BitHelper.lsb(promoRight);
            promoRight &= promoRight - 1;
            int from = us == Constants.WHITE ? to - 9 : to + 7;
            mc = addPromoCaps(moves, mc, from, to, us);
        }

        long quietLeft  = leftCaps  & ~promoRank;
        long quietRight = rightCaps & ~promoRank;

        while (quietLeft != 0) {
            int to = BitHelper.lsb(quietLeft);
            quietLeft &= quietLeft - 1;
            int from = us == Constants.WHITE ? to - 7 : to + 9;
            moves[mc++] = Move.encode(from, to, 0, Constants.CAPTURE);
        }

        while (quietRight != 0) {
            int to = BitHelper.lsb(quietRight);
            quietRight &= quietRight - 1;
            int from = us == Constants.WHITE ? to - 9 : to + 7;
            moves[mc++] = Move.encode(from, to, 0, Constants.CAPTURE);
        }

        // En passant
        if (board.enPassantSquare != -1) {
            int ep = board.enPassantSquare;
            long epMask = 1L << ep;

            if (us == Constants.WHITE) {
                if (((pawns << 7) & Constants.notHFile & epMask) != 0)
                    moves[mc++] = Move.encode(ep - 7, ep, 0, Constants.EN_PASSANT);
                if (((pawns << 9) & Constants.notAFile & epMask) != 0)
                    moves[mc++] = Move.encode(ep - 9, ep, 0, Constants.EN_PASSANT);
            } else {
                if (((pawns >>> 9) & Constants.notHFile & epMask) != 0)
                    moves[mc++] = Move.encode(ep + 9, ep, 0, Constants.EN_PASSANT);
                if (((pawns >>> 7) & Constants.notAFile & epMask) != 0)
                    moves[mc++] = Move.encode(ep + 7, ep, 0, Constants.EN_PASSANT);
            }
        }

        return mc;
    }

    private static int addPromoCaps(int[] moves, int mc, int from, int to, int us) {
        moves[mc++] = Move.encode(from, to, us == Constants.WHITE ? Constants.W_QUEEN  : Constants.B_QUEEN,  Constants.PROMO_QUEEN_CAPTURE);
        moves[mc++] = Move.encode(from, to, us == Constants.WHITE ? Constants.W_ROOK   : Constants.B_ROOK,   Constants.PROMO_ROOK_CAPTURE);
        moves[mc++] = Move.encode(from, to, us == Constants.WHITE ? Constants.W_BISHOP : Constants.B_BISHOP, Constants.PROMO_BISHOP_CAPTURE);
        moves[mc++] = Move.encode(from, to, us == Constants.WHITE ? Constants.W_KNIGHT : Constants.B_KNIGHT, Constants.PROMO_KNIGHT_CAPTURE);
        return mc;
    }



    public static int generateKnightMoves(Board board, int[] moves, int mc) {
        long knights = board.sideToMove == Constants.WHITE ? board.whiteKnights : board.blackKnights;
        long own     = board.sideToMove == Constants.WHITE ? board.whitePieces  : board.blackPieces;
        long opp     = board.sideToMove == Constants.WHITE ? board.blackPieces  : board.whitePieces;

        while (knights != 0) {
            int from = BitHelper.lsb(knights);
            knights &= knights - 1;

            long targets = Constants.KNIGHT_MASKS[from] & ~own;
            while (targets != 0) {
                int to = BitHelper.lsb(targets);
                targets &= targets - 1;
                int flag = ((1L << to) & opp) != 0 ? Constants.CAPTURE : Constants.QUIET;
                moves[mc++] = Move.encode(from, to, 0, flag);
            }
        }
        return mc;
    }


    public static int generateBishopMoves(Board board, int[] moves, int mc) {
        return generateSlidingMoves(board, moves, mc,
            board.sideToMove == Constants.WHITE ? board.whiteBishops : board.blackBishops,
            true, false);
    }

    public static int generateRookMoves(Board board, int[] moves, int mc) {
        return generateSlidingMoves(board, moves, mc,
            board.sideToMove == Constants.WHITE ? board.whiteRooks : board.blackRooks,
            false, true);
    }

    public static int generateQueenMoves(Board board, int[] moves, int mc) {
        return generateSlidingMoves(board, moves, mc,
            board.sideToMove == Constants.WHITE ? board.whiteQueens : board.blackQueens,
            true, true);
    }

    private static int generateSlidingMoves(Board board, int[] moves, int mc,
        long pieces, boolean diag, boolean ortho) {

        long own = board.sideToMove == Constants.WHITE ? board.whitePieces : board.blackPieces;
        long opp = board.sideToMove == Constants.WHITE ? board.blackPieces : board.whitePieces;

        while (pieces != 0) {
            int from = BitHelper.lsb(pieces);
            pieces &= pieces - 1;

            long attacks = 0;
            if (diag)  attacks |= generateBishopRays(board.allPieces, from);
            if (ortho) attacks |= generateRookRays(board.allPieces, from);

            attacks &= ~own;

            while (attacks != 0) {
                int to = BitHelper.lsb(attacks);
                attacks &= attacks - 1;
                int flag = ((1L << to) & opp) != 0 ? Constants.CAPTURE : Constants.QUIET;
                moves[mc++] = Move.encode(from, to, 0, flag);
            }
        }
        return mc;
    }

    public static int generateKingMoves(Board board, int[] moves, int moveCount){
        long king = board.sideToMove == Constants.WHITE ? board.whiteKing : board.blackKing;
        long own  = board.sideToMove == Constants.WHITE ? board.whitePieces : board.blackPieces;
        long opp  = board.sideToMove == Constants.WHITE ? board.blackPieces : board.whitePieces;

        int from = BitHelper.lsb(king);
        long targets = Constants.KING_MASKS[from] & ~own;

        while (targets != 0) {
            int to = BitHelper.lsb(targets);
            targets &= targets - 1;

            if (AttackGenerator.isSquareAttacked(board, to, board.sideToMove ^ 1)) continue;

            int flag = ((1L << to) & opp) != 0 ? Constants.CAPTURE : Constants.QUIET;
            moves[moveCount++] = Move.encode(from, to, 0, flag);
        }


        // Castling
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


            int from = (us == Constants.WHITE) ? to - 7 : to + 9;
            moveCount = addPromoCaps(moves, moveCount, from, to, us);
        }

        while (promoRight != 0) {
            int to = BitHelper.lsb(promoRight);
            promoRight &= promoRight - 1;


            int from = (us == Constants.WHITE) ? to - 9 : to + 7;
            moveCount = addPromoCaps(moves, moveCount, from, to, us);
        }

        long quietLeft  = leftCaps  & ~promoRank;
        long quietRight = rightCaps & ~promoRank;

        while (quietLeft != 0) {
            int to = BitHelper.lsb(quietLeft);
            quietLeft &= quietLeft - 1;


            int from = (us == Constants.WHITE) ? to - 7 : to + 9;
            moves[moveCount++] = Move.encode(from, to, 0, Constants.CAPTURE);
        }

        while (quietRight != 0) {
            int to = BitHelper.lsb(quietRight);
            quietRight &= quietRight - 1;


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


                moves[moveCount++] = Move.encode(from, to, 0, Constants.CAPTURE);
            }
        }
        return moveCount;
    }

    private static int generateKingCaptures(Board board, int[] moves, int mc) {
        long king = board.sideToMove == Constants.WHITE ? board.whiteKing : board.blackKing;
        long opp  = board.sideToMove == Constants.WHITE ? board.blackPieces : board.whitePieces;

        int from = BitHelper.lsb(king);
        long targets = Constants.KING_MASKS[from] & opp;

        while (targets != 0) {
            int to = BitHelper.lsb(targets);
            targets &= targets - 1;
            moves[mc++] = Move.encode(from, to, 0, Constants.CAPTURE);
        }
        return mc;
    }




}
