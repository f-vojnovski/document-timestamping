package com.documenttimestamp.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Getter @Setter @NoArgsConstructor
@Entity
@Table
public class Document {
    @Id
    @SequenceGenerator(
            name = "document_sequence",
            sequenceName = "document_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "document_sequence"
    )
    private Long id;

    private String title;

    @Column(length = 2048)
    private String encryptedHash;

    @Column(length = 2048)
    private String documentChecksum;

    @Column(length = 2048)
    private String targetHash;

    private Long timestamp;

    @Transient
    private String publicKey;
}
