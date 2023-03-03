package com.SJdbc.model;

import com.SJdbc.annotation.*;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

public class TableData {

    public TableData() {
        super();
    }

    /**
     * 表名
     */
    private String tableName;

    /**
     * 主键字段名
     */
    private String key;

    /**
     * 实体类主键字段名
     */
    private String entityKey;

    /**
     * 其他的字段
     * key: 数据库字段
     * value: 实体类字段
     */
    private Map<String, String> columMap;

    /**
     * 字段值
     * key: 实体类字段
     * value: 值
     */
    private Map<String, Object> dataMap;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getEntityKey() {
        return entityKey;
    }

    public void setEntityKey(String entityKey) {
        this.entityKey = entityKey;
    }

    public Map<String, String> getColumMap() {
        return columMap;
    }

    public void setColumMap(Map<String, String> columMap) {
        this.columMap = columMap;
    }

    public Map<String, Object> getDataMap() {
        return dataMap;
    }

    public void setDataMap(Map<String, Object> dataMap) {
        this.dataMap = dataMap;
    }

    /**
     * 转化为 TableData
     *
     * @param aClass
     * @return
     */
    public static TableData get(Class<?> aClass) {
        TableData tableData = new TableData();
        // 获取表名
        Table table = aClass.getAnnotation(Table.class);
        tableData.setTableName(
                Objects.nonNull(table) && StringUtils.hasText(table.name()) ?
                        table.name() :
                        aClass.getName()
        );
        // 主键
        Stream.of(aClass.getDeclaredFields())
                .filter(e -> Objects.nonNull(e.getAnnotation(Key.class)))
                .findFirst()
                .map(e -> {
                    Key key = e.getAnnotation(Key.class);
                    tableData.setKey(StringUtils.hasText(key.column()) ? key.column() : e.getName());
                    tableData.setEntityKey(e.getName());
                    return e;
                }).orElseThrow(
                        () -> new RuntimeException("实体类不可没有主键")
                );
        // 其他字段
        Map<String, String> columMap = new LinkedHashMap<>();
        for (Field declaredField : aClass.getDeclaredFields()) {
            String tableField;
            Column column = declaredField.getAnnotation(Column.class);
            if (Objects.isNull(column)) {
                tableField =  declaredField.getName();
            } else {
                tableField = StringUtils.hasText(column.filed()) ? column.filed() : declaredField.getName();
            }
            columMap.put(tableField, declaredField.getName());
        }
        tableData.setColumMap(columMap);
        return tableData;
    }

    /**
     * 为 dataMap 赋值
     *
     * @param obj
     */
    public void objData(Object obj) {
        try {
            Class<?> aClass = obj.getClass();
            Field[] fields = aClass.getDeclaredFields();
            Map<String, Object> dataMap = new HashMap<>(fields.length);
            for (Field field : fields) {
                // 设置可访问属性
                field.setAccessible(true);
                // 设置值
                Object data = field.get(obj);
                if (Objects.nonNull(data)) {
                    dataMap.put(field.getName(), data);
                }
            }
            this.dataMap = dataMap;
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    /**
     * 创建时自动更新的时间字段
     *
     * @param obj
     */
    public void getCreateTime(Object obj) {
        for (Field field : obj.getClass().getDeclaredFields()) {
            CreateTime createTime = field.getDeclaredAnnotation(CreateTime.class);
            if (Objects.isNull(createTime)) {
                continue;
            }
            if (Objects.nonNull(dataMap.get(field.getName()))) {
                continue;
            }
            dataMap.put(field.getName(), new Date());
        }
    }

    /**
     * 更新时自动更新的时间字段
     *
     * @param obj
     */
    public void getUpdateTime(Object obj) {
        for (Field field : obj.getClass().getDeclaredFields()) {
            UpdateTime updateTime = field.getAnnotation(UpdateTime.class);
            if (Objects.isNull(updateTime)) {
                continue;
            }
            if (Objects.nonNull(dataMap.get(field.getName()))) {
                continue;
            }
            dataMap.put(field.getName(), new Date());
        }
    }
}
