package com.labhub.common.dto;

import java.util.List;

/**
 * Generic paginated result wrapper used for all list responses.
 *
 * @param <T> the type of items in this page
 */
public class PagedResult<T> {

    private List<T> items;
    private int page;
    private int pageSize;
    private long totalItems;
    private int totalPages;

    public PagedResult() { }

    /**
     * Full constructor.
     *
     * @param items      items in this page
     * @param page       current page number (1-based)
     * @param pageSize   number of items per page
     * @param totalItems total number of items across all pages
     */
    public PagedResult(List<T> items, int page, int pageSize, long totalItems) {
        this.items = items;
        this.page = page;
        this.pageSize = pageSize;
        this.totalItems = totalItems;
        // Integer division rounding up
        this.totalPages = pageSize > 0 ? (int) Math.ceil((double) totalItems / pageSize) : 0;
    }

    public List<T> getItems() { return items; }

    public void setItems(List<T> items) { this.items = items; }

    public int getPage() { return page; }

    public void setPage(int page) { this.page = page; }

    public int getPageSize() { return pageSize; }

    public void setPageSize(int pageSize) { this.pageSize = pageSize; }

    public long getTotalItems() { return totalItems; }

    public void setTotalItems(long totalItems) { this.totalItems = totalItems; }

    public int getTotalPages() { return totalPages; }

    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public boolean hasNext() {
        return page < totalPages;
    }

    public boolean hasPrevious() {
        return page > 1;
    }
}
