package com.provit.persistence.entity;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.DynamicUpdate;

@Entity
@DynamicUpdate
@Table(name = "t_occupation")
@Getter
@Setter
@NoArgsConstructor
public class OccupationEntity {
    @Id
    @Column(name = "occupation_code", nullable = false, length = 20)
    private String occupationCode;

    @Column(name = "occupation_name", nullable = false, length = 200)
    private String occupationName;
}
