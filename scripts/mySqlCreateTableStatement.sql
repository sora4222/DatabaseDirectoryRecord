# CREATE TABLE directory_records(
#     FilePath varchar(2500),
#     FileHash varchar(100),
#     ComputerName varchar(300),
#     DatabaseRowCreationTime datetime default CURRENT_TIMESTAMP,
#     PRIMARY KEY(FilePath, ComputerName)
# );
# The script above is preferable but will result in: Specified key was too long; max key length is 3072 bytes
CREATE DATABASE directory_records_db;
USE directory_records_db;
CREATE TABLE directory_records_db.directory_records
(
    FilePath                varchar(600),
    FileHash                varchar(100),
    ComputerName            varchar(100),
    DatabaseRowCreationTime datetime default CURRENT_TIMESTAMP,
    PRIMARY KEY (FilePath, ComputerName)
);