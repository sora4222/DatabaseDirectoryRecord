package com.sora4222.database;

import com.sora4222.file.FileInformation;

import java.util.List;

/**
 * All classes inheriting from this are expected to be able to:
 *      * Query the database connected for a single file name
 *      * Send to the database a file it's location, and the name of it's computer.
 *      * Send the current computers name or identifier
 */
public interface DatabaseWrapper {
    /**
     * Checks the database for a file's properies, returning the
     * location of the file, the name of the device, a hash
     * and the name as a {@link FileInformation}.
     * @return a {@link FileInformation} containing the file's information.
     */
    List<FileInformation> checkForFile(FileInformation fileInformation);
    
    /**
     * Sends file information to the database that is declared.
     * @param infoToSend contains the information of the file to send
     * @return A true value when the file has been sent successfully a false otherwise
     */
    boolean insertFile(FileInformation infoToSend);
    
    boolean deleteFileRow(FileInformation fileToDelete);
    
    /**
     * Updates a database row to the new file information data
     * @param fileToUpdate Contains the information of the updated file.
     * @return A true value when the file has been sent successfully a false otherwise
     */
    boolean updateFileRow(FileInformation fileToUpdate);
    
    /**
     * Used to give the current computer name for the use of the database to
     * keep track of active computers and their files.
     * @param computerName Computer name that will be reported.
     * @return A true value when the file has been sent successfully a false otherwise
     */
    boolean currentComputerName(String computerName);
}
