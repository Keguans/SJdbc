package com.SJdbc.util;

import cn.hutool.core.util.IdUtil;
import com.SJdbc.annotation.Key;
import com.SJdbc.enums.IdTypeEnum;
import com.SJdbc.enums.SqlEnum;
import com.SJdbc.model.Paging;
import com.SJdbc.model.QueryModel;
import com.SJdbc.model.TableData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementCreatorFactory;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.*;

public class JdbcUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcUtil.class);

    private final JdbcTemplate jdbcTemplate;

    public JdbcUtil(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * get JdbcTemplate
     *
     * @return
     */
    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    /**
     * get by @Key
     *
     * @param id
     * @param aClass
     * @param fields
     * @return
     * @param <T>
     */
    public  <T> T getById(Object id, Class<T> aClass, String... fields) {
        // 获取实体类字段信息
        TableData tableData = TableData.get(aClass);
        Map<String, String> columMap = tableData.getColumMap();
        if (columMap.isEmpty()) {
            return null;
        }
        // 查询制定字段
        if (fields.length > 0) {
            List<String> fieldList = Arrays.asList(fields);
            List<String> removeList = new ArrayList<>();
            for (String key : columMap.keySet()) {
                if (!fieldList.contains(key)) {
                    removeList.add(key);
                }
            }
            removeList.forEach(columMap::remove);
        }
        // 拼装 sql 语句 -- 主键
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(tableData.getKey());
        if (!Objects.equals(tableData.getKey(), tableData.getEntityKey())) {
            sql.append(" AS ").append(tableData.getEntityKey());
        }
        sql.append(", ");
        // 拼装 sql 语句 -- 其他字段
        for (String tableColum : columMap.keySet()) {
            sql.append(tableColum);
            if (!Objects.equals(tableColum, columMap.get(tableColum))) {
                sql.append(" AS ").append(columMap.get(tableColum));
            }
            sql.append(", ");
        }
        sql.deleteCharAt(sql.length() - 2);
        // 拼装 sql 语句 -- 表名
        sql.append(" FROM ").append(tableData.getTableName());
        // 拼装 sql 语句 -- 条件
        sql.append(" WHERE ").append(tableData.getKey()).append(" = ")
                .append(convert(id));
        // 执行 sql 语句
        return jdbcTemplate.queryForObject(sql.toString(), new BeanPropertyRowMapper<>(aClass));
    }

    /**
     * count
     *
     * @param queryModel
     * @return
     */
    public Long count(QueryModel queryModel) {
        Long count = jdbcTemplate.queryForObject(queryModel.sql.toString(), Long.class);
        return Objects.isNull(count) || count == 0 ? 0 : count;
    }

    /**
     * 查询一个
     *
     * @param queryModel
     * @return
     * @param <T>
     */
    public <T> T getOne(QueryModel queryModel, Class<T> aClass) {
        String sql = queryModel.sql.toString();
        sql = sql + " " + SqlEnum.LIMIT + " 1";
        try {
            return jdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper<>(aClass));
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    /**
     * 查询列表
     *
     * @param queryModel
     * @param <T>
     * @return
     */
    public <T> List<T> getList(QueryModel queryModel, Class<T> aClass) {
        return jdbcTemplate.query(queryModel.sql.toString(), new BeanPropertyRowMapper<>(aClass));
    }

    /**
     * 分页查询
     *
     * @param aClass
     * @return
     * @param <T>
     */
    public <T> Paging<T> paging(Paging.Page page, Class<T> aClass, String... colum) {
        QueryModel queryModel = new QueryModel(aClass);
        queryModel.select().count().from();
        Long count = this.count(queryModel);
        if (Objects.isNull(count) || count == 0) {
            return new Paging<>(0L, new ArrayList<>());
        }
        queryModel.clearSql();
        int pageNum = (page.getPageNum() - 1) * page.getPageSize();
        queryModel.select().table(colum).from().limit(pageNum, page.getPageSize());
        List<T> list = this.getList(queryModel, aClass);
        return new Paging<>(count, list);
    }

    /**
     * 分页查询
     *
     * @param queryModel
     * @param page
     * @param aClass
     * @return
     * @param <T>
     */
    public <T> Paging<T> paging(QueryModel queryModel, Paging.Page page, Class<T> aClass) {
        String substring = queryModel.sql.substring(
                queryModel.sql.indexOf(SqlEnum.SELECT.getWord()) + SqlEnum.SELECT.getWord().length(),
                queryModel.sql.indexOf(SqlEnum.FROM.getWord())
        );
        String sql = queryModel.sql.toString().replaceFirst(substring, " " + SqlEnum.COUNT + "(1) ");
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        if (Objects.isNull(count) || count == 0) {
            return new Paging<>(0L, new ArrayList<>(0));
        }
        int pageNum = (page.getPageNum() - 1) * page.getPageSize();
        sql = queryModel.sql + " " + SqlEnum.LIMIT + " " + pageNum + ", " + page.getPageSize();
        List<T> list = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(aClass));
        return new Paging<>(count, list);
    }

    /**
     * insert
     *
     * @param obj
     * @return
     */
    public int insert(Object obj) {
        TableData tableData = this.tableData(obj);
        if (Objects.isNull(tableData)) {
            return 0;
        }
        tableData.getCreateTime(obj);
        Map<String, String> columMap = tableData.getColumMap();
        Map<String, Object> dataMap = tableData.getDataMap();
        // 拼装 sql 语句 -- 字段
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(tableData.getTableName()).append(" (");
        // 主键
        Object entityKey = dataMap.get(tableData.getEntityKey());
        if (Objects.nonNull(entityKey)) {
            sql.append(tableData.getEntityKey()).append(", ");
        }
        for (String colum : columMap.keySet()) {
            // 主键不写入 sql
            if (colum.equals(tableData.getKey())) {
                continue;
            }
            // 如果此字段值为空，不写入 sql
            if (Objects.isNull(dataMap.get(columMap.get(colum)))) {
                continue;
            }
            sql.append(colum).append(", ");
        }
        sql.deleteCharAt(sql.length() - 2).append(")").append(" VALUES (");
        // 拼装 sql 语句 -- 值
        if (Objects.nonNull(entityKey)) {
            sql.append(convert(entityKey)).append(", ");
        }
        for (String colum : columMap.keySet()) {
            Object data = dataMap.get(columMap.get(colum));
            if (Objects.nonNull(data)) {
                sql.append(convert(data)).append(", ");
            }
        }
        sql.deleteCharAt(sql.length() - 2).append(")");
        int count = 0;
        try {
            String sqlStr = sql.toString();
            // 根据 id 类型 获取 id 值
            Key key = obj.getClass().getDeclaredField(tableData.getKey()).getAnnotation(Key.class);
            if (key.type().equals(IdTypeEnum.ID_ASSIGN)) {
                // 如果是自动生成 id
                Object id = IdUtil.getSnowflake(1, 1).nextId();
                sqlStr = sqlStr
                        .replace(tableData.getTableName() + " (", tableData.getTableName() + " (" + tableData.getKey() + ", ")
                        .replace("VALUES (", "VALUES (" + id + ", ");
            }
            KeyHolder generatedKeyHolder = new GeneratedKeyHolder();
            PreparedStatementCreatorFactory factory = new PreparedStatementCreatorFactory(sqlStr);
            factory.setReturnGeneratedKeys(true);
            PreparedStatementCreator preparedStatementCreator = factory.newPreparedStatementCreator(new String[]{});
            count = jdbcTemplate.update(preparedStatementCreator, generatedKeyHolder);
            Field field = obj.getClass().getDeclaredField(tableData.getEntityKey());
            field.setAccessible(true);
            Number number = generatedKeyHolder.getKey();
            if (Objects.nonNull(number)) {
                Class<?> type = field.getType();
                field.set(obj, type.equals(Long.class) ? number.longValue() : (type.equals(Integer.class) ? number.intValue() : number.toString()));
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return count;
    }

    /**
     * 批量保存
     *
     * @param list
     * @return
     */
    public <T> int insertBatch(List<T> list) {
        List<TableData> tableDataList = new ArrayList<>(list.size());
        list.forEach(o -> {
            TableData tableData = this.tableData(o);
            if (Objects.nonNull(tableData)) {
                tableData.getCreateTime(o);
                tableDataList.add(tableData);
            }
        });
        if (tableDataList.isEmpty()) {
            return 0;
        }
        // 拼装 sql 语句 -- 字段
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(tableDataList.get(0).getTableName()).append(" (");
        Map<String, String> columMap = tableDataList.get(0).getColumMap();
        for (String colum : columMap.keySet()) {
            sql.append(colum).append(", ");
        }
        sql.deleteCharAt(sql.length() - 1).deleteCharAt(sql.length() - 1).append(")");
        // 拼装 sql 语句 -- 值
        int count = 0;
        sql.append(" VALUES");
        try {
            List<Object> keyList = new ArrayList<>(list.size());
            // 拼装值
            Key key = list.get(0).getClass().getDeclaredField(tableDataList.get(0).getKey()).getAnnotation(Key.class);
            for (TableData tableData : tableDataList) {
                sql.append("(");
                Map<String, Object> dataMap = tableData.getDataMap();
                for (String columKey : columMap.keySet()) {
                    Object o = dataMap.get(columKey);
                    if (columKey.equals(tableData.getKey())) {
                        // 自动生成主键
                        o = null;
                        if (key.type().equals(IdTypeEnum.ID_ASSIGN)) {
                            o = IdUtil.getSnowflake(1, 1).nextId();
                            keyList.add(o);
                        }
                    }
                    sql.append(convert(o)).append(", ");
                }
                sql.deleteCharAt(sql.length() - 1).deleteCharAt(sql.length() - 1).append("), ");
            }
            sql.deleteCharAt(sql.length() - 1).deleteCharAt(sql.length() - 1);
            // 自增
            if (key.type().equals(IdTypeEnum.ID_AUTO)) {
                KeyHolder generatedKeyHolder = new GeneratedKeyHolder();
                PreparedStatementCreatorFactory factory = new PreparedStatementCreatorFactory(sql.toString());
                factory.setReturnGeneratedKeys(true);
                PreparedStatementCreator preparedStatementCreator = factory.newPreparedStatementCreator(new String[]{});
                count = jdbcTemplate.update(preparedStatementCreator, generatedKeyHolder);
                List<Map<String, Object>> keyHolderKeyList = generatedKeyHolder.getKeyList();
                keyHolderKeyList.forEach(m -> m.keySet().forEach(k -> keyList.add(m.get(k))));
            } else {
                count = jdbcTemplate.update(sql.toString());
            }
            // id 赋值
            for (int i = 0; i < list.size(); i++) {
                Field field = list.get(i).getClass().getDeclaredField(tableDataList.get(0).getKey());
                field.setAccessible(true);
                Class<?> type = field.getType();
                field.set(list.get(i), type.equals(Long.class) ? Long.parseLong(keyList.get(i).toString()) : (type.equals(Integer.class) ? Integer.parseInt(keyList.get(i).toString()) : keyList.get(i).toString()));
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return count;
    }

    /**
     * 更新
     *
     * @param obj
     */
    public int update(Object obj) {
        TableData tableData = this.tableData(obj);
        if (Objects.isNull(tableData)) {
            return 0;
        }
        tableData.getUpdateTime(obj);
        // 拼装 sql
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ").append(tableData.getTableName()).append(" SET ");
        for (String colum : tableData.getColumMap().keySet()) {
            Object data = tableData.getDataMap().get(tableData.getColumMap().get(colum));
            if (Objects.isNull(data)) {
                continue;
            }
            sql.append(colum).append(" = ").append(convert(data)).append(", ");
        }
        sql.deleteCharAt(sql.length() - 2);
        // 拼装 sql -- 主键
        sql.append("WHERE ").append(tableData.getKey()).append(" = ");
        Object id = tableData.getDataMap().get(tableData.getEntityKey());
        sql.append(convert(id));
        return jdbcTemplate.update(sql.toString());
    }

    /**
     * 获取 table 信息
     *
     * @return
     */
    private TableData tableData(Object obj) {
        // 获取实体类字段信息
        TableData tableData = TableData.get(obj.getClass());
        Map<String, String> columMap = tableData.getColumMap();
        if (columMap.isEmpty()) {
            return null;
        }
        // 获取字段值
        tableData.objData(obj);
        Map<String, Object> dataMap = tableData.getDataMap();
        if (dataMap.isEmpty()) {
            return null;
        }
        return tableData;
    }

    /**
     * 通过 id 删除
     *
     * @param id
     * @param aClass
     * @return
     */
    public <T> int delete(Object id, Class<T> aClass) {
        // 获取 table 信息
        TableData tableData = TableData.get(aClass);
        Map<String, String> columMap = tableData.getColumMap();
        if (columMap.isEmpty()) {
            return 0;
        }
        // 拼装 sql
        String sql = "DELETE FROM " + tableData.getTableName() +
                " WHERE " + tableData.getKey() + " = " +
                (id instanceof String ? "\"" + id + "\"" : id);
        return jdbcTemplate.update(sql);
    }

    /**
     * 统一字段处理
     *
     * @param obj
     * @return
     */
    public static Object convert(Object obj) {
        if (Objects.isNull(obj)) {
            return null;
        }
        // 字符串
        if (obj instanceof String) {
            String str = (String) obj;
            str = str.replace("\"", "\\\"");
            return  "\"" + str + "\"";
        }
        // 日期
        if (obj instanceof Date) {
            obj = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Date) obj);
            return "\"" + obj + "\"";
        }
        return obj;
    }
}
