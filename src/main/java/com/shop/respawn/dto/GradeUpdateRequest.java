package com.shop.respawn.dto;

import com.shop.respawn.domain.Grade;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GradeUpdateRequest {
    private Grade newGrade;
}