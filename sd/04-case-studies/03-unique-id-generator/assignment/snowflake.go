// Package snowflake is the Module 4.3 assignment: implement a Twitter-Snowflake-style
// distributed 64-bit ID generator.
//
// Read 04-case-studies/03-unique-id-generator/README.md first.
//
// Fill in every method marked `// TODO`. Run the tests until green:
//
//	go test ./...
//	go test -race ./...   // NextID must be safe under concurrency
//
// Bit layout (63 usable bits; the top bit is left 0 so IDs are positive int64):
//
//	 1 bit  unused (sign)
//	41 bits milliseconds since a custom epoch  (~69 years of range)
//	10 bits machine ID                          (0..1023)
//	12 bits per-millisecond sequence            (0..4095, 4096 IDs/ms/machine)
//
// IDs are time-sortable: a larger timestamp always yields a larger ID. Within one
// millisecond on one machine, the sequence increments. If the sequence overflows
// inside a millisecond, we wait for the next millisecond. If the clock ever moves
// backwards (NTP correction), we refuse to generate and return an error rather than
// risk a duplicate.
package snowflake

import (
	"errors"
	"sync"
	"time"
)

const (
	// Bit widths.
	timestampBits = 41
	machineBits   = 10
	sequenceBits  = 12

	// Max values derived from the widths.
	maxMachineID = -1 ^ (-1 << machineBits)  // 1023
	maxSequence  = -1 ^ (-1 << sequenceBits) // 4095

	// Left-shift amounts for packing each field into the 64-bit ID.
	machineShift   = sequenceBits                // 12
	timestampShift = sequenceBits + machineBits  // 22

	// Custom epoch: milliseconds since this instant are stored in the 41-bit field.
	// 2024-01-01T00:00:00Z. Picking a recent epoch maximizes the usable lifespan.
	customEpochMs = int64(1704067200000)
)

var (
	ErrInvalidMachineID = errors.New("snowflake: machineID out of range (must fit in 10 bits, 0..1023)")
	ErrClockMovedBack   = errors.New("snowflake: clock moved backwards; refusing to generate ID")
)

// Generator produces unique IDs for one machine. Safe for concurrent use.
//
// `now` returns the current time in milliseconds since the custom epoch. It is a
// field so tests can inject a deterministic clock instead of using the wall clock.
type Generator struct {
	machineID int64

	mu       sync.Mutex
	lastMs   int64 // last timestamp (ms since epoch) we generated an ID for
	sequence int64 // sequence within lastMs

	now func() int64 // ms since customEpoch
}

// defaultNow returns the current wall-clock time as ms since the custom epoch.
func defaultNow() int64 {
	return time.Now().UnixMilli() - customEpochMs
}

// NewGenerator creates a generator for the given machine ID (0..1023).
func NewGenerator(machineID int64) (*Generator, error) {
	if machineID < 0 || machineID > maxMachineID {
		return nil, ErrInvalidMachineID
	}
	return &Generator{
		machineID: machineID,
		lastMs:    -1,
		now:       defaultNow,
	}, nil
}

// NextID returns the next unique, time-ordered ID.
//
// Returns ErrClockMovedBack if the injected clock reports a time earlier than the
// last ID's timestamp.
func (g *Generator) NextID() (int64, error) {
	// TODO:
	//  1. Lock g.mu (NextID is called concurrently).
	//  2. ts := g.now().
	//  3. If ts < g.lastMs: the clock went backwards -> return 0, ErrClockMovedBack.
	//  4. If ts == g.lastMs (same millisecond):
	//        g.sequence = (g.sequence + 1) & maxSequence
	//        if g.sequence == 0 { // overflowed 0..4095 within this ms
	//            ts = g.waitNextMs(ts) // spin until now() > ts
	//        }
	//     else (a new, later millisecond):
	//        g.sequence = 0
	//  5. g.lastMs = ts.
	//  6. Pack and return:
	//        id := (ts << timestampShift) | (g.machineID << machineShift) | g.sequence
	panic("TODO: implement Generator.NextID")
}

// waitNextMs busy-waits until g.now() reports a millisecond strictly greater than
// `ts`, then returns that newer millisecond. Used when the sequence overflows.
func (g *Generator) waitNextMs(ts int64) int64 {
	// TODO:
	//  Loop calling g.now() until it returns a value > ts, then return that value.
	//  (In production this spins for sub-millisecond time; in tests the injected
	//   clock is advanced so the loop terminates.)
	panic("TODO: implement Generator.waitNextMs")
}

// ---------------- decode helpers (used by tests) ----------------

// Timestamp returns the milliseconds-since-custom-epoch encoded in an ID.
func Timestamp(id int64) int64 {
	// TODO: shift right by timestampShift to recover the 41-bit timestamp field.
	panic("TODO: implement Timestamp")
}

// MachineID returns the 10-bit machine ID encoded in an ID.
func MachineID(id int64) int64 {
	// TODO: shift right by machineShift, then mask with maxMachineID.
	panic("TODO: implement MachineID")
}

// Sequence returns the 12-bit per-millisecond sequence encoded in an ID.
func Sequence(id int64) int64 {
	// TODO: mask the id with maxSequence.
	panic("TODO: implement Sequence")
}
