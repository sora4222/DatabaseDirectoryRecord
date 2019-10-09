package com.sora4222.database;

import com.sora4222.file.FileInformation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class FileCommandTest {
  
  @Test
  public void initialization() {
    FileInformation info = new FileInformation("/a/name.txt", "host", "a12fe");
    FileCommand fileCommand = new FileCommand(info, DatabaseCommand.Insert);
  }
  
  @Test
  public void testEquals() {
    FileInformation info1 = new FileInformation("/a/name.txt", "host", "a12fe");
    FileInformation info2 = new FileInformation("/a/name.txt", "host", "a12fe");
    FileCommand fileCommand1 = new FileCommand(info1, DatabaseCommand.Insert);
    FileCommand fileCommand2 = new FileCommand(info2, DatabaseCommand.Insert);
    Assertions.assertEquals(fileCommand1, fileCommand2);
    
    fileCommand2.setCommand(DatabaseCommand.Update);
    Assertions.assertNotEquals(fileCommand1, fileCommand2);
    FileInformation info3 = new FileInformation("/a/name.txt", "host", "bc19e");
    FileCommand fileCommand3 = new FileCommand(info3, DatabaseCommand.Update);
    
    Assertions.assertNotEquals(fileCommand2, fileCommand3);
    
    Assertions.assertNotEquals(fileCommand1, "name.txt");
  }
  
}