package com.sora4222.file;

import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;


public class FileHasher {
  private static Logger logger = LogManager.getLogger();
  
  private final MessageDigest digester;
  private final RandomAccessFile fileToHash;
  
  @Getter
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
      logger.warn(e);
      throw new RuntimeException(e);
    }
  
    logger.debug(String.format("File to hash: %s, size: '%s' bytes",
        fileToHash.getName(), fileToHash.length()));
    setMultiplier();
  
    try {
      digester = MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException e) {
      logger.error(e.getMessage());
      throw new RuntimeException(e);
    }
  }
  
  FileHasher(final File fileToHash, final int multiplier) {
    this(fileToHash);
    this.multiplier = multiplier;
  }
  
  /**
   * Gets a cryptographic hash of a file that can be used to compare files.
   * @return A cryptographic hash of the file for comparison use.
   */
  public String hashFile() {
    digestFile();
    byte[] resultantDigest = digester.digest();
  
    try {
      fileToHash.close();
    } catch (IOException e) {
      logger.error("A filestream for a FileHasher could not be closed.", e);
    }
    
    return new String(resultantDigest, StandardCharsets.UTF_8);
  
  }
  
  /**
   * Sets the multiplier, which sets how many parts of a file
   * will be used to calculate it's checksum.
   */
  private void setMultiplier() {
    int megabyte = 1000000;
    int gigabyte = 1000000000;
    try {
      if (fileToHash.length() < 40000) {
        multiplier = 2;
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
  
  private void digestFile() {
    long i = 0L;
    try {
      ArrayList<Byte> bytesToHash = new ArrayList<Byte>();
      while (i * multiplier < fileToHash.length()) {
        fileToHash.seek(i++ * multiplier);
        bytesToHash.add(fileToHash.readByte());
      }
      byte[] arrayAsString = normalizeBytes(bytesToHash);
      digester.update(arrayAsString);
    } catch (IOException e) {
      logger.error(e);
      throw new RuntimeException(e);
    }
  }
  
  /**
   * This converts the bytes which can be read differently in different
   * computers, this gets the computer to convert the bytes to UTF-8
   * a standard byte formation
   *
   * @param bytesToHash The
   * @return The normalized bytes ready for the hash digester.
   */
  private byte[] normalizeBytes(ArrayList<Byte> bytesToHash) {
    Byte[] collapsedArray = bytesToHash.toArray(new Byte[0]);
    byte[] convertedToPrimitive = ArrayUtils.toPrimitive(collapsedArray);
    String stringBasedBytes = new String(convertedToPrimitive);
    return stringBasedBytes.getBytes(StandardCharsets.UTF_8);
  }
}
