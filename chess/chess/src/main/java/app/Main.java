package app;

import engine.board.Board;

import engine.move.Move;
import engine.move.MoveGenerator;
import engine.search.Search;
import engine.search.TranspositionTable;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        ConsoleGame.play(false,null);
    }
}



