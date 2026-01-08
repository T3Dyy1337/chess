package engine.search;

public class TranspositionTable {

    public static final int EXACT = 0;
    public static final int LOWER = 1;
    public static final int UPPER = 2;

    public static final int MATE_THRESHOLD = 32000;

    private static final int BUCKET_SIZE = 4;

    private static final int ENTRY_SIZE = 28;

    private long[] hashes;

    private int[] depths;

    private int[] flags;

    private int[] scores;

    private int[] bestMoves;

    private int[] ages;

    private volatile int currentGeneration = 1;

    private int numBuckets;

    private final ThreadLocal<ProbeResult> localResult =
        ThreadLocal.withInitial(ProbeResult::new);

    public static final int SHALLOW_HIT = 1;
    public static final int EXACT_HIT = 2;
    public static final int BETA_CUTOFF = 3;
    public static final int ALPHA_CUTOFF = 4;

    public static final int MISS = 5;

    public static final int NO_MOVE = 0;


    public static final class ProbeResult {
        int status;
        int score;
        int bestMove;
    }

    public TranspositionTable(int sizeMB) {
        long tableBytes = (long) sizeMB * 1024 * 1024;

        int totalEntries = (int) tableBytes / ENTRY_SIZE;

        totalEntries -= totalEntries % BUCKET_SIZE;

        numBuckets = totalEntries / BUCKET_SIZE;

        assert numBuckets >= 1;

        numBuckets = largestPowerOfTwo(numBuckets);

        int entryCount = numBuckets * BUCKET_SIZE;

        hashes = new long[entryCount];
        depths = new int[entryCount];
        flags = new int[entryCount];
        scores = new int[entryCount];
        bestMoves = new int[entryCount];
        ages = new int[entryCount];

    }

    public void clear() {
        increaseGeneration();
    }

    private int largestPowerOfTwo(int num) {
        int p = 1;
        while (p <= num) {
            p = p << 1;
        }

        p = p >> 1;
        return p;
    }

    private void swap(int a, int b) {
        long h = hashes[a]; hashes[a] = hashes[b]; hashes[b] = h;
        int d = depths[a]; depths[a] = depths[b]; depths[b] = d;
        int f = flags[a]; flags[a] = flags[b]; flags[b] = f;
        int s = scores[a]; scores[a] = scores[b]; scores[b] = s;
        int m = bestMoves[a]; bestMoves[a] = bestMoves[b]; bestMoves[b] = m;
        int g = ages[a]; ages[a] = ages[b]; ages[b] = g;
    }

    public void store(long hash, int depth, int flag, int score, int bestMove, int ply) {
        int bucketIndex = (int) hash & (numBuckets - 1);
        int entryIndex = bucketIndex * BUCKET_SIZE;

        if (score >= MATE_THRESHOLD) {
            score += ply;
        } else if (score <= -MATE_THRESHOLD) {
            score -= ply;
        }

        for (int i = entryIndex; i < entryIndex + BUCKET_SIZE; i++) {
            if (hashes[i] == 0) {
                depths[i] = depth;
                flags[i] = flag;
                scores[i] = score;
                bestMoves[i] = bestMove;
                ages[i] = currentGeneration;
                hashes[i] = hash;

                if (i != entryIndex && depths[i] > depths[entryIndex]) {
                    swap(entryIndex, i);
                }
                return;
            }
        }

        if (depth > depths[entryIndex]) {
            depths[entryIndex] = depth;
            flags[entryIndex] = flag;
            scores[entryIndex] = score;
            bestMoves[entryIndex] = bestMove;
            ages[entryIndex] = currentGeneration;
            hashes[entryIndex] = hash;
            return;
        }

        int victim = entryIndex + 1;

        for (int i = entryIndex + 2; i < entryIndex + BUCKET_SIZE; i++) {
            if (depths[i] < depths[victim]
                || (depths[i] == depths[victim] && ages[i] < ages[victim])) {
                victim = i;
            }

        }

        depths[victim] = depth;
        flags[victim] = flag;
        scores[victim] = score;
        bestMoves[victim] = bestMove;
        ages[victim] = currentGeneration;
        hashes[victim] = hash;
    }

    public ProbeResult probe(long hash, int depth, int alpha, int beta, int ply) {
        ProbeResult result = localResult.get();
        result.status = MISS;

        int bucketIndex = (int) hash & (numBuckets - 1);
        int entryIndex = bucketIndex * BUCKET_SIZE;

        boolean found = false;
        int shallowBestMove = NO_MOVE;

        int i = entryIndex;
        if (hashes[i] == hash) {
            found = true;
            ages[i] = currentGeneration;

            if (depths[i] >= depth) {
                if (flags[i] == EXACT) {
                    result.status = EXACT_HIT;
                    result.score = scores[i];

                    if(result.score >= MATE_THRESHOLD){
                        result.score -= ply;
                    } else if (result.score <= -MATE_THRESHOLD) {
                        result.score += ply;
                    }

                    result.bestMove = bestMoves[i];
                    return result;
                }
                if (flags[i] == LOWER && scores[i] >= beta) {
                    result.status = BETA_CUTOFF;
                    result.score = scores[i];

                    if(result.score >= MATE_THRESHOLD){
                        result.score -= ply;
                    } else if (result.score <= -MATE_THRESHOLD) {
                        result.score += ply;
                    }

                    return result;
                }
                if (flags[i] == UPPER && scores[i] <= alpha) {
                    result.status = ALPHA_CUTOFF;
                    result.score = scores[i];

                    if(result.score >= MATE_THRESHOLD){
                        result.score -= ply;
                    } else if (result.score <= -MATE_THRESHOLD) {
                        result.score += ply;
                    }

                    return result;
                }
            }
            shallowBestMove = bestMoves[i];
        }


        for (int j = entryIndex + 1; j < entryIndex + BUCKET_SIZE; j++) {
            if (hashes[j] != hash)
                continue;

            found = true;
            ages[j] = currentGeneration;

            if (depths[j] >= depth) {
                if (flags[j] == EXACT) {
                    result.status = EXACT_HIT;
                    result.score = scores[j];

                    if(result.score >= MATE_THRESHOLD){
                        result.score -= ply;
                    } else if (result.score <= -MATE_THRESHOLD) {
                        result.score += ply;
                    }

                    result.bestMove = bestMoves[j];
                    return result;
                }
                if (flags[j] == LOWER && scores[j] >= beta) {
                    result.status = BETA_CUTOFF;
                    result.score = scores[j];

                    if(result.score >= MATE_THRESHOLD){
                        result.score -= ply;
                    } else if (result.score <= -MATE_THRESHOLD) {
                        result.score += ply;
                    }

                    return result;
                }
                if (flags[j] == UPPER && scores[j] <= alpha) {
                    result.status = ALPHA_CUTOFF;
                    result.score = scores[j];

                    if(result.score >= MATE_THRESHOLD){
                        result.score -= ply;
                    } else if (result.score <= -MATE_THRESHOLD) {
                        result.score += ply;
                    }

                    return result;
                }
            }

            if (shallowBestMove == NO_MOVE)
                shallowBestMove = bestMoves[j];
        }
        if (found) {
            result.status = SHALLOW_HIT;
            result.bestMove = shallowBestMove;
        }

        return result;
    }

    public void increaseGeneration(){
        currentGeneration++;
    }
}
