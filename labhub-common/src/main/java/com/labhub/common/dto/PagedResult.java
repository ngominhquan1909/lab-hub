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

    /** Required by Jackson. */
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

    // ── Getters / setters ──────────────────────────────────────────────────

    /** @return items on this page */
    public List<T> getItems() { return items; }

    /** @param items items */
    public void setItems(List<T> items) { this.items = items; }

    /** @return current page number (1-based) */
    public int getPage() { return page; }

    /** @param page page number */
    public void setPage(int page) { this.page = page; }

    /** @return number of items requested per page */
    public int getPageSize() { return pageSize; }

    /** @param pageSize page size */
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }

    /** @return total number of items matching the query */
    public long getTotalItems() { return totalItems; }

    /** @param totalItems total items */
    public void setTotalItems(long totalItems) { this.totalItems = totalItems; }

    /** @return total number of pages */
    public int getTotalPages() { return totalPages; }

    /** @param totalPages total pages */
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    /** @return true if there is a next page */
    public boolean hasNext() {
        return page < totalPages;
    }

    /** @return true if there is a previous page */
    public boolean hasPrevious() {
        return page > 1;
    }
}
