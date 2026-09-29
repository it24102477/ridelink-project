package com.ridelink.driver.service;

import com.ridelink.driver.model.DbSequence;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * Generates short, sequential numeric ids ("1", "2", "3", ...) in place of MongoDB's
 * default ObjectId. One counter document per sequence name is kept in the
 * "database_sequences" collection and incremented atomically via findAndModify,
 * so it stays correct under concurrent requests / multiple service instances.
 */
@Service
public class SequenceGeneratorService {

    private final MongoOperations mongoOperations;

    public SequenceGeneratorService(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    public long nextSequence(String sequenceName) {
        DbSequence counter = mongoOperations.findAndModify(
                Query.query(where("_id").is(sequenceName)),
                new Update().inc("seq", 1),
                FindAndModifyOptions.options().returnNew(true).upsert(true),
                DbSequence.class);
        return counter != null ? counter.getSeq() : 1L;
    }

    /** Convenience wrapper: entities in this service store their id as a String. */
    public String nextId(String sequenceName) {
        return String.valueOf(nextSequence(sequenceName));
    }
}
