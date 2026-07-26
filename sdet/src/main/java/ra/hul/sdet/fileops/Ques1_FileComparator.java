package ra.hul.sdet.fileops;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * File Comparator - Compare two files line by line and report differences.
 * Common SDET interview question: "Write a utility to diff two text files."
 *
 * Usage: Run main() — compares File1.txt and File2.txt from src/main/resources/testdata/
 */
public class Ques1_FileComparator {

    /**
     * Resolves a test-data file through the classpath rather than a repo-relative path, so the
     * problem runs the same from the IDE, from `mvn exec:java`, and from the CI verifier — all of
     * which have different working directories.
     */
    static String testDataPath(String fileName) throws IOException {
        java.net.URL url = Ques1_FileComparator.class.getResource("/testdata/" + fileName);
        if (url == null) {
            throw new IllegalStateException("testdata/" + fileName + " is not on the classpath");
        }
        if ("file".equals(url.getProtocol())) {
            try {
                return java.nio.file.Path.of(url.toURI()).toString();
            } catch (java.net.URISyntaxException e) {
                throw new IllegalStateException(e);
            }
        }
        // Packaged in a jar — there is no real file to point FileReader at, so stage a temp copy.
        java.nio.file.Path temp = java.nio.file.Files.createTempFile("testdata-", "-" + fileName);
        temp.toFile().deleteOnExit();
        try (java.io.InputStream in = Ques1_FileComparator.class.getResourceAsStream("/testdata/" + fileName)) {
            java.nio.file.Files.copy(in, temp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        return temp.toString();
    }

    public static void compareFiles(String filePath1, String filePath2) throws IOException {
        try (BufferedReader reader1 = new BufferedReader(new FileReader(filePath1));
             BufferedReader reader2 = new BufferedReader(new FileReader(filePath2))) {

            String line1, line2;
            int lineNumber = 0;
            boolean identical = true;

            while (true) {
                line1 = reader1.readLine();
                line2 = reader2.readLine();
                lineNumber++;

                if (line1 == null && line2 == null) break;

                if (line1 == null) {
                    System.out.printf("Line %d: File1 ended, File2 has: \"%s\"%n", lineNumber, line2);
                    identical = false;
                } else if (line2 == null) {
                    System.out.printf("Line %d: File2 ended, File1 has: \"%s\"%n", lineNumber, line1);
                    identical = false;
                } else if (!line1.equals(line2)) {
                    System.out.printf("Line %d differs:%n  File1: \"%s\"%n  File2: \"%s\"%n", lineNumber, line1, line2);
                    identical = false;
                }
            }

            if (identical) {
                System.out.println("Files are identical.");
            }
        }
    }

    static void main() throws IOException {
        compareFiles(testDataPath("File1.txt"), testDataPath("File2.txt"));

        // Self-check: a file must compare equal to itself.
        String f1 = testDataPath("File1.txt");
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        java.io.PrintStream original = System.out;
        System.setOut(new java.io.PrintStream(buffer, true));
        compareFiles(f1, f1);
        System.setOut(original);
        System.out.println(buffer.toString().contains("Files are identical.")
                ? "PASSED: identical files reported as identical."
                : "FAIL: self-comparison did not report the files as identical.");
    }
}
