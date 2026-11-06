package com.provit.persistence.entity;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

@Entity
@DynamicUpdate
@Table(name = "t_refresh_token")
@Getter
@Setter
@NoArgsConstructor
public class RefreshTokenEntity {
    @Id
    @Column(name = "token_id", nullable = false, length = 36)
    private String tokenId;

    @Column(name = "user_num", nullable = false)
    private Integer userNum;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
