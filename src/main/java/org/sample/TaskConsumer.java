package org.sample;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

public class TaskConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(TaskConsumer.class);
    private DAO dao;

    public TaskConsumer(DAO dao) {
        this.dao = dao;
    }

    public void process() throws SQLException {
        LOG.info("Picking up available task ...");
        int taskId = dao.execute(connection -> {
            List<Integer> ids = dao.query("select id from task where status='PENDING' order by created_at limit 1 for update",
                    DAO.ProcessPreparedStatement.noop(),
                    rs -> rs.getInt(1),
                    connection);
            if (ids.size() == 0) {
                LOG.info("No available task!");
                return -1;
            }
            dao.modify("update task set status='RUNNING', started_at=current_timestamp, attempt=attempt+1 where id=?",
                    ps -> ps.setInt(1, ids.get(0)),
                    connection);
            LOG.info("Task id={} picked", ids.get(0));
            return ids.get(0);
        });
        if (taskId == -1) return;
        try {
            LOG.info("Task id={} processing ...", taskId);
            Thread.sleep(1000);
            if (Math.random() < 0.2) {
                // 20% failure
                throw new InterruptedException("Chaos failure");
            }
            LOG.info("Task id={} completed!", taskId);
            dao.modify("update task set status='COMPLETED', completed_at=current_timestamp where id=?",
                    ps -> ps.setInt(1, taskId));
        } catch (InterruptedException e) {
            LOG.error("Task id={} failed", taskId, e);
            dao.modify("update task set status='FAILED', completed_at=current_timestamp where id=?",
                    ps -> ps.setInt(1, taskId));
        }
    }
}
