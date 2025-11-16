package nl.blockmock.domain;

public enum SqlDatabaseType {
    GENERIC,      // Generic SQL (H2 default mode)
    POSTGRESQL,   // PostgreSQL compatibility mode
    MYSQL,        // MySQL compatibility mode
    SQL_SERVER,   // SQL Server compatibility mode
    ORACLE        // Oracle compatibility mode
}
