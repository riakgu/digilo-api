package com.riakgu.digilo.common.dto;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;

@Data
@Builder
public class Pagination {

    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;

    public static Pagination from(Page<?> page) {
        return Pagination.builder()
                .page(page.getNumber() + 1)
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
