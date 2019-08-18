package com.sora4222.file;

import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileHasher {
  MessageDigest digest;
  private static Logger logger = LogManager.getLogger();
  
  public FileHasher() {
    try {
      digest = MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException e) {
      logger.fatal(e.getMessage());
      throw new RuntimeException(e);
    }
  }
  
  /**
   * Gets a cryptographic hash of a file that can be used to compare files.
   * @param fileToHash
   * @return
   */
  public String hashFile(final File fileToHash) {
    InputStream fileStream = getInputStream(fileToHash);
    DigestInputStream digestStream = new DigestInputStream(fileStream, digest);
    DigestInputStream resultStream = readDigestStream(digestStream);
    byte[] resultantDigest = resultStream.getMessageDigest().digest();
    return HexBin.encode(resultantDigest);
  }
  
  private DigestInputStream readDigestStream(final DigestInputStream fileStream) {
    while (true) {
      try {
        if (fileStream.read() == -1) {
          break;
        }
      } catch (IOException e) {
        logger.fatal(e);
        throw new RuntimeException(e);
      }
    }
    return fileStream;
  }
  
  private InputStream getInputStream(final File fileToHash) {
    try {
      return new FileInputStream(fileToHash);
    } catch (FileNotFoundException e) {
      logger.fatal(String.format("FileNotFoundException, the file %s, reports an error:%s",
          fileToHash, e.getMessage()));
      throw new RuntimeException(e);
    }
  }
}
