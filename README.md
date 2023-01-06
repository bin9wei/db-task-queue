# db-task-queue
Demo for task queue with database

## How to run locally
Run `App` from your IDE

## How it works
It creates h2 database under `~/db_task_queue` and run DDL `0001__init_schema.sql`


`TaskProducer`
* Periodically creates PENDING tasks

`TaskConsumer`
* Periodically picks a PENDING task
* Updates task status to RUNNING
* Sleep 1s to simulate processing, with 20% chaos
* Updates task status to COMPLETED. Updates task status to FAILED if task process interrupted
