# ♟️ Java Chess Engine

A fast chess engine written in **Java**, implementing advanced search techniques including alpha-beta pruning, transposition tables, move ordering heuristics, and quiescence search.

This project started as a learning exercise and evolved into a competitive engine with an estimated playing strength of **~2200 Elo**.

---

## ✨ Features

- Bitboard-based board representation  
- Legal + pseudo-legal move generation  
- Alpha-beta negamax search  
- Iterative deepening  
- Quiescence search (captures only)  
- Null-move pruning  
- Killer-move heuristic  
- History heuristic  
- Transposition table (bucketed + aging)  
- Mate score normalization by ply  
- Repetition & 50-move rule detection  
- Perft testing utilities  
- Console game mode  
- Experimental UCI shell

---

## 🎮 Play in the Console

Play against the engine directly in a terminal:

```bash
javac -d out $(find . -name "*.java")
java -cp out app.ConsoleGame
```

The engine loads the standard chess starting position and will alternate moves with the user.

##🧠 Search Overview
Move Ordering

- TT move first

- MVV-LVA capture scoring

- Killer moves

- History heuristic

Pruning / Reductions

- Null-move pruning

- Late-move reductions

- Futility-style evaluation margins

Endgame Handling

- Repetition detection

- 50-move rule

- Ply-based mate scoring

🧪 Perft (Correctness Testing)

The engine includes a perft implementation to verify move generator correctness:
```bash
long nodes = Perft.perft(board, depth);
```
Perft outputs can be compared with known reference engines.

🤖 UCI Support (Work in Progress)

A basic UCI shell is included:

```bash
java -cp out app.UCI
```

Currently supports:

uci

isready

position

go

ucinewgame

Integration with GUIs (Arena, CuteChess, etc.) is partially implemented and improving.

🚀 Roadmap

Full UCI time management + bestmove output

Improved aspiration windows

Search extensions (check / singular)

SMP parallel search

Opening book

Simple GUI frontend

Automated testing suite
