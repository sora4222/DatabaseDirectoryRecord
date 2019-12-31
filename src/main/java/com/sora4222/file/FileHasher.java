package com.sora4222.file;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;


public class FileHasher {
  private static Logger logger = LogManager.getLogger();
  
  private final RandomAccessFile fileToHash;
  
  private int multiplier;
  
  /**
   * Creates a FileHasher that takes a number of a files bytes
   * and calculates a cryptographic hash from it.
   *
   * @param fileToHash The file that will be read and hashed.
   */
  public FileHasher(final File fileToHash) {
    try {
      this.fileToHash = new RandomAccessFile(fileToHash, "r");
    } catch (FileNotFoundException e) {
      if (fileToHash.canExecute() && fileToHash.canRead())
        logger.warn("The file permissions were: read:" + fileToHash.canRead() +
            ", executable: " + fileToHash.canExecute(), e);
      logger.info("File failed to hash: " + fileToHash.getAbsolutePath().replace("\\", "/"));
      throw new RuntimeException(e);
    }
  
    logger.debug(String.format("File to hash: %s, size: '%s' bytes",
        fileToHash.getName(), fileToHash.length()));
    setMultiplier();
  
  }
  
  public FileHasher(final File fileToHash, final int multiplier) {
    this(fileToHash);
    this.multiplier = multiplier;
  }
  
  public int getMultiplier() {
    return multiplier;
  }
  
  /**
   * Gets a cryptographic hash of a file that can be used to compare files.
   *
   * @return A cryptographic hash of the file for comparison use.
   */
  public String hashFile() {
    try {
      String unjointBytes = getUnjointBytes();
      return DigestUtils.sha1Hex(unjointBytes);
    } catch (IOException e) {
      logger.error(e);
      return DigestUtils.sha1Hex("");
    } finally {
      try {
        fileToHash.close();
      } catch (IOException e) {
        logger.error("A file finished hashing is not closing");
      }
    }
  }
  
  /**
   * Sets the multiplier, which sets how many parts of a file
   * will be used to calculate it's checksum.
   */
  private void setMultiplier() {
    final int megabyte = 1000000;
    final int gigabyte = 1000000000;
    try {
      if (fileToHash.length() < 40000) {
        multiplier = 1;
      } else if (fileToHash.length() < megabyte) {
        multiplier = 5;
      } else if (fileToHash.length() < 100 * megabyte) {
        multiplier = megabyte / 20;
      } else if (fileToHash.length() <= gigabyte) {
        multiplier = megabyte / 8;
      } else {
        multiplier = megabyte;
      }
    } catch (IOException e) {
      logger.warn(e);
      throw new RuntimeException(e);
    }
  }
  
  /**
   * This will take the bytes from the file as characters, and
   *
   * @return A string containing the converted bytes of a file
   * @throws IOException
   */
  private String getUnjointBytes() throws IOException {
    StringBuilder sb = new StringBuilder();
    long i = 0L;
    if (fileToHash.length() - 2 < 0) {
      logger.error("A file has a length that results in not reading a single character. Size:" + fileToHash.length());
      return String.valueOf(fileToHash.readChar());
    }
    while (i * multiplier < fileToHash.length() - 2) {
      fileToHash.seek(i++ * multiplier);
      sb.append(fileToHash.readChar());
    }
    return sb.toString();
  }
}
