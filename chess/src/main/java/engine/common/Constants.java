package engine.common;

public class Constants {


    // h8 g8 f8 e8 d8 c8 b8 a8
    // h7 g7 f7 e7 d7 c7 b7 a7
    // h6 g6 f6 e6 d6 c6 b6 a6
    // h5 g5 f5 e5 d5 c5 b5 a5
    // h4 g4 f4 e4 d4 c4 b4 a4
    // h3 g3 f3 e3 d3 c3 b3 a3
    // h2 g2 f2 e2 d2 c2 b2 a2
    // h1 g1 f1 e1 d1 c1 b1 a1



    public static final long notAFile = 0xFEFEFEFEFEFEFEFEL;
    public static final long notHFile = 0x7F7F7F7F7F7F7F7FL;

    public static final long notABFiles = 0xFCFCFCFCFCFCFCFCL;
    public static final long notGHFiles = 0x3F3F3F3F3F3F3F3FL;

    public static final int A1 = 0;
    public static final int B1 = 1;
    public static final int C1 = 2;
    public static final int D1 = 3;
    public static final int E1 = 4;
    public static final int F1 = 5;
    public static final int G1 = 6;
    public static final int H1 = 7;

    public static final int A2 = 8;
    public static final int B2 = 9;
    public static final int C2 = 10;
    public static final int D2 = 11;
    public static final int E2 = 12;
    public static final int F2 = 13;
    public static final int G2 = 14;
    public static final int H2 = 15;

    public static final int A3 = 16;
    public static final int B3 = 17;
    public static final int C3 = 18;
    public static final int D3 = 19;
    public static final int E3 = 20;
    public static final int F3 = 21;
    public static final int G3 = 22;
    public static final int H3 = 23;

    public static final int A4 = 24;
    public static final int B4 = 25;
    public static final int C4 = 26;
    public static final int D4 = 27;
    public static final int E4 = 28;
    public static final int F4 = 29;
    public static final int G4 = 30;
    public static final int H4 = 31;

    public static final int A5 = 32;
    public static final int B5 = 33;
    public static final int C5 = 34;
    public static final int D5 = 35;
    public static final int E5 = 36;
    public static final int F5 = 37;
    public static final int G5 = 38;
    public static final int H5 = 39;

    public static final int A6 = 40;
    public static final int B6 = 41;
    public static final int C6 = 42;
    public static final int D6 = 43;
    public static final int E6 = 44;
    public static final int F6 = 45;
    public static final int G6 = 46;
    public static final int H6 = 47;

    public static final int A7 = 48;
    public static final int B7 = 49;
    public static final int C7 = 50;
    public static final int D7 = 51;
    public static final int E7 = 52;
    public static final int F7 = 53;
    public static final int G7 = 54;
    public static final int H7 = 55;

    public static final int A8 = 56;
    public static final int B8 = 57;
    public static final int C8 = 58;
    public static final int D8 = 59;
    public static final int E8 = 60;
    public static final int F8 = 61;
    public static final int G8 = 62;
    public static final int H8 = 63;

    public static final long RANK_1 =  0x00000000000000FFL;
    public static final long RANK_2 =  0x000000000000FF00L;
    public static final long RANK_3 =  0x0000000000FF0000L;
    public static final long RANK_4 =  0x00000000FF000000L;
    public static final long RANK_5 =  0x000000FF00000000L;
    public static final long RANK_6 =  0x0000FF0000000000L;
    public static final long RANK_7 =  0x00FF000000000000L;
    public static final long RANK_8 =  0xFF00000000000000L;

    public static final long FILE_A = 0x0101010101010101L;
    public static final long FILE_B = 0x0202020202020202L;
    public static final long FILE_C = 0x0404040404040404L;
    public static final long FILE_D = 0x0808080808080808L;
    public static final long FILE_E = 0x1010101010101010L;
    public static final long FILE_F = 0x2020202020202020L;
    public static final long FILE_G = 0x4040404040404040L;
    public static final long FILE_H = 0x8080808080808080L;

    public static final int WHITE = 0;
    public static final int BLACK = 1;

    public static final int W_PAWN = 0;
    public static final int B_PAWN = 1;

    public static final int W_KNIGHT = 2;
    public static final int B_KNIGHT = 3;

    public static final int W_BISHOP = 4;
    public static final int B_BISHOP = 5;

    public static final int W_ROOK = 6;
    public static final int B_ROOK = 7;

    public static final int W_QUEEN = 8;
    public static final int B_QUEEN = 9;

    public static final int W_KING = 10;
    public static final int B_KING = 11;


    public static final int PAWN_VALUE = 100;
    public static final int KNIGHT_VALUE = 315;
    public static final int BISHOP_VALUE = 330;

    public static final int ROOK_VALUE = 500;
    public static final int QUEEN_VALUE = 900;

    public static final int KING_VALUE = 10000;

    public static final int[] PIECE_VALUE = {PAWN_VALUE,PAWN_VALUE,KNIGHT_VALUE,KNIGHT_VALUE,BISHOP_VALUE,BISHOP_VALUE,ROOK_VALUE,ROOK_VALUE,QUEEN_VALUE,QUEEN_VALUE,KING_VALUE,KING_VALUE};

    public static final int WHITE_KINGSIDE = 1;
    public static final int WHITE_QUEENSIDE = 1 << 1;

    public static final int BLACK_KINGSIDE = 1 << 2;

    public static final int BLACK_QUEENSIDE = 1 << 3;

    public static final int QUIET = 0;
    public static final int CAPTURE = 1;
    public static final int DOUBLE_PAWN_PUSH = 2;
    public static final int KING_CASTLE = 3;
    public static final int QUEEN_CASTLE = 4;
    public static final int EN_PASSANT = 5;

    // --- Promotions (quiet) ---
    public static final int PROMO_KNIGHT = 8;
    public static final int PROMO_BISHOP = 9;
    public static final int PROMO_ROOK = 10;
    public static final int PROMO_QUEEN = 11;

    // --- Promotions (capture) ---
    public static final int PROMO_KNIGHT_CAPTURE = 12;
    public static final int PROMO_BISHOP_CAPTURE = 13;
    public static final int PROMO_ROOK_CAPTURE = 14;
    public static final int PROMO_QUEEN_CAPTURE = 15;



    public static final long[] SQUARE_MASKS = {
            1L, 2L, 4L, 8L, 16L, 32L, 64L, 128L, 256L, 512L, 1024L, 2048L, 4096L, 8192L,
            16384L, 32768L, 65536L, 131072L, 262144L, 524288L, 1048576L, 2097152L, 4194304L,
            8388608L, 16777216L, 33554432L, 67108864L, 134217728L, 268435456L, 536870912L,
            1073741824L, 2147483648L, 4294967296L, 8589934592L, 17179869184L, 34359738368L,
            68719476736L, 137438953472L, 274877906944L, 549755813888L, 1099511627776L,
            2199023255552L, 4398046511104L, 8796093022208L, 17592186044416L, 35184372088832L,
            70368744177664L, 140737488355328L, 281474976710656L, 562949953421312L,
            1125899906842624L, 2251799813685248L, 4503599627370496L, 9007199254740992L,
            18014398509481984L, 36028797018963968L, 72057594037927936L, 144115188075855872L,
            288230376151711744L, 576460752303423488L, 1152921504606846976L, 2305843009213693952L,
            4611686018427387904L, -9223372036854775808L
    };

    public static final long[] KNIGHT_MASKS = {
            132096L,
            329728L,
            659712L,
            1319424L,
            2638848L,
            5277696L,
            10489856L,
            4202496L,
            33816580L,
            84410376L,
            168886289L,
            337772578L,
            675545156L,
            1351090312L,
            2685403152L,
            1075839008L,
            8657044482L,
            21609056261L,
            43234889994L,
            86469779988L,
            172939559976L,
            345879119952L,
            687463207072L,
            275414786112L,
            2216203387392L,
            5531918402816L,
            11068131838464L,
            22136263676928L,
            44272527353856L,
            88545054707712L,
            175990581010432L,
            70506185244672L,
            567348067172352L,
            1416171111120896L,
            2833441750646784L,
            5666883501293568L,
            11333767002587136L,
            22667534005174272L,
            45053588738670592L,
            18049583422636032L,
            145241105196122112L,
            362539804446949376L,
            725361088165576704L,
            1450722176331153408L,
            2901444352662306816L,
            5802888705324613632L,
            -6913025356609880064L,
            4620693356194824192L,
            288234782788157440L,
            576469569871282176L,
            1224997833292120064L,
            2449995666584240128L,
            4899991333168480256L,
            -8646761407372591104L,
            1152939783987658752L,
            2305878468463689728L,
            1128098930098176L,
            2257297371824128L,
            4796069720358912L,
            9592139440717824L,
            19184278881435648L,
            38368557762871296L,
            4679521487814656L,
            9077567998918656L,
    };


    public static final long[] KING_MASKS = {
            770L,
            1797L,
            3594L,
            7188L,
            14376L,
            28752L,
            57504L,
            49216L,
            197123L,
            460039L,
            920078L,
            1840156L,
            3680312L,
            7360624L,
            14721248L,
            12599488L,
            50463488L,
            117769984L,
            235539968L,
            471079936L,
            942159872L,
            1884319744L,
            3768639488L,
            3225468928L,
            12918652928L,
            30149115904L,
            60298231808L,
            120596463616L,
            241192927232L,
            482385854464L,
            964771708928L,
            825720045568L,
            3307175149568L,
            7718173671424L,
            15436347342848L,
            30872694685696L,
            61745389371392L,
            123490778742784L,
            246981557485568L,
            211384331665408L,
            846636838289408L,
            1975852459884544L,
            3951704919769088L,
            7903409839538176L,
            15806819679076352L,
            31613639358152704L,
            63227278716305408L,
            54114388906344448L,
            216739030602088448L,
            505818229730443264L,
            1011636459460886528L,
            2023272918921773056L,
            4046545837843546112L,
            8093091675687092224L,
            -2260560722335367168L,
            -4593460513685372928L,
            144959613005987840L,
            362258295026614272L,
            724516590053228544L,
            1449033180106457088L,
            2898066360212914176L,
            5796132720425828352L,
            -6854478632857894912L,
            4665729213955833856L,
    };

    public static final long[] FILE_MASKS = {
            72340172838076673L,
            144680345676153346L,
            289360691352306692L,
            578721382704613384L,
            1157442765409226768L,
            2314885530818453536L,
            4629771061636907072L,
            -9187201950435737472L,
            72340172838076673L,
            144680345676153346L,
            289360691352306692L,
            578721382704613384L,
            1157442765409226768L,
            2314885530818453536L,
            4629771061636907072L,
            -9187201950435737472L,
            72340172838076673L,
            144680345676153346L,
            289360691352306692L,
            578721382704613384L,
            1157442765409226768L,
            2314885530818453536L,
            4629771061636907072L,
            -9187201950435737472L,
            72340172838076673L,
            144680345676153346L,
            289360691352306692L,
            578721382704613384L,
            1157442765409226768L,
            2314885530818453536L,
            4629771061636907072L,
            -9187201950435737472L,
            72340172838076673L,
            144680345676153346L,
            289360691352306692L,
            578721382704613384L,
            1157442765409226768L,
            2314885530818453536L,
            4629771061636907072L,
            -9187201950435737472L,
            72340172838076673L,
            144680345676153346L,
            289360691352306692L,
            578721382704613384L,
            1157442765409226768L,
            2314885530818453536L,
            4629771061636907072L,
            -9187201950435737472L,
            72340172838076673L,
            144680345676153346L,
            289360691352306692L,
            578721382704613384L,
            1157442765409226768L,
            2314885530818453536L,
            4629771061636907072L,
            -9187201950435737472L,
            72340172838076673L,
            144680345676153346L,
            289360691352306692L,
            578721382704613384L,
            1157442765409226768L,
            2314885530818453536L,
            4629771061636907072L,
            -9187201950435737472L,
    };

    public static final long[] RANK_MASKS = {
            255L,
            255L,
            255L,
            255L,
            255L,
            255L,
            255L,
            255L,
            65280L,
            65280L,
            65280L,
            65280L,
            65280L,
            65280L,
            65280L,
            65280L,
            16711680L,
            16711680L,
            16711680L,
            16711680L,
            16711680L,
            16711680L,
            16711680L,
            16711680L,
            4278190080L,
            4278190080L,
            4278190080L,
            4278190080L,
            4278190080L,
            4278190080L,
            4278190080L,
            4278190080L,
            1095216660480L,
            1095216660480L,
            1095216660480L,
            1095216660480L,
            1095216660480L,
            1095216660480L,
            1095216660480L,
            1095216660480L,
            280375465082880L,
            280375465082880L,
            280375465082880L,
            280375465082880L,
            280375465082880L,
            280375465082880L,
            280375465082880L,
            280375465082880L,
            71776119061217280L,
            71776119061217280L,
            71776119061217280L,
            71776119061217280L,
            71776119061217280L,
            71776119061217280L,
            71776119061217280L,
            71776119061217280L,
            -72057594037927936L,
            -72057594037927936L,
            -72057594037927936L,
            -72057594037927936L,
            -72057594037927936L,
            -72057594037927936L,
            -72057594037927936L,
            -72057594037927936L,
    };

    public static final long[] DIAG_MASKS = {0x8040201008040201L,
            0x0080402010080402L,
            0x0000804020100804L,
            0x0000008040201008L,
            0x0000000080402010L,
            0x0000000000804020L,
            0x0000000000008040L,
            0x0000000000000080L,
            0x4020100804020100L,
            0x8040201008040201L,
            0x0080402010080402L,
            0x0000804020100804L,
            0x0000008040201008L,
            0x0000000080402010L,
            0x0000000000804020L,
            0x0000000000008040L,
            0x2010080402010000L,
            0x4020100804020100L,
            0x8040201008040201L,
            0x0080402010080402L,
            0x0000804020100804L,
            0x0000008040201008L,
            0x0000000080402010L,
            0x0000000000804020L,
            0x1008040201000000L,
            0x2010080402010000L,
            0x4020100804020100L,
            0x8040201008040201L,
            0x0080402010080402L,
            0x0000804020100804L,
            0x0000008040201008L,
            0x0000000080402010L,
            0x0804020100000000L,
            0x1008040201000000L,
            0x2010080402010000L,
            0x4020100804020100L,
            0x8040201008040201L,
            0x0080402010080402L,
            0x0000804020100804L,
            0x0000008040201008L,
            0x0402010000000000L,
            0x0804020100000000L,
            0x1008040201000000L,
            0x2010080402010000L,
            0x4020100804020100L,
            0x8040201008040201L,
            0x0080402010080402L,
            0x0000804020100804L,
            0x0201000000000000L,
            0x0402010000000000L,
            0x0804020100000000L,
            0x1008040201000000L,
            0x2010080402010000L,
            0x4020100804020100L,
            0x8040201008040201L,
            0x0080402010080402L,
            0x0100000000000000L,
            0x0201000000000000L,
            0x0402010000000000L,
            0x0804020100000000L,
            0x1008040201000000L,
            0x2010080402010000L,
            0x4020100804020100L,
            0x8040201008040201L
    };

    public static final long[] ANTIDIAG_MASKS = {0x0000000000000001L,
            0x0000000000000102L,
            0x0000000000010204L,
            0x0000000001020408L,
            0x0000000102040810L,
            0x0000010204081020L,
            0x0001020408102040L,
            0x0102040810204080L,
            0x0000000000000102L,
            0x0000000000010204L,
            0x0000000001020408L,
            0x0000000102040810L,
            0x0000010204081020L,
            0x0001020408102040L,
            0x0102040810204080L,
            0x0204081020408000L,
            0x0000000000010204L,
            0x0000000001020408L,
            0x0000000102040810L,
            0x0000010204081020L,
            0x0001020408102040L,
            0x0102040810204080L,
            0x0204081020408000L,
            0x0408102040800000L,
            0x0000000001020408L,
            0x0000000102040810L,
            0x0000010204081020L,
            0x0001020408102040L,
            0x0102040810204080L,
            0x0204081020408000L,
            0x0408102040800000L,
            0x0810204080000000L,
            0x0000000102040810L,
            0x0000010204081020L,
            0x0001020408102040L,
            0x0102040810204080L,
            0x0204081020408000L,
            0x0408102040800000L,
            0x0810204080000000L,
            0x1020408000000000L,
            0x0000010204081020L,
            0x0001020408102040L,
            0x0102040810204080L,
            0x0204081020408000L,
            0x0408102040800000L,
            0x0810204080000000L,
            0x1020408000000000L,
            0x2040800000000000L,
            0x0001020408102040L,
            0x0102040810204080L,
            0x0204081020408000L,
            0x0408102040800000L,
            0x0810204080000000L,
            0x1020408000000000L,
            0x2040800000000000L,
            0x4080000000000000L,
            0x0102040810204080L,
            0x0204081020408000L,
            0x0408102040800000L,
            0x0810204080000000L,
            0x1020408000000000L,
            0x2040800000000000L,
            0x4080000000000000L,
            0x8000000000000000L};


}
