package engine.board;


import engine.move.AttackGenerator;
import engine.move.Move;
import engine.common.Constants;

public class Board implements CloneableBoard {

    public long whitePawns, whiteKnights, whiteBishops, whiteRooks, whiteQueens, whiteKing;
    public long blackPawns, blackKnights, blackBishops, blackRooks, blackQueens, blackKing;

    public long whitePieces, blackPieces, allPieces;
    public int sideToMove;

    public int castlingRights;
    public int enPassantSquare;
    public int halfmoveClock;
    public int fullmoveNumber;

    public long zobristKey;

    public int whiteKingSq = -1;
    public int blackKingSq = -1;

    public int whiteNonPawnMaterial;
    public int blackNonPawnMaterial;


    public long[] keyHistory = new long[256];
    public int historyPly = 0;

    public int lastIrreversiblePly = 0;



    private final int[] pieceAt = new int[64];
    private final Undo[] undoStack = new Undo[2048];
    private int undoTop = 0;





    public Board() {
        clear();
        for (int i = 0; i < undoStack.length; i++) undoStack[i] = new Undo();
    }


    public void clear() {
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

        whiteNonPawnMaterial = 0;
        blackNonPawnMaterial = 0;

        for (int i = 0; i < 64; i++) pieceAt[i] = -1;
        historyPly = 0;
        lastIrreversiblePly = 0;

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
                case 'N' -> { whiteKnights |= bit; pieceAt[sq] = Constants.W_KNIGHT; whiteNonPawnMaterial += 3; }
                case 'B' -> { whiteBishops |= bit; pieceAt[sq] = Constants.W_BISHOP; whiteNonPawnMaterial += 3; }
                case 'R' -> { whiteRooks |= bit;   pieceAt[sq] = Constants.W_ROOK;   whiteNonPawnMaterial += 5; }
                case 'Q' -> { whiteQueens |= bit;  pieceAt[sq] = Constants.W_QUEEN;  whiteNonPawnMaterial += 9; }
                case 'K' -> {
                    whiteKing |= bit;
                    whiteKingSq = sq;
                    pieceAt[sq] = Constants.W_KING;
                }

                case 'p' -> { blackPawns |= bit; pieceAt[sq] = Constants.B_PAWN; }
                case 'n' -> { blackKnights |= bit; pieceAt[sq] = Constants.B_KNIGHT; blackNonPawnMaterial += 3; }
                case 'b' -> { blackBishops |= bit; pieceAt[sq] = Constants.B_BISHOP; blackNonPawnMaterial += 3; }
                case 'r' -> { blackRooks |= bit;   pieceAt[sq] = Constants.B_ROOK;   blackNonPawnMaterial += 5; }
                case 'q' -> { blackQueens |= bit;  pieceAt[sq] = Constants.B_QUEEN;  blackNonPawnMaterial += 9; }
                case 'k' -> {
                    blackKing |= bit;
                    blackKingSq = sq;
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
        historyPly = 0;
        lastIrreversiblePly = 0;

    }

    public boolean isInCheck() {
        int kingSq = (sideToMove == Constants.WHITE) ? whiteKingSq : blackKingSq;
        int attacker = sideToMove ^ 1;
        return AttackGenerator.isSquareAttacked(this, kingSq, attacker);
    }

    public int getPieceOn(int sq) {
        return pieceAt[sq];
    }

    private static boolean isWhitePiece(int piece) {
        return (piece & 1) == 0;
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
        int m = pieceMaterial(piece);
        if (m != 0) {
            if (isWhitePiece(piece)) whiteNonPawnMaterial -= m;
            else blackNonPawnMaterial -= m;
        }

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

        int m = pieceMaterial(piece);
        if (m != 0) {
            if (isWhitePiece(piece)) whiteNonPawnMaterial += m;
            else blackNonPawnMaterial += m;
        }

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

        final int from  = Move.from(move);
        final int to    = Move.to(move);
        final int flag  = Move.flags(move);
        final int promo = Move.promo(move);

        final int us = sideToMove;

        final int movingPiece = pieceAt[from];
        if (movingPiece == -1) {
            throw new IllegalStateException("No piece on from-square " + from +
                ", to-square: " + to + ", flag :" + flag);
        }

        final Undo u = undoStack[undoTop++];
        u.move = move;
        u.movingPiece = movingPiece;
        u.castlingRights = castlingRights;
        u.enPassantSquare = enPassantSquare;
        u.halfmoveClock = halfmoveClock;
        u.fullmoveNumber = fullmoveNumber;
        u.zobristKey = zobristKey;
        u.lastIrreversiblePly = lastIrreversiblePly;

        keyHistory[historyPly++] = zobristKey;

        if (flag == Constants.CAPTURE
            || flag == Constants.EN_PASSANT
            || (flag >= Constants.PROMO_KNIGHT && flag <= Constants.PROMO_QUEEN)
            || movingPiece == Constants.W_PAWN || movingPiece == Constants.B_PAWN) {

            lastIrreversiblePly = historyPly;
        }

        int capturedPiece = -1;
        if (flag == Constants.EN_PASSANT) {
            capturedPiece = (us == Constants.WHITE) ? Constants.B_PAWN : Constants.W_PAWN;
        } else if (flag == Constants.CAPTURE ||
            (flag >= Constants.PROMO_KNIGHT_CAPTURE && flag <= Constants.PROMO_QUEEN_CAPTURE)) {
            capturedPiece = pieceAt[to];
            if (capturedPiece == -1) {
                throw new IllegalStateException("Capture flag but no victim on square " + to +
                    " (from=" + from + ", flag=" + flag + ")");
            }
        }
        u.capturedPiece = capturedPiece;


        if (enPassantSquare != -1) {
            zobristKey ^= Zobrist.ENPASSANT_KEYS[enPassantSquare];
            enPassantSquare = -1;
        }

        removePiece(movingPiece, from);

        if (capturedPiece != -1) {
            if (flag == Constants.EN_PASSANT) {
                final int capSq = (us == Constants.WHITE) ? (to - 8) : (to + 8);
                removePiece(capturedPiece, capSq);
            } else {
                removePiece(capturedPiece, to);
            }
        }

        final boolean isPromotion = (flag >= Constants.PROMO_KNIGHT);
        if (isPromotion) {
            addPiece(promo, to);
        } else {
            addPiece(movingPiece, to);
        }

        if (flag == Constants.KING_CASTLE) {
            if (us == Constants.WHITE) {
                removePiece(Constants.W_ROOK, Constants.H1);
                addPiece(Constants.W_ROOK, Constants.F1);

            } else {
                removePiece(Constants.B_ROOK, Constants.H8);
                addPiece(Constants.B_ROOK, Constants.F8);

            }
        } else if (flag == Constants.QUEEN_CASTLE) {
            if (us == Constants.WHITE) {
                removePiece(Constants.W_ROOK, Constants.A1);
                addPiece(Constants.W_ROOK, Constants.D1);

            } else {
                removePiece(Constants.B_ROOK, Constants.A8);
                addPiece(Constants.B_ROOK, Constants.D8);

            }
        }

        if (flag == Constants.DOUBLE_PAWN_PUSH) {
            enPassantSquare = (us == Constants.WHITE) ? (to - 8) : (to + 8);
            zobristKey ^= Zobrist.ENPASSANT_KEYS[enPassantSquare];
        }

        zobristKey ^= Zobrist.CASTLING_KEYS[castlingRights];
        castlingRights = updateCastlingRights(castlingRights, from, to);
        zobristKey ^= Zobrist.CASTLING_KEYS[castlingRights];

        sideToMove ^= 1;
        zobristKey ^= Zobrist.SIDE_TO_MOVE_KEY;

        final boolean pawnMove = (movingPiece == Constants.W_PAWN || movingPiece == Constants.B_PAWN);
        if (pawnMove || capturedPiece != -1 || isPromotion) halfmoveClock = 0;
        else halfmoveClock++;

        if (us == Constants.BLACK) fullmoveNumber++;
    }

    public void unmakeMove() {
        final Undo u = undoStack[--undoTop];

        final int move = u.move;
        final int from = Move.from(move);
        final int to   = Move.to(move);
        final int flag = Move.flags(move);
        final int promo = Move.promo(move);

        castlingRights  = u.castlingRights;
        enPassantSquare = u.enPassantSquare;
        halfmoveClock   = u.halfmoveClock;
        fullmoveNumber  = u.fullmoveNumber;

        sideToMove ^= 1;
        final int us = sideToMove;

        if (flag == Constants.KING_CASTLE) {
            if (us == Constants.WHITE) {
                removePieceNoHash(Constants.W_ROOK, Constants.F1);
                addPieceNoHash(Constants.W_ROOK, Constants.H1);

            } else {
                removePieceNoHash(Constants.B_ROOK, Constants.F8);
                addPieceNoHash(Constants.B_ROOK, Constants.H8);
            }
        } else if (flag == Constants.QUEEN_CASTLE) {
            if (us == Constants.WHITE) {
                removePieceNoHash(Constants.W_ROOK, Constants.D1);
                addPieceNoHash(Constants.W_ROOK, Constants.A1);
            } else {
                removePieceNoHash(Constants.B_ROOK, Constants.D8);
                addPieceNoHash(Constants.B_ROOK, Constants.A8);
            }
        }

        final boolean isPromotion = (flag >= Constants.PROMO_KNIGHT);
        if (isPromotion) {
            removePieceNoHash(promo, to);

        } else {
            removePieceNoHash(u.movingPiece, to);

        }

        addPieceNoHash(u.movingPiece, from);


        if (u.capturedPiece != -1) {
            if (flag == Constants.EN_PASSANT) {
                final int capSq = (us == Constants.WHITE) ? (to - 8) : (to + 8);
                addPieceNoHash(u.capturedPiece, capSq);

            } else {
                addPieceNoHash(u.capturedPiece, to);

            }
        }

        zobristKey = u.zobristKey;
        historyPly--;
        lastIrreversiblePly = u.lastIrreversiblePly;


    }

    public void print() {
        System.out.println();
        for (int rank = 7; rank >= 0; rank--) {
            System.out.print((rank + 1) + "  ");
            for (int file = 0; file < 8; file++) {
                int sq = rank * 8 + file;
                int piece = getPieceOn(sq);
                System.out.print(pieceChar(piece) + " ");
            }
            System.out.println();
        }
        System.out.println();
        System.out.println("   a b c d e f g h");
        System.out.println();
    }

    private char pieceChar(int piece) {
        return switch (piece) {
            case Constants.W_PAWN   -> 'P';
            case Constants.W_KNIGHT -> 'N';
            case Constants.W_BISHOP -> 'B';
            case Constants.W_ROOK   -> 'R';
            case Constants.W_QUEEN  -> 'Q';
            case Constants.W_KING   -> 'K';

            case Constants.B_PAWN   -> 'p';
            case Constants.B_KNIGHT -> 'n';
            case Constants.B_BISHOP -> 'b';
            case Constants.B_ROOK   -> 'r';
            case Constants.B_QUEEN  -> 'q';
            case Constants.B_KING   -> 'k';

            default -> '.';
        };
    }


    @Override public Board copy() {
        Board b = new Board();

        b.whitePawns = whitePawns;
        b.whiteKnights = whiteKnights;
        b.whiteBishops = whiteBishops;
        b.whiteRooks = whiteRooks;
        b.whiteQueens = whiteQueens;
        b.whiteKing = whiteKing;

        b.blackPawns = blackPawns;
        b.blackKnights = blackKnights;
        b.blackBishops = blackBishops;
        b.blackRooks = blackRooks;
        b.blackQueens = blackQueens;
        b.blackKing = blackKing;

        b.whitePieces = whitePieces;
        b.blackPieces = blackPieces;
        b.allPieces = allPieces;

        b.sideToMove = sideToMove;
        b.castlingRights = castlingRights;
        b.enPassantSquare = enPassantSquare;
        b.halfmoveClock = halfmoveClock;
        b.fullmoveNumber = fullmoveNumber;

        b.zobristKey = zobristKey;

        b.whiteKingSq = whiteKingSq;
        b.blackKingSq = blackKingSq;

        System.arraycopy(this.pieceAt, 0, b.pieceAt, 0, 64);

        b.historyPly = historyPly;
        b.lastIrreversiblePly = lastIrreversiblePly;
        System.arraycopy(this.keyHistory, 0, b.keyHistory, 0, historyPly);

        b.undoTop = undoTop;
        for (int i = 0; i < undoTop; i++) {
            b.undoStack[i].copyFrom(this.undoStack[i]);
        }

        return b;
    }

    public void makeNullMove() {
        Undo u = undoStack[undoTop++];

        u.move = 0;
        u.castlingRights = castlingRights;
        u.enPassantSquare = enPassantSquare;
        u.halfmoveClock = halfmoveClock;
        u.fullmoveNumber = fullmoveNumber;
        u.zobristKey = zobristKey;
        u.lastIrreversiblePly = lastIrreversiblePly;

        keyHistory[historyPly++] = zobristKey;

        if (enPassantSquare != -1) {
            zobristKey ^= Zobrist.ENPASSANT_KEYS[enPassantSquare];
            enPassantSquare = -1;
        }

        sideToMove ^= 1;
        zobristKey ^= Zobrist.SIDE_TO_MOVE_KEY;

        halfmoveClock++;

    }

    public void unmakeNullMove() {
        Undo u = undoStack[--undoTop];

        castlingRights = u.castlingRights;
        enPassantSquare = u.enPassantSquare;
        halfmoveClock = u.halfmoveClock;
        fullmoveNumber = u.fullmoveNumber;
        zobristKey = u.zobristKey;

        sideToMove ^= 1;

        historyPly--;
        lastIrreversiblePly = u.lastIrreversiblePly;
    }

    private static int pieceMaterial(int piece) {
        return switch (piece) {
            case Constants.W_KNIGHT, Constants.B_KNIGHT -> 3;
            case Constants.W_BISHOP, Constants.B_BISHOP -> 3;
            case Constants.W_ROOK,   Constants.B_ROOK   -> 5;
            case Constants.W_QUEEN,  Constants.B_QUEEN  -> 9;
            default -> 0;
        };
    }

    public int nonPawnMaterial(int side) {
        return side == Constants.WHITE
            ? whiteNonPawnMaterial
            : blackNonPawnMaterial;
    }

    public boolean isRepetition() {
        long key = zobristKey;

        for (int i = historyPly - 2; i >= lastIrreversiblePly; i -= 2) {
            if (keyHistory[i] == key) {
                return true;
            }
        }
        return false;
    }

    public boolean isThreefoldRepetition() {
        int repeats = 1;
        long key = zobristKey;

        for (int i = historyPly - 2; i >= 0; i -= 2) {
            if (keyHistory[i] == key) repeats++;
            if (repeats >= 3) return true;
        }
        return false;
    }




}







