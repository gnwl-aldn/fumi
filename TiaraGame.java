import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Group;
import javafx.scene.paint.Color;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

import javafx.animation.AnimationTimer;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.scene.text.Font;

import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;

import java.util.ArrayList;
import java.util.List;



public class TiaraGame extends Application {
  private int initialSceneWidth = 400;
  private int initialSceneHeight = 400;

  private MapAndChars mapAndChars;
  private double passedTime;
  private AnimationTimer timer;

  @Override
  public void start(Stage st) throws Exception {
    Group root = new Group();
    
    Scene scene = new Scene(root, initialSceneWidth, initialSceneHeight, Color.BLACK);
    st.setTitle("Tiara Game");
    st.setScene(scene);
    st.show();

    scene.setOnKeyPressed(this::keyPressed);
    
    mapAndChars = new MapAndChars(root);
    
    mapAndChars.drawScreen(30, 0.0);
    
    // 再描画（1/60秒）時間ごとの処理を定義
    AnimationTimer timer = new AnimationTimer() {
      long startTime = 0;
      @Override
      public void handle(long t) { // tは現在時刻（ナノ秒）を表す（時間に応じた処理を定義するなら使う）
        if (startTime == 0) {
          startTime = t;
        }

        mapAndChars.drawScreen(30 - (int) ((t - startTime) / 1000000000), passedTime);
	mapAndChars.animationTiara();
	passedTime = (t - startTime) / (double)1000000000;
	// ゲームクリア判定
	if (mapAndChars.numI >= 5 && !mapAndChars.gameover) {
	    mapAndChars.displayClearMessage();
	    stop();
	}
      }
    };

    timer.start();
  }

  public static void main(String[] a) {
    launch(a);
  }

  public void keyPressed(KeyEvent e) {
    KeyCode key = e.getCode();
    int dir = -1;
    switch ( key ) {
      case LEFT: dir = 2; break;// left
      case RIGHT: dir = 0; break;// right
      case UP: dir = 1; break;// up
      case DOWN: dir = 3; break;// down
    }
    if ( dir >= 0 ) mapAndChars.girlMove(dir);
  }
}


class MapAndChars {
  // 二次元マップの準備
  private char[][] map;

  // アイテムの位置のリスト
   List<Integer> itemX = new ArrayList<Integer>();
   List<Integer> itemY = new ArrayList<Integer>();

  // 収集済みの座標を記録するリスト
    List<Integer> passedX = new ArrayList<Integer>();
    List<Integer> passedY = new ArrayList<Integer>();
   
  private int MX = 6, MY = 6;
    private String[] initialMap = {"G B  I",
                                   "B   BB",
                                   "B B BI",
                                   "I B   ",
                                   " BB BB",
                                   " IB  I"};

    // 獲得したアイテムの数をカウントするための変数
    public int numI = 0;

    // 配置用（女の子）
    private int girlX, girlY;
    private Image girlImage01, girlImage02;
    private ImageView girlView;
    private double girlSize = 120;
    private Line erase1, erase2;
    private Rectangle bar1, bar2;

    // 配置用（ティアラ）
    private Image tiaraImage, tiaraImage01, tiaraImage02;
    private ImageView tiaraView;
    private int tiaraMotion;

    // time
    public boolean gameover = false;
    private Text timeText;

    // numTextI（獲得したアイテムの数の表示）
    private Text numTextI;

    // ProgressBarの進捗位置
    private Image heartImage;
    private ImageView heartView;

    private Group root;

    // コンストラクタ
    MapAndChars(Group root) {
	this.root = root;
	// マップの周囲を見えない壁で囲む
	map = new char[MY+2][MX+2];
	for (int x = 0; x <= MX+1; x++) {
	    map[0][x] = 'B';
	    map[MY+1][x] = 'B';
	}
	for (int y = 0; y <= MY+1; y++) {
	    map[y][0] = 'B';
	    map[y][MX+1] = 'B';
	}
	// マップデータの読み込み
	for (int y = 1; y <= MY; y++) {
	    for (int x = 1; x <= MX; x++) {
		map[y][x] = initialMap[y-1].charAt(x-1);
	    }
	}

	drawInitialMapAndChars();
    }

    public void drawInitialMapAndChars() {
	timeText = new Text(55, 50, "Time: 30");
	timeText.setFont(new Font("TimeRoman", 18));
	timeText.setFill(Color.WHITE);
	root.getChildren().add(timeText);

	numTextI = new Text(250, 50, "ティアラを集めよう！");
	numTextI.setFont(new Font("Meiryo UI Bold", 18));
	numTextI.setFill(Color.WHITE);
	root.getChildren().add(numTextI);

	// ProgressBarの表示
	bar1 = new Rectangle(40, 350, 300, 5);
	bar1.setFill(Color.GRAY);
	root.getChildren().add(bar1);

	heartImage = new Image("heart.png");
	heartView = new ImageView(heartImage);
	heartView.setFitWidth(40);
	heartView.setFitHeight(30);
	root.getChildren().add(heartView);

	erase1 = new Line(0, 0, 0, 0);
	erase2 = new Line(0, 0, 0, 0);
	erase1.setStroke(Color.CYAN);
	erase2.setStroke(Color.CYAN);
	root.getChildren().add(erase1);
	root.getChildren().add(erase2);

	tiaraImage = new Image("tiara.png");
	tiaraImage01 = new Image("tiara01.png");
	tiaraImage02 = new Image("tiara02.png");
	tiaraView = new ImageView(tiaraImage);
	tiaraView.setX(175);
	tiaraView.setY(10);
	tiaraView.setFitWidth(50);
	tiaraView.setFitHeight(50);
	root.getChildren().add(tiaraView);

	for (int y = 1; y <= MY; y++) {
	    for (int x = 1; x <= MX; x++) {
		int xx = 40*x+20, yy = 40*y+20;
		switch ( map[y][x] ) {

		    // アイテムの描画
		case 'I':
		    break; // ティアラは drawItem で描画
		    // ブロックの描画
		    case 'B': Block b = new Block(xx, yy);
			      root.getChildren().add(b.getParts());
			      break;
		    // 女の子の描画
		case 'G': if (girlView == null) {
		          girlImage01 = new Image("girl1.png");
		          girlImage02 = new Image("girl2.png");
			  girlView = new ImageView( girlImage01 );
			  girlView.setFitWidth(120);
			  girlView.setFitHeight(60);
			  root.getChildren().add(girlView);
			      }
			  girlX = x;
			  girlY = y;
			  girlView.toFront();
			  drawGirl();
			  break;
		}
	    }
	}
    }


    public void drawScreen(int t, double passedTime) {
	if ( girlSize < -5 ) return;
	drawItem();
	animationTiara();
	drawGirl();
	displayTime(t);
	displayNumI();
	displayProgressBar(passedTime);
    }

    public void drawItem() {
	// 描画されたティアラを一度全て削除
	root.getChildren().removeIf(node -> node instanceof ImageView && node != girlView && node != heartView && node != tiaraView);

	// マップ全体をスキャンしてティアラを描画
	for (int y = 1; y <= MY; y++) {
	    for (int x = 1; x <=MX; x++) {
		if (map[y][x] == 'I') {
		    boolean collected = false;

		    // 収集済みのティアラかどうか確認
		    for (int i = 0; i < itemX.size(); i++) {
			if (itemX.get(i) == x && itemY.get(i) == y) {
			    collected = true;
			    break;
			}
		    }

		    // 収集されていないティアラのみ描画
		    if (!collected) {
			ImageView newItemView = new ImageView(new Image("tiara.png"));
			newItemView.setFitWidth(40);
			newItemView.setFitHeight(50);
			newItemView.setX(40*x+20);
			newItemView.setY(40*y+20);
			root.getChildren().add(newItemView);
		    }
		}
	    }
	}
    }

   
    
    public void drawGirl() {
	if ( gameover ) girlSize -= 0.25;
	switch ((int) girlSize) {
	    case 39:
	girlView.setX(40*girlX+31);
	girlView.setY(40*girlY+20);
	girlView.toFront();
	break;
	case 0:
	    girlView.setImage(null);
	case -1:
	case -2:
	case -3:
	case -4:
	case -5:
	    int xx = 40*girlX+31+10, yy = 40*girlY+20+20;
	    erase1.setStartX(xx+girlSize);
	    erase1.setStartY(yy+girlSize);
	    erase1.setEndY(yy-girlSize);
            erase2.setStartX(xx-girlSize);
            erase2.setStartY(yy+girlSize);
            erase2.setEndX(xx+girlSize);
            erase2.setEndY(yy-girlSize);
	    break;
	default:
	    girlView.toFront();
	    girlView.setFitHeight(girlSize/2);
	    girlView.setFitWidth(girlSize);
            girlView.setX(40*girlX+31+(10-girlSize/2));
            girlView.setY(40*girlY+20+(20-girlSize/4));
	}
    }

    public void girlMove(int dir) {
	int dx = 0, dy = 0;
	switch ( dir ) {
        case 0: dx =  1; break; // right
        case 1: dy = -1; break; // up
        case 2: dx = -1; break; // left
        case 3: dy =  1; break; // down
	}
        if ( dx == 0 && dy == 0 ) return;
        if ( map[girlY+dy][girlX+dx] == 'B' ) return; // block
        girlX += dx; girlY += dy;

	// 収集済みの座標を確認
	if (!alreadyPassed(girlX, girlY)) {
	    passedX.add(girlX);
	    passedY.add(girlY);

	if ( map [girlY][girlX] == 'I' ) {
	    itemY.add(girlY);
	    itemX.add(girlX);
	    numI++;
	}
	}
    }

    // 収集済みの座標か判定
    private boolean alreadyPassed(int x, int y) {
	for (int i = 0; i < passedX.size(); i++) {
	    if (passedX.get(i) == x && passedY.get(i) == y) {
		return true;
	    }
	}
	return false;
    }

    public void animationTiara() {
	tiaraMotion = (tiaraMotion + 1) % 30;

	if (tiaraMotion < 10) {
	    tiaraView.setImage(tiaraImage);
	} else if (tiaraMotion < 20) {
	    tiaraView.setImage(tiaraImage01);
	} else {
	    tiaraView.setImage(tiaraImage02);
	}
    }

    public void displayTime(int t) {
	if ( gameover && numI < 5) {
	    timeText.setFill(Color.RED);
	    timeText.setText("Game Over");
	} else if (numI <5) {
	    timeText.setFill(Color.WHITE);
	    timeText.setText("Time: " + t);
	    if (t == 0) gameover = true;
	}
    }

    public void displayNumI() {
	numTextI.setFill(Color.ORANGE);
	numTextI.setText("Get Items: " + numI);
    }

    public void displayClearMessage() {
	Text clearText = new Text(110, 200, "Game Clear!");
	clearText.setFont(new Font("Meiryo UI Bold", 30));
	clearText.setFill(Color.WHITE);
	root.getChildren().add(clearText);

	// 女の子の画像を変更
	girlView.setImage(new Image("girl2.png"));
	girlSize += 20;
	girlView.setFitHeight(girlSize);
	girlView.setFitWidth(girlSize/2);
    }

    public void displayProgressBar(double passedTime) {
	double rateTime = passedTime / 30;
	if(rateTime <= 1){
	    heartView.setX((int)(300 * rateTime + 20));
	}
	heartView.setY(340);
	heartView.toFront();
    }
}

abstract class Tile {
  protected Group parts;

  Tile(int x, int y) {
    parts = new Group();
    parts.setTranslateX(x);
    parts.setTranslateY(y);
    construct();
  }
  
  //public void setX(double x) {parts.setTranslateX(x)};
  //public void setY(double y) {parts.setTranslateY(y)};
  //public double getX() {return parts.getTranslateX()};
  //public double getY() {return parts.getTranslateY()};
  public Group getParts() {return parts;};

  abstract public void construct(); // 抽象メソッド
  
}


class Block extends Tile {
    
    Block(int x, int y) {
	super(x, y);
    }

    @Override
    public void construct() { // 抽象メソッドの実装（継承したクラスで実装しなければならない）
	Color c = Color.LIGHTGRAY;

	Rectangle r1 = new Rectangle(0, 0, 26, 10);
	r1.setFill(c);
	Rectangle r2 = new Rectangle(32, 0, 8, 10);
	r2.setFill(c);
	Rectangle r3 = new Rectangle(0, 15, 10, 10);
	r3.setFill(c);
	Rectangle r4 = new Rectangle(16, 15, 24, 10);
	r4.setFill(c);
	Rectangle r5 = new Rectangle(0, 30, 18, 10);
	r5.setFill(c);
	Rectangle r6 = new Rectangle(24, 30, 16, 10);
	r6.setFill(c);
	parts.getChildren().add(r1);
	parts.getChildren().add(r2);
	parts.getChildren().add(r3);
	parts.getChildren().add(r4);
	parts.getChildren().add(r5);
	parts.getChildren().add(r6);
    }
}
