package com.habitat.api.dto.mandate;

import com.habitat.api.entity.ChangeItem;
import com.habitat.api.enums.ChangeRequestField;

public record ChangeItemResponse(
        ChangeRequestField field,
        String currentValue,
        String requestedValue
) {
    public static ChangeItemResponse from(ChangeItem item) {
        return new ChangeItemResponse(item.field(), item.currentValue(), item.requestedValue());
    }
}
