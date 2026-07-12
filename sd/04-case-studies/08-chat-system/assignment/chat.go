// Package chat is the Module 4.8 assignment: an in-memory chat Hub built the
// idiomatic Go way — one goroutine owns all shared state, and the outside world
// talks to it over channels (the "do not communicate by sharing memory; share
// memory by communicating" rule).
//
// Read 04-case-studies/08-chat-system/README.md first.
//
// Fill in every function marked `// TODO`. Run the tests until green:
//
//	go test ./...
//	go test -race ./...   // the Hub is hammered by many goroutines; it must be clean
//
// The design (mirror of a real connection server):
//   - A Client is one connected user. It has a buffered `Out` channel — the Hub
//     pushes messages into it the way a real server writes to a WebSocket.
//   - The Hub owns the room->members map. NOBODY else touches that map. Join,
//     Leave, Broadcast and Stop are thin wrappers that send a command struct down
//     a channel to the Hub's single Run() goroutine, which mutates state serially.
//     Serial ownership is why we need no mutex on the map at all.
package chat

// Message is one chat message delivered to clients in a room.
// Seq is assigned by the Hub: a per-room, monotonically increasing sequence
// number that gives every receiver the SAME ordering (see the lesson on
// per-conversation sequence numbers).
type Message struct {
	Room string
	From string
	Body string
	Seq  uint64
}

// Client is one connected participant. Out is buffered so a slow reader does not
// block the Hub; in production a full buffer means "this client is too slow" and
// you would drop or disconnect it. ID is just for debugging/equality.
type Client struct {
	ID  string
	Out chan Message
}

// NewClient makes a client with an outbound buffer of the given size.
func NewClient(id string, buffer int) *Client {
	return &Client{ID: id, Out: make(chan Message, buffer)}
}

// Hub owns all room membership and the per-room sequence counters. All mutation
// happens inside Run(); the public methods only enqueue commands.
type Hub struct {
	// Command channels. The public API sends on these; Run() receives on them.
	joinCh  chan joinCmd
	leaveCh chan leaveCmd
	bcastCh chan bcastCmd
	stopCh  chan struct{}
	doneCh  chan struct{} // closed by Run() when it has fully exited

	// State OWNED by Run() — do not read or write these from other goroutines.
	rooms map[string]map[*Client]struct{}
	seq   map[string]uint64
}

// internal command structs passed to Run().
type joinCmd struct {
	room string
	c    *Client
}

type leaveCmd struct {
	room string
	c    *Client
}

type bcastCmd struct {
	msg Message
}

// NewHub builds a Hub. Call Run() in a goroutine before using it.
func NewHub() *Hub {
	return &Hub{
		joinCh:  make(chan joinCmd),
		leaveCh: make(chan leaveCmd),
		bcastCh: make(chan bcastCmd),
		stopCh:  make(chan struct{}),
		doneCh:  make(chan struct{}),
		rooms:   make(map[string]map[*Client]struct{}),
		seq:     make(map[string]uint64),
	}
}

// Run is the Hub's single event loop. It is the ONLY goroutine that touches
// h.rooms and h.seq. Launch it once: `go hub.Run()`.
func (h *Hub) Run() {
	// TODO:
	//  Loop with `select` over the four command channels:
	//    - joinCh:  add c to h.rooms[room] (create the inner map if absent).
	//    - leaveCh: remove c from h.rooms[room]; delete the room if it becomes empty.
	//    - bcastCh: increment h.seq[room], stamp cmd.msg.Seq with it, then send the
	//               message to every current member's Out channel. Use a
	//               non-blocking send (select with default) so one slow/full client
	//               cannot freeze the whole Hub.
	//    - stopCh:  close(h.doneCh) and return.
	panic("TODO: implement Hub.Run")
}

// Join adds c to room. Safe to call from any goroutine.
func (h *Hub) Join(room string, c *Client) {
	// TODO: send a joinCmd on h.joinCh.
	panic("TODO: implement Hub.Join")
}

// Leave removes c from room. Safe to call from any goroutine.
func (h *Hub) Leave(room string, c *Client) {
	// TODO: send a leaveCmd on h.leaveCh.
	panic("TODO: implement Hub.Leave")
}

// Broadcast delivers msg to every current member of msg.Room. The Hub assigns
// msg.Seq; any Seq the caller set is overwritten. Safe to call from any goroutine.
func (h *Hub) Broadcast(room string, msg Message) {
	// TODO: set msg.Room = room and send a bcastCmd on h.bcastCh.
	panic("TODO: implement Hub.Broadcast")
}

// Stop shuts the Hub down and blocks until Run() has exited. After Stop returns,
// the Hub must not be used again.
func (h *Hub) Stop() {
	// TODO: close(h.stopCh), then wait on <-h.doneCh.
	panic("TODO: implement Hub.Stop")
}
