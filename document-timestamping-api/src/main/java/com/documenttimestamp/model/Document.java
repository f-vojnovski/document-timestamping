package com.documenttimestamp.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;

@Getter @Setter @NoArgsConstructor
@Entity
@Table(indexes = @Index(name = "idx_document_checksum", columnList = "documentChecksum"))
public class Document {
    public static final int TITLE_MAX_LENGTH = 255;
    private static final int SHA512_HEX_LENGTH = 128;

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

    @Column(length = TITLE_MAX_LENGTH, nullable = false)
    private String title;

    @Column(length = 2048, nullable = false)
    private String encryptedHash;

    @Column(length = SHA512_HEX_LENGTH, nullable = false)
    private String documentChecksum;

    @Column(length = SHA512_HEX_LENGTH, nullable = false)
    private String targetHash;

    @Column(nullable = false)
    private Long timestamp;
}
