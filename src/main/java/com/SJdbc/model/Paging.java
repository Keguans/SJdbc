package com.SJdbc.model;

import java.util.List;

/**
 * 分页
 *
 * @param <T>
 */
public class Paging<T> {

    public Paging() {
        super();
    }

    public Paging(Long total, List<T> records) {
        this.total = total;
        this.records = records;
    }

    /**
     * 总数
     */
    private Long total;

    /**
     * 数据
     */
    private List<T> records;

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }

    /**
     * 分页参数
     */
    public static class Page {

        public Page() {
            super();
        }

        public Page(int pageNum, int pageSize) {
            this.pageNum = pageNum;
            this.pageSize = pageSize;
        }

        /**
         * 页码
         */
        private int pageNum;

        /**
         * 页长
         */
        private int pageSize;

        public int getPageNum() {
            return pageNum;
        }

        public void setPageNum(int pageNum) {
            this.pageNum = pageNum;
        }

        public int getPageSize() {
            return pageSize;
        }

        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }
    }
}
