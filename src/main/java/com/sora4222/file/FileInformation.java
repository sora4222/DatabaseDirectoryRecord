package com.sora4222.file;

import lombok.Getter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;


public class FileInformation {
  public static final FileInformation EmptyFileInformation = new FileInformation(Paths.get(""));
  @Getter private final Path fullLocation;
  @Getter private final String fileHash;
  @Getter private final LocalDateTime creationTime;
  
  /**
   * Creates a FileInformation that will make it's own creation time,
   * with the rest of the properties passed to it.
   *
   * @param fullLocation the full location that the file is stored at
   * @param fileHash     the md5 of the file, used to compare files
   */
  public FileInformation(String fullLocation, String fileHash) {
    this.fullLocation = Paths.get(fullLocation).toAbsolutePath();
    this.fileHash = fileHash;
  
    this.creationTime = LocalDateTime.now();
  }
  
  public String getFullLocationAsLinuxBasedString(){
    return fullLocation.toString().replace("\\", "/");
  }
  
  /**
   * Creates a FileInformation that will make it's own creation time,
   * with the rest of the properties passed to it.
   *
   * @param fullLocation the full location that the file is stored at
   * @param fileHash          the md5 of the file, used to compare files
   */
  public FileInformation(Path fullLocation, String fileHash) {
    this.fullLocation = fullLocation.toAbsolutePath();
    this.fileHash = fileHash;
  
    this.creationTime = LocalDateTime.now();
  }
  
  public FileInformation(Path fullLocation) {
    this(fullLocation, "");
  }
  
  
  /**
   * Creates a FileInformation object that will be passed in it's datetime. This is intended
   * to be used with the received FileInformation from database objects.
   *
   * @param fullLocation the full location that the file is stored at
   * @param fileHash the hash of the file, used to compare files
   * @param creationTime the time that the file was stored into the database
   */
  public FileInformation(String fullLocation, String fileHash,
                         LocalDateTime creationTime) {
    this.fullLocation = Paths.get(fullLocation).toAbsolutePath();
    this.fileHash = fileHash;
    this.creationTime = creationTime;
  }
  
  /**
   * Checks the information on the files is equal
   * Ignores time and computer location
   */
  @Override
  public boolean equals(final Object obj) {
    if (!obj.getClass().equals(FileInformation.class)) {
      return false;
    }

    FileInformation otherFileInformation = (FileInformation) obj;
    boolean fullLocationEquals = fullLocation.equals(otherFileInformation.getFullLocation());
    boolean md5Equals = fileHash.equals(otherFileInformation.getFileHash());

    return fullLocationEquals && md5Equals;
  }

  @Override
  public String toString() {
    return String.format("Path:%s, File hash: %s",
        fullLocation.toString(), fileHash);
  }
  
  @Override
  public int hashCode() {
    int hash = 7;
    hash = 13 * hash + fileHash.hashCode();
    hash = 13 * hash + fullLocation.hashCode();
    return hash;
  }
}
