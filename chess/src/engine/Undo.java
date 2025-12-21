package engine;

final class Undo {
    int move;
    int movingPiece;
    int capturedPiece;
    int castlingRights;
    int enPassantSquare;
    int halfmoveClock;
    int fullmoveNumber;
    long zobristKey;
}

