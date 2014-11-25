package org.hype.crimsonland;

import org.junit.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

public class DarkBoxRectTest {

    @Test
    public void comparatorTest() {
        Set<DarkTextBoxRect> set = new TreeSet<DarkTextBoxRect>();
        DarkTextBoxRect box1 = new DarkTextBoxRect(100, 100, 150, 125);
        DarkTextBoxRect box2 = new DarkTextBoxRect(10, 10, 20, 25);
        DarkTextBoxRect box3 = new DarkTextBoxRect(900, 500, 950, 525);
        DarkTextBoxRect box4 = new DarkTextBoxRect(800, 400, 850, 425);
        DarkTextBoxRect box5 = new DarkTextBoxRect(550, 350, 600, 375);
        DarkTextBoxRect box6 = new DarkTextBoxRect(460, 350, 490, 375);

        set.add(box1);
        set.add(box2);
        set.add(box3);
        set.add(box4);
        set.add(box5);
        set.add(box6);

        DarkTextBoxRect[] array = set.toArray(new DarkTextBoxRect[0]);
        System.out.println(Arrays.toString(array));
        DarkTextBoxRect[] arrayCheck = {box6, box5, box4, box3, box1, box2};
        System.out.println(Arrays.toString(arrayCheck));
        assert Arrays.equals(array, arrayCheck);
    }

}
