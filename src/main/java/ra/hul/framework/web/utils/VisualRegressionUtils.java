package ra.hul.framework.web.utils;

import io.qameta.allure.Attachment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import ra.hul.framework.core.config.ConfigManager;
import ra.hul.framework.web.driver.DriverManager;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Homegrown, fully-offline visual regression helper.
 *
 * <p>Captures a screenshot of the current page (or a single element), compares it pixel-by-pixel
 * against a committed baseline PNG, and produces a highlighted diff image — no cloud service or
 * external visual-testing SaaS involved. All tuning comes from config (read via {@code getOrDefault}
 * so absence never crashes):</p>
 *
 * <ul>
 *   <li>{@code visual.baseline.dir}   — where committed baselines live (default {@code src/test/resources/visual/baseline})</li>
 *   <li>{@code visual.output.dir}     — where actual + diff artifacts are written (default {@code target/visual})</li>
 *   <li>{@code visual.pixel.tolerance}— per-channel colour delta treated as equal, 0-255 (default {@code 20})</li>
 *   <li>{@code visual.diff.threshold} — max fraction of mismatching pixels before failing, 0.0-1.0 (default {@code 0.01})</li>
 *   <li>{@code visual.update.baselines}— when {@code true}, (re)writes the baseline instead of comparing (default {@code false})</li>
 * </ul>
 *
 * <p>This class never asserts — it returns a {@link VisualComparisonResult} and the calling test
 * decides pass/fail (assertions live in tests, POM/enforcement rule).</p>
 */
public final class VisualRegressionUtils {

    private static final Logger log = LogManager.getLogger(VisualRegressionUtils.class);

    private VisualRegressionUtils() {
    }

    /** Immutable result of a single visual comparison. Highlight image bytes are attached to Allure. */
    public static final class VisualComparisonResult {
        private final String name;
        private final boolean match;
        private final boolean baselineCreated;
        private final long diffPixels;
        private final long totalPixels;
        private final double diffRatio;
        private final double threshold;
        private final boolean dimensionMismatch;
        private final Path baselinePath;
        private final Path actualPath;
        private final Path diffPath;

        VisualComparisonResult(String name, boolean match, boolean baselineCreated, long diffPixels,
                               long totalPixels, double diffRatio, double threshold,
                               boolean dimensionMismatch, Path baselinePath, Path actualPath, Path diffPath) {
            this.name = name;
            this.match = match;
            this.baselineCreated = baselineCreated;
            this.diffPixels = diffPixels;
            this.totalPixels = totalPixels;
            this.diffRatio = diffRatio;
            this.threshold = threshold;
            this.dimensionMismatch = dimensionMismatch;
            this.baselinePath = baselinePath;
            this.actualPath = actualPath;
            this.diffPath = diffPath;
        }

        public String getName() { return name; }
        public boolean isMatch() { return match; }
        public boolean isBaselineCreated() { return baselineCreated; }
        public long getDiffPixels() { return diffPixels; }
        public long getTotalPixels() { return totalPixels; }
        public double getDiffRatio() { return diffRatio; }
        public double getThreshold() { return threshold; }
        public boolean isDimensionMismatch() { return dimensionMismatch; }
        public Path getBaselinePath() { return baselinePath; }
        public Path getActualPath() { return actualPath; }
        public Path getDiffPath() { return diffPath; }

        public String summary() {
            if (baselineCreated) {
                return "Baseline '" + name + "' created at " + baselinePath + " (first run — no comparison performed)";
            }
            return String.format(
                    "Visual '%s': match=%b, diffPixels=%d/%d (ratio=%.5f, threshold=%.5f)%s",
                    name, match, diffPixels, totalPixels, diffRatio, threshold,
                    dimensionMismatch ? " [DIMENSION MISMATCH]" : "");
        }

        @Override
        public String toString() {
            return summary();
        }
    }

    /** Capture the whole page and compare against baseline {@code <name>.png}. */
    public static VisualComparisonResult compare(String name) {
        return doCompare(name, captureImage(screenshotBytes()));
    }

    /** Capture a single element and compare against baseline {@code <name>.png}. */
    public static VisualComparisonResult compare(String name, WebElement element) {
        return doCompare(name, captureImage(element.getScreenshotAs(OutputType.BYTES)));
    }

    // ---------------------------------------------------------------------------------------------

    private static VisualComparisonResult doCompare(String name, BufferedImage actual) {
        String baselineDir = ConfigManager.getOrDefault("visual.baseline.dir", "src/test/resources/visual/baseline");
        String outputDir = ConfigManager.getOrDefault("visual.output.dir", "target/visual");
        int tolerance = ConfigManager.getIntOrDefault("visual.pixel.tolerance", 20);
        double threshold = parseDoubleOrDefault("visual.diff.threshold", 0.01);
        boolean updateMode = Boolean.parseBoolean(ConfigManager.getOrDefault("visual.update.baselines", "false"));

        Path baselinePath = Path.of(baselineDir, name + ".png");
        Path outDir = Path.of(outputDir);
        Path actualPath = outDir.resolve(name + "-actual.png");
        Path diffPath = outDir.resolve(name + "-diff.png");

        try {
            Files.createDirectories(outDir);
            writePng(actual, actualPath);

            boolean baselineExists = Files.exists(baselinePath);

            if (updateMode || !baselineExists) {
                Files.createDirectories(baselinePath.getParent());
                writePng(actual, baselinePath);
                attachActual(toPng(actual));
                log.info("Visual baseline {} written to {} (updateMode={}, existed={})",
                        name, baselinePath, updateMode, baselineExists);
                return new VisualComparisonResult(name, true, true, 0,
                        (long) actual.getWidth() * actual.getHeight(), 0.0, threshold, false,
                        baselinePath, actualPath, null);
            }

            BufferedImage baseline = ImageIO.read(baselinePath.toFile());
            if (baseline == null) {
                throw new IllegalStateException("Baseline could not be read as an image: " + baselinePath);
            }

            int width = Math.max(baseline.getWidth(), actual.getWidth());
            int height = Math.max(baseline.getHeight(), actual.getHeight());
            boolean dimensionMismatch = baseline.getWidth() != actual.getWidth()
                    || baseline.getHeight() != actual.getHeight();

            BufferedImage diff = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            long diffPixels = 0;
            long totalPixels = (long) width * height;
            int highlight = Color.RED.getRGB();

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    boolean inBaseline = x < baseline.getWidth() && y < baseline.getHeight();
                    boolean inActual = x < actual.getWidth() && y < actual.getHeight();

                    if (!inBaseline || !inActual) {
                        // Out-of-overlap area (different dimensions) counts as a difference.
                        diff.setRGB(x, y, highlight);
                        diffPixels++;
                        continue;
                    }

                    int b = baseline.getRGB(x, y);
                    int a = actual.getRGB(x, y);
                    if (pixelsDiffer(b, a, tolerance)) {
                        diff.setRGB(x, y, highlight);
                        diffPixels++;
                    } else {
                        // Keep matching pixels but dim them so the red diff stands out.
                        diff.setRGB(x, y, dim(a));
                    }
                }
            }

            writePng(diff, diffPath);

            double diffRatio = totalPixels == 0 ? 0.0 : (double) diffPixels / totalPixels;
            boolean match = !dimensionMismatch && diffRatio <= threshold;

            attachBaseline(toPng(baseline));
            attachActual(toPng(actual));
            attachDiff(toPng(diff));

            VisualComparisonResult result = new VisualComparisonResult(name, match, false, diffPixels,
                    totalPixels, diffRatio, threshold, dimensionMismatch, baselinePath, actualPath, diffPath);
            log.info(result.summary());
            return result;
        } catch (IOException e) {
            throw new UncheckedIOException("Visual comparison failed for '" + name + "'", e);
        }
    }

    private static boolean pixelsDiffer(int rgb1, int rgb2, int tolerance) {
        int r1 = (rgb1 >> 16) & 0xFF, g1 = (rgb1 >> 8) & 0xFF, b1 = rgb1 & 0xFF;
        int r2 = (rgb2 >> 16) & 0xFF, g2 = (rgb2 >> 8) & 0xFF, b2 = rgb2 & 0xFF;
        return Math.abs(r1 - r2) > tolerance
                || Math.abs(g1 - g2) > tolerance
                || Math.abs(b1 - b2) > tolerance;
    }

    /** Lighten a matching pixel toward white so the red diff overlay stands out visually. */
    private static int dim(int rgb) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        r = r + (255 - r) * 3 / 5;
        g = g + (255 - g) * 3 / 5;
        b = b + (255 - b) * 3 / 5;
        return (r << 16) | (g << 8) | b;
    }

    private static byte[] screenshotBytes() {
        WebDriver driver = DriverManager.getDriver();
        return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
    }

    private static BufferedImage captureImage(byte[] png) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(png));
            if (img == null) {
                throw new IllegalStateException("Captured screenshot could not be decoded as an image");
            }
            return img;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to decode screenshot", e);
        }
    }

    private static void writePng(BufferedImage image, Path path) throws IOException {
        Files.createDirectories(path.getParent());
        ImageIO.write(image, "png", path.toFile());
    }

    private static byte[] toPng(BufferedImage image) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to encode PNG for Allure attachment", e);
        }
    }

    private static double parseDoubleOrDefault(String key, double fallback) {
        try {
            return Double.parseDouble(ConfigManager.getOrDefault(key, Double.toString(fallback)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Attachment(value = "Visual - Baseline", type = "image/png")
    private static byte[] attachBaseline(byte[] png) {
        return png;
    }

    @Attachment(value = "Visual - Actual", type = "image/png")
    private static byte[] attachActual(byte[] png) {
        return png;
    }

    @Attachment(value = "Visual - Diff", type = "image/png")
    private static byte[] attachDiff(byte[] png) {
        return png;
    }
}
