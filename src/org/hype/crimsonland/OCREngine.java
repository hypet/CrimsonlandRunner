package org.hype.crimsonland;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.locks.LockSupport;

public class OCREngine {

    private static final String DB_FILE = "chars.db";
    public static final int DARK_TEXT_BOX_COLOR_THRESHOLD = 20;
    public static final int MEDIUM_TEXT_BOX_COLOR_THRESHOLD = 30;
    public static final int LIGHT_TEXT_BOX_COLOR_THRESHOLD = 70;
    public static final int INITIAL_GAME_START_DELAY_MS = 16000;
    public static final int DELAY_BETWEEN_SCREENSHOT_MS = 200;
    public static final int SCREENSHOT_LEFT_UPPER_ANGLE_Y = 102;
    public static final int SCREENSHOT_RIGHT_BOTTOM_ANGLE_Y = 525;
    public static final int TEXT_BOX_HEIGHT = 15;
    public static final int BW_IMAGE_WHITE_PIXEL_THRESHOLD = 190;
    public static final int MIN_TEXT_BOX_WIDTH = 30;
    private static Map<ByteArrayWrapper, Character> chars = new HashMap<ByteArrayWrapper, Character>(30);
    private static final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<String>(50);
    private static boolean LEARNING_MODE = false;
    private int counter = 0;
    private int currentColorThreshold = DARK_TEXT_BOX_COLOR_THRESHOLD;
    private volatile boolean isRunning = true;
    private ForkJoinPool fjPool = new ForkJoinPool(4);

    static class WordRecognizer extends RecursiveAction {

        private static final int LIMIT = 4;
        private BufferedImage image;
        private DarkTextBoxRect[] boxes;
        private int start, end;

        WordRecognizer(BufferedImage image, DarkTextBoxRect[] boxes, int start, int end) {
            this.image = image;
            this.boxes = boxes;
            this.start = start;
            this.end = end;
        }

        @Override
        protected void compute() {
            if (end - start < LIMIT) {
                try {
                    for(int i = start; i < end; i++){
                        DarkTextBoxRect box = boxes[i];
                        String name = getChars(image.getSubimage(box.getX(), box.getY(), box.getX1() - box.getX(), box.getY1() - box.getY()));
                        try {
                            if (name != null && name.length() > 0) {
                                System.out.println("Name: " + name);
                                queue.put(name);
                            }
                        } catch (InterruptedException e) {
                            System.out.println(e);
                        }
                    }
                } catch (IOException e) {
                    // NOP
                }
            } else {
                int mid = (start + end)/2;
                WordRecognizer left = new WordRecognizer(image, boxes, start, mid);
                WordRecognizer right = new WordRecognizer(image, boxes, mid, end);
                left.fork();
                right.fork();
                left.join();
                right.join();
            }
        }
    }

    public OCREngine() {
        loadMapFromFile();
    }

    private BufferedImage getGrayImage(BufferedImage image) {
        return convertColorModel(image, BufferedImage.TYPE_BYTE_GRAY);
    }

    private BufferedImage convertColorModel(BufferedImage image, int colorModel) {
        BufferedImage bw = new BufferedImage(image.getWidth(), image.getHeight(), colorModel);
        Graphics2D g2d = bw.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        return bw;
    }

    private static BufferedImage getBWImage(BufferedImage original) {
        int color;
        int newPixel;
        BufferedImage bwImage = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        WritableRaster raster = bwImage.getData().createCompatibleWritableRaster(original.getWidth(), original.getHeight());

        for(int i = 0; i < original.getWidth(); i++) {
            for(int j = 0; j < original.getHeight(); j++) {
                color = original.getRaster().getSample(i, j, 0);
                newPixel = color > BW_IMAGE_WHITE_PIXEL_THRESHOLD ? 1 : 0;
                raster.setSample(i, j, 0, newPixel);
            }
        }
        bwImage.setData(raster);

        return bwImage;
    }

    public static String getChars(BufferedImage image) throws IOException {
        if (image == null) {
            System.out.println("image is null");
            return "";
        }
//        ImageIO.write(image, "bmp", new File(String.format("temp/temp_bw_box_%02d.bmp", counter)));
        image = getCharsImage(image);
        if (image == null) {
            return "";
        }

        int startOfCharX = 0;
        int endOfCharX = 1;

        StringBuilder result = new StringBuilder();
        while (startOfCharX < image.getWidth()) {
            int emptyCols = isSpaceBetweenChars(image, endOfCharX);
            if (emptyCols > 0) {
                Character character = extractChar(image, startOfCharX, endOfCharX);
                if (character != null) {
                    result.append(character);
                    startOfCharX = endOfCharX + emptyCols;
                    endOfCharX += emptyCols;
                } else {
                    result = new StringBuilder();
                    break;
                }
            }
            endOfCharX++;
        }
        return result.toString();
    }

    private static int isSpaceBetweenChars(BufferedImage image, int x) {
        int result = 0;
        if (isColumnEmpty(image, x)) {
            result++;
        } else {
            return 0;
        }
        if (isColumnEmpty(image, x + 1)) {
            result++;
        }
        return result;
    }

    private static boolean isColumnEmpty(BufferedImage image, int x) {
        boolean result = false;
        for (int y = 0; y < image.getHeight(); y++) {
            if (image.getData().getSample(x, y, 0) != 0) {
                result = false;
                break;
            } else {
                result = true;
            }
        }
        return result;
    }

    private static Character extractChar(BufferedImage image, int startOfCharX, int endOfCharX) {
        byte[][] data = new byte[endOfCharX - startOfCharX][image.getHeight()];
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = startOfCharX; x < endOfCharX; x++) {
                int col = image.getData().getSample(x, y, 0);
                data[x - startOfCharX][y] = (byte) col;
                if (LEARNING_MODE) {
                    System.out.print(col);
                }
            }
            if (LEARNING_MODE) {
                System.out.println();
            }
        }
        if (LEARNING_MODE) {
            System.out.println();
        }

        ByteArrayWrapper wrapper = new ByteArrayWrapper(data);
        Character result = chars.get(wrapper);
        if (LEARNING_MODE) {
            System.out.println(result);
        }
        if (chars.get(wrapper) == null) {
            System.out.println("Unknown char!");
            /*
            if (LEARNING_MODE) {
                Scanner scanner = new Scanner(System.in);
                String character = scanner.next();
                chars.put(wrapper, character.charAt(0));
                saveMapToFile();
            }
            */
        }
        return result;
    }

    private void saveMapToFile() {
        try {
            FileOutputStream fos = new FileOutputStream(DB_FILE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(chars);
            oos.close();
            fos.close();
            System.out.println("Serialized HashMap data");
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void loadMapFromFile() {
        try {
            if (new File(DB_FILE).exists()) {
                FileInputStream fis = new FileInputStream(DB_FILE);
                ObjectInputStream ois = new ObjectInputStream(fis);
                chars = (HashMap<ByteArrayWrapper, Character>) ois.readObject();
                ois.close();
                fis.close();
                System.out.println("Loaded HashMap data");

//                Set set = chars.entrySet();
//                Iterator iterator = set.iterator();
//                while (iterator.hasNext()) {
//                    Map.Entry entry = (Map.Entry) iterator.next();
//                    System.out.print("key: " + entry.getKey() + " & Value: ");
//                    System.out.println(entry.getValue());
//                }
            }
        } catch(IOException ioe) {
            ioe.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /*
        Cuts rectangle image with word. First char starts at (0,0).
     */
    private static BufferedImage getCharsImage(BufferedImage image) {
        boolean isEmptyRow = false;
        int cutX1 = 0;
        int cutWidth = image.getWidth();
        final int upperBorder = 2;
        final int bottomBorder = image.getHeight() - 2 - upperBorder;
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = upperBorder; y < bottomBorder; y++) {
                if (image.getData().getSample(x, y, 0) == 0) {
                    isEmptyRow = true;
                } else {
                    isEmptyRow = false;
                    break;
                }
            }
            if (isEmptyRow) {
                cutX1++;
                isEmptyRow = false;
            } else {
                break;
            }
        }

        isEmptyRow = false;
        for (int x = image.getWidth() - 1; x > cutX1; x--) {
            for (int y = upperBorder; y < bottomBorder; y++) {
                if (image.getData().getSample(x, y, 0) == 0) {
                    isEmptyRow = true;
                } else {
                    isEmptyRow = false;
                    break;
                }
            }
            if (isEmptyRow) {
                cutWidth--;
                isEmptyRow = false;
            } else {
                break;
            }
        }

        if (cutX1 > 0 && cutWidth - cutX1 > 0 && bottomBorder > 0) {
            return image.getSubimage(cutX1, upperBorder, cutWidth - cutX1, bottomBorder);
        } else {
            return null;
        }
    }

    private int calculateThreshold(BufferedImage image) {
        int startX = image.getWidth() / 4;
        int length = image.getWidth() / 8;
        int color = 0;
        for (int x = startX; x < startX + length; x++) {
            color += image.getRaster().getSample(x, 1, 0);
            color += image.getRaster().getSample(x, image.getHeight() - 1, 0);
        }
        int avg = color / (length * 2);
        if (avg > 0 && avg < 30) {
            return DARK_TEXT_BOX_COLOR_THRESHOLD;
        } else if (avg > 30 && avg < 54) {
            return MEDIUM_TEXT_BOX_COLOR_THRESHOLD;
        } else {
            return LIGHT_TEXT_BOX_COLOR_THRESHOLD ;
        }
    }

    private void getRegions() throws IOException {
        LEARNING_MODE = false;
        BufferedImage image = getGrayImage(ImageIO.read(new File("temp/manyNamesSample.bmp")));
        getRegions(image);
    }

    private void getRegions(BufferedImage image) throws IOException {
        image = getGrayImage(image);
        currentColorThreshold = calculateThreshold(image);
//        System.out.println("Chosen threshold: " + currentColorThreshold);
        int width = image.getWidth();
        int height = image.getHeight();
//        ImageIO.write(image, "bmp", new File(String.format("temp/temp_grayed_%04d.bmp", counter )));
        if (counter > 100) {
            counter = 0;
        } else {
            counter++;
        }

        Set<DarkTextBoxRect> set = new TreeSet<DarkTextBoxRect>();

        Raster raster = image.getData();
        long start = System.currentTimeMillis();
        for (int y = 0; y < height; y = y + 2) {
            for (int x = 0; x < width; x = x + 2) {
                try {
                    if (raster.getSample(x, y, 0) < currentColorThreshold) {
                        DarkTextBoxRect box = getDarkTextBox(raster, x, y);
                        if (box != null) {
                            if (box.getX1() - box.getX() > TEXT_BOX_HEIGHT
                                    && (box.getY1() < image.getHeight())) {
                                set.add(box);
                                x += MIN_TEXT_BOX_WIDTH;
                                y += TEXT_BOX_HEIGHT;
                            }
                        }
                    }
                } catch (Exception e) {
                    // NOP
                }
            }
        }
        System.out.println("Time to find regions: " + (System.currentTimeMillis() - start));
        start = System.currentTimeMillis();
        System.out.println("Objects found: " + set.size());
        image = getBWImage(image);
        fjPool.invoke(new WordRecognizer(image, set.toArray(new DarkTextBoxRect[0]), 0, set.size()));
        System.out.println("Time to recognize chars: " + (System.currentTimeMillis() - start));
    }

    public DarkTextBoxRect getDarkTextBox(Raster raster, int implicitX, int implicitY) {
        int x = implicitX;
        int x1 = implicitX;
        int y = implicitY;
        try {
            while (raster.getSample(x, y, 0) < currentColorThreshold) {
                y--;
            }
            y++;
            while (raster.getSample(x, y, 0) < currentColorThreshold) {
                x--;
            }
            x++;
            while (raster.getSample(x1, y, 0) < currentColorThreshold) {
                x1++;
            }
            x1--;
            return new DarkTextBoxRect(x, y, x1, y + TEXT_BOX_HEIGHT);
        } catch (ArrayIndexOutOfBoundsException e) {
            // NOP
        }
        return null;
    }

    public LinkedBlockingQueue<String> getQueue() {
        return queue;
    }

    public void start(Robot robot, GraphicsDevice gd) {
        try {
            Thread.sleep(INITIAL_GAME_START_DELAY_MS);
        } catch (InterruptedException e) {
            System.out.println(e);
        }
        while (isRunning) {
            try {
                Rectangle rect = new Rectangle(0, SCREENSHOT_LEFT_UPPER_ANGLE_Y, gd.getDisplayMode().getWidth(), SCREENSHOT_RIGHT_BOTTOM_ANGLE_Y);
                getRegions(robot.createScreenCapture(rect));
            } catch (IOException e) {
                System.out.println(e);
            }
            LockSupport.parkNanos(DELAY_BETWEEN_SCREENSHOT_MS * 1000000);
        }
        System.out.println("OCREngine stopped");
    }

    public void stop() {
        isRunning = false;
    }

    public static void main(String[] args) throws AWTException, IOException {
        OCREngine engine = new OCREngine();
        engine.getRegions();
//        engine.getText();
//        engine.getChars();
    }

}
