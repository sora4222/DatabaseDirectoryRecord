CREATE SCHEMA IF NOT EXISTS directory_records_db;
USE directory_records_db;

DROP TABLE IF EXISTS directory_records;
DROP TABLE IF EXISTS directories_stored;
DROP TABLE IF EXISTS computer_names;
DROP TABLE IF EXISTS file_paths;

CREATE TABLE IF NOT EXISTS file_paths
(
    FileId           int AUTO_INCREMENT,
    AbsoluteFilePath varchar(2500),
    PRIMARY KEY (FileId)
);

CREATE TABLE IF NOT EXISTS computer_names
(
    ComputerId   int AUTO_INCREMENT,
    ComputerName varchar(300),
    PRIMARY KEY (ComputerId)
);

CREATE TABLE IF NOT EXISTS directories_stored
(
    DirectoryId      int AUTO_INCREMENT,
    ComputerId       int,
    AbsoluteFilePath varchar(2500),
    PRIMARY KEY (DirectoryId)
);

CREATE TABLE IF NOT EXISTS directory_records
(
    FileId                  int,
    FileHash                varchar(100),
    ComputerId              int,
    DatabaseRowCreationTime datetime default CURRENT_TIMESTAMP,
    PRIMARY KEY (FileId, ComputerId),
    FOREIGN KEY (FileId) REFERENCES file_paths (FileId),
    FOREIGN KEY (ComputerId) REFERENCES computer_names (ComputerId)
);