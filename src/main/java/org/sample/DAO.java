package org.sample;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DAO {
    private static final Logger LOGGER = LoggerFactory.getLogger(DAO.class);
    private DataSource dataSource;

    public DAO(DataSource dataSource) {
        this.dataSource = dataSource;
        LOGGER.info("DAO created with dataSource={}", dataSource);
    }

    public <T> List<T> query(String sql, ProcessPreparedStatement processPreparedStatement,
                             ProcessResultSet<T> processResultSet) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            return query(sql, processPreparedStatement, processResultSet, connection);
        }
    }

    public <T> List<T> query(String sql, ProcessPreparedStatement processPreparedStatement,
                             ProcessResultSet<T> processResultSet, Connection connection) throws SQLException {
        List<T> results = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            processPreparedStatement.process(preparedStatement);

            long start = System.currentTimeMillis();
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                LOGGER.debug("Executing took executingTime={}ms for query", System.currentTimeMillis() - start);
                start = System.currentTimeMillis();
                while (resultSet.next()) {
                    final T processed = processResultSet.process(resultSet);
                    results.add(processed);
                }
                LOGGER.debug("Result took processingTime={}ms for query", System.currentTimeMillis() - start);
            }
        }
        return results;
    }

    public <T> T queryForObject(String sql, ProcessPreparedStatement processPreparedStatement,
                                ProcessResultSet<T> processResultSet) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            return queryForObject(sql, processPreparedStatement, processResultSet, connection);
        }
    }

    public <T> T queryForObject(String sql, ProcessPreparedStatement processPreparedStatement,
                                ProcessResultSet<T> processResultSet, Connection connection) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            processPreparedStatement.process(preparedStatement);

            long start = System.currentTimeMillis();
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                LOGGER.debug("Executing took executingTime={}ms for queryForObject", System.currentTimeMillis() - start);
                start = System.currentTimeMillis();
                if (resultSet.next()) {
                    final T rs = processResultSet.process(resultSet);
                    LOGGER.debug("Result took processingTime={}ms for queryForObject", System.currentTimeMillis() - start);
                    return rs;
                }
            }
        }
        return null;
    }

    public int modify(String sql, ProcessPreparedStatement processPreparedStatement) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            return modify(sql, processPreparedStatement, connection);
        }
    }

    public int modify(String sql, ProcessPreparedStatement processPreparedStatement, Connection connection) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            processPreparedStatement.process(preparedStatement);
            long start = System.currentTimeMillis();
            final int i = preparedStatement.executeUpdate();
            LOGGER.debug("Executing took executingTime-{}ms for modify", System.currentTimeMillis() - start);
            return i;
        }
    }

    public int[] modifyBatch(String sql, ProcessPreparedStatement processPreparedStatement, Connection connection) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            processPreparedStatement.process(preparedStatement);
            long start = System.currentTimeMillis();
            final int[] ints = preparedStatement.executeBatch();
            LOGGER.debug("Executing took executingTime={}ns for modifyBatch", System.currentTimeMillis() - start);
            return ints;
        }
    }

    public void execute(ConnectionConsumer connectionConsumer) throws SQLException {
        execute(connection -> {
            connectionConsumer.process(connection);
            return null;
        });
    }

    public <T> T execute(ConnectionFunction<T> connectionFunction) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            try {
                connection.setAutoCommit(false);
                final T ob = connectionFunction.process(connection);
                connection.commit();
                return ob;
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    @FunctionalInterface
    public interface ProcessPreparedStatement {
        void process(PreparedStatement ps) throws SQLException;

        static ProcessPreparedStatement noop() {
            return ps -> {
            };
        }
    }

    @FunctionalInterface
    public interface ProcessResultSet<R> {
        R process(ResultSet rs) throws SQLException;
    }

    @FunctionalInterface
    public interface ConnectionConsumer {
        void process(Connection connection) throws SQLException;
    }

    @FunctionalInterface
    public interface ConnectionFunction<R> {
        R process(Connection connection) throws SQLException;
    }
}
