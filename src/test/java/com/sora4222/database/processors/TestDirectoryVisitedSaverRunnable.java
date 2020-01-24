package com.sora4222.database.processors;

import com.sora4222.database.connectors.ConnectionStorage;
import com.sora4222.database.setup.processors.ConcurrentQueues;
import com.sora4222.database.setup.processors.DirectoryVisitedSaverRunnable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

public class TestDirectoryVisitedSaverRunnable {
  public static Logger logger = LogManager.getLogger();
  static final FileVisitor<Path> visitor = new FileVisitor<Path>() {
    
    @Override
    public FileVisitResult preVisitDirectory(Path directoryPath, BasicFileAttributes basicFileAttributes) {
      logger.info("Visited folder: " + directoryPath.toAbsolutePath().toString().replace("\\", "/"));
      
      return FileVisitResult.CONTINUE;
    }
    
    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) {
      logger.info("Visited file: " + path.toAbsolutePath().toString().replace("\\", "/"));
      
      return FileVisitResult.CONTINUE;
    }
    
    @Override
    public FileVisitResult visitFileFailed(Path path, IOException e) {
      return FileVisitResult.CONTINUE;
    }
    
    @Override
    public FileVisitResult postVisitDirectory(Path directoryPath, IOException e) {
      ConcurrentQueues.visitedDirectoriesQueue.add(directoryPath);
      return FileVisitResult.CONTINUE;
    }
  };
  
  @BeforeEach
  public void deleteDirectoriesInserted() throws SQLException {
    Connection conn = ConnectionStorage.getConnection();
    conn.prepareStatement("DELETE FROM directories_stored").execute();
    
    ConcurrentQueues.visitedDirectoriesQueue.clear();
  }
  
  @Test
  public void testWillNotAttemptToUploadWhenThereIsNoFile() throws InterruptedException, SQLException {
    DirectoryVisitedSaverRunnable directoryRunnable = new DirectoryVisitedSaverRunnable();
    Thread thread = new Thread(directoryRunnable);
    
    thread.start();
    TimeUnit.SECONDS.sleep(5);
    directoryRunnable.finishedProcessing();
    thread.join();
    
    Assertions.assertEquals(0, getCountDirectories());
  }
  
  @Test
  public void testWillUploadDirectoriesInOneRun() throws SQLException {
    DirectoryVisitedSaverRunnable directoryRunnable = new DirectoryVisitedSaverRunnable();
    
    ConcurrentQueues.visitedDirectoriesQueue.add(Paths.get(""));
    ConcurrentQueues.visitedDirectoriesQueue.add(Paths.get("aRandomFolder"));
    
    
    directoryRunnable.finishedProcessing();
    directoryRunnable.run();
    
    Assertions.assertEquals(2, getCountDirectories());
  }
  
  @Test
  public void testWillUploadInMultipleRuns() throws SQLException, InterruptedException {
    DirectoryVisitedSaverRunnable directoryRunnable = new DirectoryVisitedSaverRunnable();
    Thread thread = new Thread(directoryRunnable);
    
    ConcurrentQueues.visitedDirectoriesQueue.add(Paths.get(""));
    ConcurrentQueues.visitedDirectoriesQueue.add(Paths.get("aRandomFolder"));
    
    thread.start();
    TimeUnit.SECONDS.sleep(2);
    Assertions.assertEquals(2, getCountDirectories());
    
    ConcurrentQueues.visitedDirectoriesQueue.add(Paths.get("newFolder1"));
    ConcurrentQueues.visitedDirectoriesQueue.add(Paths.get("newFolder2"));
    
    directoryRunnable.finishedProcessing();
    thread.join();
    
    Assertions.assertEquals(4, getCountDirectories());
  }
  
  private int getCountDirectories() throws SQLException {
    Connection conn = ConnectionStorage.getConnection();
    PreparedStatement prepStatement = conn.prepareStatement("SELECT COUNT(*) AS count FROM directories_stored");
    ResultSet rs = prepStatement.executeQuery();
    Assertions.assertTrue(rs.next());
    return rs.getInt("count");
  }
  
}
