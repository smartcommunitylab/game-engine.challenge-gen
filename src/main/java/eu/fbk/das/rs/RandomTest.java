package eu.fbk.das.rs;

import org.testng.annotations.Test;

import static eu.fbk.das.utils.Utils.pf;

public class RandomTest {

    int map(long x, long in_min, long in_max, long out_min, long out_max)
    {
        return (int) ((x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min);
    }

    public void dir(int dir) {

        int min_speed = 100;
        int max_speed = 200;

        int speed_l;
        int speed_r;


        if (dir < 45) {
            speed_l = map(dir, 0, 45, max_speed, min_speed);
            speed_r = max_speed;
        } else if (dir < 90) {
            speed_l = map(dir - 45, 0, 45, -min_speed, -max_speed);
            speed_r = max_speed;
        } else if (dir < 135) {
            speed_l = -max_speed;
            speed_r = map(dir - 90, 0, 45, max_speed, min_speed);
        } else if (dir < 180) {
            speed_l = -max_speed;
            speed_r = map(dir - 135, 0, 45, -min_speed, -max_speed);
        } else if (dir < 225) {
            speed_l = map(dir - 180, 0, 45, -max_speed, -min_speed);
            speed_r = -max_speed;
        } else if (dir < 290) {
            speed_l = map(dir - 225, 0, 45, min_speed, max_speed);
            speed_r = -max_speed;
        } else if (dir < 335) {
            speed_l = max_speed;
            speed_r = map(dir - 290, 0, 45, -max_speed, -min_speed);
        } else {
            speed_l = max_speed;
            speed_r = map(dir - 335, 0, 45, min_speed, max_speed);
        }

        pf("dir: %d, l: %d, r: %d \n", dir, speed_l, speed_r);

    }

    @Test
    public void testDir() {
        dir (5);

    }
}
