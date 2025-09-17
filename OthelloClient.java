import java.net.*;
import java.io.*;
import java.util.*;

class OthelloClient {
    static final int SIZE = 8;
    static final int EMPTY = 0;
    static final int BLACK = 1;
    static final int WHITE = -1;
    static long TIME_LIMIT_MS;

    static int[][] board = new int[SIZE][SIZE];
    static int myColor;
    public static void main(String args[]) {
        Socket s;
        InputStream sIn;
        OutputStream sOut;
        BufferedReader br;
        PrintWriter pw;
        String str;
        StringTokenizer stn;

        if (args.length != 3) {
            System.out.println("No hostname given");
            System.exit(1);
        }

        int timeInSeconds = Integer.parseInt(args[2]);
        TIME_LIMIT_MS = timeInSeconds * 1000L;

        try {
            s = new Socket(args[0], Integer.parseInt(args[1]));  // ポート番号を標準入力で受け取る

            sIn = s.getInputStream();
            sOut = s.getOutputStream();
            br = new BufferedReader(new InputStreamReader(sIn));
            pw = new PrintWriter(new OutputStreamWriter(sOut), true);


            // ユーザー名を登録
            pw.println("NICK 6323113");
            str = br.readLine();
            System.out.println(str);

            stn = new StringTokenizer(str," ",false);
            String mes = stn.nextToken();
            myColor = Integer.parseInt(stn.nextToken());

            System.out.println("Message = " +mes);
            System.out.println("My Color = " +myColor);


            // サーバーからのメッセージを処理
            while ((str = br.readLine()) != null){
                System.out.println("Recv message = " +str);

                if (str.startsWith("BOARD")) {
                    // BOARD メッセージ
                    StringTokenizer boardTokens = new StringTokenizer(str.substring(6));
                    for (int i = 0; i < 64; i++) {
                        board[i / 8][i % 8] = Integer.parseInt(boardTokens.nextToken());
                    }
                }

                else if (str.startsWith("START")) {
                    // START メッセージ
                    myColor = Integer.parseInt(str.split(" ")[1]);
                    System.out.println("START " + myColor);
                }

                else if (str.startsWith("END")) {
                    // END メッセージ
                    System.out.println("=== Game End ===");

                    try {
                        String[] splitByPipe = str.split("\\|");
                        String resultPart = splitByPipe[0].trim();
                        String scorePart = splitByPipe[1].trim();

                        // 勝敗出力
                        String result = resultPart.replaceFirst("END", "").trim();
                        System.out.println("Result: " + result);

                        // スコア出力
                        String[] scores = scorePart.split(" ");
                        if (scores.length == 2) {
                            String[] black = scores[0].split(":");
                            String[] white = scores[1].split(":");
                            if (black.length == 2 && white.length == 2) {
                                System.out.println("Black: " + black[1]);
                                System.out.println("White: " + white[1]);
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Failed to parse END message.");
                    }
                    break;
                }

                else if (str.equals("CLOSE")) {
                    // CLOSE メッセージ
                    System.out.println("Opponent disconnected.");
                    break;
                }

                else if (str.startsWith("TURN " +myColor)){
                    // TURN メッセージ
                    int[] move = monteCarloSearch(myColor);
                    if (move != null) {
                        pw.println("PUT " + move[0] + " " + move[1]);
                        pw.flush();
                        System.out.println("PUT" + move[0] + " " + move[1]);
                    } else {
                        System.out.println("No valid moves.");
                    }
                } 
            }

            br.close();
            pw.close();
            s.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // モンテカルロ法で次の手を決定
    static int[] monteCarloSearch(int color) {
        List<int[]> legalMoves = getLegalMoves(board, color);
        if (legalMoves.isEmpty()) return null;

        // 四隅のマスを取れるなら採用
        for (int[] move : legalMoves) {
            if ((move[0] == 0 && move[1] == 0) || (move[0] == 0 && move[1] == 7) ||
                (move[0] == 7 && move[1] == 0) || (move[0] == 7 && move[1] == 7)) {
                    return move;
                }
        }

        int[] bestMove = null;
        double bestWins = -1;
        long endTime = System.currentTimeMillis() + TIME_LIMIT_MS;

        for (int[] move : legalMoves) {
            int x = move[0], y = move[1];
            boolean isDanger = isCornerDanger(x, y);
            double score = 0;

            while (System.currentTimeMillis() < endTime) {
                int[][] copy = copyBoard(board);
                applyMove(copy, move[0], move[1], color);
                score += simulate(copy, -color);
            }
            if (isDanger) score *= 0.5;  // ペナルティ

            if (score > bestWins) {
                bestWins = score;
                bestMove = move;
            }
        }
        return bestMove;
    }

    // 四隅を囲うマスを極力取らないようにする
    static boolean isCornerDanger(int x, int y) {
        int[][] dangerZones = {
            {0,1}, {1,0}, {1,1},
            {0,6}, {1,6}, {1,7},
            {6,0}, {6,1}, {7,1},
            {6,6}, {6,7}, {7,6}
        };
        for (int[] pos : dangerZones) {
            if (x == pos[0] && y == pos[1]) return true;
        }
        return false;
    }

    // ランダムプレイアウトで勝敗シミュレーション
    static int simulate(int[][] simBoard, int turnColor) {
        Random rand = new Random();
        int pass = 0;
        while (pass < 2) {
            List<int[]> moves = getLegalMoves(simBoard, turnColor);
            if (!moves.isEmpty()) {
                int[] move = moves.get(rand.nextInt(moves.size()));
                applyMove(simBoard, move[0], move[1], turnColor);                    
                pass = 0;
            } else {
                pass++;
            }
            turnColor = -turnColor;
        }
        return countDiscs(simBoard, myColor) > countDiscs(simBoard, -myColor) ? 1 : 0;
    }

    // 指定した色の石の数を数える
    static int countDiscs(int[][] b, int color) {
        int count = 0;
        for (int[] row : b)
            for (int cell : row)
                if (cell == color) count++;
        return count;
    }

    // ボードのコピーを作成
    static int[][] copyBoard(int[][] b) {
        int[][] copy = new int[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++)
            copy[i] = Arrays.copyOf(b[i], SIZE);
        return copy;
    }

    // 指定した色で合法手をすべて取得
    static List<int[]> getLegalMoves(int[][] b, int color) {
        List<int[]> moves = new ArrayList<>();
        for (int x = 0; x < SIZE; x++)
            for (int y = 0; y < SIZE; y++)
                if (b[x][y] == EMPTY && isValidMove(b, x, y, color))
                    moves.add(new int[]{x, y});
        return moves;
    }

    // 指定位置が合法手か判定
    static boolean isValidMove(int[][] b, int x, int y, int color) {
        int[] dx = {-1,-1,-1,0,1,1,1,0};
        int[] dy = {-1,0,1,1,1,0,-1,-1};
        for (int d = 0; d < 8; d++) {                
            int nx = x + dx[d], ny = y + dy[d];
            boolean hasOpposite = false;
            while (0 <= nx && nx < SIZE && 0 <= ny && ny < SIZE && b[nx][ny] == -color) {
                hasOpposite = true;
                nx += dx[d];
                ny += dy[d];
            }
            if (hasOpposite && 0 <= nx && nx < SIZE && 0 <= ny && ny < SIZE && b[nx][ny] == color)
                return true;
        }
        return false;
    }

    // 指定した位置に石を置き裏返す
    static void applyMove(int[][] b, int x, int y, int color) {
        b[x][y] = color;
        int[] dx = {-1,-1,-1,0,1,1,1,0};
        int[] dy = {-1,0,1,1,1,0,-1,-1};
        for (int d = 0; d < 8; d++) {
            int nx = x + dx[d], ny = y + dy[d];
            List<int[]> toFlip = new ArrayList<>();
            while (0 <= nx && nx < SIZE && 0 <= ny && ny < SIZE && b[nx][ny] == -color) {
                toFlip.add(new int[]{nx, ny});
                nx += dx[d];
                ny += dy[d];
            }
            if (0 <= nx && nx < SIZE && 0 <= ny && ny < SIZE && b[nx][ny] == color) {
                for (int[] p : toFlip)
                    b[p[0]][p[1]] = color;
            }
        }
    }
}
