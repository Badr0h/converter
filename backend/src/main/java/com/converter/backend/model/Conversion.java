package com.converter.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data // includes getter + setter + toString + equals + hashCode
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "conversions")
public class Conversion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id ;
    
    @ManyToOne(optional = true)
    @JoinColumn(name = "user_id")
    private User user ;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_format",columnDefinition = "ENUM('TEXT', 'LATEX', 'MATHML', 'UNICODE', 'PYTHON', 'NUMPY', 'SYMPY', 'SCIPY', 'JAVASCRIPT', 'MATLAB', 'R') DEFAULT 'TEXT'", nullable = false)
    private Format inputFormat = Format.TEXT; 

    @Enumerated(EnumType.STRING)
    @Column(name = "output_format",columnDefinition = "ENUM('TEXT', 'LATEX', 'MATHML', 'UNICODE', 'PYTHON', 'NUMPY', 'SYMPY', 'SCIPY', 'JAVASCRIPT', 'MATLAB', 'R') DEFAULT 'PYTHON'", nullable = false)
    private Format outputFormat = Format.PYTHON; 

    @Column(name = "created_at",columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP", insertable = false, updatable = false)
    private LocalDateTime createdAt; 

    @Lob
    @Column(name = "prompt", nullable = false)
    private String prompt ; 

    @Lob
    @Column(name = "ai_response", nullable = false)
    private String aiResponse ; 


    public enum Format{
        TEXT,
        LATEX,
        MATHML,
        UNICODE,
        PYTHON,
        NUMPY,
        SYMPY,
        SCIPY,
        JAVASCRIPT,
        MATLAB,
        R
    }


}
