package com.provit.persistence.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class ReviewSourceEntityId implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long consistencyNum;
    private Integer displayOrder;
}
