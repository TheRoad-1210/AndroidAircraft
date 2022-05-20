package com.example.androidaircraft.bullet;


import com.example.androidaircraft.MainActivity;
import com.example.androidaircraft.basic.AbstractFlyingObject;

/**
 * 子弹类。
 * 也可以考虑不同类型的子弹
 *
 * @author hitsz
 */
public class AbstractBullet extends AbstractFlyingObject {

    public void setPower(int power) {
        this.power = power;
    }

    private int power = 10;

    public AbstractBullet(int locationX, int locationY, int speedX, int speedY, int power) {
        super(locationX, locationY, speedX, speedY);
        this.power = power;
    }

    @Override
    public void forward() {
        super.forward();

        // 判定 x 轴出界
        if (locationX <= 0 || locationX >= MainActivity.screenWidth) {
            vanish();
        }

        // 判定 y 轴出界
        if (speedY > 0 && locationY >= MainActivity.screenHeight ) {
            // 向下飞行出界
            vanish();
        }else if (locationY <= 0){
            // 向上飞行出界
            vanish();
        }
    }

    public int getPower() {
        return power;
    }
}
