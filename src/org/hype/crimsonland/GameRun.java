package org.hype.crimsonland;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class GameRun {

    private static final String GAME_PATH = "d:\\games\\Crimsonland\\";
    private static final String EXEC_NAME = "crimsonland.exe";
    private static final long KEYPRESS_DELAY_NS = 100000L;
    private static final String[] KNOWN_NAMES = {"dick", "shot", "leg", "fly", "earl", "stab", "brown", "lazy", "jumper",
            "bill", "hand", "harry", "mary", "fox", "hat", "cat", "gate", "quick", "call", "mate", "lord", "king", "cow",
            "ice", "cube", "jack", "unique", "lamb", "tail", "pie", "tail", "low", "high", "road", "nose", "head", "over",
            "nerd", "tom", "zeta", "dog", "gun"
    };

    private final GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    private final Robot robot;
    private ExecutorService executor;
    private final OCREngine ocrEngine;
    private LinkedBlockingQueue<String> ocrQueue;
    private Process game;
    private volatile boolean isGameRunning = false;

    public GameRun() throws AWTException {
        robot = new Robot(gd);
        ocrEngine = new OCREngine();
        ocrQueue = ocrEngine.getQueue();
        try {
            game = Runtime.getRuntime().exec(GAME_PATH + EXEC_NAME, null, new File(GAME_PATH));
            isGameRunning = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {

        executor = Executors.newFixedThreadPool(2);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                ocrEngine.start(robot, gd);
            }
        });

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    game.waitFor();
                } catch (InterruptedException e) {
                    // NOP
                } finally {
                    ocrEngine.stop();
                    isGameRunning = false;
                }
            }
        });

        try {
            Thread.sleep(1000);

            keyPress(KeyEvent.VK_ENTER);
            Thread.sleep(6000);

            keyPress(KeyEvent.VK_ESCAPE);
            Thread.sleep(1000);

            keyPress(KeyEvent.VK_ESCAPE);

            enterGame();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public void enterGame() throws InterruptedException {
        mouseClick(1, 1);
        Thread.sleep(100);
        mouseClick(150, 170);
        Thread.sleep(1000);
        mouseClick(150, 200);
        Thread.sleep(1000);
        mouseClick(150, 200);


        Thread.sleep(5000);
        killThemAll();

        executor.shutdown();
        System.exit(0);
//        killThemAllBruteForce();
    }

    private void killThemAll() {
        while (isGameRunning) {
            try {
                String name = ocrQueue.poll(1, TimeUnit.SECONDS);
                if (name != null) {
                    System.out.println("Name: " + name);
                    enterWord(name);
                    LockSupport.parkNanos(KEYPRESS_DELAY_NS);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void killThemAllBruteForce() throws InterruptedException {
        for (int i = 0; i < 130; i++) {
            for (String s : KNOWN_NAMES) {
                enterWord(s.toUpperCase());
                Thread.sleep(1);
            }
        }

        for (int i = 0; i < 2; i++) {
            for (String s1 : KNOWN_NAMES) {
                for (String s2 : KNOWN_NAMES) {
                    enterWord((s1 + s2).toUpperCase());
                }
                enterWord(s1.toUpperCase());
            }
        }
    }

    private void enterWord(String word) throws InterruptedException {
        for (int i = 0; i < word.length(); i++) {
            if (word.charAt(i) > 'Z' || word.charAt(i) < 'A') {
                keyPress((int) word.toUpperCase().charAt(i));
            } else {
                keyPressWithShift((int) word.toUpperCase().charAt(i));
            }
        }
        LockSupport.parkNanos(KEYPRESS_DELAY_NS);
        keyPress(KeyEvent.VK_ENTER);
    }

    public void mouseClick(int x, int y) throws InterruptedException {
        robot.mouseMove(x, y);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        LockSupport.parkNanos(10000L);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    public void keyPress(int key) {
        robot.keyPress(key);
        LockSupport.parkNanos(KEYPRESS_DELAY_NS);
        robot.keyRelease(key);
    }

    public void keyPressWithShift(int key) {
        robot.keyPress(KeyEvent.VK_SHIFT);
        robot.keyPress(key);
        LockSupport.parkNanos(KEYPRESS_DELAY_NS);
        robot.keyRelease(key);
        robot.keyRelease(KeyEvent.VK_SHIFT);
    }

    public static void main(String[] args) throws AWTException, IOException, InterruptedException {
        GameRun gameRun = new GameRun();
        gameRun.start();
//        gameRun.enterWord("Hype.");
    }

}
