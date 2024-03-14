-- init.sql
ALTER
SYSTEM SET wal_level = logical;

-- Create the base table: record_execution
CREATE TABLE IF NOT EXISTS record_execution
(
    dataset_id VARCHAR
(
    255
) NOT NULL,
    execution_id VARCHAR
(
    255
) NOT NULL,
    execution_name VARCHAR
(
    255
) NOT NULL,
    record_id VARCHAR
(
    255
) NOT NULL,
    record_data TEXT NOT NULL,
    execution_parameters JSON,
    PRIMARY KEY
(
    dataset_id,
    execution_id,
    record_id
) -- Define a composite primary key
    );

CREATE TABLE IF NOT EXISTS execution_status
(
    execution_id VARCHAR
(
    255
),
    status VARCHAR
(
    255
),
    PRIMARY KEY
(
    execution_id
)
    );


-- Create table record_execution_exception with a foreign key reference to record_execution
CREATE TABLE IF NOT EXISTS record_execution_exception
(
    dataset_id VARCHAR
(
    255
) NOT NULL,
    execution_id VARCHAR
(
    255
) NOT NULL,
    record_id VARCHAR
(
    255
) NOT NULL,
    execution_name VARCHAR
(
    255
),
    exception_name VARCHAR
(
    255
) NOT NULL,
    exception_content TEXT NOT NULL,
    PRIMARY KEY
(
    dataset_id,
    execution_id,
    record_id
)
    );

-- Create table record_execution_result with a foreign key reference to record_execution
CREATE TABLE IF NOT EXISTS record_execution_result
(
    dataset_id VARCHAR
(
    255
) NOT NULL,
    execution_id VARCHAR
(
    255
) NOT NULL,
    record_id VARCHAR
(
    255
) NOT NULL,
    record_result_data TEXT NOT NULL,
    execution_name VARCHAR
(
    255
),
    PRIMARY KEY
(
    dataset_id,
    execution_id,
    record_id
)
    );
