package com.example.bakery.feature.user.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.Getter;
import lombok.Setter;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {
 @Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private UserRole name;
     public Role(UserRole name) {
    	    this.name = name;
    	}
    }