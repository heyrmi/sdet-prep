// Package snowflake is the reference solution for Module 4.3: a Twitter-Snowflake
// style 64-bit distributed ID generator. Try the assignment first!
package snowflake

import (
	"errors"
	"sync"
	"time"
)

const (
	timestampBits = 41
	machineBits   = 10
	sequenceBits  = 12

	maxMachineID = -1 ^ (-1 << machineBits)  // 1023
	maxSequence  = -1 ^ (-1 << sequenceBits) // 4095

	machineShift   = sequenceBits               // 12
	timestampShift = sequenceBits + machineBits // 22

	customEpochMs = int64(1704067200000) // 2024-01-01T00:00:00Z
)

var (
	ErrInvalidMachineID = errors.New("snowflake: machineID out of range (must fit in 10 bits, 0..1023)")
	ErrClockMovedBack   = errors.New("snowflake: clock moved backwards; refusing to generate ID")
)

type Generator struct {
	machineID int64

	mu       sync.Mutex
	lastMs   int64
	sequence int64

	now func() int64
}

func defaultNow() int64 {
	return time.Now().UnixMilli() - customEpochMs
}

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

func (g *Generator) NextID() (int64, error) {
	g.mu.Lock()
	defer g.mu.Unlock()

	ts := g.now()
	if ts < g.lastMs {
		return 0, ErrClockMovedBack
	}

	if ts == g.lastMs {
		g.sequence = (g.sequence + 1) & maxSequence
		if g.sequence == 0 {
			// Sequence exhausted within this millisecond; wait for the next one.
			ts = g.waitNextMs(ts)
		}
	} else {
		g.sequence = 0
	}

	g.lastMs = ts
	id := (ts << timestampShift) | (g.machineID << machineShift) | g.sequence
	return id, nil
}

func (g *Generator) waitNextMs(ts int64) int64 {
	t := g.now()
	for t <= ts {
		t = g.now()
	}
	return t
}

// ---------------- decode helpers ----------------

func Timestamp(id int64) int64 { return id >> timestampShift }
func MachineID(id int64) int64 { return (id >> machineShift) & maxMachineID }
func Sequence(id int64) int64  { return id & maxSequence }
