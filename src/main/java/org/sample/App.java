package org.sample;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class App {
    private static final Logger LOG = LoggerFactory.getLogger(App.class);
    static final String JDBC_DRIVER = "org.h2.Driver";
    static final String DB_URL = "jdbc:h2:~/db_task_queue";
    static final String USER = "my_user";
    static final String PASS = "mypassword123!";
    static final String MIGRATION_LOCATION = "migration";

    private final ScheduledExecutorService taskProducerScheduler = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService taskConsumerScheduler_1 = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService taskConsumerScheduler_2 = Executors.newSingleThreadScheduledExecutor();
    private final DAO dao;
    private final TaskProducer taskProducer;
    private final TaskConsumer taskConsumer;

    public static void main(String[] args) {
        App app = new App();
        app.start();
    }

    public App() {
        // Setup database connection
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL(DB_URL);
        ds.setUser(USER);
        ds.setPassword(PASS);
        dao = new DAO(ds);
        taskProducer = new TaskProducer(dao);
        taskConsumer = new TaskConsumer(dao);
    }

    private void flyway() {
        Flyway flyway = Flyway.configure()
                .locations(MIGRATION_LOCATION)
                .dataSource(DB_URL, USER, PASS)
                .validateOnMigrate(false)
                .baselineOnMigrate(true)
                .sqlMigrationPrefix("0")
                .load();
        flyway.migrate();
    }

    private void start() {
        flyway();
        taskProducerScheduler.scheduleWithFixedDelay(() -> {
            try {
                taskProducer.createTasks();
            } catch (SQLException e) {
                LOG.error("Failed to produce tasks", e);
            }
        }, 5, 300, TimeUnit.SECONDS);
        taskConsumerScheduler_1.scheduleWithFixedDelay(() -> {
            try {
                taskConsumer.process();
            } catch (SQLException e) {
                LOG.error("Failed to process tasks", e);
            }
        }, 10, 10, TimeUnit.SECONDS);
        taskConsumerScheduler_2.scheduleWithFixedDelay(() -> {
            try {
                taskConsumer.process();
            } catch (SQLException e) {
                LOG.error("Failed to process tasks", e);
            }
        }, 20, 10, TimeUnit.SECONDS);
    }
}
