package org.sample;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

public class TaskProducer {
    private static final Logger LOG = LoggerFactory.getLogger(TaskProducer.class);
    private DAO dao;

    public TaskProducer(DAO dao) {
        this.dao = dao;
    }

    public void createTasks() throws SQLException {
        LOG.info("Creating 10 pending tasks ...");
        for (int i = 0; i < 10; i++) {
            dao.modify("insert into task (status) values ('PENDING')",
                    DAO.ProcessPreparedStatement.noop());
        }
        LOG.info("Created 10 pending tasks!");
    }
}
