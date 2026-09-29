package com.ridelink.account.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Backing store for {@link com.ridelink.account.service.SequenceGeneratorService}.
 * MongoDB has no native auto-increment, so each entity type gets a counter document
 * here (one per collection name) that is atomically incremented to produce short,
 * human-friendly numeric ids (1, 2, 3, ...) instead of ObjectId hex strings.
 */
@Document(collection = "database_sequences")
public class DbSequence {

    @Id
    private String id; // sequence name, e.g. "users"

    private long seq;

    public DbSequence() {}

    public DbSequence(String id, long seq) {
        this.id = id;
        this.seq = seq;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public long getSeq() { return seq; }
    public void setSeq(long seq) { this.seq = seq; }
}
