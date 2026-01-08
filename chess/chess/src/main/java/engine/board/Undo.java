package engine.board;

final class Undo {
    int move;
    int movingPiece;
    int capturedPiece;
    int castlingRights;
    int enPassantSquare;
    int halfmoveClock;
    int fullmoveNumber;
    long zobristKey;
    int lastIrreversiblePly;

    public void copyFrom(Undo o) {
        this.move = o.move;
        this.movingPiece = o.movingPiece;
        this.capturedPiece = o.capturedPiece;
        this.castlingRights = o.castlingRights;
        this.enPassantSquare = o.enPassantSquare;
        this.halfmoveClock = o.halfmoveClock;
        this.fullmoveNumber = o.fullmoveNumber;
        this.zobristKey = o.zobristKey;
        this.lastIrreversiblePly = o.lastIrreversiblePly;
    }

}

