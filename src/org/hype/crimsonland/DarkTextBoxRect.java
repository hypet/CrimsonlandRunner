package org.hype.crimsonland;

public class DarkTextBoxRect implements Comparable<DarkTextBoxRect> {

    private static final int CENTER_X = 1024 / 2;
    private static final int CENTER_Y = 768 / 2;
    private int x;
    private int y;
    private int x1;
    private int y1;

    public DarkTextBoxRect() {
    }

    public DarkTextBoxRect(int x, int y, int x1, int y1) {
        this.x = x;
        this.y = y;
        this.x1 = x1;
        this.y1 = y1;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getX1() {
        return x1;
    }

    public void setX1(int x1) {
        this.x1 = x1;
    }

    public int getY1() {
        return y1;
    }

    public void setY1(int y1) {
        this.y1 = y1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DarkTextBoxRect that = (DarkTextBoxRect) o;

        if (x != that.x) return false;
        if (x1 != that.x1) return false;
        if (y != that.y) return false;
        if (y1 != that.y1) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        result = 31 * result + x1;
        result = 31 * result + y1;
        return result;
    }

    @Override
    public String toString() {
        return "DarkTextBoxRect{" +
                "x=" + x +
                ", y=" + y +
                ", x1=" + x1 +
                ", y1=" + y1 +
                '}';
    }

    /*
     * Compare Dark Text Boxes relative to center
     * i.e. the rectangle in the center of the images will be less than the rectangle along the image's side
     * or at the any corner.
     */
    @Override
    public int compareTo(DarkTextBoxRect o) {
        int nearestThisX = this.x1 < CENTER_X ? this.x1 : this.x;
        int nearestThisY = this.y1 < CENTER_Y ? this.y1 : this.y;

        int nearestOtherX = o.x1 < CENTER_X ? o.x1 : o.x;
        int nearestOtherY = o.y1 < CENTER_Y ? o.y1 : o.y;

        double hypThis = Math.sqrt(Math.pow(CENTER_X - nearestThisX, 2) + Math.pow(CENTER_Y - nearestThisY, 2));
        double hypOther = Math.sqrt(Math.pow(CENTER_X - nearestOtherX, 2) + Math.pow(CENTER_Y - nearestOtherY, 2));
        if (hypThis == hypOther) {
            return 0;
        }
        return hypThis < hypOther ? -1 : 1;
    }
}
