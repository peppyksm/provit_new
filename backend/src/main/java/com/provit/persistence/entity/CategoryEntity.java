package com.provit.persistence.entity;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.DynamicUpdate;

@Entity
@DynamicUpdate
@Table(name = "t_category")
@Getter
@Setter
@NoArgsConstructor
public class CategoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_t_category")
    @SequenceGenerator(name = "seq_t_category", sequenceName = "seq_t_category", allocationSize = 1)
    @Column(name = "category_num", nullable = false)
    private Integer categoryNum;

    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;
}
