CREATE SCHEMA IF NOT EXISTS directory_records_db;
USE directory_records_db;

DROP TABLE IF EXISTS directory_records;
DROP TABLE IF EXISTS computer_names;
DROP TABLE IF EXISTS file_paths;

CREATE TABLE IF NOT EXISTS file_paths
(
    FileIdNumber     int AUTO_INCREMENT,
    AbsoluteFilePath varchar(2500),
    PRIMARY KEY (FileIdNumber)
);

CREATE TABLE IF NOT EXISTS computer_names
(
    ComputerIdNumber int AUTO_INCREMENT,
    ComputerName     varchar(300),
    PRIMARY KEY (ComputerIdNumber)
);

CREATE TABLE IF NOT EXISTS directory_records
(
    FileNumber              int,
    FileHash                varchar(100),
    ComputerIdNumber        int,
    DatabaseRowCreationTime datetime default CURRENT_TIMESTAMP,
    PRIMARY KEY (FileNumber, ComputerIdNumber),
    FOREIGN KEY (FileNumber) REFERENCES file_paths (FileId),
    FOREIGN KEY (ComputerIdNumber) REFERENCES computer_names (ComputerId)
);