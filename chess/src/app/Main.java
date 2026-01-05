package app;

import engine.board.Board;

import engine.move.Move;
import engine.move.MoveGenerator;
import engine.search.Search;
import engine.search.TranspositionTable;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        Board b = new Board();
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        b.loadFEN(fen);

    }




    }



