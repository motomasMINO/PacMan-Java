import java.awt.*;
import java.awt.event.*;
import java.util.HashSet;
import java.util.Random;

import javax.sound.sampled.Clip;
import javax.swing.*;

public class PacMan extends JPanel implements ActionListener, KeyListener {
    class Block {
        int x;
        int y;
        int width;
        int height;
        Image image;
        Image originalImage;

        int startX;
        int startY;
        char direction = 'U'; // U D L R(それぞれ上下左右)
        int velocityX = 0;
        int velocityY = 0;

        boolean isScared = false; // ゴーストがイジケ状態かどうか

        Block(Image image, int x, int y, int width, int height) {
            this.image = image;
            this.originalImage = image;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.startX = x;
            this.startY = y;
        }

        void updateDirection(char direction) {
            char prevDirection = this.direction;
            this.direction = direction;
            updateVelocity();
            this.x += this.velocityX;
            this.y += this.velocityY;
            for(Block wall : walls) {
                if(collision(this, wall)) {
                   this.x -= this.velocityX;
                   this.y -= this.velocityY;
                   this.direction = prevDirection;
                   updateVelocity();
                }
            }
        }

        void updateVelocity() {
            if(this.direction == 'U') {
                this.velocityX = 0;
                this.velocityY = -tileSize/4;
            }
            else if(this.direction == 'D') {
                this.velocityX = 0;
                this.velocityY = tileSize/4;
            }
            else if(this.direction == 'L') {
                this.velocityX = -tileSize/4;
                this.velocityY = 0;
            }
            else if(this.direction == 'R') {
                this.velocityX = tileSize/4;
                this.velocityY = 0;
            }
        }

        void reset() {
            this.x = this.startX;
            this.y = this.startY;
            this.isScared = false; // 通常状態に戻す
            this.image = this.originalImage; // 画像を元に戻す
        }
    }

    private int rowCount = 21;
    private int columnCount = 19;
    private int tileSize = 32;
    private int boardWidth = columnCount * tileSize;
    private int boardHeight = rowCount * tileSize;
    private int scaredGhostCount = 0; // イジケ状態のゴーストの数
    private int nextExtraLifeScore = 10000; // 次に残機を増やすスコア
    private int eatenFoodCount = 0; // 食べたエサの数
    private int cherryScoreX = -1;
    private int cherryScoreY = -1;

    private Image wallImg;
    private Image powerFoodsImg;
    private Image blueGhostImg;
    private Image orangeGhostImg;
    private Image pinkGhostImg;
    private Image redGhostImg;
    private Image scaredGhostImg;
    private Image cherryImg;

    private Image pacmanUpImg;
    private Image pacmanDownImg;
    private Image pacmanLeftImg;
    private Image pacmanRightImg;

    private Font font;
    private Clip Eat, Lose, gameStart, ghostMoving, ghostScaring, eatGhost, extraLife, eatFruit;

    private Block cherry = null;

    private Timer cherryTimer; // チェリーが消えるタイマー
    private Timer cherryScoreTimer;

    // X = 壁(wall)、 O = 空白、 P = パックマン、 ' ' = エサ(food)、 @ = パワーエサ(powerFood)
    // ゴースト: b = 青、 o = オレンジ、 p = ピンク、 r = 赤
    private String[] tileMap = {
        "XXXXXXXXXXXXXXXXXXX",
        "X        X        X",
        "X@XX XXX X XXX XX@X",
        "X                 X",
        "X XX X XXXXX X XX X",
        "X    X       X    X",
        "XXXX XXXX XXXX XXXX",
        "OOOX X       X XOOO",
        "XXXX X XXrXX X XXXX",
        "O      XbpoX      O",
        "XXXX X XXXXX X XXXX",
        "OOOX X       X XOOO",
        "XXXX X XXXXX X XXXX",
        "X        X        X",
        "X XX XXX X XXX XX X",
        "X@ X     P     X @X",
        "XX X X XXXXX X X XX",
        "X    X   X   X    X",
        "X XXXXXX X XXXXXX X",
        "X                 X",
        "XXXXXXXXXXXXXXXXXXX"
    };

    HashSet<Block> walls;
    HashSet<Block> foods;
    HashSet<Block> ghosts;
    HashSet<Block> powerFoods;
    
    Block pacman;

    Timer gameLoop;
    char[] directions = {'U', 'D', 'L', 'R'}; // 上下左右
    Random random = new Random();
    int score = 0;
    int lives = 3;
    int rounds = 1;
    boolean gameOver = false;
    boolean gameStarted = false; // ゲームが開始されているかどうかのフラグ
    Timer startDelayTimer;
    Timer ghostScaredTimer; // ゴーストのイジケ状態を管理するタイマー

    PacMan() {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setBackground(Color.BLACK);
        addKeyListener(this);
        setFocusable(true);

        // 画像読み込み
        wallImg = new ImageIcon(getClass().getResource("./wall.png")).getImage();
        powerFoodsImg = new ImageIcon(getClass().getResource("./powerFood.png")).getImage();
        blueGhostImg = new ImageIcon(getClass().getResource("./blueGhost.png")).getImage();
        orangeGhostImg = new ImageIcon(getClass().getResource("./orangeGhost.png")).getImage();
        pinkGhostImg = new ImageIcon(getClass().getResource("./pinkGhost.png")).getImage();
        redGhostImg = new ImageIcon(getClass().getResource("./redGhost.png")).getImage();
        scaredGhostImg = new ImageIcon(getClass().getResource("./scaredGhost.png")).getImage();
        cherryImg = new ImageIcon(getClass().getResource("./cherry.png")).getImage();

        pacmanUpImg = new ImageIcon(getClass().getResource("./pacmanUp.png")).getImage();
        pacmanDownImg = new ImageIcon(getClass().getResource("./pacmanDown.png")).getImage();
        pacmanLeftImg = new ImageIcon(getClass().getResource("./pacmanLeft.png")).getImage();
        pacmanRightImg = new ImageIcon(getClass().getResource("./pacmanRight.png")).getImage();

        // サウンド読み込み
        Eat = Loader.loadSound("./Pacman_Eat.wav");
        Lose = Loader.loadSound("./Pacman_Lose.wav");
        gameStart = Loader.loadSound("./gameStart.wav");
        ghostMoving = Loader.loadSound("./ghostMoving.wav");
        ghostScaring = Loader.loadSound("./ghostScaring.wav");
        eatGhost = Loader.loadSound("./eatGhost.wav");
        extraLife = Loader.loadSound("./Extend.wav");
        eatFruit = Loader.loadSound("./Eat-Fruit.wav");

        // フォント読み込み
        font = Loader.loadFont("./arcadeFont.ttf", 18);

        loadMap();
        for(Block ghost : ghosts) {
            char newDirection = directions[random.nextInt(4)];
            ghost.updateDirection(newDirection);
        }
        // タイマーの開始にかかる時間、フレーム間の経過時間(ミリ秒)
        gameLoop = new Timer(50, this); // 20fps(1000/50)

        // 5.5秒後にゲームを開始する遅延タイマー
        startDelayTimer = new Timer(5500, new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            gameStarted = true; // ゲームを開始
            gameLoop.start();   // 実際のゲームループを開始
            ghostMoving.loop(Clip.LOOP_CONTINUOUSLY);
            startDelayTimer.stop(); // この遅延タイマーを停止
        }
    });
    startDelayTimer.setRepeats(false); // 一度だけ実行するよう設定
    startDelayTimer.start(); // 遅延タイマーをスタート
    gameStart.setFramePosition(0);
    gameStart.start();

    // チェリーのタイマー(10秒後に消える)
    cherryTimer = new Timer(10000, new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            cherry = null; // チェリーを消す
            cherryTimer.stop();
        }
    });
    cherryTimer.setRepeats(false);
}
    

    public void loadMap() {
        walls = new HashSet<Block>();
        foods = new HashSet<Block>();
        powerFoods = new HashSet<Block>();
        ghosts = new HashSet<Block>();

        for(int r = 0; r < rowCount; r++) {
            for(int c = 0; c < columnCount; c++) {
                String row = tileMap[r];
                char tileMapChar = row.charAt(c);

                int x = c * tileSize;
                int y = r * tileSize;

                if(tileMapChar == 'X') { // 壁ブロック
                    Block wall = new Block(wallImg, x, y, tileSize, tileSize);
                    walls.add(wall);
                }
                else if(tileMapChar == 'b') { // 青ゴースト
                    Block ghost = new Block(blueGhostImg, x, y, tileSize, tileSize);
                    ghosts.add(ghost);
                }
                else if(tileMapChar == 'o') { // オレンジゴースト
                    Block ghost = new Block(orangeGhostImg, x, y, tileSize, tileSize);
                    ghosts.add(ghost);
                }
                else if(tileMapChar == 'p') { // ピンクゴースト
                    Block ghost = new Block(pinkGhostImg, x, y, tileSize, tileSize);
                    ghosts.add(ghost);
                }
                else if(tileMapChar == 'r') { // 赤ゴースト
                    Block ghost = new Block(redGhostImg, x, y, tileSize, tileSize);
                    ghosts.add(ghost);
                }
                else if(tileMapChar == 'P') { // パックマン
                    pacman = new Block(pacmanLeftImg, x, y, tileSize, tileSize);
                }
                else if(tileMapChar == ' ') { // エサ
                    Block food = new Block(null, x + 14, y + 14, 4, 4);
                    foods.add(food);
                }
                else if(tileMapChar == '@') { // パワーエサ
                    Block powerFood = new Block(powerFoodsImg, x + 10, y + 10, 12, 12);
                    powerFoods.add(powerFood);
                }
            }
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
        if (!gameStarted) {
            g.setColor(Color.YELLOW);
            g.setFont(font);
            g.drawString("READY!", boardWidth / 2 - 50, boardHeight / 2 + 40);
        }
    }

    public void draw(Graphics g) {
        g.drawImage(pacman.image, pacman.x, pacman.y, pacman.width, pacman.height, null); // パックマン

        for(Block ghost : ghosts) {
            g.drawImage(ghost.image, ghost.x, ghost.y, ghost.width, ghost.height, null); // ゴースト
        }

        for(Block wall : walls) {
            g.drawImage(wall.image, wall.x, wall.y, wall.width, wall.height, null); // 壁
        }

        g.setColor(Color.WHITE);
        for(Block food : foods) {
            g.fillRect(food.x, food.y, food.width, food.height); // エサ
        }

        for(Block powerFood : powerFoods) {
            g.drawImage(powerFood.image, powerFood.x, powerFood.y, powerFood.width, powerFood.height, null); // パワーエサ
        }

        if(cherry != null) {
            g.drawImage(cherry.image, cherry.x, cherry.y, cherry.width, cherry.height, null); // チェリー
        }

        // スコア、残機、ゲームオーバー表示
        g.setFont(font);
        if(gameOver) {
            g.setColor(Color.RED);
            g.drawString("GAME OVER: " + String.valueOf(score), tileSize/2, tileSize/2);
        }
        else {
            g.drawString("LIFE x " + String.valueOf(lives) + " SCORE: " + String.valueOf(score), tileSize/2, tileSize/2);
        }
        // ラウンド
        g.setFont(font);
        g.setColor(Color.WHITE);
        g.drawString("ROUND " + String.valueOf(rounds), 450, tileSize/2);

        // チェリーを食べた時のスコア表示
        if (cherryScoreX != -1 && cherryScoreY != -1) {
        g.setColor(Color.PINK);
        g.setFont(font);
        g.drawString("100", cherryScoreX, cherryScoreY);
        }

    }

    public void move() {
        if(!gameStarted) return; // ゲームが開始していなければ移動しない
        pacman.x += pacman.velocityX;
        pacman.y += pacman.velocityY;

        // 画面外に出た場合は反対方向へ移動させる
        if (pacman.x < 0) {
            pacman.x = boardWidth - pacman.width;
        } else if (pacman.x + pacman.width > boardWidth) {
            pacman.x = 0;
        }
    
        if (pacman.y < 0) {
            pacman.y = boardHeight - pacman.height;
        } else if (pacman.y + pacman.height > boardHeight) {
            pacman.y = 0;
        }

        // 壁の当たり判定を検出
        for(Block wall : walls) {
            if(collision(pacman, wall)) {
                pacman.x -= pacman.velocityX;
                pacman.y -= pacman.velocityY;
                break;
            }
        }

        // ゴーストの当たり判定を検出
        for(Block ghost : ghosts) {
            if(collision(ghost, pacman)) {
                if(ghost.isScared) { // イジケ状態なら食べられる
                    score += 200;
                    eatGhost.setFramePosition(0);
                    eatGhost.start();

                    // 残機増加のチェック
                if(score >= nextExtraLifeScore) {
                    lives += 1; // 残機を1増やす
                    nextExtraLifeScore += 10000; // 次の目標スコアを10000点増やす
                    extraLife.setFramePosition(0);
                    extraLife.start();
                }

                    ghost.reset();
                    scaredGhostCount--; // イジケ状態のカウンターを減らす
                    ghost.isScared = false;

                    // すべてのゴーストが通常状態に戻ったらBGMを切り替える
                    if(scaredGhostCount == 0) {
                       ghostScaring.stop();
                       ghostMoving.setFramePosition(0);
                       ghostMoving.loop(Clip.LOOP_CONTINUOUSLY);
                    }
                } else { // 通常時は触れるとパックマンがやられる
                lives -= 1;
                cherry = null;
                if(cherryTimer.isRunning()) {
                    cherryTimer.stop();
                }
                Lose.setFramePosition(0);
                Lose.start();
                ghostScaring.stop();
                ghostMoving.setFramePosition(0);
                ghostMoving.loop(Clip.LOOP_CONTINUOUSLY);
                if(lives == 0) { // 残機が0になるとゲームオーバー
                    gameOver = true;
                    return;
                }
                resetPositions();
              }
            }

            if(ghost.y == tileSize*9 && ghost.direction != 'U' && ghost.direction != 'D') {
                ghost.updateDirection('U');
            } // ゴーストは壁に当たる度にランダムな方向へ移動
            ghost.x += ghost.velocityX;
            ghost.y += ghost.velocityY;
            for(Block wall : walls) {
                if(collision(ghost, wall) || ghost.x <= 0 || ghost.x + ghost.width >= boardWidth) {
                    ghost.x -= ghost.velocityX;
                    ghost.y -= ghost.velocityY;
                    char newDirection = directions[random.nextInt(4)];
                    ghost.updateDirection(newDirection);
                }
            }
        }

        // エサの当たり判定を検出
        Block foodEaten = null;
        for(Block food : foods) {
            if(collision(pacman, food)) {
                foodEaten = food;
                score += 10;
                eatenFoodCount++; // エサを食べた回数をカウント
                Eat.setFramePosition(0);
                Eat.start();

                // チェリーの出現判定(70個目と170個目)
                if(eatenFoodCount == 70 || eatenFoodCount == 170) {
                    spawnCherry();
                }

                // 残機増加のチェック
                if(score >= nextExtraLifeScore) {
                    lives += 1; // 残機を1増やす
                    nextExtraLifeScore += 10000; // 次の目標スコアを10000点増やす
                    extraLife.setFramePosition(0);
                    extraLife.start();
                }

            }
        }
        if(foodEaten != null) {
            foods.remove(foodEaten);
            repaint(); // 画面を更新
        }

        // チェリーの当たり判定を検出
        if(cherry != null && collision(pacman, cherry)) {
            score += 100; // チェリーを食べると100点

            // スコア表示用の座標を保存
            cherryScoreX = boardWidth / 2 - 20;
            cherryScoreY = boardHeight / 2 + 40;

            // 1.5秒後にスコア表示を消す
            if (cherryScoreTimer != null && cherryScoreTimer.isRunning()) {
                cherryScoreTimer.stop();
            }
            cherryScoreTimer = new Timer(1500, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    cherryScoreX = -1;
                    cherryScoreY = -1;
                    repaint();
                    cherryScoreTimer.stop();
                }
            });
            cherryScoreTimer.setRepeats(false);
            cherryScoreTimer.start();

            cherry = null; // チェリーを消す
            if(cherryTimer.isRunning()) {
                cherryTimer.stop();
            } 
            eatFruit.setFramePosition(0);
            eatFruit.start();
            repaint(); // 画面を更新
        }

        // パワーエサの当たり判定を検出
        Block powerFoodEaten = null;
        for(Block powerFood : powerFoods) {
            if(collision(pacman, powerFood)) {
                powerFoodEaten = powerFood;
                score += 50;
                Eat.setFramePosition(0);
                Eat.start();

                // 残機増加のチェック
                if(score >= nextExtraLifeScore) {
                    lives += 1; // 残機を1増やす
                    nextExtraLifeScore += 10000; // 次の目標スコアを10000点増やす
                    extraLife.setFramePosition(0);
                    extraLife.start();
                }

                // すべてのゴーストをイジケ状態にする
                scaredGhostCount = ghosts.size(); // ゴーストの数をセット
                for(Block ghost : ghosts) {
                    ghost.isScared = true;
                    ghost.image = scaredGhostImg; // イジケ状態の画像
                    ghostMoving.stop();
                    ghostScaring.setFramePosition(0);
                    ghostScaring.loop(Clip.LOOP_CONTINUOUSLY);
                }

                // 既存のタイマーがあれば停止する
                if(ghostScaredTimer != null && ghostScaredTimer.isRunning()) {
                    ghostScaredTimer.stop();
                }

                // 新しいタイマーを設定(イジケ状態は10秒間持続)
                ghostScaredTimer = new Timer(10000, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        for(Block ghost : ghosts) {
                            ghost.isScared = false;
                            ghost.image = ghost.originalImage; // 通常の画像に戻す
                            scaredGhostCount--; // カウンターを減らす
                        }
                        ghostScaredTimer.stop(); // タイマーを停止
                        ghostScaring.stop();
                        ghostMoving.setFramePosition(0);
                        ghostMoving.loop(Clip.LOOP_CONTINUOUSLY);
                    }
                });
                ghostScaredTimer.setRepeats(false); // 一度だけ実行
                ghostScaredTimer.start();
            }
        }
        if(powerFoodEaten != null) {
            powerFoods.remove(powerFoodEaten);
            repaint(); // 画面更新
        }

        if(foods.isEmpty()) { // エサをすべて食べるとゴースト、パックマン、エサの量がリセットされる
            cherry = null;
            loadMap();
            resetPositions();
            rounds += 1;
            eatenFoodCount = 0;
            ghostScaring.stop();
            ghostMoving.setFramePosition(0);
            ghostMoving.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    private void spawnCherry() {
        // ステージ中央にチェリーを配置
        int cherryX = boardWidth / 2 - tileSize /2;
        int cherryY = boardHeight / 2 - tileSize / 2 + 30;
        cherry = new Block(cherryImg, cherryX, cherryY, tileSize, tileSize);

        // チェリーのスコア表示位置を初期化
        cherryScoreX = -5;
        cherryScoreY = -5;

        // 10秒後にチェリーを消すタイマーを開始
        cherryTimer.start();
    }

    public boolean collision(Block a, Block b) {
        return a.x < b.x + b.width && a.x + a.width > b.x && 
               a.y < b.y + b.height && a.y + a.height > b.y;
    }

    public void resetPositions() {
        pacman.reset();
        pacman.velocityX = 0;
        pacman.velocityY = 0;
        for(Block ghost : ghosts) {
            ghost.reset();
            char newDirection = directions[random.nextInt(4)];
            ghost.updateDirection(newDirection);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        move();
        repaint();
        if(gameOver) {
            gameLoop.stop();
            ghostMoving.stop();
            ghostScaring.stop();
            cherryTimer.stop();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {
        if(gameOver) { // いずれかのキーを押すと再スタート
            loadMap();
            resetPositions();
            eatenFoodCount = 0;
            lives = 3;
            score = 0;
            rounds = 1;
            gameOver = false;

        // ゲーム開始フラグをリセットして "READY!" を表示する準備
        gameStarted = false;

        // 画面の更新
        repaint();

        if(ghostMoving.isRunning()) {
            ghostMoving.stop();
        }

        // 既存の遅延タイマーをそのまま再利用
        startDelayTimer.start();

        // ゲームスタートサウンドを再生
        gameStart.setFramePosition(0);
        gameStart.start();
        }
        // パックマンの操作(キー割り当て)
        if(e.getKeyCode() == KeyEvent.VK_UP) { // 上
            pacman.updateDirection('U');
        }
        else if(e.getKeyCode() == KeyEvent.VK_DOWN) { // 下
            pacman.updateDirection('D');
        }
        else if(e.getKeyCode() == KeyEvent.VK_LEFT) { // 左
            pacman.updateDirection('L');
        }
        else if(e.getKeyCode() == KeyEvent.VK_RIGHT) { // 右
            pacman.updateDirection('R');
        }
        // パックマンのそれぞれの方向の画像読み込み
        if(pacman.direction == 'U') {
            pacman.image = pacmanUpImg;
        }
        else if(pacman.direction == 'D') {
            pacman.image = pacmanDownImg;
        }
        else if(pacman.direction == 'L') {
            pacman.image = pacmanLeftImg;
        }
        else if(pacman.direction == 'R') {
            pacman.image = pacmanRightImg;
        }
    }
}