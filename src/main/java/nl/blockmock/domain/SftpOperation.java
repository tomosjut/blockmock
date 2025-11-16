package nl.blockmock.domain;

public enum SftpOperation {
    UPLOAD,    // Client uploads file to server
    DOWNLOAD,  // Client downloads file from server
    LIST,      // Client lists directory contents
    DELETE,    // Client deletes file from server
    MKDIR,     // Client creates directory
    RMDIR      // Client removes directory
}
