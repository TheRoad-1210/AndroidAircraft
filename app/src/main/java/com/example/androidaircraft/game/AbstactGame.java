package com.example.androidaircraft.game;

import static com.example.androidaircraft.factory.PropFactory.prop;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import com.example.androidaircraft.activity.RankListActivity;
import com.example.androidaircraft.activity.RegisterActivity;
import com.example.androidaircraft.activity.MainActivity;
import com.example.androidaircraft.aircraft.AbstractAircraft;
import com.example.androidaircraft.aircraft.BossEnemy;
import com.example.androidaircraft.aircraft.EliteEnemy;
import com.example.androidaircraft.aircraft.HeroAircraft;
import com.example.androidaircraft.application.ImageManager;
import com.example.androidaircraft.application.MusicService;
import com.example.androidaircraft.basic.AbstractFlyingObject;
import com.example.androidaircraft.basic.HeroController;
import com.example.androidaircraft.bullet.AbstractBullet;
import com.example.androidaircraft.factory.EnemyFactory;
import com.example.androidaircraft.player.Player;
import com.example.androidaircraft.props.AbstractProp;
import com.example.androidaircraft.props.BombSupply;
import com.example.androidaircraft.props.GoldCoin;
import com.example.androidaircraft.props.SilverCoin;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public abstract class AbstactGame extends SurfaceView implements SurfaceHolder.Callback ,Runnable{

    private Canvas canvas;
    private SurfaceHolder surfaceHolder;
    private final Paint paint = new Paint();
    public int gameMode;
    private MainActivity context;
    private final Intent intent;


    private int backGroundTop = 0;

    public void setNeedMusic(boolean needMusic) {
        this.needMusic = needMusic;
    }

    public boolean isNeedMusic() {
        return needMusic;
    }

    private boolean needMusic = false;
    /**
     * Scheduled ??????????????????????????????
     */
    private final ScheduledExecutorService executorService;

    /**
     * ????????????(ms)?????????????????????
     */
    private final int timeInterval = 10;

    private final HeroAircraft heroAircraft;


    protected final List<AbstractAircraft> enemyAircrafts;
    private final List<AbstractBullet> heroBullets;
    protected final List<AbstractBullet> enemyBullets;
    private final List<AbstractProp> abstractProp;
    public int bossScoreThreshold = 300;

    public Player getPlayer() {
        return player;
    }

    private Player player = Player.getInstance();

    protected int enemyMaxNumber = 5;

    protected boolean bossExist = false;
    protected boolean bossFlag = false;


    private int score = 0;
    private int time = 0;
    public int scorer = 0;
    protected EnemyFactory enemyFactory = new EnemyFactory();
    /**
     * ?????????ms)
     * ?????????????????????????????????????????????
     */
    private final int cycleDuration = 100;
    private int cycleTime = 0;

    public AbstactGame(MainActivity context) {

        super(context);
        this.context = context;
        this.intent = new Intent(context,MusicService.class);
        heroAircraft = HeroAircraft.getInstance();
        enemyAircrafts = new LinkedList<>();
        heroBullets = new LinkedList<>();
        enemyBullets = new LinkedList<>();
        abstractProp = new LinkedList<>();


        /*
          Scheduled ????????????????????????????????????
          ??????alibaba code guide??????????????? ThreadFactory ????????????????????????
          apache ??????????????? org.apache.commons.lang3.concurrent.BasicThreadFactory
         */
        ThreadFactory gameThread = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread t = new Thread(runnable);
                t.setName("game thread");
                return t;
            }
        };
        this.executorService = new ScheduledThreadPoolExecutor(1,gameThread);
        //??????????????????
        this.setOnTouchListener(new HeroController(this,heroAircraft));

        surfaceHolder = this.getHolder();
        surfaceHolder.addCallback(this);
        this.setFocusable(true);

    }

    /**
     * ???????????????????????????????????????
     */
    public void run() {
        if(needMusic){
            intent.putExtra("action","bgm");
            context.startService(intent);
        }


        // ???????????????????????????????????????????????????????????????????????????
        Runnable task = () -> {

            time += timeInterval;



            // ?????????????????????????????????
            if (timeCountAndNewCycleJudge()) {

                System.out.println(time);
                //Boss??????
                bossTime();

                // ???????????????
                if (enemyAircrafts.size() < enemyMaxNumber) {
                    enemyAircrafts.add(enemyFactory.create());
                }
                // ??????????????????
                shootAction();
            }

            //????????????
            grow();

            // ????????????
            bulletsMoveAction();

            // ????????????
            aircraftsMoveAction();

            //????????????
            propMoveAction();

            // ????????????
            crashCheckAction();

            // ?????????
            postProcessAction();

            //????????????????????????
            draw();


            //??????????????????
            if(needMusic){
                music();
            }
            // ??????????????????
            if (heroAircraft.getHp() <= 0) {

                //??????player
                createPlayer();

                System.out.println("game over");
                intent.putExtra("action","over");
               if(needMusic){context.startService(intent);}
                Intent intent = new Intent(context, RankListActivity.class);

                context.startActivity(intent);



                // ????????????
                executorService.shutdown();

            }
        };

        /*
          ?????????????????????????????????
          ??????????????????????????????????????????????????????????????????????????????????????????
         */
        executorService.scheduleWithFixedDelay(task, timeInterval, timeInterval, TimeUnit.MILLISECONDS);
    }

    //***********************
    //      Action ?????????
    //***********************




    private boolean timeCountAndNewCycleJudge() {
        cycleTime += timeInterval;
        if (cycleTime >= cycleDuration && cycleTime - timeInterval < cycleTime) {
            // ?????????????????????
            cycleTime %= cycleDuration;
            return true;
        } else {
            return false;
        }
    }

    private void shootAction() {
        //  ????????????
        for(AbstractAircraft e:enemyAircrafts){
            enemyBullets.addAll(e.shoot());
        }


        // ????????????
        heroBullets.addAll(heroAircraft.shoot());
    }

    private void bulletsMoveAction() {
        for (AbstractBullet bullet : heroBullets) {
            bullet.forward();
        }
        for (AbstractBullet bullet : enemyBullets) {
            bullet.forward();
        }
    }

    private void aircraftsMoveAction() {
        for (AbstractAircraft enemyAircraft : enemyAircrafts) {
            enemyAircraft.forward();
        }
    }

    private void propMoveAction(){
        for (AbstractProp prop : abstractProp) {
            Log.i("silver","move");

            prop.forward();
        }
    }


    /**
     * ???????????????
     * 1. ??????????????????
     * 2. ????????????/????????????
     * 3. ??????????????????
     */
    private void crashCheckAction() {
        //  ????????????????????????
        for (AbstractBullet enemybullet : enemyBullets) {
            if (heroAircraft.crash(enemybullet)) {
                heroAircraft.decreaseHp(enemybullet.getPower());
                enemybullet.vanish();
                if(needMusic){
                    intent.putExtra("action","bullet");
                    context.startService(intent);
                }
            }
        }
        // ????????????????????????
        for (AbstractBullet bullet : heroBullets) {
            if (bullet.notValid()) {
                continue;
            }
            for (AbstractAircraft enemyAircraft : enemyAircrafts) {
                if (enemyAircraft.notValid()) {
                    // ????????????????????????????????????????????????
                    // ???????????????????????????????????????????????????
                    continue;
                }
                if (enemyAircraft.crash(bullet)) {
                    // ??????????????????????????????
                    // ???????????????????????????
                    enemyAircraft.decreaseHp(bullet.getPower());
                    if(needMusic){
                        intent.putExtra("action","bullet");
                        context.startService(intent);
                    }
                    bullet.vanish();
                    if (enemyAircraft.notValid()) {
                        //  ?????????????????????????????????
                        score += 10;
                        scorer += 10;
                        if (enemyAircraft instanceof EliteEnemy) {
                            score += 10;
                            scorer += 10;
                            // ????????????????????????
                            if (Math.random() >= 0.2) {
                                abstractProp.add(prop(enemyAircraft));
                                Log.i("game","add");
                            }
                        }
                        if (enemyAircraft instanceof BossEnemy) {
                            score += 40;
                            scorer += 40;
                            abstractProp.add(prop(enemyAircraft));
                            abstractProp.add(prop(enemyAircraft));
                            abstractProp.add(prop(enemyAircraft));
                            abstractProp.add(prop(enemyAircraft));
                        }
                    }
                }

                // ????????? ??? ?????? ??????????????????
                if (enemyAircraft.crash(heroAircraft) || heroAircraft.crash(enemyAircraft)) {
                    enemyAircraft.vanish();
                    heroAircraft.decreaseHp(Integer.MAX_VALUE);
                }
            }

        }


        // ?????????????????????????????????

        for (AbstractProp p : abstractProp) {
            Log.i("game",p.toString());

            if (heroAircraft.crash(p)) {
                p.vanish();
                p.use(heroAircraft);

                if (needMusic) {
                    if (p instanceof BombSupply) {
                        intent.putExtra("action", "bomb");
                        context.startService(intent);
                        score += ((BombSupply) p).score;
                        scorer += ((BombSupply) p).score;
                    } else if (p instanceof GoldCoin) {
                        intent.putExtra("action", "gold");
                        context.startService(intent);
                    } else if (p instanceof SilverCoin) {
                        intent.putExtra("action", "silver");
                        context.startService(intent);
                    } else {
                        intent.putExtra("action", "supply");
                        context.startService(intent);
                    }
                }
            }
            Log.i("game",".7");

        }
    }

    /**
     * ????????????
     * 1. ?????????????????????
     * 2. ?????????????????????
     * 3. ?????????????????????
     * <p>
     * ????????????????????????????????????????????????
     */
    private void postProcessAction() {
        enemyBullets.removeIf(AbstractFlyingObject::notValid);
        heroBullets.removeIf(AbstractFlyingObject::notValid);
        enemyAircrafts.removeIf(AbstractFlyingObject::notValid);
        abstractProp.removeIf(AbstractProp::notValid);

    }


    /**
     * ??????boss???????????????????????????
     */
    protected void bossTime(){
        bossExist = false;

        boolean flag = false;
        if(this.scorer >= bossScoreThreshold){
            flag = true;
            this.scorer = this.scorer-bossScoreThreshold;
        }

        for (AbstractAircraft enemy:enemyAircrafts){
            if (enemy instanceof BossEnemy) {
                bossExist = true;
                break;
            }
        }
        if ( !bossExist && flag){

            enemyFactory.boss = true;
            enemyAircrafts.add(enemyFactory.create());
            bossExist = true;
        }
    }


    //***********************
    //      Paint ?????????
    //***********************

    /**
     * ??????paint??????
     * ??????????????????paint???????????????????????????
     *
     */




    public void draw() {
        surfaceHolder = this.getHolder();
        canvas = surfaceHolder.lockCanvas();
        super.draw(canvas);
        if (canvas==null) return;

        // ????????????????????????
        canvas.drawBitmap(ImageManager.BACKGROUND_IMAGE,0,
                backGroundTop-ImageManager.BACKGROUND_IMAGE.getHeight(),paint);
        canvas.drawBitmap(ImageManager.BACKGROUND_IMAGE,0,backGroundTop,paint);
        backGroundTop+=1;
        if (backGroundTop==ImageManager.BACKGROUND_IMAGE.getHeight()) backGroundTop=0;
        backGroundTop += 1;
        if (backGroundTop == MainActivity.screenHeight) {
            this.backGroundTop = 0;
        }

        // ?????????????????????????????????????????????
        // ????????????????????????????????????

        paintImageWithPositionRevised(abstractProp);

        paintImageWithPositionRevised(enemyBullets);
        paintImageWithPositionRevised(heroBullets);

        paintImageWithPositionRevised(enemyAircrafts);

        canvas.drawBitmap(ImageManager.HERO_IMAGE, heroAircraft.getLocationX() - ImageManager.HERO_IMAGE.getWidth() / 2,
                heroAircraft.getLocationY() - ImageManager.HERO_IMAGE.getHeight() / 2, null);

        //????????????????????????
        paintScoreAndLife();

        //??????canvas??????
        surfaceHolder.unlockCanvasAndPost(canvas);
    }


    private void paintImageWithPositionRevised(List<? extends AbstractFlyingObject> objects) {
        Log.i("game","print");

        if (objects.size() == 0) {
            return;
        }

        for (AbstractFlyingObject object : objects) {
            Bitmap image = object.getImage();
            assert image != null : objects.getClass().getName() + " has no image! ";
            canvas.drawBitmap(image, object.getLocationX() - image.getWidth() / 2,
                    object.getLocationY() - image.getHeight() / 2, paint);
        }
    }

    protected void paintScoreAndLife() {
        Paint paint = new Paint();
        paint.setColor(Color.BLUE);
        Typeface font = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
        paint.setTypeface(font);
        paint.setTextSize(50);

        int x = 30;
        int y = 75;

        canvas.drawText("SCORE:" + this.score, x, y,paint);
        y = y + 60;
        canvas.drawText("LIFE:" + this.heroAircraft.getHp(), x, y,paint);
        y = y + 60;
        canvas.drawText("MONEY:" + player.money,x,y,paint);
    }

    /**
     * ??????player
     */
    private void createPlayer(){
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd :hh:mm:ss");
        player.score = this.score;
        player.time = date.toString();
    }

    /**
     * ????????????
     */
    private void music(){
        if(bossExist && !bossFlag){
            intent.putExtra("action","boss");
            context.startService(intent);
            bossFlag = true;
        }
        else {
            if(!bossExist && bossFlag){
                System.out.println("bgm yes");
                intent.putExtra("action","stop_boss");
                context.startService(intent);
                bossFlag = false;
            }
        }
    }

    public List<AbstractAircraft> getEnemyAircrafts() {
        return enemyAircrafts;
    }

    public List<AbstractBullet> getEnemyBullets() {
        return enemyBullets;
    }

    protected void grow(){

    }




    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {

    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {

    }

}
