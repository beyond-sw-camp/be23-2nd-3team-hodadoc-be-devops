package com.beyond.hodadoc.admin.dtos;

import com.beyond.hodadoc.admin.domain.FilterCategory;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class FilterCreateDto {
    private String name;
    private FilterCategory category;
}
