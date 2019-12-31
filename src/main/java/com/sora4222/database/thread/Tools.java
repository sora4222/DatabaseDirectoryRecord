package com.sora4222.database.thread;

import com.sora4222.file.FileInformation;

import java.util.concurrent.ConcurrentLinkedQueue;

public class Tools {
  public static final ConcurrentLinkedQueue<FileInformation> hardDriveSetupQueue
    = new ConcurrentLinkedQueue<>();
}
