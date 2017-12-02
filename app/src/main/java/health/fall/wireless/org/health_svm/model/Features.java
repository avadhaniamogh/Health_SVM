package health.fall.wireless.org.health_svm.model;

/**
 * Created by Amogh on 11/17/2017.
 */

public class Features {

    float x;
    float y;
    float z;
    double smv;
    double sma;

    public Features(float x, float y, float z, double smv, double sma) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.smv = smv;
        this.sma = sma;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getZ() {
        return z;
    }

    public void setZ(float z) {
        this.z = z;
    }

    public double getSmv() {
        return smv;
    }

    public void setSmv(double smv) {
        this.smv = smv;
    }

    public double getSma() {
        return sma;
    }

    public void setSma(double sma) {
        this.sma = sma;
    }
}
