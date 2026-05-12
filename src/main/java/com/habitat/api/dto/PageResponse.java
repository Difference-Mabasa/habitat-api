package com.habitat.api.dto;

import lombok.Builder;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Typed wrapper for every paginated endpoint. Hides Spring's Page so:
 *  - we don't leak {@code pageable}, {@code sort}, etc. on the wire
 *  - the contract is identical whether the source was a Page or a manual count
 *  - max page size cap (100) is enforced consistently — see PageSizeInterceptor.
 */
@Builder
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
