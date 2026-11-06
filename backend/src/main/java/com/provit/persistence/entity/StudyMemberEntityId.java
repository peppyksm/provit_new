package com.provit.persistence.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class StudyMemberEntityId implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long studyNum;
    private Integer userNum;
}
