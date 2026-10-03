import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class MandelbrotSet {
    private static final int WIDTH = 1920;
    private static final int HEIGHT = 1080;
    private static final int MAX_ITERATIONS = 1000;
    private static final int THREAD_NUMBER = 8;

    private static final double MIN_REAL = -2.5;
    private static final double MAX_REAL = 1.0;
    private static final double MIN_IMAGINARY = -1.0;
    private static final double MAX_IMAGINARY = 1.0;

    private static boolean inSet(double real, double imaginary) {
        double zReal = 0.0;
        double zImaginary = 0.0;
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            if (zReal * zReal + zImaginary * zImaginary > 4.0) {
                return false;
            }
            double newReal = zReal * zReal - zImaginary * zImaginary + real;
            zImaginary = 2.0 * zReal * zImaginary + imaginary;
            zReal = newReal;
        }
        return true;
    }

    private static double toReal(int x) {
        return MIN_REAL + (MAX_REAL - MIN_REAL) * x / WIDTH;
    }

    private static double toImaginary(int y) {
        return MIN_IMAGINARY + (MAX_IMAGINARY - MIN_IMAGINARY) * y / HEIGHT;
    }

    public static void main(String[] args) throws Exception {
        if (!inSet(0.0, 0.0) || !inSet(-1.0, 0.0)
                || inSet(1.0, 0.0) || inSet(0.0, 2.0)) {
            throw new AssertionError("Known points classified incorrectly");
        }

        boolean[] pixels = new boolean[WIDTH * HEIGHT];
        ExecutorService pool = Executors.newFixedThreadPool(THREAD_NUMBER);
        List<Future<?>> futures = new ArrayList<>();
        int rowsPerThread = HEIGHT / THREAD_NUMBER;

        long start = System.nanoTime();
        try {
            for (int t = 0; t < THREAD_NUMBER; t++) {
                int from = t * rowsPerThread;
                int to = (t == THREAD_NUMBER - 1) ? HEIGHT : from + rowsPerThread;
                futures.add(pool.submit(() -> {
                    for (int y = from; y < to; y++) {
                        for (int x = 0; x < WIDTH; x++) {
                            pixels[y * WIDTH + x] = inSet(toReal(x), toImaginary(y));
                        }
                    }
                }));
            }
            for (Future<?> future : futures) {
                future.get(60, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
        long millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        for (int i = 0; i < pixels.length; i += 123_457) {
            int x = i % WIDTH;
            int y = i / WIDTH;
            if (pixels[i] != inSet(toReal(x), toImaginary(y))) {
                throw new AssertionError("Pixel " + i + " computed incorrectly");
            }
        }

        int count = 0;
        for (boolean b : pixels) {
            if (b) {
                count++;
            }
        }
        System.out.println("OK: pixels in set = " + count + " of " + pixels.length
                + " (" + 100 * count / pixels.length + "%), time = " + millis + " ms");
    }
}