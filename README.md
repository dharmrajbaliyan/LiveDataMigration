# Dual-write example

A working implementation of the dual-write protocol, phased reads and
compensating rollback described in *Achieving Correctness and Fault Tolerance
in Live Data Migrations*.

Runs with no infrastructure: H2 stands in for the relational source and
DynamoDB Local runs in-process as the target.

```
gradle :shipment-app:run
```

## Layout

```
dualwrite-lib/     the reusable part, ~400 lines
  MigrationPhase          the four cutover phases
  DualStore               the protocol: save() and findById()
  SchemaConverter         entity <-> document  (the one app-specific piece)
  SourceStore             the relational side
  TargetStore             the key-value side
  Compensations           registers the rollback callback
  ShadowReadComparator    compares, and tracks the match rate
  Document, DocumentKey   a store-neutral record

shipment-app/      one table, two endpoints
  domain/      ShipmentEntity, ShipmentStatus
  repo/        hand-written save/get routed through DualStore
  convert/     ShipmentSchemaConverter
  target/      DynamoDbTargetStore (conditional writes live here)
  config/      phase config, DynamoDB Local, bean wiring
  api/         ShipmentController
```

The library has no dependency on JPA, DynamoDB or the AWS SDK. It reaches the
target only through `TargetStore`, which is what lets the same protocol drive a
different engine.

## The schema

Relational (`shipment`): `shipment_id` primary key, `order_id`, `status`,
`item_count`, `estimated_delivery_date`, `executed_delivery_date`,
`created_at`, `modified_at`.

DynamoDB (`shipment`): partition key `shipmentId`, sort key `orderId`.

## Endpoints

| | |
|---|---|
| `GET /shipments/{id}/status` | read status; reports which store served it |
| `PUT /shipments/{id}/status?to=PACK` | advance status |
| `GET /shipments/migration` | current phase, match rate, comparison count |
| `PUT /shipments/migration?to=READ_TARGET` | advance the phase |

Two shipments are seeded at startup: `SHIP-1` (INIT) and `SHIP-2` (PICK).

## What to look at

**`DualStore.save`** is the protocol. Four steps, and the ordering is the
whole argument:

1. a transaction is already open
2. write the source — uncommitted, invisible to other readers
3. arm the compensation, *then* write the target — which commits on ACK
4. return, and the source transaction commits

Between 3 and 4 the target holds a committed write and the source does not.
Every failure mode lives in that window.

**One timestamp, minted once.** `DualStore.save` takes a single reading from
the clock and writes it to both stores. Comparing the two copies later
compares two copies of one reading rather than two clocks, so no skew between
the stores can arise. It is truncated to milliseconds because the target
cannot represent more — a value that survives the round trip unchanged is
worth more than the extra precision.

**`ShipmentSourceStore.findCommittedById`** is subtler than it looks. Undoing
an update means restoring what the record held *before* the transaction, so
the prior image has to be the committed row, not the mutated instance the
caller is holding. It evicts the persistence context's copy and reads again.

**`DynamoDbTargetStore.undo`** is conditional, and distinguishes the two
cases. An insert is undone by deleting. An update is undone by restoring the
prior image — deleting would not restore the old value but destroy a record
that legitimately existed. Both apply only if the record still holds what this
transaction wrote, so a concurrent writer that legitimately advanced it
survives.

## Seeing it work

### The dual write reaches both stores

Advance a status, then switch the read phase to `READ_TARGET` and read again.
The value comes back from DynamoDB with the same `modifiedAt`:

```
PUT /shipments/SHIP-1/status?to=PICK
PUT /shipments/migration?to=READ_TARGET
GET /shipments/SHIP-1/status
  -> {"status":"PICK","servedFrom":"target","modifiedAt":"...","...":"..."}
```

### Compensation restores the target

`failAfterTargetWrite=true` fails the source transaction *after* the target
write has committed — the window that is otherwise hard to reach on purpose:

```
PUT /shipments/SHIP-2/status?to=PACK&failAfterTargetWrite=true
  -> 500

GET /shipments/SHIP-2/status                     # source: still PICK
PUT /shipments/migration?to=READ_TARGET
GET /shipments/SHIP-2/status                     # target: also PICK
```

The log shows the undo:

```
WARN  DualStore           : source rolled back, undoing target write for SHIP-2/ORDER-2
INFO  DynamoDbTargetStore : compensated update on SHIP-2/ORDER-2 by restoring prior image
```

### Shadow reads and the match rate

In `SHADOW_COMPARE`, every read is served from the source and also issued
against the target and compared. `GET /shipments/migration` reports the rate
that gates advancing to the next phase.

Mismatches on records written within the last 50ms are counted separately as
transient: they are the dual-write window being observed, not real divergence.
This is why the bar is a high threshold rather than exact equality — the
window guarantees a small permanent rate of disagreement, so a bar of 100%
would never be met and the cutover would never happen.

## Phases

| Phase | Writes | Reads | Reversible |
|---|---|---|---|
| `DUAL_WRITE_READ_SOURCE` | both | source | yes |
| `SHADOW_COMPARE` | both | source, compared to target | yes |
| `READ_TARGET` | both | target | yes |
| `TARGET_ONLY` | target | target | **no** |

Set the starting phase in `application.yml`, or move between them at runtime
via `PUT /shipments/migration`.

The last row is the one that matters. Every phase up to `READ_TARGET` can be
reversed for free, because the source still holds a complete copy. Once the
source stops receiving writes it begins to rot immediately, and there is no
way back that does not lose data. That is why the gating criteria sit where
they do: a migration whose final step is irreversible has to spend its
evidence budget before taking that step, not after.

## Not included

Backfill and anti-entropy. This example covers the online path only — dual
write, phased and shadow reads, and compensation. Backfilling history and
sweeping for dangling writes are separate services described in the article.
