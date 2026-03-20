package ra.hul.sdet.fileops;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * File Comparator - Compare two files line by line and report differences.
 * Common SDET interview question: "Write a utility to diff two text files."
 *
 * Usage: Run main() — compares File1.txt and File2.txt from sdet/src/main/resources/testdata/
 */
public class Ques1_FileComparator {

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

    public static void main(String[] args) throws IOException {
        String basePath = "sdet/src/main/resources/testdata/";
        compareFiles(basePath + "File1.txt", basePath + "File2.txt");
    }
}
