package com.sora4222.database;

import com.sora4222.file.FileInformation;
import lombok.Getter;
import lombok.Setter;

public class FileCommand {
  @Getter @Setter
  FileInformation information;
  @Getter @Setter
  DatabaseCommand command;
  
  public FileCommand(FileInformation information, DatabaseCommand command) {
    this.command = command;
    this.information = information;
  }
  
  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof FileCommand))
      return false;
    else {
      FileCommand otherObj = (FileCommand)obj;
      return command.equals(otherObj.command) && information.equals(otherObj.information);
    }
  }
}
