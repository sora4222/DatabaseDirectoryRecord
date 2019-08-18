package com.sora4222.database;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import lombok.Getter;


public class FileInformation {
  
  @Getter
  private final String fileName;
  
  @Getter private final Path fullLocation;
  @Getter private final String computerName;
  @Getter private final String fileHash;
  @Getter private final LocalDateTime creationTime;
  
  /**
   * Creates a FileInformation that will make it's own creation time,
   * with the rest of the properties passed to it.
   *
   * @param fileName     the name of the file without the location
   * @param fullLocation the full location that the file is stored at
   * @param computerName the computer name of the stored file
   * @param fileHash          the md5 of the file, used to compare files
   */
  public FileInformation(String fileName, String fullLocation, String computerName, String fileHash) {
    this.fileName = fileName;
    this.fullLocation = Paths.get(fullLocation).toAbsolutePath();
    this.computerName = computerName;
    this.creationTime = LocalDateTime.now();
    this.fileHash = fileHash;
  }
  
  /**
   * Creates a FileInformation object that will be passed in it's datetime. This is intended
   * to be used with the received FileInformation from database objects.
   *
   * @param fileName     the name of the file without the location
   * @param fullLocation the full location that the file is stored at
   * @param computerName the computer name of the stored file
   * @param fileHash          the md5 of the file, used to compare files
   * @param creationTime the time that the file was stored into the database
   */
  public FileInformation(String fileName, String fullLocation, String computerName, String fileHash,
                         LocalDateTime creationTime) {
    this.fileName = fileName;
    this.fullLocation = Paths.get(fullLocation).toAbsolutePath();
    this.computerName = computerName;
    this.fileHash = fileHash;
    this.creationTime = creationTime;
  }
  
  @Override
  public boolean equals(final Object obj) {
    if (!obj.getClass().equals(FileInformation.class)) {
      return false;
    }
    
    FileInformation otherFileInformation = (FileInformation) obj;
    boolean fileNameEquals = fileName.equals(otherFileInformation.getFileName());
    boolean fullLocationEquals = fullLocation.equals(otherFileInformation.getFullLocation());
    boolean md5Equals = fileHash.equals(otherFileInformation.getFileHash());
    
    return fileNameEquals && fullLocationEquals && md5Equals;
  }
}
