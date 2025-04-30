package com.dbsync.dbsync.config;

public class Config {
    public static final String ORACLE_URL = "jdbc:oracle:thin:@192.168.107.101:1525/orcl";
    public static final String ORACLE_USER = "PT1_ECI_CQDM";
    public static final String ORACLE_PASSWORD = "ecidh.com2024";
    public static final String ORACLE_TABLE_NAME = "SYS_DATA_HELP";

    public static final String POSTGRES_URL = "jdbc:postgresql://192.168.106.103:5432/pt1_eci_cqdm";
    public static final String POSTGRES_USER =  "cqdm_basic";
    public static final String POSTGRES_PASSWORD = "cqdm_basic_1qaz";
    public static final String POSTGRES_TABLE_NAME =  "SYS_DATA_HELP1";

    public static final int BATCH_SIZE = 5000;
}