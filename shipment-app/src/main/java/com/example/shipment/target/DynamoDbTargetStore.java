package com.example.shipment.target;

import com.example.dualwrite.Document;
import com.example.dualwrite.DocumentKey;
import com.example.dualwrite.TargetStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * DynamoDB behind the library's TargetStore interface.
 *
 * <p>All the engine-specific detail lives here: attribute typing, the
 * partition/sort key pair, and the condition expressions. The library above
 * sees none of it.
 */
@Component
public class DynamoDbTargetStore implements TargetStore {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbTargetStore.class);

    public static final String TABLE = "shipment";
    private static final String PARTITION = "shipmentId";
    private static final String SORT = "orderId";

    private final DynamoDbClient ddb;

    public DynamoDbTargetStore(DynamoDbClient ddb) {
        this.ddb = ddb;
    }

    @Override
    public Optional<Document> get(DocumentKey key) {
        // A read holding only the partition key has to query rather than
        // fetch, because the sort key completes the address.
        if (key.sort() == null) {
            QueryResponse r = ddb.query(b -> b
                .tableName(TABLE)
                .keyConditionExpression("#p = :p")
                .expressionAttributeNames(Map.of("#p", PARTITION))
                .expressionAttributeValues(Map.of(":p", AttributeValue.fromS(key.partition())))
                .limit(1));
            return r.items().isEmpty() ? Optional.empty()
                                       : Optional.of(toDocument(r.items().get(0)));
        }
        GetItemResponse r = ddb.getItem(b -> b.tableName(TABLE).key(keyOf(key)));
        return r.hasItem() ? Optional.of(toDocument(r.item())) : Optional.empty();
    }

    @Override
    public void put(Document document) {
        ddb.putItem(b -> b.tableName(TABLE).item(toItem(document)));
    }

    /**
     * Undo a write, but only if it is still ours to undo.
     *
     * <p>The condition is the whole point. Between our write and this
     * compensation another writer may legitimately have advanced the record;
     * if it has, modifiedAt no longer matches what we wrote and we must
     * leave it alone. An unconditional undo here would destroy a good write
     * to repair a failed one.
     */
    @Override
    public void undo(DocumentKey key, Document prior, Instant writtenAt) {
        try {
            if (prior == null) {
                // Our write was an insert: remove it.
                ddb.deleteItem(b -> b
                    .tableName(TABLE)
                    .key(keyOf(key))
                    .conditionExpression("modifiedAt = :ours")
                    .expressionAttributeValues(ours(writtenAt)));
                log.info("compensated insert on {} by delete", key);
            } else {
                // Our write was an update: put the previous image back.
                // Deleting would not restore the old value, it would
                // destroy a record that legitimately existed before us.
                ddb.putItem(b -> b
                    .tableName(TABLE)
                    .item(toItem(prior))
                    .conditionExpression("modifiedAt = :ours")
                    .expressionAttributeValues(ours(writtenAt)));
                log.info("compensated update on {} by restoring prior image", key);
            }
        } catch (ConditionalCheckFailedException e) {
            log.info("compensation on {} skipped: record already moved on", key);
        } catch (RuntimeException e) {
            // The compensation failed outright, so the divergence stands.
            // Retries would help with throttling; what ultimately clears it
            // is the anti-entropy sweep.
            log.error("compensation on {} FAILED, divergence left for anti-entropy", key, e);
        }
    }

    private Map<String, AttributeValue> ours(Instant writtenAt) {
        return Map.of(":ours", AttributeValue.fromS(writtenAt.toString()));
    }

    private Map<String, AttributeValue> keyOf(DocumentKey key) {
        return Map.of(PARTITION, AttributeValue.fromS(key.partition()),
                      SORT, AttributeValue.fromS(key.sort()));
    }

    private Map<String, AttributeValue> toItem(Document d) {
        Map<String, AttributeValue> item = new LinkedHashMap<>();
        d.attributes().forEach((name, value) -> item.put(name, attribute(value)));
        return item;
    }

    private AttributeValue attribute(Object value) {
        if (value == null)             return AttributeValue.fromNul(true);
        if (value instanceof Number n) return AttributeValue.fromN(n.toString());
        if (value instanceof Instant i) return AttributeValue.fromS(i.toString());
        return AttributeValue.fromS(value.toString());
    }

    private Document toDocument(Map<String, AttributeValue> item) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        item.forEach((name, value) -> attributes.put(name, plain(name, value)));
        return new Document(
            DocumentKey.of((String) attributes.get(PARTITION), (String) attributes.get(SORT)),
            attributes);
    }

    private Object plain(String name, AttributeValue v) {
        if (Boolean.TRUE.equals(v.nul())) return null;
        if (v.n() != null)                return Integer.valueOf(v.n());
        if (name.endsWith("At"))          return Instant.parse(v.s());
        return v.s();
    }
}
