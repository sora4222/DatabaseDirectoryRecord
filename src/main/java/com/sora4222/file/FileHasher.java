package com.sora4222.file;

import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


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
      logger.fatal(e);
      throw new RuntimeException(e);
    }
    logger.info(String.format("File to hash: %s, size: '%s' bytes",
        fileToHash.getName(), fileToHash.length()));
    setMultiplier();
    try {
      digester = MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException e) {
      logger.fatal(e.getMessage());
      throw new RuntimeException(e);
    }
  }
  
  FileHasher(final File fileToHash, final int multiplier) {
    this(fileToHash);
    this.multiplier = multiplier;
  }
  
  /**
   * Gets a cryptographic hash of a file that can be used to compare files.
   *
   * @return A cryptographic hash of the file for comparison use.
   */
  public String hashFile() {
    digestFile();
    byte[] resultantDigest = digester.digest();
    return HexBin.encode(resultantDigest);
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
      logger.fatal(e);
      throw new RuntimeException(e);
    }
  }
  
  private void digestFile() {
    long i = 0L;
    try {
      while (i * multiplier < fileToHash.length()) {
        logger.debug(String.format("Location: %d, expected next location %d",
            fileToHash.getFilePointer(), (i + 1) * multiplier));
        
        fileToHash.seek(i++ * multiplier);
        digester.update(fileToHash.readByte());
      }
    } catch (IOException e) {
      logger.fatal(e);
      throw new RuntimeException(e);
    }
  }
}
