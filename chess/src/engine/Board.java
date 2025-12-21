package engine;

import util.BitHelper;
import util.Constants;
import util.Zobrist;

public class Board {

    public long whitePawns, whiteKnights, whiteBishops, whiteRooks, whiteQueens, whiteKing;
    public long blackPawns, blackKnights, blackBishops, blackRooks, blackQueens, blackKing;

    public long whitePieces, blackPieces, allPieces;
    public int sideToMove;

    public int castlingRights;
    public int enPassantSquare;
    public int halfmoveClock;
    public int fullmoveNumber;

    public long zobristKey;

    // ✅ NEW: cached king squares
    public int whiteKingSq = -1;
    public int blackKingSq = -1;

    private final int[] pieceAt = new int[64];
    private final Undo[] undoStack = new Undo[2048];
    private int undoTop = 0;

    public Board() {
        clear();
    }

    private void clear() {
        whitePawns = whiteKnights = whiteBishops = whiteRooks = whiteQueens = whiteKing = 0L;
        blackPawns = blackKnights = blackBishops = blackRooks = blackQueens = blackKing = 0L;
        whitePieces = blackPieces = allPieces = 0L;

        sideToMove = Constants.WHITE;
        castlingRights = 0;
        enPassantSquare = -1;
        halfmoveClock = 0;
        fullmoveNumber = 1;

        zobristKey = 0L;
        whiteKingSq = -1;
        blackKingSq = -1;

        for (int i = 0; i < 64; i++) pieceAt[i] = -1;
        undoTop = 0;
    }

    public void loadFEN(String fen) {
        clear();

        String[] parts = fen.trim().split("\\s+");
        String piecePlacement = parts[0];
        String activeColor = parts[1];
        String castling = parts[2];
        String enPassant = parts[3];

        int sq = 56;

        for (char c : piecePlacement.toCharArray()) {
            if (c == '/') { sq -= 16; continue; }
            if (Character.isDigit(c)) { sq += c - '0'; continue; }

            long bit = 1L << sq;

            switch (c) {
                case 'P' -> { whitePawns |= bit; pieceAt[sq] = Constants.W_PAWN; }
                case 'N' -> { whiteKnights |= bit; pieceAt[sq] = Constants.W_KNIGHT; }
                case 'B' -> { whiteBishops |= bit; pieceAt[sq] = Constants.W_BISHOP; }
                case 'R' -> { whiteRooks |= bit; pieceAt[sq] = Constants.W_ROOK; }
                case 'Q' -> { whiteQueens |= bit; pieceAt[sq] = Constants.W_QUEEN; }
                case 'K' -> {
                    whiteKing |= bit;
                    whiteKingSq = sq;          // ✅ NEW
                    pieceAt[sq] = Constants.W_KING;
                }

                case 'p' -> { blackPawns |= bit; pieceAt[sq] = Constants.B_PAWN; }
                case 'n' -> { blackKnights |= bit; pieceAt[sq] = Constants.B_KNIGHT; }
                case 'b' -> { blackBishops |= bit; pieceAt[sq] = Constants.B_BISHOP; }
                case 'r' -> { blackRooks |= bit; pieceAt[sq] = Constants.B_ROOK; }
                case 'q' -> { blackQueens |= bit; pieceAt[sq] = Constants.B_QUEEN; }
                case 'k' -> {
                    blackKing |= bit;
                    blackKingSq = sq;          // ✅ NEW
                    pieceAt[sq] = Constants.B_KING;
                }
            }
            sq++;
        }

        sideToMove = activeColor.equals("w") ? Constants.WHITE : Constants.BLACK;

        if (castling.contains("K")) castlingRights |= Constants.WHITE_KINGSIDE;
        if (castling.contains("Q")) castlingRights |= Constants.WHITE_QUEENSIDE;
        if (castling.contains("k")) castlingRights |= Constants.BLACK_KINGSIDE;
        if (castling.contains("q")) castlingRights |= Constants.BLACK_QUEENSIDE;

        if (!enPassant.equals("-")) {
            int file = enPassant.charAt(0) - 'a';
            int rank = enPassant.charAt(1) - '1';
            enPassantSquare = rank * 8 + file;
        }

        whitePieces = whitePawns | whiteKnights | whiteBishops | whiteRooks | whiteQueens | whiteKing;
        blackPieces = blackPawns | blackKnights | blackBishops | blackRooks | blackQueens | blackKing;
        allPieces = whitePieces | blackPieces;

        zobristKey = Zobrist.hash(this);
    }

    // ✅ FAST check test (no lsb!)
    public boolean isInCheck() {
        int kingSq = (sideToMove == Constants.WHITE) ? whiteKingSq : blackKingSq;
        int attacker = sideToMove ^ 1;
        return AttackGenerator.isSquareAttacked(this, kingSq, attacker);
    }

    public int getPieceOn(int sq) {
        return pieceAt[sq];
    }

    private static boolean isWhitePiece(int piece) {
        return piece <= Constants.W_KING;
    }

    private void removePieceNoHash(int piece, int sq) {
        long bb = 1L << sq;
        pieceAt[sq] = -1;

        switch (piece) {
            case Constants.W_KING -> { whiteKing &= ~bb; whiteKingSq = -1; }
            case Constants.B_KING -> { blackKing &= ~bb; blackKingSq = -1; }

            case Constants.W_PAWN -> whitePawns &= ~bb;
            case Constants.W_KNIGHT -> whiteKnights &= ~bb;
            case Constants.W_BISHOP -> whiteBishops &= ~bb;
            case Constants.W_ROOK -> whiteRooks &= ~bb;
            case Constants.W_QUEEN -> whiteQueens &= ~bb;

            case Constants.B_PAWN -> blackPawns &= ~bb;
            case Constants.B_KNIGHT -> blackKnights &= ~bb;
            case Constants.B_BISHOP -> blackBishops &= ~bb;
            case Constants.B_ROOK -> blackRooks &= ~bb;
            case Constants.B_QUEEN -> blackQueens &= ~bb;
        }

        if (isWhitePiece(piece)) whitePieces &= ~bb;
        else blackPieces &= ~bb;
        allPieces &= ~bb;
    }

    private void addPieceNoHash(int piece, int sq) {
        long bb = 1L << sq;
        pieceAt[sq] = piece;

        switch (piece) {
            case Constants.W_KING -> { whiteKing |= bb; whiteKingSq = sq; }
            case Constants.B_KING -> { blackKing |= bb; blackKingSq = sq; }

            case Constants.W_PAWN -> whitePawns |= bb;
            case Constants.W_KNIGHT -> whiteKnights |= bb;
            case Constants.W_BISHOP -> whiteBishops |= bb;
            case Constants.W_ROOK -> whiteRooks |= bb;
            case Constants.W_QUEEN -> whiteQueens |= bb;

            case Constants.B_PAWN -> blackPawns |= bb;
            case Constants.B_KNIGHT -> blackKnights |= bb;
            case Constants.B_BISHOP -> blackBishops |= bb;
            case Constants.B_ROOK -> blackRooks |= bb;
            case Constants.B_QUEEN -> blackQueens |= bb;
        }

        if (isWhitePiece(piece)) whitePieces |= bb;
        else blackPieces |= bb;
        allPieces |= bb;
    }

    private void addPiece(int piece, int sq) {
        addPieceNoHash(piece, sq);
        zobristKey ^= Zobrist.PIECE_KEYS[piece][sq];
    }

    private void removePiece(int piece, int sq) {
        removePieceNoHash(piece, sq);
        zobristKey ^= Zobrist.PIECE_KEYS[piece][sq];
    }


    private int updateCastlingRights(int rights, int from, int to) {
        if (from == Constants.E1) rights &= ~(Constants.WHITE_KINGSIDE | Constants.WHITE_QUEENSIDE);
        if (from == Constants.E8) rights &= ~(Constants.BLACK_KINGSIDE | Constants.BLACK_QUEENSIDE);

        if (from == Constants.H1) rights &= ~Constants.WHITE_KINGSIDE;
        if (from == Constants.A1) rights &= ~Constants.WHITE_QUEENSIDE;
        if (from == Constants.H8) rights &= ~Constants.BLACK_KINGSIDE;
        if (from == Constants.A8) rights &= ~Constants.BLACK_QUEENSIDE;

        if (to == Constants.H1) rights &= ~Constants.WHITE_KINGSIDE;
        if (to == Constants.A1) rights &= ~Constants.WHITE_QUEENSIDE;
        if (to == Constants.H8) rights &= ~Constants.BLACK_KINGSIDE;
        if (to == Constants.A8) rights &= ~Constants.BLACK_QUEENSIDE;

        return rights;
    }


    public void makeMove(int move) {
        if (undoTop >= undoStack.length) {
            throw new IllegalStateException("Undo stack overflow (depth too large).");
        }

        int from  = Move.from(move);
        int to    = Move.to(move);
        int flag  = Move.flags(move);
        int promo = Move.promo(move);

        int us = sideToMove;

        Undo u = new Undo();
        u.move = move;
        u.castlingRights = castlingRights;
        u.enPassantSquare = enPassantSquare;
        u.halfmoveClock = halfmoveClock;
        u.fullmoveNumber = fullmoveNumber;
        u.zobristKey = zobristKey;

        int movingPiece = getPieceOn(from);
        if (movingPiece == -1) {
            throw new IllegalStateException("No piece on from-square " + from + ", to-square: " + to + ", flag :" + flag);
        }
        u.movingPiece = movingPiece;

        // Determine captured piece (if any)
        if (flag == Constants.EN_PASSANT) {
            u.capturedPiece = (us == Constants.WHITE) ? Constants.B_PAWN : Constants.W_PAWN;
        } else if (flag == Constants.CAPTURE || flag >= Constants.PROMO_KNIGHT_CAPTURE) {
            u.capturedPiece = getPieceOn(to);
            if (u.capturedPiece == -1) {
                throw new IllegalStateException("Capture flag but no victim on square " + to);
            }
        } else {
            u.capturedPiece = -1;
        }

        undoStack[undoTop++] = u;

        if (enPassantSquare != -1) {
            zobristKey ^= Zobrist.ENPASSANT_KEYS[enPassantSquare];
            enPassantSquare = -1;
        }

        removePiece(movingPiece, from);

        boolean isCapture = (u.capturedPiece != -1);

        if (flag == Constants.CAPTURE || flag >= Constants.PROMO_KNIGHT_CAPTURE) {
            removePiece(u.capturedPiece, to);
        } else if (flag == Constants.EN_PASSANT) {
            int capSq = (us == Constants.WHITE) ? to - 8 : to + 8;
            removePiece(u.capturedPiece, capSq);
        }

        if (flag >= Constants.PROMO_KNIGHT) {
            addPiece(promo, to);
        } else if (flag == Constants.KING_CASTLE || flag == Constants.QUEEN_CASTLE) {
            addPiece(movingPiece, to);

            if (flag == Constants.KING_CASTLE) {
                if (us == Constants.WHITE) {
                    removePiece(Constants.W_ROOK, Constants.H1);
                    addPiece(Constants.W_ROOK, Constants.F1);
                } else {
                    removePiece(Constants.B_ROOK, Constants.H8);
                    addPiece(Constants.B_ROOK, Constants.F8);
                }
            } else {
                if (us == Constants.WHITE) {
                    removePiece(Constants.W_ROOK, Constants.A1);
                    addPiece(Constants.W_ROOK, Constants.D1);
                } else {
                    removePiece(Constants.B_ROOK, Constants.A8);
                    addPiece(Constants.B_ROOK, Constants.D8);
                }
            }
        } else if (flag == Constants.DOUBLE_PAWN_PUSH) {
            addPiece(movingPiece, to);
            enPassantSquare = (us == Constants.WHITE) ? to - 8 : to + 8;
            zobristKey ^= Zobrist.ENPASSANT_KEYS[enPassantSquare];
        } else {
            addPiece(movingPiece, to);
        }

        zobristKey ^= Zobrist.CASTLING_KEYS[castlingRights];
        castlingRights = updateCastlingRights(castlingRights, from, to);
        zobristKey ^= Zobrist.CASTLING_KEYS[castlingRights];

        sideToMove ^= 1;
        zobristKey ^= Zobrist.SIDE_TO_MOVE_KEY;

        boolean pawnMove = (movingPiece == Constants.W_PAWN || movingPiece == Constants.B_PAWN);
        boolean promoMove = (flag >= Constants.PROMO_KNIGHT);

        if (pawnMove || isCapture || promoMove) halfmoveClock = 0;
        else halfmoveClock++;

        if (us == Constants.BLACK) fullmoveNumber++;
    }

    public void unmakeMove() {
        Undo u = undoStack[--undoTop];

        int move  = u.move;
        int from  = Move.from(move);
        int to    = Move.to(move);
        int flag  = Move.flags(move);
        int promo = Move.promo(move);

        // Restore simple fields (we restore zobristKey at the END)
        castlingRights  = u.castlingRights;
        enPassantSquare = u.enPassantSquare;
        halfmoveClock   = u.halfmoveClock;
        fullmoveNumber  = u.fullmoveNumber;

        // Switch side back
        sideToMove ^= 1;
        int us = sideToMove;

        // Undo piece placement on 'to'
        if (flag >= Constants.PROMO_KNIGHT) {
            removePieceNoHash(promo, to);
        } else {
            removePieceNoHash(u.movingPiece, to);
        }

        // Put mover back on 'from'
        addPieceNoHash(u.movingPiece, from);

        // Restore captured piece
        if (u.capturedPiece != -1) {
            if (flag == Constants.EN_PASSANT) {
                int capSq = (us == Constants.WHITE) ? to - 8 : to + 8;
                addPieceNoHash(u.capturedPiece, capSq);
            } else {
                addPieceNoHash(u.capturedPiece, to);
            }
        }

        // Undo castling rook move
        if (flag == Constants.KING_CASTLE || flag == Constants.QUEEN_CASTLE) {
            if (flag == Constants.KING_CASTLE) {
                if (us == Constants.WHITE) {
                    removePieceNoHash(Constants.W_ROOK, Constants.F1);
                    addPieceNoHash(Constants.W_ROOK, Constants.H1);
                } else {
                    removePieceNoHash(Constants.B_ROOK, Constants.F8);
                    addPieceNoHash(Constants.B_ROOK, Constants.H8);
                }
            } else {
                if (us == Constants.WHITE) {
                    removePieceNoHash(Constants.W_ROOK, Constants.D1);
                    addPieceNoHash(Constants.W_ROOK, Constants.A1);
                } else {
                    removePieceNoHash(Constants.B_ROOK, Constants.D8);
                    addPieceNoHash(Constants.B_ROOK, Constants.A8);
                }
            }
        }

        zobristKey = u.zobristKey;
    }

    public void makeNullMove() {
        Undo u = new Undo();
        u.move = 0;
        u.castlingRights = castlingRights;
        u.enPassantSquare = enPassantSquare;
        u.halfmoveClock = halfmoveClock;
        u.fullmoveNumber = fullmoveNumber;
        u.zobristKey = zobristKey;
        undoStack[undoTop++] = u;

        if (enPassantSquare != -1) {
            zobristKey ^= Zobrist.ENPASSANT_KEYS[enPassantSquare];
            enPassantSquare = -1;
        }

        sideToMove ^= 1;
        zobristKey ^= Zobrist.SIDE_TO_MOVE_KEY;
    }

    public void unmakeNullMove() {
        Undo u = undoStack[--undoTop];
        castlingRights = u.castlingRights;
        enPassantSquare = u.enPassantSquare;
        halfmoveClock = u.halfmoveClock;
        fullmoveNumber = u.fullmoveNumber;
        zobristKey = u.zobristKey;
        sideToMove ^= 1;
    }

}







